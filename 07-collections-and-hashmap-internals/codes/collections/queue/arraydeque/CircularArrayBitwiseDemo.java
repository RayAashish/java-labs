package collections.queue.arraydeque;

import java.lang.reflect.Field;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.NoSuchElementException;

/**
 * Deep Dive Demonstration of Circular Buffer Wrapping and Bitwise Index Arithmetic.
 *
 * <p>Examines:
 * 1. The bitwise modulo trick: (x & (capacity - 1)) replacing (x % capacity).
 * 2. Power-of-2 capacity enforcement and why it is mathematically required for the trick.
 * 3. Deep reflection inspection of JDK's ArrayDeque (head, tail, and elements array).
 * 4. Step-by-step visualization of head and tail wrapping around circular boundaries.
 * 5. Standalone reference implementation of the JDK Circular Buffer with array realignment.
 *
 * @author raysi
 */
public class CircularArrayBitwiseDemo {

    public static void main(String[] args) {
        System.out.println("==================================================================");
        System.out.println("      ARRAYDEQUE: CIRCULAR BUFFER & BITWISE ARITHMETIC DEMO       ");
        System.out.println("                         Author: raysi                            ");
        System.out.println("==================================================================\n");

        demonstrateBitwiseModuloMathematics();
        demonstrateJdkArrayDequeReflection();
        demonstrateCircularBufferSimulation();
    }

    /**
     * Demonstrates the mathematical equivalence and performance difference
     * between standard integer modulo (%) and bitwise AND (&) with (capacity - 1).
     */
    private static void demonstrateBitwiseModuloMathematics() {
        System.out.println("--- 1. Bitwise AND Modulo Mathematics ---");
        int capacity = 16; // Power of two: 2^4 = 00010000
        int mask = capacity - 1; // 15 = 00001111

        System.out.printf("Capacity = %d (Binary: %s)\n", capacity, Integer.toBinaryString(capacity));
        System.out.printf("Mask     = %d (Binary: %s)\n", mask, Integer.toBinaryString(mask));
        System.out.println("------------------------------------------------------------");
        System.out.println(" Index | Index % 16 | Index & 15 | Binary (Index) & Binary(15) -> Result");
        System.out.println("------------------------------------------------------------");

        int[] testIndices = {0, 1, 7, 14, 15, 16, 17, 31, 32, -1};
        for (int idx : testIndices) {
            int stdMod = (idx >= 0) ? (idx % capacity) : -1;
            int bitwiseMod = idx & mask;
            System.out.printf("%6d | %10s | %10d | %s & 00001111 -> %d\n",
                    idx,
                    (idx >= 0 ? String.valueOf(stdMod) : "N/A (Neg)"),
                    bitwiseMod,
                    padBinary(idx),
                    bitwiseMod);
        }

        System.out.println("\nKEY INSIGHT: When idx = -1 (decrementing head from 0):");
        System.out.println("  -1 in 32-bit two's complement = 0b11111111_11111111_11111111_11111111");
        System.out.println("  (-1) & 15 = 15! In a SINGLE CPU cycle without any if-check, head wraps to 15!");
        System.out.println();
    }

    /**
     * Inspects JDK's java.util.ArrayDeque internal fields using reflection.
     * Note: If running on modular JDK 17/21+, launch with:
     *   --add-opens java.base/java.util=ALL-UNNAMED
     */
    private static void demonstrateJdkArrayDequeReflection() {
        System.out.println("--- 2. Reflection Inspection of JDK ArrayDeque State ---");
        ArrayDeque<String> deque = new ArrayDeque<>();

        try {
            Field elementsField = ArrayDeque.class.getDeclaredField("elements");
            Field headField = ArrayDeque.class.getDeclaredField("head");
            Field tailField = ArrayDeque.class.getDeclaredField("tail");

            elementsField.setAccessible(true);
            headField.setAccessible(true);
            tailField.setAccessible(true);

            printDequeInternalState("Initial Empty Deque", deque, elementsField, headField, tailField);

            // Add elements at tail
            deque.addLast("Tail_A");
            deque.addLast("Tail_B");
            printDequeInternalState("After addLast(Tail_A, Tail_B)", deque, elementsField, headField, tailField);

            // Add elements at head (watch head decrement and wrap around!)
            deque.addFirst("Head_1");
            printDequeInternalState("After addFirst(Head_1) -> head wraps backwards!", deque, elementsField, headField, tailField);

            deque.addFirst("Head_2");
            printDequeInternalState("After addFirst(Head_2)", deque, elementsField, headField, tailField);

            // Poll an element from head
            String polled = deque.pollFirst();
            printDequeInternalState("After pollFirst() -> element cleared to null for GC!", deque, elementsField, headField, tailField);

        } catch (Exception e) {
            System.out.println("  [Reflection Notice]: Unable to access JDK internal fields directly: " + e.getMessage());
            System.out.println("  To enable direct JDK inspection, run JVM with: --add-opens java.base/java.util=ALL-UNNAMED");
            System.out.println("  Proceeding with full custom circular buffer engine verification below!\n");
        }
    }

    private static void printDequeInternalState(String label, ArrayDeque<?> deque,
                                                Field elementsField, Field headField, Field tailField) throws Exception {
        Object[] elements = (Object[]) elementsField.get(deque);
        int head = headField.getInt(deque);
        int tail = tailField.getInt(deque);

        System.out.printf("[%s]\n", label);
        System.out.printf("  head index = %d | tail index = %d | size = %d | capacity = %d\n",
                head, tail, deque.size(), elements.length);
        System.out.print("  elements array: [");
        for (int i = 0; i < elements.length; i++) {
            System.out.print(elements[i] == null ? "_" : elements[i]);
            if (i < elements.length - 1) System.out.print(", ");
        }
        System.out.println("]\n");
    }

    /**
     * Self-contained runnable simulation of the JDK Circular Buffer with Bitwise Masking.
     */
    private static void demonstrateCircularBufferSimulation() {
        System.out.println("--- 3. Standalone JDK-Style Bitwise Ring Buffer Simulation ---");
        BitwiseRingBuffer<String> ringBuffer = new BitwiseRingBuffer<>(8);

        System.out.println("Initial Ring Buffer (Capacity = 8):");
        ringBuffer.dumpInternalState();

        // 1. Add elements to tail
        ringBuffer.addLast("T1");
        ringBuffer.addLast("T2");
        ringBuffer.dumpInternalState();

        // 2. Add elements to head (observe backward wrapping from 0 to 7)
        System.out.println("Adding to HEAD (triggers bitwise wrap from 0 to 7):");
        ringBuffer.addFirst("H1");
        ringBuffer.dumpInternalState();

        ringBuffer.addFirst("H2");
        ringBuffer.addFirst("H3");
        ringBuffer.dumpInternalState();

        // 3. Dequeue from head (observe slot nulling for GC protection)
        System.out.println("Polling from HEAD (watch reference get nulled out):");
        String p1 = ringBuffer.pollFirst();
        System.out.println("Polled: " + p1);
        ringBuffer.dumpInternalState();

        // 4. Fill buffer to trigger dynamic growth and circular array realignment
        System.out.println("Filling buffer to trigger capacity growth and System.arraycopy realignment:");
        ringBuffer.addLast("T3");
        ringBuffer.addLast("T4");
        ringBuffer.addLast("T5");
        ringBuffer.addLast("T6");
        ringBuffer.dumpInternalState();

        // This addition fills capacity and triggers grow()
        ringBuffer.addLast("T7_Trigger_Grow");
        System.out.println("After grow() doubled capacity to 16 and realigned segments:");
        ringBuffer.dumpInternalState();

        System.out.println("Draining ring buffer in logical sequence:");
        while (!ringBuffer.isEmpty()) {
            System.out.print(ringBuffer.pollFirst() + " -> ");
        }
        System.out.println("EMPTY\n");
    }

    private static String padBinary(int n) {
        String s = Integer.toBinaryString(n);
        if (s.length() > 8) s = s.substring(s.length() - 8);
        while (s.length() < 8) s = "0" + s;
        return s;
    }

    /**
     * Standalone implementation matching the exact JDK 8-16 bitwise circular buffer design.
     */
    static class BitwiseRingBuffer<E> {
        private Object[] elements;
        private int head;
        private int tail;

        public BitwiseRingBuffer(int initialCapacity) {
            int capacity = calculatePowerOfTwoCapacity(initialCapacity);
            this.elements = new Object[capacity];
            this.head = 0;
            this.tail = 0;
        }

        private static int calculatePowerOfTwoCapacity(int numElements) {
            int initial = 8;
            while (initial < numElements) {
                initial <<= 1;
            }
            return initial;
        }

        public void addFirst(E e) {
            if (e == null) throw new NullPointerException("Null elements prohibited");
            // Bitwise wrap backwards: (head - 1) & (elements.length - 1)
            head = (head - 1) & (elements.length - 1);
            elements[head] = e;
            if (head == tail) {
                doubleCapacity();
            }
        }

        public void addLast(E e) {
            if (e == null) throw new NullPointerException("Null elements prohibited");
            elements[tail] = e;
            // Bitwise wrap forwards: (tail + 1) & (elements.length - 1)
            tail = (tail + 1) & (elements.length - 1);
            if (head == tail) {
                doubleCapacity();
            }
        }

        @SuppressWarnings("unchecked")
        public E pollFirst() {
            int h = head;
            E result = (E) elements[h];
            if (result == null) return null;
            elements[h] = null; // Clear reference for GC
            head = (h + 1) & (elements.length - 1);
            return result;
        }

        public boolean isEmpty() {
            return head == tail;
        }

        public int size() {
            return (tail - head) & (elements.length - 1);
        }

        private void doubleCapacity() {
            int p = head;
            int n = elements.length;
            int r = n - p; // Number of elements to the right of p
            int newCapacity = n << 1;
            if (newCapacity < 0) throw new IllegalStateException("Deque too big");

            Object[] a = new Object[newCapacity];
            // Copy right slice: from head to end of old array -> destination 0
            System.arraycopy(elements, p, a, 0, r);
            // Copy left slice: from index 0 to head -> destination r
            System.arraycopy(elements, 0, a, r, p);

            elements = a;
            head = 0;
            tail = n;
        }

        public void dumpInternalState() {
            System.out.printf("  [Buffer State] head=%d | tail=%d | size=%d | capacity=%d\n",
                    head, tail, size(), elements.length);
            System.out.print("  Internal Array: [");
            for (int i = 0; i < elements.length; i++) {
                System.out.print(elements[i] == null ? "_" : elements[i]);
                if (i < elements.length - 1) System.out.print(", ");
            }
            System.out.println("]\n");
        }
    }
}
