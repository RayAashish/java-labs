package collections.list.linkedlist;

import java.util.*;

/**
 * Microbenchmark contrasting ArrayList vs LinkedList across critical operations:
 * 1. Sequential Append (add at tail)
 * 2. Random Indexed Read (get)
 * 3. Prepend at Head (add at index 0)
 * 4. Insertion at Middle (add at size / 2)
 * 5. Full Sequential Iteration
 *
 * Microarchitectural Context:
 * - CPU Cache Line Locality: Modern CPUs fetch 64-byte blocks. ArrayList's contiguous Object[]
 *   achieves high L1 cache hit ratios and vectorized System.arraycopy speedups.
 * - LinkedList creates a distinct 24-byte Node for each element, leading to pointer chasing,
 *   frequent cache misses, and GC Young Gen churn.
 *
 * @author raysi
 */
public class ArrayListVsLinkedListBenchmark {

    private static final int WARMUP_ITERATIONS = 5;
    private static final int OP_COUNT = 100_000;
    private static final int MID_INSERT_COUNT = 10_000;

    public static void main(String[] args) {
        System.out.println("===============================================================");
        System.out.println("   ARRAYLIST VS LINKEDLIST: ARCHITECTURAL BENCHMARK            ");
        System.out.println("   Author: raysi                                              ");
        System.out.println("===============================================================\n");

        runWarmup();

        System.out.printf("Running benchmarks with %,d operations per test...\n\n", OP_COUNT);

        benchmarkSequentialAppend();
        benchmarkRandomRead();
        benchmarkPrependAtHead();
        benchmarkInsertAtMiddle();
        benchmarkSequentialIteration();

        printArchitecturalSummary();
    }

    private static void runWarmup() {
        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            ArrayList<Integer> al = new ArrayList<>();
            LinkedList<Integer> ll = new LinkedList<>();
            for (int j = 0; j < 5_000; j++) {
                al.add(j);
                ll.add(j);
            }
        }
    }

    private static void benchmarkSequentialAppend() {
        System.out.println("--- 1. Sequential Append (add at end) ---");

        long start = System.nanoTime();
        ArrayList<Integer> arrayList = new ArrayList<>();
        for (int i = 0; i < OP_COUNT; i++) {
            arrayList.add(i);
        }
        long alDuration = System.nanoTime() - start;

        start = System.nanoTime();
        LinkedList<Integer> linkedList = new LinkedList<>();
        for (int i = 0; i < OP_COUNT; i++) {
            linkedList.add(i);
        }
        long llDuration = System.nanoTime() - start;

        printComparison("Sequential Append", alDuration, llDuration);
        System.out.println("Insight: ArrayList wins because it amortizes resizes and avoids allocating 100,000 Node objects.\n");
    }

    private static void benchmarkRandomRead() {
        System.out.println("--- 2. Random Indexed Read (get) ---");
        ArrayList<Integer> arrayList = new ArrayList<>(OP_COUNT);
        LinkedList<Integer> linkedList = new LinkedList<>();
        for (int i = 0; i < OP_COUNT; i++) {
            arrayList.add(i);
            linkedList.add(i);
        }

        Random random = new Random(42);
        int[] indices = new int[10_000];
        for (int i = 0; i < indices.length; i++) {
            indices[i] = random.nextInt(OP_COUNT);
        }

        long start = System.nanoTime();
        long alSum = 0;
        for (int idx : indices) {
            alSum += arrayList.get(idx);
        }
        long alDuration = System.nanoTime() - start;

        start = System.nanoTime();
        long llSum = 0;
        for (int idx : indices) {
            llSum += linkedList.get(idx);
        }
        long llDuration = System.nanoTime() - start;

        printComparison("10,000 Random Reads", alDuration, llDuration);
        System.out.println("Insight: ArrayList does direct memory offset O(1). LinkedList walks pointers O(n) per get.\n");
    }

    private static void benchmarkPrependAtHead() {
        System.out.println("--- 3. Prepend at Head (add at index 0) ---");
        int headCount = 20_000;

        long start = System.nanoTime();
        ArrayList<Integer> arrayList = new ArrayList<>();
        for (int i = 0; i < headCount; i++) {
            arrayList.add(0, i);
        }
        long alDuration = System.nanoTime() - start;

        start = System.nanoTime();
        LinkedList<Integer> linkedList = new LinkedList<>();
        for (int i = 0; i < headCount; i++) {
            linkedList.add(0, i);
        }
        long llDuration = System.nanoTime() - start;

        printComparison("Prepend 20,000 at Head", alDuration, llDuration);
        System.out.println("Insight: LinkedList wins here! O(1) linkFirst vs ArrayList's O(n) System.arraycopy memory shifts.\n");
    }

    private static void benchmarkInsertAtMiddle() {
        System.out.println("--- 4. Insert at Middle (add at size / 2) ---");
        ArrayList<Integer> arrayList = new ArrayList<>();
        LinkedList<Integer> linkedList = new LinkedList<>();
        for (int i = 0; i < 5_000; i++) {
            arrayList.add(i);
            linkedList.add(i);
        }

        long start = System.nanoTime();
        for (int i = 0; i < MID_INSERT_COUNT; i++) {
            arrayList.add(arrayList.size() / 2, i);
        }
        long alDuration = System.nanoTime() - start;

        start = System.nanoTime();
        for (int i = 0; i < MID_INSERT_COUNT; i++) {
            linkedList.add(linkedList.size() / 2, i);
        }
        long llDuration = System.nanoTime() - start;

        printComparison("10,000 Midpoint Insertions", alDuration, llDuration);
        System.out.println("Insight: Despite array shifts, ArrayList beats or rivals LinkedList because LinkedList must walk");
        System.out.println("thousands of scattered node pointers to find the midpoint before inserting!\n");
    }

    private static void benchmarkSequentialIteration() {
        System.out.println("--- 5. Full Sequential Iteration ---");
        ArrayList<Integer> arrayList = new ArrayList<>(OP_COUNT);
        LinkedList<Integer> linkedList = new LinkedList<>();
        for (int i = 0; i < OP_COUNT; i++) {
            arrayList.add(i);
            linkedList.add(i);
        }

        long start = System.nanoTime();
        long alSum = 0;
        for (int v : arrayList) {
            alSum += v;
        }
        long alDuration = System.nanoTime() - start;

        start = System.nanoTime();
        long llSum = 0;
        for (int v : linkedList) {
            llSum += v;
        }
        long llDuration = System.nanoTime() - start;

        printComparison("Full Scan Iteration", alDuration, llDuration);
        System.out.println("Insight: ArrayList leverages hardware prefetchers and contiguous cache lines (64 bytes).");
        System.out.println("LinkedList incurs pointer dereference cache stalls on each node jump.\n");
    }

    private static void printComparison(String opName, long alNanos, long llNanos) {
        System.out.printf("%-30s | ArrayList: %,12d ns | LinkedList: %,12d ns | Winner: %s\n",
                opName, alNanos, llNanos,
                (alNanos < llNanos ? String.format("ArrayList (%.1fx faster)", (double) llNanos / Math.max(1, alNanos))
                        : String.format("LinkedList (%.1fx faster)", (double) alNanos / Math.max(1, llNanos))));
    }

    private static void printArchitecturalSummary() {
        System.out.println("===============================================================");
        System.out.println("   ARCHITECTURAL VERDICT FOR HIGH-STAKES INTERVIEWS           ");
        System.out.println("===============================================================");
        System.out.println("1. ArrayList is the default List choice across virtually all JVM backend applications.");
        System.out.println("2. LinkedList is almost NEVER the right choice, even when insertions at the head are needed;");
        System.out.println("   ArrayDeque offers superior cache locality and zero node allocation overhead.");
        System.out.println("3. Theory (Big-O) often ignores microarchitectural reality: cache lines, pointer chasing,");
        System.out.println("   and garbage collector memory footprints dominate algorithmic runtimes in production.");
        System.out.println("===============================================================\n");
    }
}
