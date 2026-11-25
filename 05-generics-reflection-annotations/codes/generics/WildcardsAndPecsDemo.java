package generics;

import java.util.ArrayList;
import java.util.List;

/**
 * GENERIC INVARIANCE, WILDCARDS & PECS (Producer Extends, Consumer Super)
 *
 * WHAT IT IS:
 *   Rules governing subtyping of parameterized types (`? extends T`, `? super T`, `<?>`).
 *
 * WHY IT MATTERS:
 *   Java arrays are covariant (`Number[] = new Integer[2]`), leading to runtime `ArrayStoreException`.
 *   Java Generics are INVARIANT (`List<Number>` != `List<Integer>`), forcing compile-time safety
 *   while PECS wildcards restore clean API flexibility!
 */
public class WildcardsAndPecsDemo {

    // ❌ BAD: Covariant array allows storing a Double into an Integer[] at compile time, crashing at runtime!
    private static void badCovariantArrayTrap() {
        Number[] nums = new Integer[2];
        try {
            nums[0] = 3.14; // Compiles cleanly, CRASHES at runtime!
        } catch (ArrayStoreException ex) {
            System.out.println("[BAD Array Trap] Runtime ArrayStoreException: " + ex.getMessage());
        }
    }

    // ✅ GOOD (PECS - Producer Extends): Reads Number values out safely; forbids writes (except null)
    public static double sumAll(List<? extends Number> producer) {
        double total = 0.0;
        for (Number n : producer) {
            total += n.doubleValue();
        }
        return total;
    }

    // ✅ GOOD (PECS - Consumer Super): Accepts List<Integer>, List<Number>, or List<Object> as write sink!
    public static void appendSequence(List<? super Integer> consumer, int count) {
        for (int i = 1; i <= count; i++) {
            consumer.add(i * 10);
        }
    }

    // Unbounded wildcard <?> when reading Object metadata only (e.g., printing size/elements)
    public static void printAnyList(List<?> unknownList) {
        System.out.println("List size=" + unknownList.size() + " contents=" + unknownList);
    }

    public static void main(String[] args) {
        System.out.println("=== 1. Array Covariance Crash vs Generic Invariance ===");
        badCovariantArrayTrap();

        System.out.println("\n=== 2. PECS: Producer Extends (Read-Only) ===");
        List<Integer> ints = List.of(10, 20, 30);
        List<Double> doubles = List.of(1.5, 2.5);
        System.out.println("sumAll(List<Integer>): " + sumAll(ints));
        System.out.println("sumAll(List<Double>):  " + sumAll(doubles));

        System.out.println("\n=== 3. PECS: Consumer Super (Write Sink) ===");
        List<Number> numbersSink = new ArrayList<>();
        List<Object> objectsSink = new ArrayList<>();
        appendSequence(numbersSink, 3);
        appendSequence(objectsSink, 2);
        printAnyList(numbersSink);
        printAnyList(objectsSink);
    }
}
