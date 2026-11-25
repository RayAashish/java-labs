package foundations.control.loops;

/**
 * Demonstrates the exit-controlled 'do-while' loop:
 * Guarantees the body executes AT LEAST ONCE even if the condition is initially false.
 */
public class DoWhileLoop {
    public static void main(String[] args) {
        System.out.println("=== 1. Exit-Controlled Guarantee (runs once when false) ===");
        int attempts = 5;
        do {
            System.out.println("Executed body once even though attempts (" + attempts + ") >= 5!");
            attempts++;
        } while (attempts < 5);

        System.out.println("\n=== 2. Retry Polling Pattern ===");
        int[] responses = {503, 503, 200};
        int idx = 0;
        int status;
        do {
            status = responses[idx++];
            System.out.println("Attempt #" + idx + " -> HTTP " + status);
        } while (status != 200 && idx < responses.length);
    }
}
