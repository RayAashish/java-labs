package exceptions.phase3_autocloseable_and_suppressed;

/**
 * Author: raysi
 * Package: com.raysi.exceptions.phase3_autocloseable_and_suppressed.codes
 *
 * Demonstrates two critical interview concepts:
 * 1. How try-with-resources preserves both the primary exception AND the close() exception via getSuppressed().
 * 2. How a return statement inside 'finally' dangerously swallows exceptions.
 */
public class SuppressedExceptionsRecoveryDemo {

    // Custom resource implementing AutoCloseable
    static class BrokenDatabaseConnection implements AutoCloseable {
        public void executeQuery() {
            throw new RuntimeException("PRIMARY FAILURE: SQL Syntax Error in Query!");
        }

        @Override
        public void close() {
            throw new IllegalStateException("SECONDARY FAILURE: Network dropped while closing connection!");
        }
    }

    public static void main(String[] args) {
        System.out.println("=== TEST 1: Try-With-Resources (Preserves Both Exceptions) ===");
        try (BrokenDatabaseConnection conn = new BrokenDatabaseConnection()) {
            conn.executeQuery();
        } catch (Exception e) {
            System.out.println("Caught Main Exception: " + e.getMessage());

            // Recover the exception thrown inside close()!
            for (Throwable suppressed : e.getSuppressed()) {
                System.out.println("Recovered Suppressed Exception: " + suppressed.getMessage());
            }
        }

        System.out.println("\n=== TEST 2: The 'finally return' Trap (Swallows Exception!) ===");
        int result = dangerousFinallyReturnMethod();
        System.out.println("Returned value: " + result + " (Notice the ArithmeticException vanished completely!)");
    }

    private static int dangerousFinallyReturnMethod() {
        try {
            System.out.println("Inside try: About to divide by zero...");
            return 10 / 0; // Throws ArithmeticException!
        } finally {
            // ANTI-PATTERN: Returning from finally overwrites the active exception reference in JVM!
            return 999;
        }
    }
}
