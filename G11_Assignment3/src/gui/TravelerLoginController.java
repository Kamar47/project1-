package gui;

import client.*;
import common.*;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;

public class TravelerLoginController implements ClientMessageHandler {
    @FXML private TextField idField;
    @FXML private Label errorLabel;
    private static Traveler loggedInTraveler;

    @FXML
    private void handleLogin() {
        String id = idField.getText().trim();
        if (id.isEmpty()) { errorLabel.setText("Please enter your ID number."); return; }
        if (!id.matches("\\d+")) { errorLabel.setText("ID must contain only numbers."); return; }
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
}
