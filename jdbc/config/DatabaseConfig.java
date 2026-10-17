package jdbc.config;

/**
 * Database configuration settings for the JDBC laboratory.
 * Supports configurable connection properties through environment variables
 * and JVM system properties with resilient local development defaults.
 */
public final class DatabaseConfig {

    private DatabaseConfig() {
        // Prevent instantiation
    }

    private static final String DEFAULT_HOST = "localhost";
    private static final String DEFAULT_PORT = "3306";
    private static final String DEFAULT_DB = "jdbclab";
    private static final String DEFAULT_USER = "root";
    private static final String DEFAULT_PASS = "Raysi@2002";

    public static String getHost() {
        return getEnvOrProp("DB_HOST", DEFAULT_HOST);
    }

    public static String getPort() {
        return getEnvOrProp("DB_PORT", DEFAULT_PORT);
    }

    public static String getDatabaseName() {
        return getEnvOrProp("DB_NAME", DEFAULT_DB);
    }

    public static String getUser() {
        return getEnvOrProp("DB_USER", DEFAULT_USER);
    }

    public static String getPassword() {
        return getEnvOrProp("DB_PASSWORD", DEFAULT_PASS);
    }

    /**
     * Constructs the JDBC URL.
     * Crucial Senior Note:
     * - 'rewriteBatchedStatements=true': Compresses individual inserts into a multi-row single query on MySQL,
     *   boosting batch insert throughput by up to 50x.
     * - 'useSSL=false' & 'allowPublicKeyRetrieval=true': Smooth local development without TLS cert failures.
     * - 'serverTimezone=UTC': Eliminates timezone mismatch anomalies between Java JVM and MySQL server.
     */
    public static String getUrl() {
        String customUrl = getEnvOrProp("JDBC_URL", null);
        if (customUrl != null && !customUrl.isBlank()) {
            return customUrl;
        }
        return String.format(
                "jdbc:mysql://%s:%s/%s?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&rewriteBatchedStatements=true",
                getHost(), getPort(), getDatabaseName()
        );
    }

    private static String getEnvOrProp(String key, String defaultValue) {
        String envVal = System.getenv(key);
        if (envVal != null && !envVal.isBlank()) {
            return envVal;
        }
        String propVal = System.getProperty(key);
        if (propVal != null && !propVal.isBlank()) {
            return propVal;
        }
        return defaultValue;
    }
}
