package client;

import java.net.InetAddress;

import common.ClientServerMessage;
import common.Command;
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
        ClientUI.markDisconnected();

        if (handler != null) {
            handler.onDisconnected("Disconnected from server. Please reconnect before continuing.");
        }
    }

    @Override
    protected void connectionException(Exception ex) {
        ClientUI.markDisconnected();

        if (handler != null) {
            handler.onDisconnected("Server connection lost. Please reconnect before continuing.");
        }
    }

    public boolean sendMessage(ClientServerMessage msg) {
        if (!isConnected()) {
            ClientUI.markDisconnected();

            if (handler != null) {
                handler.onDisconnected("Cannot perform this action because the server is disconnected.");
            }

            return false;
        }

        try {
            sendToServer(msg);
            return true;
        } catch (Exception e) {
            ClientUI.markDisconnected();

            if (handler != null) {
                handler.onDisconnected("Failed to send request. Server may be disconnected.");
            }

            return false;
        }
    }
}
