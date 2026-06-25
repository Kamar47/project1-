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

/**
 * JavaFX controller for the main traveler navigation frame (TravelerFrame.fxml).
 * <p>
 * This frame wraps all traveler screens and provides the sidebar navigation menu.
 * It also starts a background notification polling task that queries the server
 * every 30 seconds for unread notifications (reminders, waitlist availability,
 * and cancellation notices).
 * </p>
 * <p>
 * When a notification is received, a {@link NotificationPopupController} popup is
 * displayed. The polling pauses while a popup is open ({@code popupShowing} flag)
 * to prevent stacking multiple dialogs.
 * </p>
 * <p>Navigation options: Profile, Order A Visit, View Orders, Waiting List, Exit Visit.</p>
 *
 * @author Group 11
 */
public class TravelerFrameController implements Initializable, ClientMessageHandler {

    @FXML private BorderPane mainBorderPane;
    @FXML private Label welcomeLabel;
    @FXML private VBox contentArea;

    // Polling thread — checks for notifications every 30 seconds
    private ScheduledExecutorService pollingScheduler;

    // Flag so we don't stack multiple popups if one is already showing
    private boolean popupShowing = false;

    /**
     * Initializes the main traveler frame.
     * The method displays the logged-in traveler name and starts notification polling.
     *
     * @param url the location used to resolve relative paths
     * @param rb the resources used to localize the screen
     */
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

    /**
     * Starts a background polling task that checks for unread traveler notifications.
     * The polling is executed periodically while the traveler frame is open.
     */
    private void startNotificationPolling() {
        pollingScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "notification-polling");
            t.setDaemon(true); // stops automatically when JVM exits
            return t;
        });
        // First check immediately (delay=0), then every 30 seconds
        pollingScheduler.scheduleAtFixedRate(this::pollNotifications, 0, 30, TimeUnit.SECONDS);
    }

    /**
     * Sends a request to the server to retrieve unread notifications for the logged-in traveler.
     * The method does not send a request if the server is disconnected or a popup is already open.
     */
    private void pollNotifications() {
        // Safety: don't poll if disconnected or popup already open
        if (!ClientUI.isServerConnected() || popupShowing) return;
        Traveler t = TravelerLoginController.getLoggedInTraveler();
        if (t == null) return;
        // Use dedicated polling handler so other screens don't steal the response
        ClientUI.client.setPollingHandler(this);
        ClientUI.client.sendMessage(new ClientServerMessage(Command.GET_MY_NOTIFICATIONS, t.getIdNumber()));
    }

    /**
     * Stops the notification polling task if it is currently running.
     */
    public void stopPolling() {
        if (pollingScheduler != null && !pollingScheduler.isShutdown()) {
            pollingScheduler.shutdownNow();
        }
    }

    // ─────────────────────────────────────────────
    // Handle server response
    // ─────────────────────────────────────────────

    /**
     * Handles server responses related to traveler notifications.
     * If unread notifications are received, the first notification is displayed in a popup.
     *
     * @param msg the message received from the server
     */
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

    /**
     * Handles server disconnection by stopping the notification polling task.
     *
     * @param reason the reason for the disconnection
     */
    @Override
    public void onDisconnected(String reason) {
        stopPolling();
    }

    // ─────────────────────────────────────────────
    // Popup window
    // ─────────────────────────────────────────────

    /**
     * Displays a popup window for a traveler notification.
     * The popup allows the traveler to respond to the notification or close it.
     *
     * @param notificationRow the notification data received from the server
     */
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

    /**
     * Opens the profile screen for the logged-in traveler.
     */
    @FXML private void showProfile()      { ProfileController.setContext("traveler"); loadPage("Profile.fxml"); }
    /**
     * Opens the order-a-visit screen.
     */
    @FXML private void showOrderVisit()   { loadPage("OrderVisit.fxml"); }
    /**
     * Opens the traveler orders screen.
     */
    @FXML private void showViewOrders()   { loadPage("TravelerOrdersFrame.fxml"); }
    /**
     * Opens the traveler waiting list screen.
     */
    @FXML private void showWaitingList()  { loadPage("TravelerWaitingList.fxml"); }
    /**
     * Opens the exit visit screen.
     */
    @FXML private void showExitVisit()     { loadPage("TravelerExitVisit.fxml"); }

    /**
     * Logs out the currently logged-in traveler, stops notification polling,
     * clears the traveler session, and closes the traveler frame.
     */
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

    /**
     * Loads the requested FXML page into the center area of the traveler frame.
     *
     * @param fxml the FXML file name to load
     */
    private void loadPage(String fxml) {
        try { NavigationManager.openPageInCenter(mainBorderPane, fxml); }
        catch (Exception e) { e.printStackTrace(); }
    }
}
