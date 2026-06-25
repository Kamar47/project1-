package server;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import DB.DatabaseController;

/**
 * Background daemon thread that runs scheduled tasks every 60 seconds on the GoNature server.
 * <p>
 * The thread performs three recurring operations:
 * </p>
 * <ol>
 *   <li><b>Send reminders</b> ({@link #processReminders()}): finds confirmed orders
 *       scheduled for tomorrow that have not yet received a reminder, marks them
 *       as reminded, and inserts a {@code reminder} notification record.</li>
 *   <li><b>Auto-cancel unconfirmed orders</b> ({@link #processAutoCancel()}): finds
 *       confirmed orders whose reminder was sent more than 2 hours ago but was never
 *       confirmed by the traveler. Sets the order status to {@code expired} and inserts
 *       a {@code reminder_expired} notification so the traveler sees a cancellation popup.</li>
 *   <li><b>Process waitlist</b> ({@link #processWaitlist()}): expires stale waitlist
 *       offers, then promotes the next waiting entry and sends a {@code waitlist_available}
 *       notification.</li>
 * </ol>
 * <p>
 * All database operations execute inside {@code synchronized(DB_LOCK)} to avoid
 * conflicts with concurrent client requests handled by {@link MessageHandler}.
 * </p>
 *
 * @author Group 11
 */
public class ReminderThread extends Thread {
    private DatabaseController db;
    private BackEndServer server;
    private volatile boolean running = true;

    /**
     * Creates a reminder thread that periodically performs scheduled server tasks.
     *
     * @param db the database controller used for scheduled database operations
     * @param server the server instance used for writing log messages
     */
    public ReminderThread(DatabaseController db, BackEndServer server) {
        this.db = db;
        this.server = server;
        setDaemon(true);
    }

    /**
     * Stops the reminder thread loop.
     */
    public void stopRunning() { running = false; }

    /**
     * Runs the background reminder loop.
     * The method periodically processes reminders, automatic cancellations,
     * waitlist promotions, and automatic visit completion until the thread is stopped.
     */
    @Override
    public void run() {
        server.log("[Reminder] Background reminder thread started.");
        while (running) {
            try {
                synchronized (DB.MySqlConnector.DB_LOCK) {
                    processReminders();
                    processAutoCancel();
                    processWaitlist();
                    processAutoCompleteVisits();
                }
                Thread.sleep(60000); // check every 60 seconds
            } catch (InterruptedException e) {
                break;
            } catch (Exception e) {
                server.log("[Reminder] Error: " + e.getMessage());
            }
        }
        server.log("[Reminder] Background reminder thread stopped.");
    }

    /**
     * Finds confirmed visits scheduled for tomorrow and creates reminder notifications
     * for orders that have not received a reminder yet.
     *
     * @throws SQLException if a database error occurs while processing reminders
     */
    private void processReminders() throws SQLException {
        // Find confirmed orders for tomorrow that haven't had reminders sent
        Connection conn = DB.MySqlConnector.getInstance().getConnection();
        PreparedStatement ps = conn.prepareStatement(
            "SELECT * FROM orders WHERE status = 'confirmed' AND reminder_sent = FALSE " +
            "AND visit_date = DATE_ADD(CURDATE(), INTERVAL 1 DAY)");
        ResultSet rs = ps.executeQuery();
        int count = 0;
        while (rs.next()) {
            int orderId = rs.getInt("order_id");
            String email = rs.getString("email");
            String phone = rs.getString("phone");
            // Mark reminder as sent
            PreparedStatement up = conn.prepareStatement(
                "UPDATE orders SET reminder_sent = TRUE, reminder_sent_at = NOW() WHERE order_id = ?");
            up.setInt(1, orderId);
            up.executeUpdate();
            up.close();
            // Log notification
            PreparedStatement notif = conn.prepareStatement(
                "INSERT INTO notifications (order_id, recipient_email, recipient_phone, notification_type, message_text) " +
                "VALUES (?, ?, ?, 'reminder', 'Visit reminder sent')");
            notif.setInt(1, orderId); notif.setString(2, email); notif.setString(3, phone);
            notif.executeUpdate(); notif.close();
            count++;
        }
        rs.close(); ps.close();
        if (count > 0) server.log("[Reminder] Sent " + count + " reminders for tomorrow's visits.");
    }

    /**
     * Automatically cancels confirmed orders whose reminder was sent more than two hours ago
     * and was not confirmed by the traveler.
     * A cancellation notification is created for each expired order.
     *
     * @throws SQLException if a database error occurs while cancelling orders
     */
    private void processAutoCancel() throws SQLException {
        // Auto-cancel orders where reminder was sent > 2 hours ago and not confirmed
        Connection conn = DB.MySqlConnector.getInstance().getConnection();
        PreparedStatement ps = conn.prepareStatement(
            "SELECT * FROM orders WHERE status = 'confirmed' AND reminder_sent = TRUE " +
            "AND reminder_confirmed = FALSE AND TIMESTAMPDIFF(HOUR, reminder_sent_at, NOW()) >= 2");
        ResultSet rs = ps.executeQuery();
        int count = 0;
        while (rs.next()) {
            int orderId = rs.getInt("order_id");
            String email = rs.getString("email");
            String code = rs.getString("confirmation_code") != null ? rs.getString("confirmation_code") : "#" + orderId;
            PreparedStatement up = conn.prepareStatement("UPDATE orders SET status = 'expired' WHERE order_id = ?");
            up.setInt(1, orderId); up.executeUpdate(); up.close();
            // Notify visitor - created once per order (status change prevents re-entry)
            PreparedStatement notif = conn.prepareStatement(
                "INSERT INTO notifications (order_id, recipient_email, notification_type, message_text) " +
                "VALUES (?, ?, 'reminder_expired', ?)");
            notif.setInt(1, orderId);
            notif.setString(2, email);
            notif.setString(3, "Your visit booking " + code + " was automatically cancelled because you did not confirm the reminder within 2 hours.");
            notif.executeUpdate(); notif.close();
            count++;
        }
        rs.close(); ps.close();
        if (count > 0) server.log("[Reminder] Auto-cancelled " + count + " unconfirmed orders.");
    }

    /**
     * Processes waiting list logic.
     * The method expires old pending waitlist offers and promotes eligible waitlist orders.
     *
     * @throws SQLException if a database error occurs while processing the waiting list
     */
    private void processWaitlist() throws SQLException {
        int expired = db.expireOldWaitlistOffers();

        if (expired > 0) {
            server.log("[Waitlist] Expired " + expired + " pending waitlist offer(s).");
        }

        int promoted = db.processWaitingLists();

        if (promoted > 0) {
            server.log("[Waitlist] Promoted " + promoted + " waitlist order(s) to pending.");
        }
    }

    /**
     * Automatically completes visits that remained in park longer than the estimated visit duration.
     * This handles cases where visitors did not formally record their exit.
     *
     * @throws SQLException if a database error occurs while completing expired visits
     */
    private void processAutoCompleteVisits() throws SQLException {
        // Uses db field (DatabaseController) — safe under DB_LOCK, no raw connection access
        int count = db.autoCompleteExpiredVisits();
        if (count > 0) server.log("[Reminder] Auto-completed " + count + " expired in-park visit(s).");
    }

}