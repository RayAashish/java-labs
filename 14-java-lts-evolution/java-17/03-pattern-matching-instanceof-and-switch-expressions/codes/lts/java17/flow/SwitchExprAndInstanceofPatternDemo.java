package lts.java17.flow;

import java.time.DayOfWeek;

/**
 * Java 17 LTS — Pattern Matching for `instanceof` (JEP 394) & `switch` Expressions (JEP 361).
 *
 * Covers:
 * 1. Pattern instanceof + Guarded Early Return Flow Scoping (without explicit casts).
 * 2. One-line `equals()` using `instanceof` pattern + `&&`.
 * 3. Exhaustive `switch` expressions with `->` arrows, comma labels, and multi-line `yield`.
 */
public class SwitchExprAndInstanceofPatternDemo {

    static final class Coordinate {
        private final int x;
        private final int y;

        Coordinate(int x, int y) {
            this.x = x;
            this.y = y;
        }

        @Override
        public boolean equals(Object o) {
            // Concise, bug-free idiomatic equals using flow scoping!
            return (this == o) || (o instanceof Coordinate other && x == other.x && y == other.y);
        }

        @Override
        public int hashCode() { return 31 * x + y; }
    }

    public static void main(String[] args) {
        System.out.println("=== Java 17 Pattern instanceof & Switch Expressions Demo ===");

        // 1. Flow scoping with early return + equals
        System.out.println("[1] inspectPayload('  Java 17 ') = " + inspectPayload("  Java 17 "));
        System.out.println("    inspectPayload(null)         = " + inspectPayload(null));
        System.out.println("    Coordinate equals check      = " + new Coordinate(3, 4).equals(new Coordinate(3, 4)));

        // 2. Switch Expression with arrow syntax + comma cases + yield block
        for (DayOfWeek day : new DayOfWeek[]{DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.SUNDAY}) {
            String scheduleType = switch (day) {
                case MONDAY, TUESDAY, THURSDAY, FRIDAY -> "Core Sprint Workday";
                case WEDNESDAY -> {
                    // Multi-statement block inside switch expression MUST terminate with `yield`!
                    String note = "Architecture Review + " + day;
                    yield note;
                }
                case SATURDAY, SUNDAY -> "Weekend Recharge";
            };
            System.out.println("[2] " + day + " -> " + scheduleType);
        }
    }

    private static String inspectPayload(Object payload) {
        // Early-exit flow scoping: if NOT String s, return immediately!
        if (!(payload instanceof String s)) {
            return "<not-a-string>";
        }
        // `s` remains cleanly in scope for the entire rest of the method!
        String trimmed = s.strip();
        return trimmed.isEmpty() ? "<blank>" : trimmed.toUpperCase();
    }
}
