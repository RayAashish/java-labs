package lts.java25.ergonomics;

/**
 * Java 25 LTS — Flexible Constructor Bodies (JEP 513, finalized JDK 25!).
 *
 * Demonstrates:
 * 1. Fail-fast validation BEFORE calling `super(...)` (prologue).
 * 2. Computing shared arguments once without static helper hacks before calling `super(...)`.
 * 3. Initializing subclass fields BEFORE `super(...)` so parent constructors invoking hooks observe initialized state!
 */
public class FlexibleConstructorBodiesDemo {

    static abstract class AbstractConnection {
        private final String endpoint;

        protected AbstractConnection(String endpoint) {
            this.endpoint = endpoint;
            // Even when parent constructor invokes subclass hook, prologue-initialized subclass fields are ALREADY set!
            onInitializing(endpoint);
        }

        protected abstract void onInitializing(String endpoint);

        public String endpoint() { return endpoint; }
    }

    static final class SecureTlsConnection extends AbstractConnection {
        private final String region;

        public SecureTlsConnection(String rawHost, String region) {
            // PROLOGUE (before super!): previously ILLEGAL in Java 8-21 without static helpers!
            if (rawHost == null || rawHost.isBlank()) {
                throw new IllegalArgumentException("Host must not be blank (validated BEFORE super()!)");
            }
            if (region == null || region.isBlank()) {
                throw new IllegalArgumentException("Region required");
            }
            String normalizedEndpoint = "https://" + rawHost.strip().toLowerCase() + ":8443";
            // Initialize subclass field BEFORE super() runs!
            this.region = region.strip().toUpperCase();

            super(normalizedEndpoint); // Notice `super(...)` is NO LONGER forced to line 1!
        }

        @Override
        protected void onInitializing(String endpoint) {
            // Prior to Java 25 JEP 513, `this.region` here printed `null`!
            System.out.println("  [Hook during super()] endpoint=" + endpoint + " | region=" + this.region);
        }
    }

    public static void main(String[] args) {
        System.out.println("=== Java 25 Flexible Constructor Bodies (JEP 513) Demo ===");

        SecureTlsConnection conn = new SecureTlsConnection("  API.Internal.Corp ", "ap-south-1");
        System.out.println("[1] Constructed safely -> " + conn.endpoint() + " in region=" + conn.region);

        try {
            new SecureTlsConnection("   ", "us-east-1");
        } catch (IllegalArgumentException fastFail) {
            System.out.println("[2] Fail-fast prologue rejected bad args before parent ran: " + fastFail.getMessage());
        }
    }
}
