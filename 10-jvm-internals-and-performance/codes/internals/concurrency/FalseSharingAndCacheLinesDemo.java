package internals.concurrency;

/**
 * Architectural Demonstration: False Sharing & 64-Byte CPU Cache Line Bouncing.
 * <p>
 * Key Concepts Demonstrated:
 * 1. Modern CPU cores load memory into L1 caches in 64-byte chunks (Cache Lines).
 * 2. When two threads concurrently write to independent volatile fields that happen to share
 *    the SAME 64-byte cache line, the hardware MESI protocol continuously invalidates
 *    the cache line between cores (Cache Line Ping-Pong).
 * 3. Adding 56 bytes of padding isolates fields to separate cache lines, unlocking true parallelism.
 */
public class FalseSharingAndCacheLinesDemo {

    private static final long ITERATIONS = 100_000_000L;

    public static void main(String[] args) throws InterruptedException {
        System.out.println("================================================================================");
        System.out.println("   FALSE SHARING & 64-BYTE CPU CACHE LINE CONTENTION BENCHMARK");
        System.out.println("================================================================================");

        // 1. Benchmark Unpadded (Contended Cache Line)
        System.out.println("\n[1] Running UNPADDED Benchmark (Two threads writing to same 64-byte line)...");
        UnpaddedHolder unpadded = new UnpaddedHolder();
        long unpaddedTime = runBenchmark(unpadded);
        System.out.printf("   -> Unpadded Execution Time: %d ms%n", unpaddedTime);

        // 2. Benchmark Padded (Isolated Cache Lines)
        System.out.println("\n[2] Running PADDED Benchmark (Two threads writing to isolated cache lines)...");
        PaddedHolder padded = new PaddedHolder();
        long paddedTime = runBenchmark(padded);
        System.out.printf("   -> Padded Execution Time  : %d ms%n", paddedTime);

        // 3. Comparison
        double speedup = (double) unpaddedTime / (paddedTime == 0 ? 1 : paddedTime);
        System.out.println("\n[3] Architectural Conclusion:");
        System.out.printf("   -> Cache-line isolation yielded a %.2fx speedup!%n", speedup);
        System.out.println("   -> In unpadded memory, the two CPU cores fought over the L1 cache line bus.");
        System.out.println("   -> In padded memory, each core modified its own private L1 cache line without bus contention.");

        System.out.println("\n================================================================================");
    }

    private static long runBenchmark(TargetHolder target) throws InterruptedException {
        Thread t1 = new Thread(() -> {
            for (long i = 0; i < ITERATIONS; i++) {
                target.incrementA();
            }
        });

        Thread t2 = new Thread(() -> {
            for (long i = 0; i < ITERATIONS; i++) {
                target.incrementB();
            }
        });

        long start = System.currentTimeMillis();
        t1.start();
        t2.start();
        t1.join();
        t2.join();
        return System.currentTimeMillis() - start;
    }

    public interface TargetHolder {
        void incrementA();
        void incrementB();
    }

    /**
     * UNPADDED: 'valueA' and 'valueB' are adjacent in memory.
     * Total size = 8 + 8 = 16 bytes. They easily fit within the same 64-byte cache line!
     */
    public static class UnpaddedHolder implements TargetHolder {
        public volatile long valueA = 0L;
        public volatile long valueB = 0L;

        @Override
        public void incrementA() {
            valueA++;
        }

        @Override
        public void incrementB() {
            valueB++;
        }
    }

    /**
     * PADDED: 7 dummy longs (7 * 8 = 56 bytes) placed between valueA and valueB.
     * valueA (8B) + padding (56B) = 64 bytes!
     * valueB is guaranteed to reside on a fresh, independent CPU cache line!
     */
    public static class PaddedHolder implements TargetHolder {
        public volatile long valueA = 0L;
        // 56 bytes of cache-line padding
        public long p1, p2, p3, p4, p5, p6, p7;
        public volatile long valueB = 0L;

        @Override
        public void incrementA() {
            valueA++;
        }

        @Override
        public void incrementB() {
            valueB++;
        }
    }
}
