package gui;

import java.net.URL;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ResourceBundle;

import client.ClientMessageHandler;
import client.ClientUI;
import client.InputValidation;
import common.ClientServerMessage;
import common.Command;
import common.Order;
import common.Park;
import common.Pricing;
import common.worker.GeneralParkWorker;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

/**
 * JavaFX controller for the Walk-in Visit screen (ParkWorkerUnorderedVisit.fxml).
 * <p>
 * Allows park workers to register a walk-in (unplanned) visit when space is available
 * in the park's walk-in gap ({@code gap_for_walkins}). The visitor does not need a
 * prior booking. The server checks atomically whether sufficient walk-in capacity
 * remains and records the visit if so.
 * </p>
 *
 * @author Group 11
 */
public class ParkWorkerUnorderedVisitController implements Initializable, ClientMessageHandler {
    @FXML private TextField travelerIdField, firstNameField, lastNameField;
    @FXML private TextField visitorsField, emailField, phoneField;
    @FXML private ComboBox<String> typeCombo;
    @FXML private Label priceLabel, statusLabel, availableWalkinLabel;
    private Park currentPark;
    private int walkinsUsedToday = 0;
    private String currentAction;
    
    /**
     * Initializes the walk-in visit screen.
     * The method prepares the visit type options, connects field listeners for price updates,
     * and loads the assigned park details from the server.
     *
     * @param url the location used to resolve relative paths
     * @param rb the resources used to localize the screen
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        typeCombo.setItems(FXCollections.observableArrayList("walk_in", "walk_in_group"));
        typeCombo.setValue("walk_in");
        visitorsField.textProperty().addListener((o, ov, nv) -> updatePrice());
        typeCombo.valueProperty().addListener((o, ov, nv) -> updatePrice());
        if (!ClientUI.isServerConnected()) {
            showError("Server is disconnected. Cannot load park details.");
            return;
        }
        currentAction = "LOAD_PARK";
        ClientUI.client.setHandler(this);
        GeneralParkWorker w = WorkerLoginController.getLoggedInWorker();
        if (w != null) {
            ClientUI.client.sendMessage(new ClientServerMessage(Command.GET_PARK_DETAILS, w.getParkId()));
        }
    }

    /**
     * Updates the displayed walk-in visit price according to the selected visit type,
     * number of visitors, and the park's full ticket price.
     */
    private void updatePrice() {
        if (currentPark == null) return;
        try {
            int visitors = Integer.parseInt(visitorsField.getText().trim());
            double price = Pricing.calculatePrice(typeCombo.getValue(), visitors, currentPark.getFullPrice(), false, false);
            priceLabel.setText(price + " NIS");
        } catch (Exception e) { priceLabel.setText("--"); }
    }

    /**
     * Handles creation of a walk-in visit.
     * The method validates the entered traveler and visit details, calculates the price,
     * creates an order object, and sends a walk-in order request to the server.
     */
    @FXML
    private void handleOrder() {
    	if (!ClientUI.isServerConnected()) {
    	    showError("Server is disconnected. Cannot create unordered visit.");
    	    return;
    	}
    	if (currentPark == null) {
    	    showError("Park details are not loaded. Please reconnect to the server.");
    	    return;
    	}
        String err;
        if ((err = InputValidation.validateId(travelerIdField.getText())) != null) { showError(err); return; }
        int maxV = typeCombo.getValue().equals("walk_in_group") ? 15 : 999;
        if ((err = InputValidation.validateVisitors(visitorsField.getText(), maxV)) != null) { showError(err); return; }
        if ((err = InputValidation.validateEmail(emailField.getText())) != null) { showError(err); return; }

        GeneralParkWorker w = WorkerLoginController.getLoggedInWorker();
        int visitors = Integer.parseInt(visitorsField.getText().trim());
        Order order = new Order();
        order.setVisitorId(travelerIdField.getText().trim());
        order.setParkId(w.getParkId());
        order.setVisitDate(LocalDate.now().toString());
        order.setVisitTime(LocalTime.now().getHour() + ":00:00");
        order.setNumVisitors(visitors);
        order.setEmail(emailField.getText().trim());
        order.setPhone(phoneField.getText().trim());
        order.setOrderType(typeCombo.getValue());
        double price = Pricing.calculatePrice(order.getOrderType(), visitors, currentPark.getFullPrice(), false, false);
        order.setTotalPrice(price);

        currentAction = "CREATE";
        ClientUI.client.setHandler(this);
        ClientUI.client.sendMessage(new ClientServerMessage(Command.WALKIN_ORDER, order));
    }

    /**
     * Displays an error message on the walk-in visit screen.
     *
     * @param msg the error message to display
     */
    private void showError(String msg) { statusLabel.setText(msg); statusLabel.setStyle("-fx-text-fill: #e94560;"); }

    /**
     * Handles server responses for loading park details, loading today's walk-in usage,
     * and creating a walk-in order.
     * The method updates the remaining walk-in capacity, displays invoice information,
     * or shows an error message according to the server response.
     *
     * @param msg the message received from the server
     */
    @Override
    public void handleMessage(ClientServerMessage msg) {
        Platform.runLater(() -> {
            if ("LOAD_PARK".equals(currentAction) && msg.getData() instanceof Park) {
                currentPark = (Park) msg.getData();
                // Now fetch how many walk-ins already used today
                currentAction = "LOAD_WALKINS";
                ClientUI.client.setHandler(this);
                GeneralParkWorker w = WorkerLoginController.getLoggedInWorker();
                if (w != null) ClientUI.client.sendMessage(new ClientServerMessage(Command.GET_WALKINS_TODAY, w.getParkId()));
            } else if ("LOAD_WALKINS".equals(currentAction)) {
                if (msg.getData() instanceof Integer) walkinsUsedToday = (Integer) msg.getData();
                int remaining = Math.max(0, currentPark.getGapForWalkins() - walkinsUsedToday);
                availableWalkinLabel.setText(String.valueOf(remaining));
                updatePrice();
            } else if ("CREATE".equals(currentAction) && msg.getCommand() == Command.SUCCESS) {
                Order confirmed = (Order) msg.getData();
                statusLabel.setStyle("-fx-text-fill: #00e676;");
                statusLabel.setText("Walk-in processed! Code: " + confirmed.getConfirmationCode()
                        + " | Price: " + confirmed.getTotalPrice() + " NIS");
                // Show invoice
                Alert invoice = new Alert(Alert.AlertType.INFORMATION);
                invoice.setTitle("Walk-in Invoice");
                invoice.setHeaderText("Walk-in Entry - Invoice");
                invoice.setContentText("Visitors: " + confirmed.getNumVisitors() + "\nType: " + confirmed.getOrderType()
                        + "\nTotal: " + confirmed.getTotalPrice() + " NIS\n\nPayment processed outside GoNature.");
                invoice.showAndWait();
                clearFields();
                // Reload park info
                currentAction = "LOAD_PARK";
                ClientUI.client.setHandler(this);
                GeneralParkWorker w = WorkerLoginController.getLoggedInWorker();
                ClientUI.client.sendMessage(new ClientServerMessage(Command.GET_PARK_DETAILS, w.getParkId()));
            } else if (msg.getCommand() == Command.FAILURE) {
                showError("" + msg.getData());
            }
        });
    }

    /**
     * Clears all input fields after a walk-in visit is successfully processed.
     */
    private void clearFields() {
        travelerIdField.clear(); firstNameField.clear(); lastNameField.clear();
        visitorsField.clear(); emailField.clear(); phoneField.clear();
    }

    /**
     * Handles server disconnection by displaying the disconnection reason on the screen.
     *
     * @param reason the reason for the disconnection
     */
    @Override
    public void onDisconnected(String reason) { Platform.runLater(() -> showError(reason)); }
}
