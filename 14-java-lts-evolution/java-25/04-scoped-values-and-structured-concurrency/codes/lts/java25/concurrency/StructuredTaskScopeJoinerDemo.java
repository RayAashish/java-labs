package lts.java25.concurrency;

import java.util.concurrent.StructuredTaskScope;
import java.util.concurrent.StructuredTaskScope.Joiner;
import java.util.concurrent.StructuredTaskScope.Subtask;

/**
 * Java 25 LTS — Structured Concurrency (JEP 505 Preview -> requires `--enable-preview`).
 *
 * Demonstrates:
 * 1. `StructuredTaskScope.open(Joiner.awaitAllSuccessfulOrThrow())` + automatic ScopedValue O(1) inheritance!
 * 2. Automatic cancellation of sibling virtual threads when one fails (zero leaked orphan tasks!).
 * 3. `Joiner.anySuccessfulResultOrThrow()` racing fastest replica.
 */
public class StructuredTaskScopeJoinerDemo {

    private static final ScopedValue<String> REQUEST_ID = ScopedValue.newInstance();

    record OrderAggregate(String profile, String inventoryStatus) {}

    public static void main(String[] args) throws Exception {
        System.out.println("=== Java 25 StructuredTaskScope + Joiner + ScopedValue Demo ===");

        // 1. Fan-out all-succeed scope automatically inheriting ScopedValue REQUEST_ID across virtual subtasks!
        OrderAggregate aggregate = ScopedValue.where(REQUEST_ID, "req-9988").call(() -> fetchAllOrFailFast());
        System.out.println("[1] Fan-out aggregated cleanly: " + aggregate);

        // 2. Fast-fail cancellation cancels slower sibling immediately
        try {
            failFastCancellationDemo();
        } catch (StructuredTaskScope.FailedException ex) {
            System.out.println("[2] Fail-fast interrupted slow sibling & surfaced root cause: " + ex.getCause().getMessage());
        }

        // 3. First-to-succeed hedge race
        String fastestMirror = raceFastestMirror();
        System.out.println("[3] Fastest mirror won race: " + fastestMirror);
    }

    private static OrderAggregate fetchAllOrFailFast() throws InterruptedException {
        try (var scope = StructuredTaskScope.open(Joiner.awaitAllSuccessfulOrThrow())) {
            Subtask<String> profile = scope.fork(() -> {
                Thread.sleep(25);
                return "Profile[" + REQUEST_ID.get() + "]"; // O(1) inherited ScopedValue!
            });
            Subtask<String> stock = scope.fork(() -> {
                Thread.sleep(30);
                return "IN_STOCK[" + REQUEST_ID.get() + "]";
            });

            scope.join();
            return new OrderAggregate(profile.get(), stock.get());
        }
    }

    private static void failFastCancellationDemo() throws InterruptedException {
        try (var scope = StructuredTaskScope.open(Joiner.awaitAllSuccessfulOrThrow())) {
            scope.fork(() -> {
                Thread.sleep(500);
                return "Should never complete!";
            });
            scope.fork(() -> {
                Thread.sleep(20);
                throw new IllegalStateException("Fraud check rejected!");
            });
            scope.join();
        }
    }

    private static String raceFastestMirror() throws InterruptedException {
        try (var scope = StructuredTaskScope.open(Joiner.<String>anySuccessfulResultOrThrow())) {
            scope.fork(() -> {
                Thread.sleep(150);
                return "US-East Mirror";
            });
            scope.fork(() -> {
                Thread.sleep(20);
                return "EU-Central Mirror (Fastest)";
            });
            return scope.join();
        }
    }
}
