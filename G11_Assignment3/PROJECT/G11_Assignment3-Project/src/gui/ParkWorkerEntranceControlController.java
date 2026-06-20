package gui;

import java.net.URL;
import java.util.ArrayList;
import java.util.ResourceBundle;

import client.ClientMessageHandler;
import client.ClientUI;
import client.InputValidation;
import common.ClientServerMessage;
import common.Command;
import common.Order;
import common.worker.GeneralParkWorker;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

public class ParkWorkerEntranceControlController implements Initializable, ClientMessageHandler {
    @FXML private TextField visitorIdField, exitIdField, exitCountField;
    @FXML private Label statusLabel, exitStatusLabel;
    @FXML private Label orderIdLabel, parkLabel, dateLabel, timeLabel, visitorsLabel, typeLabel, priceLabel;
    @FXML private VBox orderInfoBox;
    private Order foundOrder;
    private String currentAction;

    @Override
    public void initialize(URL url, ResourceBundle rb) {}

    @FXML
    private void handleCheck() {
        String id = visitorIdField.getText().trim();
        String err = InputValidation.validateId(id);
        if (err != null) { showStatus(err, true); return; }
        currentAction = "CHECK";
        ClientUI.client.setHandler(this);
        ClientUI.client.sendMessage(new ClientServerMessage(Command.GET_ALL_ORDERS_BY_TRAVELER, id));
    }

    @FXML
    private void handleApproveEntry() {
        if (foundOrder == null) return;
        GeneralParkWorker w = WorkerLoginController.getLoggedInWorker();
        currentAction = "ENTRY";
        ClientUI.client.setHandler(this);
        ArrayList<Object> data = new ArrayList<>();
        data.add(foundOrder.getOrderId());
        data.add(w.getParkId());
        data.add(foundOrder.getVisitorId());
        data.add(foundOrder.getNumVisitors());
        ClientUI.client.sendMessage(new ClientServerMessage(Command.PROCESS_ENTRY, data));
    }

    @FXML
    private void handleDeny() {
        orderInfoBox.setVisible(false);
        orderInfoBox.setManaged(false);
        showStatus("Entry denied.", true);
        foundOrder = null;
    }

    @FXML
    private void handleExit() {
        String id = exitIdField.getText().trim();
        String countStr = exitCountField.getText().trim();
        String err1 = InputValidation.validateId(id);
        String err2 = InputValidation.validateVisitors(countStr, 999);
        if (err1 != null) { showExitStatus(err1, true); return; }
        if (err2 != null) { showExitStatus(err2, true); return; }
        GeneralParkWorker w = WorkerLoginController.getLoggedInWorker();
        currentAction = "EXIT";
        ClientUI.client.setHandler(this);
        ArrayList<Object> data = new ArrayList<>();
        data.add(w.getParkId());
        data.add(id);
        data.add(Integer.parseInt(countStr));
        ClientUI.client.sendMessage(new ClientServerMessage(Command.PROCESS_EXIT, data));
    }

    private void showStatus(String msg, boolean error) {
        statusLabel.setText(msg);
        statusLabel.setStyle("-fx-text-fill: " + (error ? "#e94560" : "#00e676") + ";");
    }

    private void showExitStatus(String msg, boolean error) {
        exitStatusLabel.setText(msg);
        exitStatusLabel.setStyle("-fx-text-fill: " + (error ? "#e94560" : "#00e676") + ";");
    }

    @Override
    @SuppressWarnings("unchecked")
    public void handleMessage(ClientServerMessage msg) {
        Platform.runLater(() -> {
            if ("CHECK".equals(currentAction) && msg.getCommand() == Command.DATA_RESPONSE) {
                ArrayList<Order> orders = (ArrayList<Order>) msg.getData();
                GeneralParkWorker w = WorkerLoginController.getLoggedInWorker();
                String today = java.time.LocalDate.now().toString();
                Order validOrder = null;
                for (Order o : orders) {
                    boolean statusOk = o.getStatus() != null &&
                        (o.getStatus().equalsIgnoreCase("confirmed") || o.getStatus().equalsIgnoreCase("pending"));
                    boolean parkOk = o.getParkId() == w.getParkId();
                    // Compare only the date part (handles "2026-06-20" vs "2026-06-20 00:00:00")
                    boolean dateOk = o.getVisitDate() != null && o.getVisitDate().startsWith(today);
                    if (statusOk && parkOk && dateOk) {
                        validOrder = o; break;
                    }
                }
                if (validOrder != null) {
                    foundOrder = validOrder;
                    orderIdLabel.setText(String.valueOf(validOrder.getOrderId()));
                    parkLabel.setText(validOrder.getParkName() != null ? validOrder.getParkName() : "Park #" + validOrder.getParkId());
                    dateLabel.setText(validOrder.getVisitDate());
                    timeLabel.setText(validOrder.getVisitTime());
                    visitorsLabel.setText(String.valueOf(validOrder.getNumVisitors()));
                    typeLabel.setText(validOrder.getOrderType());
                    priceLabel.setText(validOrder.getTotalPrice() + " NIS");
                    orderInfoBox.setVisible(true);
                    orderInfoBox.setManaged(true);
                    showStatus("Valid booking found!", false);
                } else {
                    orderInfoBox.setVisible(false);
                    orderInfoBox.setManaged(false);
                    showStatus("No valid booking for today at this park.", true);
                }
            } else if ("ENTRY".equals(currentAction) && msg.getCommand() == Command.SUCCESS) {
                // Show invoice popup
                Alert invoice = new Alert(Alert.AlertType.INFORMATION);
                invoice.setTitle("Invoice");
                invoice.setHeaderText("Entry Approved - Invoice");
                invoice.setContentText(
                    "Order #" + foundOrder.getOrderId() + "\n" +
                    "Visitors: " + foundOrder.getNumVisitors() + "\n" +
                    "Type: " + foundOrder.getOrderType() + "\n" +
                    "Total: " + foundOrder.getTotalPrice() + " NIS\n\n" +
                    "Payment is processed outside GoNature system.");
                invoice.showAndWait();
                showStatus("Entry approved! Visitor entered the park.", false);
                orderInfoBox.setVisible(false);
                orderInfoBox.setManaged(false);
                visitorIdField.clear();
                foundOrder = null;
            } else if ("EXIT".equals(currentAction) && msg.getCommand() == Command.SUCCESS) {
                showExitStatus("Exit processed successfully. Park visitors updated.", false);
                exitIdField.clear();
                exitCountField.clear();
            } else if (msg.getCommand() == Command.FAILURE || msg.getCommand() == Command.ERROR) {
                if ("EXIT".equals(currentAction)) {
                    showExitStatus("" + msg.getData(), true);
                } else {
                    showStatus("" + msg.getData(), true);
                }
            }
        });
    }

    @Override
    public void onDisconnected(String reason) { Platform.runLater(() -> showStatus(reason, true)); }
}