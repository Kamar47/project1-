package gui;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

/**
 * Simulates email/SMS notifications as popup windows.
 * As per spec: "סימולציה של המשלוח, שתכלול הצגת הודעה מתפרצת למשתמש"
 */
public class NotificationSimulator {

    public static void showNotification(String type, String recipient, String subject, String body) {
        Platform.runLater(() -> {
            Stage popup = new Stage();
            popup.initStyle(StageStyle.UTILITY);
            popup.setTitle("SIMULATION - " + type);

            Label simLabel = new Label("*** SIMULATION ***");
            simLabel.setStyle("-fx-text-fill: #e94560; -fx-font-size: 14px; -fx-font-weight: bold;");

            Label typeLabel = new Label("Type: " + type);
            typeLabel.setStyle("-fx-text-fill: #c0c0d8; -fx-font-size: 12px;");

            Label recipientLabel = new Label("To: " + recipient);
            recipientLabel.setStyle("-fx-text-fill: #f5a623; -fx-font-size: 12px;");

            Label subjectLabel = new Label("Subject: " + subject);
            subjectLabel.setStyle("-fx-text-fill: #2d6a4f; -fx-font-size: 13px; -fx-font-weight: bold;");

            Label bodyLabel = new Label(body);
            bodyLabel.setStyle("-fx-text-fill: #e0e0f0; -fx-font-size: 12px;");
            bodyLabel.setWrapText(true);
            bodyLabel.setMaxWidth(350);

            Label noteLabel = new Label("(In production, this would be sent as a real " + type + ")");
            noteLabel.setStyle("-fx-text-fill: #606080; -fx-font-size: 10px; -fx-font-style: italic;");

            VBox layout = new VBox(8, simLabel, typeLabel, recipientLabel, subjectLabel, bodyLabel, noteLabel);
            layout.setPadding(new Insets(15));
            layout.setStyle("-fx-background-color: #1a1a2e; -fx-border-color: #2d6a4f; -fx-border-width: 2; -fx-border-radius: 8; -fx-background-radius: 8;");

            popup.setScene(new Scene(layout, 400, 250));
            popup.setAlwaysOnTop(true);
            popup.show();

            // Auto-close after 8 seconds
            new Thread(() -> {
                try { Thread.sleep(8000); } catch (InterruptedException e) {}
                Platform.runLater(popup::close);
            }).start();
        });
    }

    public static void simulateBookingConfirmation(String email, String phone, String confirmationCode, String parkName, String date, String time) {
        String body = "Your visit to " + parkName + " on " + date + " at " + time + " has been confirmed!\n" +
                "Confirmation code: " + confirmationCode + "\nPlease keep this code for entry.";
        showNotification("Email", email, "GoNature - Booking Confirmed", body);
        if (phone != null && !phone.isEmpty()) {
            showNotification("SMS", phone, "GoNature Booking", "Visit to " + parkName + " on " + date + " confirmed. Code: " + confirmationCode);
        }
    }

    public static void simulateCancellation(String email, String phone, String parkName, String date) {
        String body = "Your visit to " + parkName + " on " + date + " has been cancelled.";
        showNotification("Email", email, "GoNature - Booking Cancelled", body);
    }

    public static void simulateReminder(String email, String phone, String parkName, String date, String time) {
        String body = "Reminder: Your visit to " + parkName + " is tomorrow (" + date + ") at " + time + ".\n" +
                "Please confirm or cancel within 2 hours, or the booking will be cancelled automatically.";
        showNotification("Email", email, "GoNature - Visit Reminder", body);
        if (phone != null && !phone.isEmpty()) {
            showNotification("SMS", phone, "GoNature Reminder", "Visit to " + parkName + " tomorrow. Confirm within 2hrs or auto-cancel.");
        }
    }

    public static void simulateWaitlistAvailable(String email, String phone, String parkName, String date) {
        String body = "Good news! A spot has opened up at " + parkName + " on " + date + ".\n" +
                "You have 1 hour to confirm your booking, or it will pass to the next person in line.";
        showNotification("Email", email, "GoNature - Spot Available!", body);
        if (phone != null && !phone.isEmpty()) {
            showNotification("SMS", phone, "GoNature", "Spot available at " + parkName + " on " + date + ". Confirm within 1hr!");
        }
    }

    public static void simulateAutoCancel(String email, String phone, String parkName, String date) {
        String body = "Your booking at " + parkName + " on " + date + " has been automatically cancelled " +
                "because you did not confirm the reminder within 2 hours.";
        showNotification("Email", email, "GoNature - Booking Auto-Cancelled", body);
    }
}
