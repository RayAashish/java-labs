package lts.java08.streams;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Java 8 LTS — Every Intermediate & Terminal Operation + All 3 reduce() Overloads.
 *
 * Compares Bad/Verbose imperative nested loops vs Clean/Declarative Stream pipelines.
 */
public class StreamIntermediateAndTerminalOpsDemo {

    record LineItem(String sku, int qty, double unitPrice) {}
    record Order(String id, String customer, List<LineItem> items) {}

    public static void main(String[] args) {
        System.out.println("=== Java 8 Stream Intermediate & Terminal Ops Mastery ===");

        List<Order> orders = List.of(
                new Order("ORD-1", "Alice", List.of(new LineItem("LAPTOP", 1, 1200), new LineItem("MOUSE", 2, 25))),
                new Order("ORD-2", "Bob", List.of(new LineItem("MOUSE", 1, 25), new LineItem("USB-C", 3, 15))),
                new Order("ORD-3", "Alice", List.of(new LineItem("MONITOR", 1, 450)))
        );

        // -------------------------------------------------------------
        // 1. BAD/OLD (Nested loops + temp HashSet + sort) vs GOOD (flatMap + distinct + sorted)
        // -------------------------------------------------------------
        List<String> imperativeSkus = new ArrayList<>();
        for (Order o : orders) {
            for (LineItem item : o.items()) {
                if (item.unitPrice() >= 20 && !imperativeSkus.contains(item.sku())) {
                    imperativeSkus.add(item.sku());
                }
            }
        }
        imperativeSkus.sort(Comparator.naturalOrder());

        List<String> streamSkus = orders.stream()
                .flatMap(o -> o.items().stream())   // 1-to-Many flatten: Order -> LineItem
                .filter(i -> i.unitPrice() >= 20)   // Stateless filter
                .map(LineItem::sku)                 // Stateless transform
                .distinct()                         // Stateful dedup
                .sorted()                           // Stateful sort barrier
                .toList();

        System.out.println("[1] Unique SKUs >= $20 (Imperative vs Stream match): " + imperativeSkus.equals(streamSkus) + " -> " + streamSkus);

        // -------------------------------------------------------------
        // 2. skip + limit (Pagination pattern)
        // -------------------------------------------------------------
        List<String> pageTwo = streamSkus.stream()
                .skip(1)
                .limit(2)
                .toList();
        System.out.println("[2] Pagination (skip 1, limit 2): " + pageTwo);

        // -------------------------------------------------------------
        // 3. Boolean Short-Circuit Terminal Matchers
        // -------------------------------------------------------------
        boolean hasExpensive = orders.stream()
                .flatMap(o -> o.items().stream())
                .anyMatch(i -> i.unitPrice() > 1000);
        boolean allValidQty = orders.stream()
                .flatMap(o -> o.items().stream())
                .allMatch(i -> i.qty() > 0);
        boolean vacuousTruthOnEmpty = List.<String>of().stream().allMatch(s -> s.length() > 999);
        System.out.printf("[3] anyMatch(>$1000)=%s | allMatch(qty>0)=%s | EMPTY.allMatch=%s (vacuously true!)%n",
                hasExpensive, allValidQty, vacuousTruthOnEmpty);

        // -------------------------------------------------------------
        // 4. All 3 Overloads of reduce()
        // -------------------------------------------------------------
        List<Integer> numbers = List.of(10, 20, 30, 40);

        // Overload A: Optional<T> reduce(BinaryOperator<T>) — no identity
        Optional<Integer> maxOpt = numbers.stream().reduce(Integer::max);

        // Overload B: T reduce(T identity, BinaryOperator<T>)
        int sum = numbers.stream().reduce(0, Integer::sum);

        // Overload C: <U> U reduce(U identity, BiFunction<U,T,U> accumulator, BinaryOperator<U> combiner)
        // Notice T is Order, U is Double total revenue across ALL orders & items.
        // Combiner only executes during parallel traversal to merge thread-local partial doubles!
        double parallelRevenue = orders.parallelStream().reduce(
                0.0,
                (acc, order) -> acc + order.items().stream().mapToDouble(i -> i.qty() * i.unitPrice()).sum(),
                Double::sum
        );

        System.out.printf("[4] reduce#1 max=%s | reduce#2 sum=%d | reduce#3 parallelRevenue=$%.2f%n",
                maxOpt.orElse(0), sum, parallelRevenue);
    }
}
