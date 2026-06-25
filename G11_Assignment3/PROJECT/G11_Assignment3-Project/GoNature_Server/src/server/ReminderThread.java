package server;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import DB.DatabaseController;

/**
 * Background daemon thread that runs scheduled server tasks every 60 seconds.
 * <p>
 * The thread handles visit reminders, automatic cancellation of unconfirmed orders,
 * waitlist processing, and automatic completion of expired in-park visits.
 * All database operations are executed inside {@link DB.MySqlConnector#DB_LOCK}
 * to avoid conflicts with concurrent client requests handled by {@link MessageHandler}.
 * </p>
 *
 * @author Group 11
 */
public class ReminderThread extends Thread {
    private DatabaseController db;
    private BackEndServer server;
    private volatile boolean running = true;

    /**
     * Creates a reminder background thread.
     * The thread receives the database controller and server instance used for
     * database operations and logging.
     *
     * @param db the database controller used by the reminder thread
     * @param server the server instance used for logging reminder activity
     */
    public ReminderThread(DatabaseController db, BackEndServer server) {
        this.db = db;
        this.server = server;
        setDaemon(true);
    }

    /**
     * Stops the reminder thread loop.
     * The thread will finish after the current cycle ends.
     */
    public void stopRunning() { running = false; }

    /**
     * Runs the background reminder process.
     * Every 60 seconds, the method sends visit reminders, cancels unconfirmed orders,
     * processes waitlist offers, and completes expired in-park visits.
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
     * Sends reminders for confirmed orders scheduled for tomorrow.
     * The method marks each order as reminded and creates a reminder notification.
     *
     * @throws SQLException if the reminder query or update fails
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
     * Automatically expires confirmed orders whose reminder was sent more than two hours ago
     * and was not confirmed by the traveler.
     * The method updates the order status and creates a cancellation notification.
     *
     * @throws SQLException if the auto-cancel query or update fails
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
     * Processes the waiting lists.
     * The method first expires old waitlist offers and then tries to promote eligible
     * waiting orders to pending status.
     *
     * @throws SQLException if the waitlist processing fails
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
     * Automatically completes visits that remained in the park longer than the estimated
     * visit duration and were not formally closed by an exit action.
     *
     * @throws SQLException if the automatic completion update fails
     */
    private void processAutoCompleteVisits() throws SQLException {
        // Uses db field (DatabaseController) — safe under DB_LOCK, no raw connection access
        int count = db.autoCompleteExpiredVisits();
        if (count > 0) server.log("[Reminder] Auto-completed " + count + " expired in-park visit(s).");
    }

}