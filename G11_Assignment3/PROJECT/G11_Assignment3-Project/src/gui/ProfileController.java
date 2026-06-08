package gui;

import client.*;
import common.*;
import common.worker.GeneralParkWorker;
import javafx.application.Platform;
import javafx.fxml.*;
import javafx.scene.control.Label;
import java.net.URL;
import java.util.*;

public class ProfileController implements Initializable, ClientMessageHandler {
    @FXML private Label nameLabel, idLabel, emailLabel, roleLabel, subscriberLabel;

    // Context: "traveler" or "worker" - set before loading this page
    private static String context = "traveler";

    public static void setContext(String ctx) { context = ctx; }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        if ("traveler".equals(context)) {
            showTravelerProfile();
        } else {
            showWorkerProfile();
        }
    }

    private void showTravelerProfile() {
        Traveler t = TravelerLoginController.getLoggedInTraveler();
        if (t != null) {
            nameLabel.setText(t.getFirstName() != null ? t.getFullName() : "Traveler");
            idLabel.setText(t.getIdNumber());
            emailLabel.setText(t.getEmail() != null ? t.getEmail() : "--");
            roleLabel.setText(t.isGuide() ? "Traveler (Guide)" : "Traveler");
            subscriberLabel.setText(t.getSubscriberId() > 0 ? "Yes (Member #" + t.getSubscriberId() + ")" : "No");
        }
    }

    private void showWorkerProfile() {
        GeneralParkWorker w = WorkerLoginController.getLoggedInWorker();
        if (w != null) {
            nameLabel.setText(w.getFullName());
            idLabel.setText("Employee #" + w.getEmployeeId());
            emailLabel.setText(w.getEmail());
            roleLabel.setText(formatRole(w.getRole()));
            subscriberLabel.setText("N/A");
            if (w.getParkId() > 0) {
                ClientUI.client.setHandler(this);
                ClientUI.client.sendMessage(new ClientServerMessage(Command.GET_PARK_DETAILS, w.getParkId()));
            }
        }
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
                Park p = (Park) msg.getData();
                subscriberLabel.setText("Park: " + p.getParkName());
            }
        });
    }

    @Override
    public void onDisconnected(String reason) {}
}