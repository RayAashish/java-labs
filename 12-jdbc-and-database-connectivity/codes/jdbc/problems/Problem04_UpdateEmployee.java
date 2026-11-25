package jdbc.problems;

import jdbc.config.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * Problem 04: Update Operation & Rows Affected Validation
 * <p>
 * <b>Goal:</b>
 * Execute an UPDATE statement and validate affected row count.
 * <p>
 * <b>Requirements:</b>
 * - Method {@code int updateEmployeeEmail(int id, String newEmail)}.
 * - Verify whether a row was actually updated or if the target ID did not exist.
 * <p>
 * <b>Junior vs Senior POV:</b>
 * - <i>Junior:</i> Assumes {@code executeUpdate()} returning without an exception means the record was changed.
 * - <i>Senior:</i> Inspects the return integer:
 *   - Return = 0: ID was not found, or row was already identical (MySQL specific: `useAffectedRows=true`).
 *   - Return = 1: Record updated successfully.
 *   - Return > 1: Severe database bug or missing primary key uniqueness!
 */
public class Problem04_UpdateEmployee {

    public static void main(String[] args) {
        System.out.println("=== Problem 04: Update Specific Column ===");

        int existingId = 1;
        String newEmail = "aashish.senior." + (System.currentTimeMillis() % 10000) + "@example.com";
        int rows = updateEmployeeEmail(existingId, newEmail);
        System.out.printf("Updating ID %d: %d row(s) updated%n", existingId, rows);

        int nonExistentId = 999999;
        int missingRows = updateEmployeeEmail(nonExistentId, "nobody@example.com");
        System.out.printf("Updating non-existent ID %d: %d row(s) updated (Expected: 0)%n",
                nonExistentId, missingRows);
    }

    /**
     * Updates an employee's email address by their ID.
     *
     * @param id       employee ID
     * @param newEmail updated email
     * @return count of rows modified
     */
    public static int updateEmployeeEmail(int id, String newEmail) {
        String query = "UPDATE employees SET email = ? WHERE id = ?";

        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(query)) {

            ps.setString(1, newEmail);
            ps.setInt(2, id);

            int rowsAffected = ps.executeUpdate();
            if (rowsAffected == 0) {
                System.out.printf("[INFO] No employee found with ID: %d to update.%n", id);
            }
            return rowsAffected;
        } catch (SQLException e) {
            System.err.println("[ERROR] Failed to update employee email: " + e.getMessage());
            return 0;
        }
    }
}
