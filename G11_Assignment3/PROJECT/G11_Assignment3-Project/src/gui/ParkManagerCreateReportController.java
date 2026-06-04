package gui;

import java.net.URL;
import java.util.ArrayList;
import java.util.ResourceBundle;

import client.ClientMessageHandler;
import client.ClientUI;
import common.ClientServerMessage;
import common.Command;
import common.worker.GeneralParkWorker;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

public class ParkManagerCreateReportController implements Initializable, ClientMessageHandler {
    @FXML private ComboBox<String> reportTypeCombo, monthCombo, yearCombo;
    @FXML private Label statusLabel, reportTitleLabel;
    @FXML private VBox reportArea;
    @FXML private TextArea reportContent;
    @FXML private TextField commentField;

    private String currentAction;
    private String lastReportData;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        reportTypeCombo.setItems(FXCollections.observableArrayList("Total Visitors Report", "Usage Report"));
        monthCombo.setItems(FXCollections.observableArrayList("1","2","3","4","5","6","7","8","9","10","11","12"));
        yearCombo.setItems(FXCollections.observableArrayList("2025","2026","2027"));
        yearCombo.setValue("2026");
    }

    @FXML
    private void handleGenerate() {
        if (reportTypeCombo.getValue() == null || monthCombo.getValue() == null) {
            statusLabel.setText("Please select report type and month."); statusLabel.setStyle("-fx-text-fill: #e94560;"); return;
        }
        GeneralParkWorker w = WorkerLoginController.getLoggedInWorker();
        ArrayList<Object> params = new ArrayList<>();
        params.add(w.getParkId());
        params.add(Integer.parseInt(monthCombo.getValue()));
        params.add(Integer.parseInt(yearCombo.getValue()));
        params.add(w.getEmployeeId());

        Command cmd = reportTypeCombo.getValue().contains("Total") ? Command.GENERATE_TOTAL_VISITORS_REPORT : Command.GENERATE_USAGE_REPORT;
        currentAction = "GENERATE";
        ClientUI.client.setHandler(this);
        ClientUI.client.sendMessage(new ClientServerMessage(cmd, params));
    }

    @FXML private void handleSave() {
        GeneralParkWorker w = WorkerLoginController.getLoggedInWorker();
        String comment = commentField.getText().trim();
        String reportData = lastReportData;
        if (comment != null && !comment.isEmpty()) {
            reportData = reportData + "\n--- Comment ---\n" + comment;
        }

        ArrayList<Object> params = new ArrayList<>();
        params.add(w.getParkId());
        params.add(reportTypeCombo.getValue().contains("Total") ? "total_visitors" : "usage");
        params.add(w.getEmployeeId());
        params.add(Integer.parseInt(monthCombo.getValue()));
        params.add(Integer.parseInt(yearCombo.getValue()));
        params.add(reportData);

        currentAction = "SAVE";
        ClientUI.client.setHandler(this);
        ClientUI.client.sendMessage(new ClientServerMessage(Command.SAVE_REPORT, params));
    }

    @FXML private void handleClose() {
        reportArea.setVisible(false); reportArea.setManaged(false);
    }

    @Override
    public void handleMessage(ClientServerMessage msg) {
        Platform.runLater(() -> {
            if ("GENERATE".equals(currentAction) && (msg.getCommand() == Command.DATA_RESPONSE || msg.getCommand() == Command.SUCCESS)) {
                String data = msg.getData() != null ? msg.getData().toString() : "No data available for this period.";
                lastReportData = data;
                reportTitleLabel.setText(reportTypeCombo.getValue() + " - Month " + monthCombo.getValue() + "/" + yearCombo.getValue());
                reportContent.setText(data);
                reportArea.setVisible(true); reportArea.setManaged(true);
                statusLabel.setText("Report generated."); statusLabel.setStyle("-fx-text-fill: #00e676;");
            } else if ("SAVE".equals(currentAction) && msg.getCommand() == Command.SUCCESS) {
                statusLabel.setText("Report saved to database successfully."); statusLabel.setStyle("-fx-text-fill: #00e676;");
                commentField.clear();
            } else if (msg.getCommand() == Command.FAILURE || msg.getCommand() == Command.ERROR) {
                statusLabel.setText("Error: " + msg.getData()); statusLabel.setStyle("-fx-text-fill: #e94560;");
            }
        });
    }
    @Override public void onDisconnected(String r) { Platform.runLater(() -> statusLabel.setText(r)); }
}