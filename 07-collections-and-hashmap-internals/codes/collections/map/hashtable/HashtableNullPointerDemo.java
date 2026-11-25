package collections.map.hashtable;

import java.util.*;
import java.util.concurrent.*;

/**
 * Production-Grade Demonstration of Hashtable Legacy Mechanics & Failure Modes.
 * 
 * Explores:
 *  1. Null Key & Null Value Rejection (Exact failure points in JDK source code)
 *  2. Direct contrast with HashMap's null-key handling (bucket 0)
 *  3. Fail-Fast Iterator vs Legacy Non-Fail-Fast Enumeration
 *  4. The Legacy contains() vs containsKey() Trap
 *  5. Multithreaded Contention Benchmark:
 *     Hashtable vs Collections.synchronizedMap() vs ConcurrentHashMap
 *  6. Compound Operation Race Conditions in Synchronized Maps
 * 
 * @author raysi
 */
public class HashtableNullPointerDemo {

    public static void main(String[] args) throws InterruptedException {
        System.out.println("========================================================================");
        System.out.println(" 1. NULL KEY & NULL VALUE REJECTION IN HASHTABLE VS HASHMAP");
        System.out.println("========================================================================");

        Hashtable<String, String> hashtable = new Hashtable<>();
        Map<String, String> hashmap = new HashMap<>();

        // A. Demonstrating Null Key in Hashtable
        // JDK Source: int hash = key.hashCode(); -> Unchecked call throws NPE immediately
        try {
            System.out.println("Attempting hashtable.put(null, \"Value\")...");
            hashtable.put(null, "Value");
        } catch (NullPointerException e) {
            System.out.println("-> CAUGHT NullPointerException: Hashtable rejects null key during key.hashCode()!");
        }

        // B. Demonstrating Null Value in Hashtable
        // JDK Source: if (value == null) throw new NullPointerException();
        try {
            System.out.println("Attempting hashtable.put(\"Key\", null)...");
            hashtable.put("Key", null);
        } catch (NullPointerException e) {
            System.out.println("-> CAUGHT NullPointerException: Hashtable explicitly checks 'if (value == null)'!");
        }

        // C. Demonstrating HashMap Allowing Both
        // JDK Source: (key == null) ? 0 : (h = key.hashCode()) ^ (h >>> 16)
        // Null key is placed explicitly into bucket table[0]
        hashmap.put(null, "HashMapAllowsNullKey");
        hashmap.put("KeyWithNullValue", null);
        hashmap.put(null, "OverwrittenNullKeyVal");

        System.out.println("\nHashMap null support verification:");
        System.out.println("hashmap.get(null)                 : " + hashmap.get(null));
        System.out.println("hashmap.get(\"KeyWithNullValue\")     : " + hashmap.get("KeyWithNullValue"));
        System.out.println("hashmap.containsKey(null)         : " + hashmap.containsKey(null));
        System.out.println("hashmap.containsKey(\"KeyWithNull\"): " + hashmap.containsKey("KeyWithNullValue"));

        System.out.println("\n========================================================================");
        System.out.println(" 2. LEGACY ENUMERATION VS MODERN ITERATOR: FAIL-FAST BEHAVIOR");
        System.out.println("========================================================================");

        Hashtable<Integer, String> testTable = new Hashtable<>();
        for (int i = 1; i <= 5; i++) {
            testTable.put(i, "Value-" + i);
        }

        // Modern Iterator (Fail-Fast)
        System.out.println("A. Testing Modern Iterator with concurrent structural mutation:");
        Iterator<Integer> iterator = testTable.keySet().iterator();
        try {
            while (iterator.hasNext()) {
                Integer key = iterator.next();
                if (key.equals(2)) {
                    // Mutate map structurally while iterating
                    testTable.put(99, "Mutated-Value");
                }
            }
        } catch (ConcurrentModificationException e) {
            System.out.println("-> CAUGHT ConcurrentModificationException! Modern Iterator tracks modCount.");
        }

        // Clean up mutation
        testTable.remove(99);

        // Legacy Enumeration (NOT Fail-Fast)
        System.out.println("\nB. Testing Legacy Enumeration with concurrent structural mutation:");
        Enumeration<String> enumeration = testTable.elements();
        boolean mutated = false;
        int elementsSeen = 0;
        while (enumeration.hasMoreElements()) {
            String val = enumeration.nextElement();
            elementsSeen++;
            if (!mutated) {
                // Mutate table during traversal!
                testTable.put(100, "Injected-During-Enum");
                mutated = true;
            }
        }
        System.out.println("-> Legacy Enumeration completed WITHOUT ConcurrentModificationException!");
        System.out.println("   Total elements seen during enumeration: " + elementsSeen);
        System.out.println("   Warning: Enumeration may skip entries, loop infinitely, or read stale data.");

        System.out.println("\n========================================================================");
        System.out.println(" 3. THE INFAMOUS contains() METHOD HAZARD");
        System.out.println("========================================================================");

        Hashtable<String, String> userRoles = new Hashtable<>();
        userRoles.put("alice", "ADMIN");
        userRoles.put("bob", "GUEST");

        // Subtle Bug: table.contains(target) searches VALUES, not KEYS!
        System.out.println("userRoles.contains(\"alice\") [Checking if 'alice' exists]: " + userRoles.contains("alice"));
        System.out.println("userRoles.contains(\"ADMIN\") [Searching for value 'ADMIN'] : " + userRoles.contains("ADMIN"));
        System.out.println("userRoles.containsKey(\"alice\") [Correct Key Lookup]       : " + userRoles.containsKey("alice"));

        System.out.println("\n========================================================================");
        System.out.println(" 4. COMPOUND OPERATION RACE CONDITION DEMONSTRATION");
        System.out.println("========================================================================");

        // Hashtable methods are synchronized individually, but compound operations ARE NOT ATOMIC!
        Hashtable<String, Integer> counterTable = new Hashtable<>();
        counterTable.put("hits", 0);

        int threadCount = 10;
        int incrementsPerThread = 1000;
        ExecutorService raceExecutor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch endGate = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            raceExecutor.submit(() -> {
                try {
                    startGate.await();
                    for (int j = 0; j < incrementsPerThread; j++) {
                        // RACE CONDITION: Read-Modify-Write is NOT synchronized as a whole!
                        int current = counterTable.get("hits");
                        // Another thread can run get() right here before put() executes!
                        counterTable.put("hits", current + 1);
                    }
                } catch (InterruptedException ignored) {
                } finally {
                    endGate.countDown();
                }
            });
        }

        startGate.countDown();
        endGate.await();
        raceExecutor.shutdown();

        int expectedHits = threadCount * incrementsPerThread;
        int actualHits = counterTable.get("hits");
        System.out.println("Expected Hits (Compound Operation): " + expectedHits);
        System.out.println("Actual Hits (Hashtable without client locking): " + actualHits);
        System.out.println("Lost Updates: " + (expectedHits - actualHits) + " -> Proves Hashtable methods don't compose atomically!");

        System.out.println("\n========================================================================");
        System.out.println(" 5. PERFORMANCE BENCHMARK: CONTENTION UNDER MULTIPLE THREADS");
        System.out.println("========================================================================");

        // Compare throughput under contention:
        // 1. Hashtable (Monolithic this lock)
        // 2. Collections.synchronizedMap(new HashMap<>()) (Monolithic mutex lock)
        // 3. ConcurrentHashMap (Lock-free reads, CAS + Bucket Lock)

        int benchmarkThreads = 8;
        int operationsPerThread = 50_000;

        System.out.printf("Running benchmark with %d threads, %d read/write ops per thread...\n\n",
                benchmarkThreads, operationsPerThread);

        benchmarkMap("1. Hashtable", new Hashtable<>(), benchmarkThreads, operationsPerThread);
        benchmarkMap("2. Collections.synchronizedMap", Collections.synchronizedMap(new HashMap<>()), benchmarkThreads, operationsPerThread);
        benchmarkMap("3. ConcurrentHashMap", new ConcurrentHashMap<>(), benchmarkThreads, operationsPerThread);

        System.out.println("========================================================================");
        System.out.println(" SUMMARY: ConcurrentHashMap eliminates lock contention through fine-grained");
        System.out.println(" bucket locks and lock-free reads, completely outperforming Hashtable.");
        System.out.println("========================================================================");
    }

    private static void benchmarkMap(String name, Map<String, Integer> map, int threadCount, int ops) throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch finishLatch = new CountDownLatch(threadCount);

        long start = System.nanoTime();

        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            executor.submit(() -> {
                try {
                    startLatch.await();
                    for (int j = 0; j < ops; j++) {
                        String key = "Key-" + ((j + threadId) % 100);
                        if (j % 5 == 0) {
                            // 20% Writes
                            map.put(key, j);
                        } else {
                            // 80% Reads
                            map.get(key);
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    finishLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        finishLatch.await();
        long duration = System.nanoTime() - start;
        executor.shutdown();

        double millis = duration / 1_000_000.0;
        System.out.printf("%-32s : Completed in %8.2f ms (Throughput: %,.0f ops/sec)\n",
                name, millis, ((double) threadCount * ops / (duration / 1_000_000_000.0)));
    }
}
