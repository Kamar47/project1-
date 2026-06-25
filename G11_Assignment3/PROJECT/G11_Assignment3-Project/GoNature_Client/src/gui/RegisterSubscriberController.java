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

/**
 * JavaFX controller for the Register Subscriber screen (RegisterSubscriber.fxml),
 * accessible to service representatives.
 * <p>
 * Allows the service representative to register a new family-club subscriber
 * Required fields: national ID, first name, last name, phone, email,
 * number of family members. Credit card is optional.
 * </p>
 * <p>
 * On successful registration the server returns the assigned unique member number
 * ({@code subscriber_id}) which is displayed as confirmation.
 * </p>
 *
 * @author Group 11
 */
public class RegisterSubscriberController implements ClientMessageHandler {
    @FXML private TextField idField, firstNameField, lastNameField, phoneField, emailField, familyField, creditCardField;
    @FXML private Label statusLabel;
    private String pendingAction = "REGISTER";


    /**
     * Handles registration of a new family-club subscriber.
     * The method validates the subscriber details, builds a subscriber object,
     * and sends a registration request to the server.
     */
    @FXML
    private void handleRegister() {
        if (idField.getText().isEmpty() || firstNameField.getText().isEmpty() || lastNameField.getText().isEmpty()
                || phoneField.getText().isEmpty() || emailField.getText().isEmpty() || familyField.getText().isEmpty()) {
            statusLabel.setText("Please fill in all required fields."); statusLabel.setStyle("-fx-text-fill: #f87171;"); return;
        }
        String err;
        if ((err = client.InputValidation.validateId(idField.getText())) != null) { statusLabel.setText(err); statusLabel.setStyle("-fx-text-fill: #f87171;"); return; }
        if ((err = client.InputValidation.validateEmail(emailField.getText())) != null) { statusLabel.setText(err); statusLabel.setStyle("-fx-text-fill: #f87171;"); return; }
        if ((err = client.InputValidation.validatePhone(phoneField.getText())) != null) { statusLabel.setText(err); statusLabel.setStyle("-fx-text-fill: #f87171;"); return; }
        int familyMembers;
        try {
            familyMembers = Integer.parseInt(familyField.getText().trim());
            if (familyMembers <= 0) { statusLabel.setText("Family members must be a positive number."); statusLabel.setStyle("-fx-text-fill: #f87171;"); return; }
        } catch (NumberFormatException ex) {
            statusLabel.setText("Family members must be a valid number."); statusLabel.setStyle("-fx-text-fill: #f87171;"); return;
        }
        Subscriber sub = new Subscriber();
        sub.setIdNumber(idField.getText().trim()); sub.setFirstName(firstNameField.getText().trim());
        sub.setLastName(lastNameField.getText().trim()); sub.setPhone(phoneField.getText().trim());
        sub.setEmail(emailField.getText().trim()); sub.setFamilyMembers(familyMembers);
        sub.setCreditCard(creditCardField.getText().trim());
        ClientUI.client.setHandler(this);
        ClientUI.client.sendMessage(new ClientServerMessage(Command.REGISTER_SUBSCRIBER, sub));
    }

    /**
     * Handles the server response for subscriber registration.
     * If the registration succeeds, the assigned subscriber ID is displayed on the screen.
     *
     * @param msg the message received from the server
     */
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
    /**
     * Handles server disconnection by displaying the disconnection reason on the screen.
     *
     * @param r the reason for the disconnection
     */
    @Override public void onDisconnected(String r) { Platform.runLater(() -> statusLabel.setText(r)); }
}
