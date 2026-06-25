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

/**
 * JavaFX controller for the Park Entrance Control screen (ParkWorkerEntranceControl.fxml).
 * <p>
 * Used by park workers to admit visitors who hold a confirmed booking.
 * The worker enters the visitor's confirmation code (or scans the QR code).
 * The server validates the order (status must be {@code confirmed}, visit date must be today)
 * and, if valid:
 * </p>
 * <ol>
 *   <li>Creates a {@code park_visits} record with the entry time.</li>
 *   <li>Sets the order status to {@code in_park}.</li>
 *   <li>Increments {@code parks.current_visitors}.</li>
 * </ol>
 * <p>
 * An invoice alert is shown to the worker displaying the total amount due
 * for the entire group, calculated according to the pricing model.
 * </p>
 *
 * @author Group 11
 */
public class ParkWorkerEntranceControlController implements Initializable, ClientMessageHandler {
    @FXML private TextField visitorIdField, exitIdField, exitCountField;
    @FXML private Label statusLabel, exitStatusLabel;
    @FXML private Label orderIdLabel, parkLabel, dateLabel, timeLabel, visitorsLabel, typeLabel, priceLabel;
    @FXML private VBox orderInfoBox;
    private Order foundOrder;
    private String currentAction;

    /**
     * Initializes the park entrance control screen.
     *
     * @param url the location used to resolve relative paths
     * @param rb the resources used to localize the screen
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {}

    /**
     * Handles the check action for a visitor ID or confirmation code.
     * The method validates the input and requests the matching order details from the server.
     */
    @FXML
    private void handleCheck() {
        if (!ClientUI.isServerConnected()) {
            showStatus("Server is disconnected. Cannot check entrance.", true); return;
        }
        String input = visitorIdField.getText().trim();
        if (input.isEmpty()) { showStatus("Enter an ID or confirmation code.", true); return; }

        // If input looks like a confirmation code (contains '-'), search by code
        // Otherwise treat as visitor ID
        if (input.contains("-")) {
            currentAction = "CHECK_BY_CODE";
            ClientUI.client.setHandler(this);
            ClientUI.client.sendMessage(new ClientServerMessage(Command.GET_ORDER_BY_CODE, input.toUpperCase()));
        } else {
            String err = InputValidation.validateId(input);
            if (err != null) { showStatus(err, true); return; }
            currentAction = "CHECK";
            ClientUI.client.setHandler(this);
            ClientUI.client.sendMessage(new ClientServerMessage(Command.GET_ALL_ORDERS_BY_TRAVELER, input));
        }
    }

    /**
     * Approves visitor entry for the selected valid booking.
     * The method sends the entry details to the server and updates the visit status.
     */
    @FXML
    private void handleApproveEntry() {
    	if (!ClientUI.isServerConnected()) {
    	    showStatus("Server is disconnected. Cannot approve entry.", true);
    	    return;
    	}
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

    /**
     * Denies the current entry request and clears the displayed order details.
     */
    @FXML
    private void handleDeny() {
        orderInfoBox.setVisible(false);
        orderInfoBox.setManaged(false);
        showStatus("Entry denied.", true);
        foundOrder = null;
    }

    /**
     * Handles visitor exit from the park.
     * The method validates the visitor ID and exit count, then sends an exit request to the server.
     */
    @FXML
    private void handleExit() {
    	if (!ClientUI.isServerConnected()) {
    	    showExitStatus("Server is disconnected. Cannot process exit.", true);
    	    return;
    	}
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

    /**
     * Displays a status message for the entrance control section.
     * The message color is changed according to whether it represents an error or success.
     *
     * @param msg the message to display
     * @param error true if the message represents an error, otherwise false
     */
    private void showStatus(String msg, boolean error) {
        statusLabel.setText(msg);
        statusLabel.setStyle("-fx-text-fill: " + (error ? "#e94560" : "#00e676") + ";");
    }

    /**
     * Displays a status message for the exit processing section.
     * The message color is changed according to whether it represents an error or success.
     *
     * @param msg the message to display
     * @param error true if the message represents an error, otherwise false
     */
    private void showExitStatus(String msg, boolean error) {
        exitStatusLabel.setText(msg);
        exitStatusLabel.setStyle("-fx-text-fill: " + (error ? "#e94560" : "#00e676") + ";");
    }

    /**
     * Handles server responses for checking bookings, approving entry, and processing exit.
     * The method displays valid booking details, shows invoice information after entry approval,
     * and updates exit status messages.
     *
     * @param msg the message received from the server
     */
    @Override
    @SuppressWarnings("unchecked")
    public void handleMessage(ClientServerMessage msg) {
        Platform.runLater(() -> {
            if ("CHECK_BY_CODE".equals(currentAction) && msg.getCommand() == Command.DATA_RESPONSE
                    && msg.getData() instanceof common.Order) {
                // Searched by confirmation code — single order returned
                GeneralParkWorker w = WorkerLoginController.getLoggedInWorker();
                String today = java.time.LocalDate.now().toString();
                common.Order o = (common.Order) msg.getData();
                boolean statusOk = o.getStatus() != null && o.getStatus().equalsIgnoreCase("confirmed");
                boolean parkOk   = o.getParkId() == w.getParkId();
                boolean dateOk   = o.getVisitDate() != null && o.getVisitDate().startsWith(today);
                if (statusOk && parkOk && dateOk) {
                    foundOrder = o;
                    orderIdLabel.setText(String.valueOf(o.getOrderId()));
                    parkLabel.setText(o.getParkName() != null ? o.getParkName() : "Park #" + o.getParkId());
                    dateLabel.setText(o.getVisitDate());
                    timeLabel.setText(o.getVisitTime());
                    visitorsLabel.setText(String.valueOf(o.getNumVisitors()));
                    typeLabel.setText(o.getOrderType());
                    priceLabel.setText(o.getTotalPrice() + " NIS");
                    orderInfoBox.setVisible(true);
                    orderInfoBox.setManaged(true);
                    showStatus("Valid booking found!", false);
                } else {
                    orderInfoBox.setVisible(false);
                    orderInfoBox.setManaged(false);
                    if (!parkOk) showStatus("This booking is for a different park.", true);
                    else if (!dateOk) showStatus("This booking is not for today.", true);
                    else showStatus("Booking status is not confirmed.", true);
                }
            } else if ("CHECK_BY_CODE".equals(currentAction) && msg.getCommand() == Command.FAILURE) {
                orderInfoBox.setVisible(false);
                orderInfoBox.setManaged(false);
                showStatus("No order found with this confirmation code.", true);

            } else if ("CHECK".equals(currentAction) && msg.getCommand() == Command.DATA_RESPONSE) {
                ArrayList<Order> orders = (ArrayList<Order>) msg.getData();
                GeneralParkWorker w = WorkerLoginController.getLoggedInWorker();
                String today = java.time.LocalDate.now().toString();
                Order validOrder = null;
                for (Order o : orders) {
                    // Only 'confirmed' allowed for entry.
                    // 'pending' = waitlist offer not yet confirmed by traveler → reject.
                    boolean statusOk = o.getStatus() != null &&
                        o.getStatus().equalsIgnoreCase("confirmed");
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
                // Server returns updated Park object — show current visitors
                String parkInfo = "";
                if (msg.getData() instanceof common.Park) {
                    common.Park updatedPark = (common.Park) msg.getData();
                    parkInfo = "\nCurrent visitors in park: " + updatedPark.getCurrentVisitors();
                }
                Alert invoice = new Alert(Alert.AlertType.INFORMATION);
                invoice.setTitle("Invoice");
                invoice.setHeaderText("Entry Approved - Invoice");
                invoice.setContentText(
                    "Order #" + foundOrder.getOrderId() + "\n" +
                    "Visitors: " + foundOrder.getNumVisitors() + "\n" +
                    "Type: " + foundOrder.getOrderType() + "\n" +
                    "Total: " + foundOrder.getTotalPrice() + " NIS\n\n" +
                    "Payment is processed outside GoNature system." + parkInfo);
                invoice.showAndWait();
                showStatus("Entry approved! Visitor entered the park." + parkInfo, false);
                orderInfoBox.setVisible(false);
                orderInfoBox.setManaged(false);
                visitorIdField.clear();
                foundOrder = null;
            } else if ("EXIT".equals(currentAction) && msg.getCommand() == Command.SUCCESS) {
                // Server returns updated Park object after exit
                String exitParkInfo = "";
                if (msg.getData() instanceof common.Park) {
                    common.Park updatedPark = (common.Park) msg.getData();
                    exitParkInfo = " Current visitors: " + updatedPark.getCurrentVisitors();
                }
                showExitStatus("Exit processed successfully." + exitParkInfo, false);
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

    /**
     * Handles server disconnection by displaying the disconnection reason on the screen.
     *
     * @param reason the reason for the disconnection
     */
    @Override
    public void onDisconnected(String reason) { Platform.runLater(() -> showStatus(reason, true)); }
}