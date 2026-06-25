package gui;

import java.util.ArrayList;

import client.ClientMessageHandler;
import client.ClientUI;
import common.ClientServerMessage;
import common.Command;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

/**
 * JavaFX controller for the Register New Guide screen (RegisterGuide.fxml).
 * <p>
 * Allows service representatives to register a new tour guide in the system.
 * The guide's national ID, name, email, and phone are stored in the {@code guides} table.
 * Registered guides may book organized group visits (up to 15 visitors) and
 * enter the park free of charge.
 * </p>
 *
 * @author Group 11
 */
public class RegisterGuideController implements ClientMessageHandler {
    @FXML private TextField idField, firstNameField, lastNameField, emailField, phoneField;
    @FXML private Label statusLabel;

    /**
     * Handles registration of a new tour guide.
     * The method validates the guide details and sends a registration request to the server.
     */
    @FXML
    private void handleRegister() {
        if (idField.getText().isEmpty() || firstNameField.getText().isEmpty() || lastNameField.getText().isEmpty() || emailField.getText().isEmpty()) {
            statusLabel.setText("Please fill in all required fields."); statusLabel.setStyle("-fx-text-fill: #f87171;"); return;
        }
        String err;
        if ((err = client.InputValidation.validateId(idField.getText())) != null) { statusLabel.setText(err); statusLabel.setStyle("-fx-text-fill: #f87171;"); return; }
        if ((err = client.InputValidation.validateEmail(emailField.getText())) != null) { statusLabel.setText(err); statusLabel.setStyle("-fx-text-fill: #f87171;"); return; }
        if ((err = client.InputValidation.validatePhone(phoneField.getText())) != null) { statusLabel.setText(err); statusLabel.setStyle("-fx-text-fill: #f87171;"); return; }
        ArrayList<String> data = new ArrayList<>();
        data.add(idField.getText().trim()); data.add(firstNameField.getText().trim());
        data.add(lastNameField.getText().trim()); data.add(emailField.getText().trim());
        data.add(phoneField.getText().trim());
        ClientUI.client.setHandler(this);
        ClientUI.client.sendMessage(new ClientServerMessage(Command.REGISTER_GUIDE, data));
    }

    /**
     * Handles the server response for guide registration.
     * If the registration succeeds, the input fields are cleared and a success message is displayed.
     *
     * @param msg the message received from the server
     */
    @Override
    public void handleMessage(ClientServerMessage msg) {
        Platform.runLater(() -> {
            if (msg.getCommand() == Command.SUCCESS) {
                statusLabel.setText("Guide registered successfully!"); statusLabel.setStyle("-fx-text-fill: #00e676;");
                idField.clear(); firstNameField.clear(); lastNameField.clear(); emailField.clear(); phoneField.clear();
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
