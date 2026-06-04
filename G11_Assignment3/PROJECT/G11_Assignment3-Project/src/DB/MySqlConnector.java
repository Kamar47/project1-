package DB;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class MySqlConnector {
    private static MySqlConnector instance;
    private Connection connection;

    private MySqlConnector() {}

    public static MySqlConnector getInstance() {
        if (instance == null) instance = new MySqlConnector();
        return instance;
    }

    public void connect(String url, String user, String pass) throws SQLException {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            connection = DriverManager.getConnection(url, user, pass);
            System.out.println("[DB] Connected to database.");
        } catch (ClassNotFoundException e) {
            throw new SQLException("MySQL JDBC Driver not found.", e);
        }
    }

    public Connection getConnection() { return connection; }

    public void disconnect() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                System.out.println("[DB] Disconnected from database.");
            }
        } catch (SQLException e) { e.printStackTrace(); }
    }
}