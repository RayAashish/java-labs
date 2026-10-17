package com.raysi.lts.java08.codes;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Author: raysi
 * Package: com.raysi.lts.java08.codes
 *
 * Demonstrates:
 * 1. Lazy evaluation of intermediate operations (filter, map, peek) vs eager terminal operations.
 * 2. Short-circuiting execution paths (limit, findFirst).
 * 3. Stream lifecycle: Streams are single-use pipelines (IllegalStateException on reuse).
 * 4. Fusing operations and Spliterator mechanics.
 */
public class StreamApiIntermediateVsTerminalDemo {

    public static void main(String[] args) {
        System.out.println("=== Java 8 Stream API: Intermediate vs. Terminal Operations ===");

        List<String> rawTransactions = Arrays.asList(
                "tx_100_usd", "tx_50_eur", "tx_900_usd", "tx_20_gbp", "tx_450_usd", "tx_1200_usd"
        );

        // -------------------------------------------------------------
        // 1. Proof of Laziness: Without a terminal operation, NOTHING executes
        // -------------------------------------------------------------
        System.out.println("\n--- Step 1: Declaring Intermediate Pipeline (No Terminal Op) ---");
        Stream<String> unexecutedStream = rawTransactions.stream()
                .filter(tx -> {
                    System.out.println("  [Filter Evaluated]: " + tx);
                    return tx.endsWith("usd");
                })
                .map(tx -> {
                    System.out.println("  [Map Evaluated]: " + tx);
                    return tx.toUpperCase();
                });

        System.out.println(">> Stream pipeline constructed in memory, but NO filter/map logs appeared!");

        // -------------------------------------------------------------
        // 2. Triggering Execution with a Terminal Operation (collect)
        // -------------------------------------------------------------
        System.out.println("\n--- Step 2: Triggering Execution with collect() ---");
        List<String> processedUsd = unexecutedStream.collect(Collectors.toList());
        System.out.println(">> Resulting List: " + processedUsd);

        // -------------------------------------------------------------
        // 3. Short-Circuiting Mechanics (Loop Fusion in Action)
        // -------------------------------------------------------------
        System.out.println("\n--- Step 3: Short-Circuiting with limit(2) & Loop Fusion ---");
        // Streams process element-by-element vertically through stages, NOT stage-by-stage horizontally!
        List<String> firstTwoUsd = rawTransactions.stream()
                .peek(tx -> System.out.println("  [Stream Traversal]: " + tx))
                .filter(tx -> tx.endsWith("usd"))
                .map(String::toUpperCase)
                .limit(2) // Short-circuits the pipeline once 2 elements are satisfied
                .collect(Collectors.toList());

        System.out.println(">> First Two USD: " + firstTwoUsd);

        // -------------------------------------------------------------
        // 4. Stream Reuse Exception (Single-Use Lifecycle)
        // -------------------------------------------------------------
        System.out.println("\n--- Step 4: Stream Reuse Hazard ---");
        Stream<String> singleUseStream = rawTransactions.stream().filter(tx -> tx.startsWith("tx"));
        long count = singleUseStream.count(); // Terminal operation 1
        System.out.println(">> Processed count: " + count);

        try {
            singleUseStream.collect(Collectors.toList()); // Terminal operation 2 -> Fails!
        } catch (IllegalStateException ex) {
            System.err.println(">> [CAUGHT EXPECTED EXCEPTION]: " + ex.getMessage());
            System.err.println("   Streams cannot be reused once operated upon or closed.");
        }
    }
}
