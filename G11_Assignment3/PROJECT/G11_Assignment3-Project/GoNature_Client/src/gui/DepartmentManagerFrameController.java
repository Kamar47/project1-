package gui;

import client.*;
import common.*;
import common.worker.GeneralParkWorker;
import javafx.fxml.*;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import java.net.URL;
import java.util.ResourceBundle;

/**
 * JavaFX controller for the department manager navigation frame (DepartmentManagerFrame.fxml).
 * <p>
 * Provides navigation for the department manager. Available screens:
 * Profile, Pending Requests (parameter changes and promotions), Park Parameters, and Reports.
 * </p>
 *
 * @author Group 11
 */
public class DepartmentManagerFrameController implements Initializable {
    @FXML private BorderPane mainBorderPane;
    @FXML private Label workerNameLabel;
    @FXML private VBox contentArea;

    /**
     * Initializes the screen and prepares the UI components.
     *
     * @param url the location used to resolve relative paths
     * @param rb the resources used to localize the screen
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        GeneralParkWorker w = WorkerLoginController.getLoggedInWorker();
        if (w != null) workerNameLabel.setText(w.getFullName());
        // Handle X button close - logout worker
        javafx.application.Platform.runLater(() -> {
            if (mainBorderPane.getScene() != null && mainBorderPane.getScene().getWindow() != null) {
                mainBorderPane.getScene().getWindow().setOnCloseRequest(event -> {
                    GeneralParkWorker worker = WorkerLoginController.getLoggedInWorker();
                    if (worker != null) {
                        ClientUI.client.sendMessage(new ClientServerMessage(Command.WORKER_LOGOUT, worker.getEmployeeId()));
                        try { Thread.sleep(500); } catch (InterruptedException ex) {}
                    }
                });
            }
        });
    }

    /**
     * Opens the profile screen for the logged-in department manager.
     */
    @FXML private void showProfile() { ProfileController.setContext("worker"); loadPage("Profile.fxml"); }
    /**
     * Opens the park parameters screen for viewing park configuration values.
     */
    @FXML private void showParkParameters() { loadPage("DeptManagerParkParams.fxml"); }
    /**
     * Opens the reports screen for the department manager.
     */
    @FXML private void showReports() { loadPage("DeptManagerReports.fxml"); }
    /**
     * Opens the pending requests screen for parameter change and promotion approvals.
     */
    @FXML private void showRequests() { loadPage("DeptManagerRequests.fxml"); }
    /**
     * Logs out the currently logged-in department manager and closes the frame window.
     */
    @FXML private void handleLogout() {
        GeneralParkWorker w = WorkerLoginController.getLoggedInWorker();

        if (w != null && ClientUI.isServerConnected()) {
            ClientUI.client.sendMessage(new ClientServerMessage(Command.WORKER_LOGOUT, w.getEmployeeId()));
            try { Thread.sleep(500); } catch (InterruptedException ex) {}
        }

        mainBorderPane.getScene().getWindow().hide();
    }

    /**
     * Loads the requested FXML page into the center area of the department manager frame.
     * If the page cannot be loaded, an error message is displayed instead.
     *
     * @param fxml the FXML file name to load
     */
    private void loadPage(String fxml) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxml));
            Parent page = loader.load();
            mainBorderPane.setCenter(page);
        } catch (Exception e) {
            e.printStackTrace();

            Label error = new Label("Server disconnected.");
            error.setStyle("-fx-text-fill: #e94560; -fx-font-size: 18px;");
            mainBorderPane.setCenter(error);
        }
    }
}