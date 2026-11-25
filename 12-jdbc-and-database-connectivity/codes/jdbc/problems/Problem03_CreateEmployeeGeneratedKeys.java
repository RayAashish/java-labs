package jdbc.problems;

import jdbc.config.DatabaseConnection;
import jdbc.models.Employee;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Problem 03: Generated Keys (Auto-Increment ID Retrieval)
 * <p>
 * <b>Goal:</b>
 * Learn how to capture database-generated primary keys without running a secondary query.
 * <p>
 * <b>Requirements:</b>
 * - Insert an {@link Employee}.
 * - Instruct driver to retain generated keys via {@link Statement#RETURN_GENERATED_KEYS}.
 * - Extract and return the newly generated primary key.
 * <p>
 * <b>Junior vs Senior POV:</b>
 * - <i>Junior Anti-Pattern:</i> Executes {@code INSERT}, followed by {@code SELECT MAX(id) FROM employees}
 *   or {@code SELECT id FROM employees WHERE email = ?}. In a multi-threaded or multi-instance application,
 *   race conditions occur instantly, returning the wrong ID!
 * - <i>Senior Practice:</i> Uses {@link Statement#RETURN_GENERATED_KEYS} and retrieves the key atomically
 *   from the socket response packet via {@link PreparedStatement#getGeneratedKeys()}.
 */
public class Problem03_CreateEmployeeGeneratedKeys {

    public static void main(String[] args) {
        System.out.println("=== Problem 03: Generated Keys Retrieval ===");

        Employee emp = new Employee(
                "Shiyaram Patel",
                "shiyaram." + System.currentTimeMillis() + "@example.com",
                55
        );

        int newId = createEmployee(emp);
        System.out.printf("Employee created successfully! Generated Database ID: %d%n", newId);
        System.out.println("Updated Entity State: " + emp);
    }

    /**
     * Inserts an employee entity and populates its auto-generated primary key.
     *
     * @param e Employee entity
     * @return generated ID, or -1 on failure
     */
    public static int createEmployee(Employee e) {
        String query = "INSERT INTO employees (name, email, age, department_id) VALUES (?, ?, ?, ?)";

        try (Connection con = DatabaseConnection.getConnection();
             // Crucial: instruct driver that generated keys must be made accessible
             PreparedStatement ps = con.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, e.getName());
            ps.setString(2, e.getEmail());
            ps.setInt(3, e.getAge());
            if (e.getDepartmentId() != null) {
                ps.setInt(4, e.getDepartmentId());
            } else {
                ps.setNull(4, java.sql.Types.INTEGER);
            }

            int rowsAffected = ps.executeUpdate();
            if (rowsAffected > 0) {
                // Read the generated keys cursor
                try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        int generatedId = generatedKeys.getInt(1);
                        e.setId(generatedId); // Synchronize in-memory entity with database state
                        return generatedId;
                    }
                }
            }
        } catch (SQLException ex) {
            System.err.println("[ERROR] Failed to insert employee with generated keys: " + ex.getMessage());
        }
        return -1;
    }
}
