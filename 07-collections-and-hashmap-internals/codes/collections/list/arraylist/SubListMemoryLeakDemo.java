package collections.list.arraylist;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.List;

/**
 * Demonstrates the hidden JVM memory leak (object retention) hazard and fail-fast
 * invalidation mechanics associated with ArrayList.subList().
 *
 * Key Architectural Findings:
 * 1. SubList is NOT a detached copy; it holds a hard reference to the root ArrayList.
 * 2. If a massive ArrayList (e.g. millions of elements) is referenced only through a tiny
 *    2-element subList, the ENTIRE backing array is kept alive in the heap, blocking GC.
 * 3. Bidirectional mutation: mutating subList updates parent, and vice versa.
 * 4. Structural modification on parent increments root modCount, causing subList operations
 *    to fail immediately with ConcurrentModificationException.
 *
 * @author raysi
 */
public class SubListMemoryLeakDemo {

    public static void main(String[] args) throws InterruptedException {
        System.out.println("===============================================================");
        System.out.println("   ARRAYLIST SUBLIST MEMORY RETENTION & CME DEMO               ");
        System.out.println("   Author: raysi                                              ");
        System.out.println("===============================================================\n");

        demonstrateSubListMemoryRetentionHazard();
        demonstrateBidirectionalMutation();
        demonstrateParentMutationInvalidatesSubList();
    }

    /**
     * Demonstrates how holding a reference to a 2-element subList prevents
     * the Garbage Collector from freeing the underlying 1,000,000-element parent list.
     */
    private static void demonstrateSubListMemoryRetentionHazard() throws InterruptedException {
        System.out.println("--- 1. SubList Memory Retention (GC Leak) Hazard ---");

        WeakReference<ArrayList<byte[]>> parentWeakRef;
        List<byte[]> subListView;

        // Scope block where parent is instantiated and populated
        {
            // Allocate a parent list holding 100 large byte arrays (~100 MB of heap)
            ArrayList<byte[]> parentList = new ArrayList<>();
            for (int i = 0; i < 100; i++) {
                parentList.add(new byte[1024 * 1024]); // 1 MB payload each
            }

            // Create a WeakReference to track whether the parent list object gets GC'd
            parentWeakRef = new WeakReference<>(parentList);

            // Extract a tiny 2-element subList view
            // SubList has: private final ArrayList<E> root;
            subListView = parentList.subList(0, 2);

            System.out.printf("Parent created with %d elements (~100 MB).\n", parentList.size());
            System.out.printf("SubList created with %d elements.\n", subListView.size());

            // Discard parentList local reference
            parentList = null;
        }

        System.out.println("Parent reference dropped out of scope, but subListView is retained.");
        System.out.println("Requesting GC...");
        System.gc();
        Thread.sleep(100);

        // Check if parent is still alive
        if (parentWeakRef.get() != null) {
            System.out.println("CRITICAL HAZARD DETECTED: Parent ArrayList is STILL rooted in heap!");
            System.out.println("Reason: subListView holds an internal 'root' field pointing to parent,");
            System.out.println("preventing the entire 100 MB array buffer from being garbage collected.");
        } else {
            System.out.println("Parent was unexpectedly collected.");
        }

        // Solution: Create a detached defensive copy
        List<byte[]> safeDetachedList = new ArrayList<>(subListView);
        subListView = null; // Discard subList view

        System.out.println("\nSubList view discarded and replaced with detached 'new ArrayList<>(subList)'.");
        System.out.println("Requesting GC second time...");
        System.gc();
        Thread.sleep(100);

        if (parentWeakRef.get() == null) {
            System.out.println("SUCCESS: Parent ArrayList and all 100 MB buffers were successfully GC'd!");
        } else {
            System.out.println("Parent still alive.");
        }
        System.out.println();
    }

    /**
     * Demonstrates bidirectional mutation through the subList view.
     */
    private static void demonstrateBidirectionalMutation() {
        System.out.println("--- 2. Bidirectional Mutation Mechanics ---");
        ArrayList<String> master = new ArrayList<>();
        master.add("Alfa");
        master.add("Bravo");
        master.add("Charlie");
        master.add("Delta");

        System.out.println("Original Master: " + master);

        // Sublist covering "Bravo" and "Charlie"
        List<String> sub = master.subList(1, 3);
        System.out.println("SubList [1, 3): " + sub);

        // 1. Mutate via subList.set()
        sub.set(0, "BRAVO_EDITED");
        System.out.println("Master after sub.set(0, 'BRAVO_EDITED'): " + master);

        // 2. Add via subList.add()
        sub.add(1, "BRAVO_SUB_INSERT");
        System.out.println("Sub after sub.add(1, ...): " + sub);
        System.out.println("Master after sub.add(1, ...): " + master);

        // 3. Clear via subList.clear() - removes sub-range from parent!
        sub.clear();
        System.out.println("Sub after sub.clear(): " + sub);
        System.out.println("Master after sub.clear(): " + master);
        System.out.println();
    }

    /**
     * Demonstrates why direct structural modification to parent list breaks subList.
     */
    private static void demonstrateParentMutationInvalidatesSubList() {
        System.out.println("--- 3. Direct Parent Mutation Invalidates SubList (CME) ---");
        ArrayList<String> parent = new ArrayList<>();
        parent.add("One");
        parent.add("Two");
        parent.add("Three");

        List<String> sub = parent.subList(0, 2);
        System.out.println("Parent: " + parent + " | Sub: " + sub);

        // Directly modify parent structurally
        System.out.println("Adding 'Four' directly to parent list...");
        parent.add("Four"); // increments parent.modCount

        try {
            System.out.println("Attempting to access sub.get(0)...");
            sub.get(0); // Checks: root.modCount != this.modCount
            System.err.println("ERROR: Expected CME did not fire!");
        } catch (ConcurrentModificationException e) {
            System.out.println("Caught Expected ConcurrentModificationException!");
            System.out.println("Internal Cause: SubList detected that parent.modCount no longer matches");
            System.out.println("the expectedModCount captured at subList creation time.");
        }
        System.out.println();
    }
}
