package patterns.structural;

/**
 * STRUCTURAL PATTERN 4 — FACADE PATTERN
 *
 * WHAT IT IS:
 *   Provides a clean, simplified high-level interface to a complex subsystem of multiple
 *   interdependent classes.
 *
 * WHY IT MATTERS:
 *   Without a Facade, every controller or client duplicates the brittle multi-step ceremony
 *   (Reserve Inventory -> Fraud Check -> Charge Payment -> Generate Shipping Label -> Send Email).
 *   Facade exposes ONE clean entry point (`placeOrder()`) while keeping subsystems accessible if needed.
 */
public class FacadePatternDemo {

    // Complex internal subsystems
    static class InventorySubsystem {
        boolean reserve(String sku, int qty) {
            System.out.println("  [Inventory] Reserved " + qty + "x " + sku);
            return true;
        }
    }
    static class FraudCheckSubsystem {
        boolean isSafe(String userId) {
            System.out.println("  [FraudEngine] Risk score OK for " + userId);
            return true;
        }
    }
    static class BillingSubsystem {
        void capture(String userId, double amount) {
            System.out.println("  [Billing] Captured $" + amount + " from " + userId);
        }
    }
    static class ShippingSubsystem {
        String createLabel(String sku) {
            System.out.println("  [Shipping] Created tracking label for " + sku);
            return "TRK-99102";
        }
    }

    // ❌ BAD CODE: Client directly orchestrates 4 subsystems in fragile order everywhere!
    static void badClientOrder() {
        new FraudCheckSubsystem().isSafe("U-1");
        new InventorySubsystem().reserve("SKU-LAPTOP", 1);
        new BillingSubsystem().capture("U-1", 1200.0);
        new ShippingSubsystem().createLabel("SKU-LAPTOP");
    }

    // ✅ GOOD CODE: Facade hides subsystem wiring behind one clean method.
    static class OrderCheckoutFacade {
        private final InventorySubsystem inventory = new InventorySubsystem();
        private final FraudCheckSubsystem fraud = new FraudCheckSubsystem();
        private final BillingSubsystem billing = new BillingSubsystem();
        private final ShippingSubsystem shipping = new ShippingSubsystem();

        public String completeCheckout(String userId, String sku, int qty, double amount) {
            if (!fraud.isSafe(userId) || !inventory.reserve(sku, qty)) {
                throw new IllegalStateException("Checkout rejected");
            }
            billing.capture(userId, amount);
            return shipping.createLabel(sku);
        }
    }

    public static void main(String[] args) {
        System.out.println("=== BAD (Client tightly coupled to 4 subsystems) ===");
        badClientOrder();

        System.out.println("\n=== GOOD (OrderCheckoutFacade provides 1 clean entry point) ===");
        String trackingId = new OrderCheckoutFacade().completeCheckout("U-42", "SKU-MOUSE", 2, 79.98);
        System.out.println("Checkout complete -> Tracking ID: " + trackingId);
    }
}
