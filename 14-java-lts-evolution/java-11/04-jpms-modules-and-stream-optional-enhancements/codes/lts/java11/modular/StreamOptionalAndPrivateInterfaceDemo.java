package lts.java11.modular;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Java 11 LTS (covering Java 9->11) — Interface Private Methods, Stream & Optional Upgrades.
 */
public class StreamOptionalAndPrivateInterfaceDemo {

    interface StructuredLogger {
        default void info(String msg) {
            emit("INFO", msg);
        }

        default void warn(String msg) {
            emit("WARN", msg);
        }

        // Java 9 private instance method: shared across defaults WITHOUT leaking to public API!
        private void emit(String level, String msg) {
            System.out.println("  [" + formatTag(level) + "] " + msg);
        }

        // Java 9 private static method: callable by both static and instance interface methods!
        private static String formatTag(String level) {
            return level.strip().toUpperCase();
        }
    }

    record Metric(String service, int latencyMs) {}

    public static void main(String[] args) {
        System.out.println("=== Java 9->11 Private Interface, Stream & Optional Enhancements ===");

        // -------------------------------------------------------------
        // 1. Private Interface Methods
        // -------------------------------------------------------------
        StructuredLogger logger = new StructuredLogger() {};
        logger.info("Service booted cleanly");
        logger.warn("Cache warmup pending");

        // -------------------------------------------------------------
        // 2. Stream takeWhile, dropWhile, 3-arg iterate, ofNullable
        // -------------------------------------------------------------
        List<Integer> sortedTemps = List.of(10, 15, 22, 35, 18, 12);
        // NOTE: takeWhile stops permanently at 35 (even though 18 < 30 comes later!)
        List<Integer> safePrefix = sortedTemps.stream().takeWhile(t -> t < 30).toList();
        List<Integer> fromHotOnwards = sortedTemps.stream().dropWhile(t -> t < 30).toList();
        List<Integer> forLoopStream = Stream.iterate(0, n -> n < 10, n -> n + 3).toList();

        System.out.printf("%n[2] takeWhile(<30)=%s | dropWhile(<30)=%s | iterate(0,<10,+3)=%s%n",
                safePrefix, fromHotOnwards, forLoopStream);

        // -------------------------------------------------------------
        // 3. Collectors.filtering preserving empty group keys!
        // -------------------------------------------------------------
        List<Metric> metrics = List.of(
                new Metric("auth", 12),
                new Metric("payments", 250),
                new Metric("payments", 40)
        );
        // "auth" has 0 slow requests (>100ms), yet remains in map with empty list []!
        Map<String, List<Metric>> slowByService = metrics.stream().collect(
                Collectors.groupingBy(
                        Metric::service,
                        Collectors.filtering(m -> m.latencyMs() > 100, Collectors.toList())
                )
        );
        System.out.println("[3] Collectors.filtering preserving empty keys: " + slowByService);

        // -------------------------------------------------------------
        // 4. Optional ifPresentOrElse + or + isEmpty (Java 11)
        // -------------------------------------------------------------
        Optional<String> missingToken = Optional.empty();
        System.out.println("[4] isEmpty() = " + missingToken.isEmpty());
        missingToken.ifPresentOrElse(
                tok -> System.out.println("  Found token: " + tok),
                () -> System.out.println("  ifPresentOrElse -> Triggering anonymous session fallback")
        );

        // Inspect current module descriptor metadata at runtime
        Module currentModule = StreamOptionalAndPrivateInterfaceDemo.class.getModule();
        System.out.println("[5] Executing inside JPMS module: " +
                (currentModule.isNamed() ? currentModule.getName() : "<unnamed classpath module>"));
    }
}
