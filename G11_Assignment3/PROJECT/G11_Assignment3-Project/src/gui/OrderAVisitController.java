package gui;

import client.*;
import common.*;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.*;
import javafx.scene.control.*;
import java.net.URL;
import java.util.*;

public class OrderAVisitController implements Initializable, ClientMessageHandler {
    @FXML private ComboBox<String> parkCombo, timeCombo, typeCombo;
    @FXML private DatePicker datePicker;
    @FXML private TextField visitorsField, emailField, phoneField;
    @FXML private Label priceLabel, statusLabel;
    @FXML private javafx.scene.control.Button submitBtn;
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

        // Guard: session must be valid
        Traveler loggedIn = TravelerLoginController.getLoggedInTraveler();
        if (loggedIn == null) { showError("Session expired. Please log in again."); return; }

        // Check if all fields are empty
        boolean allEmpty = (parkCombo.getValue() == null && datePicker.getValue() == null 
            && timeCombo.getValue() == null && visitorsField.getText().trim().isEmpty() 
            && emailField.getText().trim().isEmpty() && typeCombo.getValue() == null);
        if (allEmpty) { showError("Please fill in all required fields."); return; }

        // Individual validation
        StringBuilder errors = new StringBuilder();
        if (parkCombo.getValue() == null) errors.append("Park, ");
        if (datePicker.getValue() == null) errors.append("Date, ");
        else if (datePicker.getValue().isBefore(java.time.LocalDate.now())) errors.append("Date (cannot be in the past), ");
        if (timeCombo.getValue() == null) errors.append("Time, ");
        if (visitorsField.getText().trim().isEmpty()) errors.append("Visitors, ");
        if (emailField.getText().trim().isEmpty()) errors.append("Email, ");
        if (phoneField.getText().trim().isEmpty()) errors.append("Phone, ");
        if (typeCombo.getValue() == null) errors.append("Visit type, ");

        if (errors.length() > 0) {
            errors.setLength(errors.length() - 2); // remove last ", "
            showError("Missing fields: " + errors.toString());
            return;
        }

        int numVisitors;
        try {
            numVisitors = Integer.parseInt(visitorsField.getText().trim());
            if (numVisitors <= 0) { showError("Visitors must be positive."); return; }
            if (typeCombo.getValue().equals("organized_group") && numVisitors > 15) {
                showError("Organized group limited to 15 visitors."); return;
            }
        } catch (NumberFormatException e) { showError("Visitors must be a number."); return; }

        // Bug fix: Check if time already passed when booking for today
        if (datePicker.getValue().equals(java.time.LocalDate.now())) {
            String selectedTime = timeCombo.getValue(); // e.g. "08:00"
            java.time.LocalTime orderTime = java.time.LocalTime.parse(selectedTime);
            if (orderTime.isBefore(java.time.LocalTime.now())) {
                showError("Cannot book for a time that already passed today. Please select a later time or a future date.");
                return;
            }
        }

        // Bug fix: Only registered guides can book organized groups
        if (typeCombo.getValue().equals("organized_group")) {
            Traveler traveler = TravelerLoginController.getLoggedInTraveler();
            if (!traveler.isGuide()) {
                showError("Only registered guides can book organized groups.");
                return;
            }
        }

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

        if (submitBtn != null) submitBtn.setDisable(true);
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
                        if (submitBtn != null) submitBtn.setDisable(false);
                        if (msg.getCommand() == Command.SUCCESS) {
                            if (msg.getData() instanceof Order) {
                                Order confirmed = (Order) msg.getData();
                                // carry the park name over for the confirmation screen
                                if (confirmed.getParkName() == null && pendingOrder != null) confirmed.setParkName(pendingOrder.getParkName());
                                showConfirmationScreen(confirmed);
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

    private void showConfirmationScreen(Order confirmed) {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/gui/OrderConfirmation.fxml"));
            javafx.scene.Parent root = loader.load();
            OrderConfirmationController ctrl = loader.getController();
            ctrl.setOrder(confirmed);
            javafx.scene.Scene scene = new javafx.scene.Scene(root, 460, 640);
            try { scene.getStylesheets().add(getClass().getResource("/styles/styles.css").toExternalForm()); } catch (Exception ex) {}
            javafx.stage.Stage stage = new javafx.stage.Stage();
            stage.setTitle("Order Confirmation");
            stage.setScene(scene);
            stage.show();
            // clear the form behind it
            visitorsField.clear(); emailField.clear(); phoneField.clear();
            parkCombo.setValue(null); timeCombo.setValue(null); typeCombo.setValue(null);
            datePicker.setValue(null); priceLabel.setText("--");
            statusLabel.setText("");
        } catch (Exception e) {
            // fallback to status label if FXML fails
            statusLabel.setStyle("-fx-text-fill: #34d399;");
            statusLabel.setText("Order confirmed! Order #" + confirmed.getOrderId());
        }
    }

}