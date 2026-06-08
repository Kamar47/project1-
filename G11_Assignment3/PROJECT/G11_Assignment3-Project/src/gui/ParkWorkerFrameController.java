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

public class ParkWorkerFrameController implements Initializable {
    @FXML private BorderPane mainBorderPane;
    @FXML private Label workerNameLabel;
    @FXML private VBox contentArea;

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

    @FXML private void showProfile() { ProfileController.setContext("worker"); loadPage("Profile.fxml"); }
    @FXML private void showCheckSpace() { loadPage("ParkWorkerCheckSpace.fxml"); }
    @FXML private void showEntranceControl() { loadPage("ParkWorkerEntranceControl.fxml"); }
    @FXML private void showUnorderedVisit() { loadPage("ParkWorkerUnorderedVisit.fxml"); }
    @FXML private void handleLogout() {
        GeneralParkWorker w = WorkerLoginController.getLoggedInWorker();
        if (w != null) {
            ClientUI.client.sendMessage(new ClientServerMessage(Command.WORKER_LOGOUT, w.getEmployeeId()));
            try { Thread.sleep(500); } catch (InterruptedException ex) {}
        }
        mainBorderPane.getScene().getWindow().hide();
    }

    private void loadPage(String fxml) {
        try { NavigationManager.openPageInCenter(mainBorderPane, fxml); }
        catch (Exception e) { e.printStackTrace(); }
    }
}