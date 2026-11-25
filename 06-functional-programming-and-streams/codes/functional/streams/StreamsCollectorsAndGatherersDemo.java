package functional.streams;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Gatherers;
import java.util.stream.IntStream;

/**
 * Demonstrates Stream Lazy Evaluation, groupingBy, teeing, flatMap,
 * Primitive Streams, and Java 24/25 Stream Gatherers (windowFixed).
 */
public class StreamsCollectorsAndGatherersDemo {

    record Order(String id, String customer, List<String> items, double total) {}

    public static void main(String[] args) {
        List<Order> orders = List.of(
                new Order("O1", "Alice", List.of("Laptop", "Mouse"), 1250.0),
                new Order("O2", "Bob", List.of("Keyboard"), 95.0),
                new Order("O3", "Alice", List.of("Monitor", "USB-C Cable"), 410.0)
        );

        System.out.println("=== 1. Lazy Loop Fusion Proof ===");
        String firstMatch = orders.stream()
                .filter(o -> {
                    System.out.println("   filtering " + o.id());
                    return o.total() > 200;
                })
                .map(Order::customer)
                .findFirst()
                .orElse("None");
        System.out.println("First match (stopped after O1!): " + firstMatch);

        System.out.println("\n=== 2. flatMap (1-to-Many Flattening) ===");
        List<String> allDistinctItems = orders.stream()
                .flatMap(o -> o.items().stream())
                .distinct()
                .sorted()
                .toList();
        System.out.println("All distinct items: " + allDistinctItems);

        System.out.println("\n=== 3. Collectors.groupingBy & summingDouble ===");
        Map<String, Double> spendByCustomer = orders.stream()
                .collect(Collectors.groupingBy(Order::customer, Collectors.summingDouble(Order::total)));
        System.out.println("Spend by customer: " + spendByCustomer);

        System.out.println("\n=== 4. Java 24/25 Stream.gather(Gatherers.windowFixed) ===");
        List<List<Integer>> batches = IntStream.rangeClosed(1, 10)
                .boxed()
                .gather(Gatherers.windowFixed(3))
                .toList();
        System.out.println("Fixed 3-element windows: " + batches);
    }
}
