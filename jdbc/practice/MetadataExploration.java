package jdbc.practice;

import jdbc.config.DatabaseConnection;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;

/**
 * Exploration of JDBC Metadata APIs:
 * 1. {@link DatabaseMetaData}: Introspection of the database engine, supported capabilities,
 *    table catalogs, foreign keys, transaction isolation levels, and batch update support.
 * 2. {@link ResultSetMetaData}: Dynamic discovery of column count, column names, column labels,
 *    SQL types, precision, scale, and nullability for runtime generic object mappers.
 * <p>
 * <b>Senior POV:</b>
 * Metadata APIs are the bedrock upon which modern frameworks (Hibernate, Spring Data JDBC, MyBatis)
 * dynamically generate queries, validate entity mappings against physical schemas, and tailor
 * SQL dialects to specific database engines.
 */
public class MetadataExploration {

    public static void main(String[] args) {
        System.out.println("==================================================");
        System.out.println("   JDBC METADATA INTROSPECTION & DISCOVERY        ");
        System.out.println("==================================================");

        inspectDatabaseMetadata();
        System.out.println("\n--------------------------------------------------");
        inspectResultSetMetadata("SELECT id, name, email, age, department_id FROM employees");
    }

    /**
     * Inspects global database capabilities, limits, and supported features.
     */
    public static void inspectDatabaseMetadata() {
        try (Connection con = DatabaseConnection.getConnection()) {
            DatabaseMetaData dbMeta = con.getMetaData();

            System.out.println(">>> 1. Database & Driver Details");
            System.out.println("Product Name            : " + dbMeta.getDatabaseProductName());
            System.out.println("Product Version         : " + dbMeta.getDatabaseProductVersion());
            System.out.println("Driver Name             : " + dbMeta.getDriverName());
            System.out.println("Driver Version          : " + dbMeta.getDriverVersion());
            System.out.println("JDBC Specification      : " + dbMeta.getJDBCMajorVersion() + "." + dbMeta.getJDBCMinorVersion());

            System.out.println("\n>>> 2. Transaction & Concurrency Support");
            System.out.println("Supports Transactions   : " + dbMeta.supportsTransactions());
            System.out.println("Supports Batch Updates  : " + dbMeta.supportsBatchUpdates());
            System.out.println("Supports Savepoints     : " + dbMeta.supportsSavepoints());
            System.out.println("Supports Generated Keys : " + dbMeta.supportsGetGeneratedKeys());
            System.out.println("Default Transaction Isolation: " + getIsolationName(dbMeta.getDefaultTransactionIsolation()));

            System.out.println("\n>>> 3. Concurrency / Query Capabilities");
            System.out.println("Supports Stored Procedures: " + dbMeta.supportsStoredProcedures());
            System.out.println("Max Connections Allowed   : " + (dbMeta.getMaxConnections() == 0 ? "Unlimited/Config-based" : dbMeta.getMaxConnections()));
            System.out.println("Max Columns in Table      : " + dbMeta.getMaxColumnsInTable());
            System.out.println("Max Row Size              : " + dbMeta.getMaxRowSize());

            System.out.println("\n>>> 4. Existing User Tables in Catalog:");
            try (ResultSet tables = dbMeta.getTables("jdbclab", null, "%", new String[]{"TABLE"})) {
                while (tables.next()) {
                    String tableName = tables.getString("TABLE_NAME");
                    String tableType = tables.getString("TABLE_TYPE");
                    System.out.printf("  - %-20s (%s)%n", tableName, tableType);
                }
            }
        } catch (SQLException e) {
            System.err.println("[ERROR] DatabaseMetaData query failed: " + e.getMessage());
        }
    }

    /**
     * Dynamically discovers schema structure of any given query's ResultSet.
     *
     * @param query SQL query to execute and inspect
     */
    public static void inspectResultSetMetadata(String query) {
        System.out.println(">>> Inspecting ResultSetMetaData for SQL: " + query);
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(query);
             ResultSet rs = ps.executeQuery()) {

            ResultSetMetaData rsMeta = rs.getMetaData();
            int columnCount = rsMeta.getColumnCount();
            System.out.println("Total Result Columns: " + columnCount);
            System.out.println("---------------------------------------------------------------------------------------------------");
            System.out.printf("%-4s %-16s %-16s %-16s %-12s %-10s%n",
                    "#", "Column Name", "Column Label", "Data Type", "Type Name", "Nullable?");
            System.out.println("---------------------------------------------------------------------------------------------------");

            for (int i = 1; i <= columnCount; i++) {
                int colIndex = i;
                String colName = rsMeta.getColumnName(colIndex);
                String colLabel = rsMeta.getColumnLabel(colIndex);
                int typeId = rsMeta.getColumnType(colIndex);
                String typeName = rsMeta.getColumnTypeName(colIndex);
                int isNullable = rsMeta.isNullable(colIndex);
                String nullableStr = switch (isNullable) {
                    case ResultSetMetaData.columnNullable -> "YES";
                    case ResultSetMetaData.columnNoNulls -> "NO";
                    default -> "UNKNOWN";
                };

                System.out.printf("%-4d %-16s %-16s %-16d %-12s %-10s%n",
                        colIndex, colName, colLabel, typeId, typeName, nullableStr);
            }
            System.out.println("---------------------------------------------------------------------------------------------------");
        } catch (SQLException e) {
            System.err.println("[ERROR] ResultSetMetaData inspection failed: " + e.getMessage());
        }
    }

    private static String getIsolationName(int level) {
        return switch (level) {
            case Connection.TRANSACTION_NONE -> "TRANSACTION_NONE";
            case Connection.TRANSACTION_READ_UNCOMMITTED -> "TRANSACTION_READ_UNCOMMITTED";
            case Connection.TRANSACTION_READ_COMMITTED -> "TRANSACTION_READ_COMMITTED";
            case Connection.TRANSACTION_REPEATABLE_READ -> "TRANSACTION_REPEATABLE_READ";
            case Connection.TRANSACTION_SERIALIZABLE -> "TRANSACTION_SERIALIZABLE";
            default -> "UNKNOWN (" + level + ")";
        };
    }
}
