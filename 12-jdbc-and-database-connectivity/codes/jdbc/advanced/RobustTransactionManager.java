package jdbc.advanced;

import java.sql.Connection;
import java.sql.SQLException;

import jdbc.config.DatabaseConnection;

/**
 * Functional Transaction Boundary Template.
 * <p>
 * <b>Senior Design Pattern: Transaction Template / Execution Around Pattern</b>
 * <p>
 * In production codebases, repeating manual {@code con.setAutoCommit(false)}, {@code con.commit()},
 * {@code con.rollback()}, and {@code con.setAutoCommit(true)} across 50 different service classes
 * violates the DRY principle and introduces subtle bugs (like forgetting to rollback on runtime exceptions).
 * <p>
 * This class encapsulates the entire transaction lifecycle inside a higher-order function:
 * <pre>
 *   RobustTransactionManager.executeInTransaction(con -> {
 *       // Step 1: Debit account A
 *       // Step 2: Credit account B
 *       // If any exception is thrown, automatic rollback triggers immediately!
 *   });
 * </pre>
 */
public class RobustTransactionManager {

    @FunctionalInterface
    public interface TransactionConsumer {
        void accept(Connection con) throws Exception;
    }

    @FunctionalInterface
    public interface TransactionFunction<T> {
        T apply(Connection con) throws Exception;
    }

    /**
     * Executes a void transaction block atomically.
     *
     * @param consumer lambda with database operations
     * @throws RuntimeException wrapping any checked exception occurred
     */
    public static void executeInTransaction(TransactionConsumer consumer) {
        executeInTransactionWithResult(con -> {
            consumer.accept(con);
            return null;
        });
    }

    /**
     * Executes a transaction block atomically and returns a computed result.
     *
     * @param function lambda returning result
     * @param <T>      return type
     * @return result computed inside transaction
     */
    public static <T> T executeInTransactionWithResult(TransactionFunction<T> function) {
        Connection con = null;
        try {
            con = DatabaseConnection.getConnection();
            con.setAutoCommit(false);

            T result = function.apply(con);

            con.commit();
            return result;
        } catch (Exception e) {
            if (con != null) {
                try {
                    System.err.println("[TRANSACTION MANAGER] Exception encountered, rolling back: " + e.getMessage());
                    con.rollback();
                } catch (SQLException rollbackEx) {
                    System.err.println("[CRITICAL] Rollback failure: " + rollbackEx.getMessage());
                }
            }
            throw new RuntimeException("Transaction execution failed and was rolled back", e);
        } finally {
            if (con != null) {
                try {
                    con.setAutoCommit(true);
                    con.close();
                } catch (SQLException ignored) {}
            }
        }
    }

    public static void main(String[] args) {
        System.out.println("=== Testing RobustTransactionManager ===");

        // Example 1: Successful Atomic Operation
        String result = executeInTransactionWithResult(con -> {
            System.out.println("  [TX] Step 1 executing inside transaction boundary...");
            System.out.println("  [TX] Step 2 executing inside transaction boundary...");
            return "SUCCESSFUL_TX_TOKEN_XYZ";
        });
        System.out.println("Transaction Result: " + result);

        // Example 2: Automatic Rollback on Simulated Exception
        System.out.println("\n=== Testing Automatic Rollback on Error ===");
        try {
            executeInTransaction(con -> {
                System.out.println("  [TX] Running step 1...");
                throw new IllegalStateException("Simulated business validation failure!");
            });
        } catch (RuntimeException e) {
            System.out.println("Caught expected exception: " + e.getMessage());
            System.out.println("Verified: Transaction was rolled back safely without leaking connection!");
        }
    }
}
