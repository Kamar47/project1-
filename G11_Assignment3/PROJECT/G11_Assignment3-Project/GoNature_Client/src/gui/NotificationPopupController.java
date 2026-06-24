package gui;

import client.ClientUI;
import common.ClientServerMessage;
import common.Command;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

/**
 * Controller for the notification popup window.
 * Handles both reminder and waitlist_available notifications.
 *
 * Scenarios:
 *   REMINDER:
 *     - Confirm -> CONFIRM_ORDER (sets reminder_confirmed=TRUE, order stays confirmed)
 *     - Later   -> close popup, notification stays unread so user can confirm from My Orders
 *
 *   WAITLIST_AVAILABLE:
 *     - Confirm -> CONFIRM_ORDER (pending -> confirmed, spot is theirs)
 *     - Later   -> close popup, notification stays unread, 1-hour window still counting
 *                  user can confirm from My Orders before window expires
 */
public class NotificationPopupController {

    @FXML private Label titleLabel, subtitleLabel, iconLabel;
    @FXML private Label parkLabel, dateLabel, timeLabel, visitorsLabel;
    @FXML private HBox timerBox;
    @FXML private Label timerLabel;
    @FXML private Button confirmBtn;
    @FXML private Button laterBtn;

    private int orderId;
    private int notificationId;
    private String notificationType; // "reminder" or "waitlist_available"

    /**
     * Called by TravelerFrameController after loading the FXML.
     * row = [notificationId, orderId, type, message, parkName, date, time, visitors, orderStatus]
     */
    public void setNotification(java.util.ArrayList<String> row) {
        notificationId  = Integer.parseInt(row.get(0));
        orderId         = Integer.parseInt(row.get(1));
        notificationType = row.get(2);
        String park     = row.get(4);
        String date     = row.get(5);
        String time     = row.get(6).length() >= 5 ? row.get(6).substring(0, 5) : row.get(6);
        String visitors = row.get(7);

        parkLabel.setText(park);
        dateLabel.setText(date);
        timeLabel.setText(time);
        visitorsLabel.setText(visitors + " visitors");

        if ("reminder_expired".equals(notificationType)) {
            iconLabel.setText("❌");
            iconLabel.setStyle("-fx-font-size: 26px; -fx-min-width: 48; -fx-min-height: 48; -fx-alignment: center; -fx-background-color: #2a0a0a; -fx-background-radius: 50;");
            titleLabel.setText("Visit cancelled");
            String msg = row.get(3);
            subtitleLabel.setText(msg != null && !msg.isEmpty() ? msg : "Your visit was automatically cancelled because you did not confirm the reminder within 2 hours.");
            timerBox.setVisible(false);
            timerBox.setManaged(false);
            confirmBtn.setText("OK");
            confirmBtn.setStyle("-fx-background-color: #ef4444; -fx-text-fill: white; -fx-background-radius: 10; -fx-font-size: 14px; -fx-font-weight: bold; -fx-cursor: hand;");
            if (laterBtn != null) { laterBtn.setVisible(false); laterBtn.setManaged(false); }
        } else if ("reminder".equals(notificationType)) {
            iconLabel.setText("🔔");
            iconLabel.setStyle("-fx-font-size: 26px; -fx-min-width: 48; -fx-min-height: 48; -fx-alignment: center; -fx-background-color: #1e1b3a; -fx-background-radius: 50;");
            titleLabel.setText("Visit reminder");
            subtitleLabel.setText("Confirm your visit — it will be cancelled after 2 hours if not confirmed");
            timerBox.setVisible(false);
            timerBox.setManaged(false);
            confirmBtn.setText("✓  Confirm visit");
        } else { // waitlist_available
            iconLabel.setText("🎉");
            iconLabel.setStyle("-fx-font-size: 26px; -fx-min-width: 48; -fx-min-height: 48; -fx-alignment: center; -fx-background-color: #0a1f15; -fx-background-radius: 50;");
            titleLabel.setText("Spot available!");
            subtitleLabel.setText("A spot opened for your waitlisted visit — confirm within 1 hour");
            timerBox.setVisible(true);
            timerBox.setManaged(true);
            timerLabel.setText("You have 1 hour to confirm before it goes to the next person");
            confirmBtn.setText("✓  Confirm spot");
        }
    }

    @FXML
    private void handleConfirm() {
        markAsRead();
        // For reminder_expired: just acknowledge (no confirm needed — order already expired)
        if (!"reminder_expired".equals(notificationType)) {
            ClientUI.client.sendMessage(new ClientServerMessage(Command.CONFIRM_ORDER, orderId));
        }
        closePopup();
        TravelerOrdersController.refreshIfVisible();
    }

    @FXML
    private void handleLater() {
        // Mark as read so polling won't show same popup again.
        // Order stays unchanged — user can still confirm from My Orders.
        markAsRead();
        closePopup();
        // Refresh My Orders if open so "Reserved" label shows correctly
        TravelerOrdersController.refreshIfVisible();
    }

    private void markAsRead() {
        try {
            ClientUI.client.sendMessage(new ClientServerMessage(Command.MARK_NOTIFICATION_READ, notificationId));
        } catch (Exception e) {
            System.out.println("[NotificationPopup] Could not mark notification as read: " + e.getMessage());
        }
    }

    private void closePopup() {
        Stage stage = (Stage) confirmBtn.getScene().getWindow();
        stage.close();
    }
}
