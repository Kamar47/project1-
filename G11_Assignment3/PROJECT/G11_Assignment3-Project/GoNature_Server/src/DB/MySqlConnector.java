package DB;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Singleton that manages the single shared JDBC {@link java.sql.Connection}
 * to the GoNature MySQL database.
 * <p>
 * Because the OCSF framework creates one thread per connected client,
 * all database access must be synchronized on {@link #DB_LOCK} to prevent
 * concurrent modification of the shared connection.
 * </p>
 * <p>
 * Usage: call {@link #getInstance()} to obtain the singleton, then
 * {@link #connect(String, String, String)} before the server starts handling clients.
 * </p>
 *
 * @author Group 11
 */
public class MySqlConnector {
    private static MySqlConnector instance;
    private Connection connection;

    /**
     * Global lock used to synchronize all database access.
     * This is required because the server uses a shared database connection
     * while multiple client threads may access the database at the same time.
     */
    public static final Object DB_LOCK = new Object();

    /**
     * Private constructor used to enforce the singleton pattern.
     */
    private MySqlConnector() {}

    /**
     * Returns the single instance of the MySQL connector.
     * If the instance does not exist yet, it is created.
     *
     * @return the singleton MySqlConnector instance
     */
    public static MySqlConnector getInstance() {
        if (instance == null) instance = new MySqlConnector();
        return instance;
    }

    /**
     * Opens a connection to the GoNature MySQL database.
     * The method loads the MySQL JDBC driver and creates the database connection.
     *
     * @param url the database connection URL
     * @param user the database username
     * @param pass the database password
     * @throws SQLException if the JDBC driver is missing or the connection fails
     */
    public void connect(String url, String user, String pass) throws SQLException {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            connection = DriverManager.getConnection(url, user, pass);
            System.out.println("[DB] Connected to database.");
        } catch (ClassNotFoundException e) {
            throw new SQLException("MySQL JDBC Driver not found.", e);
        }
    }

    /**
     * Returns the active database connection.
     *
     * @return the active JDBC connection, or null if no connection was opened
     */
    public Connection getConnection() { return connection; }

    /**
     * Closes the active database connection if it is open.
     */
    public void disconnect() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                System.out.println("[DB] Disconnected from database.");
            }
        } catch (SQLException e) { e.printStackTrace(); }
    }
}