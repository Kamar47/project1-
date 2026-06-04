package client;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class ClientUI extends Application {
    public static GoNatureClient client;

    @Override
    public void start(Stage primaryStage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/gui/HomePageFrame.fxml"));
        Parent root = loader.load();
        primaryStage.setTitle("GoNature - Welcome");
        primaryStage.setScene(new Scene(root, 800, 600));
        primaryStage.setOnCloseRequest(e -> {
            if (client != null && client.isConnected()) {
                try { client.closeConnection(); } catch (Exception ex) {}
            }
            Platform.exit(); System.exit(0);
        });
        primaryStage.show();
    }

    public static void connectToServer(String host, int port) throws Exception {
        client = new GoNatureClient(host, port);
        client.openConnection();
    }

    public static void main(String[] args) { launch(args); }
}
