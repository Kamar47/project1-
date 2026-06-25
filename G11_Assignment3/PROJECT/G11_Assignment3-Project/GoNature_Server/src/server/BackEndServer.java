package server;

import DB.DatabaseController;
import DB.MySqlConnector;
import common.ClientServerMessage;
import ocsf.server.AbstractServer;
import ocsf.server.ConnectionToClient;

/**
 * The main server class for the GoNature system, extending the OCSF {@link ocsf.server.AbstractServer}.
 * <p>
 * Responsibilities:
 * </p>
 * <ul>
 *   <li>Accepts incoming TCP connections from GoNature clients.</li>
 *   <li>Connects to the MySQL database via {@link DB.MySqlConnector}.</li>
 *   <li>Delegates every client message to {@link MessageHandler}.</li>
 *   <li>Starts and stops the {@link ReminderThread} background process.</li>
 *   <li>Tracks connected clients and updates the server GUI ({@link ServerController}).</li>
 *   <li>Auto-logs out employees and releases traveler sessions on disconnect.</li>
 * </ul>
 *
 * @author Group 11
 */
public class BackEndServer extends AbstractServer {
    private DatabaseController dbController;
    private ServerController uiController;
    private ReminderThread reminderThread;

    /**
     * Creates a new GoNature server that listens on the given port.
     *
     * @param port the TCP port used by the server
     */
    public BackEndServer(int port) { super(port); }

    /**
     * Sets the server GUI controller used for updating the connected clients table
     * and writing log messages to the server window.
     *
     * @param controller the server GUI controller
     */
    public void setUiController(ServerController controller) { this.uiController = controller; }

    /**
     * Connects the server to the MySQL database and initializes the database controller.
     *
     * @param url the database connection URL
     * @param user the database username
     * @param pass the database password
     * @throws Exception if the database connection fails
     */
    public void connectDB(String url, String user, String pass) throws Exception {
        MySqlConnector.getInstance().connect(url, user, pass);
        dbController = new DatabaseController(MySqlConnector.getInstance().getConnection());
    }

    /**
     * Handles server startup.
     * The method writes a startup log message and starts the background reminder thread.
     */
    @Override
    protected void serverStarted() {
        log("Server started on port " + getPort());
        // Start background reminder thread
        reminderThread = new ReminderThread(dbController, this);
        reminderThread.start();
    }

    /**
     * Handles a message received from a connected client.
     * The method validates the message type, logs the received command,
     * and delegates the request to the message handler.
     *
     * @param msg the message received from the client
     * @param client the client connection that sent the message
     */
    @Override
    protected void handleMessageFromClient(Object msg, ConnectionToClient client) {
        if (!(msg instanceof ClientServerMessage)) return;
        ClientServerMessage csMsg = (ClientServerMessage) msg;
        log("Received: " + csMsg.getCommand() + " from " + client.getInfo("IP"));
        MessageHandler.handle(csMsg, client, dbController, this);
    }

    /**
     * Handles a new client connection.
     * The method stores the client IP address, writes a log message,
     * and updates the server GUI clients table.
     *
     * @param client the connected client
     */
    @Override
    protected void clientConnected(ConnectionToClient client) {
        String ip = client.getInetAddress().getHostAddress();
        client.setInfo("IP", ip);
        log("Client connected: " + ip);
        if (uiController != null) uiController.addClient(ip, "...", "Connected");
    }

    /**
     * Handles client disconnection by processing logout and cleanup actions.
     *
     * @param client the disconnected client
     */
    @Override
    synchronized protected void clientDisconnected(ConnectionToClient client) { processDisconnect(client); }

    /**
     * Handles a client communication exception by processing the disconnect cleanup.
     *
     * @param client the client whose connection caused the exception
     * @param ex the exception that occurred during communication
     */
    @Override
    synchronized protected void clientException(ConnectionToClient client, Throwable ex) { processDisconnect(client); }

    /**
     * Performs cleanup actions after a client disconnects.
     * The method prevents duplicate disconnect handling, logs out connected employees,
     * releases traveler sessions, and removes the client from the server GUI.
     *
     * @param client the disconnected client
     */
    private void processDisconnect(ConnectionToClient client) {
        if (client.getInfo("Disconnected") == null) {
            client.setInfo("Disconnected", true);
            String ip = client.getInfo("IP") != null ? (String) client.getInfo("IP") : "unknown";
            String host = client.getInfo("HostName") != null ? (String) client.getInfo("HostName") : "unknown";
            log("Client disconnected: " + ip + " | " + host);
            // Auto-logout worker
            if (client.getInfo("LoggedInEmployeeId") != null) {
                try {
                    int empId = (int) client.getInfo("LoggedInEmployeeId");
                    dbController.workerLogout(empId);
                    log("Auto-logged out employee #" + empId + " on disconnect.");
                } catch (Exception e) { log("Error auto-logout: " + e.getMessage()); }
            }
            // Auto-release traveler session so same ID can log in again
            if (client.getInfo("travelerSession") != null) {
                try {
                    String tid = (String) client.getInfo("travelerSession");
                    server.MessageHandler.releaseTravelerSession(tid);
                    log("Auto-released traveler session: " + tid);
                } catch (Exception e) { log("Error releasing traveler session: " + e.getMessage()); }
            }
            if (uiController != null) uiController.removeClient(ip);
        }
    }

    /**
     * Handles server stop events by writing a log message.
     */
    @Override protected void serverStopped() { log("Server stopped."); }
    /**
     * Handles server shutdown.
     * The method stops the background reminder thread and disconnects from the database.
     */
    @Override protected void serverClosed() {
        log("Server closed.");
        if (reminderThread != null) reminderThread.stopRunning();
        MySqlConnector.getInstance().disconnect();
    }

    /**
     * Updates the server GUI with the resolved host name of a connected client.
     *
     * @param ip the client IP address
     * @param hostName the resolved client host name
     */
    public void updateClientInUI(String ip, String hostName) {
        if (uiController != null) {
            uiController.removeClient(ip);
            uiController.addClient(ip, hostName, "Connected");
        }
    }

    /**
     * Writes a message to the console and to the server GUI log area.
     *
     * @param msg the message to log
     */
    public void log(String msg) {
        System.out.println("[Server] " + msg);
        if (uiController != null) uiController.appendLog(msg);
    }
}