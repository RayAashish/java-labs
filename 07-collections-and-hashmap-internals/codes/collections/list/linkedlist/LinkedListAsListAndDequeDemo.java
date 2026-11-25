package collections.list.linkedlist;

import java.util.*;

/**
 * Demonstrates the dual-personality architectural contract of java.util.LinkedList:
 * 1. LinkedList operated as a positional List (get, set, add(index))
 * 2. LinkedList operated as a double-ended Queue (Deque: offerFirst, offerLast, pollFirst, pollLast)
 * 3. Reverse traversal using descendingIterator()
 * 4. Microbenchmark revealing the severe O(n) get(index) traversal penalty
 *
 * @author raysi
 */
public class LinkedListAsListAndDequeDemo {

    public static void main(String[] args) {
        System.out.println("===============================================================");
        System.out.println("   LINKEDLIST: DUAL-PERSONALITY & PERFORMANCE CHARACTERISTICS  ");
        System.out.println("   Author: raysi                                              ");
        System.out.println("===============================================================\n");

        demonstrateListOperations();
        demonstrateDequeOperations();
        demonstrateDescendingIterator();
        demonstrateRandomAccessPenalty();
    }

    /**
     * Demonstrates LinkedList fulfilling the java.util.List contract.
     * Note: Indexed operations require O(n) node traversal via node(index).
     */
    private static void demonstrateListOperations() {
        System.out.println("--- 1. LinkedList as a List (Positional Indexed Semantics) ---");
        List<String> list = new LinkedList<>();

        // Append to tail: O(1) via linkLast
        list.add("Java");
        list.add("Kotlin");
        list.add("Scala");
        System.out.println("Initial List: " + list);

        // Positional insertion: O(n) traversal via node(index) + O(1) pointer adjustment
        list.add(1, "Groovy");
        System.out.println("After add(1, 'Groovy'): " + list);

        // Positional replacement: O(n) traversal + O(1) item update
        String old = list.set(2, "Clojure");
        System.out.println("Replaced element: " + old + " -> New List: " + list);

        // Positional retrieval: O(n) traversal
        System.out.println("Element at index 3: " + list.get(3));
        System.out.println();
    }

    /**
     * Demonstrates LinkedList fulfilling the java.util.Deque contract:
     * - Head and tail operations operate strictly in O(1) time
     * - No array resizing, no element shifting
     */
    private static void demonstrateDequeOperations() {
        System.out.println("--- 2. LinkedList as a Deque (Double-Ended Queue / Stack) ---");
        Deque<String> deque = new LinkedList<>();

        // 1. Double-Ended Queue Operations: O(1) at both ends
        deque.offerFirst("Task-Head-1");
        deque.offerLast("Task-Tail-1");
        deque.offerFirst("Task-Head-2");
        deque.offerLast("Task-Tail-2");
        System.out.println("Deque state: " + deque);

        System.out.println("pollFirst() -> " + deque.pollFirst()); // Task-Head-2
        System.out.println("pollLast()  -> " + deque.pollLast());  // Task-Tail-2
        System.out.println("Remaining Deque: " + deque);

        // 2. Used as a LIFO Stack (Replacing legacy java.util.Stack)
        Deque<String> stack = new LinkedList<>();
        stack.push("Base Layer");
        stack.push("Middle Layer");
        stack.push("Top Layer");

        System.out.println("\nLIFO Stack peek(): " + stack.peek()); // Top Layer
        while (!stack.isEmpty()) {
            System.out.println("stack.pop(): " + stack.pop());
        }
        System.out.println();
    }

    /**
     * Demonstrates reverse iteration via descendingIterator() without copying.
     */
    private static void demonstrateDescendingIterator() {
        System.out.println("--- 3. Reverse Traversal via descendingIterator() ---");
        LinkedList<String> versions = new LinkedList<>(List.of("JDK 8", "JDK 11", "JDK 17", "JDK 21"));

        System.out.println("Forward order:  " + versions);
        System.out.print("Reverse order:  [");
        Iterator<String> reverseIt = versions.descendingIterator();
        while (reverseIt.hasNext()) {
            System.out.print(reverseIt.next() + (reverseIt.hasNext() ? ", " : ""));
        }
        System.out.println("]\n");
    }

    /**
     * Demonstrates why get(index) on LinkedList is an anti-pattern.
     */
    private static void demonstrateRandomAccessPenalty() {
        System.out.println("--- 4. Random Access Timing Penalty (ArrayList vs LinkedList) ---");
        int size = 50_000;
        ArrayList<Integer> arrayList = new ArrayList<>(size);
        LinkedList<Integer> linkedList = new LinkedList<>();

        for (int i = 0; i < size; i++) {
            arrayList.add(i);
            linkedList.add(i);
        }

        // Test 1: Single get at midpoint
        int midIndex = size / 2;

        long start = System.nanoTime();
        int valA = arrayList.get(midIndex);
        long arrayListGetTime = System.nanoTime() - start;

        start = System.nanoTime();
        int valL = linkedList.get(midIndex);
        long linkedListGetTime = System.nanoTime() - start;

        System.out.printf("ArrayList.get(midpoint)  : %,d ns (direct memory offset O(1))\n", arrayListGetTime);
        System.out.printf("LinkedList.get(midpoint) : %,d ns (walks 25,000 node pointers O(n))\n", linkedListGetTime);
        System.out.printf("Midpoint read slowdown   : %,.1fx slower!\n",
                (double) linkedListGetTime / Math.max(1, arrayListGetTime));

        // Test 2: The classic O(n^2) junior developer bug: indexed for-loop on LinkedList!
        System.out.println("\nTraversing first 2,000 elements via indexed for-loop vs Iterator:");

        start = System.nanoTime();
        long sumIndexed = 0;
        for (int i = 0; i < 2_000; i++) {
            sumIndexed += linkedList.get(i); // node(i) walks from head every single iteration!
        }
        long indexedLoopTime = System.nanoTime() - start;

        start = System.nanoTime();
        long sumIterator = 0;
        int count = 0;
        for (int val : linkedList) {
            sumIterator += val;
            if (++count == 2_000) break;
        }
        long iteratorLoopTime = System.nanoTime() - start;

        System.out.printf("LinkedList indexed for-loop (O(n^2)) : %,d ns\n", indexedLoopTime);
        System.out.printf("LinkedList Iterator loop    (O(n))   : %,d ns\n", iteratorLoopTime);
        System.out.printf("Indexed loop was %,.1fx slower due to redundant pointer walking!\n",
                (double) indexedLoopTime / Math.max(1, iteratorLoopTime));
        System.out.println();
    }
}
