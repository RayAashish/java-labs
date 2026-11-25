package lts.java21.recordpatterns;

import java.util.List;

/**
 * Java 21 LTS — Nested Record Patterns & Deconstruction (JEP 440).
 *
 * Covers:
 * 1. Nested deconstruction inside `switch` (DomainEvent / Order / Money / Customer).
 * 2. Generic record pattern type inference (`Envelope<Notification>(var payload)`).
 * 3. Safe handling when a nested record component is `null` (doesn't throw NPE on deconstruction check!).
 */
public class NestedRecordPatternsDemo {

    record Money(String currency, double amount) {}
    record Customer(String id, String tier) {}

    sealed interface DomainEvent permits OrderPlaced, RefundIssued {}
    record OrderPlaced(String orderId, Customer customer, Money total) implements DomainEvent {}
    record RefundIssued(String refundId, Money amount) implements DomainEvent {}

    record Envelope<T>(String traceId, T payload) {}

    public static void main(String[] args) {
        System.out.println("=== Java 21 Nested Record Patterns (JEP 440) Demo ===");

        List<DomainEvent> events = List.of(
                new OrderPlaced("ORD-1", new Customer("C-10", "VIP"), new Money("USD", 1250.0)),
                new OrderPlaced("ORD-2", new Customer("C-20", "STD"), new Money("EUR", 80.0)),
                new OrderPlaced("ORD-3", null, new Money("USD", 50.0)), // Nested customer is null!
                new RefundIssued("REF-9", new Money("USD", 45.0))
        );

        System.out.println("[1] Nested Record Deconstruction in switch:");
        for (DomainEvent ev : events) {
            System.out.println("    " + processEvent(ev));
        }

        // 2. Generic Record Pattern Inference
        Envelope<OrderPlaced> wrapped = new Envelope<>(
                "trace-abc",
                new OrderPlaced("ORD-99", new Customer("C-99", "VIP"), new Money("INR", 9000))
        );
        if (wrapped instanceof Envelope(var trace, OrderPlaced(var id, _, Money(var cur, var amt)))) {
            System.out.printf("%n[2] Generic + Nested Deconstruction -> trace=%s | order=%s | %.0f %s%n",
                    trace, id, amt, cur);
        }
    }

    private static String processEvent(DomainEvent event) {
        return switch (event) {
            case OrderPlaced(var id, Customer(_, var tier), Money(var cur, var amt))
                    when "VIP".equals(tier) && amt >= 1000 ->
                    String.format("[VIP PRIORITY] %s -> %.2f %s", id, amt, cur);

            case OrderPlaced(var id, Customer(var cid, var tier), Money(var cur, var amt)) ->
                    String.format("[STANDARD ORDER] %s customer=%s(%s) %.2f %s", id, cid, tier, amt, cur);

            // Handles OrderPlaced where nested Customer happened to be null safely without NPE!
            case OrderPlaced(var id, _, Money(var cur, var amt)) ->
                    String.format("[GUEST/NO-CUSTOMER ORDER] %s %.2f %s", id, amt, cur);

            case RefundIssued(var refId, Money(var cur, var amt)) ->
                    String.format("[REFUND] %s -> %.2f %s", refId, amt, cur);
        };
    }
}
