package com.raysi.internals.concurrency;

/**
 * Architectural Demonstration: Memory Visibility, Caching & Volatile Memory Barriers.
 * <p>
 * Key Concepts Demonstrated:
 * 1. CPU Store Buffers and L1 caches: A thread modifying a non-volatile variable can keep the
 *    change buffered in its local register or L1 cache without flushing to L3/RAM.
 * 2. Another thread executing in a tight loop may never observe the updated value, spinning forever!
 * 3. Declaring the flag 'volatile' instructs the JIT to emit hardware memory fences
 *    (StoreLoad on write, LoadLoad on read), forcing immediate cross-core visibility.
 */
public class VolatileAndInstructionReorderingDemo {

    // Volatile flag ensures visibility across CPU cores via memory fences
    private static volatile boolean running = true;
    private static int counter = 0;

    public static void main(String[] args) throws InterruptedException {
        System.out.println("================================================================================");
        System.out.println("   VOLATILE MEMORY BARRIERS & VISIBILITY DEMONSTRATION");
        System.out.println("================================================================================");

        System.out.println("\n[1] Starting worker thread reading 'volatile boolean running' flag...");
        Thread worker = new Thread(() -> {
            long iterations = 0;
            // The JIT cannot optimize this read into a CPU register loop because 'running' is volatile!
            while (running) {
                iterations++;
                counter++;
            }
            System.out.printf("   -> Worker thread observed 'running=false' after %,d iterations!%n", iterations);
        });

        worker.start();

        // Let the worker spin for 100 milliseconds
        Thread.sleep(100);

        System.out.println("[2] Main thread setting 'running = false' across memory barrier...");
        running = false; // Emits hardware memory barrier flushing store buffer!

        // Wait for worker to terminate
        worker.join(2000);

        if (worker.isAlive()) {
            System.err.println("   [!] FAILURE: Worker thread did not see the update (Thread is hung)!");
            worker.interrupt();
        } else {
            System.out.println("   -> SUCCESS: Worker thread terminated cleanly!");
            System.out.println("   -> Final shared counter: " + counter);
        }

        System.out.println("\n================================================================================");
        System.out.println("   Hardware memory fence visibility confirmed.");
        System.out.println("================================================================================");
    }
}
