package client;

import common.ClientServerMessage;

public interface ClientMessageHandler {
    void handleMessage(ClientServerMessage msg);
    void onDisconnected(String reason);
}
