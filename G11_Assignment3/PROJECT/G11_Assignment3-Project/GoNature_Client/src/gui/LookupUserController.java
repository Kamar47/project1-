package gui;

import client.ClientMessageHandler;
import client.ClientUI;
import common.ClientServerMessage;
import common.Command;
import common.Subscriber;
import common.worker.GeneralParkWorker;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class LookupUserController implements ClientMessageHandler {
    @FXML private TextField lookupIdField;
    @FXML private VBox resultCard;
    @FXML private Label cardTypeLabel;
    @FXML private VBox cardRows;
    @FXML private Label errorLabel;

    @FXML
    private void handleLookup() {
        String id = lookupIdField.getText().trim();
        if (id.isEmpty()) { showError("Please enter an ID."); return; }
        hideAll();
        ClientUI.client.setHandler(this);
        ClientUI.client.sendMessage(new ClientServerMessage(Command.LOOKUP_SUBSCRIBER, id));
    }

    @Override
    public void handleMessage(ClientServerMessage msg) {
        Platform.runLater(() -> {
            if (msg.getCommand() == Command.SUCCESS && msg.getData() instanceof Subscriber) {
                Subscriber s = (Subscriber) msg.getData();
                cardTypeLabel.setText("Subscriber");
                cardTypeLabel.setStyle("-fx-text-fill: #34d399; -fx-font-size: 13px; -fx-font-weight: bold;");
                cardRows.getChildren().clear();
                addRow(cardRows, "Full Name",     s.getFirstName() + " " + s.getLastName(), "#34d399");
                addRow(cardRows, "ID Number",     s.getIdNumber(),                           "#34d399");
                addRow(cardRows, "Email",         orNA(s.getEmail()),                        "#34d399");
                addRow(cardRows, "Phone",         orNA(s.getPhone()),                        "#34d399");
                addRow(cardRows, "Family Size",   String.valueOf(s.getFamilyMembers()),      "#34d399");
                addRow(cardRows, "Member #",      String.valueOf(s.getSubscriberId()),       "#34d399");
                showCard();
            } else if (msg.getCommand() == Command.SUCCESS && msg.getData() instanceof GeneralParkWorker) {
                GeneralParkWorker w = (GeneralParkWorker) msg.getData();
                cardTypeLabel.setText("Employee");
                cardTypeLabel.setStyle("-fx-text-fill: #60a5fa; -fx-font-size: 13px; -fx-font-weight: bold;");
                cardRows.getChildren().clear();
                addRow(cardRows, "Full Name",    w.getFullName(),                                "#60a5fa");
                addRow(cardRows, "Employee #",   String.valueOf(w.getEmployeeId()),              "#60a5fa");
                addRow(cardRows, "Role",         formatRole(w.getRole()),                        "#60a5fa");
                addRow(cardRows, "Park / Dept",  w.getParkId() > 0 ? "Park #" + w.getParkId() : "HQ", "#60a5fa");
                addRow(cardRows, "Email",        orNA(w.getEmail()),                             "#60a5fa");
                showCard();
            } else {
                showError("Not found: " + msg.getData());
            }
        });
    }

    private void addRow(VBox parent, String label, String value, String accent) {
        if (parent.getChildren().size() > 0)
            parent.getChildren().add(new Separator());

        HBox row = new HBox(12);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setStyle("-fx-padding: 10 20;");

        Label lbl = new Label(label);
        lbl.setMinWidth(110);
        lbl.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 12px;");

        Label val = new Label(value);
        val.setStyle("-fx-text-fill: #f1f5f9; -fx-font-size: 13px; -fx-font-weight: bold;");
        val.setWrapText(true);

        row.getChildren().addAll(lbl, val);
        parent.getChildren().add(row);
    }

    private void showCard() {
        resultCard.setVisible(true);
        resultCard.setManaged(true);
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
    }

    private void showError(String msg) {
        errorLabel.setText(msg);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
        resultCard.setVisible(false);
        resultCard.setManaged(false);
    }

    private void hideAll() {
        resultCard.setVisible(false);
        resultCard.setManaged(false);
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
    }

    private String orNA(String v) { return (v == null || v.isEmpty()) ? "N/A" : v; }

    private String formatRole(String role) {
        if (role == null) return "N/A";
        switch (role) {
            case "park_worker":        return "Park Worker";
            case "park_manager":       return "Park Manager";
            case "department_manager": return "Department Manager";
            case "service_rep":        return "Service Representative";
            default:                   return role;
        }
    }

    @Override
    public void onDisconnected(String r) {
        Platform.runLater(() -> showError("Server disconnected."));
    }
}
