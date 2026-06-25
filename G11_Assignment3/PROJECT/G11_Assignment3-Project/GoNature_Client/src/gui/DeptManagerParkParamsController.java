package gui;

import java.net.URL;
import java.util.ArrayList;
import java.util.ResourceBundle;

import client.ClientMessageHandler;
import client.ClientUI;
import common.ClientServerMessage;
import common.Command;
import common.Park;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

/**
 * JavaFX controller for the Department Manager Park Parameters screen
 * (DeptManagerParkParams.fxml).
 * <p>
 * Allows the department manager to view the current parameters of all parks
 * in the system (maximum visitors, walk-in gap, estimated visit duration).
 * Parameter changes are initiated by park managers and approved here.
 * </p>
 *
 * @author Group 11
 */
public class DeptManagerParkParamsController implements Initializable, ClientMessageHandler {
    @FXML private ComboBox<String> parkCombo;
    @FXML private VBox paramsBox;
    @FXML private Label nameLabel, currentLabel, maxLabel, gapLabel, capacityLabel, stayLabel, statusLabel;
    private ArrayList<Park> parks;
    private String currentAction;

    /**
     * Initializes the department manager park parameters screen.
     * The method checks the server connection and loads the list of parks from the server.
     *
     * @param url the location used to resolve relative paths
     * @param rb the resources used to localize the screen
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        if (!ClientUI.isServerConnected()) {
            showStatus("Server disconnected. Cannot load park parameters.", true);
            return;
        }
        currentAction = "LOAD_PARKS";
        ClientUI.client.setHandler(this);
        ClientUI.client.sendMessage(new ClientServerMessage(Command.GET_ALL_PARKS));
    }

    /**
     * Loads and displays the parameter details for the selected park.
     * If no park is selected or the server is disconnected, an error message is shown.
     */
    @FXML
    private void handleGetParams() {
        if (!ClientUI.isServerConnected()) {
            showStatus("Server disconnected. Cannot load park details.", true);
            return;
        }
        if (parkCombo.getValue() == null) {
            showStatus("Select a park.", true);
            return;
        }
        if (parks == null || parks.isEmpty()) {
            showStatus("Park list is not loaded. Please reconnect to the server.", true);
            return;
        }
        Park selected = parks.stream().filter(p -> p.getParkName().equals(parkCombo.getValue())).findFirst().orElse(null);
        if (selected == null) {
        	showStatus("Park not found.", true);
            return;}
        currentAction = "LOAD_DETAILS";
        ClientUI.client.setHandler(this);
        ClientUI.client.sendMessage(new ClientServerMessage(Command.GET_PARK_DETAILS, selected.getParkId()));
    }

    /**
     * Handles server responses for loading the park list and selected park details.
     * The method updates the park combo box or displays the selected park parameters.
     *
     * @param msg the message received from the server
     */
    @Override
    @SuppressWarnings("unchecked")
    public void handleMessage(ClientServerMessage msg) {
        Platform.runLater(() -> {
            if ("LOAD_PARKS".equals(currentAction) && msg.getData() instanceof ArrayList) {
                parks = (ArrayList<Park>) msg.getData();
                ArrayList<String> names = new ArrayList<>();
                for (Park p : parks) names.add(p.getParkName());
                parkCombo.setItems(FXCollections.observableArrayList(names));
            } else if ("LOAD_DETAILS".equals(currentAction) && msg.getData() instanceof Park) {
                Park p = (Park) msg.getData();
                nameLabel.setText(p.getParkName());
                currentLabel.setText(String.valueOf(p.getCurrentVisitors()));
                maxLabel.setText(String.valueOf(p.getMaxVisitors()));
                gapLabel.setText(String.valueOf(p.getGapForWalkins()));
                capacityLabel.setText(String.valueOf(p.getMaxVisitors() - p.getGapForWalkins()));
                stayLabel.setText(p.getEstimatedVisitDuration() + " hours");
                paramsBox.setVisible(true); paramsBox.setManaged(true);
            }
        });
    }
    /**
     * Displays a status message on the screen.
     * The message color is changed according to whether it represents an error or success.
     *
     * @param msg the message to display
     * @param error true if the message represents an error, otherwise false
     */
    private void showStatus(String msg, boolean error) {
        statusLabel.setWrapText(true);
        statusLabel.setMaxWidth(700);
        statusLabel.setText(msg);
        statusLabel.setStyle("-fx-text-fill: " + (error ? "#e94560" : "#00e676") + ";");
    }
    /**
     * Handles server disconnection by displaying an error message to the department manager.
     *
     * @param r the reason for the disconnection
     */
    @Override
    public void onDisconnected(String r) {
        Platform.runLater(() -> {
            showStatus("Server disconnected. Cannot load park parameters.", true);
        });
    }
}
