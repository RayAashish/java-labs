package collections.hashmap_deep_dive.demo;

/**
 * Interactive Demonstration of HashMap's Internal Mechanics:
 * 1. The 32-bit Perturbation Hash Function: (h ^ (h >>> 16)).
 * 2. Bucket Index Masking: (n - 1) & hash.
 * 3. Why Power-of-Two Capacity is Required (Uniform Bit Distribution).
 * 4. High-Bit Partition Resizing Simulation.
 */
public class InternalMechanicsDemo {

    public static void main(String[] args) {
        System.out.println("======================================================================");
        System.out.println("       JAVA HASHMAP: INTERNAL MECHANICS & BITWISE MATH VISUALIZER      ");
        System.out.println("======================================================================");

        demonstrateHashPerturbation("Aashish");
        demonstrateHashPerturbation("JavaMastery");

        demonstratePowerOfTwoIndexMath(16);
        demonstrateOddCapacityHazard(15);

        demonstrateHighBitResizeSimulation();
    }

    /**
     * Demonstrates how the perturbation function XORs the upper 16 bits
     * into the lower 16 bits to prevent small-table collisions.
     */
    static void demonstrateHashPerturbation(String key) {
        System.out.println("\n----------------------------------------------------------------------");
        System.out.println("1. HASH PERTURBATION DEMO FOR KEY: \"" + key + "\"");
        System.out.println("----------------------------------------------------------------------");

        int h = key.hashCode();
        int shifted = h >>> 16;
        int perturbedHash = h ^ shifted;

        System.out.println("Raw key.hashCode()  (h)       : " + toBinary32(h) + " (dec: " + h + ")");
        System.out.println("Unsigned Shifted    (h >>> 16): " + toBinary32(shifted));
        System.out.println("----------------------------------------------------------------------");
        System.out.println("Perturbed Hash      (h ^ >>>) : " + toBinary32(perturbedHash) + " (dec: " + perturbedHash + ")");
        System.out.println("\nObservation: The top 16 bits have been mixed into the bottom 16 bits.");
        System.out.println("Even if multiple keys differ ONLY in their top bits, their bottom bits");
        System.out.println("now vary significantly, preventing collisions in small tables!");
    }

    /**
     * Demonstrates why (n - 1) & hash works when n is a power of 2.
     */
    static void demonstratePowerOfTwoIndexMath(int capacity) {
        System.out.println("\n----------------------------------------------------------------------");
        System.out.println("2. BUCKET INDEX CALCULATION WITH POWER-OF-TWO CAPACITY (n = " + capacity + ")");
        System.out.println("----------------------------------------------------------------------");

        int mask = capacity - 1;
        System.out.println("Capacity n          : " + capacity + " (" + toBinary32(capacity) + ")");
        System.out.println("Index Mask (n - 1)  : " + mask + " (" + toBinary32(mask) + ")");
        System.out.println("Notice that (n - 1) is a continuous sequence of 1s in the lower bits!");

        String[] testKeys = {"Alpha", "Beta", "Gamma", "Delta", "Epsilon", "Zeta"};
        System.out.println("\nIndex Calculation Table:");
        System.out.printf("%-10s | %-12s | %-34s | %-5s%n", "Key", "hashCode", "Lower 8 Bits of Perturbed Hash", "Index");
        System.out.println("----------------------------------------------------------------------");

        for (String k : testKeys) {
            int h = k.hashCode();
            int hash = h ^ (h >>> 16);
            int index = (capacity - 1) & hash;
            String lower8Bits = toBinary32(hash).substring(26);
            System.out.printf("%-10s | %-12d | ... %-30s | %-5d%n", k, h, lower8Bits, index);
        }
    }

    /**
     * Demonstrates what goes wrong if capacity is NOT a power of 2 (e.g. 15).
     */
    static void demonstrateOddCapacityHazard(int oddCapacity) {
        System.out.println("\n----------------------------------------------------------------------");
        System.out.println("3. HAZARD: WHAT HAPPENS WITH NON-POWER-OF-TWO CAPACITY (n = " + oddCapacity + ")");
        System.out.println("----------------------------------------------------------------------");

        int mask = oddCapacity - 1; // 14 = 0000 1110
        System.out.println("Capacity n         : " + oddCapacity);
        System.out.println("Index Mask (n - 1) : " + mask + " (Binary: " + Integer.toBinaryString(mask) + ")");
        System.out.println("\nCRITICAL OBSERVATION:");
        System.out.println("The last bit of mask (14) is '0' (0b1110).");
        System.out.println("Because any number bitwise AND-ed with 0 is always 0:");
        System.out.println(">>> (hash & 14) can NEVER produce an ODD index! <<<");
        System.out.println("Indices 1, 3, 5, 7, 9, 11, 13 CAN NEVER BE ACCESSED!");
        System.out.println("50% of the table is permanently wasted, doubling collisions!");
    }

    /**
     * Simulates the Java 8 High-Bit Test during table resizing from 16 to 32.
     */
    static void demonstrateHighBitResizeSimulation() {
        System.out.println("\n----------------------------------------------------------------------");
        System.out.println("4. JAVA 8 RESIZING OPTIMIZATION: THE HIGH-BIT TEST");
        System.out.println("----------------------------------------------------------------------");

        int oldCap = 16;
        int newCap = 32;

        System.out.println("Resizing from oldCap = " + oldCap + " to newCap = " + newCap);
        System.out.println("The test evaluates: (hash & oldCap) == 0 ? stays at oldIndex : oldIndex + oldCap\n");

        int[] sampleHashes = {
            0b0000_0101, // 5  -> bit 4 is 0 -> stays at 5
            0b0001_0101, // 21 -> bit 4 is 1 -> moves to 5 + 16 = 21
            0b0010_0101, // 37 -> bit 4 is 0 -> stays at 5
            0b0011_0101  // 53 -> bit 4 is 1 -> moves to 5 + 16 = 21
        };

        System.out.printf("%-10s | %-12s | %-15s | %-10s | %-10s%n",
                "Hash (dec)", "Hash (bin)", "(hash & 16)", "Old Index", "New Index");
        System.out.println("----------------------------------------------------------------------");

        for (int h : sampleHashes) {
            int oldIdx = (oldCap - 1) & h;
            int highBit = h & oldCap;
            int newIdx = (newCap - 1) & h;
            int computedNewIdx = (highBit == 0) ? oldIdx : (oldIdx + oldCap);

            System.out.printf("%-10d | %-12s | %-15s | %-10d | %-10d (%s)%n",
                    h,
                    String.format("%8s", Integer.toBinaryString(h)).replace(' ', '0'),
                    (highBit == 0 ? "0 (Bit 4 is 0)" : "16 (Bit 4 is 1)"),
                    oldIdx,
                    computedNewIdx,
                    (highBit == 0 ? "Stays at " + oldIdx : "Moves to " + oldIdx + "+16=" + computedNewIdx)
            );
        }
        System.out.println("\nConclusion: Zero rehashing required! Node order preserved completely.");
    }

    /**
     * Formats an integer as a 32-bit binary string separated by underscores.
     */
    static String toBinary32(int val) {
        String raw = String.format("%32s", Integer.toBinaryString(val)).replace(' ', '0');
        return raw.substring(0, 4) + "_" +
               raw.substring(4, 8) + "_" +
               raw.substring(8, 12) + "_" +
               raw.substring(12, 16) + "_" +
               raw.substring(16, 20) + "_" +
               raw.substring(20, 24) + "_" +
               raw.substring(24, 28) + "_" +
               raw.substring(28, 32);
    }
}
