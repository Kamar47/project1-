package gui;

import java.net.URL;
import java.util.ResourceBundle;

import client.ClientMessageHandler;
import client.ClientUI;
import common.ClientServerMessage;
import common.Command;
import common.Park;
import common.worker.GeneralParkWorker;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;

public class ParkWorkerCheckSpaceController implements Initializable, ClientMessageHandler {
    @FXML private Label parkNameLabel, maxVisitorsLabel, currentVisitorsLabel;
    @FXML private Label availableSpotsLabel, gapLabel, statusLabel;

    @Override
    public void initialize(URL url, ResourceBundle rb) { loadParkInfo(); }

    @FXML
    public void loadParkInfo() {
        GeneralParkWorker w = WorkerLoginController.getLoggedInWorker();
        if (w != null) {
            ClientUI.client.setHandler(this);
            ClientUI.client.sendMessage(new ClientServerMessage(Command.GET_PARK_DETAILS, w.getParkId()));
        }
    }

    @Override
    public void handleMessage(ClientServerMessage msg) {
        Platform.runLater(() -> {
            if (msg.getCommand() == Command.DATA_RESPONSE && msg.getData() instanceof Park) {
                Park park = (Park) msg.getData();
                parkNameLabel.setText(park.getParkName());
                maxVisitorsLabel.setText(String.valueOf(park.getMaxVisitors()));
                currentVisitorsLabel.setText(String.valueOf(park.getCurrentVisitors()));
                int available = park.getAvailableSpots();
                availableSpotsLabel.setText(String.valueOf(available));
                gapLabel.setText(String.valueOf(park.getGapForWalkins()));
                if (available > 0) {
                    statusLabel.setText("Open - Space available");
                    statusLabel.setStyle("-fx-text-fill: #00e676; -fx-font-weight: bold;");
                } else {
                    statusLabel.setText("Full - No space available");
                    statusLabel.setStyle("-fx-text-fill: #e94560; -fx-font-weight: bold;");
                }
            }
        });
    }

    @Override
    public void onDisconnected(String reason) {}
}
