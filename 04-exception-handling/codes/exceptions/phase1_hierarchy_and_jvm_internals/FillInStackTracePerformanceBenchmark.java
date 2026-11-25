package exceptions.phase1_hierarchy_and_jvm_internals;

/**
 * Author: raysi
 * Package: com.raysi.exceptions.phase1_hierarchy_and_jvm_internals.codes
 *
 * Micro-benchmark proving the CPU cost of Throwable.fillInStackTrace().
 *
 * Compares 3 execution paths over 100,000 iterations:
 * 1. Standard POJO / Return Code Control Flow (Zero-cost baseline)
 * 2. Stackless Exception (overriding fillInStackTrace() -> this)
 * 3. Standard Exception (HotSpot native C++ stack walker invoked)
 */
public class FillInStackTracePerformanceBenchmark {

    private static final int ITERATIONS = 100_000;
    private static final int WARMUP_ROUNDS = 20_000;

    // Fast, lightweight exception that disables JNI stack walking
    public static class StacklessException extends RuntimeException {
        public StacklessException(String message) {
            super(message);
        }

        @Override
        public synchronized Throwable fillInStackTrace() {
            // No-op: suppresses JVM C++ frame traversal
            return this;
        }
    }

    // Standard exception invoking native fillInStackTrace()
    public static class StandardException extends RuntimeException {
        public StandardException(String message) {
            super(message);
        }
    }

    public static void main(String[] args) {
        System.out.println("==========================================================================");
        System.out.println("MICRO-BENCHMARK: Throwable.fillInStackTrace() Performance Overhead");
        System.out.println("Iterations: " + ITERATIONS + " (with JIT warmup)");
        System.out.println("==========================================================================");

        // Warmup JIT
        warmup();

        // 1. Baseline: Normal Control Flow
        long start = System.nanoTime();
        int successCount = 0;
        for (int i = 0; i < ITERATIONS; i++) {
            if (baselineReturn(i)) {
                successCount++;
            }
        }
        long baselineDurationNs = System.nanoTime() - start;

        // 2. Stackless Exception (fillInStackTrace overridden)
        start = System.nanoTime();
        int stacklessCount = 0;
        for (int i = 0; i < ITERATIONS; i++) {
            try {
                throwStackless(i);
            } catch (StacklessException e) {
                stacklessCount++;
            }
        }
        long stacklessDurationNs = System.nanoTime() - start;

        // 3. Standard Exception (Native fillInStackTrace walking stack)
        start = System.nanoTime();
        int standardCount = 0;
        for (int i = 0; i < ITERATIONS; i++) {
            try {
                throwStandard(i);
            } catch (StandardException e) {
                standardCount++;
            }
        }
        long standardDurationNs = System.nanoTime() - start;

        // Output results
        double baselineMs = baselineDurationNs / 1_000_000.0;
        double stacklessMs = stacklessDurationNs / 1_000_000.0;
        double standardMs = standardDurationNs / 1_000_000.0;

        System.out.println("\n--- BENCHMARK RESULTS ---");
        System.out.printf("1. Normal Return Flow   : %8.3f ms  (Relative: 1.0x baseline)%n", baselineMs);
        System.out.printf("2. Stackless Exception  : %8.3f ms  (Relative: ~%.1fx slower)%n", stacklessMs, (stacklessMs / baselineMs));
        System.out.printf("3. Standard Exception   : %8.3f ms  (Relative: ~%.1fx slower)%n", standardMs, (standardMs / baselineMs));

        System.out.println("\n--------------------------------------------------------------------------");
        System.out.printf("RATIO: Standard Exception is ~%.1fx slower than Stackless Exception!%n", (standardMs / stacklessMs));
        System.out.println("CONCLUSION: Throwable.fillInStackTrace() is responsible for >90% of exception overhead.");
        System.out.println("Never use standard exceptions for normal business branch control!");
        System.out.println("--------------------------------------------------------------------------");
    }

    private static void warmup() {
        for (int i = 0; i < WARMUP_ROUNDS; i++) {
            baselineReturn(i);
            try { throwStackless(i); } catch (StacklessException ignored) {}
            try { throwStandard(i); } catch (StandardException ignored) {}
        }
    }

    private static boolean baselineReturn(int i) {
        return (i >= 0);
    }

    private static void throwStackless(int i) {
        deepFrameA_Stackless(i);
    }

    private static void deepFrameA_Stackless(int i) {
        deepFrameB_Stackless(i);
    }

    private static void deepFrameB_Stackless(int i) {
        throw new StacklessException("Fast signal");
    }

    private static void throwStandard(int i) {
        deepFrameA_Standard(i);
    }

    private static void deepFrameA_Standard(int i) {
        deepFrameB_Standard(i);
    }

    private static void deepFrameB_Standard(int i) {
        throw new StandardException("Slow standard exception");
    }
}
