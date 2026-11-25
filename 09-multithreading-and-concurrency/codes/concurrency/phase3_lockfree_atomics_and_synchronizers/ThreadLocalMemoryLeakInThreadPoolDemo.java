package concurrency.phase3_lockfree_atomics_and_synchronizers;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Author: raysi
 * Demonstrates:
 * 1. ThreadLocal Architecture: Thread -> ThreadLocalMap -> Entry (WeakReference Key, Strong Value).
 * 2. The Classic Production ThreadLocal Memory Leak in Thread Pools (Tomcat / ExecutorService).
 * 3. The Golden Rule: Always invoke threadLocal.remove() inside a finally block!
 */
public class ThreadLocalMemoryLeakInThreadPoolDemo {

    // Simulates a large request payload (e.g. 10MB User Context per HTTP request)
    static class BigUserContext {
        private final byte[] payload = new byte[5 * 1024 * 1024]; // 5 MB
        private final String userId;

        public BigUserContext(String userId) {
            this.userId = userId;
        }

        @Override
        public String toString() {
            return "BigUserContext{userId='" + userId + "', size=" + (payload.length / 1024 / 1024) + "MB}";
        }
    }

    private static final ThreadLocal<BigUserContext> userContextHolder = new ThreadLocal<>();

    public static void main(String[] args) throws InterruptedException {
        System.out.println("======================================================================");
        System.out.println("  PHASE 3 LAB: THREADLOCAL INTERNALS & THREAD POOL MEMORY LEAK");
        System.out.println("  Author: raysi | Package: com.raysi.concurrency");
        System.out.println("======================================================================\n");

        printMemoryUsage("Initial State");

        // Simulating a web server thread pool (reusable worker threads)
        ExecutorService workerPool = Executors.newFixedThreadPool(2);

        System.out.println("=== PART 1: The Memory Leak Trap (Forgetting .remove()) ===");
        for (int i = 1; i <= 2; i++) {
            final String userId = "user-" + i;
            workerPool.submit(() -> {
                userContextHolder.set(new BigUserContext(userId));
                System.out.printf("[%s] Processing request for %s. Set ThreadLocal value.%n",
                        Thread.currentThread().getName(), userId);
                // BUG: Task finishes without calling userContextHolder.remove()!
                // The worker thread stays alive in the pool, holding a strong reference to the 5MB payload!
            });
        }

        Thread.sleep(300);
        System.gc(); // Request GC
        Thread.sleep(200);
        printMemoryUsage("After leaky tasks + GC (Memory remains pinned to pool threads!)");

        System.out.println("\n=== PART 2: The Production Fix (try-finally with .remove()) ===");
        for (int i = 3; i <= 4; i++) {
            final String userId = "user-" + i;
            workerPool.submit(() -> {
                try {
                    userContextHolder.set(new BigUserContext(userId));
                    System.out.printf("[%s] Safely processing request for %s.%n",
                            Thread.currentThread().getName(), userId);
                } finally {
                    // SENIOR BEST PRACTICE: Always remove to prevent memory leaks and data pollution across requests!
                    userContextHolder.remove();
                    System.out.printf("[%s] Successfully cleaned up ThreadLocal via remove()!%n",
                            Thread.currentThread().getName());
                }
            });
        }

        workerPool.shutdown();
        workerPool.awaitTermination(2, TimeUnit.SECONDS);

        System.gc();
        Thread.sleep(200);
        printMemoryUsage("Final State after pool shutdown + GC");

        System.out.println("\n[Key Takeaway for Interviews]:");
        System.out.println("  - ThreadLocalMap.Entry extends WeakReference<ThreadLocal<?>>.");
        System.out.println("  - The KEY is a WeakReference, but the VALUE is a STRONG reference!");
        System.out.println("  - When a ThreadLocal variable goes out of scope, the key is collected (becomes null).");
        System.out.println("  - But because worker threads in a pool NEVER die, Entry.value remains in the heap forever!");
        System.out.println("  - In web applications, this causes OutOfMemoryError and security data bleeding.");
    }

    private static void printMemoryUsage(String stage) {
        Runtime runtime = Runtime.getRuntime();
        long usedMemory = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024);
        System.out.printf("[Memory Status] %-45s | Used Heap: ~%d MB%n", stage, usedMemory);
    }
}
