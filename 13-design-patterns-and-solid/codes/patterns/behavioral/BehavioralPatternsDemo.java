package patterns.behavioral;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Demonstrates Behavioral Patterns:
 * 1. Strategy (Runtime algorithm selection)
 * 2. Observer (Lightweight event broadcast)
 * 3. Chain of Responsibility (HTTP Middleware pipeline)
 */
public class BehavioralPatternsDemo {

    // 1. Strategy
    @FunctionalInterface
    interface ShippingCostStrategy {
        double calculate(double weightKg);
    }

    // 2. Observer
    static class OrderEventBus {
        private final List<Consumer<String>> listeners = new ArrayList<>();
        void subscribe(Consumer<String> listener) { listeners.add(listener); }
        void publish(String event) { listeners.forEach(l -> l.accept(event)); }
    }

    // 3. Chain of Responsibility
    static abstract class RequestFilter {
        private RequestFilter next;

        RequestFilter linkWith(RequestFilter next) {
            this.next = next;
            return next;
        }

        public boolean handle(String request) {
            if (!check(request)) return false;
            return next == null || next.handle(request);
        }

        protected abstract boolean check(String request);
    }

    static class AuthFilter extends RequestFilter {
        protected boolean check(String request) {
            boolean ok = request.contains("Bearer ");
            System.out.println("[AuthFilter] " + (ok ? "PASS" : "BLOCKED"));
            return ok;
        }
    }

    static class RateLimitFilter extends RequestFilter {
        protected boolean check(String request) {
            System.out.println("[RateLimitFilter] PASS");
            return true;
        }
    }

    public static void main(String[] args) {
        System.out.println("=== 1. Strategy ===");
        ShippingCostStrategy standard = kg -> 5.0 + kg * 1.5;
        ShippingCostStrategy airExpress = kg -> 15.0 + kg * 4.0;
        System.out.println("Standard (4kg): $" + standard.calculate(4));
        System.out.println("Express  (4kg): $" + airExpress.calculate(4));

        System.out.println("\n=== 2. Observer ===");
        OrderEventBus bus = new OrderEventBus();
        bus.subscribe(e -> System.out.println("[Inventory] Reserved stock for " + e));
        bus.subscribe(e -> System.out.println("[Analytics] Recorded metric for " + e));
        bus.publish("ORDER#777");

        System.out.println("\n=== 3. Chain of Responsibility ===");
        RequestFilter chain = new AuthFilter();
        chain.linkWith(new RateLimitFilter());
        chain.handle("GET /api/orders Header: Bearer xyz");
        chain.handle("GET /api/orders (no token)");
    }
}
