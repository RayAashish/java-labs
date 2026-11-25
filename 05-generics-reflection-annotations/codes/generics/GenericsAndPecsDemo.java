package generics;

import java.util.ArrayList;
import java.util.List;

/**
 * Demonstrates Generics Invariance, Bounded Type Parameters,
 * PECS (Producer Extends, Consumer Super), and Type Erasure.
 */
public class GenericsAndPecsDemo {

    public static void main(String[] args) {
        System.out.println("=== 1. Bounded Type Parameter ===");
        System.out.println("Max of 10 and 25: " + max(10, 25));
        System.out.println("Max of \"Java\" and \"Go\": " + max("Java", "Go"));

        System.out.println("\n=== 2. PECS: Producer Extends (Read-Only Source) ===");
        List<Integer> ints = List.of(10, 20, 30);
        List<Double> doubles = List.of(1.5, 2.5);
        System.out.println("Sum of ints:    " + sumNumbers(ints));
        System.out.println("Sum of doubles: " + sumNumbers(doubles));

        System.out.println("\n=== 3. PECS: Consumer Super (Write-Safe Sink) ===");
        List<Number> numberSink = new ArrayList<>();
        List<Object> objectSink = new ArrayList<>();
        addIntegers(numberSink, 3);
        addIntegers(objectSink, 2);
        System.out.println("Number sink: " + numberSink);
        System.out.println("Object sink: " + objectSink);

        System.out.println("\n=== 4. Type Erasure Proof ===");
        List<String> strList = new ArrayList<>();
        List<Integer> intList = new ArrayList<>();
        // Both share the exact same erased runtime class (ArrayList.class)!
        System.out.println("Same runtime class? " + (strList.getClass() == intList.getClass()));
    }

    public static <T extends Comparable<T>> T max(T a, T b) {
        return a.compareTo(b) >= 0 ? a : b;
    }

    // Producer: reads Number values out -> use '? extends Number'
    public static double sumNumbers(List<? extends Number> producer) {
        double sum = 0.0;
        for (Number n : producer) {
            sum += n.doubleValue();
        }
        return sum;
    }

    // Consumer: inserts Integer values into list -> use '? super Integer'
    public static void addIntegers(List<? super Integer> consumer, int count) {
        for (int i = 1; i <= count; i++) {
            consumer.add(i * 10);
        }
    }
}
