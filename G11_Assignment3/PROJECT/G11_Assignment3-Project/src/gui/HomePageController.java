package gui;

import client.ClientUI;
import client.ClientMessageHandler;
import common.ClientServerMessage;
import client.NavigationManager;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

public class HomePageController implements ClientMessageHandler {
    @FXML private TextField serverHostField, serverPortField;
    @FXML private Button connectBtn, travelerLoginBtn, workerLoginBtn;
    @FXML private Label connectionStatus;
    @FXML
    private void initialize() {
        travelerLoginBtn.setDisable(true);
        workerLoginBtn.setDisable(true);
    }
    @FXML
    private void handleConnect() {
        try {
            String host = serverHostField.getText().trim();
            int port = Integer.parseInt(serverPortField.getText().trim());
            ClientUI.connectToServer(host, port);
            ClientUI.client.setHandler(this);
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
        if (!ClientUI.isServerConnected()) {
            setDisconnectedState("Server is disconnected. Please connect to the server first.");
            return;
        }

        try {
            NavigationManager.openPage("TravelerLoginFrame.fxml", null, "Traveler Login", false);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleWorkerLogin() {
        if (!ClientUI.isServerConnected()) {
            setDisconnectedState("Server is disconnected. Please connect to the server first.");
            return;
        }

        try {
            NavigationManager.openPage("WorkerLoginFrame.fxml", null, "Worker Login", false);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private void setDisconnectedState(String message) {
        connectionStatus.setText(message);
        connectionStatus.setStyle("-fx-text-fill: #e94560;");

        travelerLoginBtn.setDisable(true);
        workerLoginBtn.setDisable(true);

        connectBtn.setDisable(false);
        serverHostField.setDisable(false);
        serverPortField.setDisable(false);
    }

    @Override
    public void handleMessage(ClientServerMessage msg) {
        // Home page does not need to handle normal server messages
    }

    @Override
    public void onDisconnected(String reason) {
        Platform.runLater(() -> setDisconnectedState(reason));
    }

    @FXML
    private void handleExit() { Platform.exit(); System.exit(0); }
}
