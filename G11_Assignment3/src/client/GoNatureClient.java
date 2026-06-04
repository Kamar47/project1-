package client;

import java.net.InetAddress;
import common.*;
import ocsf.client.AbstractClient;

public class GoNatureClient extends AbstractClient {
    public static GoNatureClient instance;
    private ClientMessageHandler handler;

    public GoNatureClient(String host, int port) { super(host, port); instance = this; }

    public void setHandler(ClientMessageHandler handler) { this.handler = handler; }

    @Override
    protected void handleMessageFromServer(Object msg) {
        if (msg instanceof ClientServerMessage && handler != null) {
            handler.handleMessage((ClientServerMessage) msg);
        }
    }

    @Override
    protected void connectionEstablished() {
        try {
            String hostName = InetAddress.getLocalHost().getHostName();
            sendToServer(new ClientServerMessage(Command.CLIENT_CONNECT, hostName));
        } catch (Exception e) {}
    }

    @Override
    protected void connectionClosed() {
        if (handler != null) handler.onDisconnected("Disconnected from server.");
    }

    @Override
    protected void connectionException(Exception ex) {
        if (handler != null) handler.onDisconnected("Disconnected from server.");
    }

    public void sendMessage(ClientServerMessage msg) {
        try { sendToServer(msg); } catch (Exception e) { System.err.println("Send error: " + e.getMessage()); }
    }
}
