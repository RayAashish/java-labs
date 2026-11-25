package lts.java25.unnamed;

import java.util.ArrayDeque;
import java.util.List;
import java.util.Map;

/**
 * Java 25 LTS (JEP 456, finalized JDK 22+) — Unnamed Variables & Patterns (`_`).
 */
public class UnnamedVariablesAndPatternsDemo {

    record TraceScope(String label) implements AutoCloseable {
        @Override
        public void close() {
            System.out.println("    [AutoClose] Closed scope cleanly: " + label);
        }
    }

    sealed interface Packet permits Ping, Data {}
    record Ping(long timestampMs) implements Packet {}
    record Data(String payload, int checksum, String sourceHost) implements Packet {}

    public static void main(String[] args) {
        System.out.println("=== Java 25 Unnamed Variables & Patterns (`_`, JEP 456) Demo ===");

        // 1. try-with-resources where the variable is ONLY used for deterministic .close()
        try (var _ = new TraceScope("HTTP_REQUEST"); var _ = new TraceScope("DB_TX")) {
            System.out.println("[1] Multiple `_` guards active simultaneously without name collisions!");
        }

        // 2. Unused catch exception
        int parsed = 0;
        try {
            parsed = Integer.parseInt("not-a-number");
        } catch (NumberFormatException _) {
            parsed = -1;
        }
        System.out.println("[2] Caught NumberFormatException via `catch (NumberFormatException _)`: fallback=" + parsed);

        // 3. Unused lambda parameters (multiple `_` allowed in same lambda!)
        Map<String, Integer> quotas = Map.of("alpha", 10, "beta", 20);
        System.out.print("[3] Map keys ignoring values: ");
        quotas.forEach((key, _) -> System.out.print(key + " "));
        System.out.println();

        // 4. Enhanced for + local side-effect variable discard
        ArrayDeque<String> queue = new ArrayDeque<>(List.of("first", "second", "third"));
        String _ = queue.removeFirst(); // Explicitly discards head without needing `unusedHead`!
        int remaining = 0;
        for (String _ : queue) {
            remaining++;
        }
        System.out.println("[4] Remaining count after discarding head via `_`: " + remaining);

        // 5. Unnamed Record Pattern components (`_`) in switch
        List<Packet> packets = List.of(new Ping(12345L), new Data("ALERT", 0xFA, "edge-1"));
        System.out.println("[5] Packet summaries using unnamed record patterns:");
        for (Packet p : packets) {
            String summary = switch (p) {
                case Ping(_) -> "HEARTBEAT_ACK"; // Unpacks and ignores timestamp cleanly
                case Data(var payload, _, _) -> "PAYLOAD=" + payload; // Extracts ONLY payload!
            };
            System.out.println("    " + summary);
        }
    }
}
