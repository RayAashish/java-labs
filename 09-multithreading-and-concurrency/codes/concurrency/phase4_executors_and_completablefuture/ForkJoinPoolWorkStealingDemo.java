package concurrency.phase4_executors_and_completablefuture;

import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveTask;

/**
 * Author: raysi
 * Demonstrates:
 * 1. ForkJoinPool Architecture & Work-Stealing Algorithm.
 * 2. Deque mechanics: LIFO access by the worker thread (tail), FIFO stealing by idle peers (head).
 * 3. RecursiveTask divide-and-conquer parallel array processing.
 */
public class ForkJoinPoolWorkStealingDemo {

    private static final int ARRAY_SIZE = 50_000_000;
    private static final int SEQUENTIAL_THRESHOLD = 500_000;

    public static void main(String[] args) {
        System.out.println("======================================================================");
        System.out.println("  PHASE 4 LAB: FORKJOINPOOL & WORK-STEALING ALGORITHM");
        System.out.println("  Author: raysi | Package: com.raysi.concurrency");
        System.out.println("======================================================================\n");

        System.out.printf("Initializing array of %d elements...%n", ARRAY_SIZE);
        long[] numbers = new long[ARRAY_SIZE];
        for (int i = 0; i < ARRAY_SIZE; i++) {
            numbers[i] = i + 1;
        }

        // 1. Sequential Benchmark
        long startSeq = System.currentTimeMillis();
        long sequentialSum = 0;
        for (int i = 0; i < ARRAY_SIZE; i++) {
            sequentialSum += numbers[i];
        }
        long seqDuration = System.currentTimeMillis() - startSeq;
        System.out.printf("Sequential Sum       : %d in %d ms%n", sequentialSum, seqDuration);

        // 2. Parallel Work-Stealing ForkJoinPool
        ForkJoinPool customPool = new ForkJoinPool(Runtime.getRuntime().availableProcessors());
        System.out.printf("Created ForkJoinPool with Parallelism: %d workers%n", customPool.getParallelism());

        long startFjp = System.currentTimeMillis();
        ParallelSumTask rootTask = new ParallelSumTask(numbers, 0, numbers.length);
        long fjpSum = customPool.invoke(rootTask);
        long fjpDuration = System.currentTimeMillis() - startFjp;

        System.out.printf("ForkJoinPool Sum     : %d in %d ms%n", fjpSum, fjpDuration);
        System.out.printf("Parallel Speedup: %.2fx faster!%n", (double) seqDuration / fjpDuration);

        customPool.shutdown();

        System.out.println("\n[Key Takeaway for Interviews]:");
        System.out.println("  - ForkJoinPool uses Work-Stealing to prevent worker starvation.");
        System.out.println("  - Each worker has its own Deque: owner pushes/pops from TAIL (LIFO, cache-friendly);");
        System.out.println("    thieves steal from HEAD (FIFO, largest un-split chunk).");
        System.out.println("  - Crucial Rule: Always fork() subtask 1 and invoke compute() on subtask 2 to reuse");
        System.out.println("    the current thread, rather than calling fork() on both!");
    }

    static class ParallelSumTask extends RecursiveTask<Long> {
        private final long[] array;
        private final int start;
        private final int end;

        public ParallelSumTask(long[] array, int start, int end) {
            this.array = array;
            this.start = start;
            this.end = end;
        }

        @Override
        protected Long compute() {
            int length = end - start;
            if (length <= SEQUENTIAL_THRESHOLD) {
                // Base case: execute sequentially when below threshold
                long sum = 0;
                for (int i = start; i < end; i++) {
                    sum += array[i];
                }
                return sum;
            }

            // Divide-and-Conquer: split task in half
            int mid = start + length / 2;
            ParallelSumTask leftTask = new ParallelSumTask(array, start, mid);
            ParallelSumTask rightTask = new ParallelSumTask(array, mid, end);

            // SENIOR OPTIMIZATION:
            // fork() left task asynchronously onto the pool
            leftTask.fork();

            // Compute right task synchronously on the CURRENT thread (saves scheduling overhead!)
            long rightResult = rightTask.compute();

            // Wait for left task and combine
            long leftResult = leftTask.join();

            return leftResult + rightResult;
        }
    }
}
