package foundations.control.decision;

/**
 * Quick decision-making overview comparing if, if-else, if-else-if, and switch.
 */
public class Information {
    public static void main(String[] args) {
        int latencyMs = 180;

        if (latencyMs > 100) {
            System.out.println("[Alert] Latency above 100ms threshold: " + latencyMs + "ms");
        }

        String tier = (latencyMs < 50) ? "FAST"
                : (latencyMs < 200) ? "ACCEPTABLE"
                : "SLOW";
        System.out.println("SLA Tier: " + tier);
    }
}
