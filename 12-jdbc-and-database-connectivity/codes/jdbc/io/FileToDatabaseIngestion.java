package jdbc.io;

import jdbc.config.DatabaseConnection;
import jdbc.models.Laptop;
import jdbc.models.Student;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * High-Performance File-to-Database Ingestion Engine.
 * <p>
 * Combines, restructures, and modernizes the ingestion logic from:
 * - 'jdbc-with-IO/CRUDOperationsInJDBC.java' (students.txt)
 * - 'jdbc-with-IO/JDBCRevision.java' (laptop.txt)
 * <p>
 * <b>Senior Architecture Design:</b>
 * 1. <b>Decoupled Parsing & Ingestion:</b> Line tokenization is isolated from JDBC persistence.
 * 2. <b>Batch + Transaction Ingestion:</b> Instead of committing row-by-row inside the file reading loop,
 *    records are batched ({@code addBatch()}) and committed in an atomic transaction boundary.
 * 3. <b>Defensive Parsing:</b> Malformed lines or blank rows are logged and skipped rather than
 *    halting the entire ingestion pipeline with an unhandled exception.
 */
public class FileToDatabaseIngestion {

    public static void main(String[] args) {
        System.out.println("==================================================");
        System.out.println("   FILE-TO-DATABASE INGESTION ENGINE              ");
        System.out.println("==================================================");

        // 1. Ingest students.txt
        int studentsCount = ingestStudentsFile("data/students.txt");
        System.out.printf("[INGESTION] Total Students Ingested: %d%n", studentsCount);

        // 2. Ingest laptop.txt
        int laptopsCount = ingestLaptopsFile("data/laptop.txt");
        System.out.printf("[INGESTION] Total Laptops Ingested: %d%n", laptopsCount);

        // 3. Ingest employees.csv
        int empCount = ingestEmployeesCsv("data/employees.csv");
        System.out.printf("[INGESTION] Total CSV Employees Ingested: %d%n", empCount);
    }

    /**
     * Reads and ingests student records from space-separated text file:
     * Format: <Name> <Age>
     */
    public static int ingestStudentsFile(String filePath) {
        Path path = FileReaderUtility.resolvePath(filePath);
        String sql = "INSERT INTO students (name, age) VALUES (?, ?)";
        int inserted = 0;

        try (Connection con = DatabaseConnection.getConnection();
             BufferedReader reader = Files.newBufferedReader(path);
             PreparedStatement ps = con.prepareStatement(sql)) {

            con.setAutoCommit(false);
            String line;
            int lineNumber = 0;

            while ((line = reader.readLine()) != null) {
                lineNumber++;
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#")) continue;

                String[] tokens = trimmed.split("\\s+");
                if (tokens.length < 2) {
                    System.err.printf("[WARN] Skipping malformed line %d in %s: '%s'%n", lineNumber, filePath, line);
                    continue;
                }

                try {
                    String name = tokens[0];
                    int age = Integer.parseInt(tokens[1]);

                    ps.setString(1, name);
                    ps.setInt(2, age);
                    ps.addBatch();
                    inserted++;
                } catch (NumberFormatException e) {
                    System.err.printf("[WARN] Invalid age at line %d: '%s'%n", lineNumber, tokens[1]);
                }
            }

            ps.executeBatch();
            con.commit();
            con.setAutoCommit(true);

        } catch (IOException | SQLException e) {
            System.err.println("[ERROR] Student file ingestion failed: " + e.getMessage());
        }

        return inserted;
    }

    /**
     * Reads and ingests laptop hardware records from space-separated text file:
     * Format: <ModelName> <RAM_GB> <SSD_GB>
     */
    public static int ingestLaptopsFile(String filePath) {
        Path path = FileReaderUtility.resolvePath(filePath);
        String sql = "INSERT INTO laptop (name, ram, ssd) VALUES (?, ?, ?)";
        int inserted = 0;

        try (Connection con = DatabaseConnection.getConnection();
             BufferedReader reader = Files.newBufferedReader(path);
             PreparedStatement ps = con.prepareStatement(sql)) {

            con.setAutoCommit(false);
            String line;
            int lineNumber = 0;

            while ((line = reader.readLine()) != null) {
                lineNumber++;
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#")) continue;

                String[] tokens = trimmed.split("\\s+");
                if (tokens.length < 3) {
                    System.err.printf("[WARN] Skipping malformed laptop record at line %d: '%s'%n", lineNumber, line);
                    continue;
                }

                try {
                    String name = tokens[0];
                    int ram = Integer.parseInt(tokens[1]);
                    int ssd = Integer.parseInt(tokens[2]);

                    ps.setString(1, name);
                    ps.setInt(2, ram);
                    ps.setInt(3, ssd);
                    ps.addBatch();
                    inserted++;
                } catch (NumberFormatException e) {
                    System.err.printf("[WARN] Number parsing error at line %d: '%s'%n", lineNumber, line);
                }
            }

            ps.executeBatch();
            con.commit();
            con.setAutoCommit(true);

        } catch (IOException | SQLException e) {
            System.err.println("[ERROR] Laptop file ingestion failed: " + e.getMessage());
        }

        return inserted;
    }

    /**
     * Reads and ingests employee records from a comma-separated (CSV) file.
     * Skips CSV header and uses UPSERT / ON DUPLICATE KEY UPDATE to ensure idempotency.
     */
    public static int ingestEmployeesCsv(String filePath) {
        Path path = FileReaderUtility.resolvePath(filePath);
        String sql = "INSERT INTO employees (id, name, email, age, department_id) VALUES (?, ?, ?, ?, ?) " +
                     "ON DUPLICATE KEY UPDATE name=VALUES(name), age=VALUES(age), department_id=VALUES(department_id)";
        int inserted = 0;

        try (Connection con = DatabaseConnection.getConnection();
             BufferedReader reader = Files.newBufferedReader(path);
             PreparedStatement ps = con.prepareStatement(sql)) {

            con.setAutoCommit(false);
            String header = reader.readLine(); // Skip CSV header
            String line;
            int lineNumber = 1;

            while ((line = reader.readLine()) != null) {
                lineNumber++;
                String trimmed = line.trim();
                if (trimmed.isEmpty()) continue;

                String[] tokens = trimmed.split(",");
                if (tokens.length < 5) continue;

                try {
                    int id = Integer.parseInt(tokens[0].trim());
                    String name = tokens[1].trim();
                    String email = tokens[2].trim();
                    int age = Integer.parseInt(tokens[3].trim());
                    Integer deptId = tokens[4].trim().isEmpty() ? null : Integer.parseInt(tokens[4].trim());

                    ps.setInt(1, id);
                    ps.setString(2, name);
                    ps.setString(3, email);
                    ps.setInt(4, age);
                    if (deptId != null) {
                        ps.setInt(5, deptId);
                    } else {
                        ps.setNull(5, java.sql.Types.INTEGER);
                    }

                    ps.addBatch();
                    inserted++;
                } catch (Exception e) {
                    System.err.printf("[WARN] Error parsing CSV line %d: '%s'%n", lineNumber, line);
                }
            }

            ps.executeBatch();
            con.commit();
            con.setAutoCommit(true);

        } catch (IOException | SQLException e) {
            System.err.println("[ERROR] CSV ingestion failed: " + e.getMessage());
        }

        return inserted;
    }
}
