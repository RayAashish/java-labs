package collections.queue.priorityqueue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Random;

/**
 * Top-K Elements Processing: Min-Heap O(N log K) vs. Full Sort O(N log N).
 *
 * <p>Why Min-Heap for Top-K Largest?
 * To maintain the K largest elements from a stream of size N:
 * - We maintain a Min-Heap of size K.
 * - The root (peek) of the min-heap holds the smallest of the K largest elements seen so far.
 * - When a new element arrives:
 *     - If heap.size() < K, insert directly.
 *     - If new element > min-heap.peek(), poll() the old minimum and insert the new element.
 * - Resulting time complexity: O(N log K), space complexity: O(K).
 * - For large N (e.g., 10,000,000) and small K (e.g., 10), O(N log K) runs orders of magnitude
 *   faster and avoids storing the entire dataset in heap memory.
 *
 * @author raysi
 */
public class TopKElementsHeapDemo {

    public static void main(String[] args) {
        System.out.println("==================================================================");
        System.out.println("      TOP-K FREQUENT / LARGEST ELEMENTS: MIN-HEAP DEMO            ");
        System.out.println("                         Author: raysi                            ");
        System.out.println("==================================================================\n");

        verifySmallScaleTopK();
        verifyStreamingTopK();
        runBenchmarkComparison();
    }

    /**
     * Demonstrates top-K logic on a small verifiable array.
     */
    private static void verifySmallScaleTopK() {
        System.out.println("--- 1. Small-Scale Top-K Largest Verification ---");
        int[] stream = {3, 2, 1, 5, 6, 4, 10, 8, 7, 9, 20, 15};
        int k = 4;

        List<Integer> topK = findTopKLargest(stream, k);
        System.out.println("Input stream: " + Arrays.toString(stream));
        System.out.println("Top " + k + " largest elements (ascending): " + topK);
        System.out.println();
    }

    /**
     * Demonstrates finding Top-K largest elements from a stream using a Min-Heap of size K.
     *
     * @param stream input array of integers
     * @param k number of top elements to retain
     * @return sorted list of top K elements
     */
    public static List<Integer> findTopKLargest(int[] stream, int k) {
        if (k <= 0) return Collections.emptyList();

        // Min-Heap of capacity k
        PriorityQueue<Integer> minHeap = new PriorityQueue<>(k);

        for (int val : stream) {
            if (minHeap.size() < k) {
                minHeap.offer(val);
            } else if (val > minHeap.peek()) {
                // Sift-down replacement: remove smallest candidate of the top K
                minHeap.poll();
                minHeap.offer(val);
            }
            // If val <= minHeap.peek(), discard immediately in O(1) time!
        }

        List<Integer> result = new ArrayList<>(minHeap);
        Collections.sort(result); // Sorting only K elements takes O(K log K)
        return result;
    }

    /**
     * Demonstrates streaming analytics: feeding elements continuously into an online
     * Top-K tracker and querying the running top-K at any point.
     */
    private static void verifyStreamingTopK() {
        System.out.println("--- 2. Online Streaming Top-K Tracker ---");
        OnlineTopKTracker tracker = new OnlineTopKTracker(3);

        int[] incomingTelemetry = {120, 45, 890, 230, 999, 15, 600, 1050, 400};
        for (int dataPoint : incomingTelemetry) {
            tracker.add(dataPoint);
            System.out.printf("Pushed: %4d -> Current Top 3: %s\n", dataPoint, tracker.getCurrentTopK());
        }
        System.out.println();
    }

    /**
     * Benchmarks O(N log K) Min-Heap vs O(N log N) Arrays.sort on 1,000,000 integers.
     */
    private static void runBenchmarkComparison() {
        System.out.println("--- 3. Performance Benchmark: Min-Heap O(N log K) vs Full Sort O(N log N) ---");
        int datasetSize = 1_000_000;
        int k = 10;
        System.out.printf("Dataset size: %,d elements | K = %d\n", datasetSize, k);

        int[] dataset = new Random(42).ints(datasetSize, 0, 100_000_000).toArray();

        // Benchmark 1: Min-Heap O(N log K)
        long startHeap = System.nanoTime();
        List<Integer> heapTopK = findTopKLargest(dataset, k);
        long endHeap = System.nanoTime();
        double heapTimeMs = (endHeap - startHeap) / 1_000_000.0;

        // Benchmark 2: Full Array Clone + Arrays.sort O(N log N)
        long startSort = System.nanoTime();
        int[] sortedDataset = dataset.clone();
        Arrays.sort(sortedDataset);
        List<Integer> sortTopK = new ArrayList<>();
        for (int i = sortedDataset.length - k; i < sortedDataset.length; i++) {
            sortTopK.add(sortedDataset[i]);
        }
        long endSort = System.nanoTime();
        double sortTimeMs = (endSort - startSort) / 1_000_000.0;

        System.out.printf("Min-Heap   Time: %8.2f ms | Result: %s\n", heapTimeMs, heapTopK);
        System.out.printf("Full Sort  Time: %8.2f ms | Result: %s\n", sortTimeMs, sortTopK);
        System.out.printf("Speedup Factor:  %8.2fx faster using Min-Heap!\n", sortTimeMs / heapTimeMs);
        System.out.println("Memory Savings:  Heap uses O(K) space (10 ints) vs Sort using O(N) space (1,000,000 ints clone)!\n");
    }

    /**
     * Stateful online tracker for streaming data feeds.
     */
    static class OnlineTopKTracker {
        private final int k;
        private final PriorityQueue<Integer> minHeap;

        public OnlineTopKTracker(int k) {
            this.k = k;
            this.minHeap = new PriorityQueue<>(k);
        }

        public void add(int value) {
            if (minHeap.size() < k) {
                minHeap.offer(value);
            } else if (value > minHeap.peek()) {
                minHeap.poll();
                minHeap.offer(value);
            }
        }

        public List<Integer> getCurrentTopK() {
            List<Integer> current = new ArrayList<>(minHeap);
            Collections.sort(current);
            return current;
        }
    }
}
