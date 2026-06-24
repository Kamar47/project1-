package gui;

import client.ClientMessageHandler;
import client.ClientUI;
import common.ClientServerMessage;
import common.Command;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

public class RegisterExistingAsGuideController implements ClientMessageHandler {
    @FXML private TextField idField;
    @FXML private Label statusLabel;

    @FXML
    private void handleSubmit() {
        String id = idField.getText().trim();
        if (id.isEmpty()) { statusLabel.setText("Please enter an ID."); statusLabel.setStyle("-fx-text-fill: #e94560;"); return; }
        ClientUI.client.setHandler(this);
        ClientUI.client.sendMessage(new ClientServerMessage(Command.REGISTER_EXISTING_AS_GUIDE, id));
    }

    @Override
    public void handleMessage(ClientServerMessage msg) {
        Platform.runLater(() -> {
            if (msg.getCommand() == Command.SUCCESS) {
                statusLabel.setText("Traveler registered as guide successfully!");
                statusLabel.setStyle("-fx-text-fill: #00e676;"); idField.clear();
            } else { statusLabel.setText("Error: " + msg.getData()); statusLabel.setStyle("-fx-text-fill: #e94560;"); }
        });
    }
    @Override public void onDisconnected(String r) { Platform.runLater(() -> statusLabel.setText(r)); }
}
