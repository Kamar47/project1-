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
 * JavaFX controller for the park worker navigation frame (ParkWorkerFrame.fxml).
 * <p>
 * Provides the main navigation menu for park workers. Available screens:
 * Profile, Entrance Control (entry by confirmation code), Walk-in Visit,
 * and Check Available Space.
 * </p>
 *
 * @author Group 11
 */
public class ParkWorkerFrameController implements Initializable {
    @FXML private BorderPane mainBorderPane;
    @FXML private Label workerNameLabel;
    @FXML private VBox contentArea;

    /**
     * Initializes the park worker frame.
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
                    	if (ClientUI.isServerConnected()) {
                    	    ClientUI.client.sendMessage(new ClientServerMessage(Command.WORKER_LOGOUT, worker.getEmployeeId()));
                    	    try { Thread.sleep(500); } catch (InterruptedException ex) {}
                    	}
                        try { Thread.sleep(500); } catch (InterruptedException ex) {}
                    }
                });
            }
        });
    }

    /**
     * Opens the profile screen for the logged-in park worker.
     */
    @FXML private void showProfile() { ProfileController.setContext("worker"); loadPage("Profile.fxml"); }
    
    /**
     * Opens the screen for checking available park space.
     */
    @FXML private void showCheckSpace() { loadPage("ParkWorkerCheckSpace.fxml"); }
    
    /**
     * Opens the entrance control screen for handling visitor entry by confirmation code.
     */
    @FXML private void showEntranceControl() { loadPage("ParkWorkerEntranceControl.fxml"); }
    
    /**
     * Opens the walk-in visit screen for handling visitors without an existing order.
     */
    @FXML private void showUnorderedVisit() { loadPage("ParkWorkerUnorderedVisit.fxml"); }
    
    /**
     * Logs out the currently logged-in park worker and closes the frame window.
     */
    @FXML
    private void handleLogout() {
        GeneralParkWorker w = WorkerLoginController.getLoggedInWorker();

        if (w != null && ClientUI.isServerConnected()) {
            ClientUI.client.sendMessage(new ClientServerMessage(Command.WORKER_LOGOUT, w.getEmployeeId()));
            try { Thread.sleep(500); } catch (InterruptedException ex) {}
        }
        mainBorderPane.getScene().getWindow().hide();
    }

    /**
     * Loads the requested FXML page into the center area of the park worker frame.
     *
     * @param fxml the FXML file name to load
     */
    private void loadPage(String fxml) {
        try { NavigationManager.openPageInCenter(mainBorderPane, fxml); }
        catch (Exception e) { e.printStackTrace(); }
    }
}