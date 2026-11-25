package patterns.solid;

import java.util.Map;

/**
 * O — OPEN / CLOSED PRINCIPLE (OCP)
 *
 * WHAT IT IS:
 *   Software entities should be OPEN for extension, but CLOSED for modification.
 *
 * WHY IT MATTERS:
 *   If adding a new discount tier or payment method forces you to edit a central
 *   if-else / switch block, every new feature risks regressing tested existing branches.
 *
 * IN THIS FILE:
 *   1. BAD CODE:  BadCheckoutCalculator uses an ever-growing if-else ladder.
 *   2. GOOD CODE: PaymentStrategy interface lets you plug in new methods (e.g., Crypto, ApplePay)
 *                 by adding a new implementation without touching CheckoutEngine!
 */
public class OpenClosedPrincipleDemo {

    // =========================================================================
    // ❌ BAD CODE: Must MODIFY this method every time a new payment type launches!
    // =========================================================================
    static class BadPaymentService {
        public void pay(String method, double amount) {
            if ("CARD".equals(method)) {
                System.out.println("[BAD] Charging credit card: $" + amount);
            } else if ("UPI".equals(method)) {
                System.out.println("[BAD] Charging UPI handle: $" + amount);
            } else if ("PAYPAL".equals(method)) {
                System.out.println("[BAD] Redirecting to PayPal: $" + amount);
            } else {
                throw new IllegalArgumentException("Unsupported method: " + method);
            }
        }
    }

    // =========================================================================
    // ✅ GOOD CODE: Closed for modification, open for extension via polymorphism.
    // =========================================================================
    @FunctionalInterface
    interface PaymentMethod {
        void pay(double amount);
    }

    static class CardPayment implements PaymentMethod {
        @Override
        public void pay(double amount) {
            System.out.println("[GOOD-CARD] Authorized & captured $" + amount);
        }
    }

    static class UpiPayment implements PaymentMethod {
        @Override
        public void pay(double amount) {
            System.out.println("[GOOD-UPI]  Instant settlement $" + amount);
        }
    }

    /** CheckoutEngine NEVER changes when new payment providers are added! */
    static class CheckoutEngine {
        private final Map<String, PaymentMethod> registry;

        CheckoutEngine(Map<String, PaymentMethod> registry) {
            this.registry = registry;
        }

        public void process(String methodKey, double amount) {
            PaymentMethod method = registry.get(methodKey);
            if (method == null) {
                throw new IllegalArgumentException("Unknown payment key: " + methodKey);
            }
            method.pay(amount);
        }
    }

    public static void main(String[] args) {
        System.out.println("=== BAD (Rigid if-else requires modifying tested code) ===");
        new BadPaymentService().pay("UPI", 49.99);

        System.out.println("\n=== GOOD (Extensible Registry — new methods added with 0 edits to engine) ===");
        CheckoutEngine engine = new CheckoutEngine(Map.of(
                "CARD", new CardPayment(),
                "UPI", new UpiPayment(),
                "CRYPTO", amt -> System.out.println("[GOOD-CRYPTO] Settled USDC $" + amt) // Extended cleanly!
        ));
        engine.process("CARD", 49.99);
        engine.process("CRYPTO", 199.00);
    }
}
