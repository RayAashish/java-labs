package patterns.creational;

import java.util.Objects;

/**
 * Demonstrates Creational Patterns:
 * 1. Enum Singleton (thread-safe, reflection-safe, serialization-safe)
 * 2. Double-Checked Locking (DCL) Singleton with mandatory 'volatile'
 * 3. Builder Pattern for readable, validated immutable objects
 * 4. Factory Method for decoupled creation
 */
public class CreationalPatternsDemo {

    // 1. Enum Singleton (Effective Java gold standard)
    enum AppRegistry {
        INSTANCE;

        private final String environment = "PRODUCTION";

        public String environment() {
            return environment;
        }
    }

    // 2. Lazy Double-Checked Locking Singleton (volatile prevents partially constructed reads)
    static final class ConnectionConfig {
        private static volatile ConnectionConfig instance;
        private final String url;

        private ConnectionConfig(String url) {
            this.url = url;
        }

        public static ConnectionConfig getInstance() {
            if (instance == null) {
                synchronized (ConnectionConfig.class) {
                    if (instance == null) {
                        instance = new ConnectionConfig("jdbc:mysql://localhost:3306/javalabs");
                    }
                }
            }
            return instance;
        }

        public String url() {
            return url;
        }
    }

    // 3. Builder Pattern
    static final class HttpRequestSpec {
        private final String url;
        private final String method;
        private final int timeoutMs;

        private HttpRequestSpec(Builder builder) {
            this.url = Objects.requireNonNull(builder.url, "url required");
            this.method = builder.method;
            this.timeoutMs = builder.timeoutMs;
        }

        @Override
        public String toString() {
            return method + " " + url + " (timeout=" + timeoutMs + "ms)";
        }

        static class Builder {
            private String url;
            private String method = "GET";
            private int timeoutMs = 3000;

            Builder url(String url) { this.url = url; return this; }
            Builder method(String method) { this.method = method; return this; }
            Builder timeoutMs(int timeoutMs) {
                if (timeoutMs <= 0) throw new IllegalArgumentException("timeoutMs > 0");
                this.timeoutMs = timeoutMs;
                return this;
            }
            HttpRequestSpec build() { return new HttpRequestSpec(this); }
        }
    }

    // 4. Factory Method
    interface AlertChannel { void send(String msg); }

    static AlertChannel createChannel(String type) {
        return switch (type.toUpperCase()) {
            case "SLACK" -> msg -> System.out.println("[Slack] " + msg);
            case "SMS" -> msg -> System.out.println("[SMS]   " + msg);
            default -> throw new IllegalArgumentException("Unknown channel: " + type);
        };
    }

    public static void main(String[] args) {
        System.out.println("Enum Singleton env: " + AppRegistry.INSTANCE.environment());
        System.out.println("DCL Singleton url:  " + ConnectionConfig.getInstance().url());

        HttpRequestSpec req = new HttpRequestSpec.Builder()
                .url("https://api.example.com/v1/orders")
                .method("POST")
                .timeoutMs(5000)
                .build();
        System.out.println("Built request:      " + req);

        createChannel("SLACK").send("Deployment succeeded!");
    }
}
