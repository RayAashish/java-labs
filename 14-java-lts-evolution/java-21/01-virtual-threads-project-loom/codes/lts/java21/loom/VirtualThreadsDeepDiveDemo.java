package lts.java21.loom;

import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Java 21 LTS — Virtual Threads (Project Loom / JEP 444).
 *
 * Covers:
 * 1. Spawning 10,000 concurrent blocking tasks over tiny OS carrier threads.
 * 2. Proper concurrency throttling via `Semaphore` (instead of pooling Virtual Threads!).
 * 3. Protecting shared I/O state with `ReentrantLock` cleanly (Java 21 pinning avoidance).
 */
public class VirtualThreadsDeepDiveDemo {

    private static final ReentrantLock SAFE_IO_LOCK = new ReentrantLock();

    public static void main(String[] args) throws Exception {
        System.out.println("=== Java 21 Virtual Threads (Project Loom / JEP 444) Demo ===");

        int taskCount = 10_000;
        Set<String> carriersObserved = ConcurrentHashMap.newKeySet();
        AtomicInteger completed = new AtomicInteger(0);

        Instant start = Instant.now();

        // Always use a fresh virtual thread per task; NEVER wrap virtual threads in a fixed thread pool!
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            for (int i = 0; i < taskCount; i++) {
                executor.submit(() -> {
                    String desc = Thread.currentThread().toString(); // e.g. VirtualThread[#32]/runnable@ForkJoinPool-1-worker-1
                    int atIdx = desc.lastIndexOf("ForkJoinPool");
                    if (atIdx >= 0) {
                        carriersObserved.add(desc.substring(atIdx));
                    }
                    try {
                        // Blocking sleep causes Continuation unmount -> frees OS carrier thread immediately!
                        Thread.sleep(60);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    completed.incrementAndGet();
                });
            }
        } // AutoCloseable blocks cleanly until all 10,000 virtual threads finish!

        long elapsedMs = Duration.between(start, Instant.now()).toMillis();
        System.out.printf("[1] 10,000 blocking tasks completed=%d in %d ms using only %d OS carriers!%n",
                completed.get(), elapsedMs, carriersObserved.size());

        // -------------------------------------------------------------
        // 2. Correct Throttling Pattern: Semaphore + ReentrantLock (NOT Fixed Thread Pool!)
        // -------------------------------------------------------------
        Semaphore dbConnectionLimit = new Semaphore(3); // Max 3 concurrent DB calls even with 100k virtual threads
        AtomicInteger maxInFlight = new AtomicInteger(0);
        AtomicInteger currentInFlight = new AtomicInteger(0);

        try (ExecutorService vtExec = Executors.newVirtualThreadPerTaskExecutor()) {
            for (int i = 0; i < 15; i++) {
                vtExec.submit(() -> {
                    dbConnectionLimit.acquireUninterruptibly();
                    try {
                        int active = currentInFlight.incrementAndGet();
                        maxInFlight.accumulateAndGet(active, Math::max);
                        Thread.sleep(10); // Simulated DB query
                        currentInFlight.decrementAndGet();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        dbConnectionLimit.release();
                    }
                });
            }
        }
        System.out.println("[2] Semaphore-throttled DB concurrency peak (must be <= 3): " + maxInFlight.get());

        // -------------------------------------------------------------
        // 3. ReentrantLock guards blocking section cleanly without pinning carriers on JDK 21
        // -------------------------------------------------------------
        Thread singleVt = Thread.ofVirtual().name("vt-reentrant-demo").start(() -> {
            SAFE_IO_LOCK.lock();
            try {
                Thread.sleep(5);
                System.out.println("[3] Executed safely under ReentrantLock on " + Thread.currentThread().getName()
                        + " (isVirtual=" + Thread.currentThread().isVirtual() + ")");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                SAFE_IO_LOCK.unlock();
            }
        });
        singleVt.join();
    }
}
