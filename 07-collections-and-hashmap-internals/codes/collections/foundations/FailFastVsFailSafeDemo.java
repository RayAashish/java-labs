package collections.foundations;

import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * FailFastVsFailSafeDemo — Deep Dive into Iterator Modification Contracts & Memory Internals.
 *
 * Demonstrates:
 * 1. Fail-Fast Mechanics: ArrayList throwing ConcurrentModificationException on structural modification.
 * 2. Deep Reflection Analysis of modCount vs expectedModCount internal fields.
 * 3. The Classic "Second-to-Last Element" Interview Anomaly (how CME is silently masked).
 * 4. Safe In-Flight Removal via Iterator.remove() and its internal modCount resynchronization.
 * 5. Fail-Safe Snapshot Mechanics: CopyOnWriteArrayList array-reference swapping.
 * 6. Weakly Consistent Iteration: ConcurrentHashMap dynamic bin traversal.
 *
 * Author: raysi
 */
public class FailFastVsFailSafeDemo {

    public static void main(String[] args) {
        System.out.println("======================================================================");
        System.out.println(" 1. FAIL-FAST: STRUCTURAL MODIFICATION & REFLECTION ON MODCOUNT");
        System.out.println("======================================================================");
        demonstrateFailFastWithReflection();

        System.out.println("\n======================================================================");
        System.out.println(" 2. THE CLASSIC INTERVIEW ANOMALY: REMOVING SECOND-TO-LAST ELEMENT");
        System.out.println("======================================================================");
        demonstrateSecondToLastElementBug();

        System.out.println("\n======================================================================");
        System.out.println(" 3. SAFE IN-FLIGHT REMOVAL VIA ITERATOR.REMOVE()");
        System.out.println("======================================================================");
        demonstrateSafeIteratorRemove();

        System.out.println("\n======================================================================");
        System.out.println(" 4. FAIL-SAFE SNAPSHOT: COPYONWRITEARRAYLIST");
        System.out.println("======================================================================");
        demonstrateCopyOnWriteSnapshot();

        System.out.println("\n======================================================================");
        System.out.println(" 5. WEAKLY CONSISTENT ITERATOR: CONCURRENTHASHMAP");
        System.out.println("======================================================================");
        demonstrateWeaklyConsistentIterator();
    }

    /**
     * Demonstrates standard fail-fast behavior and inspects internal fields using Java reflection.
     */
    private static void demonstrateFailFastWithReflection() {
        List<String> list = new ArrayList<>();
        list.add("Node-1");
        list.add("Node-2");
        list.add("Node-3");

        System.out.println("Initial ArrayList modCount: " + formatField(getModCount(list)));

        Iterator<String> it = list.iterator();
        System.out.println("Iterator created. expectedModCount: " + formatField(getExpectedModCount(it)));
        System.out.println("Iterator cursor: " + formatField(getCursor(it)));

        System.out.println("\nAdvancing iterator: " + it.next());
        System.out.println("After it.next() -> cursor=" + formatField(getCursor(it)) + ", lastRet=" + formatField(getLastRet(it)));

        // Structural modification: Direct list addition
        System.out.println("\nPerforming structural mutation: list.add(\"Node-4\")...");
        list.add("Node-4");

        System.out.println("New ArrayList modCount: " + formatField(getModCount(list)));
        System.out.println("Iterator expectedModCount remains: " + formatField(getExpectedModCount(it)));

        try {
            System.out.println("Calling it.next() with mismatched modCount...");
            it.next(); // Triggers checkForComodification() -> CME
            System.err.println("ERROR: Expected ConcurrentModificationException was not thrown!");
        } catch (ConcurrentModificationException e) {
            System.out.println("SUCCESS: Caught expected ConcurrentModificationException!");
            System.out.println("  Cause: modCount (" + formatField(getModCount(list)) +
                               ") != expectedModCount (" + formatField(getExpectedModCount(it)) + ")");
        }
    }

    /**
     * Demonstrates the infamous Java interview trap:
     * Removing the second-to-last element in an enhanced for-loop does NOT throw ConcurrentModificationException,
     * but quietly corrupts traversal by skipping the last element!
     */
    private static void demonstrateSecondToLastElementBug() {
        List<String> list = new ArrayList<>(Arrays.asList("Alpha", "Beta", "Gamma"));
        System.out.println("Original List: " + list);
        System.out.println("Goal: Remove 'Beta' (the second-to-last element, index 1 of 3) inside enhanced for-loop");

        int iterations = 0;
        try {
            for (String item : list) {
                iterations++;
                System.out.println("  Visiting: " + item);
                if ("Beta".equals(item)) {
                    System.out.println("  Found 'Beta'! Calling list.remove(item)...");
                    list.remove(item); // STRUCTURAL MUTATION!
                }
            }
            System.out.println("TRAP TRIGGERED: Loop finished WITHOUT ConcurrentModificationException!");
            System.out.println("Total iterations completed: " + iterations + " (Expected 3, but Gamma was SKIPPED!)");
            System.out.println("List state after loop: " + list);
            System.out.println("WHY: When 'Beta' was removed, list size dropped from 3 to 2.");
            System.out.println("     Iterator cursor was already 2.");
            System.out.println("     On next loop check: hasNext() evaluated (cursor != size) -> (2 != 2) -> FALSE!");
            System.out.println("     The loop terminated before it.next() could execute checkForComodification().");
        } catch (ConcurrentModificationException e) {
            System.out.println("Caught CME (unexpected for second-to-last element)!");
        }

        // Contrast: Removing the FIRST element throws CME immediately!
        List<String> list2 = new ArrayList<>(Arrays.asList("Alpha", "Beta", "Gamma"));
        try {
            for (String item : list2) {
                if ("Alpha".equals(item)) {
                    list2.remove(item);
                }
            }
        } catch (ConcurrentModificationException e) {
            System.out.println("\nCONTRAST: Removing 'Alpha' (first element) immediately threw CME as expected: " + e.getClass().getSimpleName());
        }
    }

    /**
     * Demonstrates how Iterator.remove() safely removes elements without breaking the iterator.
     */
    private static void demonstrateSafeIteratorRemove() {
        List<String> list = new ArrayList<>(Arrays.asList("10", "20", "30", "40"));
        Iterator<String> it = list.iterator();

        System.out.println("Before removals: list=" + list + ", modCount=" + formatField(getModCount(list)));

        while (it.hasNext()) {
            String val = it.next();
            if ("20".equals(val) || "30".equals(val)) {
                System.out.println("Removing [" + val + "] safely via it.remove()...");
                it.remove();
                System.out.println("  list=" + list);
                System.out.println("  modCount=" + formatField(getModCount(list)) +
                                   ", expectedModCount=" + formatField(getExpectedModCount(it)));
                System.out.println("  cursor=" + formatField(getCursor(it)) +
                                   ", lastRet=" + formatField(getLastRet(it)) + " (reset to -1!)");
            }
        }
        System.out.println("Final list: " + list);
    }

    /**
     * Demonstrates Fail-Safe snapshot semantics with CopyOnWriteArrayList.
     *
     * MEMORY MECHANICS:
     * - CopyOnWriteArrayList holds a volatile reference to an array: private transient volatile Object[] array;
     * - Creating an iterator grabs a direct local reference to the CURRENT array snapshot.
     * - Mutations allocate a brand NEW array of length (len + 1), copy elements via Arrays.copyOf,
     *   place the new element, and atomically swap the volatile array pointer using a ReentrantLock.
     * - Existing iterators continue iterating over their immutable array snapshot in complete isolation!
     */
    private static void demonstrateCopyOnWriteSnapshot() {
        CopyOnWriteArrayList<String> cowList = new CopyOnWriteArrayList<>(Arrays.asList("Snap-1", "Snap-2", "Snap-3"));

        // Grab iterator (holds snapshot reference to original array)
        Iterator<String> snapshotIt = cowList.iterator();

        System.out.println("Snapshot iterator created on: " + cowList);

        // Mutate collection heavily while iterator is alive
        cowList.add("Snap-4");
        cowList.remove("Snap-1");
        cowList.add("Snap-5");

        System.out.println("Backing CopyOnWriteArrayList after mutations: " + cowList);

        System.out.print("Snapshot Iterator traversing original state: ");
        while (snapshotIt.hasNext()) {
            System.out.print("[" + snapshotIt.next() + "] ");
        }
        System.out.println("\nPASSED: Iterator saw purely the original immutable snapshot without CME!");

        // Iterator mutation restriction
        try {
            Iterator<String> it = cowList.iterator();
            it.next();
            it.remove(); // Must throw UnsupportedOperationException
        } catch (UnsupportedOperationException e) {
            System.out.println("PASSED: CopyOnWriteArrayList iterator does not permit removal (read-only snapshot).");
        }
    }

    /**
     * Demonstrates Weakly Consistent iteration in ConcurrentHashMap.
     *
     * CONTRACT:
     * - Never throws ConcurrentModificationException.
     * - Traverses elements as they existed at iterator construction.
     * - May (or may not) reflect concurrent updates made during traversal without locking the map.
     */
    private static void demonstrateWeaklyConsistentIterator() {
        ConcurrentHashMap<String, Integer> map = new ConcurrentHashMap<>();
        map.put("Key-A", 1);
        map.put("Key-B", 2);
        map.put("Key-C", 3);

        Iterator<Map.Entry<String, Integer>> it = map.entrySet().iterator();

        System.out.println("ConcurrentHashMap iterator created.");

        // Read first entry
        if (it.hasNext()) {
            System.out.println("First element read: " + it.next());
        }

        // Mutate map concurrently during iteration
        map.put("Key-D", 4);
        map.remove("Key-B");
        map.put("Key-E", 5);

        System.out.println("Map mutated concurrently during active iteration.");
        System.out.print("Remaining elements traversed by iterator: ");
        while (it.hasNext()) {
            System.out.print(it.next() + " ");
        }
        System.out.println();
        System.out.println("PASSED: Weakly consistent traversal completed cleanly with zero locking or CME!");
    }

    // =========================================================================
    // REFLECTION UTILITIES FOR JDK INTERNALS INSPECTION
    // =========================================================================

    private static String formatField(Integer val) {
        return val != null ? val.toString() : "[Inaccessible under strict JPMS]";
    }

    private static Integer getModCount(List<?> list) {
        return getDeepField(list, "modCount");
    }

    private static Integer getExpectedModCount(Iterator<?> it) {
        return getDeepField(it, "expectedModCount");
    }

    private static Integer getCursor(Iterator<?> it) {
        return getDeepField(it, "cursor");
    }

    private static Integer getLastRet(Iterator<?> it) {
        return getDeepField(it, "lastRet");
    }

    private static Integer getDeepField(Object target, String fieldName) {
        Class<?> curr = target.getClass();
        while (curr != null) {
            try {
                Field f = curr.getDeclaredField(fieldName);
                f.setAccessible(true);
                return (Integer) f.get(target);
            } catch (NoSuchFieldException e) {
                curr = curr.getSuperclass();
            } catch (Throwable t) {
                // Returns null if JVM module encapsulation prevents access without --add-opens
                return null;
            }
        }
        return null;
    }
}
