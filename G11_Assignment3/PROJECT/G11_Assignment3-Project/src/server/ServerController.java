package server;

import java.net.URL;
import java.util.ResourceBundle;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

public class ServerController implements Initializable {
    private BackEndServer server;
    @FXML private TextField portField, dbUrlField, dbUserField;
    @FXML private PasswordField dbPassField;
    @FXML private Button startButton, stopButton;
    @FXML private TableView<String[]> clientTable;
    @FXML private TableColumn<String[], String> colIp, colHost, colStatus;
    @FXML private TextArea logArea;
    private ObservableList<String[]> clientData = FXCollections.observableArrayList();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        colIp.setCellValueFactory(d -> new SimpleStringProperty(d.getValue()[0]));
        colHost.setCellValueFactory(d -> new SimpleStringProperty(d.getValue()[1]));
        colStatus.setCellValueFactory(d -> new SimpleStringProperty(d.getValue()[2]));
        clientTable.setItems(clientData);
        appendLog("Server ready. Configure and click Start.");
    }

    @FXML
    public void handleStart() {
        String portStr = getText(portField);
        String dbUrl = getText(dbUrlField);
        String dbUser = getText(dbUserField);
        String dbPass = getText(dbPassField);

        boolean hasError = false;
        int port = -1;

        // Validate Port
        if (portStr.isEmpty()) {
            appendLog("ERROR: Please enter a port number.");
            hasError = true;
        } else {
            try {
                port = Integer.parseInt(portStr);

                if (port <= 0 || port > 65535) {
                    appendLog("ERROR: Port must be between 1 and 65535.");
                    hasError = true;
                }

            } catch (NumberFormatException e) {
                appendLog("ERROR: Port must be a valid number, for example 5555.");
                hasError = true;
            }
        }

        // Validate DB URL
        if (dbUrl.isEmpty()) {
            appendLog("ERROR: Please enter the database URL.");
            hasError = true;
        }

        // Validate DB User
        if (dbUser.isEmpty()) {
            appendLog("ERROR: Please enter the database username.");
            hasError = true;
        }

        // Validate DB Password
        if (dbPass.isEmpty()) {
            appendLog("ERROR: Please enter the database password.");
            hasError = true;
        }

        // If there are validation errors, do not start the server
        if (hasError) {
            return;
        }

        try {
            server = new BackEndServer(port);
            server.setUiController(this);

            server.connectDB(dbUrl, dbUser, dbPass);
            appendLog("Connected to database.");

            server.listen();

            startButton.setDisable(true);
            stopButton.setDisable(false);

            portField.setDisable(true);
            dbUrlField.setDisable(true);
            dbUserField.setDisable(true);
            dbPassField.setDisable(true);

        } catch (Exception e) {
            appendLog("ERROR: " + getFriendlyErrorMessage(e));
            server = null;
        }
    }
    private String getText(TextField field) {
        if (field == null || field.getText() == null) {
            return "";
        }
        return field.getText().trim();
    }

    private String getFriendlyErrorMessage(Exception e) {
        String msg = e.getMessage();

        if (msg == null || msg.isEmpty()) {
            return e.getClass().getSimpleName();
        }

        if (msg.contains("Access denied")) {
            return "Database login failed. Please check the DB username and password.";
        }

        if (msg.contains("Communications link failure")) {
            return "Cannot connect to MySQL. Please check that MySQL is running and the DB URL is correct.";
        }

        if (msg.contains("Address already in use")) {
            return "This port is already in use. Please choose another port.";
        }

        return msg;
    }

    @FXML
    public void handleStop() {
        if (server != null) { try { server.close(); } catch (Exception e) {} }
        Platform.runLater(() -> {
            clientData.clear();
            startButton.setDisable(false); stopButton.setDisable(true);
            portField.setDisable(false); dbUrlField.setDisable(false);
            dbUserField.setDisable(false); dbPassField.setDisable(false);
        });
    }

    public void appendLog(String msg) { Platform.runLater(() -> logArea.appendText(msg + "\n")); }
    public void addClient(String ip, String host, String status) {
        Platform.runLater(() -> { clientData.removeIf(c -> c[0].equals(ip)); clientData.add(new String[]{ip, host, status}); });
    }
    public void removeClient(String ip) { Platform.runLater(() -> clientData.removeIf(c -> c[0].equals(ip))); }
}
