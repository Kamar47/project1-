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

/**
 * JavaFX controller for the employee login screen.
 * <p>
 * Employees log in with their username and password. The server validates
 * credentials, checks the {@code is_logged_in} flag to prevent simultaneous
 * logins, and returns a {@link common.worker.GeneralParkWorker} object.
 * After login the user is redirected to the appropriate frame based on their role.
 * </p>
 *
 * @author Group 11
 */
public class WorkerLoginController implements ClientMessageHandler {
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label errorLabel;
    private static GeneralParkWorker loggedInWorker;

    /**
     * Initializes the employee login screen and registers this controller
     * as the current client message handler.
     */
    @FXML
    private void initialize() {
        if (ClientUI.client != null) {
            ClientUI.client.setHandler(this);
        }
    }
    /**
     * Handles employee login by validating the username and password fields
     * and sending a login request to the server.
     */
    @FXML
    private void handleLogin() {
    	if (!ClientUI.isServerConnected()) {
    	    showError("Server is disconnected. Please reconnect from the home page.");
    	    return;
    	}
        String user = usernameField.getText().trim();
        String pass = passwordField.getText();
        if (user.isEmpty() || pass.isEmpty()) { errorLabel.setText("Please fill in all fields."); return; }
        ClientUI.client.setHandler(this);
        ClientUI.client.sendMessage(new ClientServerMessage(Command.WORKER_LOGIN,
            ClientServerMessage.packData(user, pass)));
    }

    /**
     * Closes the employee login window and returns to the previous screen.
     */
    @FXML
    private void handleBack() { usernameField.getScene().getWindow().hide(); }

    /**
     * Handles the server response for employee login.
     * If the login succeeds, the matching frame is opened according to the employee role.
     *
     * @param msg the message received from the server
     */
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

    /**
     * Handles server disconnection by displaying the disconnection reason on the screen.
     *
     * @param reason the reason for the disconnection
     */
    @Override
    public void onDisconnected(String reason) {
        Platform.runLater(() -> errorLabel.setText(reason));
    }
    /**
     * Displays an error message on the employee login screen.
     *
     * @param message the error message to display
     */
    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setStyle("-fx-text-fill: #e94560;");
    }

    /**
     * Returns the employee currently logged in during this client session.
     *
     * @return the logged-in employee, or null if no employee is logged in
     */
    public static GeneralParkWorker getLoggedInWorker() { return loggedInWorker; }
}
