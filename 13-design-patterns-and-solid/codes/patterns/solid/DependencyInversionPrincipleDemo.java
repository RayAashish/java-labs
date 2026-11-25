package patterns.solid;

import java.util.HashMap;
import java.util.Map;

/**
 * D — DEPENDENCY INVERSION PRINCIPLE (DIP)
 *
 * WHAT IT IS:
 *   1. High-level business modules should NOT depend on low-level concrete implementations.
 *      Both should depend on abstractions (interfaces).
 *   2. Abstractions should NOT depend on details; details should depend on abstractions.
 *
 * WHY IT MATTERS:
 *   When OrderService hardcodes `new MySqlOrderDao()`, you cannot switch to PostgreSQL,
 *   add caching, or run fast unit tests without booting a real MySQL server.
 *
 * IN THIS FILE:
 *   1. BAD CODE:  BadOrderService directly instantiates concrete MySQL & SMTP dependencies.
 *   2. GOOD CODE: OrderService receives OrderRepository & AlertSender abstractions via constructor,
 *                 letting production pass MySQL/Email and tests pass an in-memory Fake!
 */
public class DependencyInversionPrincipleDemo {

    // =========================================================================
    // ❌ BAD CODE: High-level OrderService tightly coupled to low-level MySQL concrete class.
    // =========================================================================
    static class BadMySqlDatabase {
        public void persist(String orderId) {
            System.out.println("[BAD-MYSQL] TCP connect + INSERT " + orderId);
        }
    }

    static class BadOrderService {
        // Hardcoded concrete dependency — impossible to swap or unit-test in isolation!
        private final BadMySqlDatabase db = new BadMySqlDatabase();

        public void placeOrder(String orderId) {
            db.persist(orderId);
        }
    }

    // =========================================================================
    // ✅ GOOD CODE: Both high-level service & low-level adapters depend on abstractions.
    // =========================================================================
    interface OrderStore {
        void save(String orderId, double amount);
        Double findAmount(String orderId);
    }

    interface ReceiptDispatcher {
        void send(String message);
    }

    /** High-level business policy: zero knowledge of SQL, HTTP, or SMTP! */
    static class OrderService {
        private final OrderStore store;
        private final ReceiptDispatcher dispatcher;

        // Constructor Injection (the cornerstone of DIP & Spring DI)
        OrderService(OrderStore store, ReceiptDispatcher dispatcher) {
            this.store = store;
            this.dispatcher = dispatcher;
        }

        public void placeOrder(String orderId, double amount) {
            if (amount <= 0) {
                throw new IllegalArgumentException("Order amount must be positive");
            }
            store.save(orderId, amount);
            dispatcher.send("Order " + orderId + " placed for $" + amount);
        }
    }

    /** Low-level Production Adapter */
    static class PostgresOrderStore implements OrderStore {
        @Override public void save(String orderId, double amount) {
            System.out.println("[PROD-POSTGRES] Saved " + orderId + " ($" + amount + ")");
        }
        @Override public Double findAmount(String orderId) { return null; }
    }

    /** Fast In-Memory Test Double enabled by DIP! */
    static class InMemoryOrderStore implements OrderStore {
        private final Map<String, Double> db = new HashMap<>();
        @Override public void save(String orderId, double amount) { db.put(orderId, amount); }
        @Override public Double findAmount(String orderId) { return db.get(orderId); }
    }

    public static void main(String[] args) {
        System.out.println("=== BAD (Hardwired low-level dependency) ===");
        new BadOrderService().placeOrder("ORD-1");

        System.out.println("\n=== GOOD (DIP Production Wiring) ===");
        OrderService prodService = new OrderService(
                new PostgresOrderStore(),
                msg -> System.out.println("[PROD-EMAIL] " + msg)
        );
        prodService.placeOrder("ORD-100", 250.0);

        System.out.println("\n=== GOOD (DIP Fast Isolated Test Wiring) ===");
        InMemoryOrderStore fakeDb = new InMemoryOrderStore();
        OrderService testService = new OrderService(
                fakeDb,
                msg -> System.out.println("[TEST-SPY]   " + msg)
        );
        testService.placeOrder("ORD-TEST", 99.0);
        System.out.println("Verified stored amount in memory fake: $" + fakeDb.findAmount("ORD-TEST"));
    }
}
