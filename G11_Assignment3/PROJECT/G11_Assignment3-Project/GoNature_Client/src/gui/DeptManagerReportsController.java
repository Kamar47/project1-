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

/**
 * JavaFX controller for the Department Manager Reports screen (DeptManagerReports.fxml).
 * <p>
 * Allows the department manager to generate and view reports across all parks
 * or for a specific park. Supports the same four report types as
 * {@link ParkManagerCreateReportController}: visits, cancellations,
 * total visitors, and usage.
 * </p>
 *
 * @author Group 11
 */
public class DeptManagerReportsController implements Initializable, ClientMessageHandler {
    @FXML private ComboBox<String> reportTypeCombo, parkCombo, monthCombo, yearCombo;
    @FXML private Label statusLabel;
    @FXML private VBox reportBox;
    private ArrayList<Park> parks;
    private String currentAction;

    /**
     * Initializes the department manager reports screen.
     * The method prepares the report type, month, and year selection fields,
     * checks the server connection, and loads the list of parks from the server.
     *
     * @param url the location used to resolve relative paths
     * @param rb the resources used to localize the screen
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        reportTypeCombo.setItems(FXCollections.observableArrayList("Visit Report", "Cancellation Report"));
        monthCombo.setItems(FXCollections.observableArrayList("1","2","3","4","5","6","7","8","9","10","11","12"));
        yearCombo.setItems(FXCollections.observableArrayList("2025","2026","2027"));
        yearCombo.setValue("2026");
        if (!ClientUI.isServerConnected()) {
            showStatus("Server disconnected. Cannot load or generate reports.", true);
            return;
        }
        currentAction = "LOAD_PARKS";
        ClientUI.client.setHandler(this);
        ClientUI.client.sendMessage(new ClientServerMessage(Command.GET_ALL_PARKS));
    }

    /**
     * Handles report generation according to the selected report type, park, month, and year.
     * The method validates the required selections and sends the appropriate report request
     * to the server.
     */
    @FXML
    private void handleGenerate() {
    	if (!ClientUI.isServerConnected()) {
            showStatus("Server disconnected. Cannot generate report.", true);
            return;
        }
        if (reportTypeCombo.getValue() == null || parkCombo.getValue() == null || monthCombo.getValue() == null) {
            statusLabel.setText("Please select all fields."); statusLabel.setStyle("-fx-text-fill: #e94560;"); return;
        }
        if (parks == null || parks.isEmpty()) {
            showStatus("Park list is not loaded. Please reconnect to the server.", true);
            return;
        }
        Park selected = parks.stream().filter(p -> p.getParkName().equals(parkCombo.getValue())).findFirst().orElse(null);
        if (selected == null) return;
        GeneralParkWorker w = WorkerLoginController.getLoggedInWorker();
        ArrayList<Object> params = new ArrayList<>();
        params.add(selected.getParkId());
        params.add(Integer.parseInt(monthCombo.getValue()));
        params.add(Integer.parseInt(yearCombo.getValue()));
        params.add(w.getEmployeeId());
        Command cmd = reportTypeCombo.getValue().contains("Visit") ? Command.GENERATE_VISITS_REPORT : Command.GENERATE_CANCELLATION_REPORT;
        currentAction = "GENERATE";
        ClientUI.client.setHandler(this);
        ClientUI.client.sendMessage(new ClientServerMessage(cmd, params));
    }

    /**
     * Requests all saved reports from the server and displays them on the screen.
     */
    @FXML
    private void handleViewExisting() {
    	if (!ClientUI.isServerConnected()) {
            showStatus("Server disconnected. Cannot view saved reports.", true);
            return;
        }
        currentAction = "VIEW_REPORTS";
        ClientUI.client.setHandler(this);
        ClientUI.client.sendMessage(new ClientServerMessage(Command.GET_ALL_REPORTS));
    }

    /**
     * Handles server responses related to park loading, report generation,
     * and saved report retrieval.
     * The method updates the combo boxes, opens report windows, or displays saved reports
     * according to the current action.
     *
     * @param msg the message received from the server
     */
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
                statusLabel.setText("Report generated."); statusLabel.setStyle("-fx-text-fill: #00e676;");
                if (reportTypeCombo.getValue().contains("Visit")) openVisitReportWindow(data);
                else openCancellationReportWindow(data);
            } else if ("VIEW_REPORTS".equals(currentAction)) {
                if (msg.getData() instanceof ArrayList) {
                    showSavedReports((ArrayList<String>) msg.getData());
                } else if (msg.getData() instanceof String) {
                    showSavedReports(null);
                    statusLabel.setText(msg.getData().toString());
                }
            }
        });
    }

    // ========== VISIT REPORT POPUP ==========
    /**
     * Opens a popup window that displays the generated visit report.
     * The method parses the report data, builds a line chart for average stay time,
     * and displays a summary table for individual visitors and organized groups.
     *
     * @param data the generated visit report data received from the server
     */
    private void openVisitReportWindow(String data) {
        Map<Integer, List<Integer>> indivStays = new TreeMap<>();
        Map<Integer, List<Integer>> groupStays = new TreeMap<>();
        boolean inIndiv = false, inGroup = false;
        int indivVisits = 0, groupVisits = 0, indivVisitors = 0, groupVisitors = 0;
        double indivTotalStay = 0, groupTotalStay = 0;

        for (String line : data.split("\n")) {
            if (line.contains("Individual")) { inIndiv = true; inGroup = false; }
            if (line.contains("Organized")) { inIndiv = false; inGroup = true; }
            if (line.contains("Summary")) { inIndiv = false; inGroup = false; }
            if (line.contains("Entry:") && line.contains("Stay:")) {
                try {
                    int hour = Integer.parseInt(line.split("Entry:")[1].split("\\|")[0].trim().split(" ")[1].split(":")[0]);
                    String sp = line.split("Stay:")[1].split("\\|")[0].trim();
                    int mins = Integer.parseInt(sp.split("h")[0].trim()) * 60 + Integer.parseInt(sp.split("h")[1].replace("m","").trim());
                    int vis = Integer.parseInt(line.split("Visitors:")[1].trim());
                    if (inIndiv) { indivStays.computeIfAbsent(hour, k -> new ArrayList<>()).add(mins); indivVisits++; indivVisitors += vis; indivTotalStay += mins; }
                    else if (inGroup) { groupStays.computeIfAbsent(hour, k -> new ArrayList<>()).add(mins); groupVisits++; groupVisitors += vis; groupTotalStay += mins; }
                } catch (Exception e) {}
            }
        }

        Stage popup = new Stage();
        popup.setTitle("Visit Report - " + parkCombo.getValue());

        VBox root = new VBox(12);
        root.setAlignment(Pos.CENTER);
        root.setStyle("-fx-background-color: #1a1a2e; -fx-padding: 25;");

        Label title = new Label("Visit Report for month " + monthCombo.getValue());
        title.setStyle("-fx-text-fill: #2d6a4f; -fx-font-size: 18px; -fx-font-weight: bold; -fx-underline: true;");

        // Line chart - average stay per entry hour
        NumberAxis xAxis = new NumberAxis(7, 18, 1);
        NumberAxis yAxis = new NumberAxis();
        xAxis.setLabel("Entrance Time (hour)"); yAxis.setLabel("Stay Time (minutes)");
        xAxis.setTickLabelFont(Font.font(11)); yAxis.setTickLabelFont(Font.font(11));
        LineChart<Number, Number> chart = new LineChart<>(xAxis, yAxis);
        chart.setLegendVisible(false);
        chart.setCreateSymbols(true);
        chart.setPrefHeight(380); chart.setPrefWidth(580); chart.setMinHeight(380);
        try { chart.getStylesheets().add(getClass().getResource("/styles/charts.css").toExternalForm()); } catch (Exception e) {}

        XYChart.Series<Number, Number> s1 = new XYChart.Series<>();
        for (Map.Entry<Integer, List<Integer>> e : indivStays.entrySet())
            s1.getData().add(new XYChart.Data<>(e.getKey(), e.getValue().stream().mapToInt(i->i).average().orElse(0)));
        XYChart.Series<Number, Number> s2 = new XYChart.Series<>();
        for (Map.Entry<Integer, List<Integer>> e : groupStays.entrySet())
            s2.getData().add(new XYChart.Data<>(e.getKey(), e.getValue().stream().mapToInt(i->i).average().orElse(0)));
        chart.getData().addAll(s1, s2);

        // Custom legend
        HBox legend = new HBox(20);
        legend.setAlignment(Pos.CENTER);
        Label l1 = new Label("● Individual / Family"); l1.setStyle("-fx-text-fill: #2d6a4f; -fx-font-size: 13px; -fx-font-weight: bold;");
        Label l2 = new Label("● Organized Groups"); l2.setStyle("-fx-text-fill: #e94560; -fx-font-size: 13px; -fx-font-weight: bold;");
        legend.getChildren().addAll(l1, l2);

        // Summary table
        double avgI = indivVisits > 0 ? indivTotalStay / indivVisits : 0;
        double avgG = groupVisits > 0 ? groupTotalStay / groupVisits : 0;
        GridPane table = new GridPane();
        table.setHgap(25); table.setVgap(6); table.setAlignment(Pos.CENTER);
        table.setStyle("-fx-padding: 10; -fx-background-color: #0d1b2a; -fx-background-radius: 8;");
        addRow(table, 0, "Type", "Visits", "Visitors", "Avg Stay", true);
        addRow(table, 1, "Individual / Family", "" + indivVisits, "" + indivVisitors, Math.round(avgI) + " min", false);
        addRow(table, 2, "Organized Groups", "" + groupVisits, "" + groupVisitors, Math.round(avgG) + " min", false);
        addRow(table, 3, "Total", "" + (indivVisits + groupVisits), "" + (indivVisitors + groupVisitors), "", false);

        Button closeBtn = new Button("Close window");
        closeBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #e94560; -fx-border-color: #e94560; -fx-border-radius: 8; -fx-background-radius: 8; -fx-padding: 8 25;");
        closeBtn.setOnAction(e -> popup.close());

        root.getChildren().addAll(title, chart, legend, table, closeBtn);
        popup.setScene(new Scene(root, 650, 700));
        popup.show();
    }

    // ========== CANCELLATION REPORT POPUP ==========
    /**
     * Opens a popup window that displays the generated cancellation report.
     * The method parses the report data, builds a daily cancellation chart,
     * and displays a summary table with cancellation statistics.
     *
     * @param data the generated cancellation report data received from the server
     */
    private void openCancellationReportWindow(String data) {
        int cancelled = 0, noShow = 0, expired = 0;
        Map<String, Integer> daily = new LinkedHashMap<>();
        double avg = 0; int totalDays = 0, total = 0;

        for (String line : data.split("\n")) {
            line = line.trim();
            try {
                if (line.startsWith("cancelled:")) cancelled = Integer.parseInt(line.split(":")[1].trim().split(" ")[0]);
                if (line.startsWith("no_show:")) noShow = Integer.parseInt(line.split(":")[1].trim().split(" ")[0]);
                if (line.startsWith("expired:")) expired = Integer.parseInt(line.split(":")[1].trim().split(" ")[0]);
                if (line.contains("Average per day:")) avg = Double.parseDouble(line.split(":")[1].trim().split(" ")[0]);
                if (line.contains("Work days")) totalDays = Integer.parseInt(line.split(":")[1].trim());
                if (line.contains("Total cancellations:")) total = Integer.parseInt(line.split(":")[1].trim());
                if (line.contains("|")) {
                    String date = line.split("\\|")[0].trim();
                    int cnt = Integer.parseInt(line.split("\\|")[1].trim().split(":")[1].trim().split(" ")[0]);
                    daily.merge(date, cnt, Integer::sum);
                }
            } catch (Exception e) {}
        }

        Stage popup = new Stage();
        popup.setTitle("Cancellation Report - " + parkCombo.getValue());

        VBox root = new VBox(12);
        root.setAlignment(Pos.CENTER);
        root.setStyle("-fx-background-color: #1a1a2e; -fx-padding: 25;");

        Label title = new Label("Cancellation Report for month " + monthCombo.getValue());
        title.setStyle("-fx-text-fill: #2d6a4f; -fx-font-size: 18px; -fx-font-weight: bold; -fx-underline: true;");

        // Bar chart - daily
        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();
        xAxis.setLabel("Day of Month"); yAxis.setLabel("Cancellations");
        xAxis.setTickLabelFont(Font.font(10)); yAxis.setTickLabelFont(Font.font(11));
        BarChart<String, Number> chart = new BarChart<>(xAxis, yAxis);
        chart.setTitle("Daily Distribution");
        chart.setLegendVisible(false);
        chart.setBarGap(0); chart.setCategoryGap(3);
        chart.setPrefHeight(350); chart.setPrefWidth(580); chart.setMinHeight(350);
        try { chart.getStylesheets().add(getClass().getResource("/styles/charts.css").toExternalForm()); } catch (Exception e) {}

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        for (Map.Entry<String, Integer> e : daily.entrySet()) {
            String day;
            try { day = String.valueOf(Integer.parseInt(e.getKey().substring(e.getKey().lastIndexOf("-") + 1))); }
            catch (Exception ex) { day = e.getKey(); }
            series.getData().add(new XYChart.Data<>(day, e.getValue()));
        }
        chart.getData().add(series);

        // Summary table
        GridPane table = new GridPane();
        table.setHgap(25); table.setVgap(6); table.setAlignment(Pos.CENTER);
        table.setStyle("-fx-padding: 10; -fx-background-color: #0d1b2a; -fx-background-radius: 8;");
        addRow(table, 0, "Status", "Count", "", "", true);
        addRow(table, 1, "Cancelled", "" + cancelled, "", "", false);
        addRow(table, 2, "No Show", "" + noShow, "", "", false);
        addRow(table, 3, "Expired", "" + expired, "", "", false);
        addRow(table, 4, "─────────", "────", "", "", false);
        addRow(table, 5, "Total", "" + total, "", "", false);
        addRow(table, 6, "Days with cancellations", "" + totalDays, "", "", false);
        addRow(table, 7, "Average per day", String.format("%.1f", avg), "", "", false);

        Button closeBtn = new Button("Close window");
        closeBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #e94560; -fx-border-color: #e94560; -fx-border-radius: 8; -fx-background-radius: 8; -fx-padding: 8 25;");
        closeBtn.setOnAction(e -> popup.close());

        root.getChildren().addAll(title, chart, table, closeBtn);
        popup.setScene(new Scene(root, 650, 700));
        popup.show();
    }

    // ========== SAVED REPORTS ==========
    /**
     * Displays the list of saved reports inside the report area.
     * If no reports are available, an empty-state message is shown.
     *
     * @param reports the list of saved reports to display
     */
    private void showSavedReports(ArrayList<String> reports) {
        reportBox.getChildren().clear();
        Label title = new Label("Saved Reports");
        title.setStyle("-fx-text-fill: #2d6a4f; -fx-font-size: 16px; -fx-font-weight: bold;");
        reportBox.getChildren().add(title);

        if (reports == null || reports.isEmpty()) {
            Label empty = new Label("No saved reports found.");
            empty.setStyle("-fx-text-fill: #e0e0f0; -fx-font-size: 13px;");
            reportBox.getChildren().add(empty);
        } else {
            for (String report : reports) {
                VBox card = new VBox(5);
                card.setStyle("-fx-background-color: #0d1b2a; -fx-padding: 12; -fx-background-radius: 8; -fx-border-color: #0f3460; -fx-border-radius: 8;");
                Label reportLabel = new Label(report);
                reportLabel.setStyle("-fx-text-fill: #e0e0f0; -fx-font-size: 12px;");
                reportLabel.setWrapText(true);
                card.getChildren().add(reportLabel);
                reportBox.getChildren().add(card);
            }
        }
        reportBox.setVisible(true); reportBox.setManaged(true);
    }

    // ========== HELPERS ==========
    /**
     * Adds a formatted row to a report summary table.
     *
     * @param grid the grid pane that represents the table
     * @param row the row index in the grid
     * @param c1 the first column text
     * @param c2 the second column text
     * @param c3 the third column text
     * @param c4 the fourth column text
     * @param isHeader true if the row should be styled as a header, otherwise false
     */
    private void addRow(GridPane grid, int row, String c1, String c2, String c3, String c4, boolean isHeader) {
        String style = isHeader ? "-fx-text-fill: #2d6a4f; -fx-font-weight: bold; -fx-font-size: 13px;" : "-fx-text-fill: #e0e0f0; -fx-font-size: 12px;";
        Label l1 = new Label(c1); l1.setStyle(style); l1.setMinWidth(120); grid.add(l1, 0, row);
        Label l2 = new Label(c2); l2.setStyle(style); grid.add(l2, 1, row);
        if (!c3.isEmpty()) { Label l3 = new Label(c3); l3.setStyle(style); grid.add(l3, 2, row); }
        if (!c4.isEmpty()) { Label l4 = new Label(c4); l4.setStyle(style); grid.add(l4, 3, row); }
    }
    /**
     * Displays a status message on the screen.
     * The message color is changed according to whether it represents an error or success.
     *
     * @param msg the message to display
     * @param error true if the message represents an error, otherwise false
     */
    private void showStatus(String msg, boolean error) {
        statusLabel.setWrapText(true);
        statusLabel.setMaxWidth(700);
        statusLabel.setText(msg);
        statusLabel.setStyle("-fx-text-fill: " + (error ? "#e94560" : "#00e676") + ";");
    }

    /**
     * Handles server disconnection by displaying an error message to the department manager.
     *
     * @param r the reason for the disconnection
     */
    @Override
    public void onDisconnected(String r) {
        Platform.runLater(() -> {
            showStatus("Server disconnected. Cannot load or generate reports.", true);
        });
    }
}