package gui;

import java.net.URL;
import java.util.ResourceBundle;

import client.NavigationManager;
import common.Traveler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;

public class TravelerFrameController implements Initializable {
    @FXML private BorderPane mainBorderPane;
    @FXML private Label welcomeLabel;
    @FXML private VBox contentArea;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        Traveler t = TravelerLoginController.getLoggedInTraveler();
        if (t != null && t.getFirstName() != null && !t.getFirstName().isEmpty()) {
            welcomeLabel.setText("Welcome, " + t.getFullName() + "!");
        } else if (t != null) {
            welcomeLabel.setText("Welcome, Traveler " + t.getIdNumber() + "!");
        }
    }

    @FXML private void showProfile() { loadPage("Profile.fxml"); }
    @FXML private void showOrderVisit() { loadPage("OrderVisit.fxml"); }
    @FXML private void showViewOrders() { loadPage("TravelerOrdersFrame.fxml"); }
    @FXML private void showWaitingList() { loadPage("TravelerWaitingList.fxml"); }
    @FXML private void handleLogout() { mainBorderPane.getScene().getWindow().hide(); }

    private void loadPage(String fxml) {
        try { NavigationManager.openPageInCenter(mainBorderPane, fxml); }
        catch (Exception e) { e.printStackTrace(); }
    }
}
