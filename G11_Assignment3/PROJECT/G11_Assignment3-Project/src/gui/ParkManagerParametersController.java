package gui;

import java.net.URL;
import java.util.ArrayList;
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
import javafx.scene.control.TextField;

public class ParkManagerParametersController implements Initializable, ClientMessageHandler {
    @FXML private Label parkNameLabel, currentVisitorsLabel, statusLabel;
    @FXML private TextField maxVisitorsField, gapField, durationField;
    private Park currentPark;
    private String currentAction;

    @Override
    public void initialize(URL url, ResourceBundle rb) { loadParkInfo(); }

    @FXML
    public void loadParkInfo() {
        GeneralParkWorker w = WorkerLoginController.getLoggedInWorker();
        currentAction = "LOAD";
        ClientUI.client.setHandler(this);
        ClientUI.client.sendMessage(new ClientServerMessage(Command.GET_PARK_DETAILS, w.getParkId()));
    }

    @FXML
    private void handleSubmitChange() {
        if (currentPark == null) { statusLabel.setText("Park info not loaded."); return; }
        GeneralParkWorker w = WorkerLoginController.getLoggedInWorker();
        try {
            int newMax = Integer.parseInt(maxVisitorsField.getText().trim());
            int newGap = Integer.parseInt(gapField.getText().trim());
            double newDuration = Double.parseDouble(durationField.getText().trim());

            ArrayList<Object> requests = new ArrayList<>();
            if (newMax != currentPark.getMaxVisitors()) {
                requests.add(createRequest(w, "max_visitors", currentPark.getMaxVisitors(), newMax));
            }
            if (newGap != currentPark.getGapForWalkins()) {
                requests.add(createRequest(w, "gap_for_walkins", currentPark.getGapForWalkins(), newGap));
            }
            if (newDuration != currentPark.getEstimatedVisitDuration()) {
                requests.add(createRequest(w, "estimated_visit_duration", currentPark.getEstimatedVisitDuration(), newDuration));
            }
            if (requests.isEmpty()) {
                statusLabel.setText("No changes detected."); statusLabel.setStyle("-fx-text-fill: #f5a623;"); return;
            }
            currentAction = "SUBMIT";
            ClientUI.client.setHandler(this);
            ClientUI.client.sendMessage(new ClientServerMessage(Command.REQUEST_PARAMETER_CHANGE, requests));
        } catch (NumberFormatException e) {
            statusLabel.setText("Please enter valid numbers."); statusLabel.setStyle("-fx-text-fill: #e94560;");
        }
    }

    private ArrayList<Object> createRequest(GeneralParkWorker w, String param, double oldVal, double newVal) {
        ArrayList<Object> req = new ArrayList<>();
        req.add(w.getParkId()); req.add(param); req.add(oldVal); req.add(newVal); req.add(w.getEmployeeId());
        return req;
    }

    @Override
    public void handleMessage(ClientServerMessage msg) {
        Platform.runLater(() -> {
            if ("LOAD".equals(currentAction) && msg.getCommand() == Command.DATA_RESPONSE && msg.getData() instanceof Park) {
                currentPark = (Park) msg.getData();
                parkNameLabel.setText(currentPark.getParkName());
                currentVisitorsLabel.setText(String.valueOf(currentPark.getCurrentVisitors()));
                maxVisitorsField.setText(String.valueOf(currentPark.getMaxVisitors()));
                gapField.setText(String.valueOf(currentPark.getGapForWalkins()));
                durationField.setText(String.valueOf(currentPark.getEstimatedVisitDuration()));
            } else if ("SUBMIT".equals(currentAction) && msg.getCommand() == Command.SUCCESS) {
                statusLabel.setText("Change request submitted! Awaiting department manager approval.");
                statusLabel.setStyle("-fx-text-fill: #00e676;");
            } else if (msg.getCommand() == Command.FAILURE || msg.getCommand() == Command.ERROR) {
                statusLabel.setText("Error: " + msg.getData()); statusLabel.setStyle("-fx-text-fill: #e94560;");
            }
        });
    }
    @Override public void onDisconnected(String r) { Platform.runLater(() -> statusLabel.setText(r)); }
}
