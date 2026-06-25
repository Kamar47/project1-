package gui;

import client.*;
import common.*;
import common.worker.GeneralParkWorker;
import javafx.application.Platform;
import javafx.fxml.*;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.Button;
import java.net.URL;
import java.util.*;

/**
 * JavaFX controller for the Profile screen (Profile.fxml).
 * <p>
 * Displays the account information of the currently logged-in user.
 * The screen adapts based on context (set via {@link #setContext(String)}):
 * </p>
 * <ul>
 *   <li><b>Traveler context:</b> shows name, ID, email, role, and subscriber status.
 *       If the traveler is a subscriber, an edit section is shown allowing them to
 *       update their first name, last name, and email ({@code UPDATE_SUBSCRIBER_PROFILE}).</li>
 *   <li><b>Worker context:</b> shows employee name, ID, email, role, and park assignment.
 *       Edit section is hidden.</li>
 * </ul>
 *
 * @author Group 11
 */
public class ProfileController implements Initializable, ClientMessageHandler {
    @FXML private Label nameLabel, idLabel, emailLabel, roleLabel, subscriberLabel;
    @FXML private Label statusLabel;
    @FXML private TextField editFirstNameField, editLastNameField, editEmailField;
    @FXML private Button saveProfileBtn;

    private static String context = "traveler";
    /**
     * Sets the profile display context.
     * The context determines whether the screen shows traveler details or worker details.
     *
     * @param ctx the profile context, either traveler or worker
     */
    public static void setContext(String ctx) { context = ctx; }

    /**
     * Initializes the profile screen according to the selected context.
     * The method displays either the logged-in traveler profile or the logged-in worker profile.
     *
     * @param url the location used to resolve relative paths
     * @param rb the resources used to localize the screen
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        if ("traveler".equals(context)) {
            showTravelerProfile();
        } else {
            showWorkerProfile();
            setEditVisible(false);
        }
    }

    /**
     * Shows or hides the editable subscriber profile fields.
     *
     * @param v true to show the edit fields, otherwise false
     */
    private void setEditVisible(boolean v) {
        if (editFirstNameField != null) editFirstNameField.setVisible(v);
        if (editLastNameField  != null) editLastNameField.setVisible(v);
        if (editEmailField     != null) editEmailField.setVisible(v);
        if (saveProfileBtn     != null) saveProfileBtn.setVisible(v);
        if (statusLabel        != null) statusLabel.setVisible(v);
    }

    /**
     * Displays the profile details of the currently logged-in traveler.
     * If the traveler is a subscriber, editable profile fields are prepared.
     */
    private void showTravelerProfile() {
        Traveler t = TravelerLoginController.getLoggedInTraveler();
        if (t != null) {
            nameLabel.setText(t.getFirstName() != null ? t.getFullName() : "Traveler");
            idLabel.setText(t.getIdNumber());
            emailLabel.setText(t.getEmail() != null ? t.getEmail() : "--");
            roleLabel.setText(t.isGuide() ? "Traveler (Guide)" : "Traveler");
            subscriberLabel.setText(t.getSubscriberId() > 0 ? "Yes (Member #" + t.getSubscriberId() + ")" : "No");

            if (t.getSubscriberId() > 0) {
                if (editFirstNameField != null) editFirstNameField.setText(t.getFirstName() != null ? t.getFirstName() : "");
                if (editLastNameField  != null) editLastNameField.setText(t.getLastName() != null ? t.getLastName() : "");
                if (editEmailField     != null) editEmailField.setText(t.getEmail() != null ? t.getEmail() : "");
            } else {
                setEditVisible(false);
            }
        }
        if (!ClientUI.isServerConnected()) {
            subscriberLabel.setText("Server disconnected.");
            subscriberLabel.setStyle("-fx-text-fill: #e94560;");
        }
    }

    /**
     * Displays the profile details of the currently logged-in worker.
     * If the worker is assigned to a park, the park details are requested from the server.
     */
    private void showWorkerProfile() {
        GeneralParkWorker w = WorkerLoginController.getLoggedInWorker();
        if (w != null) {
            nameLabel.setText(w.getFullName());
            idLabel.setText("Employee #" + w.getEmployeeId());
            emailLabel.setText(w.getEmail());
            roleLabel.setText(formatRole(w.getRole()));
            subscriberLabel.setText(w.getParkId() > 0 ? "Park ID: " + w.getParkId() : "N/A");
            if (!ClientUI.isServerConnected()) {
                subscriberLabel.setText("Server disconnected.");
                subscriberLabel.setStyle("-fx-text-fill: #e94560;");
                return;
            }
            if (w.getParkId() > 0) {
                ClientUI.client.setHandler(this);
                ClientUI.client.sendMessage(new ClientServerMessage(Command.GET_PARK_DETAILS, w.getParkId()));
            }
        }
    }

    /**
     * Handles saving edited subscriber profile details.
     * The method validates the edited fields and sends an update request to the server.
     */
    @FXML
    private void handleSaveProfile() {
        Traveler t = TravelerLoginController.getLoggedInTraveler();
        if (t == null || t.getSubscriberId() <= 0) return;
        String fn = editFirstNameField.getText().trim();
        String ln = editLastNameField.getText().trim();
        String em = editEmailField.getText().trim();
        if (fn.isEmpty() || ln.isEmpty() || em.isEmpty()) {
            if (statusLabel != null) { statusLabel.setText("All fields required."); statusLabel.setStyle("-fx-text-fill: #f87171;"); }
            return;
        }
        String err = client.InputValidation.validateEmail(em);
        if (err != null) {
            if (statusLabel != null) { statusLabel.setText(err); statusLabel.setStyle("-fx-text-fill: #f87171;"); }
            return;
        }
        ArrayList<Object> data = new ArrayList<>();
        data.add(t.getIdNumber()); data.add(fn); data.add(ln); data.add(em);
        ClientUI.client.setHandler(this);
        ClientUI.client.sendMessage(new ClientServerMessage(Command.UPDATE_SUBSCRIBER_PROFILE, data));
    }

    /**
     * Converts an internal worker role value into a user-friendly display label.
     *
     * @param role the internal worker role value
     * @return the formatted role label
     */
    private String formatRole(String role) {
        switch (role) {
            case "park_worker": return "Park Worker";
            case "park_manager": return "Park Manager";
            case "department_manager": return "Department Manager";
            case "service_rep": return "Service Representative";
            default: return role;
        }
    }

    /**
     * Handles server responses for profile-related actions.
     * The method updates worker park details or confirms successful subscriber profile updates.
     *
     * @param msg the message received from the server
     */
    @Override
    public void handleMessage(ClientServerMessage msg) {
        Platform.runLater(() -> {
            if (msg.getCommand() == Command.DATA_RESPONSE && msg.getData() instanceof Park) {
                subscriberLabel.setText("Park: " + ((Park) msg.getData()).getParkName());
            } else if (msg.getCommand() == Command.SUCCESS) {
                Traveler t = TravelerLoginController.getLoggedInTraveler();
                if (t != null) {
                    t.setFirstName(editFirstNameField.getText().trim());
                    t.setLastName(editLastNameField.getText().trim());
                    t.setEmail(editEmailField.getText().trim());
                    nameLabel.setText(t.getFullName());
                    emailLabel.setText(t.getEmail());
                }
                if (statusLabel != null) { statusLabel.setText("Saved!"); statusLabel.setStyle("-fx-text-fill: #34d399;"); }
            } else if (msg.getCommand() == Command.FAILURE) {
                if (statusLabel != null) { statusLabel.setText("Error: " + msg.getData()); statusLabel.setStyle("-fx-text-fill: #f87171;"); }
            }
        });
    }

    /**
     * Handles server disconnection by displaying an error message on the profile screen.
     *
     * @param reason the reason for the disconnection
     */
    @Override
    public void onDisconnected(String reason) {
        Platform.runLater(() -> { subscriberLabel.setText("Server disconnected."); subscriberLabel.setStyle("-fx-text-fill: #e94560;"); });
    }
}
