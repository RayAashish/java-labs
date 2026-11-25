package jdbc.problems;

import jdbc.config.DatabaseConnection;
import jdbc.models.Employee;

import java.sql.BatchUpdateException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

/**
 * Problem 08: Batch Processing (HIGH PRIORITY)
 * <p>
 * <b>Goal:</b>
 * Execute high-throughput bulk inserts/updates using {@link PreparedStatement#addBatch()}
 * and {@link PreparedStatement#executeBatch()}.
 * <p>
 * <b>CRITICAL SENIOR PERSPECTIVES:</b>
 * <ul>
 *   <li><b>The Round-Trip Latency Catastrophe:</b> Executing 1,000 individual {@code executeUpdate()} calls
 *       involves 1,000 network round trips (TCP handshakes, serialization, socket packet send, DB wait, ACK).
 *       On a cloud network with 2ms latency, that is a guaranteed 2 full seconds wasted purely waiting on ping time!</li>
 *   <li><b>Batching Mechanics:</b> {@code ps.addBatch()} buffers query parameters locally in JVM memory.
 *       {@code ps.executeBatch()} flushes the buffered commands across the network in a single payload.</li>
 *   <li><b>The MySQL Driver Secret: {@code rewriteBatchedStatements=true}</b>
 *       By default, MySQL's JDBC driver executes batch items sequentially one by one over the wire.
 *       When {@code rewriteBatchedStatements=true} is appended to the JDBC URL, MySQL Connector/J transforms:
 *       {@code INSERT INTO emp (name) VALUES (?); INSERT INTO emp (name) VALUES (?);}
 *       into a single multi-value SQL statement:
 *       {@code INSERT INTO emp (name) VALUES (?), (?);}.
 *       This yields an astonishing <b>10x to 50x throughput improvement</b>!</li>
 *   <li><b>Memory Management & Batch Chunking:</b> Buffering 500,000 rows in {@code addBatch()} exhausts the
 *       JVM heap with {@code OutOfMemoryError}. Senior engineers chunk batches into windowed bursts
 *       (e.g., chunks of 500 to 2,000 rows), executing {@code executeBatch()} and {@code con.commit()} per chunk.</li>
 *   <li><b>BatchUpdateException:</b> When a row violates a constraint, a {@link BatchUpdateException} is thrown.
 *       Its {@code getUpdateCounts()} array reveals which specific statements succeeded, failed, or were not executed.</li>
 * </ul>
 */
public class Problem08_BatchProcessing {

    public static void main(String[] args) {
        System.out.println("==================================================");
        System.out.println("   PROBLEM 08: HIGH-PERFORMANCE BATCH PROCESSING  ");
        System.out.println("==================================================");

        // 1. Programmatic Benchmark: Insert 1,000 employees using Chunked Batch Processing
        int totalRowsToInsert = 1000;
        int chunkSize = 250;
        System.out.printf("Executing Chunked Batch Insert of %d records (Chunk size: %d)...%n",
                totalRowsToInsert, chunkSize);

        long start = System.currentTimeMillis();
        int successfulInserts = insertEmployeesInBatches(generateSampleEmployees(totalRowsToInsert), chunkSize);
        long elapsed = System.currentTimeMillis() - start;

        System.out.printf("[BENCHMARK COMPLETED] Inserted %d records in %d ms (Throughput: %.1f rows/sec)%n",
                successfulInserts, elapsed, (successfulInserts / (elapsed / 1000.0)));

        // 2. Interactive Console Input Demonstration (Preserved from original user implementation)
        System.out.println("\n(To run interactive batch input via Scanner, invoke interactiveBatchInput())");
    }

    /**
     * Inserts a large collection of employees in memory-efficient, transactional chunks.
     *
     * @param employees collection of employees to insert
     * @param batchSize number of statements to buffer before flushing
     * @return total rows successfully inserted
     */
    public static int insertEmployeesInBatches(List<Employee> employees, int batchSize) {
        String sql = "INSERT INTO employees (name, email, age, department_id) VALUES (?, ?, ?, ?)";
        int totalInserted = 0;
        Connection con = null;

        try {
            con = DatabaseConnection.getConnection();
            // Senior Requirement: Disable auto-commit to eliminate per-statement filesystem sync overhead
            con.setAutoCommit(false);

            try (PreparedStatement ps = con.prepareStatement(sql)) {
                int count = 0;

                for (Employee emp : employees) {
                    ps.setString(1, emp.getName());
                    ps.setString(2, emp.getEmail());
                    ps.setInt(3, emp.getAge());
                    if (emp.getDepartmentId() != null) {
                        ps.setInt(4, emp.getDepartmentId());
                    } else {
                        ps.setNull(4, java.sql.Types.INTEGER);
                    }

                    // Buffer parameters in local JDBC driver memory
                    ps.addBatch();
                    count++;

                    // Once chunk boundary is reached, flush over network to DB
                    if (count % batchSize == 0) {
                        int[] results = ps.executeBatch();
                        totalInserted += countSuccess(results);
                        con.commit(); // Commit the current chunk
                        ps.clearBatch(); // Clear driver buffer to release JVM memory
                    }
                }

                // Flush any remaining statements in the final incomplete chunk
                if (count % batchSize != 0) {
                    int[] remainingResults = ps.executeBatch();
                    totalInserted += countSuccess(remainingResults);
                    con.commit();
                }
            }
        } catch (BatchUpdateException bue) {
            System.err.println("[BATCH ERROR] BatchUpdateException occurred: " + bue.getMessage());
            int[] updateCounts = bue.getUpdateCounts();
            System.err.println("Statements completed prior to failure: " + countSuccess(updateCounts));
            if (con != null) {
                try { con.rollback(); } catch (SQLException ex) { ex.printStackTrace(); }
            }
        } catch (SQLException e) {
            System.err.println("[SQL ERROR] Failed batch execution: " + e.getMessage());
            if (con != null) {
                try { con.rollback(); } catch (SQLException ex) { ex.printStackTrace(); }
            }
        } finally {
            if (con != null) {
                try {
                    con.setAutoCommit(true);
                    con.close();
                } catch (SQLException ignored) {}
            }
        }

        return totalInserted;
    }

    /**
     * Interactive scanner-based batching (Enhanced from original user implementation).
     */
    public static void interactiveBatchInput() {
        Scanner sc = new Scanner(System.in);
        String sql = "INSERT INTO employees (name, email, age) VALUES (?, ?, ?)";

        try (Connection con = DatabaseConnection.getConnection()) {
            con.setAutoCommit(false);

            try (PreparedStatement ps = con.prepareStatement(sql)) {
                while (true) {
                    System.out.print("Enter name : ");
                    String name = sc.next();
                    System.out.print("Enter email : ");
                    String email = sc.next();
                    System.out.print("Enter age : ");
                    int age = sc.nextInt();

                    ps.setString(1, name);
                    ps.setString(2, email);
                    ps.setInt(3, age);
                    ps.addBatch();

                    System.out.print("Do you want to continue adding employees? (y/n): ");
                    String choice = sc.next();
                    if (choice.equalsIgnoreCase("N")) break;
                }

                int[] results = ps.executeBatch();
                con.commit();
                System.out.println("Batch execution complete! Total rows inserted: " + countSuccess(results));
            }
        } catch (SQLException e) {
            System.err.println("[ERROR] Interactive batch failed: " + e.getMessage());
        } finally {
            sc.close();
        }
    }

    private static int countSuccess(int[] results) {
        if (results == null) return 0;
        int sum = 0;
        for (int res : results) {
            if (res >= 0) {
                sum += res;
            } else if (res == Statement.SUCCESS_NO_INFO) {
                // MySQL driver with rewriteBatchedStatements returns SUCCESS_NO_INFO (-2)
                sum += 1;
            }
        }
        return sum;
    }

    private static List<Employee> generateSampleEmployees(int count) {
        List<Employee> list = new ArrayList<>(count);
        long timestamp = System.currentTimeMillis();
        for (int i = 1; i <= count; i++) {
            list.add(new Employee(
                    "BatchEmp_" + i,
                    "batch_" + timestamp + "_" + i + "@jdbclab.io",
                    20 + (i % 40),
                    1 + (i % 3)
            ));
        }
        return list;
    }
}
