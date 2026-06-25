package gui;

import client.ClientMessageHandler;
import client.ClientUI;
import client.InputValidation;
import client.NavigationManager;
import common.ClientServerMessage;
import javafx.application.Platform;
import common.ClientServerMessage;
import common.Command;
import common.Traveler;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

/**
 * JavaFX controller for the traveler login screen.
 * <p>
 * The traveler identifies themselves by entering their national ID number.
 * The server validates the ID, retrieves any existing bookings, and returns
 * a {@link common.Traveler} object. If the same ID is already logged in
 * on another machine, the login is rejected.
 * </p>
 * <p>
 * The logged-in traveler object is stored as a static reference via
 * {@link #getLoggedInTraveler()} for use by other controllers in the same session.
 * </p>
 *
 * @author Group 11
 */
public class TravelerLoginController implements ClientMessageHandler {
    @FXML private TextField idField;
    @FXML private Label errorLabel;
    private static Traveler loggedInTraveler;

    /**
     * Initializes the traveler login screen and registers this controller
     * as the current client message handler.
     */
    @FXML
    private void initialize() {
        if (ClientUI.client != null) {
            ClientUI.client.setHandler(this);
        }
    }
    /**
     * Handles traveler login by validating the entered ID number
     * and sending a login request to the server.
     */
    @FXML
    private void handleLogin() {
    	if (!ClientUI.isServerConnected()) {
    	    showError("Server is disconnected. Please reconnect from the home page.");
    	    return;
    	}
        String id = idField.getText().trim();
        if (id.isEmpty()) { errorLabel.setText("Please enter your ID number."); return; }
        if (!id.matches("\\d+")) { errorLabel.setText("ID must contain only numbers."); return; }
        if (id.length() > 9) { errorLabel.setText("ID number must be up to 9 digits."); return; }
        if (!InputValidation.isValidIsraeliId(id)) { errorLabel.setText("Please Enter valid ID number."); return; }
        ClientUI.client.setHandler(this);
        ClientUI.client.sendMessage(new ClientServerMessage(Command.TRAVELER_LOGIN, id));
    }

    /**
     * Closes the traveler login window and returns to the previous screen.
     */
    @FXML
    private void handleBack() {
        idField.getScene().getWindow().hide();
    }

    /**
     * Handles the server response for traveler login.
     * If the login succeeds, the traveler frame is opened and the logged-in traveler is stored.
     *
     * @param msg the message received from the server
     */
    @Override
    public void handleMessage(ClientServerMessage msg) {
        Platform.runLater(() -> {
            if (msg.getCommand() == Command.SUCCESS) {
                loggedInTraveler = (Traveler) msg.getData();
                loggedInTraveler.setIdNumber(idField.getText().trim());
                try {
                    NavigationManager.openPage("TravelerFrame.fxml", null, "GoNature - Traveler", false);
                    idField.getScene().getWindow().hide();
                } catch (Exception e) { e.printStackTrace(); }
            } else {
                errorLabel.setText("Login failed.");
            }
        });
    }

    /**
     * Handles server disconnection by displaying the disconnection reason on the screen.
     *
     * @param reason the reason for the disconnection
     */
    @Override
    public void onDisconnected(String reason) {
        Platform.runLater(() -> errorLabel.setText(reason));
    }

    /**
     * Returns the traveler currently logged in during this client session.
     *
     * @return the logged-in traveler, or null if no traveler is logged in
     */
    public static Traveler getLoggedInTraveler() { return loggedInTraveler; }
    /**
     * Clears the stored logged-in traveler after logout or session reset.
     */
    public static void clearLoggedInTraveler() { loggedInTraveler = null; }
    /**
     * Displays an error message on the traveler login screen.
     *
     * @param message the error message to display
     */
    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setStyle("-fx-text-fill: #e94560;");
    }
}