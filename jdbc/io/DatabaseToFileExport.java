package jdbc.io;

import jdbc.config.DatabaseConnection;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;

/**
 * Memory-conscious Database-to-File Exporter.
 * <p>
 * <b>Senior Architecture Lesson: Large Dataset Exporting</b>
 * When exporting 5,000,000 rows to a CSV file:
 * - <i>The Junior Trap:</i> Executing {@code List<Employee> list = dao.findAll()} and iterating the list.
 *   The entire 5,000,000 records must be materialized into memory, immediately throwing an {@link OutOfMemoryError}.
 * - <i>The Senior Solution:</i> Streaming cursor export. Stream rows straight from the {@link ResultSet}
 *   directly into a buffered output stream / {@link BufferedWriter}, flushing periodically.
 *   Heap memory consumption stays strictly bounded to a tiny few kilobytes!
 */
public class DatabaseToFileExport {

    public static void main(String[] args) {
        System.out.println("=== Exporting Database Records to CSV ===");

        String outputCsv = "data/exported_employees.csv";
        String query = "SELECT id, name, email, age, department_id FROM employees ORDER BY id";

        int exported = exportQueryToCsv(query, outputCsv);
        System.out.printf("Successfully exported %d records to '%s'%n", exported, outputCsv);
    }

    /**
     * Executes any SELECT query and streams rows directly into a CSV file.
     * Uses ResultSetMetaData to write CSV header dynamically.
     *
     * @param sqlQuery    SELECT query
     * @param destination target file path
     * @return count of exported rows
     */
    public static int exportQueryToCsv(String sqlQuery, String destination) {
        Path targetPath = FileReaderUtility.resolvePath(destination);
        int rowCount = 0;

        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sqlQuery);
             ResultSet rs = ps.executeQuery();
             BufferedWriter writer = Files.newBufferedWriter(targetPath, StandardCharsets.UTF_8)) {

            ResultSetMetaData meta = rs.getMetaData();
            int colCount = meta.getColumnCount();

            // 1. Write Header
            for (int i = 1; i <= colCount; i++) {
                writer.write(escapeCsv(meta.getColumnLabel(i)));
                if (i < colCount) writer.write(",");
            }
            writer.newLine();

            // 2. Stream Rows
            while (rs.next()) {
                rowCount++;
                for (int i = 1; i <= colCount; i++) {
                    Object val = rs.getObject(i);
                    writer.write(escapeCsv(val == null ? "" : val.toString()));
                    if (i < colCount) writer.write(",");
                }
                writer.newLine();

                // Periodic flush to prevent unbounded OS buffer growth
                if (rowCount % 1000 == 0) {
                    writer.flush();
                }
            }
            writer.flush();

        } catch (SQLException | IOException e) {
            System.err.println("[ERROR] CSV Export failed: " + e.getMessage());
        }

        return rowCount;
    }

    private static String escapeCsv(String val) {
        if (val == null) return "";
        if (val.contains(",") || val.contains("\"") || val.contains("\n")) {
            return "\"" + val.replace("\"", "\"\"") + "\"";
        }
        return val;
    }
}
