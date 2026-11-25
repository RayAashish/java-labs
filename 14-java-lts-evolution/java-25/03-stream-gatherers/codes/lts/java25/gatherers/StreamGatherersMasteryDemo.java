package lts.java25.gatherers;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Gatherer;
import java.util.stream.Gatherers;
import java.util.stream.Stream;

/**
 * Java 25 LTS — Stream Gatherers (JEP 485, finalized JDK 24+).
 *
 * Covers:
 * 1. All 5 built-in Gatherers: windowFixed, windowSliding, scan, fold, mapConcurrent.
 * 2. Custom stateful Gatherer (`distinctBy` preserving encounter order — impossible with built-in distinct()!).
 * 3. Custom short-circuiting Gatherer (`takeUntilSumExceeds`).
 */
public class StreamGatherersMasteryDemo {

    record User(int id, String email) {}

    public static void main(String[] args) {
        System.out.println("=== Java 25 Stream Gatherers (JEP 485) Mastery ===");

        List<Integer> nums = List.of(10, 20, 30, 40, 50);

        // -------------------------------------------------------------
        // 1. Built-in Gatherers Catalog
        // -------------------------------------------------------------
        System.out.println("[1] windowFixed(2)   = " + nums.stream().gather(Gatherers.windowFixed(2)).toList());
        System.out.println("    windowSliding(3) = " + nums.stream().gather(Gatherers.windowSliding(3)).toList());
        System.out.println("    scan(runningSum) = " + nums.stream().gather(Gatherers.scan(() -> 0, Integer::sum)).toList());
        System.out.println("    fold(sumAsOne)   = " + nums.stream().gather(Gatherers.fold(() -> 0, Integer::sum)).toList());

        List<String> concurrentFetched = Stream.of("svc-A", "svc-B", "svc-C")
                .gather(Gatherers.mapConcurrent(2, svc -> svc.toUpperCase() + "_OK"))
                .toList();
        System.out.println("    mapConcurrent(2) = " + concurrentFetched);

        // -------------------------------------------------------------
        // 2. Custom Gatherer #1: `distinctBy(keyExtractor)` (Stateful Intermediate Op!)
        // -------------------------------------------------------------
        List<User> users = List.of(
                new User(1, "alice@acme.io"),
                new User(2, "bob@acme.io"),
                new User(3, "alice@acme.io") // Duplicate email, different ID!
        );
        List<User> uniqueByEmail = users.stream()
                .gather(distinctBy(User::email))
                .toList();
        System.out.println("\n[2] Custom distinctBy(email) -> " + uniqueByEmail);

        // -------------------------------------------------------------
        // 3. Custom Gatherer #2: Early Short-Circuiting (returns `false` when downstream stops or budget hit!)
        // -------------------------------------------------------------
        List<Integer> prefixUnderBudget = Stream.of(15, 25, 40, 100)
                .gather(takeWhileCumulativeSumUnder(50))
                .toList();
        System.out.println("[3] Short-circuit cumulative sum < 50 -> " + prefixUnderBudget);
    }

    /**
     * Custom sequential Gatherer emitting first occurrence per extracted key.
     */
    private static <T, K> Gatherer<T, Set<K>, T> distinctBy(Function<T, K> keyExtractor) {
        return Gatherer.ofSequential(
                HashSet::new,
                Gatherer.Integrator.ofGreedy((seenKeys, element, downstream) -> {
                    if (seenKeys.add(keyExtractor.apply(element))) {
                        return downstream.push(element);
                    }
                    return true; // Continue consuming even if duplicate skipped
                })
        );
    }

    /**
     * Custom short-circuiting Gatherer tracking running total and halting stream immediately when budget exceeded.
     */
    private static Gatherer<Integer, int[], Integer> takeWhileCumulativeSumUnder(int maxSum) {
        return Gatherer.ofSequential(
                () -> new int[]{0},
                (runningTotal, nextVal, downstream) -> {
                    if (runningTotal[0] + nextVal >= maxSum) {
                        return false; // Short-circuits upstream cleanly!
                    }
                    runningTotal[0] += nextVal;
                    return downstream.push(nextVal);
                }
        );
    }
}
