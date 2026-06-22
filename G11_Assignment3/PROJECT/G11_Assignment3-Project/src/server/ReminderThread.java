package server;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import DB.DatabaseController;

/**
 * Background thread that runs on the server.
 * 1. Sends reminders 1 day before visit
 * 2. Auto-cancels orders that weren't confirmed within 2 hours of reminder
 * 3. Processes waitlist when spots open up
 */
public class ReminderThread extends Thread {
    private DatabaseController db;
    private BackEndServer server;
    private volatile boolean running = true;

    public ReminderThread(DatabaseController db, BackEndServer server) {
        this.db = db;
        this.server = server;
        setDaemon(true);
    }

    public void stopRunning() { running = false; }

    @Override
    public void run() {
        server.log("[Reminder] Background reminder thread started.");
        while (running) {
            try {
                processReminders();
                processAutoCancel();
                processWaitlist();
                Thread.sleep(60000); // check every 60 seconds
            } catch (InterruptedException e) {
                break;
            } catch (Exception e) {
                server.log("[Reminder] Error: " + e.getMessage());
            }
        }
        server.log("[Reminder] Background reminder thread stopped.");
    }

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
            PreparedStatement up = conn.prepareStatement("UPDATE orders SET status = 'expired' WHERE order_id = ?");
            up.setInt(1, orderId); up.executeUpdate(); up.close();
            // Log
            PreparedStatement notif = conn.prepareStatement(
                "INSERT INTO notifications (order_id, recipient_email, notification_type, message_text) " +
                "VALUES (?, ?, 'reminder_expired', 'Order auto-cancelled - reminder not confirmed')");
            notif.setInt(1, orderId); notif.setString(2, rs.getString("email"));
            notif.executeUpdate(); notif.close();
            count++;
        }
        rs.close(); ps.close();
        if (count > 0) server.log("[Reminder] Auto-cancelled " + count + " unconfirmed orders.");
    }

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
}