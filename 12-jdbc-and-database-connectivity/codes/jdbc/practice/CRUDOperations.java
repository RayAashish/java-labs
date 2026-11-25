package jdbc.practice;

import jdbc.config.DatabaseConnection;
import jdbc.models.Student;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Standard Create, Read, Update, Delete (CRUD) operations in JDBC.
 * <p>
 * Preserved and refactored from 'jdbc-with-IO/CRUDOperationsInJDBC.java'.
 * Key Improvements:
 * - Deterministic resource closure via try-with-resources on Connection, PreparedStatement, and ResultSet.
 * - Auto-generated key retrieval on INSERT operations.
 * - Returns domain objects ({@link Student}) rather than printing raw console output.
 * - Defensive parameter handling and informative exception propagation.
 */
public class CRUDOperations {

    /**
     * CREATE: Inserts a new student and returns the auto-generated primary key ID.
     *
     * @param student student domain object (name, age)
     * @return generated ID, or -1 if insertion failed
     */
    public static int insertStudent(Student student) {
        String sql = "INSERT INTO students (name, age) VALUES (?, ?)";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, student.getName());
            ps.setInt(2, student.getAge());

            int rowsAffected = ps.executeUpdate();
            if (rowsAffected > 0) {
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next()) {
                        int generatedId = keys.getInt(1);
                        student.setId(generatedId);
                        return generatedId;
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("[ERROR] Failed to insert student: " + e.getMessage());
        }
        return -1;
    }

    /**
     * READ: Retrieves a student by their primary key ID.
     *
     * @param id student ID
     * @return Optional containing the Student if found
     */
    public static Optional<Student> getStudentById(int id) {
        String sql = "SELECT id, name, age FROM students WHERE id = ?";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Student student = new Student(
                            rs.getInt("id"),
                            rs.getString("name"),
                            rs.getInt("age")
                    );
                    return Optional.of(student);
                }
            }
        } catch (SQLException e) {
            System.err.println("[ERROR] Failed to query student by ID: " + e.getMessage());
        }
        return Optional.empty();
    }

    /**
     * READ ALL: Retrieves all students from the database.
     *
     * @return List of all students
     */
    public static List<Student> getAllStudents() {
        List<Student> students = new ArrayList<>();
        String sql = "SELECT id, name, age FROM students ORDER BY id";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                students.add(new Student(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getInt("age")
                ));
            }
        } catch (SQLException e) {
            System.err.println("[ERROR] Failed to fetch all students: " + e.getMessage());
        }
        return students;
    }

    /**
     * UPDATE: Updates the name and age of a student with the given ID.
     *
     * @param id      target student ID
     * @param newName updated name
     * @param newAge  updated age
     * @return number of rows affected (1 if successful, 0 if ID was not found)
     */
    public static int updateStudent(int id, String newName, int newAge) {
        String sql = "UPDATE students SET name = ?, age = ? WHERE id = ?";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, newName);
            ps.setInt(2, newAge);
            ps.setInt(3, id);

            return ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[ERROR] Failed to update student: " + e.getMessage());
        }
        return 0;
    }

    /**
     * DELETE: Deletes a student by primary key ID.
     *
     * @param id student ID
     * @return number of rows affected
     */
    public static int deleteStudent(int id) {
        String sql = "DELETE FROM students WHERE id = ?";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, id);
            return ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[ERROR] Failed to delete student: " + e.getMessage());
        }
        return 0;
    }

    public static void main(String[] args) {
        System.out.println("=== Testing CRUD Operations Lifecycle ===");

        // 1. Create
        Student newStudent = new Student("Nikkey", 21);
        int generatedId = insertStudent(newStudent);
        System.out.printf("[CREATE] Inserted student '%s' with Generated ID: %d%n", newStudent.getName(), generatedId);

        // 2. Read Single
        Optional<Student> fetched = getStudentById(generatedId);
        System.out.println("[READ] Fetched: " + fetched.orElse(null));

        // 3. Update
        int rowsUpdated = updateStudent(generatedId, "Nikkey Sharma", 22);
        System.out.printf("[UPDATE] Rows affected: %d%n", rowsUpdated);
        System.out.println("[READ] After Update: " + getStudentById(generatedId).orElse(null));

        // 4. Read All
        System.out.println("[READ ALL] Current Students:");
        getAllStudents().forEach(System.out::println);

        // 5. Delete
        int rowsDeleted = deleteStudent(generatedId);
        System.out.printf("[DELETE] Rows affected: %d%n", rowsDeleted);
        System.out.println("[VERIFY] Exists after delete? " + getStudentById(generatedId).isPresent());
    }
}
