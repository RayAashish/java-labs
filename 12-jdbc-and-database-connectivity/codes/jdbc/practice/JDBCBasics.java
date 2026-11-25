package jdbc.practice;

import jdbc.config.DatabaseConnection;
import jdbc.models.Employee;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import java.util.Set;

/**
 * Fundamental JDBC concepts and general development practices.
 * <p>
 * Demonstrates:
 * 1. Establishing connections via {@link DatabaseConnection}.
 * 2. Parameterized SELECT queries returning {@link Optional}.
 * 3. Safe dynamic updates using SQL column whitelisting (defending against SQL injection).
 * 4. Iterating {@link ResultSet} using both column indexes and column names.
 * 5. Deterministic resource cleanup with try-with-resources.
 */
public class JDBCBasics {

    // Whitelist of allowed column names to prevent SQL injection in dynamic UPDATEs
    private static final Set<String> ALLOWED_COLUMNS = Set.of("name", "email", "age");

    public static void main(String[] args) {
        System.out.println("=== 1. Inspecting Current Database State ===");
        printAllEmployees();

        System.out.println("\n=== 2. Parameterized Query With Optional ===");
        int testId = 1;
        Optional<String> employeeName = getNameById(testId);
        employeeName.ifPresentOrElse(
                name -> System.out.printf("Employee found with ID %d: %s%n", testId, name),
                () -> System.out.printf("No employee found with ID: %d%n", testId)
        );

        System.out.println("\n=== 3. Safe Dynamic Column Update (Whitelisted) ===");
        int updatedRows = updateEmployeeField("email", "aashish.updated@example.com", testId);
        System.out.printf("Rows updated for ID %d: %d%n", testId, updatedRows);

        System.out.println("\n=== 4. Attempting Invalid Column Update (Security Check) ===");
        try {
            updateEmployeeField("salary; DROP TABLE employees; --", "99999", testId);
        } catch (IllegalArgumentException e) {
            System.out.println("Security check passed! Caught illegal column name: " + e.getMessage());
        }

        System.out.println("\n=== 5. Final State in Database ===");
        printAllEmployees();
    }

    /**
     * Traverses the ResultSet and prints all employee records.
     * Uses try-with-resources to ensure Connection, PreparedStatement, and ResultSet
     * are automatically closed in reverse order of creation.
     */
    public static void printAllEmployees() {
        String query = "SELECT id, name, email, age FROM employees ORDER BY id";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(query);
             ResultSet resultSet = preparedStatement.executeQuery()) {

            System.out.printf("%-5s %-18s %-28s %-5s%n", "ID", "NAME", "EMAIL", "AGE");
            System.out.println("----------------------------------------------------------------");
            while (resultSet.next()) {
                int id = resultSet.getInt("id");
                String name = resultSet.getString("name");
                String email = resultSet.getString("email");
                int age = resultSet.getInt("age");
                System.out.printf("%-5d %-18s %-28s %-5d%n", id, name, email, age);
            }
        } catch (SQLException e) {
            System.err.println("[ERROR] Failed to read employees: " + e.getMessage());
        }
    }

    /**
     * Fetches an employee's name by ID using a parameterized query.
     * Returns an Optional to avoid returning null and forcing null checks.
     *
     * @param id employee primary key
     * @return Optional containing the name if found, or empty
     */
    public static Optional<String> getNameById(int id) {
        String query = "SELECT name FROM employees WHERE id = ?";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement ps = connection.prepareStatement(query)) {

            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.ofNullable(rs.getString("name"));
                }
            }
        } catch (SQLException e) {
            System.err.println("[ERROR] Query failed: " + e.getMessage());
        }
        return Optional.empty();
    }

    /**
     * Dynamically updates a specific column of an employee.
     * <p>
     * <b>Senior Engineering Note:</b>
     * Column and table names CANNOT be bound using SQL placeholders ({@code ?}).
     * Any string concatenation in SQL creates a potential SQL injection vulnerability.
     * To safely support dynamic column updates, we strictly validate against a rigid
     * whitelist of known column names before concatenating into the query string.
     *
     * @param columnName   the column to update (name, email, age)
     * @param replaceValue new value as String
     * @param id           target employee ID
     * @return number of affected rows
     */
    public static int updateEmployeeField(String columnName, String replaceValue, int id) {
        if (columnName == null || !ALLOWED_COLUMNS.contains(columnName.trim().toLowerCase())) {
            throw new IllegalArgumentException("Forbidden or unknown column name: " + columnName);
        }

        String safeColumn = columnName.trim().toLowerCase();
        String query = "UPDATE employees SET " + safeColumn + " = ? WHERE id = ?";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement ps = connection.prepareStatement(query)) {

            if ("age".equals(safeColumn)) {
                ps.setInt(1, Integer.parseInt(replaceValue));
            } else {
                ps.setString(1, replaceValue);
            }
            ps.setInt(2, id);

            return ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[ERROR] Update failed: " + e.getMessage());
        } catch (NumberFormatException e) {
            System.err.println("[ERROR] Invalid number format for age: " + replaceValue);
        }
        return 0;
    }
}
