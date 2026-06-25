package gui;

import client.*;
import common.*;
import common.worker.GeneralParkWorker;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.*;
import javafx.scene.control.*;
import java.net.URL;
import java.util.*;

/**
 * JavaFX controller for the Park Parameters screen (ParkManagerParameters.fxml).
 * <p>
 * Allows the park manager to view and request changes to the three configurable
 * park parameters: maximum visitors ({@code max_visitors}), walk-in gap
 * ({@code gap_for_walkins}), and estimated visit duration ({@code estimated_visit_duration}).
 * </p>
 * <p>
 * Change requests are sent to the department manager for approval via
 * {@code REQUEST_PARAMETER_CHANGE}. The request is applied to the park only
 * after the department manager approves it ({@code APPROVE_CHANGE}).
 * </p>
 *
 * @author Group 11
 */
public class ParkManagerParametersController implements Initializable, ClientMessageHandler {
    @FXML private Label parkNameLabel, statusLabel;
    @FXML private Label currentMaxVisitors, currentGap, currentStayTime;
    @FXML private TextField newMaxVisitors, newGap, newStayTime;
    @FXML private TableView<ArrayList<String>> requestsTable;
    @FXML private TableColumn<ArrayList<String>, String> colParam, colOld, colNew, colStatus, colDate;

    private Park currentPark;
    private String currentAction;

    /**
     * Initializes the park manager parameters screen.
     * The method prepares the parameter requests table and loads the current park data.
     *
     * @param url the location used to resolve relative paths
     * @param rb the resources used to localize the screen
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        colParam.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().get(0)));
        colOld.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().get(1)));
        colNew.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().get(2)));
        colStatus.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().get(3)));
        colDate.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().get(4)));

        loadParkData();
    }

    /**
     * Loads the current parameter values for the park managed by the logged-in park manager.
     * If the server is disconnected or the worker session is missing, an error message is displayed.
     */
    private void loadParkData() {
        if (!ClientUI.isServerConnected()) {
            showStatus("Server disconnected. Cannot load park parameters.", true);
            return;
        }
        GeneralParkWorker w = WorkerLoginController.getLoggedInWorker();
        if (w == null) {
            showStatus("Worker session expired. Please log in again.", true);
            return;
        }
        currentAction = "LOAD_PARK";
        ClientUI.client.setHandler(this);
        ClientUI.client.sendMessage(new ClientServerMessage(Command.GET_PARK_DETAILS, w.getParkId()));
    }

    /**
     * Handles submission of parameter change requests.
     * The method validates the entered values and sends one request for each changed parameter.
     */
    @FXML
    private void handleSubmit() {
    	if (!ClientUI.isServerConnected()) {
            showStatus("Server disconnected. Cannot submit parameter change.", true);
            return;
        }
    	if (currentPark == null) { statusLabel.setText("Park data not loaded yet."); return; }

        String maxV = newMaxVisitors.getText().trim();
        String gap = newGap.getText().trim();
        String stay = newStayTime.getText().trim();

        if (maxV.isEmpty() && gap.isEmpty() && stay.isEmpty()) {
            statusLabel.setText("Enter at least one new value.");
            statusLabel.setStyle("-fx-text-fill: #f87171;");
            return;
        }

        GeneralParkWorker w = WorkerLoginController.getLoggedInWorker();
        int sent = 0;

        try {
            if (!maxV.isEmpty()) {
                int val = Integer.parseInt(maxV);
                if (val <= 0) { statusLabel.setText("Max visitors must be positive."); statusLabel.setStyle("-fx-text-fill: #f87171;"); return; }
                sendRequest(w, "max_visitors", currentPark.getMaxVisitors(), val); sent++;
            }
            if (!gap.isEmpty()) {
                int val = Integer.parseInt(gap);
                if (val < 0) { statusLabel.setText("Gap cannot be negative."); statusLabel.setStyle("-fx-text-fill: #f87171;"); return; }
                sendRequest(w, "gap_for_walkins", currentPark.getGapForWalkins(), val); sent++;
            }
            if (!stay.isEmpty()) {
                double val = Double.parseDouble(stay);
                if (val <= 0) { statusLabel.setText("Stay time must be positive."); statusLabel.setStyle("-fx-text-fill: #f87171;"); return; }
                sendRequest(w, "estimated_visit_duration", currentPark.getEstimatedVisitDuration(), val); sent++;
            }
        } catch (NumberFormatException e) {
            statusLabel.setText("Values must be numbers.");
            statusLabel.setStyle("-fx-text-fill: #f87171;");
            return;
        }

        statusLabel.setText(sent + " change request(s) sent for approval.");
        statusLabel.setStyle("-fx-text-fill: #34d399;");
        newMaxVisitors.clear(); newGap.clear(); newStayTime.clear();

        // Reload requests after short delay
        new Thread(() -> {
            try { Thread.sleep(500); } catch (InterruptedException e) {}
            Platform.runLater(this::loadRequests);
        }).start();
    }

    /**
     * Sends a single parameter change request to the server for department manager approval.
     *
     * @param w the logged-in park manager who submits the request
     * @param param the database name of the parameter to change
     * @param oldVal the current parameter value
     * @param newVal the requested new parameter value
     */
    private void sendRequest(GeneralParkWorker w, String param, double oldVal, double newVal) {
    	if (!ClientUI.isServerConnected()) {
            showStatus("Server disconnected. Cannot send request.", true);
            return;}
        if (w == null) {
            showStatus("Worker session expired. Please log in again.", true);
            return;}
    	ArrayList<Object> params = new ArrayList<>();
        params.add(w.getParkId());
        params.add(param);
        params.add(oldVal);
        params.add(newVal);
        params.add(w.getEmployeeId());
        currentAction = "SUBMIT";
        ClientUI.client.setHandler(this);
        ClientUI.client.sendMessage(new ClientServerMessage(Command.REQUEST_PARAMETER_CHANGE, params));
    }

    /**
     * Loads all parameter change requests submitted for the park managed by the logged-in manager.
     * If the server is disconnected or the worker session is missing, an error message is displayed.
     */
    private void loadRequests() {
        if (!ClientUI.isServerConnected()) {
            showStatus("Server disconnected. Cannot load parameter requests.", true);
            return;}
        GeneralParkWorker w = WorkerLoginController.getLoggedInWorker();
        if (w == null) {
            showStatus("Worker session expired. Please log in again.", true);
            return;}
        currentAction = "LOAD_REQUESTS";
        ClientUI.client.setHandler(this);
        ClientUI.client.sendMessage(new ClientServerMessage(Command.GET_MY_PARAMETER_REQUESTS, w.getParkId()));
    }

    /**
     * Handles server responses for loading park data, loading parameter requests,
     * and submitting parameter change requests.
     * The method updates the parameter labels and requests table according to the current action.
     *
     * @param msg the message received from the server
     */
    @Override
    @SuppressWarnings("unchecked")
    public void handleMessage(ClientServerMessage msg) {
        Platform.runLater(() -> {
            switch (currentAction) {
                case "LOAD_PARK":
                    if (msg.getData() instanceof Park) {
                        currentPark = (Park) msg.getData();
                        parkNameLabel.setText(currentPark.getParkName());
                        currentMaxVisitors.setText(String.valueOf(currentPark.getMaxVisitors()));
                        currentGap.setText(String.valueOf(currentPark.getGapForWalkins()));
                        currentStayTime.setText(String.valueOf(currentPark.getEstimatedVisitDuration()));
                        loadRequests();
                    }
                    break;
                case "LOAD_REQUESTS":
                    if (msg.getData() instanceof ArrayList) {
                        ArrayList<ArrayList<String>> requests = (ArrayList<ArrayList<String>>) msg.getData();
                        requestsTable.setItems(FXCollections.observableArrayList(requests));
                    }
                    break;
                case "SUBMIT":
                    // Status already shown
                    break;
            }
        });
    }
    /**
     * Displays a status message on the park manager parameters screen.
     * The message color is changed according to whether it represents an error or success.
     *
     * @param msg the message to display
     * @param error true if the message represents an error, otherwise false
     */
    private void showStatus(String msg, boolean error) {
        statusLabel.setWrapText(true);
        statusLabel.setMaxWidth(600);
        statusLabel.setText(msg);
        statusLabel.setStyle("-fx-text-fill: " + (error ? "#e94560" : "#34d399") + ";");
    }

    /**
     * Handles server disconnection by displaying an error message on the screen.
     *
     * @param r the reason for the disconnection
     */
    @Override
    public void onDisconnected(String r) {
        Platform.runLater(() -> {
            showStatus("Server disconnected. Actions are unavailable.", true);
        });
    }
}