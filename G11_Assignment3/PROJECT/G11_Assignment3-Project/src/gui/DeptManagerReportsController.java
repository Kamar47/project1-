package gui;

import java.net.URL;
import java.util.ArrayList;
import java.util.ResourceBundle;

import client.ClientMessageHandler;
import client.ClientUI;
import common.ClientServerMessage;
import common.Command;
import common.Park;
import common.worker.GeneralParkWorker;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.layout.VBox;

public class DeptManagerReportsController implements Initializable, ClientMessageHandler {
    @FXML private ComboBox<String> reportTypeCombo, parkCombo, monthCombo;
    @FXML private Label statusLabel, reportTitle;
    @FXML private TableView<Object> reportsTable;
    @FXML private TableColumn<Object, ?> colReportId, colType, colPark, colMonth, colComment;
    @FXML private VBox reportResultBox;
    @FXML private TextArea reportData;
    private ArrayList<Park> parks;
    private String currentAction;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        reportTypeCombo.setItems(FXCollections.observableArrayList("Visit Report", "Cancellation Report"));
        monthCombo.setItems(FXCollections.observableArrayList("1","2","3","4","5","6","7","8","9","10","11","12"));
        currentAction = "LOAD_PARKS";
        ClientUI.client.setHandler(this);
        ClientUI.client.sendMessage(new ClientServerMessage(Command.GET_ALL_PARKS));
    }

    @FXML
    private void handleGenerate() {
        if (reportTypeCombo.getValue() == null || parkCombo.getValue() == null || monthCombo.getValue() == null) {
            statusLabel.setText("Please select all fields."); statusLabel.setStyle("-fx-text-fill: #e94560;"); return;
        }
        Park selected = parks.stream().filter(p -> p.getParkName().equals(parkCombo.getValue())).findFirst().orElse(null);
        if (selected == null) return;
        GeneralParkWorker w = WorkerLoginController.getLoggedInWorker();
        ArrayList<Object> params = new ArrayList<>();
        params.add(selected.getParkId());
        params.add(Integer.parseInt(monthCombo.getValue()));
        params.add(2026);
        params.add(w.getEmployeeId());
        Command cmd = reportTypeCombo.getValue().contains("Visit") ? Command.GENERATE_VISITS_REPORT : Command.GENERATE_CANCELLATION_REPORT;
        currentAction = "GENERATE";
        ClientUI.client.setHandler(this);
        ClientUI.client.sendMessage(new ClientServerMessage(cmd, params));
    }

    @FXML private void handleViewExisting() {
        currentAction = "VIEW";
        ClientUI.client.setHandler(this);
        ClientUI.client.sendMessage(new ClientServerMessage(Command.GET_ALL_REPORTS));
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
            } else if ("GENERATE".equals(currentAction)) {
                String data = msg.getData() != null ? msg.getData().toString() : "No data available.";
                reportTitle.setText(reportTypeCombo.getValue() + " - " + parkCombo.getValue() + " - Month " + monthCombo.getValue());
                reportData.setText(data);
                reportResultBox.setVisible(true); reportResultBox.setManaged(true);
                statusLabel.setText("Report generated."); statusLabel.setStyle("-fx-text-fill: #00e676;");
            }
        });
    }
    @Override public void onDisconnected(String r) { Platform.runLater(() -> statusLabel.setText(r)); }
}
