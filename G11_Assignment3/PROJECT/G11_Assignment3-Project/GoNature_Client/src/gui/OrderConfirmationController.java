package gui;

import common.Order;
import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

public class OrderConfirmationController {
    @FXML private Label dateLabel, timeLabel, parkLabel, typeLabel, visitorsLabel,
                        statusLabel, priceLabel, orderNumberLabel, confirmationCodeLabel;
    @FXML private StackPane qrPane;

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

    @FXML
    private void handleHome() {
        Stage stage = (Stage) orderNumberLabel.getScene().getWindow();
        stage.close();
    }
}
