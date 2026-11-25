package foundations.control.decision;

/**
 * Demonstrates the evolution of Java switch:
 * 1. Classic switch statement (break & fall-through danger)
 * 2. Modern switch expression with arrow (->) and yield (Java 14+)
 * 3. Pattern matching + null handling in switch (Java 21+)
 */
public class ModernSwitchExpressionDemo {

    public static void main(String[] args) {
        System.out.println("=== 1. Modern Switch Expression (->) ===");
        int httpStatus = 429;
        String category = switch (httpStatus) {
            case 200, 201, 204 -> "Success";
            case 400, 401, 403, 404 -> "Client Error";
            case 429 -> {
                System.out.println("[Audit] Rate-limit encountered; triggering backoff.");
                yield "Throttled (Retry with Exponential Backoff)";
            }
            default -> httpStatus >= 500 ? "Server Error" : "Other";
        };
        System.out.println("HTTP " + httpStatus + " -> " + category);

        System.out.println("\n=== 2. Java 21+ Null & Guarded Pattern Switch ===");
        System.out.println(describeInput(null));
        System.out.println(describeInput("Java 25"));
        System.out.println(describeInput(42));
        System.out.println(describeInput(-5));
    }

    private static String describeInput(Object input) {
        return switch (input) {
            case null -> "Input is null (handled safely without NPE!)";
            case String s when s.isBlank() -> "Blank string";
            case String s -> "Text of length " + s.length() + ": " + s;
            case Integer n when n > 0 -> "Positive integer: " + n;
            case Integer n -> "Non-positive integer: " + n;
            default -> "Unsupported type: " + input.getClass().getSimpleName();
        };
    }
}
