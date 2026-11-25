package generics;

import java.util.ArrayList;
import java.util.List;

/**
 * GENERICS BASICS & BOUNDED TYPE PARAMETERS
 *
 * WHAT IT IS:
 *   Parameterized types (`Box<T>`, `List<String>`) enabling classes and methods to operate
 *   on objects of various types while guaranteeing compile-time type safety.
 *
 * WHY IT MATTERS:
 *   Without Generics, collections hold `Object`, allowing accidental mixed types that only
 *   crash in production with `ClassCastException`.
 *
 * IN THIS FILE:
 *   1. BAD CODE:  Raw `List` silently accepts an Integer and crashes at cast time.
 *   2. GOOD CODE: `List<String>`, generic `Pair<K,V>`, and multi-bounded `<T extends Number & Comparable<T>>`.
 */
public class GenericsBasicsAndBoundsDemo {

    record Pair<K, V>(K key, V value) {}

    // Multiple Bounds: Class bound (Number) MUST precede interface bounds (Comparable)
    public static <T extends Number & Comparable<T>> T maxNumber(T a, T b) {
        return a.compareTo(b) >= 0 ? a : b;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void badRawListCrash() {
        List rawNames = new ArrayList(); // ❌ BAD: Raw type bypasses compiler check!
        rawNames.add("Aashish");
        rawNames.add(404); // Bug silently enters list!
        try {
            String second = (String) rawNames.get(1); // Crashes at runtime!
            System.out.println(second);
        } catch (ClassCastException ex) {
            System.out.println("[BAD] Runtime ClassCastException caught: " + ex.getMessage());
        }
    }

    public static void main(String[] args) {
        System.out.println("=== 1. BAD Raw Type vs GOOD Generic Type Safety ===");
        badRawListCrash();

        List<String> safeNames = new ArrayList<>();
        safeNames.add("Aashish");
        // safeNames.add(404); // ❌ Compile Error caught immediately!
        System.out.println("[GOOD] Type-safe element: " + safeNames.getFirst());

        System.out.println("\n=== 2. Generic Pair & Multiple Bounded Generic Method ===");
        Pair<String, Integer> status = new Pair<>("HTTP_OK", 200);
        System.out.println("Pair: " + status);
        System.out.println("Max Integer (10, 42): " + maxNumber(10, 42));
        System.out.println("Max Double (3.14, 2.71): " + maxNumber(3.14, 2.71));
    }
}
