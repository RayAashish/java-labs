package collections.list;

import java.util.*;
import java.util.function.UnaryOperator;

/**
 * Demonstrates deep behavioral semantics, memory mechanics, and edge-case traps
 * of the core java.util.List interface methods in modern Java (JDK 17/21+).
 *
 * Concepts covered:
 * 1. Positional access, zero-indexing, search (indexOf/lastIndexOf)
 * 2. subList() non-detached view mechanics, fail-fast invalidation
 * 3. List.of() vs Arrays.asList() vs ArrayList mutability and memory differences
 * 4. Java 8 default methods (replaceAll, sort)
 * 5. Primitive overload ambiguity: remove(int) vs remove(Object)
 *
 * @author raysi
 */
public class ListInterfaceMethodsDemo {

    public static void main(String[] args) {
        System.out.println("===============================================================");
        System.out.println("   JAVA COLLECTIONS FRAMEWORK: LIST INTERFACE DEEP DIVE        ");
        System.out.println("   Author: raysi                                              ");
        System.out.println("===============================================================\n");

        demoPositionalAccessAndSearch();
        demoSubListViewSemantics();
        demoFactoryMethodDifferences();
        demoDefaultMethodsSortAndReplaceAll();
        demoRemoveOverloadAmbiguity();
    }

    /**
     * Demonstrates positional access: get, set, add(index, E) and search methods.
     */
    private static void demoPositionalAccessAndSearch() {
        System.out.println("--- 1. Positional Access & Search Semantics ---");
        List<String> list = new ArrayList<>();
        list.add("Apple");
        list.add("Banana");
        list.add("Cherry");
        list.add("Banana");
        list.add("Date");

        // Positional retrieval: O(1) in ArrayList
        System.out.println("Element at index 2: " + list.get(2)); // Cherry

        // In-place replacement via set(index, element): returns previously stored element
        String previous = list.set(1, "Blueberry");
        System.out.println("Replaced element: " + previous + " -> New element at index 1: " + list.get(1));

        // Positional insertion: shifts elements right starting from index
        list.add(2, "Boysenberry");
        System.out.println("List after add(2, 'Boysenberry'): " + list);

        // Search: indexOf finds first occurrence, lastIndexOf finds last occurrence
        int firstBananaIndex = list.indexOf("Banana");
        int lastBananaIndex = list.lastIndexOf("Banana");
        System.out.println("First occurrence of 'Banana': index " + firstBananaIndex);
        System.out.println("Last occurrence of 'Banana': index " + lastBananaIndex);
        System.out.println("Non-existent element index: " + list.indexOf("Dragonfruit")); // -1
        System.out.println();
    }

    /**
     * Demonstrates that subList is a windowed view onto the parent collection,
     * not a detached copy. Mutations synchronize bidirectionally, but structural
     * mutations to the parent list directly will invalidate the subList view.
     */
    private static void demoSubListViewSemantics() {
        System.out.println("--- 2. subList() View Semantics & Fail-Fast Behavior ---");
        List<String> master = new ArrayList<>(List.of("Zero", "One", "Two", "Three", "Four", "Five"));
        System.out.println("Original Master: " + master);

        // Create range view: fromIndex (inclusive) to toIndex (exclusive)
        // Memory note: No new Object[] is created! subList holds a reference to master list
        List<String> sub = master.subList(1, 4); // ["One", "Two", "Three"]
        System.out.println("Initial SubList [1, 4): " + sub);

        // 1. Modifying elements via subList affects master directly
        sub.set(0, "ONE_MUTATED");
        System.out.println("Master after sub.set(0, ...): " + master);

        // 2. Structural modification on subList reflects in master
        sub.add("INSERTED_IN_SUB");
        System.out.println("SubList after sub.add(): " + sub);
        System.out.println("Master after sub.add(): " + master);

        // 3. Direct modification of parent invalidates subList (ConcurrentModificationException)
        master.add("Six");
        System.out.println("Master modified directly with add('Six').");

        try {
            // Any operation on subList now checks parent.modCount != sub.modCount
            sub.get(0);
            System.err.println("ERROR: Expected ConcurrentModificationException!");
        } catch (ConcurrentModificationException e) {
            System.out.println("Caught expected ConcurrentModificationException! Parent modification invalidated subList.");
        }
        System.out.println();
    }

    /**
     * Contrasts List.of() vs Arrays.asList() vs new ArrayList<>():
     * - Mutability
     * - Resizability
     * - Null acceptance
     */
    private static void demoFactoryMethodDifferences() {
        System.out.println("--- 3. Factory Method Comparison (List.of vs Arrays.asList vs ArrayList) ---");

        // 1. Arrays.asList(T...)
        // Backed directly by the provided array. Fixed length. Mutable elements. Allows null.
        String[] array = {"Alpha", "Bravo", "Charlie"};
        List<String> arraysAsList = Arrays.asList(array);
        arraysAsList.set(1, "BRAVO_UPDATED");
        System.out.println("Arrays.asList mutated array element [1]: " + array[1]); // Backing array changed!

        try {
            arraysAsList.add("Delta"); // Structural modification forbidden
        } catch (UnsupportedOperationException e) {
            System.out.println("Arrays.asList rejected add(): Fixed-size wrapper.");
        }

        // 2. List.of(...) (JDK 9+)
        // Completely immutable, null-hostile, optimized memory layout (no backing array for <= 2 elements).
        List<String> listOf = List.of("X-Ray", "Yankee", "Zulu");
        try {
            listOf.set(0, "X");
        } catch (UnsupportedOperationException e) {
            System.out.println("List.of rejected set(): Truly immutable.");
        }

        try {
            List.of("Valid", null);
        } catch (NullPointerException e) {
            System.out.println("List.of rejected null element with NullPointerException.");
        }

        // 3. standard new ArrayList<>()
        // Fully mutable, fully resizable, allows nulls
        List<String> standardList = new ArrayList<>();
        standardList.add(null);
        standardList.add("Valid");
        standardList.set(0, "FixedNull");
        System.out.println("ArrayList fully supports mutations and nulls: " + standardList);
        System.out.println();
    }

    /**
     * Demonstrates Java 8 default methods on List:
     * - replaceAll(UnaryOperator)
     * - sort(Comparator)
     */
    private static void demoDefaultMethodsSortAndReplaceAll() {
        System.out.println("--- 4. Java 8 Default Methods: replaceAll() and sort() ---");
        List<String> tokens = new ArrayList<>(List.of("delta", "alpha", "charlie", "bravo", "echo"));
        System.out.println("Initial tokens: " + tokens);

        // replaceAll transforms elements in-place via ListIterator
        UnaryOperator<String> toUpperAndPrefix = s -> "ID_" + s.toUpperCase(Locale.ROOT);
        tokens.replaceAll(toUpperAndPrefix);
        System.out.println("After replaceAll: " + tokens);

        // List.sort(Comparator) sorts in-place using TimSort without auxiliary list allocation
        tokens.sort(Comparator.naturalOrder());
        System.out.println("After sort (natural): " + tokens);

        // Sort descending by length or custom criteria
        tokens.sort(Comparator.reverseOrder());
        System.out.println("After sort (reverse): " + tokens);
        System.out.println();
    }

    /**
     * Highlights the subtle overload pitfall when calling remove() on a List of Integers.
     */
    private static void demoRemoveOverloadAmbiguity() {
        System.out.println("--- 5. Overload Trap: remove(int) vs remove(Object) ---");
        List<Integer> numbers = new ArrayList<>();
        numbers.add(100);
        numbers.add(200);
        numbers.add(300);
        numbers.add(400);

        System.out.println("Numbers list: " + numbers);

        // Ambiguity Trap: Does remove(1) remove value 1 or index 1?
        // Primitive literal 1 matches remove(int index) without autoboxing.
        Integer removedItem = numbers.remove(1);
        System.out.println("numbers.remove(1) invoked remove(int index). Removed element: " + removedItem);
        System.out.println("Numbers list after remove(1): " + numbers); // [100, 300, 400]

        // To remove the object by value, explicit boxing is required:
        boolean removedByValue = numbers.remove(Integer.valueOf(300));
        System.out.println("numbers.remove(Integer.valueOf(300)) invoked remove(Object). Result: " + removedByValue);
        System.out.println("Numbers list after value removal: " + numbers); // [100, 400]
        System.out.println();
    }
}
