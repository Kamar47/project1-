package gui;

import java.net.URL;
import java.util.ArrayList;
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
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;

public class TravelerWaitingListController implements Initializable, ClientMessageHandler {
    @FXML private TableView<Order> waitlistTable;
    @FXML private TableColumn<Order, Integer> colId, colVisitors;
    @FXML private TableColumn<Order, String> colPark, colDate, colTime, colStatus;
    @FXML private Label statusLabel;
    private ObservableList<Order> waitlistData = FXCollections.observableArrayList();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        colId.setCellValueFactory(new PropertyValueFactory<>("orderId"));
        colPark.setCellValueFactory(new PropertyValueFactory<>("parkName"));
        colDate.setCellValueFactory(new PropertyValueFactory<>("visitDate"));
        colTime.setCellValueFactory(new PropertyValueFactory<>("visitTime"));
        colVisitors.setCellValueFactory(new PropertyValueFactory<>("numVisitors"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        waitlistTable.setItems(waitlistData);
        loadWaitlist();
    }

    @FXML
    public void loadWaitlist() {
    	if (!ClientUI.isServerConnected()) {
            statusLabel.setText("Server is disconnected. Cannot load waiting list.");
            statusLabel.setStyle("-fx-text-fill: #e94560;");
            return;
        }
        ClientUI.client.setHandler(this);
        String id = TravelerLoginController.getLoggedInTraveler().getIdNumber();
        ClientUI.client.sendMessage(new ClientServerMessage(Command.GET_ALL_ORDERS_BY_TRAVELER, id));
    }

    @FXML
    private void handleRemove() {
    	if (!ClientUI.isServerConnected()) {
    	    statusLabel.setText("Server is disconnected. Cannot remove from waiting list.");
    	    statusLabel.setStyle("-fx-text-fill: #e94560;");
    	    return;
    	}
        Order selected = waitlistTable.getSelectionModel().getSelectedItem();
        if (selected == null) { statusLabel.setText("Select an order first."); return; }
        ClientUI.client.setHandler(this);
        ClientUI.client.sendMessage(new ClientServerMessage(Command.CANCEL_ORDER, selected.getOrderId()));
    }

    @Override
    @SuppressWarnings("unchecked")
    public void handleMessage(ClientServerMessage msg) {
        Platform.runLater(() -> {
            if (msg.getCommand() == Command.DATA_RESPONSE && msg.getData() instanceof ArrayList) {
                waitlistData.clear();
                ArrayList<Order> allOrders = (ArrayList<Order>) msg.getData();
                for (Order o : allOrders) {
                    if ("waitlist".equals(o.getStatus())) waitlistData.add(o);
                }
                statusLabel.setText(waitlistData.size() + " items in waiting list.");
            } else if (msg.getCommand() == Command.SUCCESS) {
                statusLabel.setText("Removed from waiting list.");
                statusLabel.setStyle("-fx-text-fill: #00e676;");
                loadWaitlist();
            }
        });
    }

    @Override
    public void onDisconnected(String reason) { Platform.runLater(() -> statusLabel.setText(reason)); }
}
