package gui;

import client.*;
import common.*;
import common.worker.GeneralParkWorker;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.*;
import javafx.scene.control.*;
import java.net.URL;
import java.time.LocalDate;
import java.util.*;

public class ParkManagerPromotionsController implements Initializable, ClientMessageHandler {
    @FXML private TextField discountField, descField;
    @FXML private DatePicker startDatePicker, endDatePicker;
    @FXML private Label statusLabel;
    @FXML private TableView<ArrayList<String>> promotionsTable;
    @FXML private TableColumn<ArrayList<String>, String> colId, colDiscount, colStart, colEnd, colDesc, colStatus;
    private String currentAction;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        colId.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().get(0)));
        colDiscount.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().get(1) + "%"));
        colStart.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().get(2)));
        colEnd.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().get(3)));
        colDesc.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().get(4)));
        colStatus.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().get(5)));

        // Color-code status column
        colStatus.setCellFactory(col -> new TableCell<ArrayList<String>, String>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setStyle(""); }
                else {
                    setText(item);
                    switch (item.toLowerCase()) {
                        case "approved": setStyle("-fx-text-fill: #34d399; -fx-font-weight: bold;"); break;
                        case "rejected": setStyle("-fx-text-fill: #f87171; -fx-font-weight: bold;"); break;
                        case "pending": setStyle("-fx-text-fill: #fbbf24; -fx-font-weight: bold;"); break;
                        default: setStyle("-fx-text-fill: #f1f5f9;");
                    }
                }
            }
        });

        if (!ClientUI.isServerConnected()) {
            showStatus("Server disconnected. Cannot create or load promotions.", true);
            return;
        }

        loadPromotions();
    }

    @FXML
    private void handleSubmit() {
    	if (!ClientUI.isServerConnected()) {
            showStatus("Server disconnected. Cannot submit promotion request.", true);
            return;
        }
        String discountStr = discountField.getText().trim();
        LocalDate start = startDatePicker.getValue();
        LocalDate end = endDatePicker.getValue();
        String desc = descField.getText().trim();

        // Validation
        if (discountStr.isEmpty()) { showError("Enter a discount percentage."); return; }
        if (start == null) { showError("Select a start date."); return; }
        if (end == null) { showError("Select an end date."); return; }

        double discount;
        try {
            discount = Double.parseDouble(discountStr);
            if (discount <= 0 || discount >= 100) { showError("Discount must be between 1 and 99."); return; }
        } catch (NumberFormatException e) { showError("Discount must be a number."); return; }

        // Date validation - cannot be in the past
        if (start.isBefore(LocalDate.now())) { showError("Start date cannot be in the past."); return; }
        if (end.isBefore(start)) { showError("End date must be after start date."); return; }
        if (end.isBefore(LocalDate.now())) { showError("End date cannot be in the past."); return; }

        GeneralParkWorker w = WorkerLoginController.getLoggedInWorker();
        ArrayList<Object> params = new ArrayList<>();
        params.add(w.getParkId());
        params.add(discount);
        params.add(start.toString());
        params.add(end.toString());
        params.add(desc.isEmpty() ? "Promotion" : desc);
        params.add(w.getEmployeeId());

        currentAction = "SUBMIT";
        ClientUI.client.setHandler(this);
        ClientUI.client.sendMessage(new ClientServerMessage(Command.CREATE_PROMOTION, params));
    }

    private void showError(String msg) {
        showStatus(msg, true);
    }

    @FXML private void handleRefresh() { loadPromotions(); }

    private void loadPromotions() {
        if (!ClientUI.isServerConnected()) {
            showStatus("Server disconnected. Cannot load promotions.", true);
            return;
        }
        GeneralParkWorker w = WorkerLoginController.getLoggedInWorker();
        if (w == null) {
            showStatus("Worker session expired. Please log in again.", true);
            return;
        }
        currentAction = "LOAD";
        ClientUI.client.setHandler(this);
        ClientUI.client.sendMessage(new ClientServerMessage(Command.GET_MY_PROMOTIONS, w.getParkId()));
    }

    @Override
    @SuppressWarnings("unchecked")
    public void handleMessage(ClientServerMessage msg) {
        Platform.runLater(() -> {
            if ("SUBMIT".equals(currentAction)) {
                if (msg.getCommand() == Command.SUCCESS) {
                    statusLabel.setText("Promotion request submitted for approval.");
                    statusLabel.setStyle("-fx-text-fill: #34d399;");
                    discountField.clear(); descField.clear();
                    startDatePicker.setValue(null); endDatePicker.setValue(null);
                    new Thread(() -> {
                        try { Thread.sleep(400); } catch (InterruptedException e) {}
                        Platform.runLater(this::loadPromotions);
                    }).start();
                } else {
                    showError("Error: " + msg.getData());
                }
            } else if ("LOAD".equals(currentAction)) {
                if (msg.getData() instanceof ArrayList) {
                    ArrayList<ArrayList<String>> promos = (ArrayList<ArrayList<String>>) msg.getData();
                    promotionsTable.setItems(FXCollections.observableArrayList(promos));
                }
            }
        });
    }
    private void showStatus(String msg, boolean error) {
        statusLabel.setWrapText(true);
        statusLabel.setMaxWidth(700);
        statusLabel.setText(msg);
        statusLabel.setStyle("-fx-text-fill: " + (error ? "#e94560" : "#00e676") + ";");
    }

    @Override
    public void onDisconnected(String r) {
        Platform.runLater(() -> {
            showStatus("Server disconnected. Cannot create or load promotions.", true);
        });
    }
}