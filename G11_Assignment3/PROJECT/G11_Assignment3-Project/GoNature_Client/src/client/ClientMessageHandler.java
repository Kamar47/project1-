package client;

import common.ClientServerMessage;
/**
 * Interface for client-side controllers that receive responses from the server.
 * Each active screen implements this interface in order to handle server messages
 * and server disconnection events.
 */
public interface ClientMessageHandler {
	/**
     * Handles a message received from the server.
     *
     * @param msg the message received from the server
     */
    void handleMessage(ClientServerMessage msg);
    /**
     * Handles a server disconnection event.
     *
     * @param reason the reason for the disconnection
     */
    void onDisconnected(String reason);
}
