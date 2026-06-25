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

/**
 * JavaFX controller for the GoNature Server GUI (ServerGUI.fxml).
 * <p>
 * This screen allows the server administrator to:
 * </p>
 * <ul>
 *   <li>Configure the server port and database connection (URL, user, password).</li>
 *   <li>Start and stop the {@link BackEndServer}.</li>
 *   <li>View currently connected clients in a real-time table.</li>
 *   <li>Monitor the server log output.</li>
 * </ul>
 *
 * @author Group 11
 */
public class ServerController implements Initializable {
    private BackEndServer server;
    @FXML private TextField portField, dbUrlField, dbUserField;
    @FXML private PasswordField dbPassField;
    @FXML private Button startButton, stopButton;
    @FXML private TableView<String[]> clientTable;
    @FXML private TableColumn<String[], String> colIp, colHost, colStatus;
    @FXML private TextArea logArea;
    @FXML private javafx.scene.control.Label serverIpLabel;
    private ObservableList<String[]> clientData = FXCollections.observableArrayList();

    /**
     * Initializes the server GUI components.
     * The method configures the connected clients table, sets the initial log message,
     * and displays the server IP address when it is available.
     *
     * @param url the location used to resolve relative paths for the root object
     * @param rb the resource bundle used to localize the root object
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        colIp.setCellValueFactory(d -> new SimpleStringProperty(d.getValue()[0]));
        colHost.setCellValueFactory(d -> new SimpleStringProperty(d.getValue()[1]));
        colStatus.setCellValueFactory(d -> new SimpleStringProperty(d.getValue()[2]));
        clientTable.setItems(clientData);
        appendLog("Server ready. Configure and click Start.");
        // Detect and display server IP
        try {
            java.net.InetAddress localHost = java.net.InetAddress.getLocalHost();
            String ip = localHost.getHostAddress();
            if (serverIpLabel != null) serverIpLabel.setText("Your IP: " + ip);
            appendLog("Server IP: " + ip);
        } catch (Exception e) {
            if (serverIpLabel != null) serverIpLabel.setText("IP: unavailable");
        }
    }

    /**
     * Handles the Start button action.
     * The method validates the server port and database connection fields,
     * creates the server, connects it to the database, starts listening for clients,
     * and disables the configuration fields after a successful start.
     */
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

            // Update IP display with best available address
            try {
                java.util.Enumeration<java.net.NetworkInterface> nics = java.net.NetworkInterface.getNetworkInterfaces();
                while (nics.hasMoreElements()) {
                    java.net.NetworkInterface nic = nics.nextElement();
                    java.util.Enumeration<java.net.InetAddress> addrs = nic.getInetAddresses();
                    while (addrs.hasMoreElements()) {
                        java.net.InetAddress addr = addrs.nextElement();
                        if (!addr.isLoopbackAddress() && addr instanceof java.net.Inet4Address) {
                            String ip = addr.getHostAddress();
                            if (serverIpLabel != null)
                                Platform.runLater(() -> serverIpLabel.setText("Your IP: " + ip + "  |  Port: " + portStr));
                            appendLog("Server started. Clients connect to: " + ip + ":" + portStr);
                            break;
                        }
                    }
                }
            } catch (Exception ignored) {}

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
    /**
     * Safely reads and trims the text from a text field.
     *
     * @param field the text field to read from
     * @return the trimmed text, or an empty string if the field is null
     */
    private String getText(TextField field) {
        if (field == null || field.getText() == null) {
            return "";
        }
        return field.getText().trim();
    }

    /**
     * Converts technical exception messages into user-friendly error messages
     * that can be displayed in the server log area.
     *
     * @param e the exception that occurred while starting the server
     * @return a readable error message for the server administrator
     */
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

    /**
     * Handles the Stop button action.
     * The method closes the running server, clears the connected clients table,
     * and enables the configuration fields again.
     */
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

    /**
     * Appends a message to the server log area.
     * The update is executed on the JavaFX application thread.
     *
     * @param msg the message to display in the log area
     */
    public void appendLog(String msg) { Platform.runLater(() -> logArea.appendText(msg + "\n")); }
    /**
     * Adds or updates a connected client in the clients table.
     * If a client with the same IP already exists, it is replaced with the new data.
     *
     * @param ip the client IP address
     * @param host the client host name
     * @param status the client connection status
     */
    public void addClient(String ip, String host, String status) {
        Platform.runLater(() -> { clientData.removeIf(c -> c[0].equals(ip)); clientData.add(new String[]{ip, host, status}); });
    }
    
    /**
     * Removes a disconnected client from the clients table according to its IP address.
     * The update is executed on the JavaFX application thread.
     *
     * @param ip the IP address of the client to remove
     */
    public void removeClient(String ip) { Platform.runLater(() -> clientData.removeIf(c -> c[0].equals(ip))); }
}
