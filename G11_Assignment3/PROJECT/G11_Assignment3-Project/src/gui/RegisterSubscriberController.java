package gui;

import client.ClientMessageHandler;
import client.ClientUI;
import common.ClientServerMessage;
import common.Command;
import common.Subscriber;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

public class RegisterSubscriberController implements ClientMessageHandler {
    @FXML private TextField idField, firstNameField, lastNameField, phoneField, emailField, familyField, creditCardField;
    @FXML private Label statusLabel;

    @FXML
    private void handleRegister() {
        if (idField.getText().isEmpty() || firstNameField.getText().isEmpty() || lastNameField.getText().isEmpty()
                || phoneField.getText().isEmpty() || emailField.getText().isEmpty() || familyField.getText().isEmpty()) {
            statusLabel.setText("Please fill in all required fields."); statusLabel.setStyle("-fx-text-fill: #e94560;"); return;
        }
        Subscriber sub = new Subscriber();
        sub.setIdNumber(idField.getText().trim()); sub.setFirstName(firstNameField.getText().trim());
        sub.setLastName(lastNameField.getText().trim()); sub.setPhone(phoneField.getText().trim());
        sub.setEmail(emailField.getText().trim()); sub.setFamilyMembers(Integer.parseInt(familyField.getText().trim()));
        sub.setCreditCard(creditCardField.getText().trim());
        ClientUI.client.setHandler(this);
        ClientUI.client.sendMessage(new ClientServerMessage(Command.REGISTER_SUBSCRIBER, sub));
    }

    @Override
    public void handleMessage(ClientServerMessage msg) {
        Platform.runLater(() -> {
            if (msg.getCommand() == Command.SUCCESS) {
                Subscriber s = (Subscriber) msg.getData();
                statusLabel.setText("Subscriber registered! ID: " + s.getSubscriberId());
                statusLabel.setStyle("-fx-text-fill: #00e676;");
            } else { statusLabel.setText("Error: " + msg.getData()); statusLabel.setStyle("-fx-text-fill: #e94560;"); }
        });
    }
    @Override public void onDisconnected(String r) { Platform.runLater(() -> statusLabel.setText(r)); }
}
