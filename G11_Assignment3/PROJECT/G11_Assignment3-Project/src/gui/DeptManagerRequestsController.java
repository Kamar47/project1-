package gui;

import java.net.URL;
import java.util.ArrayList;
import java.util.ResourceBundle;

import client.ClientMessageHandler;
import client.ClientUI;
import common.ClientServerMessage;
import common.Command;
import common.worker.GeneralParkWorker;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

public class DeptManagerRequestsController implements Initializable, ClientMessageHandler {
    @FXML private TableView<ArrayList<String>> paramRequestsTable, promoRequestsTable;
    @FXML private TableColumn<ArrayList<String>, String> colParamId, colParamPark, colParamName, colOldVal, colNewVal, colParamStatus;
    @FXML private TableColumn<ArrayList<String>, String> colPromoId, colPromoPark, colDiscount, colStart, colEnd, colDesc, colPromoStatus;
    @FXML private Label statusLabel;
    private ObservableList<ArrayList<String>> paramData = FXCollections.observableArrayList();
    private ObservableList<ArrayList<String>> promoData = FXCollections.observableArrayList();
    private String currentAction;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        setupColumn(colParamId, 0); setupColumn(colParamPark, 1); setupColumn(colParamName, 2);
        setupColumn(colOldVal, 3); setupColumn(colNewVal, 4); setupColumn(colParamStatus, 5);
        paramRequestsTable.setItems(paramData);

        setupColumn(colPromoId, 0); setupColumn(colPromoPark, 1); setupColumn(colDiscount, 2);
        setupColumn(colStart, 3); setupColumn(colEnd, 4); setupColumn(colDesc, 5); setupColumn(colPromoStatus, 6);
        promoRequestsTable.setItems(promoData);

        loadRequests();
    }

    private void setupColumn(TableColumn<ArrayList<String>, String> col, int index) {
        col.setCellValueFactory(data -> {
            ArrayList<String> row = data.getValue();
            return new SimpleStringProperty(index < row.size() ? row.get(index) : "");
        });
    }

    @FXML
    public void loadRequests() {
    	if (!ClientUI.isServerConnected()) {
    	    showStatus("Server disconnected. Cannot load requests.", true);
    	    return;
    	}
        currentAction = "LOAD";
        ClientUI.client.setHandler(this);
        ClientUI.client.sendMessage(new ClientServerMessage(Command.GET_CHANGE_REQUESTS));
    }

    @FXML private void handleApproveParam() { processParamRequest("APPROVE"); }
    @FXML private void handleRejectParam() { processParamRequest("REJECT"); }

    private void processParamRequest(String action) {
    	if (!ClientUI.isServerConnected()) {
            showStatus("Server disconnected. Cannot process parameter request.", true);
            return;
        }
    	ArrayList<String> selected = paramRequestsTable.getSelectionModel().getSelectedItem();
        if (selected == null) { statusLabel.setText("Select a request first."); statusLabel.setStyle("-fx-text-fill: #e94560;"); return; }
        GeneralParkWorker w = WorkerLoginController.getLoggedInWorker();
        ArrayList<Object> data = new ArrayList<>();
        data.add(Integer.parseInt(selected.get(0)));
        data.add(w.getEmployeeId());
        currentAction = action;
        Command cmd = "APPROVE".equals(action) ? Command.APPROVE_CHANGE : Command.REJECT_CHANGE;
        ClientUI.client.setHandler(this);
        ClientUI.client.sendMessage(new ClientServerMessage(cmd, data));
    }

    @FXML private void handleApprovePromo() { processPromoRequest(Command.APPROVE_PROMOTION); }
    @FXML private void handleRejectPromo() { processPromoRequest(Command.REJECT_PROMOTION); }

    private void processPromoRequest(Command cmd) {
    	if (!ClientUI.isServerConnected()) {
            showStatus("Server disconnected. Cannot process promotion request.", true);
            return;
        }
    	ArrayList<String> selected = promoRequestsTable.getSelectionModel().getSelectedItem();
        if (selected == null) { statusLabel.setText("Select a promotion first."); statusLabel.setStyle("-fx-text-fill: #e94560;"); return; }
        GeneralParkWorker w = WorkerLoginController.getLoggedInWorker();
        ArrayList<Object> data = new ArrayList<>();
        data.add(Integer.parseInt(selected.get(0)));
        data.add(w.getEmployeeId());
        currentAction = "PROMO";
        ClientUI.client.setHandler(this);
        ClientUI.client.sendMessage(new ClientServerMessage(cmd, data));
    }

    @Override
    @SuppressWarnings("unchecked")
    public void handleMessage(ClientServerMessage msg) {
        Platform.runLater(() -> {
            if ("LOAD".equals(currentAction) && msg.getCommand() == Command.DATA_RESPONSE) {
                ArrayList<ArrayList<ArrayList<String>>> allData = (ArrayList<ArrayList<ArrayList<String>>>) msg.getData();
                if (allData != null && allData.size() >= 2) {
                    paramData.clear(); paramData.addAll(allData.get(0));
                    promoData.clear(); promoData.addAll(allData.get(1));
                    statusLabel.setText(paramData.size() + " param requests, " + promoData.size() + " promo requests.");
                    statusLabel.setStyle("-fx-text-fill: #a0a0b8;");
                }
            } else if (msg.getCommand() == Command.SUCCESS) {
                statusLabel.setText("Request processed successfully.");
                statusLabel.setStyle("-fx-text-fill: #00e676;");
                loadRequests();
            } else if (msg.getCommand() == Command.FAILURE) {
                statusLabel.setText("Error: " + msg.getData());
                statusLabel.setStyle("-fx-text-fill: #e94560;");
            }
        });
    }
    private void showStatus(String msg, boolean error) {
        statusLabel.setWrapText(true);
        statusLabel.setMaxWidth(600);
        statusLabel.setText(msg);
        statusLabel.setStyle("-fx-text-fill: " + (error ? "#e94560" : "#a0a0b8") + ";");
    }
    @Override
    public void onDisconnected(String r) {
        Platform.runLater(() -> {
            showStatus("Server disconnected. Cannot load or process requests.", true);
        });
    }
}
