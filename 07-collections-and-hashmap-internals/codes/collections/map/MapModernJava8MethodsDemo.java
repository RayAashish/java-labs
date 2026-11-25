package collections.map;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ============================================================================
 * Java Collections Framework Mastery — Map Modern Methods Deep Dive
 * ============================================================================
 *
 * Demonstrates the internal mechanics, memory implications, and non-obvious
 * traps of Java 8+ functional map methods:
 *  1. {@link Map#compute(Object, java.util.function.BiFunction)}
 *  2. {@link Map#computeIfAbsent(Object, java.util.function.Function)}
 *  3. {@link Map#computeIfPresent(Object, java.util.function.BiFunction)}
 *  4. {@link Map#merge(Object, Object, java.util.function.BiFunction)}
 *  5. {@link Map#getOrDefault(Object, Object)}
 *  6. Backed Collection Views and in-place mutation
 *  7. {@link Map#of()} immutable collections vs {@link HashMap}
 *
 * @author raysi
 */
public class MapModernJava8MethodsDemo {

    public static void main(String[] args) {
        System.out.println("===============================================================");
        System.out.println("   JAVA 8+ MAP API INTERNALS & EDGE CASES DEMONSTRATION        ");
        System.out.println("   Author: raysi                                               ");
        System.out.println("===============================================================\n");

        demonstrateCollectionViewsMutation();
        demonstrateWordFrequencyMerge();
        demonstrateComputeIfAbsentCaching();
        demonstrateComputeAndNullEvictionTrap();
        demonstrateReplaceAllAndForEach();
        demonstrateImmutableMapOfVsHashMap();
    }

    /**
     * Demonstrates that keySet(), values(), and entrySet() are NOT copies;
     * they are live backed views over the underlying map table.
     */
    private static void demonstrateCollectionViewsMutation() {
        System.out.println("--- 1. Backed Collection Views & Live Mutation ---");
        Map<String, Integer> map = new HashMap<>();
        map.put("Alpha", 100);
        map.put("Beta", 200);
        map.put("Gamma", 300);

        Set<String> keySet = map.keySet();
        Collection<Integer> values = map.values();
        Set<Map.Entry<String, Integer>> entrySet = map.entrySet();

        System.out.println("Initial Map: " + map);

        // Mutating via keySet removes from map!
        System.out.println("[Action] Removing 'Alpha' via keySet.remove(\"Alpha\")...");
        keySet.remove("Alpha");
        System.out.println("Map after keySet removal: " + map + " (Size: " + map.size() + ")");

        // Mutating via values view removes the matching entry!
        System.out.println("[Action] Removing value '200' via values.remove(200)...");
        values.remove(200);
        System.out.println("Map after values removal: " + map + " (Size: " + map.size() + ")");

        // Attempting to add via keySet view triggers UnsupportedOperationException
        try {
            System.out.println("[Action] Attempting keySet.add(\"Delta\")...");
            keySet.add("Delta");
        } catch (UnsupportedOperationException e) {
            System.out.println("[Expected Exception] keySet.add() failed: " + e.getClass().getSimpleName()
                    + " (A view cannot synthesize a value for a new key!)");
        }
        System.out.println();
    }

    /**
     * Demonstrates Map.merge() for lock-free / clean aggregation (e.g. Word Frequency).
     * Internal logic:
     * - If key absent -> map.put(key, value)
     * - If key present -> map.put(key, remappingFunction.apply(oldVal, value))
     */
    private static void demonstrateWordFrequencyMerge() {
        System.out.println("--- 2. High-Performance Word Frequency with Map.merge() ---");
        String text = "jvm gc heap stack jvm memory heap jvm garbage gc";
        String[] tokens = text.split("\\s+");

        Map<String, Integer> freqMap = new HashMap<>();
        for (String token : tokens) {
            // Memory Note: Integer.valueOf(1) uses the Integer cache (-128 to 127),
            // avoiding object allocation during initial insertion!
            freqMap.merge(token, 1, Integer::sum);
        }

        System.out.println("Token counts: " + freqMap);
        System.out.println("Count for 'jvm': " + freqMap.getOrDefault("jvm", 0));
        System.out.println("Count for 'missing': " + freqMap.getOrDefault("missing", 0));
        System.out.println();
    }

    /**
     * Demonstrates thread-safe / clean memoization caching via computeIfAbsent().
     * Trap: Never perform long blocking I/O inside the lambda, especially on ConcurrentHashMap,
     * as it locks the bucket bin!
     */
    private static void demonstrateComputeIfAbsentCaching() {
        System.out.println("--- 3. Lazy Caching Pattern with computeIfAbsent() ---");
        Map<Integer, String> fibCache = new HashMap<>();

        // Multi-level memoization calculation
        for (int i = 1; i <= 5; i++) {
            final int n = i;
            String result = fibCache.computeIfAbsent(n, key -> {
                System.out.println("   [Cache Miss] Computing factorial for key: " + key);
                return String.valueOf(factorial(key));
            });
            System.out.println("   FibCache.get(" + n + ") = " + result);
        }

        // Second access hits cache without lambda invocation
        System.out.println("[Cache Hit Test] Accessing key 3 again:");
        fibCache.computeIfAbsent(3, k -> {
            throw new AssertionError("Must not be invoked on cache hit!");
        });
        System.out.println("   Retrieved value: " + fibCache.get(3));
        System.out.println();
    }

    private static long factorial(int n) {
        long result = 1;
        for (int i = 2; i <= n; i++) result *= i;
        return result;
    }

    /**
     * Demonstrates compute(), computeIfPresent(), and the "Silent Removal Trap".
     * If the lambda returns null, the key is pruned from the map!
     */
    private static void demonstrateComputeAndNullEvictionTrap() {
        System.out.println("--- 4. The Silent Removal Trap in compute() & merge() ---");
        Map<String, Integer> inventory = new HashMap<>();
        inventory.put("Apples", 10);
        inventory.put("Oranges", 5);

        System.out.println("Initial Inventory: " + inventory);

        // Standard decrement
        inventory.computeIfPresent("Apples", (k, v) -> v - 2);
        System.out.println("After selling 2 Apples: " + inventory);

        // THE TRAP: If remapping returns null, the key is removed from the map!
        System.out.println("[Action] Selling remaining 5 Oranges (lambda returns null when stock reaches 0)...");
        inventory.computeIfPresent("Oranges", (k, v) -> {
            int remaining = v - 5;
            return remaining <= 0 ? null : remaining; // Returning null evicts the key!
        });
        System.out.println("Inventory after stock depletion: " + inventory);
        System.out.println("Contains 'Oranges'? " + inventory.containsKey("Oranges") + " (Silently removed!)");

        // Merge behaves identically when remapping function returns null
        inventory.put("Bananas", 1);
        System.out.println("Bananas before merge null return: " + inventory);
        inventory.merge("Bananas", 1, (oldV, newV) -> null);
        System.out.println("Bananas after merge returning null: " + inventory + " (Pruned from table!)");
        System.out.println();
    }

    /**
     * Demonstrates bulk functional transformations with replaceAll() and forEach().
     */
    private static void demonstrateReplaceAllAndForEach() {
        System.out.println("--- 5. Bulk In-Place Transformations via replaceAll() ---");
        Map<String, Double> priceCatalog = new HashMap<>();
        priceCatalog.put("CPU", 300.0);
        priceCatalog.put("GPU", 800.0);
        priceCatalog.put("RAM", 120.0);

        System.out.println("Original Catalog: " + priceCatalog);

        // Apply 10% tax in-place across all entries
        priceCatalog.replaceAll((item, price) -> Math.round(price * 1.10 * 100.0) / 100.0);
        System.out.println("Catalog after 10% tax: " + priceCatalog);

        System.out.println("Iterating with forEach():");
        priceCatalog.forEach((item, price) ->
                System.out.printf("   - %s: $%.2f%n", item, price));
        System.out.println();
    }

    /**
     * Highlights deep differences between mutable HashMap and immutable Map.of() / Map.copyOf():
     *  - Heap density (no Entry node overhead for small maps)
     *  - Null rejection
     *  - Duplicate key rejection during construction
     *  - Immutability enforcement
     */
    private static void demonstrateImmutableMapOfVsHashMap() {
        System.out.println("--- 6. Map.of() vs HashMap Deep Divergence ---");

        // 1. Null Rejection in Java 9+ Immutable Map
        try {
            System.out.println("[Action] Attempting Map.of(\"Key\", null)...");
            Map.of("Key", null);
        } catch (NullPointerException e) {
            System.out.println("[Expected Exception] Map.of() is strictly null-hostile: " + e);
        }

        // 2. Duplicate Key Rejection during construction
        try {
            System.out.println("[Action] Attempting Map.of(\"K1\", 1, \"K1\", 2)...");
            Map.of("K1", 1, "K1", 2);
        } catch (IllegalArgumentException e) {
            System.out.println("[Expected Exception] Map.of() rejects duplicate keys at construction: " + e.getMessage());
        }

        // 3. Immutability
        Map<String, String> immutableMap = Map.of("ENV", "PROD", "REGION", "US-EAST-1");
        System.out.println("Immutable Map Class: " + immutableMap.getClass().getName());
        try {
            System.out.println("[Action] Attempting immutableMap.put(\"VERSION\", \"1.0\")...");
            immutableMap.put("VERSION", "1.0");
        } catch (UnsupportedOperationException e) {
            System.out.println("[Expected Exception] Mutation blocked: " + e.getClass().getSimpleName());
        }

        // 4. Memory Density & Optimization
        // Map.of() with <= 2 entries uses specialized classes (Map0, Map1, Map2)
        // storing fields directly in the object, avoiding ANY internal node or array allocations!
        Map<String, String> singleEntry = Map.of("SingletonKey", "SingletonValue");
        System.out.println("Single Entry Map Class: " + singleEntry.getClass().getName()
                + " (Zero internal table array, minimal heap footprint!)");
        System.out.println();
    }
}
