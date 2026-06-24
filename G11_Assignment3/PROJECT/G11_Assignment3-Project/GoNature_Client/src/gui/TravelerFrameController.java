package gui;

import client.ClientMessageHandler;
import client.ClientUI;
import client.NavigationManager;
import common.ClientServerMessage;
import common.Command;
import common.Traveler;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.net.URL;
import java.util.ArrayList;
import java.util.ResourceBundle;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class TravelerFrameController implements Initializable, ClientMessageHandler {

    @FXML private BorderPane mainBorderPane;
    @FXML private Label welcomeLabel;
    @FXML private VBox contentArea;

    // Polling thread — checks for notifications every 30 seconds
    private ScheduledExecutorService pollingScheduler;

    // Flag so we don't stack multiple popups if one is already showing
    private boolean popupShowing = false;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        Traveler t = TravelerLoginController.getLoggedInTraveler();
        if (t != null && t.getFirstName() != null && !t.getFirstName().isEmpty()) {
            welcomeLabel.setText("Welcome, " + t.getFullName() + "!");
        } else if (t != null) {
            welcomeLabel.setText("Welcome, Traveler " + t.getIdNumber() + "!");
        }
        startNotificationPolling();
    }

    // ─────────────────────────────────────────────
    // Polling
    // ─────────────────────────────────────────────

    private void startNotificationPolling() {
        pollingScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "notification-polling");
            t.setDaemon(true); // stops automatically when JVM exits
            return t;
        });
        // First check immediately (delay=0), then every 30 seconds
        pollingScheduler.scheduleAtFixedRate(this::pollNotifications, 0, 30, TimeUnit.SECONDS);
    }

    private void pollNotifications() {
        // Safety: don't poll if disconnected or popup already open
        if (!ClientUI.isServerConnected() || popupShowing) return;
        Traveler t = TravelerLoginController.getLoggedInTraveler();
        if (t == null) return;
        // Use dedicated polling handler so other screens don't steal the response
        ClientUI.client.setPollingHandler(this);
        ClientUI.client.sendMessage(new ClientServerMessage(Command.GET_MY_NOTIFICATIONS, t.getIdNumber()));
    }

    public void stopPolling() {
        if (pollingScheduler != null && !pollingScheduler.isShutdown()) {
            pollingScheduler.shutdownNow();
        }
    }

    // ─────────────────────────────────────────────
    // Handle server response
    // ─────────────────────────────────────────────

    @Override
    @SuppressWarnings("unchecked")
    public void handleMessage(ClientServerMessage msg) {
        if (msg.getCommand() == Command.DATA_RESPONSE && msg.getData() instanceof ArrayList) {
            ArrayList<ArrayList<String>> notifications = (ArrayList<ArrayList<String>>) msg.getData();
            if (!notifications.isEmpty() && !popupShowing) {
                // Show first unread notification — one popup at a time
                Platform.runLater(() -> showNotificationPopup(notifications.get(0)));
            }
        }
        // SUCCESS from CONFIRM_ORDER (sent by popup controller)
        // Nothing to do here — popup is already closed by the time this arrives
    }

    @Override
    public void onDisconnected(String reason) {
        stopPolling();
    }

    // ─────────────────────────────────────────────
    // Popup window
    // ─────────────────────────────────────────────

    private void showNotificationPopup(ArrayList<String> notificationRow) {
        try {
            popupShowing = true;

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/gui/NotificationPopup.fxml"));
            Parent root = loader.load();

            NotificationPopupController ctrl = loader.getController();
            ctrl.setNotification(notificationRow);

            Scene scene = new Scene(root);
            try {
                scene.getStylesheets().add(getClass().getResource("/styles/styles.css").toExternalForm());
            } catch (Exception ignored) {}

            Stage popup = new Stage();
            popup.setTitle("GoNature — Notification");
            popup.setScene(scene);
            popup.setResizable(false);
            // Modal so user can't ignore it by clicking behind — must choose Confirm or Later
            popup.initModality(Modality.APPLICATION_MODAL);
            popup.initStyle(StageStyle.DECORATED);

            // When closed (Confirm or Later), allow next poll to show the next notification
            popup.setOnHidden(e -> popupShowing = false);

            popup.show();

        } catch (Exception e) {
            popupShowing = false;
            System.out.println("[TravelerFrame] Could not show notification popup: " + e.getMessage());
        }
    }

    // ─────────────────────────────────────────────
    // Navigation
    // ─────────────────────────────────────────────

    @FXML private void showProfile()      { ProfileController.setContext("traveler"); loadPage("Profile.fxml"); }
    @FXML private void showOrderVisit()   { loadPage("OrderVisit.fxml"); }
    @FXML private void showViewOrders()   { loadPage("TravelerOrdersFrame.fxml"); }
    @FXML private void showWaitingList()  { loadPage("TravelerWaitingList.fxml"); }

    @FXML
    private void handleLogout() {
        stopPolling();
        ClientUI.client.clearPollingHandler();
        // Release traveler session on server
        common.Traveler t = TravelerLoginController.getLoggedInTraveler();
        if (t != null && ClientUI.isServerConnected()) {
            ClientUI.client.sendMessage(new common.ClientServerMessage(common.Command.TRAVELER_LOGOUT, t.getIdNumber()));
        }
        TravelerLoginController.clearLoggedInTraveler();
        mainBorderPane.getScene().getWindow().hide();
    }

    private void loadPage(String fxml) {
        try { NavigationManager.openPageInCenter(mainBorderPane, fxml); }
        catch (Exception e) { e.printStackTrace(); }
    }
}
