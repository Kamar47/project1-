package client;

import java.io.IOException;
import javafx.event.Event;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

/**
 * Utility class for JavaFX navigation within the GoNature client.
 * <p>
 * Provides a helper method to load and display an FXML page inside
 * a {@link javafx.scene.layout.BorderPane} center area, used by all
 * frame controllers (traveler, worker, manager, service rep).
 * </p>
 *
 * @author Group 11
 */
public class NavigationManager {

	/**
	 * Applies the global CSS stylesheet to the given scene.
	 * If the stylesheet cannot be found, the scene remains with the default styling.
	 *
	 * @param scene the JavaFX scene to which the stylesheet should be applied
	 */
    private static void applyTheme(Scene scene) {
        try {
            scene.getStylesheets().add(NavigationManager.class.getResource("/styles/styles.css").toExternalForm());
        } catch (Exception e) {
            System.out.println("styles.css not found, using default styling.");
        }
    }

    /**
     * Opens a new JavaFX window using the given FXML file.
     * The method can optionally hide the current window after opening the new one.
     *
     * @param fxmlFile the name of the FXML file located in the gui package
     * @param event the event that triggered the navigation
     * @param title the title of the new window
     * @param hideCurrent true if the current window should be hidden, otherwise false
     * @throws IOException if the FXML file cannot be loaded
     */
    public static void openPage(String fxmlFile, Event event, String title, boolean hideCurrent) throws IOException {
        Stage currentStage = null;
        if (event != null && event.getSource() instanceof Node) {
            currentStage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        }
        FXMLLoader loader = new FXMLLoader(NavigationManager.class.getResource("/gui/" + fxmlFile));
        Parent pane = loader.load();
        Stage newStage = new Stage();
        Scene scene = new Scene(pane);
        applyTheme(scene);
        newStage.setScene(scene);
        newStage.setTitle(title);
        if (currentStage != null && hideCurrent) currentStage.hide();
        newStage.show();
    }

    /**
     * Loads an FXML page and displays it inside the center area of a BorderPane.
     * This method is used by frame controllers to switch between internal screens.
     *
     * @param borderPane the main BorderPane that contains the screen content
     * @param fxmlFile the name of the FXML file located in the gui package
     * @throws IOException if the FXML file cannot be loaded
     */
    public static void openPageInCenter(BorderPane borderPane, String fxmlFile) throws IOException {
        FXMLLoader loader = new FXMLLoader(NavigationManager.class.getResource("/gui/" + fxmlFile));
        Node page = loader.load();
        borderPane.setCenter(page);
    }

    /**
     * Closes the window from which the given event was triggered.
     *
     * @param event the event triggered from the window that should be closed
     */
    public static void closeWindow(Event event) {
        ((Stage) ((Node) event.getSource()).getScene().getWindow()).close();
    }
}