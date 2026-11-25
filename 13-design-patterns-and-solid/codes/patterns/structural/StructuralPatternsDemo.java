package patterns.structural;

import java.util.HashMap;
import java.util.Map;

/**
 * Demonstrates Structural Patterns:
 * 1. Decorator: Dynamically wrapping MessageSender with Compression + Encryption
 * 2. Proxy: Caching proxy standing in front of an expensive DataFetcher
 * 3. Adapter: Bridging an incompatible LegacyPrinter to ModernPrinter
 */
public class StructuralPatternsDemo {

    // 1. Decorator
    interface DataPayload {
        String format(String raw);
    }

    static class PlainPayload implements DataPayload {
        public String format(String raw) { return raw; }
    }

    static class Base64TagDecorator implements DataPayload {
        private final DataPayload wrapped;
        Base64TagDecorator(DataPayload wrapped) { this.wrapped = wrapped; }
        public String format(String raw) { return "[ENC:" + wrapped.format(raw) + "]"; }
    }

    static class TimestampDecorator implements DataPayload {
        private final DataPayload wrapped;
        TimestampDecorator(DataPayload wrapped) { this.wrapped = wrapped; }
        public String format(String raw) { return "[TS=2026] " + wrapped.format(raw); }
    }

    // 2. Caching Proxy
    interface ExchangeRateClient {
        double usdToInr(String date);
    }

    static class SlowRemoteFxClient implements ExchangeRateClient {
        public double usdToInr(String date) {
            System.out.println("   [HTTP CALL] Fetching rate for " + date + "...");
            return 84.50;
        }
    }

    static class CachingFxProxy implements ExchangeRateClient {
        private final ExchangeRateClient delegate;
        private final Map<String, Double> cache = new HashMap<>();

        CachingFxProxy(ExchangeRateClient delegate) { this.delegate = delegate; }

        public double usdToInr(String date) {
            return cache.computeIfAbsent(date, delegate::usdToInr);
        }
    }

    // 3. Adapter
    interface ModernPrinter { void printLine(String text); }
    static class LegacyPrinter { void outputUpper(String s) { System.out.println("[LEGACY] " + s.toUpperCase()); } }
    static class LegacyPrinterAdapter implements ModernPrinter {
        private final LegacyPrinter legacy;
        LegacyPrinterAdapter(LegacyPrinter legacy) { this.legacy = legacy; }
        public void printLine(String text) { legacy.outputUpper(text); }
    }

    public static void main(String[] args) {
        System.out.println("=== 1. Decorator Pipeline ===");
        DataPayload pipeline = new TimestampDecorator(new Base64TagDecorator(new PlainPayload()));
        System.out.println(pipeline.format("secret-token"));

        System.out.println("\n=== 2. Caching Proxy ===");
        ExchangeRateClient client = new CachingFxProxy(new SlowRemoteFxClient());
        System.out.println("First call:  " + client.usdToInr("2026-11-25"));
        System.out.println("Second call: " + client.usdToInr("2026-11-25") + " (served from cache!)");

        System.out.println("\n=== 3. Adapter ===");
        ModernPrinter printer = new LegacyPrinterAdapter(new LegacyPrinter());
        printer.printLine("hello adapter");
    }
}
