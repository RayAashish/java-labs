package ionio.phase1_classic_io_streams.bytestreams;

/**
 * Author: raysi
 * Package: com.raysi.ionio.phase1_classic_io_streams.bytestreams.codes
 *
 * Demonstrates the internal mechanics of Java 7+ Try-With-Resources (TWR):
 * 1. The AutoCloseable contract.
 * 2. Reverse declaration closure order (LIFO - Last-In, First-Out).
 * 3. Suppressed Exceptions: How TWR preserves the root cause exception and
 *    attaches closing failures via Throwable.addSuppressed(), preventing the
 *    fatal exception-masking bug common in legacy try-catch-finally.
 * 4. Bytecode emulation: Explaining how javac decompiles TWR into null-checks
 *    and synthetic exception handlers.
 */
public class TryWithResourcesAutoCloseDemo {

    public static void main(String[] args) {
        System.out.println("==========================================================================");
        System.out.println(">>> DEMO: Try-With-Resources Lifecycle & Suppressed Exceptions <<<");
        System.out.println(">>> Author: raysi | AutoCloseable Mechanics Deep Dive");
        System.out.println("==========================================================================\n");

        // Experiment 1: Verifying Reverse Closure Order (LIFO)
        System.out.println("--- Experiment 1: Resource Closing Order (LIFO) ---");
        verifyClosingOrder();

        System.out.println("\n--------------------------------------------------------------------------\n");

        // Experiment 2: Primary Exception with Multiple Suppressed Exceptions
        System.out.println("--- Experiment 2: Exception Handling & Suppressed Exceptions ---");
        verifySuppressedExceptions();

        System.out.println("\n--------------------------------------------------------------------------\n");

        // Experiment 3: Contrast with Legacy Pre-Java 7 try-finally (The Masking Anti-Pattern)
        System.out.println("--- Experiment 3: Legacy try-finally Exception Masking Trap ---");
        demonstrateLegacyMaskingBug();

        System.out.println("\n==========================================================================");
        System.out.println(">>> BYTECODE TAKEAWAY <<<");
        System.out.println("In compiled bytecode, javac creates synthetic local variables for each resource");
        System.out.println("and wraps them in nested try-catch blocks. If primary block throws Throwable t1,");
        System.out.println("and resource.close() throws Throwable t2, javac invokes t1.addSuppressed(t2).");
        System.out.println("The primary root cause is NEVER lost!");
        System.out.println("==========================================================================");
    }

    private static void verifyClosingOrder() {
        System.out.println("[Step 1] Initializing Resource A, then Resource B...");
        try (MockResource resA = new MockResource("Resource_A");
             MockResource resB = new MockResource("Resource_B");
             MockResource resC = new MockResource("Resource_C")) {

            System.out.println("[Step 2] Inside try block: Operating on A, B, and C...");
            resA.doWork();
            resB.doWork();
            resC.doWork();
            System.out.println("[Step 3] Exiting try block. Watch the closure order below:");

        } catch (Exception e) {
            System.err.println("Caught unexpected error: " + e.getMessage());
        }
    }

    private static void verifySuppressedExceptions() {
        try (FaultyResource r1 = new FaultyResource("DatabaseConnection_1", true);
             FaultyResource r2 = new FaultyResource("FileStream_2", true)) {

            System.out.println("[Step 1] Executing business logic that triggers primary exception...");
            throw new IllegalStateException("CRITICAL_ERROR: Primary business transaction failed!");

        } catch (Exception primaryEx) {
            System.out.println("[Step 2] Caught Primary Exception: " + primaryEx.getClass().getSimpleName()
                    + " -> " + primaryEx.getMessage());

            // Inspecting suppressed exceptions attached by try-with-resources
            Throwable[] suppressed = primaryEx.getSuppressed();
            System.out.println("[Step 3] Number of suppressed closing exceptions: " + suppressed.length);

            for (int i = 0; i < suppressed.length; i++) {
                System.out.printf("   ├── Suppressed [%d]: %s -> %s%n",
                        i + 1,
                        suppressed[i].getClass().getSimpleName(),
                        suppressed[i].getMessage());
            }
        }
    }

    private static void demonstrateLegacyMaskingBug() {
        // Prior to Java 7, finally blocks routinely masked original exceptions
        try {
            FaultyResource r1 = new FaultyResource("LegacyResource", true);
            try {
                System.out.println("[Step 1] Executing legacy code... throws PRIMARY exception.");
                throw new ArithmeticException("PRIMARY: Division by zero in transaction!");
            } finally {
                System.out.println("[Step 2] Executing finally block... throws CLOSING exception.");
                // In legacy code, closing also threw an exception:
                r1.close(); // Throws IOException
            }
        } catch (Exception e) {
            System.out.println("[Step 3] Notice what was caught in catch block:");
            System.out.println("   -> Caught: " + e.getClass().getSimpleName() + ": " + e.getMessage());
            System.out.println("   [WARNING] The primary ArithmeticException was COMPLETELY SWALLOWED and LOST!");
            System.out.println("   [WARNING] This nightmare bug wasted thousands of debugging hours before Java 7.");
        }
    }

    // Helper mock classes
    static class MockResource implements AutoCloseable {
        private final String name;

        MockResource(String name) {
            this.name = name;
            System.out.println("   [ALLOCATED] " + name);
        }

        void doWork() {
            System.out.println("   [WORKING]   " + name + " performing I/O task.");
        }

        @Override
        public void close() {
            System.out.println("   [CLOSED]    " + name + " released successfully (LIFO sequence).");
        }
    }

    static class FaultyResource implements AutoCloseable {
        private final String name;
        private final boolean failOnClose;

        FaultyResource(String name, boolean failOnClose) {
            this.name = name;
            this.failOnClose = failOnClose;
            System.out.println("   [ALLOCATED] " + name);
        }

        @Override
        public void close() throws Exception {
            if (failOnClose) {
                System.out.println("   [FAILING]   " + name + " failed during close()!");
                throw new java.io.IOException("ERR_CLOSE: Failed to flush & close socket for " + name);
            }
            System.out.println("   [CLOSED]    " + name + " closed cleanly.");
        }
    }
}
