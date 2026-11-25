package concurrency.phase3_lockfree_atomics_and_synchronizers;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicStampedReference;

/**
 * Author: raysi
 * Demonstrates:
 * 1. Performance benchmark: Synchronized Counter vs Lock-Free AtomicInteger (CAS).
 * 2. Hardware instruction: lock cmpxchg (Compare-And-Swap).
 * 3. The ABA Problem and its solution using AtomicStampedReference (value + version stamp).
 */
public class AtomicCASVsSynchronizedBenchmark {

    private static final int THREAD_COUNT = 8;
    private static final int ITERATIONS_PER_THREAD = 500_000;

    private static int syncCounter = 0;
    private static final Object syncLock = new Object();
    private static final AtomicInteger atomicCounter = new AtomicInteger(0);

    public static void main(String[] args) throws InterruptedException {
        System.out.println("======================================================================");
        System.out.println("  PHASE 3 LAB: LOCK-FREE CAS ATOMICS VS SYNCHRONIZED & ABA PROBLEM");
        System.out.println("  Author: raysi | Package: com.raysi.concurrency");
        System.out.println("======================================================================\n");

        System.out.println("=== PART 1: Performance Benchmark: Synchronized vs AtomicInteger ===");
        long startSync = System.currentTimeMillis();
        runBenchmark(AtomicCASVsSynchronizedBenchmark::incrementSync);
        long syncDuration = System.currentTimeMillis() - startSync;
        System.out.printf("Synchronized Counter : %d in %d ms%n", syncCounter, syncDuration);

        long startAtomic = System.currentTimeMillis();
        runBenchmark(atomicCounter::incrementAndGet);
        long atomicDuration = System.currentTimeMillis() - startAtomic;
        System.out.printf("AtomicInteger (CAS)  : %d in %d ms%n", atomicCounter.get(), atomicDuration);
        System.out.printf("Speedup factor: %.2fx faster with CAS Atomics!%n%n", (double) syncDuration / atomicDuration);

        System.out.println("=== PART 2: The ABA Problem & Solution via AtomicStampedReference ===");
        demonstrateAbaProblem();

        System.out.println("\n[Key Takeaway for Interviews]:");
        System.out.println("  - CAS uses CPU hardware instruction 'lock cmpxchg' instead of kernel locks.");
        System.out.println("  - CAS avoids OS thread suspension and context switches under moderate contention.");
        System.out.println("  - The ABA problem occurs when a value changes from A -> B -> A; naive CAS succeeds");
        System.out.println("    even though intermediate state changes occurred!");
        System.out.println("  - AtomicStampedReference pairs the reference with an integer version stamp.");
    }

    private static void incrementSync() {
        synchronized (syncLock) {
            syncCounter++;
        }
    }

    private static void runBenchmark(Runnable task) throws InterruptedException {
        Thread[] threads = new Thread[THREAD_COUNT];
        for (int i = 0; i < THREAD_COUNT; i++) {
            threads[i] = new Thread(() -> {
                for (int j = 0; j < ITERATIONS_PER_THREAD; j++) {
                    task.run();
                }
            });
            threads[i].start();
        }
        for (Thread t : threads) {
            t.join();
        }
    }

    private static void demonstrateAbaProblem() throws InterruptedException {
        String initialRef = "A";
        int initialStamp = 1;
        AtomicStampedReference<String> stampedRef = new AtomicStampedReference<>(initialRef, initialStamp);

        Thread victim = new Thread(() -> {
            int stamp = stampedRef.getStamp();
            String value = stampedRef.getReference();
            System.out.printf("[Victim-Thread] Read initial state: Value='%s', Stamp=%d%n", value, stamp);

            try {
                // Simulate delay where another thread changes A -> B -> A
                Thread.sleep(200);
            } catch (InterruptedException ignored) {}

            // Attempt CAS using expected stamp
            boolean success = stampedRef.compareAndSet("A", "C", stamp, stamp + 1);
            System.out.printf("[Victim-Thread] Attempted CAS (A -> C with Stamp %d): Succeeded? %b (Prevented ABA!)%n",
                    stamp, success);
        }, "Victim-Thread");

        Thread actor = new Thread(() -> {
            try {
                Thread.sleep(50);
            } catch (InterruptedException ignored) {}

            int[] holder = new int[1];
            String val = stampedRef.get(holder);
            System.out.printf("  [Actor-Thread] Modifying Value: '%s' -> 'B' (Stamp: %d -> %d)%n",
                    val, holder[0], holder[0] + 1);
            stampedRef.compareAndSet("A", "B", holder[0], holder[0] + 1);

            val = stampedRef.get(holder);
            System.out.printf("  [Actor-Thread] Modifying Value: '%s' -> 'A' (Stamp: %d -> %d)%n",
                    val, holder[0], holder[0] + 1);
            stampedRef.compareAndSet("B", "A", holder[0], holder[0] + 1);
            System.out.println("  [Actor-Thread] Completed ABA cycle! Value is 'A' again, but Stamp is 3.");
        }, "Actor-Thread");

        victim.start();
        actor.start();

        victim.join();
        actor.join();
    }
}
