package concurrency.phase0_beginner;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Beginner lab demonstrating:
 * 1. Creating threads cleanly with Runnable + lambdas (and Virtual Threads).
 * 2. Why count++ loses updates under concurrent execution (Race Condition).
 * 3. Fixing the race condition with synchronized vs AtomicInteger.
 */
public class BeginnerThreadAndRaceConditionDemo {

    private static int unsafeCounter = 0;
    private static int syncCounter = 0;
    private static final AtomicInteger atomicCounter = new AtomicInteger(0);
    private static final Object LOCK = new Object();

    public static void main(String[] args) throws InterruptedException {
        int threads = 4;
        int incrementsPerThread = 25_000;

        Thread[] workers = new Thread[threads];
        for (int i = 0; i < threads; i++) {
            workers[i] = new Thread(() -> {
                for (int j = 0; j < incrementsPerThread; j++) {
                    unsafeCounter++;                   // BUG: non-atomic read-modify-write
                    synchronized (LOCK) { syncCounter++; } // Safe via mutual exclusion
                    atomicCounter.incrementAndGet();   // Safe via lock-free CPU CAS
                }
            }, "Worker-" + i);
            workers[i].start();
        }

        for (Thread t : workers) {
            t.join(); // Wait for all worker threads to finish
        }

        int expected = threads * incrementsPerThread;
        System.out.println("Expected count:      " + expected);
        System.out.println("Unsafe counter:      " + unsafeCounter + "  <-- lost updates!");
        System.out.println("Synchronized count:  " + syncCounter);
        System.out.println("AtomicInteger count: " + atomicCounter.get());
    }
}
