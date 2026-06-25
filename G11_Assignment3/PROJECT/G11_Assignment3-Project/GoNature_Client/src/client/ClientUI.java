package client;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * JavaFX application entry point for the GoNature client.
 * <p>
 * Launches the application, loads the home page ({@code HomePageFrame.fxml}),
 * applies the global CSS stylesheet, and provides static helpers for
 * server connectivity ({@link #isServerConnected()}, {@link #markDisconnected()},
 * {@link #connectToServer(String, int)}).
 * </p>
 * <p>
 * The static {@link #client} reference holds the active {@link GoNatureClient}
 * connection and is set to {@code null} on disconnect.
 * </p>
 *
 * @author Group 11
 */
public class ClientUI extends Application {
    public static GoNatureClient client;
    /**
     * Checks whether the client is currently connected to the server.
     *
     * @return true if the client exists and the connection is active, otherwise false
     */
    public static boolean isServerConnected() {
        return client != null && client.isConnected();
    }

    /**
     * Marks the client as disconnected by clearing the active client connection.
     */
    public static void markDisconnected() {
        client = null;
    }
    /**
     * Starts the GoNature client application window.
     * This method loads the home page FXML file, applies the CSS stylesheet,
     * configures the main stage, and handles application closing.
     *
     * @param primaryStage the main JavaFX stage
     * @throws Exception if the FXML file cannot be loaded
     */
    @Override
    public void start(Stage primaryStage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/gui/HomePageFrame.fxml"));
        Parent root = loader.load();
        Scene scene = new Scene(root, 800, 600);
        // Load CSS stylesheet
        try {
            String css = getClass().getResource("/styles/styles.css").toExternalForm();
            scene.getStylesheets().add(css);
        } catch (Exception e) { System.out.println("CSS not found, using defaults."); }

        primaryStage.setTitle("GoNature - Welcome");
        primaryStage.setScene(scene);
        primaryStage.setOnCloseRequest(e -> {
            if (client != null && client.isConnected()) {
                try { client.closeConnection(); } catch (Exception ex) {}
            }
            Platform.exit(); System.exit(0);
        });
        primaryStage.show();
    }

    /**
     * Creates a new connection to the GoNature server.
     *
     * @param host the server host address
     * @param port the server port number
     * @throws Exception if the connection to the server fails
     */
    public static void connectToServer(String host, int port) throws Exception {
        client = new GoNatureClient(host, port);
        client.openConnection();
    }

    /**
     * Launches the GoNature client application.
     *
     * @param args command-line arguments
     */
    public static void main(String[] args) { launch(args); }
}
