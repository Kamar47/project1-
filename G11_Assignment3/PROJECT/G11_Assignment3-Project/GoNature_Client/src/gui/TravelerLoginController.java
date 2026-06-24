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

public class TravelerLoginController implements ClientMessageHandler {
    @FXML private TextField idField;
    @FXML private Label errorLabel;
    private static Traveler loggedInTraveler;

    @FXML
    private void initialize() {
        if (ClientUI.client != null) {
            ClientUI.client.setHandler(this);
        }
    }
    
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

    @FXML
    private void handleBack() {
        idField.getScene().getWindow().hide();
    }

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

    @Override
    public void onDisconnected(String reason) {
        Platform.runLater(() -> errorLabel.setText(reason));
    }

    public static Traveler getLoggedInTraveler() { return loggedInTraveler; }
    public static void clearLoggedInTraveler() { loggedInTraveler = null; }
    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setStyle("-fx-text-fill: #e94560;");
    }
}