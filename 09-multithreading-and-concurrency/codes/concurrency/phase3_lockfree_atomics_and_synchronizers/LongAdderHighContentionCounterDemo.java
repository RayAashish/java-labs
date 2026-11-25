package concurrency.phase3_lockfree_atomics_and_synchronizers;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

/**
 * Author: raysi
 * Demonstrates:
 * 1. The CAS Retry Storm problem: Why AtomicLong degrades under high contention (Cache Line Bouncing).
 * 2. LongAdder architecture: Stripped 64-bit Cell[] array, Thread probe hashing, and sum() aggregation.
 * 3. High contention benchmark (16 threads, 10,000,000 increments).
 */
public class LongAdderHighContentionCounterDemo {

    private static final int THREADS = 16;
    private static final int ITERATIONS_PER_THREAD = 1_000_000;

    public static void main(String[] args) throws InterruptedException {
        System.out.println("======================================================================");
        System.out.println("  PHASE 3 LAB: LONGADDER VS ATOMICLONG (CAS RETRY STORM BENCHMARK)");
        System.out.println("  Author: raysi | Package: com.raysi.concurrency");
        System.out.println("======================================================================\n");

        System.out.printf("Running High Contention Benchmark: %d Threads x %d Increments = %d Total%n%n",
                THREADS, ITERATIONS_PER_THREAD, (long) THREADS * ITERATIONS_PER_THREAD);

        // 1. Benchmark AtomicLong
        AtomicLong atomicLong = new AtomicLong(0);
        long startAtomic = System.currentTimeMillis();
        runThreads(() -> {
            for (int i = 0; i < ITERATIONS_PER_THREAD; i++) {
                atomicLong.incrementAndGet(); // Single volatile variable: severe CAS retry storm!
            }
        });
        long durationAtomic = System.currentTimeMillis() - startAtomic;
        System.out.printf("AtomicLong Duration : %d ms | Final Count: %d%n",
                durationAtomic, atomicLong.get());

        // 2. Benchmark LongAdder
        LongAdder longAdder = new LongAdder();
        long startAdder = System.currentTimeMillis();
        runThreads(() -> {
            for (int i = 0; i < ITERATIONS_PER_THREAD; i++) {
                longAdder.increment(); // Disperses writes across Cell[] array via thread probe!
            }
        });
        long durationAdder = System.currentTimeMillis() - startAdder;
        System.out.printf("LongAdder Duration  : %d ms | Final Count: %d%n",
                durationAdder, longAdder.sum());

        double speedup = (double) durationAtomic / durationAdder;
        System.out.printf("%n[Result]: LongAdder is %.2fx faster under %d concurrent threads!%n", speedup, THREADS);

        System.out.println("\n[Key Takeaway for Interviews]:");
        System.out.println("  - AtomicLong uses a SINGLE volatile memory location. 16 cores competing causes");
        System.out.println("    constant Cache Line Bouncing (MESI 'Invalidate' bus traffic).");
        System.out.println("  - LongAdder extends Striped64, maintaining a dynamic Cell[] array.");
        System.out.println("  - Each Cell is padded with @Contended to prevent False Sharing.");
        System.out.println("  - Trade-off: LongAdder provides an eventual snapshot with .sum() (not atomic across time).");
        System.out.println("    Use AtomicLong when you need compareAndSet(); use LongAdder for high-throughput metrics.");
    }

    private static void runThreads(Runnable task) throws InterruptedException {
        Thread[] threads = new Thread[THREADS];
        for (int i = 0; i < THREADS; i++) {
            threads[i] = new Thread(task, "Worker-" + i);
            threads[i].start();
        }
        for (Thread t : threads) {
            t.join();
        }
    }
}
