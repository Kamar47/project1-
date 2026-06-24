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
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;

public class TravelerOrdersController implements Initializable, ClientMessageHandler {

    // Static reference so NotificationPopupController can trigger a refresh
    private static TravelerOrdersController activeInstance = null;

    public static void refreshIfVisible() {
        if (activeInstance != null) {
            javafx.application.Platform.runLater(activeInstance::loadOrders);
        }
    }

    @FXML private TableView<Order> ordersTable;
    @FXML private TableColumn<Order, Integer> colId, colVisitors;
    @FXML private TableColumn<Order, String> colPark, colDate, colTime, colType, colStatus;
    @FXML private TableColumn<Order, Double> colPrice;
    @FXML private Label statusLabel;
    @FXML private Button confirmBtn;

    private ObservableList<Order> orderData = FXCollections.observableArrayList();
    private String currentAction;

    // ─────────────────────────────────────────────
    // Init
    // ─────────────────────────────────────────────

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        colId.setCellValueFactory(new PropertyValueFactory<>("orderId"));
        colPark.setCellValueFactory(new PropertyValueFactory<>("parkName"));
        colVisitors.setCellValueFactory(new PropertyValueFactory<>("numVisitors"));
        colDate.setCellValueFactory(new PropertyValueFactory<>("visitDate"));
        colTime.setCellValueFactory(new PropertyValueFactory<>("visitTime"));
        colType.setCellValueFactory(new PropertyValueFactory<>("orderType"));
        colPrice.setCellValueFactory(new PropertyValueFactory<>("totalPrice"));

        // Status column: display label based on status + reminder flags
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colStatus.setCellFactory(col -> new TableCell<Order, String>() {
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) { setText(null); setStyle(""); return; }

                Order order = getTableView().getItems().get(getIndex());
                String label = resolveLabel(order);
                String color = resolveColor(order);
                setText(label);
                setStyle("-fx-text-fill: " + color + ";");
            }
        });

        ordersTable.setItems(orderData);

        // Update confirm button visibility when selection changes
        ordersTable.getSelectionModel().selectedItemProperty().addListener((obs, old, selected) ->
            updateConfirmButton(selected));

        if (confirmBtn != null) confirmBtn.setDisable(true);
        activeInstance = this;
        loadOrders();
    }

    // ─────────────────────────────────────────────
    // Label + color logic (display only, no DB change)
    // ─────────────────────────────────────────────

    private String resolveLabel(Order o) {
        if (o == null) return "";
        String status = o.getStatus();
        if (status == null) return "";

        switch (status) {
            case "confirmed":
                if (o.isReminderSent() && !o.isReminderConfirmed())
                    return "\u26a0 Confirm visit";          // ⚠ Confirm visit
                if (o.isReminderConfirmed())
                    return "\u2705 Confirmed";              // ✅ Confirmed
                return "Reserved";                          // reminder not sent yet
            case "pending":   return "\u23f0 Spot available"; // ⏰ Spot available
            case "waitlist":  return "Waiting list";
            case "in_park":   return "In park";
            case "completed": return "Completed";
            case "cancelled": return "Cancelled";
            case "expired":   return "Expired";
            case "no_show":   return "No show";
            default:          return status;
        }
    }

    private String resolveColor(Order o) {
        if (o == null) return "#c0c0d8";
        String status = o.getStatus();
        if (status == null) return "#c0c0d8";

        switch (status) {
            case "confirmed":
                if (o.isReminderSent() && !o.isReminderConfirmed()) return "#fbbf24"; // amber — action needed
                if (o.isReminderConfirmed())                         return "#34d399"; // green — all good
                return "#60a5fa";                                                       // blue  — reserved
            case "pending":   return "#fbbf24"; // amber
            case "waitlist":  return "#94a3b8"; // muted
            case "in_park":   return "#34d399"; // green
            case "completed": return "#2d6a4f"; // dark green
            case "cancelled": case "expired": case "no_show": return "#f87171"; // red
            default:          return "#c0c0d8";
        }
    }

    // Show Confirm button only for:
    // 1. confirmed + reminder_sent=true + reminder_confirmed=false  (reminder confirmation)
    // 2. pending (waitlist spot offer)
    private boolean needsConfirm(Order o) {
        if (o == null) return false;
        String status = o.getStatus();
        if ("pending".equals(status)) return true;
        if ("confirmed".equals(status) && o.isReminderSent() && !o.isReminderConfirmed()) return true;
        return false;
    }

    private void updateConfirmButton(Order selected) {
        if (confirmBtn == null) return;
        confirmBtn.setDisable(!needsConfirm(selected));
    }

    // ─────────────────────────────────────────────
    // Actions
    // ─────────────────────────────────────────────

    @FXML
    public void loadOrders() {
        if (!ClientUI.isServerConnected()) { showStatus("Server disconnected.", true); return; }
        currentAction = "LOAD";
        ClientUI.client.setHandler(this);
        String id = TravelerLoginController.getLoggedInTraveler().getIdNumber();
        ClientUI.client.sendMessage(new ClientServerMessage(Command.GET_ALL_ORDERS_BY_TRAVELER, id));
    }

    @FXML
    private void handleCancel() {
        if (!ClientUI.isServerConnected()) { showStatus("Server disconnected.", true); return; }
        Order selected = ordersTable.getSelectionModel().getSelectedItem();
        if (selected == null) { showStatus("Select an order first.", true); return; }

        String s = selected.getStatus();
        if (!"confirmed".equals(s) && !"pending".equals(s) && !"waitlist".equals(s)) {
            showStatus("Can only cancel confirmed, pending, or waitlisted orders.", true); return;
        }
        Alert dlg = new Alert(Alert.AlertType.CONFIRMATION);
        dlg.setTitle("Cancel Order");
        dlg.setHeaderText("Cancel order #" + selected.getOrderId() + "?");
        dlg.setContentText("Park: " + selected.getParkName() + "\nDate: " + selected.getVisitDate());
        Optional<ButtonType> result = dlg.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            currentAction = "CANCEL";
            ClientUI.client.setHandler(this);
            ClientUI.client.sendMessage(new ClientServerMessage(Command.CANCEL_ORDER, selected.getOrderId()));
        }
    }

    @FXML
    private void handleConfirm() {
        if (!ClientUI.isServerConnected()) { showStatus("Server disconnected.", true); return; }
        Order selected = ordersTable.getSelectionModel().getSelectedItem();
        if (selected == null) { showStatus("Select an order first.", true); return; }

        if (!needsConfirm(selected)) {
            showStatus("This order does not require confirmation.", true); return;
        }

        // CONFIRM_ORDER handles both cases server-side:
        // - pending → confirmWaitlistOffer() → confirmed
        // - confirmed + reminder_sent → confirmReminder() → reminder_confirmed = TRUE
        currentAction = "CONFIRM";
        if (confirmBtn != null) confirmBtn.setDisable(true);
        ClientUI.client.setHandler(this);
        ClientUI.client.sendMessage(new ClientServerMessage(Command.CONFIRM_ORDER, selected.getOrderId()));
    }

    // ─────────────────────────────────────────────
    // Server response
    // ─────────────────────────────────────────────

    @Override
    @SuppressWarnings("unchecked")
    public void handleMessage(ClientServerMessage msg) {
        Platform.runLater(() -> {
            if ("LOAD".equals(currentAction)
                    && msg.getCommand() == Command.DATA_RESPONSE
                    && msg.getData() instanceof ArrayList) {
                orderData.clear();
                orderData.addAll((ArrayList<Order>) msg.getData());
                showStatus(orderData.size() + " orders loaded.", false);
                updateConfirmButton(ordersTable.getSelectionModel().getSelectedItem());

            } else if (msg.getCommand() == Command.SUCCESS) {
                if ("CANCEL".equals(currentAction)) {
                    showStatus("Order cancelled successfully.", false);
                    // Simulate cancellation notification
                    Order sel = ordersTable.getSelectionModel().getSelectedItem();
                    if (sel != null && sel.getEmail() != null)
                        gui.NotificationSimulator.simulateCancellation(sel.getEmail(), sel.getPhone(),
                            sel.getParkName() != null ? sel.getParkName() : "Park", sel.getVisitDate());
                } else if ("CONFIRM".equals(currentAction))
                    showStatus("Confirmed successfully.", false);
                loadOrders();

            } else if (msg.getCommand() == Command.FAILURE) {
                showStatus("" + msg.getData(), true);
                if (confirmBtn != null) confirmBtn.setDisable(false);
            }
        });
    }

    @Override
    public void onDisconnected(String reason) {
        Platform.runLater(() -> showStatus(reason, true));
    }

    private void showStatus(String msg, boolean error) {
        statusLabel.setText(msg);
        statusLabel.setStyle("-fx-text-fill: " + (error ? "#f87171" : "#34d399") + ";");
    }
}
