package gui;

import client.NavigationManager;
import common.Traveler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import java.net.URL;
import java.util.ResourceBundle;

public class TravelerFrameController implements Initializable {
    @FXML private Label welcomeLabel;
    @FXML private VBox contentArea;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        Traveler t = TravelerLoginController.getLoggedInTraveler();
        if (t != null && t.getFirstName() != null) {
            welcomeLabel.setText("Welcome, " + t.getFullName() + "!");
        } else if (t != null) {
            welcomeLabel.setText("Welcome, Traveler " + t.getIdNumber() + "!");
        }
    }

    private BorderPane getBorderPane() {
        return (BorderPane) contentArea.getParent();
    }

    @FXML private void showProfile() {
        try { NavigationManager.openPageInCenter(getBorderPane(), "Profile.fxml"); }
        catch (Exception e) { e.printStackTrace(); }
    }
    @FXML private void showOrderVisit() {
        try { NavigationManager.openPageInCenter(getBorderPane(), "OrderVisit.fxml"); }
        catch (Exception e) { e.printStackTrace(); }
    }
    @FXML private void showViewOrders() {
        try { NavigationManager.openPageInCenter(getBorderPane(), "TravelerOrdersFrame.fxml"); }
        catch (Exception e) { e.printStackTrace(); }
    }
    @FXML private void showWaitingList() {
        try { NavigationManager.openPageInCenter(getBorderPane(), "TravelerWaitingList.fxml"); }
        catch (Exception e) { e.printStackTrace(); }
    }
    @FXML private void handleLogout() {
        welcomeLabel.getScene().getWindow().hide();
    }
}
