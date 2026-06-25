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

/**
 * JavaFX controller for the traveler's View Orders screen (TravelerOrdersFrame.fxml).
 * <p>
 * Displays all orders belonging to the logged-in traveler in a sortable table.
 * Each order shows its status with a colour-coded label:
 * </p>
 * <ul>
 *   <li>Blue — reserved (reminder not yet sent)</li>
 *   <li>Amber — action needed (reminder sent, awaiting confirmation)</li>
 *   <li>Green — confirmed / in park / completed</li>
 *   <li>Red — cancelled / expired / no-show</li>
 * </ul>
 * <p>
 * The traveler may cancel an eligible order or confirm a pending reminder or
 * waitlist spot directly from this screen. A static {@link #refreshIfVisible()}
 * method is called by the notification popup to keep the table up to date.
 * </p>
 *
 * @author Group 11
 */
public class TravelerOrdersController implements Initializable, ClientMessageHandler {

    // Static reference so NotificationPopupController can trigger a refresh
    private static TravelerOrdersController activeInstance = null;

    /**
     * Refreshes the traveler orders table if the screen is currently open.
     * This method is used by notification popups after the traveler responds to a notification.
     */
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

    /**
     * Initializes the traveler orders screen.
     * The method prepares the orders table, configures the status column,
     * updates the confirmation button according to the selected order,
     * and loads the traveler's orders from the server.
     *
     * @param url the location used to resolve relative paths
     * @param rb the resources used to localize the screen
     */
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

    /**
     * Resolves the display label shown for an order status.
     * The label may also depend on reminder confirmation flags.
     *
     * @param o the order whose status should be displayed
     * @return the label text to show in the status column
     */
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

    /**
     * Resolves the text color used for displaying an order status.
     *
     * @param o the order whose status color should be resolved
     * @return the CSS color value for the status text
     */
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

    
    /**
     * Checks whether the selected order requires traveler confirmation.
     * Confirmation is required for pending waitlist offers and for confirmed orders
     * whose reminder was sent but not yet confirmed.
     *
     * @param o the order to check
     * @return true if the order requires confirmation, otherwise false
     */
    private boolean needsConfirm(Order o) {
        if (o == null) return false;
        String status = o.getStatus();
        if ("pending".equals(status)) return true;
        if ("confirmed".equals(status) && o.isReminderSent() && !o.isReminderConfirmed()) return true;
        return false;
    }

    /**
     * Enables or disables the confirmation button according to the selected order.
     *
     * @param selected the currently selected order
     */
    private void updateConfirmButton(Order selected) {
        if (confirmBtn == null) return;
        confirmBtn.setDisable(!needsConfirm(selected));
    }

    // ─────────────────────────────────────────────
    // Actions
    // ─────────────────────────────────────────────

    /**
     * Loads all orders that belong to the currently logged-in traveler.
     * If the server is disconnected, an error message is displayed.
     */
    @FXML
    public void loadOrders() {
        if (!ClientUI.isServerConnected()) { showStatus("Server disconnected.", true); return; }
        currentAction = "LOAD";
        ClientUI.client.setHandler(this);
        String id = TravelerLoginController.getLoggedInTraveler().getIdNumber();
        ClientUI.client.sendMessage(new ClientServerMessage(Command.GET_ALL_ORDERS_BY_TRAVELER, id));
    }

    /**
     * Handles cancellation of the selected order.
     * The method validates that an order is selected, checks whether it can be cancelled,
     * asks the traveler for confirmation, and sends a cancellation request to the server.
     */
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

    /**
     * Handles confirmation of the selected order.
     * This is used for confirming a pending waitlist offer or confirming a visit reminder.
     */
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

    /**
     * Handles server responses for loading, cancelling, and confirming traveler orders.
     * The method updates the orders table and displays the relevant status message.
     *
     * @param msg the message received from the server
     */
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

    /**
     * Handles server disconnection by displaying the disconnection reason on the screen.
     *
     * @param reason the reason for the disconnection
     */
    @Override
    public void onDisconnected(String reason) {
        Platform.runLater(() -> showStatus(reason, true));
    }

    /**
     * Displays a status message on the traveler orders screen.
     * The message color is changed according to whether it represents an error or success.
     *
     * @param msg the message to display
     * @param error true if the message represents an error, otherwise false
     */
    private void showStatus(String msg, boolean error) {
        statusLabel.setText(msg);
        statusLabel.setStyle("-fx-text-fill: " + (error ? "#f87171" : "#34d399") + ";");
    }
}
