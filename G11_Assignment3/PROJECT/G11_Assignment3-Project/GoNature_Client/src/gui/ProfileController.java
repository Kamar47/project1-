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

public class ProfileController implements Initializable, ClientMessageHandler {
    @FXML private Label nameLabel, idLabel, emailLabel, roleLabel, subscriberLabel;
    @FXML private Label statusLabel;
    @FXML private TextField editFirstNameField, editLastNameField, editEmailField;
    @FXML private Button saveProfileBtn;

    private static String context = "traveler";
    public static void setContext(String ctx) { context = ctx; }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        if ("traveler".equals(context)) {
            showTravelerProfile();
        } else {
            showWorkerProfile();
            setEditVisible(false);
        }
    }

    private void setEditVisible(boolean v) {
        if (editFirstNameField != null) editFirstNameField.setVisible(v);
        if (editLastNameField  != null) editLastNameField.setVisible(v);
        if (editEmailField     != null) editEmailField.setVisible(v);
        if (saveProfileBtn     != null) saveProfileBtn.setVisible(v);
        if (statusLabel        != null) statusLabel.setVisible(v);
    }

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

    private String formatRole(String role) {
        switch (role) {
            case "park_worker": return "Park Worker";
            case "park_manager": return "Park Manager";
            case "department_manager": return "Department Manager";
            case "service_rep": return "Service Representative";
            default: return role;
        }
    }

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

    @Override
    public void onDisconnected(String reason) {
        Platform.runLater(() -> { subscriberLabel.setText("Server disconnected."); subscriberLabel.setStyle("-fx-text-fill: #e94560;"); });
    }
}
