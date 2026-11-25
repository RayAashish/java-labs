package foundations.wrappers;

/**
 * Demonstrates Java Wrapper Caching (JLS §5.1.7) & Autoboxing/Unboxing Traps:
 * 1. IntegerCache caches values [-128, 127], making == true inside range and false outside!
 * 2. Null unboxing throws NullPointerException at runtime.
 * 3. Performance difference between primitive loops vs boxed wrapper loops.
 */
public class WrapperCachingAndEqualityDemo {

    public static void main(String[] args) {
        System.out.println("=== 1. IntegerCache Trap (-128 to 127) ===");
        Integer a = 127;
        Integer b = 127;
        Integer x = 128;
        Integer y = 128;

        System.out.println("127 == 127 (reference equality): " + (a == b));       // true (same cached object)
        System.out.println("128 == 128 (reference equality): " + (x == y));       // false (two Heap objects)
        System.out.println("128.equals(128) (value equality): " + x.equals(y));   // true (ALWAYS use .equals!)

        System.out.println("\n=== 2. Ternary & Null Unboxing Trap ===");
        Boolean condition = true;
        Integer nullableScore = null;
        try {
            // Compiler emits nullableScore.intValue() because other branch is primitive 0!
            int resolved = condition ? nullableScore : 0;
            System.out.println("Resolved: " + resolved);
        } catch (NullPointerException e) {
            System.out.println("Caught expected NullPointerException when unboxing null Integer in ternary!");
        }

        System.out.println("\n=== 3. Autoboxing Overhead Benchmark ===");
        long t1 = System.nanoTime();
        long primitiveSum = 0L;
        for (int i = 0; i < 2_000_000; i++) {
            primitiveSum += i;
        }
        long primitiveMs = (System.nanoTime() - t1) / 1_000_000;

        long t2 = System.nanoTime();
        Long boxedSum = 0L; // Unboxes and re-boxes on EVERY iteration!
        for (int i = 0; i < 2_000_000; i++) {
            boxedSum += i;
        }
        long boxedMs = (System.nanoTime() - t2) / 1_000_000;

        System.out.println("Primitive long sum (" + primitiveSum + "): " + primitiveMs + " ms");
        System.out.println("Boxed Long sum     (" + boxedSum + "): " + boxedMs + " ms");
    }
}
