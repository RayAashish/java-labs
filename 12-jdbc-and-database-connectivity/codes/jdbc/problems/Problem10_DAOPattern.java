package jdbc.problems;

import jdbc.config.DatabaseConnection;
import jdbc.models.Employee;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Problem 10: Data Access Object (DAO) Layer Design
 * <p>
 * <b>Goal:</b>
 * Build clean architecture and enterprise separation of concerns by isolating
 * raw SQL and JDBC plumbing behind a contract interface ({@link EmployeeDAO}).
 * <p>
 * <b>Architecture Overview:</b>
 * <pre>
 *   +---------------------------------------------+
 *   | Business Logic / Application Service / Main |
 *   +---------------------------------------------+
 *                         |
 *                         v (Calls contract)
 *   +---------------------------------------------+
 *   |           <<interface>> EmployeeDAO         |
 *   +---------------------------------------------+
 *                         |
 *                         v (Implements contract)
 *   +---------------------------------------------+
 *   |               EmployeeDAOImpl               |
 *   +---------------------------------------------+
 *                         |
 *                         v (Acquires connections)
 *   +---------------------------------------------+
 *   |       DatabaseConnection / DataSource       |
 *   +---------------------------------------------+
 * </pre>
 * <p>
 * <b>Senior Architecture Principles:</b>
 * 1. <b>Decoupling:</b> Service layers know nothing about SQL syntax, JDBC Drivers, or ResultSets.
 * 2. <b>Testability:</b> Service layers can be unit-tested by mocking {@code EmployeeDAO} without a physical database.
 * 3. <b>Centralized Mapping:</b> Mapping logic from {@link ResultSet} to {@link Employee} is located in one reusable method ({@code mapRow}).
 */
public class Problem10_DAOPattern {

    // =========================================================================
    // 1. DAO Interface (Contract)
    // =========================================================================
    public interface EmployeeDAO {
        int save(Employee employee);
        Optional<Employee> findById(int id);
        List<Employee> findAll();
        List<Employee> findByMinAge(int minAge);
        boolean update(Employee employee);
        boolean deleteById(int id);
    }

    // =========================================================================
    // 2. DAO Concrete Implementation
    // =========================================================================
    public static class EmployeeDAOImpl implements EmployeeDAO {

        @Override
        public int save(Employee employee) {
            String sql = "INSERT INTO employees (name, email, age, department_id) VALUES (?, ?, ?, ?)";
            try (Connection con = DatabaseConnection.getConnection();
                 PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

                ps.setString(1, employee.getName());
                ps.setString(2, employee.getEmail());
                ps.setInt(3, employee.getAge());
                if (employee.getDepartmentId() != null) {
                    ps.setInt(4, employee.getDepartmentId());
                } else {
                    ps.setNull(4, java.sql.Types.INTEGER);
                }

                int affected = ps.executeUpdate();
                if (affected > 0) {
                    try (ResultSet keys = ps.getGeneratedKeys()) {
                        if (keys.next()) {
                            int generatedId = keys.getInt(1);
                            employee.setId(generatedId);
                            return generatedId;
                        }
                    }
                }
            } catch (SQLException e) {
                System.err.println("[DAO ERROR] save failed: " + e.getMessage());
            }
            return -1;
        }

        @Override
        public Optional<Employee> findById(int id) {
            String sql = "SELECT id, name, email, age, department_id FROM employees WHERE id = ?";
            try (Connection con = DatabaseConnection.getConnection();
                 PreparedStatement ps = con.prepareStatement(sql)) {

                ps.setInt(1, id);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return Optional.of(mapRow(rs));
                    }
                }
            } catch (SQLException e) {
                System.err.println("[DAO ERROR] findById failed: " + e.getMessage());
            }
            return Optional.empty();
        }

        @Override
        public List<Employee> findAll() {
            List<Employee> list = new ArrayList<>();
            String sql = "SELECT id, name, email, age, department_id FROM employees ORDER BY id";
            try (Connection con = DatabaseConnection.getConnection();
                 PreparedStatement ps = con.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {

                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            } catch (SQLException e) {
                System.err.println("[DAO ERROR] findAll failed: " + e.getMessage());
            }
            return list;
        }

        @Override
        public List<Employee> findByMinAge(int minAge) {
            List<Employee> list = new ArrayList<>();
            String sql = "SELECT id, name, email, age, department_id FROM employees WHERE age >= ? ORDER BY age";
            try (Connection con = DatabaseConnection.getConnection();
                 PreparedStatement ps = con.prepareStatement(sql)) {

                ps.setInt(1, minAge);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        list.add(mapRow(rs));
                    }
                }
            } catch (SQLException e) {
                System.err.println("[DAO ERROR] findByMinAge failed: " + e.getMessage());
            }
            return list;
        }

        @Override
        public boolean update(Employee employee) {
            String sql = "UPDATE employees SET name = ?, email = ?, age = ?, department_id = ? WHERE id = ?";
            try (Connection con = DatabaseConnection.getConnection();
                 PreparedStatement ps = con.prepareStatement(sql)) {

                ps.setString(1, employee.getName());
                ps.setString(2, employee.getEmail());
                ps.setInt(3, employee.getAge());
                if (employee.getDepartmentId() != null) {
                    ps.setInt(4, employee.getDepartmentId());
                } else {
                    ps.setNull(4, java.sql.Types.INTEGER);
                }
                ps.setInt(5, employee.getId());

                return ps.executeUpdate() > 0;
            } catch (SQLException e) {
                System.err.println("[DAO ERROR] update failed: " + e.getMessage());
                return false;
            }
        }

        @Override
        public boolean deleteById(int id) {
            String sql = "DELETE FROM employees WHERE id = ?";
            try (Connection con = DatabaseConnection.getConnection();
                 PreparedStatement ps = con.prepareStatement(sql)) {

                ps.setInt(1, id);
                return ps.executeUpdate() > 0;
            } catch (SQLException e) {
                System.err.println("[DAO ERROR] deleteById failed: " + e.getMessage());
                return false;
            }
        }

        /**
         * Reusable RowMapper: Centralizes column mapping to maintain DRY principle.
         */
        private Employee mapRow(ResultSet rs) throws SQLException {
            return new Employee(
                    rs.getInt("id"),
                    rs.getString("name"),
                    rs.getString("email"),
                    rs.getInt("age"),
                    (Integer) rs.getObject("department_id")
            );
        }
    }

    // =========================================================================
    // 3. Main Execution Runner
    // =========================================================================
    public static void main(String[] args) {
        System.out.println("=== Problem 10: DAO Layer Architecture Demonstration ===");

        // Program against the Interface, not the implementation
        EmployeeDAO employeeDAO = new EmployeeDAOImpl();

        // 1. Create via DAO
        Employee newHire = new Employee("Kavita Rao", "kavita." + System.currentTimeMillis() + "@dao.com", 29, 1);
        int generatedId = employeeDAO.save(newHire);
        System.out.printf("[DAO CREATE] Successfully saved new employee with ID: %d%n", generatedId);

        // 2. Query Single via DAO
        Optional<Employee> retrieved = employeeDAO.findById(generatedId);
        System.out.println("[DAO FIND BY ID] " + retrieved.orElse(null));

        // 3. Update via DAO
        if (retrieved.isPresent()) {
            Employee toUpdate = retrieved.get();
            toUpdate.setAge(30);
            toUpdate.setName("Kavita R. Sharma");
            boolean updated = employeeDAO.update(toUpdate);
            System.out.println("[DAO UPDATE] Updated successfully? " + updated);
            System.out.println("[DAO VERIFY UPDATE] " + employeeDAO.findById(generatedId).orElse(null));
        }

        // 4. Query All via DAO
        System.out.println("\n[DAO FIND ALL] Listing first 5 employees:");
        employeeDAO.findAll().stream().limit(5).forEach(System.out::println);

        // 5. Delete via DAO
        boolean deleted = employeeDAO.deleteById(generatedId);
        System.out.printf("[DAO DELETE] Deleted ID %d? %s%n", generatedId, deleted);
        System.out.println("[DAO VERIFY DELETE] Found after delete? " + employeeDAO.findById(generatedId).isPresent());
    }
}
