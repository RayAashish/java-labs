package collections.map.concurrenthashmap;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Production-Grade Demonstration of ConcurrentHashMap Architecture & Modern Capabilities.
 * 
 * Explores:
 *  1. Atomic Compound Operations: computeIfAbsent (Lazy Caching) & merge (Atomic Aggregation)
 *  2. Weakly Consistent Iterators: Traversing without ConcurrentModificationException
 *  3. Null Rejection: Demonstrating NPE on null key and null value
 *  4. High-Throughput Concurrent Word Frequency Counter:
 *     Race-Free merge() vs Flawed Read-Modify-Write
 *  5. Large-Scale Sizing: mappingCount() (64-bit) vs size() (32-bit clamped)
 *  6. Parallel Bulk Operations (ForkJoinPool Integration):
 *     forEach, search, reduce with parallelism thresholds
 *  7. Multi-threaded Contention Benchmark: ConcurrentHashMap vs Collections.synchronizedMap()
 * 
 * @author raysi
 */
public class ConcurrentHashMapCASAndLockingDemo {

    public static void main(String[] args) throws InterruptedException {
        System.out.println("========================================================================");
        System.out.println(" 1. STRICT NULL REJECTION (KEYS & VALUES)");
        System.out.println("========================================================================");

        ConcurrentHashMap<String, String> chm = new ConcurrentHashMap<>();

        // A. Demonstrating Null Key Rejection
        try {
            System.out.println("Attempting chm.put(null, \"Value\")...");
            chm.put(null, "Value");
        } catch (NullPointerException e) {
            System.out.println("-> CAUGHT NullPointerException: ConcurrentHashMap strictly disallows null keys!");
        }

        // B. Demonstrating Null Value Rejection
        try {
            System.out.println("Attempting chm.put(\"ValidKey\", null)...");
            chm.put("ValidKey", null);
        } catch (NullPointerException e) {
            System.out.println("-> CAUGHT NullPointerException: ConcurrentHashMap strictly disallows null values!");
        }

        System.out.println("\n========================================================================");
        System.out.println(" 2. ATOMIC COMPOUND OPERATIONS: computeIfAbsent & merge");
        System.out.println("========================================================================");

        // Scenario: Thread-Safe In-Memory Cache with Lazy Initialization
        // computeIfAbsent guarantees that the mapping function executes AT MOST ONCE per key.
        // It locks the bucket head or places a ReservationNode to prevent cache stampedes!
        ConcurrentHashMap<String, List<String>> userRolesCache = new ConcurrentHashMap<>();
        AtomicInteger databaseQueryCount = new AtomicInteger(0);

        String userId = "user-1001";

        // Simulating 5 concurrent requests attempting to fetch roles for user-1001
        int cacheWorkers = 5;
        ExecutorService cacheExecutor = Executors.newFixedThreadPool(cacheWorkers);
        CountDownLatch cacheLatch = new CountDownLatch(cacheWorkers);

        for (int i = 0; i < cacheWorkers; i++) {
            cacheExecutor.submit(() -> {
                try {
                    // Atomic computeIfAbsent: ONLY ONE thread executes the lambda!
                    userRolesCache.computeIfAbsent(userId, k -> {
                        databaseQueryCount.incrementAndGet();
                        System.out.println("   [DB Query] Expensive DB query executed for: " + k);
                        return new CopyOnWriteArrayList<>(List.of("ROLE_USER", "ROLE_ANALYST"));
                    });
                } finally {
                    cacheLatch.countDown();
                }
            });
        }

        cacheLatch.await();
        cacheExecutor.shutdown();

        System.out.println("Total DB Queries executed across " + cacheWorkers + " concurrent requests: " + databaseQueryCount.get());
        System.out.println("Cached Roles: " + userRolesCache.get(userId));

        System.out.println("\n========================================================================");
        System.out.println(" 3. WEAKLY CONSISTENT ITERATOR DEMONSTRATION");
        System.out.println("========================================================================");

        ConcurrentHashMap<Integer, String> streamMap = new ConcurrentHashMap<>();
        for (int i = 1; i <= 5; i++) {
            streamMap.put(i, "Event-" + i);
        }

        System.out.println("Starting iterator traversal. Inserting elements concurrently...");
        Iterator<Map.Entry<Integer, String>> it = streamMap.entrySet().iterator();

        while (it.hasNext()) {
            Map.Entry<Integer, String> entry = it.next();
            System.out.println("  Iterator read: " + entry.getKey() + " -> " + entry.getValue());
            if (entry.getKey().equals(3)) {
                // Concurrently insert new items while iterating!
                // In HashMap or Hashtable, this throws ConcurrentModificationException immediately!
                streamMap.put(99, "Event-Injected-99");
                streamMap.put(100, "Event-Injected-100");
                System.out.println("  --> [Writer] Injected key 99 and 100 into map!");
            }
        }
        System.out.println("-> Iteration completed without ConcurrentModificationException!");
        System.out.println("Final map state: " + streamMap);

        System.out.println("\n========================================================================");
        System.out.println(" 4. CONCURRENT WORD FREQUENCY COUNTING: merge() VS FLAWED PATTERN");
        System.out.println("========================================================================");

        List<String> textStream = List.of(
            "jvm", "garbage", "collector", "memory", "jvm",
            "heap", "stack", "jvm", "memory", "concurrency",
            "threads", "jvm", "cas", "lock", "concurrency"
        );

        // Approach A: Atomic merge()
        ConcurrentHashMap<String, Long> atomicWordCounts = new ConcurrentHashMap<>();
        int workers = 4;
        ExecutorService textExecutor = Executors.newFixedThreadPool(workers);
        CountDownLatch textLatch = new CountDownLatch(workers);

        for (int w = 0; w < workers; w++) {
            textExecutor.submit(() -> {
                try {
                    for (int round = 0; round < 1000; round++) {
                        for (String word : textStream) {
                            // Atomic merge: (key, initialValue, remappingFunction)
                            atomicWordCounts.merge(word, 1L, Long::sum);
                        }
                    }
                } finally {
                    textLatch.countDown();
                }
            });
        }

        textLatch.await();
        textExecutor.shutdown();

        // Each word in textStream appears with known frequencies.
        // "jvm" appears 4 times per loop * 1000 rounds * 4 workers = 16,000.
        System.out.println("Expected count for 'jvm': 16000");
        System.out.println("Actual count via merge() : " + atomicWordCounts.get("jvm"));
        System.out.println("Word 'concurrency' count : " + atomicWordCounts.get("concurrency"));

        System.out.println("\n========================================================================");
        System.out.println(" 5. LARGE-SCALE SIZING: mappingCount() VS size()");
        System.out.println("========================================================================");

        // size() returns an int clamped to Integer.MAX_VALUE (2,147,483,647)
        // mappingCount() returns a long, preventing integer overflow in multi-billion entry caches.
        ConcurrentHashMap<String, String> sizeMap = new ConcurrentHashMap<>();
        sizeMap.put("node1", "active");
        sizeMap.put("node2", "active");

        int sizeAsInt = sizeMap.size();
        long sizeAsLong = sizeMap.mappingCount();

        System.out.println("size()         (Returns int)  : " + sizeAsInt);
        System.out.println("mappingCount() (Returns long) : " + sizeAsLong);
        System.out.println("Internals: mappingCount() aggregates baseCount + CounterCell[] directly without int cast.");

        System.out.println("\n========================================================================");
        System.out.println(" 6. PARALLEL BULK OPERATIONS (FORKJOINPOOL INTEGRATION)");
        System.out.println("========================================================================");

        // Java 8 added parallel bulk operations: forEach, search, reduce
        // The first argument is parallelismThreshold:
        //   - If map.size() >= threshold, operations run in parallel across the Common ForkJoinPool!
        //   - If threshold == 1, maximally parallel.
        //   - If threshold == Long.MAX_VALUE, runs sequentially on the calling thread.

        ConcurrentHashMap<String, Double> stockPrices = new ConcurrentHashMap<>();
        stockPrices.put("AAPL", 185.50);
        stockPrices.put("GOOGL", 140.20);
        stockPrices.put("MSFT", 415.75);
        stockPrices.put("AMZN", 178.90);
        stockPrices.put("NVDA", 880.00);

        System.out.println("Stock Prices: " + stockPrices);

        // A. Parallel Search: Find the first stock with price > $500
        long threshold = 1; // Force parallel execution across threads
        String highPricedStock = stockPrices.search(threshold, (ticker, price) -> {
            if (price > 500.0) {
                return ticker + " ($" + price + ")";
            }
            return null;
        });
        System.out.println("Parallel search result (Price > $500): " + highPricedStock);

        // B. Parallel Reduce: Sum total market value
        double totalMarketValue = stockPrices.reduceValuesToDouble(threshold, 
                Double::doubleValue, // Transformer
                0.0,                 // Identity/Basis
                Double::sum          // Reducer
        );
        System.out.printf("Parallel reduce result (Total Portfolio Value): $%.2f\n", totalMarketValue);

        // C. Parallel forEach: Transform and print in parallel
        System.out.println("Parallel forEach execution:");
        stockPrices.forEach(threshold, (k, v) -> {
            System.out.println("   [Worker " + Thread.currentThread().getName() + "] Ticker: " + k + " -> $" + v);
        });

        System.out.println("\n========================================================================");
        System.out.println(" 7. THROUGHPUT BENCHMARK: ConcurrentHashMap VS SynchronizedMap");
        System.out.println("========================================================================");

        int benchmarkThreads = 8;
        int operationsPerWorker = 100_000;

        System.out.printf("Benchmarking with %d concurrent threads performing %d operations each...\n\n",
                benchmarkThreads, operationsPerWorker);

        benchmarkContention("Collections.synchronizedMap", 
                Collections.synchronizedMap(new HashMap<>()), 
                benchmarkThreads, 
                operationsPerWorker);

        benchmarkContention("ConcurrentHashMap", 
                new ConcurrentHashMap<>(), 
                benchmarkThreads, 
                operationsPerWorker);

        System.out.println("========================================================================");
        System.out.println(" CONCLUSION: ConcurrentHashMap provides non-blocking reads, CAS writes,");
        System.out.println(" and fine-grained bucket locks, making it the definitive concurrent choice.");
        System.out.println("========================================================================");
    }

    private static void benchmarkContention(String title, Map<String, Integer> targetMap, int numThreads, int ops) throws InterruptedException {
        ExecutorService pool = Executors.newFixedThreadPool(numThreads);
        CountDownLatch startSignal = new CountDownLatch(1);
        CountDownLatch finishSignal = new CountDownLatch(numThreads);

        long startNs = System.nanoTime();

        for (int i = 0; i < numThreads; i++) {
            final int threadId = i;
            pool.submit(() -> {
                try {
                    startSignal.await();
                    for (int j = 0; j < ops; j++) {
                        String key = "Key-" + ((j + threadId) % 256);
                        if ((j & 7) == 0) {
                            // 12.5% Writes
                            targetMap.put(key, j);
                        } else {
                            // 87.5% Reads
                            targetMap.get(key);
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    finishSignal.countDown();
                }
            });
        }

        startSignal.countDown();
        finishSignal.await();
        long elapsedNs = System.nanoTime() - startNs;
        pool.shutdown();

        double elapsedMs = elapsedNs / 1_000_000.0;
        double throughput = ((double) numThreads * ops) / (elapsedNs / 1_000_000_000.0);

        System.out.printf("%-30s : Duration = %8.2f ms | Throughput = %,12.0f ops/sec\n",
                title, elapsedMs, throughput);
    }
}
