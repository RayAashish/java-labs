package concurrency.phase5_virtual_threads_project_loom;

import java.util.concurrent.StructuredTaskScope;
import java.util.concurrent.StructuredTaskScope.Joiner;
import java.util.concurrent.StructuredTaskScope.Subtask;

/**
 * Demonstrates Java 25 Structured Concurrency (JEP 505 preview / evolution from Java 21 JEP 453):
 * 1. StructuredTaskScope.open(Joiner.awaitAllSuccessfulOrThrow()) -> replaces JDK 21 ShutdownOnFailure.
 * 2. Automatic cancellation of sibling subtasks if one fails (zero orphan thread leaks!).
 * 3. StructuredTaskScope.open(Joiner.anySuccessfulResultOrThrow()) -> replaces JDK 21 ShutdownOnSuccess.
 */
public class StructuredTaskScopeShutdownOnFailureDemo {

    public static void main(String[] args) {
        System.out.println("======================================================================");
        System.out.println("  PHASE 5 LAB: STRUCTURED CONCURRENCY (JDK 25 StructuredTaskScope)");
        System.out.println("======================================================================\n");

        System.out.println("=== PART 1: awaitAllSuccessfulOrThrow (All Must Succeed) ===");
        try {
            OrderResponse order = fetchOrderDetails("order-456");
            System.out.println("Successfully compiled order: " + order);
        } catch (Exception e) {
            System.err.println("Order retrieval failed: " + e.getMessage());
        }

        System.out.println("\n=== PART 2: Fast-Fail Cancellation (Automatic Sibling Cancellation) ===");
        try {
            fetchOrderWithFailingInventory("order-789");
        } catch (Exception e) {
            System.out.println("[Handled Error] StructuredTaskScope aborted sibling subtasks: " + e.getCause());
        }

        System.out.println("\n=== PART 3: anySuccessfulResultOrThrow (Race / First-to-Win) ===");
        try {
            String quote = fetchFastestWeatherQuote("Bengaluru");
            System.out.println("Fastest winning quote: " + quote);
        } catch (Exception e) {
            System.err.println("Failed to fetch weather: " + e.getMessage());
        }
    }

    private static OrderResponse fetchOrderDetails(String orderId) throws InterruptedException {
        try (var scope = StructuredTaskScope.open(Joiner.awaitAllSuccessfulOrThrow())) {
            Subtask<String> customerTask = scope.fork(() -> queryCustomerService(orderId));
            Subtask<String> paymentTask = scope.fork(() -> queryPaymentGateway(orderId));

            scope.join(); // Blocks until both succeed, or throws FailedException on first error
            return new OrderResponse(customerTask.get(), paymentTask.get());
        }
    }

    private static void fetchOrderWithFailingInventory(String orderId) throws InterruptedException {
        try (var scope = StructuredTaskScope.open(Joiner.awaitAllSuccessfulOrThrow())) {
            scope.fork(() -> {
                Thread.sleep(500);
                System.out.println("  [SlowTask] Should be interrupted before finishing!");
                return "Slow Result";
            });

            scope.fork(() -> {
                Thread.sleep(50);
                throw new IllegalStateException("Inventory out of stock for ID: " + orderId);
            });

            scope.join();
        }
    }

    private static String fetchFastestWeatherQuote(String city) throws InterruptedException {
        try (var scope = StructuredTaskScope.open(Joiner.<String>anySuccessfulResultOrThrow())) {
            scope.fork(() -> {
                Thread.sleep(120);
                return "Server A (OpenWeather): 24C in " + city;
            });
            scope.fork(() -> {
                Thread.sleep(40);
                return "Server B (AccuWeather): 24C in " + city;
            });
            scope.fork(() -> {
                Thread.sleep(200);
                return "Server C (WeatherGov): 24C in " + city;
            });

            return scope.join(); // Returns first successful result and cancels slower siblings!
        }
    }

    private static String queryCustomerService(String orderId) throws InterruptedException {
        Thread.sleep(60);
        return "Customer: John Smith (" + orderId + ")";
    }

    private static String queryPaymentGateway(String orderId) throws InterruptedException {
        Thread.sleep(80);
        return "Payment: Captured $199.99";
    }

    record OrderResponse(String customer, String payment) {}
}
