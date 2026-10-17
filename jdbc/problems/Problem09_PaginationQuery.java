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
 * Problem 09: Pagination Query (LIMIT & OFFSET)
 * <p>
 * <b>Goal:</b>
 * Retrieve manageable page windows from massive database tables without loading everything into memory.
 * <p>
 * <b>Requirements:</b>
 * - Method {@code List<Employee> getEmployeesPage(int page, int pageSize)}.
 * - Calculate total record count and total pages.
 * - Guard against negative values and SQL injection.
 * <p>
 * <b>Junior vs Senior POV:</b>
 * - <i>Junior:</i> Queries all records and uses {@code list.subList(from, to)}, bringing gigabytes over the wire!
 * - <i>Senior:</i>
 *   - Uses {@code LIMIT ? OFFSET ?} with deterministic {@code ORDER BY} (sorting on non-unique columns causes non-deterministic pagination).
 *   - Calculates offset: {@code offset = (pageNumber - 1) * pageSize}.
 *   - Caveat for giant datasets (e.g. 50 million rows): High offsets ({@code OFFSET 10000000}) become slow in MySQL
 *     because InnoDB still traverses and discards 10,000,000 index entries.
 *     In ultra-high-scale systems, Senior engineers switch to <i>Seek / Keyset Pagination</i>
 *     ({@code WHERE id > last_seen_id ORDER BY id LIMIT 20}).
 */
public class Problem09_PaginationQuery {

    public static void main(String[] args) {
        System.out.println("=== Problem 09: Pagination Query ===");

        int pageSize = 3;
        int totalEmployees = getTotalEmployeeCount();
        int totalPages = (int) Math.ceil((double) totalEmployees / pageSize);

        System.out.printf("Total Records in DB: %d | Page Size: %d | Total Pages: %d%n%n",
                totalEmployees, pageSize, totalPages);

        for (int page = 1; page <= Math.min(3, totalPages); page++) {
            System.out.printf("--- Displaying Page %d of %d ---%n", page, totalPages);
            List<Employee> pageRecords = getEmployeesPage(page, pageSize);
            pageRecords.forEach(System.out::println);
            System.out.println();
        }
    }

    /**
     * Retrieves a paginated subset of employees.
     *
     * @param pageNumber 1-based page index (page 1, 2, 3...)
     * @param pageSize   number of records per page
     * @return List of employees for the requested page
     */
    public static List<Employee> getEmployeesPage(int pageNumber, int pageSize) {
        if (pageNumber < 1 || pageSize < 1) {
            throw new IllegalArgumentException("Page number and page size must be strictly positive (> 0)");
        }

        int offset = (pageNumber - 1) * pageSize;
        List<Employee> employeeList = new ArrayList<>();
        // Deterministic sorting with ORDER BY id is mandatory for stable pagination
        String query = "SELECT id, name, email, age, department_id FROM employees ORDER BY id LIMIT ? OFFSET ?";

        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(query)) {

            ps.setInt(1, pageSize);
            ps.setInt(2, offset);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    employeeList.add(new Employee(
                            rs.getInt("id"),
                            rs.getString("name"),
                            rs.getString("email"),
                            rs.getInt("age"),
                            (Integer) rs.getObject("department_id")
                    ));
                }
            }
        } catch (SQLException e) {
            System.err.println("[ERROR] Pagination query failed: " + e.getMessage());
        }
        return employeeList;
    }

    /**
     * Computes the total count of employee records in the database.
     *
     * @return total count
     */
    public static int getTotalEmployeeCount() {
        String countSql = "SELECT COUNT(*) FROM employees";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(countSql);
             ResultSet rs = ps.executeQuery()) {

            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            System.err.println("[ERROR] Count query failed: " + e.getMessage());
        }
        return 0;
    }
}
