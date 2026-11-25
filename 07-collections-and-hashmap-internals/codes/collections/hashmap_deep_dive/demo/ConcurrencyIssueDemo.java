package collections.hashmap_deep_dive.demo;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;

/**
 * Demonstrates the concurrency vulnerabilities of standard HashMap
 * compared to the thread-safety of ConcurrentHashMap.
 *
 * Runs 10 concurrent worker threads, each inserting 1,000 unique key-value pairs
 * (total expected = 10,000 mappings).
 */
public class ConcurrencyIssueDemo {

    private static final int NUM_THREADS = 10;
    private static final int INSERTS_PER_THREAD = 1_000;
    private static final int EXPECTED_TOTAL = NUM_THREADS * INSERTS_PER_THREAD;

    public static void main(String[] args) throws InterruptedException {
        System.out.println("======================================================================");
        System.out.println("        HASHMAP CONCURRENCY PITFALLS VS CONCURRENTHASHMAP DEMO        ");
        System.out.println("======================================================================");
        System.out.println("Threads: " + NUM_THREADS + " | Inserts per thread: " + INSERTS_PER_THREAD);
        System.out.println("Expected Final Map Size: " + EXPECTED_TOTAL + "\n");

        testStandardHashMap();
        testConcurrentHashMap();
    }

    /**
     * Demonstrates data loss / race conditions in unsynchronized java.util.HashMap.
     */
    static void testStandardHashMap() throws InterruptedException {
        System.out.println("----------------------------------------------------------------------");
        System.out.println("TEST 1: STANDARD java.util.HashMap (UNSYNCHRONIZED)");
        System.out.println("----------------------------------------------------------------------");

        Map<String, Integer> map = new HashMap<>();
        ExecutorService executor = Executors.newFixedThreadPool(NUM_THREADS);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(NUM_THREADS);

        long startTime = System.currentTimeMillis();

        for (int t = 0; t < NUM_THREADS; t++) {
            final int threadId = t;
            executor.submit(() -> {
                try {
                    startLatch.await(); // Synchronize all threads to start at the exact same instant
                    for (int i = 0; i < INSERTS_PER_THREAD; i++) {
                        String key = "T" + threadId + "_K" + i;
                        map.put(key, i);
                    }
                } catch (Exception e) {
                    System.err.println("Exception in thread " + threadId + ": " + e.getMessage());
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown(); // Fire simultaneous execution!
        doneLatch.await();
        executor.shutdown();

        long elapsed = System.currentTimeMillis() - startTime;
        int actualSize = map.size();

        System.out.println("Time taken           : " + elapsed + " ms");
        System.out.println("Expected Map Size    : " + EXPECTED_TOTAL);
        System.out.println("Actual Map Size      : " + actualSize);
        int lostEntries = EXPECTED_TOTAL - actualSize;

        if (actualSize != EXPECTED_TOTAL) {
            System.out.println(">>> CRITICAL BUG DETECTED: " + lostEntries + " ENTRIES SILENTLY LOST! <<<");
            System.out.println("Reason: Multiple threads concurrently executing putVal() checked");
            System.out.println("table[i] == null at the same time, overwriting each other's nodes,");
            System.out.println("plus the unsynchronized ++size operation suffered lost updates.");
        } else {
            System.out.println("No entries lost in this specific run (race conditions are non-deterministic).");
        }
    }

    /**
     * Demonstrates complete thread-safety with java.util.concurrent.ConcurrentHashMap.
     */
    static void testConcurrentHashMap() throws InterruptedException {
        System.out.println("\n----------------------------------------------------------------------");
        System.out.println("TEST 2: java.util.concurrent.ConcurrentHashMap (THREAD-SAFE)");
        System.out.println("----------------------------------------------------------------------");

        Map<String, Integer> concurrentMap = new ConcurrentHashMap<>();
        ExecutorService executor = Executors.newFixedThreadPool(NUM_THREADS);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(NUM_THREADS);

        long startTime = System.currentTimeMillis();

        for (int t = 0; t < NUM_THREADS; t++) {
            final int threadId = t;
            executor.submit(() -> {
                try {
                    startLatch.await();
                    for (int i = 0; i < INSERTS_PER_THREAD; i++) {
                        String key = "T" + threadId + "_K" + i;
                        concurrentMap.put(key, i);
                    }
                } catch (Exception e) {
                    System.err.println("Exception in thread " + threadId + ": " + e.getMessage());
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        doneLatch.await();
        executor.shutdown();

        long elapsed = System.currentTimeMillis() - startTime;
        int actualSize = concurrentMap.size();

        System.out.println("Time taken           : " + elapsed + " ms");
        System.out.println("Expected Map Size    : " + EXPECTED_TOTAL);
        System.out.println("Actual Map Size      : " + actualSize);

        if (actualSize == EXPECTED_TOTAL) {
            System.out.println(">>> [SUCCESS] EXACTLY 10,000 ENTRIES PRESERVED WITH ZERO DATA LOSS! <<<");
            System.out.println("How: ConcurrentHashMap uses CAS for empty bucket allocation and");
            System.out.println("synchronized(bucketHead) for bucket collisions, ensuring full thread safety.");
        }
    }
}
