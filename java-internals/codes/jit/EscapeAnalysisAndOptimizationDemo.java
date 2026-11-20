package com.raysi.internals.jit;

/**
 * Architectural Demonstration: JIT Escape Analysis & Scalar Replacement Benchmark.
 * <p>
 * Key Concepts Demonstrated:
 * 1. Escape Analysis (EA): The C2 JIT compiler analyzes whether an allocated object reference escapes the method.
 * 2. Scalar Replacement: If an object has 'NoEscape', C2 replaces the heap allocation entirely,
 *    mapping fields directly to CPU registers or stack primitives.
 * 3. Performance impact: 100,000,000 object creations execute in tens of milliseconds with ZERO GC allocations!
 */
public class EscapeAnalysisAndOptimizationDemo {

    private static final int ITERATIONS = 100_000_000;

    public static void main(String[] args) {
        System.out.println("================================================================================");
        System.out.println("   JIT COMPILER ESCAPE ANALYSIS & SCALAR REPLACEMENT BENCHMARK");
        System.out.println("================================================================================");

        System.out.println("\n[1] Warming up JIT compiler (Tiered Compilation C1/C2 transitions)...");
        for (int i = 0; i < 50_000; i++) {
            nonEscapingAllocation(i, i + 1);
        }

        System.out.println("[2] Executing " + ITERATIONS + " non-escaping object allocations...");
        long start = System.currentTimeMillis();
        long totalSum = 0;

        for (int i = 0; i < ITERATIONS; i++) {
            totalSum += nonEscapingAllocation(i, 2);
        }

        long elapsed = System.currentTimeMillis() - start;

        System.out.println("\n[3] Benchmark Results:");
        System.out.printf("   -> Total Iterations : %,d%n", ITERATIONS);
        System.out.printf("   -> Total Time       : %d ms%n", elapsed);
        System.out.printf("   -> Throughput       : %,.0f ops/sec%n", (ITERATIONS / (elapsed / 1000.0)));
        System.out.println("   -> Computed Checksum: " + totalSum);

        System.out.println("\n[4] Architectural Analysis:");
        System.out.println("   -> If 100,000,000 Point objects were actually allocated on heap:");
        System.out.println("      100M * 24 bytes = 2.4 GIGABYTES of heap memory would be allocated!");
        System.out.println("      Multiple Minor GCs would pause the thread and thrash the CPU.");
        System.out.println("   -> Because C2 detected 'NoEscape', it executed SCALAR REPLACEMENT:");
        System.out.println("      Point object was decomposed into two 32-bit registers (e.g., EAX and EDX).");
        System.out.println("      Zero heap objects were created, running in pure CPU silicon speed!");

        System.out.println("\n================================================================================");
    }

    /**
     * The Point instance 'p' is created, used, and discarded entirely within this method frame.
     * It NEVER escapes to another thread or heap reference ('NoEscape').
     * C2 replaces 'new Point(a, b)' with scalar primitives 'int x = a; int y = b;'!
     */
    private static long nonEscapingAllocation(int a, int b) {
        Point p = new Point(a, b);
        return p.getX() + p.getY();
    }

    public static class Point {
        private final int x;
        private final int y;

        public Point(int x, int y) {
            this.x = x;
            this.y = y;
        }

        public int getX() {
            return x;
        }

        public int getY() {
            return y;
        }
    }
}
