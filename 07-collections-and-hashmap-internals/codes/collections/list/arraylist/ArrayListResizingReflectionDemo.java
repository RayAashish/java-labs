package collections.list.arraylist;

import java.lang.reflect.Field;
import java.util.ArrayList;

/**
 * Demonstrates internal resizing mechanics, capacity transitions, and memory reclamation
 * in java.util.ArrayList using Reflection to inspect the private 'elementData' array buffer.
 *
 * NOTE FOR JDK 17/21+:
 * Because JDK 17+ strongly encapsulates JDK internals (JEP 403), run this program with:
 * --add-opens java.base/java.util=ALL-UNNAMED
 * E.g.: java --add-opens java.base/java.util=ALL-UNNAMED com.raysi.jcf.list.arraylist.codes.ArrayListResizingReflectionDemo
 *
 * Concepts demonstrated:
 * 1. Lazy initialization: initial capacity is 0 (DEFAULTCAPACITY_EMPTY_ELEMENTDATA)
 * 2. Expansion upon 1st insertion: jumps directly to DEFAULT_CAPACITY (10)
 * 3. 1.5x expansion progression: 10 -> 15 -> 22 -> 33 -> 49 ...
 * 4. trimToSize() effect: reclaims slack capacity by trimming array to exact size
 * 5. ensureCapacity() optimization: avoids multiple array reallocations upfront
 *
 * @author raysi
 */
public class ArrayListResizingReflectionDemo {

    private static Field elementDataField;
    private static boolean reflectionAvailable = false;

    static {
        try {
            elementDataField = ArrayList.class.getDeclaredField("elementData");
            elementDataField.setAccessible(true);
            reflectionAvailable = true;
        } catch (Throwable t) {
            System.err.println("[NOTE] JVM Strong Encapsulation prevented reflective access to elementData.");
            System.err.println("[NOTE] Please run with: --add-opens java.base/java.util=ALL-UNNAMED");
            reflectionAvailable = false;
        }
    }

    public static void main(String[] args) {
        System.out.println("===============================================================");
        System.out.println("   ARRAYLIST RESIZING & CAPACITY REFLECTION DEMO               ");
        System.out.println("   Author: raysi                                              ");
        System.out.println("===============================================================\n");

        if (!reflectionAvailable) {
            System.out.println("Running in simulation mode with JDK source code formulas since reflection was blocked.\n");
        }

        demonstrateLazyInitializationAndIncrementalGrowth();
        demonstrateExplicitCapacityZeroVsDefault();
        demonstrateEnsureCapacityOptimization();
        demonstrateTrimToSizeReclamation();
    }

    /**
     * Inspects the length of the internal elementData array.
     */
    private static int getInternalCapacity(ArrayList<?> list) {
        if (reflectionAvailable) {
            try {
                Object[] data = (Object[]) elementDataField.get(list);
                return data.length;
            } catch (Exception e) {
                throw new RuntimeException("Reflection read failed", e);
            }
        }
        return -1; // Fallback
    }

    private static void demonstrateLazyInitializationAndIncrementalGrowth() {
        System.out.println("--- 1. Lazy Initialization & Capacity Growth (1.5x) ---");
        ArrayList<Integer> list = new ArrayList<>();

        printStatus("After 'new ArrayList<>()' [Lazy Init]", list);

        // Add 1st element: triggers initial inflation to DEFAULT_CAPACITY (10)
        list.add(100);
        printStatus("After adding 1st element", list);

        // Fill up through capacity 10
        for (int i = 2; i <= 10; i++) {
            list.add(i * 100);
        }
        printStatus("After adding 10th element (buffer full)", list);

        // 11th element triggers grow(): 10 + (10 >> 1) = 15
        list.add(1100);
        printStatus("After adding 11th element (triggers 1.5x grow -> 15)", list);

        // Fill up through capacity 15
        for (int i = 12; i <= 15; i++) {
            list.add(i * 100);
        }
        printStatus("After adding 15th element (buffer full)", list);

        // 16th element triggers grow(): 15 + (15 >> 1) = 22
        list.add(1600);
        printStatus("After adding 16th element (triggers 1.5x grow -> 22)", list);

        // Adding more elements up to 22
        for (int i = 17; i <= 22; i++) {
            list.add(i * 100);
        }
        printStatus("After adding 22nd element (buffer full)", list);

        // 23rd element triggers grow(): 22 + (22 >> 1) = 33
        list.add(2300);
        printStatus("After adding 23rd element (triggers 1.5x grow -> 33)", list);
        System.out.println();
    }

    private static void demonstrateExplicitCapacityZeroVsDefault() {
        System.out.println("--- 2. new ArrayList<>() vs new ArrayList<>(0) ---");
        ArrayList<String> defaultList = new ArrayList<>();
        ArrayList<String> explicitZeroList = new ArrayList<>(0);

        printStatus("defaultList initial", defaultList);
        printStatus("explicitZeroList initial", explicitZeroList);

        // Add 1 element to both
        defaultList.add("A");
        explicitZeroList.add("A");

        printStatus("defaultList after 1st add (inflates to 10)", defaultList);
        printStatus("explicitZeroList after 1st add (inflates to 1 only!)", explicitZeroList);

        explicitZeroList.add("B");
        printStatus("explicitZeroList after 2nd add (grows to 2)", explicitZeroList);

        explicitZeroList.add("C");
        printStatus("explicitZeroList after 3rd add (grows to 3)", explicitZeroList);
        System.out.println();
    }

    private static void demonstrateEnsureCapacityOptimization() {
        System.out.println("--- 3. Pre-allocation via ensureCapacity() ---");
        ArrayList<Integer> naiveList = new ArrayList<>();
        ArrayList<Integer> optimizedList = new ArrayList<>();

        int targetElements = 1_000_000;

        // Warm-up JIT compiler
        for (int w = 0; w < 5; w++) {
            ArrayList<Integer> warmup = new ArrayList<>();
            for (int i = 0; i < 50_000; i++) warmup.add(i);
        }

        // Naive addition (triggers repeated array allocations: 10, 15, 22, 33, 49, 73, 109...)
        long startNaive = System.nanoTime();
        for (int i = 0; i < targetElements; i++) {
            naiveList.add(i);
        }
        long naiveDuration = System.nanoTime() - startNaive;

        // Optimized addition: pre-size backing array
        long startOptimized = System.nanoTime();
        optimizedList.ensureCapacity(targetElements);
        for (int i = 0; i < targetElements; i++) {
            optimizedList.add(i);
        }
        long optimizedDuration = System.nanoTime() - startOptimized;

        System.out.printf("Naive 1M additions time      : %,d ns\n", naiveDuration);
        System.out.printf("Optimized 1M additions time  : %,d ns\n", optimizedDuration);
        System.out.printf("Speedup with ensureCapacity  : %.2fx faster\n", (double) naiveDuration / optimizedDuration);
        if (reflectionAvailable) {
            System.out.println("naiveList final capacity: " + getInternalCapacity(naiveList));
            System.out.println("optimizedList final capacity: " + getInternalCapacity(optimizedList));
        }
        System.out.println();
    }

    private static void demonstrateTrimToSizeReclamation() {
        System.out.println("--- 4. trimToSize() Reclaiming Slack Capacity ---");
        ArrayList<String> largeList = new ArrayList<>(100_000);
        for (int i = 0; i < 50; i++) {
            largeList.add("Element " + i);
        }

        printStatus("largeList after adding 50 items (pre-allocated 100,000 slots)", largeList);

        // trimToSize reclaims the 99,950 unused null slots!
        largeList.trimToSize();
        printStatus("largeList after trimToSize()", largeList);
        System.out.println();
    }

    private static void printStatus(String label, ArrayList<?> list) {
        if (reflectionAvailable) {
            int capacity = getInternalCapacity(list);
            int size = list.size();
            int slack = capacity - size;
            System.out.printf("%-65s | Size: %6d | Capacity: %6d | Slack (Wasted): %6d slots\n",
                    label, size, capacity, slack);
        } else {
            System.out.printf("%-65s | Size: %6d\n", label, list.size());
        }
    }
}
