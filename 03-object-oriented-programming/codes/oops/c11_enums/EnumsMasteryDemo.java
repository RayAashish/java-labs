package oops.c11_enums;

import java.util.EnumMap;
import java.util.EnumSet;

/**
 * Demonstrates Java Enums as First-Class Rich Types:
 * 1. Fields, private constructors, and singleton identity (==).
 * 2. Constant-specific method bodies (Strategy pattern inside an Enum).
 * 3. High-performance EnumSet (bit-vector) and EnumMap (array-backed).
 */
public class EnumsMasteryDemo {

    public static void main(String[] args) {
        System.out.println("=== 1. Enum Fields & Singleton Identity ===");
        DeviceMode m1 = DeviceMode.ECO;
        DeviceMode m2 = DeviceMode.valueOf("ECO");
        System.out.println("m1 == m2: " + (m1 == m2) + ", watts limit: " + m1.maxWatts());

        System.out.println("\n=== 2. Constant-Specific Method Strategy ===");
        for (PricingTier tier : PricingTier.values()) {
            System.out.printf("%s discount on $100 -> Final: $%.2f%n", tier, tier.applyDiscount(100.0));
        }

        System.out.println("\n=== 3. EnumSet & EnumMap Performance Collections ===");
        EnumSet<DeviceMode> lowPowerModes = EnumSet.of(DeviceMode.SLEEP, DeviceMode.ECO);
        System.out.println("Low power modes (bit-vector): " + lowPowerModes);

        EnumMap<DeviceMode, String> labels = new EnumMap<>(DeviceMode.class);
        labels.put(DeviceMode.ECO, "Energy Saving Active");
        labels.put(DeviceMode.TURBO, "Maximum Cooling Output");
        System.out.println("EnumMap (array-indexed): " + labels);
    }

    enum DeviceMode {
        SLEEP(5),
        ECO(45),
        TURBO(180);

        private final int maxWatts;

        // Enum constructors are implicitly private
        DeviceMode(int maxWatts) {
            this.maxWatts = maxWatts;
        }

        public int maxWatts() {
            return maxWatts;
        }
    }

    enum PricingTier {
        STANDARD {
            @Override
            double applyDiscount(double amount) {
                return amount;
            }
        },
        PRO {
            @Override
            double applyDiscount(double amount) {
                return amount * 0.90;
            }
        },
        ENTERPRISE {
            @Override
            double applyDiscount(double amount) {
                return amount * 0.75;
            }
        };

        abstract double applyDiscount(double amount);
    }
}
