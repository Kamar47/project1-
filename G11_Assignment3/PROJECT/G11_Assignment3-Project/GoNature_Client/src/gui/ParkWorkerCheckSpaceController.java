package gui;

import java.net.URL;
import java.util.ResourceBundle;

import client.ClientMessageHandler;
import client.ClientUI;
import common.ClientServerMessage;
import common.Command;
import common.Park;
import common.worker.GeneralParkWorker;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;

/**
 * JavaFX controller for the Check Available Space screen (ParkWorkerCheckSpace.fxml).
 * <p>
 * Allows park workers to view the current visitor count and available capacity
 * for their assigned park in real time.
 * </p>
 *
 * @author Group 11
 */
public class ParkWorkerCheckSpaceController implements Initializable, ClientMessageHandler {
    @FXML private Label parkNameLabel, maxVisitorsLabel, currentVisitorsLabel;
    @FXML private Label availableSpotsLabel, gapLabel, statusLabel;

    /**
     * Initializes the park worker available space screen.
     * The method loads the current capacity information for the worker's assigned park.
     *
     * @param url the location used to resolve relative paths
     * @param rb the resources used to localize the screen
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) { loadParkInfo(); }

    /**
     * Requests the current park capacity details from the server.
     * If the server is disconnected, an error message is displayed.
     */
    @FXML
    public void loadParkInfo() {
    	if (!ClientUI.isServerConnected()) {
            showStatus("Server is disconnected.", true);
            return;
        }
        GeneralParkWorker w = WorkerLoginController.getLoggedInWorker();
        if (w != null) {
            ClientUI.client.setHandler(this);
            ClientUI.client.sendMessage(new ClientServerMessage(Command.GET_PARK_DETAILS, w.getParkId()));
        }
    }

    /**
     * Handles the server response containing the current park capacity details.
     * The method updates the visitor count, available spots, walk-in gap,
     * and availability status labels.
     *
     * @param msg the message received from the server
     */
    @Override
    public void handleMessage(ClientServerMessage msg) {
        Platform.runLater(() -> {
            if (msg.getCommand() == Command.DATA_RESPONSE && msg.getData() instanceof Park) {
                Park park = (Park) msg.getData();
                parkNameLabel.setText(park.getParkName());
                maxVisitorsLabel.setText(String.valueOf(park.getMaxVisitors()));
                currentVisitorsLabel.setText(String.valueOf(park.getCurrentVisitors()));
                int available = park.getAvailableSpots();
                availableSpotsLabel.setText(String.valueOf(available));
                gapLabel.setText(String.valueOf(park.getGapForWalkins()));
                if (available > 0) {
                    statusLabel.setText("Open - Space available");
                    statusLabel.setStyle("-fx-text-fill: #00e676; -fx-font-weight: bold;");
                } else {
                    statusLabel.setText("Full - No space available");
                    statusLabel.setStyle("-fx-text-fill: #e94560; -fx-font-weight: bold;");
                }
            }
        });
    }
    /**
     * Displays a status message on the available space screen.
     * The message color is changed according to whether it represents an error or success.
     *
     * @param msg the message to display
     * @param error true if the message represents an error, otherwise false
     */
    private void showStatus(String msg, boolean error) {
        statusLabel.setText(msg);
        statusLabel.setStyle("-fx-text-fill: " + (error ? "#e94560" : "#00e676") + ";");
    }

    /**
     * Handles server disconnection by displaying an error message on the screen.
     *
     * @param reason the reason for the disconnection
     */
    @Override
    public void onDisconnected(String reason) {Platform.runLater(() -> showStatus("Server is disconnected. Cannot load available space.", true));}
}
