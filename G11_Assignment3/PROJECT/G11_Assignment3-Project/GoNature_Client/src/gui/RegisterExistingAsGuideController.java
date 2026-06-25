package gui;

import client.ClientMessageHandler;
import client.ClientUI;
import common.ClientServerMessage;
import common.Command;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

/**
 * JavaFX controller for the Register Existing Subscriber as Guide screen
 * (RegisterExistingAsGuide.fxml).
 * <p>
 * Allows service representatives to register an existing subscriber as a tour guide.
 * The subscriber's information is retrieved from the {@code subscribers} table and
 * a corresponding entry is created in the {@code guides} table.
 * </p>
 *
 * @author Group 11
 */
public class RegisterExistingAsGuideController implements ClientMessageHandler {
    @FXML private TextField idField;
    @FXML private Label statusLabel;

    /**
     * Handles registration of an existing subscriber as a tour guide.
     * The method validates that an ID was entered and sends the registration request to the server.
     */
    @FXML
    private void handleSubmit() {
        String id = idField.getText().trim();
        if (id.isEmpty()) { statusLabel.setText("Please enter an ID."); statusLabel.setStyle("-fx-text-fill: #e94560;"); return; }
        ClientUI.client.setHandler(this);
        ClientUI.client.sendMessage(new ClientServerMessage(Command.REGISTER_EXISTING_AS_GUIDE, id));
    }

    /**
     * Handles the server response for registering an existing subscriber as a guide.
     * If the registration succeeds, the ID field is cleared and a success message is displayed.
     *
     * @param msg the message received from the server
     */
    @Override
    public void handleMessage(ClientServerMessage msg) {
        Platform.runLater(() -> {
            if (msg.getCommand() == Command.SUCCESS) {
                statusLabel.setText("Traveler registered as guide successfully!");
                statusLabel.setStyle("-fx-text-fill: #00e676;"); idField.clear();
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
