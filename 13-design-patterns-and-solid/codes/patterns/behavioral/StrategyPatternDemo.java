package patterns.behavioral;

/**
 * BEHAVIORAL PATTERN 1 — STRATEGY PATTERN
 *
 * WHAT IT IS:
 *   Defines a family of interchangeable algorithms, encapsulates each one behind a common
 *   interface, and makes them swappable at runtime.
 *
 * WHY IT MATTERS:
 *   Avoids giant `if-else` / `switch` ladders inside business classes when pricing, sorting,
 *   compression, or routing rules vary dynamically per customer or request.
 *   JDK Examples: Comparator<T> passed to List.sort() / Arrays.sort().
 */
public class StrategyPatternDemo {

    // ❌ BAD CODE: Hardcoded algorithm branches inside CartService!
    static double badCalculateShipping(String mode, double weightKg) {
        if ("STANDARD".equals(mode)) return 5.0 + weightKg * 1.5;
        if ("EXPRESS".equals(mode))  return 15.0 + weightKg * 4.0;
        if ("DRONE".equals(mode))    return 25.0 + weightKg * 8.0;
        throw new IllegalArgumentException("Unknown mode: " + mode);
    }

    // ✅ GOOD CODE: Strategy interface + runtime injection (works cleanly with lambdas & enums!)
    @FunctionalInterface
    interface ShippingStrategy {
        double calculateFee(double weightKg);
    }

    static class ShippingCalculator {
        private ShippingStrategy strategy;

        ShippingCalculator(ShippingStrategy strategy) {
            this.strategy = strategy;
        }

        public void setStrategy(ShippingStrategy strategy) {
            this.strategy = strategy;
        }

        public double quote(double weightKg) {
            return strategy.calculateFee(weightKg);
        }
    }

    public static void main(String[] args) {
        System.out.println("=== BAD (Hardcoded conditional ladder) ===");
        System.out.println("Bad EXPRESS (3kg): $" + badCalculateShipping("EXPRESS", 3.0));

        System.out.println("\n=== GOOD (Runtime-Swappable Strategy) ===");
        ShippingCalculator calculator = new ShippingCalculator(w -> 5.0 + w * 1.5);
        System.out.println("Standard (3kg): $" + calculator.quote(3.0));

        calculator.setStrategy(w -> 15.0 + w * 4.0);
        System.out.println("Express  (3kg): $" + calculator.quote(3.0));

        calculator.setStrategy(w -> w <= 2.0 ? 20.0 : 40.0);
        System.out.println("Drone    (3kg): $" + calculator.quote(3.0));
    }
}
