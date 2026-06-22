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
    private boolean isSubmitting = false;
    private String pendingOrderKey = null;
    private String lastConfirmedOrderKey = null;

    
    private String buildOrderKey(Park park, int numVisitors, String email, String phone) {
        return park.getParkId() + "|" +
               datePicker.getValue() + "|" +
               timeCombo.getValue() + "|" +
               typeCombo.getValue() + "|" +
               numVisitors + "|" +
               email.trim().toLowerCase() + "|" +
               phone.trim();
    }
    
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
        if (!ClientUI.isServerConnected()) {
            showError("Server is disconnected. Cannot load parks or create an order.");
            return;}
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

        if (isSubmitting) {
            showError("Order is already being submitted. Please wait.");
            return;
        }
        StringBuilder errors = new StringBuilder();

        // Server connection validation
        if (!ClientUI.isServerConnected()) {
            errors.append("• Server is disconnected. Cannot submit an order.\n");
        }

        // Guard: session must be valid
        Traveler loggedIn = TravelerLoginController.getLoggedInTraveler();
        if (loggedIn == null) {
            errors.append("• Session expired. Please log in again.\n");
        }

        String visitorsText = visitorsField.getText() == null ? "" : visitorsField.getText().trim();
        String emailText = emailField.getText() == null ? "" : emailField.getText().trim();
        String phoneText = phoneField.getText() == null ? "" : phoneField.getText().trim();

        // Required fields validation
        if (parkCombo.getValue() == null) {
            errors.append("• Park is required.\n");
        }

        if (datePicker.getValue() == null) {
            errors.append("• Date is required.\n");
        } else if (datePicker.getValue().isBefore(java.time.LocalDate.now())) {
            errors.append("• Date cannot be in the past.\n");
        }

        if (timeCombo.getValue() == null) {
            errors.append("• Time is required.\n");
        }

        if (typeCombo.getValue() == null) {
            errors.append("• Visit type is required.\n");
        }

        if (visitorsText.isEmpty()) {
            errors.append("• Visitors is required.\n");
        }

        if (emailText.isEmpty()) {
            errors.append("• Email is required.\n");
        }

        if (phoneText.isEmpty()) {
            errors.append("• Phone is required.\n");
        }

        // Visitors validation
        int numVisitors = -1;

        if (!visitorsText.isEmpty()) {
            try {
                numVisitors = Integer.parseInt(visitorsText);

                if (numVisitors <= 0) {
                    errors.append("• Visitors must be positive.\n");
                }

                if ("organized_group".equals(typeCombo.getValue()) && numVisitors > 15) {
                    errors.append("• Organized group limited to 15 visitors.\n");
                }

            } catch (NumberFormatException e) {
                errors.append("• Visitors must be a number.\n");
            }
        }

        // Email validation
        String err = InputValidation.validateEmail(emailText);
        if (err != null) {
            errors.append("• ").append(err).append("\n");
        }

        // Phone validation
        err = InputValidation.validatePhone(phoneText);
        if (err != null) {
            errors.append("• ").append(err).append("\n");
        }

        // Check if time already passed today
        if (datePicker.getValue() != null && timeCombo.getValue() != null) {
            if (datePicker.getValue().equals(java.time.LocalDate.now())) {
                java.time.LocalTime orderTime = java.time.LocalTime.parse(timeCombo.getValue());

                if (orderTime.isBefore(java.time.LocalTime.now())) {
                    errors.append("• Cannot book for a time that already passed today. Please select a later time or a future date.\n");
                }
            }
        }

        // Only registered guides can book organized groups
        if ("organized_group".equals(typeCombo.getValue())) {
            if (loggedIn == null || !loggedIn.isGuide()) {
                errors.append("• Only registered guides can book organized groups.\n");
            }
        }

        // Parks validation
        if (parks == null) {
            errors.append("• Parks not loaded yet. Please wait.\n");
        }

        // If there are errors, show all of them together
        if (errors.length() > 0) {
            showError(errors.toString());
            return;
        }

        // Find selected park
        Park selectedPark = parks.stream()
                .filter(p -> p.getParkName().equals(parkCombo.getValue()))
                .findFirst()
                .orElse(null);

        if (selectedPark == null) {
            showError("• Park not found.");
            return;
        }
        String currentOrderKey = buildOrderKey(selectedPark, numVisitors, emailText, phoneText);

        if (currentOrderKey.equals(lastConfirmedOrderKey)) {
            showError("You did not change anything. Please change the order details and then submit again.");
            return;
        }

        // Create order
        Order order = new Order();
        order.setVisitorId(loggedIn.getIdNumber());
        order.setParkId(selectedPark.getParkId());
        order.setParkName(selectedPark.getParkName());
        order.setVisitDate(datePicker.getValue().toString());
        order.setVisitTime(timeCombo.getValue() + ":00");
        order.setNumVisitors(numVisitors);
        order.setEmail(emailText);
        order.setPhone(phoneText);
        order.setOrderType(typeCombo.getValue());
        order.setSubscriberId(loggedIn.getSubscriberId());

        pendingOrder = order;
        currentAction = "CREATE";

        statusLabel.setText("Submitting order...");
        statusLabel.setStyle("-fx-text-fill: #f5a623;");

        System.out.println("[OrderVisit] Sending CREATE_ORDER: park=" + selectedPark.getParkId()
                + " date=" + order.getVisitDate()
                + " visitors=" + numVisitors);

        if (submitBtn != null) {
            submitBtn.setDisable(true);
        }
        isSubmitting = true;
        pendingOrderKey = currentOrderKey;

        if (submitBtn != null) {
            submitBtn.setDisable(true);
        }

        ClientUI.client.setHandler(this);
        ClientUI.client.sendMessage(new ClientServerMessage(Command.CREATE_ORDER, order));
    }

    private void showError(String msg) {
        statusLabel.setWrapText(true);
        statusLabel.setText(msg);
        statusLabel.setStyle("-fx-text-fill: #e94560;");
    }

    @Override
    public void handleMessage(ClientServerMessage msg) {
        System.out.println("[OrderVisit] Received: " + msg.getCommand()
                + " action=" + currentAction
                + " data=" + (msg.getData() != null ? msg.getData().getClass().getSimpleName() : "null"));

        Platform.runLater(() -> {
            try {
                switch (currentAction) {

                    case "LOAD_PARKS":
                        if (msg.getData() instanceof ArrayList) {
                            parks = (ArrayList<Park>) msg.getData();

                            ArrayList<String> names = new ArrayList<>();
                            for (Park p : parks) {
                                names.add(p.getParkName());
                            }

                            parkCombo.setItems(FXCollections.observableArrayList(names));
                            System.out.println("[OrderVisit] Loaded " + parks.size() + " parks");
                        }
                        break;

                    case "CREATE":
                        isSubmitting = false;

                        if (submitBtn != null) {
                            submitBtn.setDisable(false);
                        }

                        if (msg.getCommand() == Command.SUCCESS) {
                            lastConfirmedOrderKey = pendingOrderKey;
                            pendingOrderKey = null;

                            if (msg.getData() instanceof Order) {
                                Order confirmed = (Order) msg.getData();

                                // carry the park name over for the confirmation screen
                                if (confirmed.getParkName() == null && pendingOrder != null) {
                                    confirmed.setParkName(pendingOrder.getParkName());
                                }

                                // Simulate sending the confirmation email/SMS to the visitor
                                NotificationSimulator.simulateBookingConfirmation(
                                        confirmed.getEmail(), confirmed.getPhone(),
                                        confirmed.getConfirmationCode(), confirmed.getParkName(),
                                        confirmed.getVisitDate(), confirmed.getVisitTime());

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

                                isSubmitting = true;

                                if (submitBtn != null) {
                                    submitBtn.setDisable(true);
                                }

                                ClientUI.client.setHandler(this);
                                ClientUI.client.sendMessage(new ClientServerMessage(Command.ADD_TO_WAITLIST, pendingOrder));
                            } else {
                                pendingOrderKey = null;
                                showError("Booking cancelled.");
                            }

                        } else if (msg.getCommand() == Command.ERROR) {
                            pendingOrderKey = null;
                            showError("Server error: " + msg.getData());

                        } else {
                            pendingOrderKey = null;
                            showError("Unexpected response: " + msg.getCommand());
                        }

                        break;

                    case "WAITLIST":
                        isSubmitting = false;

                        if (submitBtn != null) {
                            submitBtn.setDisable(false);
                        }

                        if (msg.getCommand() == Command.SUCCESS) {
                            lastConfirmedOrderKey = pendingOrderKey;
                            pendingOrderKey = null;

                            statusLabel.setStyle("-fx-text-fill: #f5a623;");

                            if (msg.getData() instanceof Order) {
                                Order wlOrder = (Order) msg.getData();
                                statusLabel.setText("Added to waiting list! Code: " + wlOrder.getConfirmationCode());

                                // Simulate the "added to waiting list" notification
                                NotificationSimulator.showNotification(
                                        "Email", wlOrder.getEmail(),
                                        "GoNature - Added to Waiting List",
                                        "You have been added to the waiting list for "
                                                + (wlOrder.getParkName() != null ? wlOrder.getParkName() : "the park")
                                                + " on " + wlOrder.getVisitDate() + " at " + wlOrder.getVisitTime()
                                                + ".\nWaiting list code: " + wlOrder.getConfirmationCode()
                                                + "\nWe will notify you if a spot opens up.");
                            } else {
                                statusLabel.setText("Added to waiting list!");
                            }

                        } else {
                            pendingOrderKey = null;
                            showError("Failed to join waitlist: " + msg.getData());
                        }

                        break;

                    default:
                        System.out.println("[OrderVisit] Unhandled action: " + currentAction
                                + " cmd: " + msg.getCommand());
                        break;
                }

            } catch (Exception e) {
                isSubmitting = false;
                pendingOrderKey = null;

                if (submitBtn != null) {
                    submitBtn.setDisable(false);
                }

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