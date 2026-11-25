package collections.queue;

import java.util.AbstractQueue;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.NoSuchElementException;
import java.util.Queue;
import java.util.Stack;

/**
 * Production-Grade Demonstration of Queue and Deque Contracts in Java.
 *
 * <p>Examines:
 * 1. The Dual-API contract (throws exception vs. returns sentinel value).
 * 2. Deque double-ended mechanics (head vs. tail operations).
 * 3. Deque as the modern, high-performance replacement for legacy java.util.Stack.
 * 4. Boundary conditions: behaviors on empty queues and full bounded queues.
 * 5. AbstractQueue template method pattern implementation.
 *
 * @author raysi
 */
public class QueueVsDequeContractDemo {

    public static void main(String[] args) {
        System.out.println("==================================================================");
        System.out.println("      JAVA COLLECTIONS FRAMEWORK: QUEUE & DEQUE CONTRACT DEMO     ");
        System.out.println("                         Author: raysi                            ");
        System.out.println("==================================================================\n");

        demonstrateQueueDualApiContract();
        demonstrateDequeDoubleEndedOperations();
        demonstrateDequeAsStackReplacement();
        demonstrateAbstractQueueTemplatePattern();
        demonstrateNullSentinelAmbiguity();
    }

    /**
     * Demonstrates the two sets of Queue methods:
     * Set A (Throws Exception): add(e), remove(), element()
     * Set B (Special Value):    offer(e), poll(), peek()
     */
    private static void demonstrateQueueDualApiContract() {
        System.out.println("--- 1. Queue Dual-API Contract on Empty / Full States ---");
        Queue<String> queue = new ArrayDeque<>();

        // 1.1 Special value operations on empty queue
        System.out.println("[Special Value] peek() on empty queue: " + queue.peek()); // returns null
        System.out.println("[Special Value] poll() on empty queue: " + queue.poll()); // returns null

        // 1.2 Exception throwing operations on empty queue
        try {
            System.out.println("[Exception API] Calling element() on empty queue...");
            queue.element();
        } catch (NoSuchElementException e) {
            System.out.println("  -> Caught expected NoSuchElementException: " + e.getMessage());
        }

        try {
            System.out.println("[Exception API] Calling remove() on empty queue...");
            queue.remove();
        } catch (NoSuchElementException e) {
            System.out.println("  -> Caught expected NoSuchElementException: " + e.getMessage());
        }

        // 1.3 Normal FIFO additions and removals
        queue.offer("Alpha");
        queue.offer("Beta");
        queue.offer("Gamma");

        System.out.println("Queue after 3 offers: " + queue);
        System.out.println("peek() head: " + queue.peek()); // Alpha
        System.out.println("poll() head: " + queue.poll()); // Alpha
        System.out.println("poll() next: " + queue.poll()); // Beta
        System.out.println("Remaining element(): " + queue.element()); // Gamma
        System.out.println("remove() remaining: " + queue.remove()); // Gamma
        System.out.println("Queue size now: " + queue.size()); // 0
        System.out.println();
    }

    /**
     * Demonstrates Deque (Double-Ended Queue) capabilities:
     * Inserting, examining, and removing from both ends in O(1) time.
     */
    private static void demonstrateDequeDoubleEndedOperations() {
        System.out.println("--- 2. Deque Double-Ended (Head & Tail) Operations ---");
        Deque<String> deque = new ArrayDeque<>();

        // Add to both ends
        deque.addFirst("Head-1");
        deque.addLast("Tail-1");
        deque.offerFirst("Head-2"); // Becomes new first element
        deque.offerLast("Tail-2");   // Becomes new last element

        // Current order: [Head-2, Head-1, Tail-1, Tail-2]
        System.out.println("Deque after head/tail insertions: " + deque);
        System.out.println("getFirst(): " + deque.getFirst());       // Head-2
        System.out.println("getLast(): " + deque.getLast());         // Tail-2
        System.out.println("peekFirst(): " + deque.peekFirst());     // Head-2
        System.out.println("peekLast(): " + deque.peekLast());       // Tail-2

        // Removal from both ends
        System.out.println("pollFirst(): " + deque.pollFirst());     // Head-2
        System.out.println("pollLast(): " + deque.pollLast());       // Tail-2
        System.out.println("removeFirst(): " + deque.removeFirst()); // Head-1
        System.out.println("removeLast(): " + deque.removeLast());   // Tail-1

        System.out.println("Deque isEmpty(): " + deque.isEmpty());
        System.out.println();
    }

    /**
     * Demonstrates why Deque (specifically ArrayDeque) is the recommended
     * replacement for the legacy java.util.Stack.
     */
    private static void demonstrateDequeAsStackReplacement() {
        System.out.println("--- 3. Deque vs. Legacy java.util.Stack ---");

        // 3.1 Modern Deque as LIFO Stack
        Deque<String> modernStack = new ArrayDeque<>();
        modernStack.push("Layer_1_Database");
        modernStack.push("Layer_2_Service");
        modernStack.push("Layer_3_Controller");

        System.out.println("Modern ArrayDeque Stack top (peek): " + modernStack.peek());
        while (!modernStack.isEmpty()) {
            System.out.println("  ArrayDeque.pop() -> " + modernStack.pop());
        }

        // 3.2 Legacy Stack Architectural Flaws Demonstration
        Stack<String> legacyStack = new Stack<>();
        legacyStack.push("Data_A");
        legacyStack.push("Data_B");
        legacyStack.push("Data_C");

        // Flaw 1: Inheriting Vector allows arbitrary index insertions, breaking LIFO!
        legacyStack.add(1, "HACKED_IN_THE_MIDDLE");
        System.out.println("\nLegacy Stack after illegal mid-stack insertion: " + legacyStack);

        // Flaw 2: Iteration order of Vector/Stack is bottom-up (FIFO), not top-down (LIFO)!
        System.out.print("Legacy Stack iteration order (bottom-to-top flaw): ");
        for (String item : legacyStack) {
            System.out.print(item + " -> ");
        }
        System.out.println("END");

        // ArrayDeque iteration when used as a stack is properly top-down!
        Deque<String> correctStack = new ArrayDeque<>();
        correctStack.push("Data_A");
        correctStack.push("Data_B");
        correctStack.push("Data_C");

        System.out.print("ArrayDeque iteration order (top-to-bottom correct LIFO): ");
        for (String item : correctStack) {
            System.out.print(item + " -> ");
        }
        System.out.println("END\n");
    }

    /**
     * Demonstrates how AbstractQueue implements the Template Method pattern:
     * add() calls offer(), remove() calls poll(), element() calls peek().
     */
    private static void demonstrateAbstractQueueTemplatePattern() {
        System.out.println("--- 4. AbstractQueue Template Method Pattern in Action ---");

        SimpleBoundedRingBuffer<Integer> boundedQueue = new SimpleBoundedRingBuffer<>(3);

        // Fill capacity
        System.out.println("offer(101): " + boundedQueue.offer(101));
        System.out.println("offer(102): " + boundedQueue.offer(102));
        System.out.println("offer(103): " + boundedQueue.offer(103));

        // Attempting to offer past capacity returns false (special value)
        boolean offerExceeded = boundedQueue.offer(104);
        System.out.println("offer(104) past capacity returned: " + offerExceeded);

        // Attempting to add past capacity throws IllegalStateException via AbstractQueue template!
        try {
            System.out.println("Executing add(104) via AbstractQueue template...");
            boundedQueue.add(104);
        } catch (IllegalStateException e) {
            System.out.println("  -> AbstractQueue.add() threw: " + e.getMessage());
        }

        // Verify FIFO extraction
        System.out.println("Polling elements:");
        while (!boundedQueue.isEmpty()) {
            System.out.println("  Polled: " + boundedQueue.poll());
        }

        // remove() on empty queue throws NoSuchElementException via AbstractQueue template
        try {
            System.out.println("Executing remove() on now-empty queue...");
            boundedQueue.remove();
        } catch (NoSuchElementException e) {
            System.out.println("  -> AbstractQueue.remove() threw: " + e.getClass().getSimpleName());
        }
        System.out.println();
    }

    /**
     * Demonstrates why allowing null elements in Queue (as LinkedList does)
     * leads to fatal ambiguity.
     */
    private static void demonstrateNullSentinelAmbiguity() {
        System.out.println("--- 5. The Null Sentinel Ambiguity Anti-Pattern ---");

        Queue<String> linkedListQueue = new LinkedList<>();
        linkedListQueue.offer(null); // Legal in LinkedList, but highly problematic

        // poll() returns null. Does null mean the queue was empty, or did it contain null?
        String extracted = linkedListQueue.poll();
        System.out.println("LinkedList polled value: " + extracted);
        System.out.println("Was the queue empty BEFORE the poll? No! It contained a null element.");
        System.out.println("Is the queue empty AFTER the poll? " + linkedListQueue.isEmpty());

        // ArrayDeque avoids this completely by strictly rejecting nulls
        Queue<String> arrayDeque = new ArrayDeque<>();
        try {
            arrayDeque.offer(null);
        } catch (NullPointerException e) {
            System.out.println("ArrayDeque proactively prevents ambiguity: rejected null with NullPointerException.");
        }
        System.out.println();
    }

    /**
     * Concrete bounded queue demonstrating how AbstractQueue works.
     */
    static class SimpleBoundedRingBuffer<E> extends AbstractQueue<E> {
        private final Object[] elements;
        private final int capacity;
        private int head = 0;
        private int tail = 0;
        private int count = 0;

        public SimpleBoundedRingBuffer(int capacity) {
            if (capacity <= 0) {
                throw new IllegalArgumentException("Capacity must be positive");
            }
            this.capacity = capacity;
            this.elements = new Object[capacity];
        }

        @Override
        public boolean offer(E e) {
            if (e == null) throw new NullPointerException("Null elements are not supported");
            if (count == capacity) {
                return false; // Queue full
            }
            elements[tail] = e;
            tail = (tail + 1) % capacity;
            count++;
            return true;
        }

        @Override
        @SuppressWarnings("unchecked")
        public E poll() {
            if (count == 0) {
                return null; // Queue empty
            }
            E item = (E) elements[head];
            elements[head] = null; // Clean reference for Garbage Collector
            head = (head + 1) % capacity;
            count--;
            return item;
        }

        @Override
        @SuppressWarnings("unchecked")
        public E peek() {
            if (count == 0) return null;
            return (E) elements[head];
        }

        @Override
        public Iterator<E> iterator() {
            return new Iterator<>() {
                private int cursor = head;
                private int remaining = count;

                @Override
                public boolean hasNext() {
                    return remaining > 0;
                }

                @Override
                @SuppressWarnings("unchecked")
                public E next() {
                    if (!hasNext()) throw new NoSuchElementException();
                    E val = (E) elements[cursor];
                    cursor = (cursor + 1) % capacity;
                    remaining--;
                    return val;
                }
            };
        }

        @Override
        public int size() {
            return count;
        }
    }
}
