package client;

import java.net.InetAddress;

import common.ClientServerMessage;
import common.Command;
import ocsf.client.AbstractClient;

public class GoNatureClient extends AbstractClient {
    public static GoNatureClient instance;
    private ClientMessageHandler handler;
    // Separate persistent handler for polling — never overwritten by other screens
    private ClientMessageHandler pollingHandler;

    public GoNatureClient(String host, int port) { super(host, port); instance = this; }

    public void setHandler(ClientMessageHandler handler) { this.handler = handler; }

    /** Set a persistent polling handler that receives GET_MY_NOTIFICATIONS responses
     *  regardless of which screen is currently active. */
    public void setPollingHandler(ClientMessageHandler h) { this.pollingHandler = h; }
    public void clearPollingHandler() { this.pollingHandler = null; }

    @Override
    protected void handleMessageFromServer(Object msg) {
        if (!(msg instanceof ClientServerMessage)) return;
        ClientServerMessage csMsg = (ClientServerMessage) msg;
        System.out.println("[Client] Received: " + csMsg.getCommand()
            + " data=" + (csMsg.getData() != null ? csMsg.getData().getClass().getSimpleName() : "null"));

        // Polling handler gets notification responses ONLY.
        // A notification response is: ArrayList<ArrayList<String>> where inner list has 9 elements.
        // This prevents stealing ALT_SLOTS or other ArrayList responses.
        if (pollingHandler != null
                && csMsg.getCommand() == common.Command.DATA_RESPONSE
                && csMsg.getData() instanceof java.util.ArrayList) {
            java.util.ArrayList<?> data = (java.util.ArrayList<?>) csMsg.getData();
            boolean isNotificationResponse = false;
            if (!data.isEmpty() && data.get(0) instanceof java.util.ArrayList) {
                java.util.ArrayList<?> first = (java.util.ArrayList<?>) data.get(0);
                // Notification rows have exactly 9 String fields
                if (first.size() == 9 && first.get(0) instanceof String) {
                    // Check field 2 is notification_type (reminder or waitlist_available)
                    String type = (String) first.get(2);
                    if ("reminder".equals(type) || "waitlist_available".equals(type) || "reminder_expired".equals(type)) {
                        isNotificationResponse = true;
                    }
                }
            } else if (data.isEmpty()) {
                // Empty notification list — also for polling handler
                isNotificationResponse = true;
            }
            if (isNotificationResponse) {
                pollingHandler.handleMessage(csMsg);
                return;
            }
        }

        if (handler != null) {
            handler.handleMessage(csMsg);
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
