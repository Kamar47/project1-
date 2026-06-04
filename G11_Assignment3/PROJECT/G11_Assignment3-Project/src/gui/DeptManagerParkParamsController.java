package gui;

import java.net.URL;
import java.util.ArrayList;
import java.util.ResourceBundle;

import client.ClientMessageHandler;
import client.ClientUI;
import common.ClientServerMessage;
import common.Command;
import common.Park;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

public class DeptManagerParkParamsController implements Initializable, ClientMessageHandler {
    @FXML private ComboBox<String> parkCombo;
    @FXML private VBox paramsBox;
    @FXML private Label nameLabel, currentLabel, maxLabel, gapLabel, capacityLabel, stayLabel, statusLabel;
    private ArrayList<Park> parks;
    private String currentAction;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        currentAction = "LOAD_PARKS";
        ClientUI.client.setHandler(this);
        ClientUI.client.sendMessage(new ClientServerMessage(Command.GET_ALL_PARKS));
    }

    @FXML
    private void handleGetParams() {
        if (parkCombo.getValue() == null) { statusLabel.setText("Select a park."); return; }
        Park selected = parks.stream().filter(p -> p.getParkName().equals(parkCombo.getValue())).findFirst().orElse(null);
        if (selected == null) return;
        currentAction = "LOAD_DETAILS";
        ClientUI.client.setHandler(this);
        ClientUI.client.sendMessage(new ClientServerMessage(Command.GET_PARK_DETAILS, selected.getParkId()));
    }

    @Override
    @SuppressWarnings("unchecked")
    public void handleMessage(ClientServerMessage msg) {
        Platform.runLater(() -> {
            if ("LOAD_PARKS".equals(currentAction) && msg.getData() instanceof ArrayList) {
                parks = (ArrayList<Park>) msg.getData();
                ArrayList<String> names = new ArrayList<>();
                for (Park p : parks) names.add(p.getParkName());
                parkCombo.setItems(FXCollections.observableArrayList(names));
            } else if ("LOAD_DETAILS".equals(currentAction) && msg.getData() instanceof Park) {
                Park p = (Park) msg.getData();
                nameLabel.setText(p.getParkName());
                currentLabel.setText(String.valueOf(p.getCurrentVisitors()));
                maxLabel.setText(String.valueOf(p.getMaxVisitors()));
                gapLabel.setText(String.valueOf(p.getGapForWalkins()));
                capacityLabel.setText(String.valueOf(p.getMaxVisitors() - p.getGapForWalkins()));
                stayLabel.setText(p.getEstimatedVisitDuration() + " hours");
                paramsBox.setVisible(true); paramsBox.setManaged(true);
            }
        });
    }
    @Override public void onDisconnected(String r) { Platform.runLater(() -> statusLabel.setText(r)); }
}
