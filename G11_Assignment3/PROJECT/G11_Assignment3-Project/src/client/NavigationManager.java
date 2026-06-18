package client;

import java.io.IOException;
import javafx.event.Event;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

public class NavigationManager {

    /** Applies the global stylesheet to any scene */
    private static void applyTheme(Scene scene) {
        try {
            scene.getStylesheets().add(NavigationManager.class.getResource("/styles/styles.css").toExternalForm());
        } catch (Exception e) {
            System.out.println("styles.css not found, using default styling.");
        }
    }

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

    public static void openPageInCenter(BorderPane borderPane, String fxmlFile) throws IOException {
        FXMLLoader loader = new FXMLLoader(NavigationManager.class.getResource("/gui/" + fxmlFile));
        Node page = loader.load();
        borderPane.setCenter(page);
    }

    public static void closeWindow(Event event) {
        ((Stage) ((Node) event.getSource()).getScene().getWindow()).close();
    }
}