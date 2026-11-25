package lts.java08.streams;

import java.util.DoubleSummaryStatistics;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Java 8 LTS — Exhaustive Collectors Catalog, Custom Collector.of & Primitive Streams.
 */
public class StreamCollectorsAndPrimitivesDemo {

    record Sale(String region, String rep, double amount) {}

    public static void main(String[] args) {
        System.out.println("=== Java 8 Collectors & Primitive Streams Mastery ===");

        List<Sale> sales = List.of(
                new Sale("US", "Alice", 500.0),
                new Sale("EU", "Bob", 300.0),
                new Sale("US", "Charlie", 700.0),
                new Sale("US", "Alice", 200.0),
                new Sale("APAC", "Devon", 450.0)
        );

        // -------------------------------------------------------------
        // 1. toMap Trap (Duplicate Key Exception vs Merge Function)
        // -------------------------------------------------------------
        try {
            // BAD: Duplicate key "US" throws IllegalStateException!
            sales.stream().collect(Collectors.toMap(Sale::region, Sale::amount));
        } catch (IllegalStateException expected) {
            System.out.println("[1] Caught expected 2-arg toMap duplicate key error: " + expected.getMessage());
        }

        // GOOD: 4-arg toMap with merge function + custom Map implementation preserving insertion order
        Map<String, Double> revenueByRegion = sales.stream().collect(
                Collectors.toMap(Sale::region, Sale::amount, Double::sum, LinkedHashMap::new)
        );
        System.out.println("    Safe revenueByRegion (LinkedHashMap): " + revenueByRegion);

        // -------------------------------------------------------------
        // 2. Multi-Level groupingBy + Downstream Collectors
        // -------------------------------------------------------------
        Map<String, Map<String, Double>> regionThenRepSum = sales.stream().collect(
                Collectors.groupingBy(
                        Sale::region,
                        Collectors.groupingBy(Sale::rep, Collectors.summingDouble(Sale::amount))
                )
        );
        System.out.println("\n[2] Nested groupingBy(region -> rep -> sum): " + regionThenRepSum);

        // partitioningBy ALWAYS populates both true and false keys
        Map<Boolean, Long> highVsStandardCounts = sales.stream().collect(
                Collectors.partitioningBy(s -> s.amount() >= 500.0, Collectors.counting())
        );
        System.out.println("    partitioningBy (>=500 count): " + highVsStandardCounts);

        // -------------------------------------------------------------
        // 3. summarizingDouble + joining + collectingAndThen
        // -------------------------------------------------------------
        DoubleSummaryStatistics stats = sales.stream()
                .collect(Collectors.summarizingDouble(Sale::amount));
        System.out.printf("%n[3] Summary -> count=%d, sum=%.1f, avg=%.1f, min=%.1f, max=%.1f%n",
                stats.getCount(), stats.getSum(), stats.getAverage(), stats.getMin(), stats.getMax());

        String formattedReps = sales.stream()
                .map(Sale::rep)
                .distinct()
                .collect(Collectors.collectingAndThen(
                        Collectors.joining(" | ", "[", "]"),
                        String::toUpperCase
                ));
        System.out.println("    collectingAndThen(joining + upper): " + formattedReps);

        // -------------------------------------------------------------
        // 4. Custom Collector via Collector.of (supplier, accumulator, combiner, finisher)
        // -------------------------------------------------------------
        Collector<Sale, StringJoiner, String> regionTickerCollector = Collector.of(
                () -> new StringJoiner("; "),                          // supplier
                (joiner, sale) -> joiner.add(sale.region() + ":" + (int) sale.amount()), // accumulator
                StringJoiner::merge,                                   // combiner (for parallel)
                StringJoiner::toString                                 // finisher
        );
        System.out.println("\n[4] Custom Collector output: " + sales.stream().collect(regionTickerCollector));

        // -------------------------------------------------------------
        // 5. Primitive Streams (Zero Boxing Overhead)
        // -------------------------------------------------------------
        int fastSum = IntStream.rangeClosed(1, 100)
                .filter(n -> n % 2 == 0)
                .sum();
        double avg = sales.stream()
                .mapToDouble(Sale::amount) // Avoids Double boxing!
                .average()
                .orElse(0.0);
        System.out.printf("%n[5] IntStream even sum(1..100)=%d | mapToDouble avg=%.2f%n", fastSum, avg);
    }
}
