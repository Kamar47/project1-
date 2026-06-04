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
        try {
            int port = Integer.parseInt(portField.getText().trim());
            server = new BackEndServer(port);
            server.setUiController(this);
            server.connectDB(dbUrlField.getText().trim(), dbUserField.getText().trim(), dbPassField.getText());
            appendLog("Connected to database.");
            server.listen();
            startButton.setDisable(true); stopButton.setDisable(false);
            portField.setDisable(true); dbUrlField.setDisable(true);
            dbUserField.setDisable(true); dbPassField.setDisable(true);
        } catch (Exception e) { appendLog("ERROR: " + e.getMessage()); }
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
