package gui;

import common.Order;
import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

/**
 * JavaFX controller for the Order Confirmation screen (OrderConfirmation.fxml).
 * <p>
 * Displayed after a successful booking. Shows the order details (park, date, time,
 * visitors, total price) along with the unique confirmation code and a QR code
 * generated from that code using {@link QRCodeGenerator}.
 * The traveler presents this QR code at the park entrance.
 * </p>
 *
 * @author Group 11
 */
public class OrderConfirmationController {
    @FXML private Label dateLabel, timeLabel, parkLabel, typeLabel, visitorsLabel,
                        statusLabel, priceLabel, orderNumberLabel, confirmationCodeLabel;
    @FXML private StackPane qrPane;

    /**
     * Displays the confirmed order details on the confirmation screen.
     * The method fills the order information labels and generates a QR code
     * from the order confirmation code.
     *
     * @param order the confirmed order to display
     */
    public void setOrder(Order order) {
        dateLabel.setText(order.getVisitDate());
        timeLabel.setText(order.getVisitTime());
        parkLabel.setText(order.getParkName() != null ? order.getParkName() : "Park #" + order.getParkId());
        typeLabel.setText(formatType(order.getOrderType()));
        visitorsLabel.setText(String.valueOf(order.getNumVisitors()));
        statusLabel.setText(order.getStatus() != null ? order.getStatus().toUpperCase() : "PENDING");
        String code = order.getConfirmationCode() != null ? order.getConfirmationCode() : "--";
        confirmationCodeLabel.setText(code);
        priceLabel.setText(order.getTotalPrice() + " NIS");
        orderNumberLabel.setText(String.valueOf(order.getOrderId()));

        // Generate QR code display from confirmation code
        if (qrPane != null && order.getConfirmationCode() != null) {
            try {
                Canvas qr = QRCodeGenerator.generateQR(order.getConfirmationCode(), 150);
                qrPane.getChildren().clear();
                qrPane.getChildren().add(qr);
            } catch (Exception e) {
                System.err.println("[QR] Failed to generate QR: " + e.getMessage());
            }
        }
    }

    /**
     * Converts the internal order type value into a user-friendly display label.
     *
     * @param type the internal order type value
     * @return the formatted order type label
     */
    private String formatType(String type) {
        if (type == null) return "";
        switch (type) {
            case "individual":     return "Individual";
            case "family":         return "Family";
            case "organized_group":return "Organized Group";
            case "walk_in":        return "Walk-in";
            case "walk_in_group":  return "Walk-in Group";
            default:               return type;
        }
    }

    /**
     * Closes the order confirmation window and returns the traveler to the main frame.
     */
    @FXML
    private void handleHome() {
        Stage stage = (Stage) orderNumberLabel.getScene().getWindow();
        stage.close();
    }
}
