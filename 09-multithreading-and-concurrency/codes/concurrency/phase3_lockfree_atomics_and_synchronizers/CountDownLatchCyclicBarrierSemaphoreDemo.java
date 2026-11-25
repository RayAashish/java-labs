package concurrency.phase3_lockfree_atomics_and_synchronizers;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * Author: raysi
 * Demonstrates the Big Three Thread Coordination Synchronizers:
 * 1. CountDownLatch: One-shot latch (N -> 0). Cannot be reset.
 * 2. CyclicBarrier: Reusable barrier (parties synchronize and advance in lockstep cycles).
 * 3. Semaphore: Permit-based resource limiter / throttle.
 */
public class CountDownLatchCyclicBarrierSemaphoreDemo {

    public static void main(String[] args) throws InterruptedException {
        System.out.println("======================================================================");
        System.out.println("  PHASE 3 LAB: SYNCHRONIZERS (LATCH vs BARRIER vs SEMAPHORE)");
        System.out.println("  Author: raysi | Package: com.raysi.concurrency");
        System.out.println("======================================================================\n");

        System.out.println("=== PART 1: CountDownLatch (One-Shot Service Startup Gateway) ===");
        CountDownLatch startupLatch = new CountDownLatch(3);

        new Thread(new ServiceComponent("PostgreSQL Database", 100, startupLatch)).start();
        new Thread(new ServiceComponent("Redis Cache Cluster", 70, startupLatch)).start();
        new Thread(new ServiceComponent("Kafka Message Broker", 120, startupLatch)).start();

        System.out.println("[Gateway] Main thread waiting for all 3 critical dependencies to initialize...");
        startupLatch.await(5, TimeUnit.SECONDS); // Blocks until count drops to 0
        System.out.println("[Gateway] All dependencies healthy! Accepting incoming HTTP traffic.\n");

        System.out.println("=== PART 2: CyclicBarrier (Multi-Phase Lockstep Computation) ===");
        // 3 worker threads must complete Phase 1 before ANY worker starts Phase 2
        CyclicBarrier barrier = new CyclicBarrier(3, () ->
                System.out.println("  >>> [Barrier Action] All 3 workers arrived! Advancing to next phase... <<<"));

        Thread[] workers = new Thread[3];
        for (int i = 0; i < 3; i++) {
            final int id = i + 1;
            workers[i] = new Thread(() -> {
                try {
                    System.out.printf("[Worker-%d] Finished Phase 1 computations. Waiting at barrier...%n", id);
                    barrier.await(); // Synchronize at barrier 1

                    System.out.printf("[Worker-%d] Running Phase 2 computations. Waiting at barrier...%n", id);
                    barrier.await(); // Synchronize at barrier 2 (reused!)

                    System.out.printf("[Worker-%d] Job complete!%n", id);
                } catch (Exception e) {
                    Thread.currentThread().interrupt();
                }
            }, "Worker-" + id);
            workers[i].start();
        }
        for (Thread w : workers) {
            w.join();
        }

        System.out.println("\n=== PART 3: Semaphore (Throttling / Database Connection Pool) ===");
        // Only 2 concurrent connections allowed to the downstream database
        Semaphore dbConnectionPool = new Semaphore(2);

        Thread[] dbClients = new Thread[4];
        for (int i = 0; i < 4; i++) {
            final int clientId = i + 1;
            dbClients[i] = new Thread(() -> {
                try {
                    System.out.printf("[Client-%d] Requesting DB connection permit...%n", clientId);
                    dbConnectionPool.acquire(); // Blocks if 0 permits available
                    try {
                        System.out.printf("[Client-%d] >>> PERMIT ACQUIRED! Executing query on connection... (Available: %d)%n",
                                clientId, dbConnectionPool.availablePermits());
                        Thread.sleep(150); // Simulate query
                    } finally {
                        System.out.printf("[Client-%d] <<< Releasing DB connection permit.%n", clientId);
                        dbConnectionPool.release();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }, "Client-" + clientId);
            dbClients[i].start();
        }
        for (Thread c : dbClients) {
            c.join();
        }

        System.out.println("\n[Key Takeaway for Interviews]:");
        System.out.println("  - CountDownLatch: One-time gate. Threads wait for N events. Cannot be reset.");
        System.out.println("  - CyclicBarrier: Cyclic synchronization. N threads wait for EACH OTHER. Reusable.");
        System.out.println("  - Semaphore: Manages a fixed set of permits. Used for rate-limiting & connection pools.");
    }

    static class ServiceComponent implements Runnable {
        private final String name;
        private final long startupTimeMs;
        private final CountDownLatch latch;

        public ServiceComponent(String name, long startupTimeMs, CountDownLatch latch) {
            this.name = name;
            this.startupTimeMs = startupTimeMs;
            this.latch = latch;
        }

        @Override
        public void run() {
            try {
                System.out.printf("[Service] Initializing %s...%n", name);
                Thread.sleep(startupTimeMs);
                System.out.printf("[Service] %s INITIALIZED! (Calling countDown())%n", name);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                latch.countDown();
            }
        }
    }
}
