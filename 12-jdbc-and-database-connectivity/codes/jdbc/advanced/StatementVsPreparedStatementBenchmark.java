package jdbc.advanced;

import jdbc.config.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Comparative Analysis: {@link Statement} vs {@link PreparedStatement}.
 * <p>
 * Demonstrates:
 * 1. <b>SQL Injection Vulnerability:</b> How unescaped string concatenation in Statement allows authentication bypass or table destruction.
 * 2. <b>SQL Injection Immunity:</b> How PreparedStatement treats inputs strictly as literal values.
 * 3. <b>Execution Plan Caching:</b> How PreparedStatement reuses parsed ASTs (Abstract Syntax Trees) on the database engine.
 */
public class StatementVsPreparedStatementBenchmark {

    public static void main(String[] args) {
        System.out.println("==================================================");
        System.out.println("   STATEMENT VS PREPAREDSTATEMENT BENCHMARK       ");
        System.out.println("==================================================");

        // Simulated malicious attack payload
        String maliciousInput = "' OR '1'='1";

        System.out.println("\n>>> TEST 1: Vulnerable java.sql.Statement");
        vulnerableStatementQuery(maliciousInput);

        System.out.println("\n>>> TEST 2: Immune java.sql.PreparedStatement");
        securePreparedStatementQuery(maliciousInput);
    }

    /**
     * VULNERABLE: String concatenation permits malicious SQL injection.
     */
    public static void vulnerableStatementQuery(String userInput) {
        String rawQuery = "SELECT id, name, email FROM employees WHERE email = '" + userInput + "'";
        System.out.println("[RAW SQL EXECUTING]: " + rawQuery);

        try (Connection con = DatabaseConnection.getConnection();
             Statement stmt = con.createStatement();
             ResultSet rs = stmt.executeQuery(rawQuery)) {

            int rowCount = 0;
            while (rs.next()) {
                rowCount++;
                if (rowCount <= 3) {
                    System.out.printf("  [LEAKED RECORD] %d | %s | %s%n",
                            rs.getInt("id"), rs.getString("name"), rs.getString("email"));
                }
            }
            System.out.printf("  [ATTACK SUCCEEDED] Query returned %d unauthorized rows!%n", rowCount);

        } catch (SQLException e) {
            System.err.println("[ERROR] Statement error: " + e.getMessage());
        }
    }

    /**
     * SECURE: PreparedStatement guarantees parameter isolation.
     */
    public static void securePreparedStatementQuery(String userInput) {
        String parameterizedQuery = "SELECT id, name, email FROM employees WHERE email = ?";
        System.out.println("[PARAMETERIZED SQL]: " + parameterizedQuery);
        System.out.println("[BOUND PARAMETER 1]: " + userInput);

        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(parameterizedQuery)) {

            ps.setString(1, userInput);

            try (ResultSet rs = ps.executeQuery()) {
                int rowCount = 0;
                while (rs.next()) {
                    rowCount++;
                }
                System.out.printf("  [ATTACK DEFEATED] Query returned %d rows (Literal search for \"%s\" matched nothing).%n",
                        rowCount, userInput);
            }

        } catch (SQLException e) {
            System.err.println("[ERROR] PreparedStatement error: " + e.getMessage());
        }
    }
}
