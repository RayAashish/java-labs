package functional.streams;

import java.util.IntSummaryStatistics;
import java.util.List;
import java.util.stream.Gatherers;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * PRIMITIVE STREAMS (ZERO BOXING) & JAVA 24/25 STREAM GATHERERS (JEP 485)
 *
 * Covers:
 * - IntStream / LongStream / DoubleStream avoiding Integer boxing overhead
 * - sum(), average(), summaryStatistics(), mapToObj()
 * - Java 24/25 Gatherers: windowFixed, windowSliding, and scan (running totals!)
 */
public class PrimitiveStreamsAndGatherersDemo {

    public static void main(String[] args) {
        System.out.println("=== 1. IntStream Zero-Boxing Statistics ===");
        IntSummaryStatistics stats = IntStream.rangeClosed(1, 100).summaryStatistics();
        System.out.printf("1..100 -> count=%d sum=%d avg=%.1f min=%d max=%d%n",
                stats.getCount(), stats.getSum(), stats.getAverage(), stats.getMin(), stats.getMax());

        System.out.println("\n=== 2. Java 24/25 Stream.gather(Gatherers.windowFixed) ===");
        List<List<Integer>> batches = IntStream.rangeClosed(1, 8)
                .boxed()
                .gather(Gatherers.windowFixed(3))
                .toList();
        System.out.println("Fixed 3-item batches: " + batches);

        System.out.println("\n=== 3. Java 24/25 Stream.gather(Gatherers.windowSliding) ===");
        List<List<Integer>> sliding = Stream.of(10, 20, 30, 40, 50)
                .gather(Gatherers.windowSliding(3))
                .toList();
        System.out.println("Sliding 3-item windows: " + sliding);

        System.out.println("\n=== 4. Java 24/25 Stream.gather(Gatherers.scan) Running Prefix Sum ===");
        List<Integer> runningSums = Stream.of(10, 20, 30, 40)
                .gather(Gatherers.scan(() -> 0, Integer::sum))
                .toList();
        System.out.println("Running prefix sums: " + runningSums);
    }
}
