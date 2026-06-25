package gui;

import client.*;
import common.*;
import common.worker.GeneralParkWorker;
import javafx.fxml.*;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import java.net.URL;
import java.util.ResourceBundle;

/**
 * JavaFX controller for the service representative navigation frame (ServiceWorkerFrame.fxml).
 * <p>
 * Provides navigation for service representatives at headquarters.
 * Available screens: Profile, Register New Guide, Register Subscriber,
 * Lookup User, and Register Existing As Guide.
 * </p>
 *
 * @author Group 11
 */
public class ServiceWorkerFrameController implements Initializable {
    @FXML private BorderPane mainBorderPane;
    @FXML private Label workerNameLabel;
    @FXML private VBox contentArea;

    /**
     * Initializes the service representative frame.
     * The method displays the logged-in worker name and registers a logout action
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
     * Opens the profile screen for the logged-in service representative.
     */
    @FXML private void showProfile() { ProfileController.setContext("worker"); loadPage("Profile.fxml"); }
    /**
     * Opens the screen for registering a new guide.
     */
    @FXML private void showRegisterGuide() { loadPage("RegisterGuide.fxml"); }
    /**
     * Opens the screen for registering a new subscriber.
     */
    @FXML private void showRegisterSubscriber() { loadPage("RegisterSubscriber.fxml"); }
    /**
     * Opens the screen for registering an existing traveler as a guide.
     */
    @FXML private void showRegisterExisting() { loadPage("RegisterExistingAsGuide.fxml"); }
    /**
     * Opens the user lookup screen.
     */
    @FXML private void showLookupUser()       { loadPage("LookupUser.fxml"); }
    /**
     * Logs out the currently logged-in service representative and closes the frame window.
     */
    @FXML private void handleLogout() {
        GeneralParkWorker w = WorkerLoginController.getLoggedInWorker();
        if (w != null) {
            ClientUI.client.sendMessage(new ClientServerMessage(Command.WORKER_LOGOUT, w.getEmployeeId()));
            try { Thread.sleep(500); } catch (InterruptedException ex) {}
        }
        mainBorderPane.getScene().getWindow().hide();
    }

    /**
     * Loads the requested FXML page into the center area of the service representative frame.
     *
     * @param fxml the FXML file name to load
     */
    private void loadPage(String fxml) {
        try { NavigationManager.openPageInCenter(mainBorderPane, fxml); }
        catch (Exception e) { e.printStackTrace(); }
    }
}