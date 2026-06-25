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

/**
 * JavaFX controller for the Lookup User screen (LookupUser.fxml),
 * accessible to service representatives.
 * <p>
 * Allows the representative to search for any registered user by ID:
 * </p>
 * <ul>
 *   <li>Subscriber search: enter the subscriber's national ID number.
 *       Returns name, ID, email, phone, family size, and member number.</li>
 *   <li>Employee search: enter the employee's internal employee number.
 *       Returns name, employee number, role, park assignment, and email.
 *       Password is never displayed.</li>
 * </ul>
 * <p>
 * Results are displayed in a styled card with colour-coded accent:
 * green for subscribers, blue for employees, red for not found.
 * </p>
 *
 * @author Group 11
 */
public class LookupUserController implements ClientMessageHandler {
    @FXML private TextField lookupIdField;
    @FXML private VBox resultCard;
    @FXML private Label cardTypeLabel;
    @FXML private VBox cardRows;
    @FXML private Label errorLabel;

    /**
     * Handles user lookup by reading the entered ID and sending a subscriber lookup request
     * to the server. If the ID field is empty, an error message is displayed.
     */
    @FXML
    private void handleLookup() {
        String id = lookupIdField.getText().trim();
        if (id.isEmpty()) { showError("Please enter an ID."); return; }
        hideAll();
        ClientUI.client.setHandler(this);
        ClientUI.client.sendMessage(new ClientServerMessage(Command.LOOKUP_SUBSCRIBER, id));
    }

    /**
     * Handles the server response for user lookup.
     * The method displays subscriber details, employee details, or a not-found message
     * according to the response data returned by the server.
     *
     * @param msg the message received from the server
     */
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

    /**
     * Adds a formatted information row to the result card.
     *
     * @param parent the VBox that contains the result rows
     * @param label the field name displayed on the left side of the row
     * @param value the field value displayed on the right side of the row
     * @param accent the CSS color used as the row accent
     */
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

    /**
     * Displays the result card and hides the error message.
     */
    private void showCard() {
        resultCard.setVisible(true);
        resultCard.setManaged(true);
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
    }

    /**
     * Displays an error message and hides the result card.
     *
     * @param msg the error message to display
     */
    private void showError(String msg) {
        errorLabel.setText(msg);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
        resultCard.setVisible(false);
        resultCard.setManaged(false);
    }

    /**
     * Hides both the result card and the error message.
     */
    private void hideAll() {
        resultCard.setVisible(false);
        resultCard.setManaged(false);
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
    }

    /**
     * Converts an empty or null value into a default display value.
     *
     * @param v the value to check
     * @return the original value, or N/A if the value is null or empty
     */
    private String orNA(String v) { return (v == null || v.isEmpty()) ? "N/A" : v; }

    /**
     * Converts an internal employee role value into a user-friendly display label.
     *
     * @param role the internal role value
     * @return the formatted role label
     */
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

    /**
     * Handles server disconnection by displaying an error message on the screen.
     *
     * @param r the reason for the disconnection
     */
    @Override
    public void onDisconnected(String r) {
        Platform.runLater(() -> showError("Server disconnected."));
    }
}
