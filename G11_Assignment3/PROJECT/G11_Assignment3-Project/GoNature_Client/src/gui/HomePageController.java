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

/**
 * JavaFX controller for the GoNature home page (HomePageFrame.fxml).
 * <p>
 * This is the first screen displayed when the application starts.
 * It allows the user to enter the server IP address and connect.
 * After a successful connection, the user can choose to log in as
 * a traveler or an employee.
 * </p>
 *
 * @author Group 11
 */
public class HomePageController implements ClientMessageHandler {
    @FXML private TextField serverHostField, serverPortField;
    @FXML private Button connectBtn, travelerLoginBtn, workerLoginBtn;
    @FXML private Label connectionStatus;
    
    /**
     * Initializes the home page and disables the login buttons until a server connection is established.
     */
    @FXML
    private void initialize() {
        travelerLoginBtn.setDisable(true);
        workerLoginBtn.setDisable(true);
    }
    /**
     * Handles the connection request to the server.
     * The method reads the host and port fields, opens the client connection,
     * and enables the login buttons if the connection succeeds.
     */
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

    /**
     * Opens the traveler login screen if the client is connected to the server.
     * If the server is disconnected, the home page is returned to the disconnected state.
     */
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

    /**
     * Opens the worker login screen if the client is connected to the server.
     * If the server is disconnected, the home page is returned to the disconnected state.
     */
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
    /**
     * Updates the home page after a server disconnection or failed connection.
     * The method disables login actions and enables the connection fields again.
     *
     * @param message the disconnection or error message to display
     */
    private void setDisconnectedState(String message) {
        connectionStatus.setText(message);
        connectionStatus.setStyle("-fx-text-fill: #e94560;");

        travelerLoginBtn.setDisable(true);
        workerLoginBtn.setDisable(true);

        connectBtn.setDisable(false);
        serverHostField.setDisable(false);
        serverPortField.setDisable(false);
    }

    /**
     * Handles messages received from the server.
     * The home page does not process regular server responses.
     *
     * @param msg the message received from the server
     */
    @Override
    public void handleMessage(ClientServerMessage msg) {
        // Home page does not need to handle normal server messages
    }

    /**
     * Handles server disconnection by updating the home page state on the JavaFX thread.
     *
     * @param reason the reason for the disconnection
     */
    @Override
    public void onDisconnected(String reason) {
        Platform.runLater(() -> setDisconnectedState(reason));
    }
    /**
     * Exits the client application.
     */
    @FXML
    private void handleExit() { Platform.exit(); System.exit(0); }
}
