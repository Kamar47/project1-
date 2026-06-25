package gui;

import client.ClientMessageHandler;
import client.ClientUI;
import common.ClientServerMessage;
import common.Command;
import common.Order;
import common.Traveler;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * JavaFX controller for the traveler self-exit screen (TravelerExitVisit.fxml).
 * <p>
 * Allows a logged-in traveler to register their own exit from a park visit,
 * covering all members of their group in one action.
 * </p>
 * <p>
 * On initialization, the screen queries the server for any active ({@code in_park})
 * order belonging to the logged-in traveler. If found, the visit details are displayed
 * in a card. The traveler confirms exit with a single button click.
 * </p>
 * <p>
 * The server validates ownership and active status before processing the exit.
 * It reads the real visitor count from the database (the client never sends this value)
 * and updates {@code park_visits.exit_time}, {@code orders.status}, and
 * {@code parks.current_visitors} atomically. Duplicate exit attempts are rejected.
 * </p>
 *
 * @author Group 11
 */
public class TravelerExitVisitController implements Initializable, ClientMessageHandler {

    @FXML private VBox visitCard;
    @FXML private Label parkLabel, dateLabel, timeLabel, visitorsLabel, codeLabel, statusLabel;
    @FXML private Label messageLabel;
    @FXML private Button confirmExitBtn;

    private Order activeOrder = null;
    private String currentAction = "LOAD";

    /**
     * Initializes the traveler exit visit screen.
     * The method hides the visit card, displays a loading message,
     * and requests the active visit of the logged-in traveler from the server.
     *
     * @param url the location used to resolve relative paths
     * @param rb the resources used to localize the screen
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        visitCard.setVisible(false);
        visitCard.setManaged(false);
        showMessage("Loading...", "#94a3b8");
        loadActiveVisit();
    }

    /**
     * Loads the active visit of the currently logged-in traveler.
     * If the server is disconnected or the traveler session is missing,
     * an error message is displayed.
     */
    private void loadActiveVisit() {
        if (!ClientUI.isServerConnected()) { showMessage("Server disconnected.", "#f87171"); return; }
        Traveler t = TravelerLoginController.getLoggedInTraveler();
        if (t == null) { showMessage("Session expired.", "#f87171"); return; }
        currentAction = "LOAD";
        ClientUI.client.setHandler(this);
        ClientUI.client.sendMessage(new ClientServerMessage(Command.GET_ACTIVE_VISIT, t.getIdNumber()));
    }

    /**
     * Handles traveler confirmation for exiting the park.
     * The method sends an exit visit request to the server for the logged-in traveler.
     */
    @FXML
    private void handleConfirmExit() {
        if (!ClientUI.isServerConnected()) { showMessage("Server disconnected.", "#f87171"); return; }
        Traveler t = TravelerLoginController.getLoggedInTraveler();
        if (t == null) { showMessage("Session expired.", "#f87171"); return; }
        confirmExitBtn.setDisable(true);
        currentAction = "EXIT";
        ClientUI.client.setHandler(this);
        ClientUI.client.sendMessage(new ClientServerMessage(Command.TRAVELER_EXIT_VISIT, t.getIdNumber()));
    }

    /**
     * Handles server responses for loading the active visit and recording traveler exit.
     * The method displays the active visit card, shows an empty-state message,
     * or updates the screen after a successful exit.
     *
     * @param msg the message received from the server
     */
    @Override
    public void handleMessage(ClientServerMessage msg) {
        Platform.runLater(() -> {
            if ("LOAD".equals(currentAction)) {
                if (msg.getCommand() == Command.DATA_RESPONSE && msg.getData() instanceof Order) {
                    activeOrder = (Order) msg.getData();
                    showCard(activeOrder);
                } else {
                    visitCard.setVisible(false);
                    visitCard.setManaged(false);
                    showMessage("You do not have an active visit to exit.", "#94a3b8");
                }
            } else if ("EXIT".equals(currentAction)) {
                if (msg.getCommand() == Command.SUCCESS) {
                    visitCard.setVisible(false);
                    visitCard.setManaged(false);
                    showMessage("✅ Exit recorded successfully. Thank you for your visit!", "#34d399");
                } else {
                    showMessage("" + msg.getData(), "#f87171");
                    confirmExitBtn.setDisable(false);
                }
            }
        });
    }

    /**
     * Displays the active visit details in the visit card.
     *
     * @param o the active order to display
     */
    private void showCard(Order o) {
        parkLabel.setText(o.getParkName() != null ? o.getParkName() : "Park #" + o.getParkId());
        dateLabel.setText(o.getVisitDate() != null ? o.getVisitDate() : "--");
        timeLabel.setText(o.getVisitTime() != null ? o.getVisitTime() : "--");
        visitorsLabel.setText(String.valueOf(o.getNumVisitors()));
        codeLabel.setText(o.getConfirmationCode() != null ? o.getConfirmationCode() : "--");
        statusLabel.setText("In Park");
        visitCard.setVisible(true);
        visitCard.setManaged(true);
        confirmExitBtn.setDisable(false);
        showMessage("", "#94a3b8");
    }

    /**
     * Displays a message on the screen using the given text color.
     *
     * @param msg the message to display
     * @param color the CSS color value used for the message text
     */
    private void showMessage(String msg, String color) {
        messageLabel.setText(msg);
        messageLabel.setStyle("-fx-text-fill: " + color + ";");
    }

    /**
     * Handles server disconnection by displaying an error message on the screen.
     *
     * @param r the reason for the disconnection
     */
    @Override
    public void onDisconnected(String r) {
        Platform.runLater(() -> showMessage("Server disconnected.", "#f87171"));
    }
}
