package lts.java21.switchpatterns;

import java.util.List;

/**
 * Java 21 LTS — Pattern Matching for `switch` (JEP 441).
 *
 * Covers:
 * 1. Explicit `case null` without throwing NullPointerException.
 * 2. Guarded `when` clauses ordered from specific -> general (Dominance rule).
 * 3. Exhaustive `switch` over sealed interface + qualified enum constants!
 */
public class SwitchPatternMatchingDeepDiveDemo {

    sealed interface ServiceHealth permits StandardState, CustomIncident {}
    enum StandardState implements ServiceHealth { UP, DEGRADED, DOWN }
    record CustomIncident(String ticketId, int severity) implements ServiceHealth {}

    public static void main(String[] args) {
        System.out.println("=== Java 21 Pattern Matching for switch (JEP 441) Demo ===");

        // 1. Dynamic Object inspection with `case null` & guarded `when`
        List<Object> samples = new java.util.ArrayList<>();
        samples.add(null);
        samples.add("URGENT: Disk full");
        samples.add("  ");
        samples.add(503);
        samples.add(List.of("a", "b"));

        System.out.println("[1] Object payload classification:");
        for (Object sample : samples) {
            System.out.println("    " + classifyObject(sample));
        }

        // 2. Qualified enum constants + Record inside exhaustive sealed switch (no `default` needed!)
        List<ServiceHealth> healthReports = List.of(
                StandardState.UP,
                StandardState.DEGRADED,
                new CustomIncident("INC-911", 1),
                new CustomIncident("INC-104", 3)
        );
        System.out.println("\n[2] Sealed hierarchy + qualified enum exhaustiveness:");
        for (ServiceHealth h : healthReports) {
            System.out.println("    " + routeIncident(h));
        }
    }

    private static String classifyObject(Object obj) {
        return switch (obj) {
            case null -> "NULL_SAFE: null payload ignored without NPE";
            case String s when s.isBlank() -> "STRING_BLANK";
            case String s when s.startsWith("URGENT") -> "STRING_PRIORITY -> " + s;
            case String s -> "STRING -> " + s; // Must come AFTER guarded String `when` cases!
            case Integer i when i >= 500 -> "HTTP_SERVER_ERROR (" + i + ")";
            case Integer i -> "INT (" + i + ")";
            case List<?> list -> "LIST size=" + list.size();
            default -> "OTHER (" + obj.getClass().getSimpleName() + ")";
        };
    }

    private static String routeIncident(ServiceHealth health) {
        // Notice: ZERO `default:` branch! Compiler guarantees exhaustiveness across StandardState + CustomIncident!
        return switch (health) {
            case StandardState.UP -> "STATUS_GREEN: Healthy";
            case StandardState.DEGRADED -> "STATUS_YELLOW: Warn SRE Slack";
            case StandardState.DOWN -> "STATUS_RED: Page on-call immediately!";
            case CustomIncident inc when inc.severity() == 1 -> "SEV-1 PAGER: " + inc.ticketId();
            case CustomIncident inc -> "Ticket logged: " + inc.ticketId() + " (sev=" + inc.severity() + ")";
        };
    }
}
