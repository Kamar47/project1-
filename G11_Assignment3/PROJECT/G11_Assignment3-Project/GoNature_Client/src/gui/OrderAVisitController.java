package gui;

import client.*;
import common.*;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.*;
import javafx.scene.control.*;
import java.net.URL;
import java.util.*;

/**
 * JavaFX controller for the Order A Visit screen (OrderVisit.fxml).
 * <p>
 * Allows a logged-in traveler to book a visit to a nature park. The traveler
 * selects the park, visit date and time, number of visitors, and order type.
 * </p>
 * <p>
 * Validation rules enforced client-side (and re-validated server-side):
 * </p>
 * <ul>
 *   <li>Individual visit: exactly 1 visitor.</li>
 *   <li>Organized group: maximum 15 visitors; only registered guides may book.</li>
 *   <li>Family booking: number of visitors may not exceed the subscriber's registered family size.</li>
 * </ul>
 * <p>
 * If the park is fully booked, the server automatically places the order on the waitlist.
 * On success, the traveler is shown the {@link OrderConfirmationController} screen with
 * the confirmation code and QR code.
 * </p>
 *
 * @author Group 11
 */
public class OrderAVisitController implements Initializable, ClientMessageHandler {
    @FXML private ComboBox<String> parkCombo, timeCombo, typeCombo;
    @FXML private DatePicker datePicker;
    @FXML private TextField visitorsField, emailField, phoneField;
    @FXML private javafx.scene.control.CheckBox paidInAdvanceCheck;
    @FXML private Label priceLabel, statusLabel;
    @FXML private javafx.scene.control.Button submitBtn;
    private ArrayList<Park> parks;
    private String currentAction;
    private Order pendingOrder;
    private boolean isSubmitting = false;
    private String pendingOrderKey = null;
    private String lastConfirmedOrderKey = null;

    /**
     * Builds a unique key for the current order details.
     * The key is used to prevent submitting the same confirmed order more than once.
     *
     * @param park the selected park
     * @param numVisitors the number of visitors in the order
     * @param email the traveler email address
     * @param phone the traveler phone number
     * @return a unique text key that represents the order details
     */
    private String buildOrderKey(Park park, int numVisitors, String email, String phone) {
        return park.getParkId() + "|" +
               datePicker.getValue() + "|" +
               timeCombo.getValue() + "|" +
               typeCombo.getValue() + "|" +
               numVisitors + "|" +
               email.trim().toLowerCase() + "|" +
               phone.trim();
    }

    /**
     * Initializes the order-a-visit screen.
     * The method prepares the park, time, visit type, date, price fields,
     * and loads the available parks from the server.
     *
     * @param url the location used to resolve relative paths
     * @param rb the resources used to localize the screen
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        timeCombo.setItems(FXCollections.observableArrayList("08:00","09:00","10:00","11:00","12:00","13:00","14:00","15:00","16:00"));
        typeCombo.setItems(FXCollections.observableArrayList("individual","family","organized_group"));
        Traveler t = TravelerLoginController.getLoggedInTraveler();
        if (t != null) {
            if (t.getEmail() != null) emailField.setText(t.getEmail());
            if (t.getPhone() != null) phoneField.setText(t.getPhone());
        }
        visitorsField.setText("1");
        visitorsField.textProperty().addListener((o, ov, nv) -> updatePrice());

        datePicker.setDayCellFactory(picker -> new javafx.scene.control.DateCell() {
            @Override
            public void updateItem(java.time.LocalDate date, boolean empty) {
                super.updateItem(date, empty);
                java.time.LocalDate today = java.time.LocalDate.now();
                if (date.isBefore(today)) {
                    setDisable(true);
                    setStyle("-fx-background-color: #2a2a3e; -fx-text-fill: #444466;");
                } else if (date.equals(today)) {
                    setStyle("-fx-background-color: #0f3d1f; -fx-text-fill: #34d399; -fx-font-weight: bold;");
                }
            }
        });
        datePicker.setValue(java.time.LocalDate.now().plusDays(1));

        javafx.application.Platform.runLater(() -> {
            if (paidInAdvanceCheck != null)
                paidInAdvanceCheck.selectedProperty().addListener((o, ov, nv) -> updatePrice());
        });
        typeCombo.valueProperty().addListener((o, ov, nv) -> {
            updatePrice();
            if (paidInAdvanceCheck != null) {
                boolean isGroup = "organized_group".equals(nv);
                paidInAdvanceCheck.setVisible(isGroup);
                paidInAdvanceCheck.setManaged(isGroup);
                if (!isGroup) paidInAdvanceCheck.setSelected(false);
            }
        });
        if (paidInAdvanceCheck != null) {
            paidInAdvanceCheck.setVisible(false);
            paidInAdvanceCheck.setManaged(false);
        }
        parkCombo.valueProperty().addListener((o, ov, nv) -> updatePrice());

        if (!ClientUI.isServerConnected()) {
            showError("Server is disconnected. Cannot load parks or create an order.");
            return;
        }
        currentAction = "LOAD_PARKS";
        ClientUI.client.setHandler(this);
        ClientUI.client.sendMessage(new ClientServerMessage(Command.GET_ALL_PARKS));
    }

    /**
     * Updates the displayed visit price according to the selected park,
     * visit type, number of visitors, subscriber status, and advance payment option.
     */
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
            boolean paidAdv = paidInAdvanceCheck != null && paidInAdvanceCheck.isSelected();
            double price = Pricing.calculatePrice(typeCombo.getValue(), visitors, park.getFullPrice(), isSub, paidAdv);
            priceLabel.setText(price + " NIS");
        } catch (NumberFormatException e) { priceLabel.setText("--"); }
    }

    /**
     * Handles submission of a new visit order.
     * The method validates all form fields, builds an order object,
     * prevents duplicate submissions, and sends the order request to the server.
     */
    @FXML
    private void handleSubmit() {
        if (isSubmitting) { showError("Order is already being submitted. Please wait."); return; }
        StringBuilder errors = new StringBuilder();

        if (!ClientUI.isServerConnected()) errors.append("• Server is disconnected.\n");

        Traveler loggedIn = TravelerLoginController.getLoggedInTraveler();
        if (loggedIn == null) errors.append("• Session expired. Please log in again.\n");

        String visitorsText = visitorsField.getText() == null ? "" : visitorsField.getText().trim();
        String emailText    = emailField.getText()    == null ? "" : emailField.getText().trim();
        String phoneText    = phoneField.getText()    == null ? "" : phoneField.getText().trim();

        if (parkCombo.getValue() == null)  errors.append("• Park is required.\n");
        if (datePicker.getValue() == null) errors.append("• Date is required.\n");
        else if (datePicker.getValue().isBefore(java.time.LocalDate.now())) errors.append("• Date cannot be in the past.\n");
        if (timeCombo.getValue() == null)  errors.append("• Time is required.\n");
        if (typeCombo.getValue() == null)  errors.append("• Visit type is required.\n");
        if (visitorsText.isEmpty())        errors.append("• Visitors is required.\n");
        if (emailText.isEmpty())           errors.append("• Email is required.\n");
        if (phoneText.isEmpty())           errors.append("• Phone is required.\n");

        int numVisitors = -1;
        if (!visitorsText.isEmpty()) {
            try {
                numVisitors = Integer.parseInt(visitorsText);
                if (numVisitors <= 0) errors.append("• Visitors must be positive.\n");
                if ("individual".equals(typeCombo.getValue()) && numVisitors != 1)
                    errors.append("• Individual visit is for exactly 1 visitor.\n");
                if ("organized_group".equals(typeCombo.getValue()) && numVisitors > 15)
                    errors.append("• Organized group limited to 15 visitors.\n");
            } catch (NumberFormatException e) { errors.append("• Visitors must be a number.\n"); }
        }

        String err = InputValidation.validateEmail(emailText);
        if (err != null) errors.append("• ").append(err).append("\n");
        err = InputValidation.validatePhone(phoneText);
        if (err != null) errors.append("• ").append(err).append("\n");

        if (datePicker.getValue() != null && timeCombo.getValue() != null) {
            if (datePicker.getValue().equals(java.time.LocalDate.now())) {
                java.time.LocalTime orderTime = java.time.LocalTime.parse(timeCombo.getValue());
                if (orderTime.isBefore(java.time.LocalTime.now()))
                    errors.append("• Cannot book for a time that already passed today.\n");
            }
        }

        if ("organized_group".equals(typeCombo.getValue())) {
            if (loggedIn == null || !loggedIn.isGuide())
                errors.append("• Only registered guides can book organized groups.\n");
        }

        if (parks == null) errors.append("• Parks not loaded yet. Please wait.\n");

        if (errors.length() > 0) { showError(errors.toString()); return; }

        Park selectedPark = parks.stream()
                .filter(p -> p.getParkName().equals(parkCombo.getValue()))
                .findFirst().orElse(null);
        if (selectedPark == null) { showError("• Park not found."); return; }

        String currentOrderKey = buildOrderKey(selectedPark, numVisitors, emailText, phoneText);
        if (currentOrderKey.equals(lastConfirmedOrderKey)) {
            showError("You did not change anything. Please change the order details and try again.");
            return;
        }

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
        order.setPaidInAdvance(paidInAdvanceCheck != null && paidInAdvanceCheck.isSelected());

        submitOrder(order, currentOrderKey);
    }

    /**
     * Sends a create order request to the server.
     * This method is used both by the regular submit action and by alternative slot selection.
     *
     * @param order the order to submit
     * @param orderKey the unique order key used for duplicate submission prevention
     */
    private void submitOrder(Order order, String orderKey) {
        pendingOrder  = order;
        currentAction = "CREATE";
        isSubmitting  = true;
        pendingOrderKey = orderKey;
        statusLabel.setText("Submitting order...");
        statusLabel.setStyle("-fx-text-fill: #f5a623;");
        if (submitBtn != null) submitBtn.setDisable(true);
        ClientUI.client.setHandler(this);
        ClientUI.client.sendMessage(new ClientServerMessage(Command.CREATE_ORDER, order));
    }

    /**
     * Displays an error message on the order-a-visit screen.
     *
     * @param msg the error message to display
     */
    private void showError(String msg) {
        statusLabel.setWrapText(true);
        statusLabel.setText(msg);
        statusLabel.setStyle("-fx-text-fill: #e94560;");
    }

    /**
     * Handles server responses related to park loading, order creation,
     * waitlist registration, and alternative visit slots.
     * The method updates the screen according to the current action.
     *
     * @param msg the message received from the server
     */
    @Override
    public void handleMessage(ClientServerMessage msg) {
        Platform.runLater(() -> {
            try {
                switch (currentAction) {

                    case "LOAD_PARKS":
                        if (msg.getData() instanceof ArrayList) {
                            parks = (ArrayList<Park>) msg.getData();
                            ArrayList<String> names = new ArrayList<>();
                            for (Park p : parks) names.add(p.getParkName());
                            parkCombo.setItems(FXCollections.observableArrayList(names));
                        }
                        break;

                    case "CREATE":
                        isSubmitting = false;
                        if (submitBtn != null) submitBtn.setDisable(false);

                        if (msg.getCommand() == Command.SUCCESS) {
                            lastConfirmedOrderKey = pendingOrderKey;
                            pendingOrderKey = null;

                            if (msg.getData() instanceof Order) {
                                Order confirmed = (Order) msg.getData();
                                if (confirmed.getParkName() == null && pendingOrder != null)
                                    confirmed.setParkName(pendingOrder.getParkName());
                                // Simulate booking confirmation notification
                                if (confirmed.getEmail() != null)
                                    NotificationSimulator.simulateBookingConfirmation(
                                        confirmed.getEmail(),
                                        confirmed.getPhone(),
                                        confirmed.getConfirmationCode(),
                                        confirmed.getParkName() != null ? confirmed.getParkName() : "Park",
                                        confirmed.getVisitDate(),
                                        confirmed.getVisitTime());
                                showConfirmationScreen(confirmed);
                            } else {
                                statusLabel.setStyle("-fx-text-fill: #00e676;");
                                statusLabel.setText("Order submitted successfully!");
                            }

                        } else if (msg.getCommand() == Command.FAILURE) {
                            String failMsg = msg.getData() != null ? msg.getData().toString() : "No availability";
                            // Reset stuck "Submitting order..." state
                            isSubmitting = false;
                            statusLabel.setText("No availability at the requested time.");
                            statusLabel.setStyle("-fx-text-fill: #f87171;");
                            showParkFullWindow(failMsg);

                        } else if (msg.getCommand() == Command.ERROR) {
                            pendingOrderKey = null;
                            showError("Server error: " + msg.getData());
                        } else {
                            pendingOrderKey = null;
                            showError("Unexpected response: " + msg.getCommand());
                        }
                        break;

                    case "ALT_SLOTS":
                        System.out.println("[Client] ALT_SLOTS case reached, cmd=" + msg.getCommand());
                        if ((msg.getCommand() == Command.ALT_SLOTS_RESPONSE
                                || msg.getCommand() == Command.DATA_RESPONSE)
                                && msg.getData() instanceof java.util.ArrayList) {
                            @SuppressWarnings("unchecked")
                            java.util.ArrayList<java.util.ArrayList<String>> slots =
                                    (java.util.ArrayList<java.util.ArrayList<String>>) msg.getData();
                            System.out.println("[Client] ALT_SLOTS count = " + slots.size());
                            // Reset status — no longer "Submitting order..."
                            isSubmitting = false;
                            statusLabel.setText("");
                            if (submitBtn != null) submitBtn.setDisable(false);
                            showAlternativeSlotsWindow(slots);
                        }
                        break;

                    case "WAITLIST":
                        isSubmitting = false;
                        if (submitBtn != null) submitBtn.setDisable(false);

                        if (msg.getCommand() == Command.SUCCESS) {
                            lastConfirmedOrderKey = pendingOrderKey;
                            pendingOrderKey = null;
                            statusLabel.setStyle("-fx-text-fill: #f5a623;");
                            if (msg.getData() instanceof Order) {
                                Order wlOrder = (Order) msg.getData();
                                statusLabel.setText("Added to waiting list! Code: " + wlOrder.getConfirmationCode());
                                // Simulate waitlist notification
                                if (wlOrder.getEmail() != null)
                                    NotificationSimulator.showNotification("Email", wlOrder.getEmail(),
                                        "GoNature - Added to Waiting List",
                                        "You have been added to the waiting list for "
                                        + (wlOrder.getParkName() != null ? wlOrder.getParkName() : "the park")
                                        + " on " + wlOrder.getVisitDate() + " at " + wlOrder.getVisitTime()
                                        + ". Code: " + wlOrder.getConfirmationCode());
                            } else {
                                statusLabel.setText("Added to waiting list!");
                            }
                        } else {
                            pendingOrderKey = null;
                            showError("Failed to join waitlist: " + msg.getData());
                        }
                        break;

                    default:
                        break;
                }
            } catch (Exception e) {
                isSubmitting = false;
                pendingOrderKey = null;
                if (submitBtn != null) submitBtn.setDisable(false);
                e.printStackTrace();
                showError("Error: " + e.getMessage());
            }
        });
    }

    /**
     * Handles server disconnection by displaying the disconnection reason on the screen.
     *
     * @param reason the reason for the disconnection
     */
    @Override
    public void onDisconnected(String reason) { Platform.runLater(() -> showError(reason)); }

    // ─────────────────────────────────────────────
    // "Park Full" window — non-blocking Stage
    // ─────────────────────────────────────────────
    /**
     * Displays a popup window when there is no availability for the requested visit time.
     * The traveler can choose to join the waiting list, view alternative times, or cancel.
     *
     * @param failMsg the failure message received from the server
     */
    private void showParkFullWindow(String failMsg) {
        javafx.stage.Stage stage = new javafx.stage.Stage();
        stage.setTitle("No Availability");
        stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);

        javafx.scene.layout.VBox box = new javafx.scene.layout.VBox(12);
        box.setStyle("-fx-padding: 24; -fx-background-color: #12121f; -fx-alignment: center;");

        Label header = new Label("No availability at the requested time");
        header.setStyle("-fx-text-fill: #f87171; -fx-font-size: 15px; -fx-font-weight: bold;");
        header.setWrapText(true);

        Label sub = new Label(failMsg + "\n\nWhat would you like to do?");
        sub.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 13px;");
        sub.setWrapText(true);

        Button waitlistBtn = new Button("⏳  Join Waiting List");
        waitlistBtn.setMaxWidth(Double.MAX_VALUE);
        waitlistBtn.setStyle("-fx-background-color: #f59e0b; -fx-text-fill: white; -fx-background-radius: 8; -fx-padding: 12; -fx-cursor: hand; -fx-font-weight: bold; -fx-font-size: 13px;");
        waitlistBtn.setOnAction(ev -> {
            stage.close();
            currentAction = "WAITLIST";
            isSubmitting   = true;
            if (submitBtn != null) submitBtn.setDisable(true);
            ClientUI.client.setHandler(OrderAVisitController.this);
            ClientUI.client.sendMessage(new ClientServerMessage(Command.ADD_TO_WAITLIST, pendingOrder));
        });

        Button altBtn = new Button("📅  See Other Times");
        altBtn.setMaxWidth(Double.MAX_VALUE);
        altBtn.setStyle("-fx-background-color: #10b981; -fx-text-fill: white; -fx-background-radius: 8; -fx-padding: 12; -fx-cursor: hand; -fx-font-weight: bold; -fx-font-size: 13px;");
        altBtn.setOnAction(ev -> {
            stage.close();
            currentAction = "ALT_SLOTS";
            ArrayList<Object> req = new ArrayList<>();
            req.add(pendingOrder.getParkId());
            req.add(pendingOrder.getVisitDate());
            req.add(pendingOrder.getVisitTime());
            req.add(pendingOrder.getNumVisitors());
            ClientUI.client.setHandler(OrderAVisitController.this);
            ClientUI.client.sendMessage(new ClientServerMessage(Command.GET_ALTERNATIVE_SLOTS, req));
        });

        Button cancelBtn = new Button("✕  Cancel");
        cancelBtn.setMaxWidth(Double.MAX_VALUE);
        cancelBtn.setStyle("-fx-background-color: #252538; -fx-text-fill: #94a3b8; -fx-background-radius: 8; -fx-padding: 12; -fx-cursor: hand; -fx-font-size: 13px;");
        cancelBtn.setOnAction(ev -> {
            stage.close();
            pendingOrderKey = null;
            if (submitBtn != null) submitBtn.setDisable(false);
            statusLabel.setText("");
        });

        box.getChildren().addAll(header, sub, waitlistBtn, altBtn, cancelBtn);

        javafx.scene.Scene scene = new javafx.scene.Scene(box, 380, 250);
        try { scene.getStylesheets().add(getClass().getResource("/styles/styles.css").toExternalForm()); }
        catch (Exception ignored) {}
        stage.setScene(scene);
        stage.show();
    }

    // ─────────────────────────────────────────────
    // Alternative slots window — non-blocking Stage
    // ─────────────────────────────────────────────
    /**
     * Displays a popup window with alternative available visit slots.
     * When the traveler selects a slot, the order date and time are updated and submitted again.
     *
     * @param slots the list of alternative slots received from the server
     */
    private void showAlternativeSlotsWindow(java.util.ArrayList<java.util.ArrayList<String>> slots) {
        if (slots.isEmpty()) {
            javafx.scene.control.Alert a = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.INFORMATION);
            a.setTitle("No Alternative Times");
            a.setHeaderText("No available slots found in the next 14 days.");
            a.setContentText("Please try a different date or join the waiting list.");
            a.show();
            return;
        }

        javafx.stage.Stage stage = new javafx.stage.Stage();
        stage.setTitle("Available Times");
        stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);

        javafx.scene.layout.VBox box = new javafx.scene.layout.VBox(10);
        box.setStyle("-fx-padding: 24; -fx-background-color: #12121f;");

        Label title = new Label("Choose an alternative time"
                + (pendingOrder != null && pendingOrder.getParkName() != null
                   ? " for " + pendingOrder.getParkName() : ""));
        title.setStyle("-fx-text-fill: #34d399; -fx-font-size: 15px; -fx-font-weight: bold;");
        title.setWrapText(true);
        box.getChildren().add(title);

        for (java.util.ArrayList<String> slot : slots) {
            String date  = slot.get(0);
            String time  = slot.get(1);
            String avail = slot.get(2);

            Button btn = new Button("📅  " + date + "   🕐  " + time
                    + "   (" + avail + " spots available)");
            btn.setMaxWidth(Double.MAX_VALUE);
            btn.setStyle("-fx-background-color: #1c1c2e; -fx-text-fill: #f1f5f9; "
                    + "-fx-border-color: #34d399; -fx-border-radius: 8; -fx-background-radius: 8; "
                    + "-fx-padding: 12; -fx-cursor: hand; -fx-font-size: 13px;");
            btn.setOnAction(ev -> {
                stage.close();
                // Update pendingOrder with the new date/time
                if (pendingOrder != null) {
                    pendingOrder.setVisitDate(date);
                    pendingOrder.setVisitTime(time + ":00");
                    // Update the form fields too
                    datePicker.setValue(java.time.LocalDate.parse(date));
                    timeCombo.setValue(time);
                    // Clear lastConfirmedOrderKey so the duplicate check doesn't block re-submit
                    lastConfirmedOrderKey = null;
                    // Re-submit — pass null key so duplicate check is skipped
                    submitOrder(pendingOrder, null);
                }
            });
            box.getChildren().add(btn);
        }

        Button cancelBtn = new Button("✕  Cancel");
        cancelBtn.setMaxWidth(Double.MAX_VALUE);
        cancelBtn.setStyle("-fx-background-color: #252538; -fx-text-fill: #94a3b8; "
                + "-fx-background-radius: 8; -fx-padding: 12; -fx-cursor: hand;");
        cancelBtn.setOnAction(ev -> stage.close());
        box.getChildren().add(cancelBtn);

        javafx.scene.Scene scene = new javafx.scene.Scene(box, 460, 80 + slots.size() * 58);
        try { scene.getStylesheets().add(getClass().getResource("/styles/styles.css").toExternalForm()); }
        catch (Exception ignored) {}
        stage.setScene(scene);
        stage.show();
    }

    /**
     * Opens the order confirmation screen after a successful order creation.
     * The confirmation screen displays the order details, confirmation code, and QR code.
     *
     * @param confirmed the confirmed order returned from the server
     */
    private void showConfirmationScreen(Order confirmed) {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                    getClass().getResource("/gui/OrderConfirmation.fxml"));
            javafx.scene.Parent root = loader.load();
            OrderConfirmationController ctrl = loader.getController();
            ctrl.setOrder(confirmed);
            javafx.scene.Scene scene = new javafx.scene.Scene(root, 460, 640);
            try { scene.getStylesheets().add(getClass().getResource("/styles/styles.css").toExternalForm()); }
            catch (Exception ex) {}
            javafx.stage.Stage stage = new javafx.stage.Stage();
            stage.setTitle("Order Confirmation");
            stage.setScene(scene);
            stage.show();
            visitorsField.clear(); emailField.clear(); phoneField.clear();
            parkCombo.setValue(null); timeCombo.setValue(null); typeCombo.setValue(null);
            datePicker.setValue(null); priceLabel.setText("--");
            statusLabel.setText("");
        } catch (Exception e) {
            statusLabel.setStyle("-fx-text-fill: #34d399;");
            statusLabel.setText("Order confirmed! Order #" + confirmed.getOrderId());
        }
    }
}
