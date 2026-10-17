package com.raysi.lts.java21.codes;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.SequencedCollection;
import java.util.SequencedMap;

/**
 * Author: raysi
 * Package: com.raysi.lts.java21.codes
 *
 * Demonstrates:
 * 1. Sequenced Collections (JEP 431): getFirst(), getLast(), addFirst(), reversed() view.
 * 2. SequencedMap: firstEntry(), lastEntry(), reversed().
 * 3. Pattern Matching for switch (JEP 441) with null cases & when guards.
 * 4. Record Patterns (JEP 440): Deconstructing records directly inside switch arms.
 */
public class SequencedCollectionsAndSwitchPatternsDemo {

    // Domain models for Record Pattern Matching
    public sealed interface DomainEvent permits OrderPlaced, PaymentProcessed, OrderCancelled {}

    public record OrderPlaced(String orderId, double amount, String customerTier) implements DomainEvent {}
    public record PaymentProcessed(String orderId, String gatewayTxId, boolean success) implements DomainEvent {}
    public record OrderCancelled(String orderId, String cancellationReason) implements DomainEvent {}

    public static void main(String[] args) {
        System.out.println("=== Java 21: Sequenced Collections & Switch Patterns Demo ===");

        // -------------------------------------------------------------
        // 1. Sequenced Collections (JEP 431)
        // -------------------------------------------------------------
        System.out.println("\n--- 1. Sequenced Collections (List, Set, Deque Unified) ---");
        SequencedCollection<String> seqList = new ArrayList<>(List.of("alpha", "beta", "gamma"));
        seqList.addFirst("first_item");
        seqList.addLast("last_item");

        System.out.println(">> Full List: " + seqList);
        System.out.println(">> getFirst(): " + seqList.getFirst());
        System.out.println(">> getLast(): " + seqList.getLast());
        System.out.println(">> reversed() View: " + seqList.reversed());

        // Sequenced Set (LinkedHashSet preserves order)
        LinkedHashSet<String> orderedSet = new LinkedHashSet<>(List.of("node-A", "node-B", "node-C"));
        System.out.println(">> SequencedSet first: " + orderedSet.getFirst() + ", last: " + orderedSet.getLast());

        // Sequenced Map
        SequencedMap<String, Integer> cache = new LinkedHashMap<>();
        cache.put("user_101", 95);
        cache.put("user_102", 82);
        cache.put("user_103", 99);

        System.out.println(">> SequencedMap firstEntry: " + cache.firstEntry());
        System.out.println(">> SequencedMap lastEntry: " + cache.lastEntry());
        System.out.println(">> SequencedMap reversed(): " + cache.reversed());

        // -------------------------------------------------------------
        // 2. Pattern Matching for switch (JEP 441) & Record Patterns (JEP 440)
        // -------------------------------------------------------------
        System.out.println("\n--- 2. Pattern Matching for switch with Record Patterns ---");
        List<DomainEvent> events = List.of(
                new OrderPlaced("ord-9001", 1500.0, "VIP_GOLD"),
                new OrderPlaced("ord-9002", 45.0, "STANDARD"),
                new PaymentProcessed("ord-9001", "gw_stripe_881", true),
                new OrderCancelled("ord-9002", "Customer requested cancellation")
        );

        for (DomainEvent event : events) {
            String auditLog = dispatchEventWithSwitchPattern(event);
            System.out.println(">> " + auditLog);
        }

        // Test explicit null handling in switch
        System.out.println(">> Null Event Handling: " + dispatchEventWithSwitchPattern(null));
    }

    /**
     * Java 21 Pattern Matching for switch:
     * - Exhaustiveness guaranteed at compile time (DomainEvent is sealed).
     * - Direct record deconstruction: OrderPlaced(var id, var amt, var tier).
     * - Guard expressions with 'when'.
     * - Safe null case handling inside switch without NPE!
     */
    public static String dispatchEventWithSwitchPattern(DomainEvent event) {
        return switch (event) {
            case null -> "EVENT_DROPPED: Null payload received safely without NPE";

            // Record pattern deconstruction with guarded 'when' clause
            case OrderPlaced(var id, var amt, var tier) when amt >= 1000.0 ->
                    String.format("[HIGH-PRIORITY ORDER] Id: %s, Amount: $%.2f, Tier: %s (Priority Route)", id, amt, tier);

            case OrderPlaced(var id, var amt, var tier) ->
                    String.format("[STANDARD ORDER] Id: %s, Amount: $%.2f, Tier: %s", id, amt, tier);

            // Record pattern matching without deconstruction
            case PaymentProcessed p when p.success() ->
                    String.format("[PAYMENT SETTLED] Order: %s via Gateway Tx: %s", p.orderId(), p.gatewayTxId());

            case PaymentProcessed p ->
                    String.format("[PAYMENT FAILED] Order: %s failed on Gateway Tx: %s", p.orderId(), p.gatewayTxId());

            case OrderCancelled(var id, var reason) ->
                    String.format("[ORDER CANCELLED] Id: %s, Reason: %s", id, reason);
        };
    }
}
