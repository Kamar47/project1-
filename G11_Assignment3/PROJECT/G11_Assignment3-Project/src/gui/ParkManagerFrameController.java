package gui;

import java.net.URL;
import java.util.ResourceBundle;

import client.ClientUI;
import client.NavigationManager;
import common.ClientServerMessage;
import common.Command;
import common.worker.GeneralParkWorker;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;

public class ParkManagerFrameController implements Initializable {
    @FXML private BorderPane mainBorderPane;
    @FXML private Label workerNameLabel;
    @FXML private VBox contentArea;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        GeneralParkWorker w = WorkerLoginController.getLoggedInWorker();
        if (w != null) workerNameLabel.setText(w.getFullName());
    }

    @FXML private void showProfile() { loadPage("Profile.fxml"); }
    @FXML private void showParameters() { loadPage("ParkManagerParameters.fxml"); }
    @FXML private void showCreateReport() { loadPage("ParkManagerCreateReport.fxml"); }
    @FXML private void showPromotions() { loadPage("ParkManagerPromotions.fxml"); }
    @FXML private void handleLogout() {
        GeneralParkWorker w = WorkerLoginController.getLoggedInWorker();
        if (w != null) ClientUI.client.sendMessage(new ClientServerMessage(Command.WORKER_LOGOUT, w.getEmployeeId()));
        mainBorderPane.getScene().getWindow().hide();
    }

    private void loadPage(String fxml) {
        try { NavigationManager.openPageInCenter(mainBorderPane, fxml); }
        catch (Exception e) { e.printStackTrace(); }
    }
}
