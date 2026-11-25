package concurrency.phase4_executors_and_completablefuture;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Author: raysi
 * Demonstrates:
 * 1. ThreadPoolExecutor Lifecycle & Task Routing: Core -> Queue -> Max -> Reject.
 * 2. Why Executors.newFixedThreadPool() causes OutOfMemoryError in production (Unbounded Queue).
 * 3. The 4 Built-in Rejection Policies:
 *    - AbortPolicy (Throws RejectedExecutionException)
 *    - CallerRunsPolicy (Pushes backpressure onto caller thread)
 *    - DiscardPolicy (Silently drops task)
 *    - DiscardOldestPolicy (Drops oldest queued task)
 * 4. Production-grade Custom Rejection Handler with logging & fallback.
 */
public class CustomThreadPoolExecutorRejectionDemo {

    public static void main(String[] args) throws InterruptedException {
        System.out.println("======================================================================");
        System.out.println("  PHASE 4 LAB: THREADPOOLEXECUTOR SATURATION & REJECTION POLICIES");
        System.out.println("  Author: raysi | Package: com.raysi.concurrency");
        System.out.println("======================================================================\n");

        // Bounded ThreadPool: Core=2, Max=4, Queue Capacity=2
        // Total concurrent saturation threshold = Max (4) + Queue (2) = 6 tasks!
        // The 7th task triggers the RejectedExecutionHandler.
        System.out.println("=== PART 1: CallerRunsPolicy (Backpressure in Microservices) ===");
        ThreadPoolExecutor callerRunsPool = new ThreadPoolExecutor(
                2,                               // Core pool size
                4,                               // Maximum pool size
                60L, TimeUnit.SECONDS,           // Keep-alive time for idle extra threads
                new ArrayBlockingQueue<>(2),     // Bounded queue (capacity 2)
                new CustomNamedThreadFactory("CallerRuns-Worker"),
                new ThreadPoolExecutor.CallerRunsPolicy() // Caller executes task when saturated!
        );

        submitTasks(callerRunsPool, 8, "CallerRuns");
        callerRunsPool.shutdown();
        callerRunsPool.awaitTermination(3, TimeUnit.SECONDS);

        System.out.println("\n=== PART 2: Custom Production Rejection Handler ===");
        ThreadPoolExecutor customRejectPool = new ThreadPoolExecutor(
                1, 2, 60L, TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(2),
                new CustomNamedThreadFactory("Production-Worker"),
                (task, executor) -> {
                    // Custom fallback: Log metrics, alert APM, or route to fallback dead-letter store
                    System.err.printf("  [ALERT - REJECTION HANDLER] Task %s REJECTED! Pool Saturation: [Active: %d, Queue: %d, Completed: %d]%n",
                            task, executor.getActiveCount(), executor.getQueue().size(), executor.getCompletedTaskCount());
                }
        );

        submitTasks(customRejectPool, 6, "CustomReject");
        customRejectPool.shutdown();
        customRejectPool.awaitTermination(3, TimeUnit.SECONDS);

        System.out.println("\n[Key Takeaway for Interviews]:");
        System.out.println("  - Task Routing Order: (1) Core Thread -> (2) BlockingQueue -> (3) Max Thread -> (4) Reject.");
        System.out.println("  - Never use Executors.newFixedThreadPool() in production (uses LinkedBlockingQueue with Integer.MAX_VALUE).");
        System.out.println("  - Always use a Bounded Queue (ArrayBlockingQueue) with CallerRunsPolicy or a custom handler");
        System.out.println("    to apply organic backpressure to upstream callers.");
    }

    private static void submitTasks(ThreadPoolExecutor pool, int taskCount, String label) {
        for (int i = 1; i <= taskCount; i++) {
            final int taskId = i;
            try {
                System.out.printf("[%s-Submitter] Submitting Task-%d (Pool Active: %d, Queue: %d)...%n",
                        label, taskId, pool.getActiveCount(), pool.getQueue().size());
                pool.execute(() -> {
                    System.out.printf("  --> [%s] Executing Task-%d on thread: %s%n",
                            label, taskId, Thread.currentThread().getName());
                    try {
                        Thread.sleep(200); // Simulate processing time
                    } catch (InterruptedException ignored) {}
                });
            } catch (Exception e) {
                System.err.printf("[%s-Submitter] Submission failed for Task-%d: %s%n", label, taskId, e.getMessage());
            }
        }
    }

    static class CustomNamedThreadFactory implements ThreadFactory {
        private final String prefix;
        private final AtomicInteger counter = new AtomicInteger(1);

        public CustomNamedThreadFactory(String prefix) {
            this.prefix = prefix;
        }

        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r, prefix + "-" + counter.getAndIncrement());
            t.setDaemon(false);
            return t;
        }
    }
}
