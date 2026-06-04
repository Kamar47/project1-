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
import common.Park;
import common.Pricing;
import common.Traveler;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

public class OrderAVisitController implements Initializable, ClientMessageHandler {
    @FXML private ComboBox<String> parkCombo, timeCombo, typeCombo;
    @FXML private DatePicker datePicker;
    @FXML private TextField visitorsField, emailField, phoneField;
    @FXML private Label priceLabel, statusLabel;
    private ArrayList<Park> parks;
    private String currentAction;
    private Order pendingOrder;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        timeCombo.setItems(FXCollections.observableArrayList("08:00","09:00","10:00","11:00","12:00","13:00","14:00","15:00","16:00"));
        typeCombo.setItems(FXCollections.observableArrayList("individual","family","organized_group"));
        Traveler t = TravelerLoginController.getLoggedInTraveler();
        if (t != null) {
            if (t.getEmail() != null) emailField.setText(t.getEmail());
            if (t.getPhone() != null) phoneField.setText(t.getPhone());
        }
        visitorsField.textProperty().addListener((o, ov, nv) -> updatePrice());
        typeCombo.valueProperty().addListener((o, ov, nv) -> updatePrice());
        parkCombo.valueProperty().addListener((o, ov, nv) -> updatePrice());

        currentAction = "LOAD_PARKS";
        ClientUI.client.setHandler(this);
        ClientUI.client.sendMessage(new ClientServerMessage(Command.GET_ALL_PARKS));
    }

    private void updatePrice() {
        if (parks == null || parkCombo.getValue() == null || typeCombo.getValue() == null || visitorsField.getText().isEmpty()) {
            priceLabel.setText("--"); return;
        }
        try {
            Park park = parks.stream().filter(p -> p.getParkName().equals(parkCombo.getValue())).findFirst().orElse(null);
            if (park == null) return;
            int visitors = Integer.parseInt(visitorsField.getText().trim());
            Traveler t = TravelerLoginController.getLoggedInTraveler();
            boolean isSub = t != null && t.getSubscriberId() > 0;
            double price = Pricing.calculatePrice(typeCombo.getValue(), visitors, park.getFullPrice(), isSub, false);
            priceLabel.setText(price + " NIS");
        } catch (NumberFormatException e) { priceLabel.setText("--"); }
    }

    @FXML
    private void handleSubmit() {
        System.out.println("[OrderVisit] Submit clicked");

        // Validation
        if (parkCombo.getValue() == null) { showError("Please select a park."); return; }
        if (datePicker.getValue() == null) { showError("Please select a date."); return; }
        if (datePicker.getValue().isBefore(java.time.LocalDate.now())) { showError("Date cannot be in the past."); return; }
        if (timeCombo.getValue() == null) { showError("Please select a time."); return; }
        if (visitorsField.getText().trim().isEmpty()) { showError("Please enter number of visitors."); return; }
        if (emailField.getText().trim().isEmpty()) { showError("Please enter email."); return; }
        if (typeCombo.getValue() == null) { showError("Please select visit type."); return; }

        int numVisitors;
        try {
            numVisitors = Integer.parseInt(visitorsField.getText().trim());
            if (numVisitors <= 0) { showError("Visitors must be positive."); return; }
            if (typeCombo.getValue().equals("organized_group") && numVisitors > 15) {
                showError("Organized group limited to 15 visitors."); return;
            }
        } catch (NumberFormatException e) { showError("Visitors must be a number."); return; }

        if (parks == null) { showError("Parks not loaded yet. Please wait."); return; }

        Traveler t = TravelerLoginController.getLoggedInTraveler();
        Park selectedPark = parks.stream().filter(p -> p.getParkName().equals(parkCombo.getValue())).findFirst().orElse(null);
        if (selectedPark == null) { showError("Park not found."); return; }

        Order order = new Order();
        order.setVisitorId(t.getIdNumber());
        order.setParkId(selectedPark.getParkId());
        order.setParkName(selectedPark.getParkName());
        order.setVisitDate(datePicker.getValue().toString());
        order.setVisitTime(timeCombo.getValue() + ":00");
        order.setNumVisitors(numVisitors);
        order.setEmail(emailField.getText().trim());
        order.setPhone(phoneField.getText().trim());
        order.setOrderType(typeCombo.getValue());
        order.setSubscriberId(t.getSubscriberId());

        pendingOrder = order;
        currentAction = "CREATE";

        statusLabel.setText("Submitting order...");
        statusLabel.setStyle("-fx-text-fill: #f5a623;");

        System.out.println("[OrderVisit] Sending CREATE_ORDER: park=" + selectedPark.getParkId()
            + " date=" + order.getVisitDate() + " visitors=" + numVisitors);

        ClientUI.client.setHandler(this);
        ClientUI.client.sendMessage(new ClientServerMessage(Command.CREATE_ORDER, order));
    }

    private void showError(String msg) {
        statusLabel.setText(msg);
        statusLabel.setStyle("-fx-text-fill: #e94560;");
    }

    @Override
    @SuppressWarnings("unchecked")
    public void handleMessage(ClientServerMessage msg) {
        System.out.println("[OrderVisit] Received: " + msg.getCommand() + " action=" + currentAction + " data=" + (msg.getData() != null ? msg.getData().getClass().getSimpleName() : "null"));

        Platform.runLater(() -> {
            try {
                switch (currentAction) {
                    case "LOAD_PARKS":
                        if (msg.getData() instanceof ArrayList) {
                            parks = (ArrayList<Park>) msg.getData();
                            ArrayList<String> names = new ArrayList<>();
                            for (Park p : parks) names.add(p.getParkName());
                            parkCombo.setItems(FXCollections.observableArrayList(names));
                            System.out.println("[OrderVisit] Loaded " + parks.size() + " parks");
                        }
                        break;

                    case "CREATE":
                        if (msg.getCommand() == Command.SUCCESS) {
                            if (msg.getData() instanceof Order) {
                                Order confirmed = (Order) msg.getData();
                                statusLabel.setStyle("-fx-text-fill: #00e676;");
                                statusLabel.setText("Order confirmed! Code: " + confirmed.getConfirmationCode()
                                        + " | Price: " + confirmed.getTotalPrice() + " NIS");
                                NotificationSimulator.simulateBookingConfirmation(
                                    confirmed.getEmail(), confirmed.getPhone(), confirmed.getConfirmationCode(),
                                    pendingOrder.getParkName(), confirmed.getVisitDate(), confirmed.getVisitTime());
                            } else {
                                statusLabel.setStyle("-fx-text-fill: #00e676;");
                                statusLabel.setText("Order submitted successfully!");
                            }
                        } else if (msg.getCommand() == Command.FAILURE) {
                            String failMsg = msg.getData() != null ? msg.getData().toString() : "No availability";
                            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                            alert.setTitle("Park Full");
                            alert.setHeaderText("No availability at the requested time.");
                            alert.setContentText(failMsg + "\n\nWould you like to join the waiting list?");
                            ButtonType waitlistBtn = new ButtonType("Join Waiting List");
                            ButtonType cancelBtn = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
                            alert.getButtonTypes().setAll(waitlistBtn, cancelBtn);
                            Optional<ButtonType> result = alert.showAndWait();
                            if (result.isPresent() && result.get() == waitlistBtn) {
                                currentAction = "WAITLIST";
                                ClientUI.client.setHandler(this);
                                ClientUI.client.sendMessage(new ClientServerMessage(Command.ADD_TO_WAITLIST, pendingOrder));
                            } else {
                                showError("Booking cancelled.");
                            }
                        } else if (msg.getCommand() == Command.ERROR) {
                            showError("Server error: " + msg.getData());
                        } else {
                            showError("Unexpected response: " + msg.getCommand());
                        }
                        break;

                    case "WAITLIST":
                        if (msg.getCommand() == Command.SUCCESS) {
                            statusLabel.setStyle("-fx-text-fill: #f5a623;");
                            if (msg.getData() instanceof Order) {
                                Order wlOrder = (Order) msg.getData();
                                statusLabel.setText("Added to waiting list! Code: " + wlOrder.getConfirmationCode());
                            } else {
                                statusLabel.setText("Added to waiting list!");
                            }
                        } else {
                            showError("Failed to join waitlist: " + msg.getData());
                        }
                        break;

                    default:
                        System.out.println("[OrderVisit] Unhandled action: " + currentAction + " cmd: " + msg.getCommand());
                        break;
                }
            } catch (Exception e) {
                System.err.println("[OrderVisit] Error in handleMessage: " + e.getMessage());
                e.printStackTrace();
                showError("Error: " + e.getMessage());
            }
        });
    }

    @Override
    public void onDisconnected(String reason) { Platform.runLater(() -> showError(reason)); }
}