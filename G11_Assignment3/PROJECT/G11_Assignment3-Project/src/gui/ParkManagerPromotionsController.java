package gui;

import java.util.ArrayList;

import client.ClientMessageHandler;
import client.ClientUI;
import common.ClientServerMessage;
import common.Command;
import common.worker.GeneralParkWorker;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

public class ParkManagerPromotionsController implements ClientMessageHandler {
    @FXML private TextField discountField, descriptionField;
    @FXML private DatePicker startDatePicker, endDatePicker;
    @FXML private Label statusLabel;

    @FXML
    private void handleSubmit() {
        if (discountField.getText().isEmpty() || startDatePicker.getValue() == null || endDatePicker.getValue() == null) {
            statusLabel.setText("Please fill in all fields."); statusLabel.setStyle("-fx-text-fill: #e94560;"); return;
        }
        GeneralParkWorker w = WorkerLoginController.getLoggedInWorker();
        ArrayList<Object> data = new ArrayList<>();
        data.add(w.getParkId());
        data.add(Double.parseDouble(discountField.getText().trim()));
        data.add(startDatePicker.getValue().toString());
        data.add(endDatePicker.getValue().toString());
        data.add(descriptionField.getText().trim());
        data.add(w.getEmployeeId());
        ClientUI.client.setHandler(this);
        ClientUI.client.sendMessage(new ClientServerMessage(Command.CREATE_PROMOTION, data));
    }

    @Override
    public void handleMessage(ClientServerMessage msg) {
        Platform.runLater(() -> {
            if (msg.getCommand() == Command.SUCCESS) {
                statusLabel.setText("Promotion submitted! Awaiting department manager approval.");
                statusLabel.setStyle("-fx-text-fill: #00e676;");
                discountField.clear(); descriptionField.clear();
                startDatePicker.setValue(null); endDatePicker.setValue(null);
            } else { statusLabel.setText("Error: " + msg.getData()); statusLabel.setStyle("-fx-text-fill: #e94560;"); }
        });
    }
    @Override public void onDisconnected(String r) { Platform.runLater(() -> statusLabel.setText(r)); }
}
