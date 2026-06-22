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

public class ParkWorkerUnorderedVisitController implements Initializable, ClientMessageHandler {
    @FXML private TextField travelerIdField, firstNameField, lastNameField;
    @FXML private TextField visitorsField, emailField, phoneField;
    @FXML private ComboBox<String> typeCombo;
    @FXML private Label priceLabel, statusLabel, availableWalkinLabel;
    private Park currentPark;
    private String currentAction;
    
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

    private void updatePrice() {
        if (currentPark == null) return;
        try {
            int visitors = Integer.parseInt(visitorsField.getText().trim());
            double price = Pricing.calculatePrice(typeCombo.getValue(), visitors, currentPark.getFullPrice(), false, false);
            priceLabel.setText(price + " NIS");
        } catch (Exception e) { priceLabel.setText("--"); }
    }

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

    private void showError(String msg) { statusLabel.setText(msg); statusLabel.setStyle("-fx-text-fill: #e94560;"); }

    @Override
    public void handleMessage(ClientServerMessage msg) {
        Platform.runLater(() -> {
            if ("LOAD_PARK".equals(currentAction) && msg.getData() instanceof Park) {
                currentPark = (Park) msg.getData();
                int walkinSpots = currentPark.getGapForWalkins();
                availableWalkinLabel.setText(String.valueOf(walkinSpots));
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

    private void clearFields() {
        travelerIdField.clear(); firstNameField.clear(); lastNameField.clear();
        visitorsField.clear(); emailField.clear(); phoneField.clear();
    }

    @Override
    public void onDisconnected(String reason) { Platform.runLater(() -> showError(reason)); }
}
