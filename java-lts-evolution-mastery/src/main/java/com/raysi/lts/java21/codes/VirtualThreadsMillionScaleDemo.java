package com.raysi.lts.java21.codes;

import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Author: raysi
 * Package: com.raysi.lts.java21.codes
 *
 * Demonstrates:
 * 1. Project Loom: Virtual Threads (JEP 444) at massive concurrent scale.
 * 2. Executors.newVirtualThreadPerTaskExecutor() replacing pooled thread executors.
 * 3. Carrier thread mapping: 10,000 concurrent blocking tasks executed across a tiny carrier pool.
 * 4. Fiber unmounting and continuation mechanics during blocking I/O simulation.
 */
public class VirtualThreadsMillionScaleDemo {

    public static void main(String[] args) throws Exception {
        System.out.println("=== Java 21: Project Loom Virtual Threads Million-Scale Demo ===");

        int taskCount = 10_000;
        Set<String> carrierThreadNames = ConcurrentHashMap.newKeySet();
        AtomicInteger completedTasks = new AtomicInteger(0);

        System.out.printf("[+] Launching %d concurrent blocking tasks with Virtual Threads...%n", taskCount);
        Instant start = Instant.now();

        // 1. Create a VirtualThreadPerTaskExecutor
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            for (int i = 0; i < taskCount; i++) {
                final int taskId = i;
                executor.submit(() -> {
                    // Extract the underlying carrier thread name from the virtual thread string
                    // VirtualThread toString: VirtualThread[#32,runnable]/ForkJoinPool-1-worker-1
                    String threadStr = Thread.currentThread().toString();
                    if (threadStr.contains("/")) {
                        String carrier = threadStr.substring(threadStr.indexOf("/") + 1);
                        carrierThreadNames.add(carrier);
                    }

                    try {
                        // SIMULATE BLOCKING I/O: This causes the virtual thread to UNMOUNT from its carrier thread!
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }

                    completedTasks.incrementAndGet();
                });
            }
        } // AutoCloseable on ExecutorService blocks until all submitted tasks complete!

        Duration totalTime = Duration.between(start, Instant.now());

        System.out.println("\n--- Execution Telemetry ---");
        System.out.printf(">> Total Tasks Completed: %d%n", completedTasks.get());
        System.out.printf(">> Total Wall-Clock Time: %d ms%n", totalTime.toMillis());
        System.out.printf(">> Unique OS Carrier Threads Used: %d%n", carrierThreadNames.size());
        System.out.println(">> Sample Carrier Worker Threads: " + carrierThreadNames.stream().limit(5).toList());
        System.out.println("[+] Notice: 10,000 blocking tasks completed in ~150-250ms with a fraction of OS threads!");
    }
}
