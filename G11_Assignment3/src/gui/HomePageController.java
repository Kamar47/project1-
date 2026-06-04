package gui;

import client.ClientUI;
import client.NavigationManager;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.application.Platform;

public class HomePageController {
    @FXML private TextField serverHostField, serverPortField;
    @FXML private Button connectBtn, travelerLoginBtn, workerLoginBtn;
    @FXML private Label connectionStatus;

    @FXML
    private void handleConnect() {
        try {
            String host = serverHostField.getText().trim();
            int port = Integer.parseInt(serverPortField.getText().trim());
            ClientUI.connectToServer(host, port);
            connectionStatus.setText("Connected to server!");
            connectionStatus.setStyle("-fx-text-fill: #00e676;");
            travelerLoginBtn.setDisable(false);
            workerLoginBtn.setDisable(false);
            connectBtn.setDisable(true);
            serverHostField.setDisable(true);
            serverPortField.setDisable(true);
        } catch (Exception e) {
            connectionStatus.setText("Failed to connect: " + e.getMessage());
            connectionStatus.setStyle("-fx-text-fill: #e94560;");
        }
    }

    @FXML
    private void handleTravelerLogin() {
        try { NavigationManager.openPage("TravelerLoginFrame.fxml", null, "Traveler Login", false); }
        catch (Exception e) { e.printStackTrace(); }
    }

    @FXML
    private void handleWorkerLogin() {
        try { NavigationManager.openPage("WorkerLoginFrame.fxml", null, "Worker Login", false); }
        catch (Exception e) { e.printStackTrace(); }
    }

    @FXML
    private void handleExit() { Platform.exit(); System.exit(0); }
}
