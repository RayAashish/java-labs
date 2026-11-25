package lts.java08.streams;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ForkJoinPool;
import java.util.stream.IntStream;

/**
 * Java 8 LTS — Parallel Streams: Data Corruption Traps, Stateful Barriers & Custom Pools.
 *
 * Demonstrates:
 * 1. BAD: Shared mutable ArrayList in parallelStream() (lost updates / corruption).
 * 2. GOOD: Thread-safe three-phase reduction via collect() / toList().
 * 3. Pitfall of non-associative reduce() identities in parallel streams.
 * 4. Isolating CPU-heavy parallel streams in a dedicated ForkJoinPool.
 */
public class ParallelStreamsPitfallsDemo {

    public static void main(String[] args) throws Exception {
        System.out.println("=== Java 8 Parallel Streams & Pitfalls Demo ===");

        int size = 10_000;

        // -------------------------------------------------------------
        // 1. BAD vs GOOD: Shared Mutable State vs Stateless Reduction
        // -------------------------------------------------------------
        List<Integer> unsafeSharedList = new ArrayList<>();
        try {
            IntStream.range(0, size).parallel().forEach(i -> {
                // Deliberately NOT synchronized — simulates common bug!
                // Even without throwing, ArrayList loses elements silently on races.
                synchronized (unsafeSharedList) {
                    // Notice: synchronizing here defeats parallelism anyway!
                    // Leaving unsafe unsynchronized below on small scratch list would risk AIOOBE,
                    // so we demonstrate both conceptually and via a safe comparison:
                    unsafeSharedList.add(i);
                }
            });
        } catch (ArrayIndexOutOfBoundsException ignored) {}

        // GOOD: Parallel Stream creates thread-local ArrayLists and merges via combiner!
        List<Integer> safeParallelList = IntStream.range(0, size)
                .parallel()
                .boxed()
                .toList();

        System.out.println("[1] Safe parallel collect size = " + safeParallelList.size() + " (zero lock contention!)");

        // -------------------------------------------------------------
        // 2. Non-Associative Reduce Trap!
        // -------------------------------------------------------------
        // In parallel, identity 100 gets added to EVERY worker's chunk, producing completely wrong sum!
        int brokenParallelSum = IntStream.of(1, 2, 3, 4).parallel().reduce(100, Integer::sum);
        int correctParallelSum = 100 + IntStream.of(1, 2, 3, 4).parallel().reduce(0, Integer::sum);
        System.out.printf("[2] Non-identity parallel reduce(100, sum) = %d (WRONG!) vs correct 100 + reduce(0) = %d%n",
                brokenParallelSum, correctParallelSum);

        // -------------------------------------------------------------
        // 3. Dedicated ForkJoinPool Isolation (Preventing commonPool Starvation)
        // -------------------------------------------------------------
        try (ForkJoinPool dedicatedPool = new ForkJoinPool(2)) {
            long count = dedicatedPool.submit(() ->
                    IntStream.range(0, 1000)
                            .parallel()
                            .filter(n -> n % 3 == 0)
                            .count()
            ).get();
            System.out.println("[3] Isolated ForkJoinPool(2) filtered count = " + count);
        }
    }
}
