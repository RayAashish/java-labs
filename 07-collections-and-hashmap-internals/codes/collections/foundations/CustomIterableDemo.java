package collections.foundations;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * CustomIterableDemo — Deep Dive into Custom Iterable & Lazy Iterator Mechanics.
 *
 * Demonstrates:
 * 1. Building a custom Iterable<T> from scratch (FixedCapacityRingBuffer).
 * 2. Implementing a stateful, lazy Iterator with structural modification tracking (modCount).
 * 3. Exact bytecode lowering equivalence: for-each vs explicit while-loop.
 * 4. Java 8+ forEachRemaining() high-throughput batch consumption.
 * 5. Edge cases: NoSuchElementException on exhausted cursor, hasNext() idempotence.
 *
 * Author: raysi
 */
public class CustomIterableDemo {

    /**
     * A memory-efficient circular ring buffer implementing Iterable<E>.
     *
     * MEMORY LAYOUT (64-bit HotSpot VM with Compressed OOPs):
     * - FixedCapacityRingBuffer Object: 24 bytes
     *   ├── Mark Word: 8 bytes
     *   ├── Klass Pointer: 4 bytes
     *   ├── Object[] buffer: 4 bytes (reference to array on heap)
     *   ├── int capacity: 4 bytes
     *   ├── int head: 4 bytes
     *   ├── int tail: 4 bytes
     *   ├── int size: 4 bytes
     *   └── int modCount: 4 bytes
     *   Total shallow size = 36 bytes -> padded to 40 bytes (8-byte alignment)
     */
    public static class FixedCapacityRingBuffer<E> implements Iterable<E> {
        private final Object[] buffer;
        private final int capacity;
        private int head = 0; // Read position
        private int tail = 0; // Write position
        private int size = 0;
        private int modCount = 0; // Structural version tracking

        public FixedCapacityRingBuffer(int capacity) {
            if (capacity <= 0) {
                throw new IllegalArgumentException("Capacity must be positive: " + capacity);
            }
            this.capacity = capacity;
            this.buffer = new Object[capacity];
        }

        public synchronized void add(E element) {
            Objects.requireNonNull(element, "Ring buffer does not permit null elements");
            if (size == capacity) {
                // Overwriting oldest element at head
                head = (head + 1) % capacity;
            } else {
                size++;
            }
            buffer[tail] = element;
            tail = (tail + 1) % capacity;
            modCount++; // Invalidate active iterators
        }

        public synchronized int size() {
            return size;
        }

        public synchronized int capacity() {
            return capacity;
        }

        @Override
        public Iterator<E> iterator() {
            /*
             * MEMORY NOTICE:
             * Creating RingBufferIterator creates an object on the Java Heap (approx 32 bytes).
             * If the iterator does NOT escape the method scope (e.g. inside a local loop),
             * the C2 JIT compiler's Escape Analysis (EA) can perform Scalar Replacement,
             * allocating its fields directly into CPU registers without heap overhead.
             */
            return new RingBufferIterator();
        }

        /**
         * Custom Iterator with fail-fast detection, lazy indexing, and forEachRemaining.
         */
        private class RingBufferIterator implements Iterator<E> {
            private int cursor = 0;                  // Number of elements traversed so far
            private int expectedModCount = modCount; // Snapshot of structural state
            private int lastRet = -1;                // Internal index of last returned item

            @Override
            public boolean hasNext() {
                /*
                 * IDEMPOTENCE CONTRACT:
                 * hasNext() MUST be side-effect free. Invoking it 100 times sequentially
                 * without calling next() must not mutate the cursor.
                 *
                 * NOTE: hasNext() intentionally does NOT check for co-modification
                 * according to standard JDK collection convention.
                 */
                return cursor < size;
            }

            @Override
            @SuppressWarnings("unchecked")
            public E next() {
                // Step 1: Comodification barrier
                checkForComodification();

                // Step 2: Boundary check
                if (cursor >= size) {
                    throw new NoSuchElementException(
                        "Iterator exhausted: cursor=" + cursor + ", size=" + size
                    );
                }

                // Step 3: Compute circular physical index lazily
                int physicalIndex = (head + cursor) % capacity;
                lastRet = physicalIndex;
                cursor++;

                // Step 4: Retrieve and cast
                return (E) buffer[physicalIndex];
            }

            @Override
            @SuppressWarnings("unchecked")
            public void forEachRemaining(Consumer<? super E> action) {
                Objects.requireNonNull(action);
                checkForComodification();

                /*
                 * HIGH-PERFORMANCE PATH:
                 * Bypasses repeated hasNext() method calls and avoids per-element comodification checks.
                 * Evaluates remaining items in a tight CPU cache-friendly loop,
                 * performing a single terminal co-modification verification.
                 */
                int remaining = size - cursor;
                if (remaining <= 0) return;

                int currentHead = head;
                int currentCap = capacity;
                Object[] b = buffer;

                for (int i = cursor; i < size; i++) {
                    if (modCount != expectedModCount) {
                        throw new java.util.ConcurrentModificationException();
                    }
                    int idx = (currentHead + i) % currentCap;
                    action.accept((E) b[idx]);
                }
                cursor = size;
                lastRet = (currentHead + size - 1) % currentCap;
                checkForComodification();
            }

            private void checkForComodification() {
                if (modCount != expectedModCount) {
                    throw new java.util.ConcurrentModificationException(
                        "RingBuffer mutated concurrently: modCount=" + modCount +
                        ", expectedModCount=" + expectedModCount
                    );
                }
            }
        }
    }

    public static void main(String[] args) {
        System.out.println("===============================================================");
        System.out.println(" 1. CUSTOM ITERABLE VERIFICATION: CIRCULAR RING BUFFER");
        System.out.println("===============================================================");

        FixedCapacityRingBuffer<String> ringBuffer = new FixedCapacityRingBuffer<>(4);
        ringBuffer.add("Packet-1");
        ringBuffer.add("Packet-2");
        ringBuffer.add("Packet-3");

        System.out.println("Initial Buffer Size: " + ringBuffer.size());

        System.out.println("\n--- A. Traversal via Java Enhanced For-Loop (Syntactic Sugar) ---");
        // Enhanced for-loop requires Iterable<T>
        for (String packet : ringBuffer) {
            System.out.println("Received: " + packet);
        }

        System.out.println("\n--- B. Exact Compiler Decompilation Equivalent (Manual While-Loop) ---");
        // This is the EXACT bytecode structure emitted by javac for the enhanced for-loop above:
        Iterator<String> manualIt = ringBuffer.iterator();
        while (manualIt.hasNext()) {
            String packet = manualIt.next();
            System.out.println("Decompiled while-loop received: " + packet);
        }

        System.out.println("\n===============================================================");
        System.out.println(" 2. RING OVERFLOW & BUFFER WRAP-AROUND");
        System.out.println("===============================================================");
        // Pushing beyond capacity 4. Packet-1 and Packet-2 will be overwritten!
        ringBuffer.add("Packet-4");
        ringBuffer.add("Packet-5"); // Overwrites Packet-1
        ringBuffer.add("Packet-6"); // Overwrites Packet-2

        System.out.println("Buffer after overwrites (Capacity=4, Size=" + ringBuffer.size() + "):");
        for (String packet : ringBuffer) {
            System.out.println("Active slot: " + packet);
        }

        System.out.println("\n===============================================================");
        System.out.println(" 3. HIGH-THROUGHPUT BATCH CONSUMPTION: forEachRemaining()");
        System.out.println("===============================================================");
        Iterator<String> batchIt = ringBuffer.iterator();
        System.out.println("Consume first element manually: " + batchIt.next());
        System.out.print("Remaining consumed via forEachRemaining(): ");
        batchIt.forEachRemaining(p -> System.out.print("[" + p + "] "));
        System.out.println();

        System.out.println("\n===============================================================");
        System.out.println(" 4. EDGE CASE: Calling next() on Exhausted Cursor");
        System.out.println("===============================================================");
        try {
            System.out.println("Attempting next() on fully exhausted iterator...");
            batchIt.next(); // Should crash with NoSuchElementException
        } catch (NoSuchElementException e) {
            System.out.println("PASSED: Correctly caught expected exception -> " + e.getMessage());
        }

        System.out.println("\n===============================================================");
        System.out.println(" 5. EDGE CASE: hasNext() Idempotence Verification");
        System.out.println("===============================================================");
        Iterator<String> testIt = ringBuffer.iterator();
        // Calling hasNext() multiple times must not alter cursor state
        boolean c1 = testIt.hasNext();
        boolean c2 = testIt.hasNext();
        boolean c3 = testIt.hasNext();
        System.out.println("Triple invocation of hasNext(): c1=" + c1 + ", c2=" + c2 + ", c3=" + c3);
        System.out.println("First call to next() after triple hasNext(): " + testIt.next());

        System.out.println("\n===============================================================");
        System.out.println(" 6. FAIL-FAST DETECTION VIA STRUCTURAL MODCOUNT MUTATION");
        System.out.println("===============================================================");
        Iterator<String> failFastIt = ringBuffer.iterator();
        System.out.println("Created iterator. Next element: " + failFastIt.next());

        System.out.println("Mutating ringBuffer structurally via ringBuffer.add('Packet-7')...");
        ringBuffer.add("Packet-7");

        try {
            failFastIt.next();
            System.err.println("FAILED: Iterator failed to detect concurrent mutation!");
        } catch (java.util.ConcurrentModificationException e) {
            System.out.println("PASSED: Caught expected ConcurrentModificationException -> " + e.getMessage());
        }
    }
}
