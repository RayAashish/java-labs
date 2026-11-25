package functional.streams;

import java.util.Comparator;
import java.util.List;

/**
 * STREAM INTERMEDIATE OPERATIONS DEEP DIVE
 *
 * Demonstrates:
 * - Lazy Loop Fusion (zero work without terminal op; vertical short-circuiting)
 * - filter(), map(), flatMap(), mapMulti() (Java 16)
 * - distinct(), sorted(), limit(), skip()
 * - takeWhile() & dropWhile() (Java 9) vs filter()
 * - peek() behavior & Java 9 count() trap
 */
public class StreamIntermediateOpsMasteryDemo {

    record Dept(String name, List<String> engineers) {}

    public static void main(String[] args) {
        System.out.println("=== 1. Lazy Evaluation & Vertical Loop Fusion ===");
        List<String> names = List.of("Ana", "Bob", "Alexander", "Beatrice", "Charlie");
        String firstLong = names.stream()
                .filter(n -> {
                    System.out.println("  [filter] checking " + n);
                    return n.length() > 5;
                })
                .map(n -> {
                    System.out.println("  [map] uppercasing " + n);
                    return n.toUpperCase();
                })
                .findFirst()
                .orElse("");
        System.out.println("Result (Beatrice & Charlie NEVER even visited!): " + firstLong);

        System.out.println("\n=== 2. flatMap() vs mapMulti() (Java 16) ===");
        List<Dept> depts = List.of(
                new Dept("Platform", List.of("Aashish", "Meera")),
                new Dept("Payments", List.of("Meera", "Rohan"))
        );
        List<String> uniqueSorted = depts.stream()
                .flatMap(d -> d.engineers().stream())
                .distinct()
                .sorted(Comparator.reverseOrder())
                .toList();
        System.out.println("flatMap -> distinct -> reverse sorted: " + uniqueSorted);

        // mapMulti pushes 0, 1, or N elements directly without allocating intermediate Stream objects!
        List<Object> mixed = List.of("Java", 42, "Loom", 3.14, "ZGC");
        List<String> onlyStrings = mixed.stream()
                .<String>mapMulti((obj, downstream) -> {
                    if (obj instanceof String s) {
                        downstream.accept(s.toLowerCase());
                    }
                })
                .toList();
        System.out.println("mapMulti extracted strings: " + onlyStrings);

        System.out.println("\n=== 3. takeWhile() & dropWhile() (Java 9) vs filter() ===");
        List<Integer> sortedNums = List.of(2, 4, 6, 7, 8, 10);
        System.out.println("filter(even):    " + sortedNums.stream().filter(n -> n % 2 == 0).toList());
        System.out.println("takeWhile(even): " + sortedNums.stream().takeWhile(n -> n % 2 == 0).toList());
        System.out.println("dropWhile(even): " + sortedNums.stream().dropWhile(n -> n % 2 == 0).toList());

        System.out.println("\n=== 4. skip() + limit() Pagination ===");
        List<Integer> page2 = sortedNums.stream().skip(2).limit(3).toList();
        System.out.println("Page (skip 2, limit 3): " + page2);
    }
}
