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
    private ArrayList<Park> parks;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        timeCombo.setItems(FXCollections.observableArrayList("08:00","09:00","10:00","11:00","12:00","13:00","14:00","15:00","16:00"));
        typeCombo.setItems(FXCollections.observableArrayList("individual","family","organized_group"));
        Traveler t = TravelerLoginController.getLoggedInTraveler();
        if (t != null) {
            if (t.getEmail() != null) emailField.setText(t.getEmail());
            if (t.getPhone() != null) phoneField.setText(t.getPhone());
        }
        ClientUI.client.setHandler(this);
        ClientUI.client.sendMessage(new ClientServerMessage(Command.GET_ALL_PARKS));
    }

    @FXML
    private void handleSubmit() {
        if (parkCombo.getValue() == null || datePicker.getValue() == null || timeCombo.getValue() == null
                || visitorsField.getText().isEmpty() || emailField.getText().isEmpty() || typeCombo.getValue() == null) {
            statusLabel.setText("Please fill in all fields."); return;
        }
        Traveler t = TravelerLoginController.getLoggedInTraveler();
        Order order = new Order();
        order.setVisitorId(t.getIdNumber());
        Park selectedPark = parks.stream().filter(p -> p.getParkName().equals(parkCombo.getValue())).findFirst().orElse(null);
        if (selectedPark == null) { statusLabel.setText("Park not found."); return; }
        order.setParkId(selectedPark.getParkId());
        order.setVisitDate(datePicker.getValue().toString());
        order.setVisitTime(timeCombo.getValue() + ":00");
        order.setNumVisitors(Integer.parseInt(visitorsField.getText()));
        order.setEmail(emailField.getText());
        order.setPhone(phoneField.getText());
        order.setOrderType(typeCombo.getValue());
        order.setSubscriberId(t.getSubscriberId());
        double price = Pricing.calculatePrice(order.getOrderType(), order.getNumVisitors(),
                selectedPark.getFullPrice(), t.getSubscriberId() > 0, false);
        order.setTotalPrice(price);
        ClientUI.client.setHandler(this);
        ClientUI.client.sendMessage(new ClientServerMessage(Command.CREATE_ORDER, order));
    }

    @Override
    public void handleMessage(ClientServerMessage msg) {
        Platform.runLater(() -> {
            switch (msg.getCommand()) {
                case DATA_RESPONSE:
                    if (msg.getData() instanceof ArrayList) {
                        parks = (ArrayList<Park>) msg.getData();
                        ArrayList<String> names = new ArrayList<>();
                        for (Park p : parks) names.add(p.getParkName());
                        parkCombo.setItems(FXCollections.observableArrayList(names));
                    }
                    break;
                case SUCCESS:
                    if (msg.getData() instanceof Order) {
                        Order confirmed = (Order) msg.getData();
                        statusLabel.setStyle("-fx-text-fill: #00e676;");
                        statusLabel.setText("Order confirmed! Code: " + confirmed.getConfirmationCode()
                                + " | Price: " + confirmed.getTotalPrice() + " NIS");
                    }
                    break;
                case FAILURE:
                    statusLabel.setStyle("-fx-text-fill: #e94560;");
                    statusLabel.setText("" + msg.getData());
                    break;
                default: break;
            }
        });
    }

    @Override
    public void onDisconnected(String reason) { Platform.runLater(() -> statusLabel.setText(reason)); }
}
