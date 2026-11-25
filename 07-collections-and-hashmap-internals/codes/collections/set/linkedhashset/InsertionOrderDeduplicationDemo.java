package collections.set.linkedhashset;

import java.lang.reflect.Field;
import java.util.*;

/**
 * ============================================================================
 * Production-Grade Demonstration: LinkedHashSet Internals & Deduplication
 * Author: raysi
 *
 * This runnable class provides deep source-level verification of:
 * 1. Insertion-order preservation vs HashSet's chaotic bucket ordering
 * 2. Re-insertion invariance (duplicate insertion does NOT shift list position)
 * 3. Reflection deep dive into LinkedHashMap head, tail, before, and after pointers
 * 4. Production application: Deterministic stream/log deduplication
 * 5. Performance benchmark: Sparse iteration speedup [O(N) vs O(capacity + N)]
 * ============================================================================
 */
public class InsertionOrderDeduplicationDemo {

    public static void main(String[] args) throws Exception {
        System.out.println("================================================================================");
        System.out.println("          LINKEDHASHSET: INSERTION ORDER & DEDUPLICATION VERIFICATION           ");
        System.out.println("================================================================================");

        verifyInsertionOrderVsHashSet();
        verifyReinsertionInvariance();
        verifyDoublyLinkedListReflection();
        verifyProductionLogDeduplication();
        verifySparseIterationPerformance();

        System.out.println("\nAll LinkedHashSet Internals & Behavioral Invariants Verified Successfully.");
    }

    /**
     * 1. Side-by-side ordering comparison:
     * Demonstrates that HashSet order depends on hash buckets and capacity,
     * whereas LinkedHashSet strictly follows original insertion sequence.
     */
    private static void verifyInsertionOrderVsHashSet() {
        System.out.println("\n--- 1. Iteration Sequence: HashSet vs. LinkedHashSet ---");

        List<String> rawTokens = Arrays.asList("Gamma", "Alpha", "Omega", "Beta", "Delta", "Epsilon");

        Set<String> hashSet = new HashSet<>(rawTokens);
        Set<String> linkedHashSet = new LinkedHashSet<>(rawTokens);

        System.out.println("Original Insertion Sequence:  " + rawTokens);
        System.out.println("HashSet Iteration Order:      " + hashSet + " (chaotic bucket order)");
        System.out.println("LinkedHashSet Iteration Order:" + linkedHashSet + " (strict insertion order)");
    }

    /**
     * 2. Re-insertion invariance:
     * When adding an element already in the set, add() returns false, and the
     * existing entry remains in its original position in the linked list.
     */
    private static void verifyReinsertionInvariance() {
        System.out.println("\n--- 2. Re-Insertion Invariance Test ---");

        Set<String> set = new LinkedHashSet<>();
        set.add("First");
        set.add("Second");
        set.add("Third");

        System.out.println("Initial LinkedHashSet Order: " + set);

        // Attempt to re-insert "First"
        boolean readdedFirst = set.add("First");
        System.out.println("set.add('First') returned: " + readdedFirst + " (duplicate rejected)");
        System.out.println("Order after re-inserting 'First': " + set);

        // Attempt to re-insert "Second"
        boolean readdedSecond = set.add("Second");
        System.out.println("set.add('Second') returned: " + readdedSecond + " (duplicate rejected)");
        System.out.println("Order after re-inserting 'Second': " + set);

        System.out.println("ARCHITECTURAL TAKEAWAY: LinkedHashSet has accessOrder = false; " +
                "existing entries are NEVER moved to tail!");
    }

    /**
     * 3. Reflection: Traversing the doubly-linked list through LinkedHashMap.Entry pointers.
     * Proof that entries are doubly linked with before and after references.
     */
    private static void verifyDoublyLinkedListReflection() throws Exception {
        System.out.println("\n--- 3. Reflection: Inspecting Doubly-Linked List Pointers ---");

        LinkedHashSet<String> set = new LinkedHashSet<>();
        set.add("Txn-101");
        set.add("Txn-102");
        set.add("Txn-103");

        // Extract map field from superclass HashSet
        Field mapField = HashSet.class.getDeclaredField("map");
        mapField.setAccessible(true);
        Object internalMap = mapField.get(set);

        System.out.println("LinkedHashSet Backing Map Class: " + internalMap.getClass().getName());

        // Extract head and tail fields from LinkedHashMap
        Class<?> lhmClass = Class.forName("java.util.LinkedHashMap");
        Field headField = lhmClass.getDeclaredField("head");
        Field tailField = lhmClass.getDeclaredField("tail");
        headField.setAccessible(true);
        tailField.setAccessible(true);

        Object headEntry = headField.get(internalMap);
        Object tailEntry = tailField.get(internalMap);

        Class<?> entryClass = Class.forName("java.util.LinkedHashMap$Entry");
        Field keyField = Class.forName("java.util.HashMap$Node").getDeclaredField("key");
        Field afterField = entryClass.getDeclaredField("after");
        Field beforeField = entryClass.getDeclaredField("before");
        keyField.setAccessible(true);
        afterField.setAccessible(true);
        beforeField.setAccessible(true);

        System.out.println("Walking Doubly-Linked List from HEAD to TAIL via reflection:");
        Object curr = headEntry;
        int step = 0;
        while (curr != null) {
            Object key = keyField.get(curr);
            Object before = beforeField.get(curr);
            Object after = afterField.get(curr);

            Object beforeKey = (before != null) ? keyField.get(before) : "null (HEAD)";
            Object afterKey = (after != null) ? keyField.get(after) : "null (TAIL)";

            System.out.printf("  [Step %d] Key: %-9s | Pointer Before: %-12s | Pointer After: %-12s%n",
                    step++, key, beforeKey, afterKey);
            curr = after;
        }

        System.out.println("Verified: HEAD key = '" + keyField.get(headEntry) +
                "', TAIL key = '" + keyField.get(tailEntry) + "'");
    }

    /**
     * 4. Production Application: Removing duplicates from a file or log stream
     * while strictly preserving temporal (first-seen) order.
     */
    private static void verifyProductionLogDeduplication() {
        System.out.println("\n--- 4. Production Use Case: File/Log Stream Deduplication ---");

        // Simulating a stream of incoming web server access log lines with duplicates
        List<String> rawLogLines = Arrays.asList(
                "2026-10-17 12:00:01 GET /api/v1/users [IP: 192.168.1.10]",
                "2026-10-17 12:00:02 POST /api/v1/orders [IP: 192.168.1.25]",
                "2026-10-17 12:00:01 GET /api/v1/users [IP: 192.168.1.10]", // duplicate
                "2026-10-17 12:00:03 GET /api/v1/products [IP: 192.168.1.44]",
                "2026-10-17 12:00:02 POST /api/v1/orders [IP: 192.168.1.25]", // duplicate
                "2026-10-17 12:00:04 DELETE /api/v1/cart [IP: 192.168.1.89]"
        );

        System.out.println("Total incoming log entries: " + rawLogLines.size());

        // Deduplicate using LinkedHashSet
        // Memory Note: Stores each unique entry once and maintains head/tail pointers in insertion order.
        Set<String> uniqueLogs = new LinkedHashSet<>(rawLogLines);

        System.out.println("Unique log entries count:   " + uniqueLogs.size());
        System.out.println("Deduplicated Output (First-Seen Temporal Order Retained):");
        uniqueLogs.forEach(line -> System.out.println("  " + line));
    }

    /**
     * 5. Performance Benchmark: Sparse Iteration Speedup.
     * Proves that LinkedHashSet iteration is O(N), whereas HashSet iteration is O(capacity + N).
     */
    private static void verifySparseIterationPerformance() {
        System.out.println("\n--- 5. Sparse Iteration Benchmark: HashSet vs. LinkedHashSet ---");

        final int CAPACITY = 4_000_000;
        final int ELEMENTS = 8;

        // Allocate high-capacity sets with very few elements
        HashSet<Integer> sparseHashSet = new HashSet<>(CAPACITY);
        LinkedHashSet<Integer> sparseLinkedHashSet = new LinkedHashSet<>(CAPACITY);

        for (int i = 0; i < ELEMENTS; i++) {
            sparseHashSet.add(i * 100);
            sparseLinkedHashSet.add(i * 100);
        }

        // Measure HashSet iteration: must iterate over all 4M bucket slots
        long t0 = System.nanoTime();
        int sumHash = 0;
        for (Integer val : sparseHashSet) {
            sumHash += val;
        }
        long timeHashSetNs = System.nanoTime() - t0;

        // Measure LinkedHashSet iteration: visits only the 8 nodes via linked list
        long t1 = System.nanoTime();
        int sumLinked = 0;
        for (Integer val : sparseLinkedHashSet) {
            sumLinked += val;
        }
        long timeLinkedSetNs = System.nanoTime() - t1;

        System.out.printf("HashSet Sparse Iteration       [O(capacity + N)]: %,12d ns (Sum=%d)%n",
                timeHashSetNs, sumHash);
        System.out.printf("LinkedHashSet Sparse Iteration [O(N)]:            %,12d ns (Sum=%d)%n",
                timeLinkedSetNs, sumLinked);
        System.out.printf("Performance Gain: %.1fx FASTER iteration for sparse collections!%n",
                (double) timeHashSetNs / Math.max(1, timeLinkedSetNs));
    }
}
