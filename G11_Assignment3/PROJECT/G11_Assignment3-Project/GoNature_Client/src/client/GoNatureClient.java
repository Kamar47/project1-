package client;

import java.net.InetAddress;

import common.ClientServerMessage;
import common.Command;
import ocsf.client.AbstractClient;

/**
 * GoNature client-side network layer, extending the OCSF {@link ocsf.client.AbstractClient}.
 * <p>
 * Handles all TCP communication with the GoNature server. Every screen that needs
 * to interact with the server calls {@link #setHandler(ClientMessageHandler)} to register
 * itself, then calls {@link #sendMessage(ClientServerMessage)}.
 * </p>
 * <p>
 * A separate {@code pollingHandler} is maintained exclusively for notification polling
 * (GET_MY_NOTIFICATIONS responses). This prevents notification responses from being
 * accidentally routed to the active screen's handler (which could corrupt data tables).
 * Notification responses are identified by their inner {@code ArrayList} structure
 * (9 String fields) and notification type.
 * </p>
 *
 * @author Group 11
 */
public class GoNatureClient extends AbstractClient {
    public static GoNatureClient instance;
    private ClientMessageHandler handler;
    // Separate persistent handler for polling — never overwritten by other screens
    private ClientMessageHandler pollingHandler;

    /**
     * Creates a GoNature client and initializes the connection settings.
     *
     * @param host the server host address
     * @param port the server port number
     */
    public GoNatureClient(String host, int port) { super(host, port); instance = this; }

    /**
     * Sets the active client-side message handler.
     * The handler is usually the controller of the currently displayed screen.
     *
     * @param handler the controller that should handle server messages
     */
    public void setHandler(ClientMessageHandler handler) { this.handler = handler; }

    /**
     * Sets a persistent polling handler for notification responses.
     * This handler receives notification polling responses even when another screen is active.
     *
     * @param h the handler that should receive notification polling responses
     */
    public void setPollingHandler(ClientMessageHandler h) { this.pollingHandler = h; }
    
    /**
     * Clears the polling message handler.
     * This is used when a screen that performs background polling is closed or no longer active.
     */
    public void clearPollingHandler() { this.pollingHandler = null; }

    /**
     * Handles a message received from the server.
     * The method forwards the response to the active screen handler or polling handler
     * according to the message type and current client state.
     *
     * @param msg the message received from the server
     */
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

    /**
     * Handles successful connection establishment with the server.
     * This method is called after the client connection is opened.
     */
    @Override
    protected void connectionEstablished() {
        try {
            String hostName = InetAddress.getLocalHost().getHostName();
            sendToServer(new ClientServerMessage(Command.CLIENT_CONNECT, hostName));
        } catch (Exception e) {}
    }

    /**
     * Handles normal server connection closure.
     * The method marks the client as disconnected and notifies the active handler.
     */
    @Override
    protected void connectionClosed() {
        ClientUI.markDisconnected();

        if (handler != null) {
            handler.onDisconnected("Disconnected from server. Please reconnect before continuing.");
        }
    }

    /**
     * Handles unexpected connection errors.
     * The method marks the client as disconnected and notifies the active handler
     * with an appropriate error message.
     *
     * @param ex the connection exception that occurred
     */
    @Override
    protected void connectionException(Exception ex) {
        ClientUI.markDisconnected();

        if (handler != null) {
            handler.onDisconnected("Server connection lost. Please reconnect before continuing.");
        }
    }

    /**
     * Sends a message to the GoNature server.
     * <p>
     * If the client is not connected, {@link #handler} is notified via
     * {@link ClientMessageHandler#onDisconnected(String)} and {@code false} is returned.
     * </p>
     *
     * @param msg the message to send
     * @return {@code true} if the message was sent successfully; {@code false} otherwise
     */
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
