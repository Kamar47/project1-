package server;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * JavaFX application entry point for the GoNature server.
 * <p>
 * This class loads the server GUI from the FXML file, creates the main server window,
 * connects it to the {@link ServerController}, and handles application shutdown.
 * </p>
 *
 * @author Group 11
 */
public class ServerApp extends Application {
	/**
	 * Starts the GoNature server GUI.
	 * The method loads the FXML layout, creates the main scene, sets the window title,
	 * and defines the close action that stops the server before exiting the application.
	 *
	 * @param primaryStage the main JavaFX stage
	 * @throws Exception if the FXML file cannot be loaded
	 */
	@Override
    public void start(Stage primaryStage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("ServerGUI.fxml"));
        Parent root = loader.load();
        primaryStage.setTitle("GoNature Server - Group 11");
        primaryStage.setScene(new Scene(root, 550, 600));
        ServerController controller = loader.getController();
        primaryStage.setOnCloseRequest(e -> { controller.handleStop(); Platform.exit(); System.exit(0); });
        primaryStage.show();
    }
	/**
	 * Launches the JavaFX server application.
	 *
	 * @param args command-line arguments
	 */
    public static void main(String[] args) { launch(args); }
}
