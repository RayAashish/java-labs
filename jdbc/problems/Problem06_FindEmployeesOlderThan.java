package jdbc.problems;

import jdbc.config.DatabaseConnection;
import jdbc.models.Employee;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Problem 06: Search with Condition (Predicates & Index Utilization)
 * <p>
 * <b>Goal:</b>
 * Execute conditional queries and understand SQL predicate pushdown.
 * <p>
 * <b>Requirements:</b>
 * - Method {@code List<Employee> findEmployeesOlderThan(int age)}.
 * <p>
 * <b>CRITICAL SENIOR LESSON: In-Memory Filtering vs Database Pushdown</b>
 * <ul>
 *   <li><b>The Junior Anti-Pattern:</b> Querying {@code SELECT * FROM employees}, transporting all
 *       1,000,000 records across the network, deserializing 1,000,000 Java objects into the JVM heap,
 *       and executing {@code list.stream().filter(e -> e.getAge() >= age)}.
 *       <i>Outcome:</i> Network saturation, GC pauses, JVM OutOfMemoryError, and CPU exhaustion.</li>
 *   <li><b>The Senior Production Pattern:</b> Pushing the filter condition directly into the SQL engine:
 *       {@code SELECT ... WHERE age >= ?}.
 *       <i>Outcome:</i> If an index on {@code age} exists, the DB performs a B-Tree index range scan,
 *       reads only matching disk blocks, and transmits only the required matching rows across the socket.</li>
 * </ul>
 */
public class Problem06_FindEmployeesOlderThan {

    public static void main(String[] args) {
        System.out.println("=== Problem 06: Search with SQL Condition ===");

        int filterAge = 30;

        System.out.println("\n[SENIOR APPROACH] Pushing predicate to Database (WHERE age >= ?):");
        List<Employee> optimizedList = findEmployeesOlderThanDatabasePushdown(filterAge);
        optimizedList.forEach(System.out::println);

        System.out.println("\n[JUNIOR ANTI-PATTERN] In-memory Java Stream filtering (Educational demo):");
        List<Employee> inMemoryList = findEmployeesOlderThanInMemoryAntiPattern(filterAge);
        System.out.printf("In-memory filter produced %d results (Identical count, but terrible performance at scale)%n",
                inMemoryList.size());
    }

    /**
     * SENIOR PRODUCTION PRACTICE:
     * Executes filtering at the database layer using parameterized SQL WHERE clause.
     *
     * @param age minimum age threshold
     * @return List of matching employees
     */
    public static List<Employee> findEmployeesOlderThanDatabasePushdown(int age) {
        List<Employee> result = new ArrayList<>();
        String sql = "SELECT id, name, email, age, department_id FROM employees WHERE age >= ? ORDER BY age ASC";

        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, age);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(new Employee(
                            rs.getInt("id"),
                            rs.getString("name"),
                            rs.getString("email"),
                            rs.getInt("age"),
                            (Integer) rs.getObject("department_id")
                    ));
                }
            }
        } catch (SQLException e) {
            System.err.println("[ERROR] Database query failed: " + e.getMessage());
        }
        return result;
    }

    /**
     * JUNIOR ANTI-PATTERN (Refactored from original submission for educational demonstration):
     * Loads the ENTIRE table into JVM memory and filters with Java Streams.
     * Avoid this pattern in production!
     */
    public static List<Employee> findEmployeesOlderThanInMemoryAntiPattern(int age) {
        // Warning: Fetches ALL rows across network!
        List<Employee> allEmployees = Problem01_GetAllEmployees.getAllEmployees();

        return allEmployees.stream()
                .filter(e -> e.getAge() >= age)
                .toList();
    }
}
