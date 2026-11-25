package patterns.structural;

/**
 * STRUCTURAL PATTERN 1 — ADAPTER PATTERN
 *
 * WHAT IT IS:
 *   Converts the incompatible interface of an existing/third-party class into the target
 *   interface your application expects — like a travel power-plug adapter.
 *
 * WHY IT MATTERS:
 *   You cannot edit third-party SDK jars or legacy vendor APIs. Instead of polluting your
 *   business code with `if (vendor instanceof LegacySdk)` checks, wrap the legacy API once
 *   inside an Adapter!
 *   JDK Examples: InputStreamReader(InputStream), Arrays.asList(array).
 */
public class AdapterPatternDemo {

    /** Target interface our CheckoutService speaks natively (cents in USD/INR). */
    interface PaymentGateway {
        boolean charge(String orderId, long amountCents);
    }

    /** Incompatible 3rd-party SDK we CANNOT modify (uses double dollars & String amount!). */
    static final class LegacyBankSdk {
        public String submitTransaction(String merchantRef, double dollarAmount) {
            System.out.printf("[LegacyBankSDK] Processing ref=%s amount=$%.2f%n", merchantRef, dollarAmount);
            return "TX_OK";
        }
    }

    // ❌ BAD CODE: Business service has vendor-specific conversions & branching everywhere!
    static void badCheckout(Object gateway, String orderId, long cents) {
        if (gateway instanceof LegacyBankSdk sdk) {
            sdk.submitTransaction("ORD-" + orderId, cents / 100.0); // Leaked currency conversion!
        }
    }

    // ✅ GOOD CODE: Adapter translates cents -> dollars and target method -> legacy method cleanly.
    static class LegacyBankGatewayAdapter implements PaymentGateway {
        private final LegacyBankSdk sdk;

        LegacyBankGatewayAdapter(LegacyBankSdk sdk) {
            this.sdk = sdk;
        }

        @Override
        public boolean charge(String orderId, long amountCents) {
            double dollars = amountCents / 100.0;
            String status = sdk.submitTransaction("ORD-" + orderId, dollars);
            return "TX_OK".equals(status);
        }
    }

    public static void main(String[] args) {
        System.out.println("=== BAD (Caller polluted by LegacyBankSdk details) ===");
        badCheckout(new LegacyBankSdk(), "101", 4999);

        System.out.println("\n=== GOOD (Clean Adapter hides third-party mismatch) ===");
        PaymentGateway gateway = new LegacyBankGatewayAdapter(new LegacyBankSdk());
        boolean paid = gateway.charge("102", 4999);
        System.out.println("Payment success: " + paid);
    }
}
