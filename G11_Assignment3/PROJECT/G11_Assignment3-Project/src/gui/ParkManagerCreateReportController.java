package gui;

import client.*;
import common.*;
import common.worker.GeneralParkWorker;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.*;
import javafx.geometry.*;
import javafx.scene.Scene;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import java.net.URL;
import java.util.*;

public class ParkManagerCreateReportController implements Initializable, ClientMessageHandler {
    @FXML private ComboBox<String> reportTypeCombo, monthCombo, yearCombo;
    @FXML private Label statusLabel, reportTitleLabel;
    @FXML private VBox reportArea, chartArea, savedReportsBox;
    @FXML private TextArea reportContent;
    @FXML private TextField commentField;
    @FXML private TableView<ArrayList<String>> savedReportsTable;
    @FXML private TableColumn<ArrayList<String>, String> colId, colType, colMonth, colYear, colCreated, colHasComment;
    private String currentAction;
    private String lastReportData;
    private ArrayList<ArrayList<String>> savedReports;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        reportTypeCombo.setItems(FXCollections.observableArrayList("Total Visitors Report", "Usage Report"));
        monthCombo.setItems(FXCollections.observableArrayList("1","2","3","4","5","6","7","8","9","10","11","12"));
        yearCombo.setItems(FXCollections.observableArrayList("2025","2026","2027"));
        yearCombo.setValue("2026");

        if (savedReportsTable != null) {
            colId.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(d.getValue().get(0)));
            colType.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(d.getValue().get(1)));
            colMonth.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(d.getValue().get(2)));
            colYear.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(d.getValue().get(3)));
            colCreated.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(d.getValue().get(4)));
            colHasComment.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(
                d.getValue().get(5).contains("--- Comment ---") ? "Yes" : "No"));

            // Double click to open report
            savedReportsTable.setRowFactory(tv -> {
                TableRow<ArrayList<String>> row = new TableRow<>();
                row.setOnMouseClicked(event -> {
                    if (event.getClickCount() == 2 && !row.isEmpty()) {
                        openSavedReport(row.getItem());
                    }
                });
                return row;
            });
        }
    }

    @FXML
    private void handleViewSaved() {
        GeneralParkWorker w = WorkerLoginController.getLoggedInWorker();
        currentAction = "VIEW_SAVED";
        ClientUI.client.setHandler(this);
        ClientUI.client.sendMessage(new ClientServerMessage(Command.GET_ALL_REPORTS, w.getParkId()));
    }

    private void openSavedReport(ArrayList<String> report) {
        // report: [id, type, month, year, created, data]
        String reportType = report.get(1);
        String month = report.get(2);
        String fullData = report.get(5);

        // Split report data from comment
        String reportData = fullData;
        String comment = null;
        if (fullData.contains("--- Comment ---")) {
            String[] parts = fullData.split("--- Comment ---");
            reportData = parts[0].trim();
            comment = parts[1].trim();
        }

        Stage popup = new Stage();
        popup.setTitle("Saved Report #" + report.get(0));

        VBox root = new VBox(12);
        root.setAlignment(Pos.CENTER);
        root.setStyle("-fx-background-color: #12121f; -fx-padding: 25;");

        // Title
        String displayType = reportType.equals("total_visitors") ? "Visitors Report" : "Usage Report";
        Label title = new Label(displayType + " for month " + month);
        title.setStyle("-fx-text-fill: #34d399; -fx-font-size: 18px; -fx-font-weight: bold; -fx-underline: true;");

        Label created = new Label("Created: " + report.get(4));
        created.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 11px;");

        root.getChildren().addAll(title, created);

        // Rebuild the same chart
        if (reportType.equals("total_visitors")) {
            // Parse counts and build pie chart + legend (same as generate)
            Map<String, Integer> typeCounts = new LinkedHashMap<>();
            typeCounts.put("Individual", 0);
            typeCounts.put("Family", 0);
            typeCounts.put("Group", 0);
            for (String line : reportData.split("\n")) {
                line = line.trim();
                if (line.contains(":") && !line.startsWith("===") && !line.startsWith("TOTAL")) {
                    try {
                        String type = line.split(":")[0].trim();
                        int count = Integer.parseInt(line.split(":")[1].trim().split(" ")[0]);
                        String displayName;
                        switch (type) {
                            case "individual": displayName = "Individual"; break;
                            case "family": displayName = "Family"; break;
                            case "organized_group": displayName = "Group"; break;
                            case "walk_in": displayName = "Walk-in"; break;
                            case "walk_in_group": displayName = "Walk-in Group"; break;
                            default: displayName = type; break;
                        }
                        typeCounts.put(displayName, typeCounts.getOrDefault(displayName, 0) + count);
                    } catch (Exception e) {}
                }
            }

            PieChart pieChart = new PieChart();
            pieChart.setLabelsVisible(false);
            pieChart.setLegendVisible(false);
            pieChart.setPrefHeight(280);
            pieChart.setPrefWidth(350);
            pieChart.setStyle("-fx-background-color: transparent;");
            try { pieChart.getStylesheets().add(getClass().getResource("/styles/charts.css").toExternalForm()); } catch (Exception e) {}
            for (Map.Entry<String, Integer> e : typeCounts.entrySet())
                if (e.getValue() > 0) pieChart.getData().add(new PieChart.Data(e.getKey(), e.getValue()));

            String[] legendColors = {"#e94560", "#2d6a4f", "#0f3460", "#f5a623"};
            HBox legendBox = new HBox(20);
            legendBox.setAlignment(Pos.CENTER);
            int colorIdx = 0;
            for (Map.Entry<String, Integer> e : typeCounts.entrySet()) {
                String color;
                if (e.getValue() > 0) {
                    color = legendColors[colorIdx % legendColors.length];
                    colorIdx++;
                } else {
                    color = "#64748b";
                }
                Label dot = new Label("\u25CF");
                dot.setStyle("-fx-text-fill: " + color + "; -fx-font-size: 16px;");
                Label text = new Label(e.getKey() + " " + e.getValue());
                text.setStyle("-fx-text-fill: white; -fx-font-size: 13px; -fx-font-weight: bold;");
                HBox item = new HBox(5, dot, text);
                item.setAlignment(Pos.CENTER);
                legendBox.getChildren().add(item);
            }
            int total = typeCounts.values().stream().mapToInt(i -> i).sum();
            Label totalLabel = new Label("Total Visitors: " + total);
            totalLabel.setStyle("-fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold;");

            root.getChildren().addAll(pieChart, legendBox, totalLabel);
        } else {
            // Usage report - bar chart
            List<String> fullDays = new ArrayList<>();
            List<String> notFullDays = new ArrayList<>();
            for (String line : reportData.split("\n")) {
                line = line.trim();
                if (line.contains("[FULL]") && !line.contains("[NOT FULL]")) fullDays.add(line.split(":")[0].trim());
                else if (line.contains("[NOT FULL]")) notFullDays.add(line.split(":")[0].trim());
            }
            CategoryAxis xAxis = new CategoryAxis();
            NumberAxis yAxis = new NumberAxis();
            xAxis.setLabel("Status"); yAxis.setLabel("Days");
            BarChart<String, Number> barChart = new BarChart<>(xAxis, yAxis);
            barChart.setLegendVisible(false);
            barChart.setCategoryGap(60);
            barChart.setPrefHeight(260);
            try { barChart.getStylesheets().add(getClass().getResource("/styles/charts.css").toExternalForm()); } catch (Exception e) {}
            XYChart.Series<String, Number> series = new XYChart.Series<>();
            series.getData().add(new XYChart.Data<>("Full Capacity", fullDays.size()));
            series.getData().add(new XYChart.Data<>("Not Full", notFullDays.size()));
            barChart.getData().add(series);
            Label summary = new Label("Full days: " + fullDays.size() + "  |  Not full days: " + notFullDays.size());
            summary.setStyle("-fx-text-fill: white; -fx-font-size: 13px; -fx-font-weight: bold;");
            root.getChildren().addAll(barChart, summary);
        }

        // Show comment if exists
        if (comment != null && !comment.isEmpty()) {
            Label commentTitle = new Label("Comment:");
            commentTitle.setStyle("-fx-text-fill: #34d399; -fx-font-size: 13px; -fx-font-weight: bold;");
            Label commentText = new Label(comment);
            commentText.setStyle("-fx-text-fill: #fbbf24; -fx-font-size: 13px; -fx-font-style: italic;");
            commentText.setWrapText(true);
            commentText.setMaxWidth(400);
            root.getChildren().addAll(commentTitle, commentText);
        }

        Button closeBtn = new Button("Close window");
        closeBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #f87171; -fx-border-color: #f87171; -fx-border-radius: 8; -fx-background-radius: 8; -fx-padding: 8 25;");
        closeBtn.setOnAction(e -> popup.close());
        root.getChildren().add(closeBtn);

        popup.setScene(new Scene(root, 520, 640));
        popup.show();
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
        String comment = commentField != null ? commentField.getText().trim() : "";
        String reportData = lastReportData;
        if (!comment.isEmpty()) reportData = reportData + "\n--- Comment ---\n" + comment;
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
    @SuppressWarnings("unchecked")
    public void handleMessage(ClientServerMessage msg) {
        Platform.runLater(() -> {
            if ("GENERATE".equals(currentAction) && (msg.getCommand() == Command.DATA_RESPONSE || msg.getCommand() == Command.SUCCESS)) {
                String data = msg.getData() != null ? msg.getData().toString() : "No data available.";
                lastReportData = data;
                statusLabel.setText("Report generated."); statusLabel.setStyle("-fx-text-fill: #00e676;");

                if (reportTypeCombo.getValue().contains("Total")) {
                    openVisitorsReportWindow(data);
                } else {
                    openUsageReportWindow(data);
                }
            } else if ("VIEW_SAVED".equals(currentAction)) {
                if (msg.getData() instanceof ArrayList) {
                    savedReports = (ArrayList<ArrayList<String>>) msg.getData();
                    savedReportsTable.setItems(FXCollections.observableArrayList(savedReports));
                    savedReportsBox.setVisible(true); savedReportsBox.setManaged(true);
                    statusLabel.setText(savedReports.size() + " saved report(s) found.");
                    statusLabel.setStyle("-fx-text-fill: #34d399;");
                }
            } else if ("SAVE".equals(currentAction) && msg.getCommand() == Command.SUCCESS) {
                statusLabel.setText("Report saved to database."); statusLabel.setStyle("-fx-text-fill: #00e676;");
            } else if (msg.getCommand() == Command.FAILURE || msg.getCommand() == Command.ERROR) {
                statusLabel.setText("Error: " + msg.getData()); statusLabel.setStyle("-fx-text-fill: #e94560;");
            }
        });
    }

    // ========== NEW WINDOW: Total Visitors Pie Chart ==========
    private void openVisitorsReportWindow(String data) {
        Map<String, Integer> typeCounts = new LinkedHashMap<>();
        typeCounts.put("Individual", 0);
        typeCounts.put("Family", 0);
        typeCounts.put("Group", 0);

        for (String line : data.split("\n")) {
            line = line.trim();
            if (line.contains(":") && !line.startsWith("===") && !line.startsWith("TOTAL")) {
                try {
                    String type = line.split(":")[0].trim();
                    int count = Integer.parseInt(line.split(":")[1].trim().split(" ")[0]);
                    String displayName;
                    switch (type) {
                        case "individual": displayName = "Individual"; break;
                        case "family": displayName = "Family"; break;
                        case "organized_group": displayName = "Group"; break;
                        case "walk_in": displayName = "Walk-in"; break;
                        case "walk_in_group": displayName = "Walk-in Group"; break;
                        default: displayName = type; break;
                    }
                    typeCounts.put(displayName, typeCounts.getOrDefault(displayName, 0) + count);
                } catch (Exception e) {}
            }
        }

        Stage popup = new Stage();
        popup.setTitle("Visitors Report");

        VBox root = new VBox(15);
        root.setAlignment(Pos.CENTER);
        root.setStyle("-fx-background-color: #1a1a2e; -fx-padding: 30;");

        Label title = new Label("Visitors Report for month " + monthCombo.getValue());
        title.setStyle("-fx-text-fill: #2d6a4f; -fx-font-size: 18px; -fx-font-weight: bold; -fx-underline: true;");

        // Pie Chart - no built-in legend
        PieChart pieChart = new PieChart();
        pieChart.setLabelsVisible(false);
        pieChart.setLegendVisible(false);
        pieChart.setPrefHeight(320);
        pieChart.setPrefWidth(380);
        pieChart.setStyle("-fx-background-color: transparent;");
        try { pieChart.getStylesheets().add(getClass().getResource("/styles/charts.css").toExternalForm()); } catch (Exception e) {}

        for (Map.Entry<String, Integer> e : typeCounts.entrySet()) {
            if (e.getValue() > 0) {
                pieChart.getData().add(new PieChart.Data(e.getKey(), e.getValue()));
            }
        }

        // Custom legend - colors match pie slice order (only non-zero get pie colors)
        String[] legendColors = {"#e94560", "#2d6a4f", "#0f3460", "#f5a623", "#9b59b6"};
        HBox legendBox = new HBox(20);
        legendBox.setAlignment(Pos.CENTER);
        legendBox.setStyle("-fx-padding: 10;");
        int colorIdx = 0;
        for (Map.Entry<String, Integer> e : typeCounts.entrySet()) {
            String color;
            if (e.getValue() > 0) {
                color = legendColors[colorIdx % legendColors.length];
                colorIdx++;  // only advance color for slices that exist in the pie
            } else {
                color = "#64748b";  // grey dot for zero (not in pie)
            }
            Label dot = new Label("●");
            dot.setStyle("-fx-text-fill: " + color + "; -fx-font-size: 18px;");
            Label text = new Label(e.getKey() + " " + e.getValue());
            text.setStyle("-fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold;");
            HBox item = new HBox(5, dot, text);
            item.setAlignment(Pos.CENTER);
            legendBox.getChildren().add(item);
        }

        // Total
        int total = typeCounts.values().stream().mapToInt(i -> i).sum();
        Label totalLabel = new Label("Total Visitors: " + total);
        totalLabel.setStyle("-fx-text-fill: white; -fx-font-size: 15px; -fx-font-weight: bold;");

        // Comment
        Label commentLabel = new Label("Add comments to the report");
        commentLabel.setStyle("-fx-text-fill: #2d6a4f; -fx-font-size: 13px;");
        TextField popupComment = new TextField();
        popupComment.setMaxWidth(350);
        popupComment.setStyle("-fx-background-color: #16213e; -fx-text-fill: white; -fx-border-color: #2d6a4f; -fx-border-radius: 5; -fx-background-radius: 5; -fx-padding: 8;");

        // Buttons
        Button closeBtn = new Button("Close window");
        closeBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #e94560; -fx-border-color: #e94560; -fx-border-radius: 8; -fx-background-radius: 8; -fx-padding: 8 25; -fx-font-size: 12px;");
        closeBtn.setOnAction(e -> popup.close());
        Button saveBtn = new Button("Save report");
        saveBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #2d6a4f; -fx-border-color: #2d6a4f; -fx-border-radius: 8; -fx-background-radius: 8; -fx-padding: 8 25; -fx-font-size: 12px;");
        saveBtn.setOnAction(e -> {
            if (!popupComment.getText().isEmpty()) lastReportData = lastReportData + "\n--- Comment ---\n" + popupComment.getText();
            handleSave();
            popup.close();
        });
        HBox buttons = new HBox(30, closeBtn, saveBtn);
        buttons.setAlignment(Pos.CENTER);

        root.getChildren().addAll(title, pieChart, legendBox, totalLabel, commentLabel, popupComment, buttons);
        Scene scene = new Scene(root, 500, 650);
        popup.setScene(scene);
        popup.show();
    }

    // ========== NEW WINDOW: Usage Report ==========
    private void openUsageReportWindow(String data) {
        List<String> fullDays = new ArrayList<>();
        List<String> notFullDays = new ArrayList<>();

        for (String line : data.split("\n")) {
            line = line.trim();
            if (line.contains("[FULL]") && !line.contains("[NOT FULL]")) {
                fullDays.add(line.split(":")[0].trim());
            } else if (line.contains("[NOT FULL]")) {
                notFullDays.add(line.split(":")[0].trim());
            }
        }

        Stage popup = new Stage();
        popup.setTitle("Usage Report");

        VBox root = new VBox(15);
        root.setAlignment(Pos.CENTER);
        root.setStyle("-fx-background-color: #1a1a2e; -fx-padding: 30;");

        Label title = new Label("Usage Report for month " + monthCombo.getValue());
        title.setStyle("-fx-text-fill: #2d6a4f; -fx-font-size: 18px; -fx-font-weight: bold; -fx-underline: true;");

        // Bar chart
        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();
        xAxis.setLabel("Status"); yAxis.setLabel("Days");
        BarChart<String, Number> barChart = new BarChart<>(xAxis, yAxis);
        barChart.setTitle("Park Usage");
        barChart.setLegendVisible(false);
        barChart.setCategoryGap(60);
        barChart.setPrefHeight(300);
        try { barChart.getStylesheets().add(getClass().getResource("/styles/charts.css").toExternalForm()); } catch (Exception e) {}

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.getData().add(new XYChart.Data<>("Full Capacity", fullDays.size()));
        series.getData().add(new XYChart.Data<>("Not Full", notFullDays.size()));
        barChart.getData().add(series);

        Label summary = new Label("Full days: " + fullDays.size() + "  |  Not full days: " + notFullDays.size());
        summary.setStyle("-fx-text-fill: #2d6a4f; -fx-font-size: 14px; -fx-font-weight: bold;");

        // Comment + buttons
        Label commentLabel = new Label("Add comments to the report");
        commentLabel.setStyle("-fx-text-fill: #2d6a4f; -fx-font-size: 13px;");

        TextField popupComment = new TextField();
        popupComment.setMaxWidth(350);
        popupComment.setStyle("-fx-background-color: #16213e; -fx-text-fill: #e0e0f0; -fx-border-color: #2d6a4f; -fx-border-radius: 5; -fx-background-radius: 5; -fx-padding: 8;");

        Button closeBtn = new Button("Close window");
        closeBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #e94560; -fx-border-color: #e94560; -fx-border-radius: 8; -fx-background-radius: 8; -fx-padding: 8 25;");
        closeBtn.setOnAction(e -> popup.close());

        Button saveBtn = new Button("Save report");
        saveBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #2d6a4f; -fx-border-color: #2d6a4f; -fx-border-radius: 8; -fx-background-radius: 8; -fx-padding: 8 25;");
        saveBtn.setOnAction(e -> {
            if (!popupComment.getText().isEmpty()) lastReportData = lastReportData + "\n--- Comment ---\n" + popupComment.getText();
            handleSave();
            popup.close();
        });

        HBox buttons = new HBox(30, closeBtn, saveBtn);
        buttons.setAlignment(Pos.CENTER);

        root.getChildren().addAll(title, barChart, summary, commentLabel, popupComment, buttons);

        Scene scene = new Scene(root, 500, 600);
        popup.setScene(scene);
        popup.show();
    }

    @Override
    public void onDisconnected(String r) { Platform.runLater(() -> statusLabel.setText(r)); }
}