package gui;

import client.*;
import common.*;
import javafx.application.Platform;
import javafx.collections.*;
import javafx.fxml.*;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import java.net.URL;
import java.util.*;

public class TravelerOrdersController implements Initializable, ClientMessageHandler {
    @FXML private TableView<Order> ordersTable;
    @FXML private TableColumn<Order, Integer> colId, colVisitors;
    @FXML private TableColumn<Order, String> colPark, colDate, colTime, colType, colStatus;
    @FXML private TableColumn<Order, Double> colPrice;
    @FXML private Label statusLabel;
    private ObservableList<Order> orderData = FXCollections.observableArrayList();

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
        loadOrders();
    }

    @FXML
    public void loadOrders() {
        ClientUI.client.setHandler(this);
        String id = TravelerLoginController.getLoggedInTraveler().getIdNumber();
        ClientUI.client.sendMessage(new ClientServerMessage(Command.GET_ALL_ORDERS_BY_TRAVELER, id));
    }

    @FXML
    private void handleCancel() {
        Order selected = ordersTable.getSelectionModel().getSelectedItem();
        if (selected == null) { statusLabel.setText("Select an order first."); return; }
        ClientUI.client.setHandler(this);
        ClientUI.client.sendMessage(new ClientServerMessage(Command.CANCEL_ORDER, selected.getOrderId()));
    }

    @FXML
    private void handleConfirm() {
        Order selected = ordersTable.getSelectionModel().getSelectedItem();
        if (selected == null) { statusLabel.setText("Select an order first."); return; }
        ClientUI.client.setHandler(this);
        ClientUI.client.sendMessage(new ClientServerMessage(Command.CONFIRM_ORDER, selected.getOrderId()));
    }

    @Override
    @SuppressWarnings("unchecked")
    public void handleMessage(ClientServerMessage msg) {
        Platform.runLater(() -> {
            if (msg.getCommand() == Command.DATA_RESPONSE && msg.getData() instanceof ArrayList) {
                orderData.clear();
                orderData.addAll((ArrayList<Order>) msg.getData());
                statusLabel.setText(orderData.size() + " orders loaded.");
                statusLabel.setStyle("-fx-text-fill: #00e676;");
            } else if (msg.getCommand() == Command.SUCCESS) {
                statusLabel.setText("Order updated successfully.");
                statusLabel.setStyle("-fx-text-fill: #00e676;");
                loadOrders();
            }
        });
    }

    @Override
    public void onDisconnected(String reason) { Platform.runLater(() -> statusLabel.setText(reason)); }
}
