package gui;

import common.Order;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.stage.Stage;

public class OrderConfirmationController {
    @FXML private Label dateLabel, timeLabel, parkLabel, typeLabel, visitorsLabel, statusLabel, priceLabel, orderNumberLabel;

    /** Called after FXML load to fill in the order details */
    public void setOrder(Order order) {
        dateLabel.setText(order.getVisitDate());
        timeLabel.setText(order.getVisitTime());
        parkLabel.setText(order.getParkName() != null ? order.getParkName() : "Park #" + order.getParkId());
        typeLabel.setText(formatType(order.getOrderType()));
        visitorsLabel.setText(String.valueOf(order.getNumVisitors()));
        statusLabel.setText(order.getStatus() != null ? order.getStatus().toUpperCase() : "PENDING");
        priceLabel.setText(order.getTotalPrice() + " NIS");
        orderNumberLabel.setText(String.valueOf(order.getOrderId()));
    }

    private String formatType(String type) {
        if (type == null) return "";
        switch (type) {
            case "individual": return "Individual";
            case "family": return "Family";
            case "organized_group": return "Organized Group";
            case "walk_in": return "Walk-in";
            case "walk_in_group": return "Walk-in Group";
            default: return type;
        }
    }

    @FXML
    private void handleHome() {
        Stage stage = (Stage) orderNumberLabel.getScene().getWindow();
        stage.close();
    }
}