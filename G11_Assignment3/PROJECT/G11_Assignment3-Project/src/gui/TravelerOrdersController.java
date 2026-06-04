package gui;

import java.net.URL;
import java.util.ArrayList;
import java.util.Optional;
import java.util.ResourceBundle;

import client.ClientMessageHandler;
import client.ClientUI;
import common.ClientServerMessage;
import common.Command;
import common.Order;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;

public class TravelerOrdersController implements Initializable, ClientMessageHandler {
    @FXML private TableView<Order> ordersTable;
    @FXML private TableColumn<Order, Integer> colId, colVisitors;
    @FXML private TableColumn<Order, String> colPark, colDate, colTime, colType, colStatus;
    @FXML private TableColumn<Order, Double> colPrice;
    @FXML private Label statusLabel;
    private ObservableList<Order> orderData = FXCollections.observableArrayList();
    private String currentAction;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        colId.setCellValueFactory(new PropertyValueFactory<>("orderId"));
        colPark.setCellValueFactory(new PropertyValueFactory<>("parkName"));
        colVisitors.setCellValueFactory(new PropertyValueFactory<>("numVisitors"));
        colDate.setCellValueFactory(new PropertyValueFactory<>("visitDate"));
        colTime.setCellValueFactory(new PropertyValueFactory<>("visitTime"));
        colType.setCellValueFactory(new PropertyValueFactory<>("orderType"));
        colPrice.setCellValueFactory(new PropertyValueFactory<>("totalPrice"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        ordersTable.setItems(orderData);

        // Color-code status column
        colStatus.setCellFactory(col -> new TableCell<Order, String>() {
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) { setText(null); setStyle(""); return; }
                setText(status);
                switch (status) {
                    case "confirmed": setStyle("-fx-text-fill: #00e676;"); break;
                    case "pending": case "waitlist": setStyle("-fx-text-fill: #f5a623;"); break;
                    case "cancelled": case "expired": case "no_show": setStyle("-fx-text-fill: #e94560;"); break;
                    case "completed": setStyle("-fx-text-fill: #2d6a4f;"); break;
                    default: setStyle("-fx-text-fill: #c0c0d8;"); break;
                }
            }
        });
        loadOrders();
    }

    @FXML
    public void loadOrders() {
        currentAction = "LOAD";
        ClientUI.client.setHandler(this);
        String id = TravelerLoginController.getLoggedInTraveler().getIdNumber();
        ClientUI.client.sendMessage(new ClientServerMessage(Command.GET_ALL_ORDERS_BY_TRAVELER, id));
    }

    @FXML
    private void handleCancel() {
        Order selected = ordersTable.getSelectionModel().getSelectedItem();
        if (selected == null) { showStatus("Select an order first.", true); return; }
        if (!"confirmed".equals(selected.getStatus()) && !"pending".equals(selected.getStatus()) && !"waitlist".equals(selected.getStatus())) {
            showStatus("Can only cancel confirmed, pending, or waitlisted orders.", true); return;
        }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Cancel Order");
        confirm.setHeaderText("Are you sure you want to cancel order #" + selected.getOrderId() + "?");
        confirm.setContentText("Park: " + selected.getParkName() + "\nDate: " + selected.getVisitDate());
        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            currentAction = "CANCEL";
            ClientUI.client.setHandler(this);
            ClientUI.client.sendMessage(new ClientServerMessage(Command.CANCEL_ORDER, selected.getOrderId()));
            // Simulate cancellation notification
            NotificationSimulator.simulateCancellation(selected.getEmail(), selected.getPhone(),
                selected.getParkName(), selected.getVisitDate());
        }
    }

    @FXML
    private void handleConfirm() {
        Order selected = ordersTable.getSelectionModel().getSelectedItem();
        if (selected == null) { showStatus("Select an order first.", true); return; }
        if (!"pending".equals(selected.getStatus())) {
            showStatus("Can only confirm pending orders.", true); return;
        }
        currentAction = "CONFIRM";
        ClientUI.client.setHandler(this);
        ClientUI.client.sendMessage(new ClientServerMessage(Command.CONFIRM_ORDER, selected.getOrderId()));
    }

    private void showStatus(String msg, boolean error) {
        statusLabel.setText(msg);
        statusLabel.setStyle("-fx-text-fill: " + (error ? "#e94560" : "#00e676") + ";");
    }

    @Override
    @SuppressWarnings("unchecked")
    public void handleMessage(ClientServerMessage msg) {
        Platform.runLater(() -> {
            if ("LOAD".equals(currentAction) && msg.getCommand() == Command.DATA_RESPONSE && msg.getData() instanceof ArrayList) {
                orderData.clear();
                orderData.addAll((ArrayList<Order>) msg.getData());
                showStatus(orderData.size() + " orders loaded.", false);
            } else if (msg.getCommand() == Command.SUCCESS) {
                if ("CANCEL".equals(currentAction)) showStatus("Order cancelled successfully.", false);
                else if ("CONFIRM".equals(currentAction)) showStatus("Order confirmed successfully.", false);
                loadOrders();
            } else if (msg.getCommand() == Command.FAILURE) {
                showStatus("Error: " + msg.getData(), true);
            }
        });
    }

    @Override
    public void onDisconnected(String reason) { Platform.runLater(() -> showStatus(reason, true)); }
}
