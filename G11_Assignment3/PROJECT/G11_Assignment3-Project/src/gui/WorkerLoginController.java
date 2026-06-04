package gui;

import client.ClientMessageHandler;
import client.ClientUI;
import client.NavigationManager;
import common.ClientServerMessage;
import common.Command;
import common.worker.GeneralParkWorker;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class WorkerLoginController implements ClientMessageHandler {
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label errorLabel;
    private static GeneralParkWorker loggedInWorker;

    @FXML
    private void handleLogin() {
        String user = usernameField.getText().trim();
        String pass = passwordField.getText();
        if (user.isEmpty() || pass.isEmpty()) { errorLabel.setText("Please fill in all fields."); return; }
        ClientUI.client.setHandler(this);
        ClientUI.client.sendMessage(new ClientServerMessage(Command.WORKER_LOGIN,
            ClientServerMessage.packData(user, pass)));
    }

    @FXML
    private void handleBack() { usernameField.getScene().getWindow().hide(); }

    @Override
    public void handleMessage(ClientServerMessage msg) {
        Platform.runLater(() -> {
            if (msg.getCommand() == Command.SUCCESS && msg.getData() instanceof GeneralParkWorker) {
                loggedInWorker = (GeneralParkWorker) msg.getData();
                try {
                    String page;
                    String title;
                    switch (loggedInWorker.getRole()) {
                        case "park_worker": page = "ParkWorkerFrame.fxml"; title = "Park Worker"; break;
                        case "park_manager": page = "ParkManagerFrame.fxml"; title = "Park Manager"; break;
                        case "department_manager": page = "DepartmentManagerFrame.fxml"; title = "Department Manager"; break;
                        case "service_rep": page = "ServiceWorkerFrame.fxml"; title = "Service Worker"; break;
                        default: errorLabel.setText("Unknown role."); return;
                    }
                    NavigationManager.openPage(page, null, "GoNature - " + title, false);
                    usernameField.getScene().getWindow().hide();
                } catch (Exception e) { errorLabel.setText("Error opening screen."); e.printStackTrace(); }
            } else {
                errorLabel.setText("Invalid credentials or already logged in.");
            }
        });
    }

    @Override
    public void onDisconnected(String reason) {
        Platform.runLater(() -> errorLabel.setText(reason));
    }

    public static GeneralParkWorker getLoggedInWorker() { return loggedInWorker; }
}
