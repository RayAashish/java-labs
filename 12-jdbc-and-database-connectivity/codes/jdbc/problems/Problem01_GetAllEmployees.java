package jdbc.problems;

import jdbc.config.DatabaseConnection;
import jdbc.models.Employee;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Problem 01: Basic Read Operation & ResultSet Mapping
 * <p>
 * <b>Goal:</b>
 * Understand {@link Connection}, {@link PreparedStatement}, and {@link ResultSet} traversal.
 * <p>
 * <b>Requirements:</b>
 * - Query all rows from table 'employees'.
 * - Map each relational tuple into an {@link Employee} domain object.
 * - Collect into a {@link List}.
 * <p>
 * <b>Junior vs Senior POV:</b>
 * - <i>Junior:</i> Uses {@code rs.getString("name")} by column name. Convenient, but involves internal map lookup.
 * - <i>Senior:</i> Understands that 1-based index access ({@code rs.getInt(1)}) is faster, but index-fragile
 *   if SQL column projections change. Always explicitly specify column names in SELECT (never {@code SELECT *}),
 *   and properly handle SQL NULL values for primitives using {@code rs.wasNull()} or {@code rs.getObject()}.
 */
public class Problem01_GetAllEmployees {

    public static void main(String[] args) {
        System.out.println("=== Problem 01: Basic Read Operation ===");
        List<Employee> employeeList = getAllEmployees();

        // Sort employees by age using Comparable implementation
        Collections.sort(employeeList);

        System.out.println("Total Employees Retrieved: " + employeeList.size());
        employeeList.forEach(System.out::println);
    }

    /**
     * Reads all employees from the database and maps them to a list of Employee domain objects.
     *
     * @return List of Employee records
     */
    public static List<Employee> getAllEmployees() {
        List<Employee> result = new ArrayList<>();
        // Explicitly project required columns rather than SELECT * to preserve memory and schema resilience
        String query = "SELECT id, name, email, age, department_id FROM employees ORDER BY id";

        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(query);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                // Column name access
                int id = rs.getInt("id");
                String name = rs.getString("name");
                String email = rs.getString("email");
                int age = rs.getInt("age");

                // Handling nullable column: department_id
                Integer departmentId = (Integer) rs.getObject("department_id");

                result.add(new Employee(id, name, email, age, departmentId));
            }
        } catch (SQLException e) {
            System.err.println("[ERROR] Failed to fetch employees: " + e.getMessage());
        }
        return result;
    }
}
