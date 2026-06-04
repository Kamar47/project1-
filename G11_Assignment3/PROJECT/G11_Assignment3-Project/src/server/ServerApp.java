package server;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class ServerApp extends Application {
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
    public static void main(String[] args) { launch(args); }
}
