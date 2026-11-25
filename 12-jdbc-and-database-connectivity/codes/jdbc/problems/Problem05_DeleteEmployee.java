package jdbc.problems;

import jdbc.config.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * Problem 05: Delete Operation & Verification
 * <p>
 * <b>Goal:</b>
 * Remove a record safely from the database and handle existence checks.
 * <p>
 * <b>Requirements:</b>
 * - Method {@code int deleteEmployee(int id)}.
 * - Verify if record was actually deleted.
 * <p>
 * <b>Junior vs Senior POV:</b>
 * - <i>Junior:</i> Blindly issues {@code DELETE FROM ...}.
 * - <i>Senior:</i>
 *   - Checks for foreign key constraints (`ON DELETE CASCADE` vs `ON DELETE RESTRICT` vs `ON DELETE SET NULL`).
 *   - Employs soft-deletes (e.g., `is_deleted = true`, `deleted_at = NOW()`) in production systems
 *     to preserve audit trails and historical compliance.
 *   - Inspects affected rows to inform caller whether entity existed.
 */
public class Problem05_DeleteEmployee {

    public static void main(String[] args) {
        System.out.println("=== Problem 05: Delete Operation ===");

        // First insert a temporary employee to delete
        int tempId = Problem03_CreateEmployeeGeneratedKeys.createEmployee(
                new jdbc.models.Employee("Temp To Delete", "temp." + System.currentTimeMillis() + "@test.com", 20)
        );
        System.out.println("Created temp employee with ID: " + tempId);

        // Delete the newly created employee
        int rows = deleteEmployee(tempId);
        if (rows > 0) {
            System.out.printf("Employee with ID %d successfully deleted!%n", tempId);
        } else {
            System.out.printf("Failed to delete employee with ID %d (Not found).%n", tempId);
        }

        // Attempting to delete again should report 0
        int secondAttempt = deleteEmployee(tempId);
        System.out.printf("Second deletion attempt returned: %d rows (Expected: 0)%n", secondAttempt);
    }

    /**
     * Deletes an employee by primary key.
     *
     * @param id employee ID
     * @return number of affected rows
     */
    public static int deleteEmployee(int id) {
        String query = "DELETE FROM employees WHERE id = ?";

        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(query)) {

            ps.setInt(1, id);
            return ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[ERROR] Failed to delete employee: " + e.getMessage());
            return 0;
        }
    }
}
