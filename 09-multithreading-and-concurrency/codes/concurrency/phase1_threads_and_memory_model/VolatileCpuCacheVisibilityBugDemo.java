package concurrency.phase1_threads_and_memory_model;

/**
 * Author: raysi
 * Demonstrates the two most critical concepts in the Java Memory Model (JMM):
 * 1. VISIBILITY BUG: Why a reader thread loops forever without 'volatile' (CPU L1 Cache / JIT hoisting).
 * 2. ATOMICITY BUG: Why 'volatile' is NOT enough for compound actions like 'count++'.
 */
public class VolatileCpuCacheVisibilityBugDemo {

    // Try removing 'volatile' here! Without 'volatile', the Worker Thread caches 'running = true'
    // locally in CPU registers / L1 cache and NEVER sees the Main Thread set 'running = false'!
    private static volatile boolean running = true;

    // Even with 'volatile', count++ is NOT atomic (Read -> Increment -> Write are 3 separate steps)
    private static volatile int unsafeVolatileCounter = 0;

    public static void main(String[] args) throws InterruptedException {
        System.out.println("======================================================================");
        System.out.println("  PHASE 1 LAB: CPU CACHE VISIBILITY & ATOMICITY WITH VOLATILE");
        System.out.println("  Author: raysi | Package: com.raysi.concurrency");
        System.out.println("======================================================================\n");

        System.out.println("=== TEST 1: CPU Cache Visibility with 'volatile' ===");

        Thread worker = new Thread(() -> {
            long iterations = 0;
            // The JIT compiler (C2) optimizes this loop into an infinite loop if 'running' is not volatile
            // (known as Loop Hoisting: transforming 'while(running)' into 'if(running) while(true)')
            while (running) {
                iterations++; // Tight loop without I/O or memory fences keeps reading register/L1
            }
            System.out.println("[Worker-Thread] Detected running=false! Stopped cleanly after iterations: " + iterations);
        }, "Worker-Thread");

        worker.start();
        Thread.sleep(500); // Let worker thread warm up, trigger JIT compilation, and cache 'running'

        System.out.println("[Main-Thread] Setting running = false in Main Memory...");
        running = false; // Volatile write inserts a StoreLoad Memory Barrier, flushing CPU cache to RAM!
        worker.join(2000);

        if (worker.isAlive()) {
            System.err.println("[Main-Thread] ERROR: Worker thread is STILL RUNNING! Visibility failure reproduced.");
            System.exit(1);
        } else {
            System.out.println("[Main-Thread] Worker thread terminated successfully due to volatile visibility guarantee.\n");
        }

        System.out.println("=== TEST 2: Why 'volatile' Fails at Atomicity (count++) ===");
        Thread t1 = new Thread(VolatileCpuCacheVisibilityBugDemo::increment100kTimes, "Incrementer-1");
        Thread t2 = new Thread(VolatileCpuCacheVisibilityBugDemo::increment100kTimes, "Incrementer-2");

        t1.start();
        t2.start();
        t1.join();
        t2.join();

        // Expected: 200,000 | Actual: Significantly less due to lost concurrent updates!
        System.out.println("Expected Counter: 200,000");
        System.out.println("Actual Counter:   " + unsafeVolatileCounter);
        int lostUpdates = 200_000 - unsafeVolatileCounter;
        System.out.println("Lost Updates:     " + lostUpdates);
        System.out.println("\n[Key Takeaway for Interviews]:");
        System.out.println("  - 'volatile' guarantees VISIBILITY (CPU cache flushes) & ORDERING (memory barriers).");
        System.out.println("  - 'volatile' does NOT guarantee ATOMICITY for compound operations (Read-Modify-Write).");
        System.out.println("  - For atomic increments, always use AtomicInteger.incrementAndGet() or LongAdder.");
    }

    private static void increment100kTimes() {
        for (int i = 0; i < 100_000; i++) {
            unsafeVolatileCounter++; // Compiles to: getstatic, iconst_1, iadd, putstatic (Race Condition!)
        }
    }
}
