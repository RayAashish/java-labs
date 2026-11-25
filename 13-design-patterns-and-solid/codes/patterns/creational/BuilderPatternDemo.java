package patterns.creational;

import java.util.Objects;

/**
 * CREATIONAL PATTERN 4 — BUILDER PATTERN
 *
 * WHAT IT IS:
 *   Separates the construction of a complex, immutable object from its representation,
 *   enabling fluent step-by-step configuration and invariant validation before creation.
 *
 * WHY IT MATTERS:
 *   Replaces unreadable "telescoping constructors" (`new Client("host", 8080, true, false, 3000, 3)`)
 *   and half-baked mutable JavaBean setters (where objects exist in invalid intermediate states).
 *
 * IN THIS FILE:
 *   1. BAD CODE:  Telescoping constructor with confusing boolean/int argument order.
 *   2. GOOD CODE: Immutable HttpClientConfig built via fluent validated Builder.
 */
public class BuilderPatternDemo {

    // =========================================================================
    // ❌ BAD CODE: Hard to read, easy to swap boolean/int args silently!
    // =========================================================================
    static class BadClientConfig {
        BadClientConfig(String host, int port, boolean tls, boolean retry, int timeoutMs, int maxPool) {
            System.out.println("[BAD] Created ambiguous config: " + host + ":" + port + " tls=" + tls + " retry=" + retry);
        }
    }

    // =========================================================================
    // ✅ GOOD CODE: Self-documenting, immutable, invariant-checked Builder.
    // =========================================================================
    static final class HttpClientConfig {
        private final String baseUrl;
        private final int port;
        private final boolean tlsEnabled;
        private final int connectTimeoutMs;
        private final int maxRetries;

        private HttpClientConfig(Builder b) {
            this.baseUrl = Objects.requireNonNull(b.baseUrl, "baseUrl is mandatory");
            this.port = b.port;
            this.tlsEnabled = b.tlsEnabled;
            this.connectTimeoutMs = b.connectTimeoutMs;
            this.maxRetries = b.maxRetries;
        }

        @Override
        public String toString() {
            return String.format("HttpClientConfig{url='%s:%d', tls=%s, timeout=%dms, retries=%d}",
                    baseUrl, port, tlsEnabled, connectTimeoutMs, maxRetries);
        }

        public static Builder builder(String baseUrl) {
            return new Builder(baseUrl);
        }

        public static final class Builder {
            private final String baseUrl;
            private int port = 443;
            private boolean tlsEnabled = true;
            private int connectTimeoutMs = 3000;
            private int maxRetries = 2;

            private Builder(String baseUrl) {
                this.baseUrl = baseUrl;
            }

            public Builder port(int port) {
                if (port < 1 || port > 65535) throw new IllegalArgumentException("Invalid port: " + port);
                this.port = port;
                return this;
            }

            public Builder tlsEnabled(boolean tlsEnabled) {
                this.tlsEnabled = tlsEnabled;
                return this;
            }

            public Builder connectTimeoutMs(int connectTimeoutMs) {
                if (connectTimeoutMs <= 0) throw new IllegalArgumentException("Timeout must be > 0");
                this.connectTimeoutMs = connectTimeoutMs;
                return this;
            }

            public Builder maxRetries(int maxRetries) {
                if (maxRetries < 0) throw new IllegalArgumentException("maxRetries >= 0");
                this.maxRetries = maxRetries;
                return this;
            }

            public HttpClientConfig build() {
                if (!tlsEnabled && port == 443) {
                    throw new IllegalStateException("Cannot disable TLS on standard HTTPS port 443!");
                }
                return new HttpClientConfig(this);
            }
        }
    }

    public static void main(String[] args) {
        System.out.println("=== BAD (Cryptic positional boolean/int arguments) ===");
        new BadClientConfig("api.payments.io", 443, true, false, 5000, 3);

        System.out.println("\n=== GOOD (Readable, Immutable, Validated Builder) ===");
        HttpClientConfig config = HttpClientConfig.builder("https://api.payments.io")
                .port(8443)
                .connectTimeoutMs(5000)
                .maxRetries(4)
                .build();
        System.out.println(config);
    }
}
