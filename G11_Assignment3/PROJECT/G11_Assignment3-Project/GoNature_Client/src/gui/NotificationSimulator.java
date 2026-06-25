package gui;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

/**
 * Utility class for simulating email and SMS notifications in the GoNature client.
 * <p>
 * The project does not require real email or SMS sending. Instead, required messages
 * are displayed as popup windows that include the notification type, recipient,
 * subject, and message body.
 * </p>
 */
public class NotificationSimulator {

	/**
	 * Displays a simulated email or SMS notification as a popup window.
	 * The popup represents the notification that would be sent to the user in a real system.
	 *
	 * @param type the notification type, such as Email or SMS
	 * @param recipient the email address or phone number of the recipient
	 * @param subject the notification subject
	 * @param body the notification message body
	 */
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

    /**
     * Simulates sending a booking confirmation notification to the traveler.
     * The notification includes the park name, visit date and time, and confirmation code.
     *
     * @param email the traveler email address
     * @param phone the traveler phone number
     * @param confirmationCode the order confirmation code
     * @param parkName the park name
     * @param date the visit date
     * @param time the visit time
     */
    public static void simulateBookingConfirmation(String email, String phone, String confirmationCode, String parkName, String date, String time) {
        String body = "Your visit to " + parkName + " on " + date + " at " + time + " has been confirmed!\n" +
                "Confirmation code: " + confirmationCode + "\nPlease keep this code for entry.";
        showNotification("Email", email, "GoNature - Booking Confirmed", body);
        if (phone != null && !phone.isEmpty()) {
            showNotification("SMS", phone, "GoNature Booking", "Visit to " + parkName + " on " + date + " confirmed. Code: " + confirmationCode);
        }
    }

    /**
     * Simulates sending a booking cancellation notification to the traveler.
     *
     * @param email the traveler email address
     * @param phone the traveler phone number
     * @param parkName the park name
     * @param date the cancelled visit date
     */
    public static void simulateCancellation(String email, String phone, String parkName, String date) {
        String body = "Your visit to " + parkName + " on " + date + " has been cancelled.";
        showNotification("Email", email, "GoNature - Booking Cancelled", body);
    }

    /**
     * Simulates sending a visit reminder notification to the traveler.
     * The reminder asks the traveler to confirm or cancel the booking within two hours.
     *
     * @param email the traveler email address
     * @param phone the traveler phone number
     * @param parkName the park name
     * @param date the visit date
     * @param time the visit time
     */
    public static void simulateReminder(String email, String phone, String parkName, String date, String time) {
        String body = "Reminder: Your visit to " + parkName + " is tomorrow (" + date + ") at " + time + ".\n" +
                "Please confirm or cancel within 2 hours, or the booking will be cancelled automatically.";
        showNotification("Email", email, "GoNature - Visit Reminder", body);
        if (phone != null && !phone.isEmpty()) {
            showNotification("SMS", phone, "GoNature Reminder", "Visit to " + parkName + " tomorrow. Confirm within 2hrs or auto-cancel.");
        }
    }

    /**
     * Simulates sending a waitlist availability notification to the traveler.
     * The notification informs the traveler that a spot is available and must be confirmed within one hour.
     *
     * @param email the traveler email address
     * @param phone the traveler phone number
     * @param parkName the park name
     * @param date the visit date
     */
    public static void simulateWaitlistAvailable(String email, String phone, String parkName, String date) {
        String body = "Good news! A spot has opened up at " + parkName + " on " + date + ".\n" +
                "You have 1 hour to confirm your booking, or it will pass to the next person in line.";
        showNotification("Email", email, "GoNature - Spot Available!", body);
        if (phone != null && !phone.isEmpty()) {
            showNotification("SMS", phone, "GoNature", "Spot available at " + parkName + " on " + date + ". Confirm within 1hr!");
        }
    }

    /**
     * Simulates sending an automatic cancellation notification to the traveler.
     * This is used when a booking is cancelled because the traveler did not confirm the reminder in time.
     *
     * @param email the traveler email address
     * @param phone the traveler phone number
     * @param parkName the park name
     * @param date the cancelled visit date
     */
    public static void simulateAutoCancel(String email, String phone, String parkName, String date) {
        String body = "Your booking at " + parkName + " on " + date + " has been automatically cancelled " +
                "because you did not confirm the reminder within 2 hours.";
        showNotification("Email", email, "GoNature - Booking Auto-Cancelled", body);
    }
}
