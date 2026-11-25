package collections.list.vectorandstack;

import java.util.*;

/**
 * Production-grade verification of legacy Vector and Stack design pitfalls:
 * 1. Breaking LIFO contract via inherited Vector methods
 * 2. Single-threaded synchronization overhead benchmarking (Vector vs ArrayList)
 * 3. Compound action check-then-act concurrency race condition
 * 4. ArrayDeque as the modern, high-performance LIFO stack replacement
 *
 * @author raysi
 */
public class LegacyStackPitfallsDemo {

    public static void main(String[] args) {
        System.out.println("===============================================================");
        System.out.println("   VECTOR & STACK: ARCHITECTURAL PITFALLS & PERFORMANCE TAX   ");
        System.out.println("   Author: raysi                                              ");
        System.out.println("===============================================================\n");

        demonstrateBreakingLIFOContract();
        demonstrateSynchronizationOverhead();
        demonstrateArrayDequeReplacement();
        demonstrateCompoundActionRaceHazard();
    }

    /**
     * Demonstrates how Stack violates Liskov Substitution Principle and breaks LIFO
     * by exposing Vector's positional indexed methods.
     */
    private static void demonstrateBreakingLIFOContract() {
        System.out.println("--- 1. Breaking LIFO Contract in java.util.Stack ---");
        Stack<String> stack = new Stack<>();
        stack.push("Bottom");
        stack.push("Middle");
        stack.push("Top");

        System.out.println("Stack before illegal mutation: " + stack);
        System.out.println("Expected pop() order: Top -> Middle -> Bottom");

        // DESIGN FLAW: Stack extends Vector, allowing index-based insertion at index 0 (bottom)!
        stack.add(0, "ALIEN_INTRUDER_AT_BOTTOM");
        System.out.println("Stack after stack.add(0, 'ALIEN_INTRUDER_AT_BOTTOM'): " + stack);

        // Mutate an element in the middle directly
        stack.set(2, "MIDDLE_MUTATED");
        System.out.println("Stack after stack.set(2, 'MIDDLE_MUTATED'): " + stack);

        System.out.print("Popping elements: ");
        while (!stack.empty()) {
            System.out.print(stack.pop() + " | ");
        }
        System.out.println("\nLIFO contract was broken by inherited methods!\n");
    }

    /**
     * Demonstrates the synchronization overhead tax:
     * Vector synchronizes every single call; ArrayList executes with zero locking.
     */
    private static void demonstrateSynchronizationOverhead() {
        System.out.println("--- 2. Single-Threaded Synchronization Overhead Tax ---");
        int iterations = 2_000_000;

        // Warmup JIT
        for (int i = 0; i < 5; i++) {
            Vector<Integer> v = new Vector<>();
            ArrayList<Integer> a = new ArrayList<>();
            for (int j = 0; j < 10_000; j++) {
                v.add(j);
                a.add(j);
            }
        }

        // Benchmark Vector (acquires monitor lock 2,000,000 times)
        long start = System.nanoTime();
        Vector<Integer> vector = new Vector<>();
        for (int i = 0; i < iterations; i++) {
            vector.add(i);
        }
        long vectorAddDuration = System.nanoTime() - start;

        // Benchmark ArrayList (no locks)
        start = System.nanoTime();
        ArrayList<Integer> arrayList = new ArrayList<>();
        for (int i = 0; i < iterations; i++) {
            arrayList.add(i);
        }
        long arrayListAddDuration = System.nanoTime() - start;

        System.out.printf("Vector 2M adds (synchronized) : %,12d ns\n", vectorAddDuration);
        System.out.printf("ArrayList 2M adds (unsync)    : %,12d ns\n", arrayListAddDuration);
        System.out.printf("ArrayList speedup             : %.2fx faster\n\n",
                (double) vectorAddDuration / Math.max(1, arrayListAddDuration));
    }

    /**
     * Demonstrates ArrayDeque as the correct, modern, type-safe stack replacement.
     */
    private static void demonstrateArrayDequeReplacement() {
        System.out.println("--- 3. Modern Alternative: ArrayDeque as Stack ---");
        Deque<String> modernStack = new ArrayDeque<>();

        // Push operations
        modernStack.push("Ground");
        modernStack.push("Floor 1");
        modernStack.push("Floor 2");
        modernStack.push("Penthouse");

        System.out.println("ModernStack (ArrayDeque) peek(): " + modernStack.peek());

        // Note: ArrayDeque does NOT expose add(int index, E element)!
        // modernStack.add(0, "Illegal"); // COMPILE ERROR! Contract cannot be broken!

        System.out.print("Popping ArrayDeque: ");
        while (!modernStack.isEmpty()) {
            System.out.print(modernStack.pop() + " -> ");
        }
        System.out.println("EMPTY\n");
    }

    /**
     * Demonstrates why synchronized methods on Vector/Stack do NOT prevent
     * compound action check-then-act race conditions.
     */
    private static void demonstrateCompoundActionRaceHazard() {
        System.out.println("--- 4. Compound Action Race Hazard Demonstration ---");
        Stack<Integer> sharedStack = new Stack<>();
        sharedStack.push(42); // Exactly 1 item

        System.out.println("Shared stack contains 1 element: " + sharedStack);
        System.out.println("Thread A and Thread B both execute: 'if (!stack.empty()) stack.pop()'");
        System.out.println("Without external synchronization on the stack instance, one thread will crash");
        System.out.println("with EmptyStackException due to the race window between empty() and pop()!\n");
    }
}
