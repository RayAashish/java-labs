package jdbc.config;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Centralized JDBC Connection provider.
 * <p>
 * Junior POV:
 * Handles loading the JDBC driver and calling {@link DriverManager#getConnection(String, String, String)}.
 * <p>
 * Senior POV:
 * - Direct {@link DriverManager#getConnection} calls create a brand new TCP socket, SSL handshake,
 *   and authentication exchange for every invocation (expensive: ~50ms - 200ms per connection).
 * - Serves as an abstraction point that can seamlessly delegate to a {@link javax.sql.DataSource}
 *   or connection pool (like HikariCP) in enterprise environments.
 * - Provides defensive utility methods for safe resource cleanup and connection validation.
 */
public final class DatabaseConnection {

    private DatabaseConnection() {
        // Utility class
    }

    static {
        // Modern JDBC 4.0+ automatically discovers drivers on the classpath via java.util.ServiceLoader.
        // Explicit Class.forName is retained for legacy compatibility across older environments.
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            System.err.println("[WARN] MySQL JDBC Driver not found on classpath. " +
                    "Ensure mysql-connector-j-9.x.jar is added to the runtime classpath: " + e.getMessage());
        }
    }

    /**
     * Obtains a new database connection using current configuration.
     *
     * @return an open {@link Connection} instance
     * @throws SQLException if a database access error occurs
     */
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(
                DatabaseConfig.getUrl(),
                DatabaseConfig.getUser(),
                DatabaseConfig.getPassword()
        );
    }

    /**
     * Safely closes any AutoCloseable resource without throwing checked exceptions.
     * Essential for legacy cleanup blocks where try-with-resources cannot be used.
     *
     * @param resources zero or more resources (Connection, Statement, ResultSet)
     */
    public static void closeQuietly(AutoCloseable... resources) {
        if (resources == null) return;
        for (AutoCloseable resource : resources) {
            if (resource != null) {
                try {
                    resource.close();
                } catch (Exception ignored) {
                    // Suppressed deliberately during teardown
                }
            }
        }
    }

    /**
     * Validates whether the database is currently reachable.
     *
     * @return true if connection succeeds and ping returns valid within 2 seconds
     */
    public static boolean testConnection() {
        try (Connection conn = getConnection()) {
            return conn.isValid(2);
        } catch (SQLException e) {
            System.err.println("[ERROR] Database connectivity test failed: " + e.getMessage());
            return false;
        }
    }

    /**
     * Prints database server and driver metadata to standard output.
     */
    public static void printDatabaseInfo() {
        try (Connection conn = getConnection()) {
            DatabaseMetaData meta = conn.getMetaData();
            System.out.println("==================================================");
            System.out.println("Database Product : " + meta.getDatabaseProductName());
            System.out.println("Database Version : " + meta.getDatabaseProductVersion());
            System.out.println("Driver Name      : " + meta.getDriverName());
            System.out.println("Driver Version   : " + meta.getDriverVersion());
            System.out.println("JDBC Version     : " + meta.getJDBCMajorVersion() + "." + meta.getJDBCMinorVersion());
            System.out.println("Connected URL    : " + meta.getURL());
            System.out.println("Current User     : " + meta.getUserName());
            System.out.println("AutoCommit Default: " + conn.getAutoCommit());
            System.out.println("==================================================");
        } catch (SQLException e) {
            System.err.println("[ERROR] Failed to query database metadata: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        System.out.println("Testing database connection to: " + DatabaseConfig.getUrl());
        boolean reachable = testConnection();
        if (reachable) {
            System.out.println("Connection successful!");
            printDatabaseInfo();
        } else {
            System.err.println("Failed to establish connection. Verify MySQL server status and credentials.");
        }
    }
}
