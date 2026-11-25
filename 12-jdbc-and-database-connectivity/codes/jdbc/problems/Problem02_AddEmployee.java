package jdbc.problems;

import jdbc.config.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * Problem 02: Insert Operation & Parameter Binding
 * <p>
 * <b>Goal:</b>
 * Master parameterized queries and {@link PreparedStatement#executeUpdate()}.
 * <p>
 * <b>Requirements:</b>
 * - Insert employee with name, email, and age.
 * - Return the number of affected rows.
 * <p>
 * <b>Junior vs Senior POV:</b>
 * - <i>Junior:</i> Might concatenate variables directly into SQL string ({@code "VALUES ('" + name + "')"}),
 *   creating catastrophic SQL Injection vulnerabilities.
 * - <i>Senior:</i> Strictly mandates parameter placeholders ({@code ?}). Understands that the database engine
 *   pre-compiles the query template, treating bound values strictly as literal data, preventing injection,
 *   enabling execution plan reuse, and automatically escaping problematic quotes.
 */
public class Problem02_AddEmployee {

    public static void main(String[] args) {
        System.out.println("=== Problem 02: Parameterized Insert Operation ===");

        String name = "Meera Sharma";
        String email = "meera.sharma." + System.currentTimeMillis() + "@example.com";
        int age = 28;

        int rows = addEmployee(name, email, age);
        System.out.printf("addEmployee result: %d row(s) affected%n", rows);
    }

    /**
     * Inserts a new employee record into the database.
     *
     * @param name  employee full name
     * @param email unique email address
     * @param age   employee age
     * @return number of affected rows (typically 1 on success, 0 on failure)
     */
    public static int addEmployee(String name, String email, int age) {
        String query = "INSERT INTO employees (name, email, age) VALUES (?, ?, ?)";

        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(query)) {

            // 1-based parameter indexing
            ps.setString(1, name);
            ps.setString(2, email);
            ps.setInt(3, age);

            return ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[ERROR] Failed to insert employee: " + e.getMessage());
            return 0;
        }
    }
}
