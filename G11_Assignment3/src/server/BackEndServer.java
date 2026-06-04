package server;

import java.io.IOException;
import common.*;
import DB.*;
import ocsf.server.*;

public class BackEndServer extends AbstractServer {
    private DatabaseController dbController;
    private ServerController uiController;

    public BackEndServer(int port) { super(port); }

    public void setUiController(ServerController controller) { this.uiController = controller; }

    public void connectDB(String url, String user, String pass) throws Exception {
        MySqlConnector.getInstance().connect(url, user, pass);
        dbController = new DatabaseController(MySqlConnector.getInstance().getConnection());
    }

    @Override
    protected void handleMessageFromClient(Object msg, ConnectionToClient client) {
        if (!(msg instanceof ClientServerMessage)) return;
        ClientServerMessage csMsg = (ClientServerMessage) msg;
        log("Received: " + csMsg.getCommand() + " from " + client.getInfo("IP"));
        MessageHandler.handle(csMsg, client, dbController, this);
    }

    @Override
    protected void clientConnected(ConnectionToClient client) {
        String ip = client.getInetAddress().getHostAddress();
        client.setInfo("IP", ip);
        log("Client connected: " + ip);
        if (uiController != null) uiController.addClient(ip, "...", "Connected");
    }

    @Override
    synchronized protected void clientDisconnected(ConnectionToClient client) { processDisconnect(client); }

    @Override
    synchronized protected void clientException(ConnectionToClient client, Throwable ex) { processDisconnect(client); }

    private void processDisconnect(ConnectionToClient client) {
        if (client.getInfo("Disconnected") == null) {
            client.setInfo("Disconnected", true);
            String ip = client.getInfo("IP") != null ? (String) client.getInfo("IP") : "unknown";
            String host = client.getInfo("HostName") != null ? (String) client.getInfo("HostName") : "unknown";
            log("Client disconnected: " + ip + " | " + host);
            if (uiController != null) uiController.removeClient(ip);
        }
    }

    @Override protected void serverStarted() { log("Server started on port " + getPort()); }
    @Override protected void serverStopped() { log("Server stopped."); }
    @Override protected void serverClosed() {
        log("Server closed.");
        MySqlConnector.getInstance().disconnect();
    }

    public void log(String msg) {
        System.out.println("[Server] " + msg);
        if (uiController != null) uiController.appendLog(msg);
    }
}
