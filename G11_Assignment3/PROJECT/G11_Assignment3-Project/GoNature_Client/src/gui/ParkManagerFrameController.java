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
 * JavaFX controller for the park manager navigation frame (ParkManagerFrame.fxml).
 * <p>
 * Provides navigation for park managers. Available screens:
 * Profile, Park Parameters (view/request changes), Promotions, and Reports.
 * </p>
 *
 * @author Group 11
 */
public class ParkManagerFrameController implements Initializable {
    @FXML private BorderPane mainBorderPane;
    @FXML private Label workerNameLabel;
    @FXML private VBox contentArea;

    /**
     * Initializes the park manager frame.
     * The method displays the logged-in park manager name and registers a logout action
     * when the window is closed.
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
     * Opens the profile screen for the logged-in park manager.
     */
    @FXML private void showProfile() { ProfileController.setContext("worker"); loadPage("Profile.fxml"); }
    
    /**
     * Opens the park parameters screen for viewing and requesting parameter changes.
     */
    @FXML private void showParameters() { loadPage("ParkManagerParameters.fxml"); }
    
    /**
     * Opens the report creation screen for the park manager.
     */
    @FXML private void showCreateReport() { loadPage("ParkManagerCreateReport.fxml"); }
    
    /**
     * Opens the promotions screen for creating and managing park promotions.
     */
    @FXML private void showPromotions() { loadPage("ParkManagerPromotions.fxml"); }
    
    /**
     * Logs out the currently logged-in park manager and closes the frame window.
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
     * Loads the requested FXML page into the center area of the park manager frame.
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

            Label error = new Label("Server disconnected. Page actions are unavailable.");
            error.setStyle("-fx-text-fill: #e94560; -fx-font-size: 18px;");
            mainBorderPane.setCenter(error);
        }
    }
}