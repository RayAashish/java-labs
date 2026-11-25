package concurrency.phase5_virtual_threads_project_loom;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Author: raysi
 * Demonstrates:
 * 1. Platform Thread Scalability Wall: Why 1:1 OS threads cannot scale to 100,000 concurrent tasks
 *    (each consumes ~1MB stack memory and exhausts OS thread descriptors).
 * 2. Java 21+ Virtual Threads (Project Loom): Running 100,000 concurrent blocking tasks smoothly
 *    using Executors.newVirtualThreadPerTaskExecutor().
 * 3. Execution time and memory footprint comparison.
 */
public class PlatformVsVirtualThreads100kBenchmark {

    private static final int PLATFORM_THREAD_COUNT = 2_000;
    private static final int VIRTUAL_THREAD_COUNT = 100_000;
    private static final int TASK_SLEEP_MS = 100;

    public static void main(String[] args) {
        System.out.println("======================================================================");
        System.out.println("  PHASE 5 LAB: PLATFORM THREADS VS VIRTUAL THREADS 100K BENCHMARK");
        System.out.println("  Author: raysi | Package: com.raysi.concurrency");
        System.out.println("======================================================================\n");

        printMemorySnapshot("Baseline Memory");

        // 1. Benchmark Classic Platform Threads (2,000 threads)
        System.out.printf("=== TEST 1: Running %d Platform Threads (1:1 OS Mapping) ===%n", PLATFORM_THREAD_COUNT);
        try (ExecutorService platformExecutor = Executors.newFixedThreadPool(500)) {
            Instant start = Instant.now();
            AtomicInteger completedCount = new AtomicInteger();

            for (int i = 0; i < PLATFORM_THREAD_COUNT; i++) {
                platformExecutor.submit(() -> {
                    simulateBlockingIo();
                    completedCount.incrementAndGet();
                });
            }
            platformExecutor.close(); // Waits for all submitted tasks to complete
            Duration duration = Duration.between(start, Instant.now());
            System.out.printf("Platform Threads Completed: %d tasks in %d ms%n",
                    completedCount.get(), duration.toMillis());
        }

        printMemorySnapshot("After Platform Threads");

        // 2. Benchmark Java 21+ Virtual Threads (100,000 threads!)
        System.out.printf("%n=== TEST 2: Running %d VIRTUAL THREADS (M:N User-Space Fibers) ===%n", VIRTUAL_THREAD_COUNT);
        try (ExecutorService virtualExecutor = Executors.newVirtualThreadPerTaskExecutor()) {
            Instant start = Instant.now();
            AtomicInteger completedCount = new AtomicInteger();

            for (int i = 0; i < VIRTUAL_THREAD_COUNT; i++) {
                virtualExecutor.submit(() -> {
                    simulateBlockingIo();
                    completedCount.incrementAndGet();
                });
            }
            virtualExecutor.close(); // Waits for all 100,000 virtual threads
            Duration duration = Duration.between(start, Instant.now());
            System.out.printf("Virtual Threads Completed : %d tasks in %d ms!%n",
                    completedCount.get(), duration.toMillis());
        }

        printMemorySnapshot("After 100,000 Virtual Threads");

        System.out.println("\n[Key Takeaway for Interviews]:");
        System.out.println("  - Platform Threads are 1:1 with OS Kernel threads (~1MB native stack each).");
        System.out.println("    Creating 100,000 platform threads requires ~100 GB of RAM (crashes JVM with OOM).");
        System.out.println("  - Virtual Threads are user-space JVM primitives scheduled on a tiny ForkJoinPool.");
        System.out.println("  - A Virtual Thread stack begins at only a few hundred bytes on the Java Heap.");
        System.out.println("  - When a virtual thread blocks on I/O, it unmounts from its carrier thread—zero OS starvation!");
    }

    private static void simulateBlockingIo() {
        try {
            // Virtual thread unmounts from its OS carrier thread during Thread.sleep!
            Thread.sleep(TASK_SLEEP_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static void printMemorySnapshot(String stage) {
        Runtime runtime = Runtime.getRuntime();
        long usedMb = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024);
        System.out.printf("[Memory Profiler] %-30s | Heap Used: ~%d MB%n", stage, usedMb);
    }
}
