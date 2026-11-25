package functional.streams;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

/**
 * PARALLEL STREAMS — RACE CONDITION PITFALL vs SAFE REDUCTION & SIZING
 *
 * Covers:
 * - ❌ BAD: Mutating an external ArrayList inside parallel().forEach() -> lost items / crashes!
 * - ✅ GOOD: Stateless parallel collection managed by the Stream collector/toList framework.
 * - Why small N or poor Spliterator (LinkedList) performs WORSE in parallel than sequential!
 */
public class ParallelStreamsPitfallsAndBenchmarkDemo {

    public static void main(String[] args) {
        System.out.println("=== 1. BAD vs GOOD: Shared Mutable State in Parallel Streams ===");
        int targetCount = 10_000;

        // ❌ BAD: ArrayList is NOT thread-safe! Multiple ForkJoinPool threads race on array growth/write.
        List<Integer> unsafeList = new ArrayList<>();
        try {
            IntStream.range(0, targetCount).parallel().forEach(unsafeList::add);
            System.out.println("BAD unsafe parallel ArrayList size (expected " + targetCount + "): " + unsafeList.size());
        } catch (ArrayIndexOutOfBoundsException ex) {
            System.out.println("BAD unsafe parallel ArrayList CRASHED with ArrayIndexOutOfBoundsException!");
        }

        // ✅ GOOD: Thread-safe parallel reduction handled cleanly by Stream library
        List<Integer> safeList = IntStream.range(0, targetCount).parallel().boxed().toList();
        System.out.println("GOOD thread-safe parallel .toList() size: " + safeList.size());

        System.out.println("\n=== 2. When Parallelism Helps vs Hurts (Small N Overhead) ===");
        long t1 = System.nanoTime();
        long seqSmall = IntStream.range(0, 1_000).mapToLong(i -> i * 2L).sum();
        long seqNs = System.nanoTime() - t1;

        long t2 = System.nanoTime();
        long parSmall = IntStream.range(0, 1_000).parallel().mapToLong(i -> i * 2L).sum();
        long parNs = System.nanoTime() - t2;

        System.out.printf("N=1,000 -> Sequential: %d us | Parallel (ForkJoin overhead): %d us (sum=%d/%d)%n",
                seqNs / 1_000, parNs / 1_000, seqSmall, parSmall);
    }
}
