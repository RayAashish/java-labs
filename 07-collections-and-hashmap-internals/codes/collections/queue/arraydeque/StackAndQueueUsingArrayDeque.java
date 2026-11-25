package collections.queue.arraydeque;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedList;
import java.util.Queue;

/**
 * ArrayDeque as Dual-Purpose High-Performance Data Structure:
 * 1. ArrayDeque as a LIFO Stack (push, pop, peek).
 * 2. ArrayDeque as a FIFO Queue (offer, poll, peek).
 * 3. Performance Benchmark vs. LinkedList for both Stack and Queue workloads.
 * 4. Null rejection enforcement and anti-pattern analysis.
 *
 * @author raysi
 */
public class StackAndQueueUsingArrayDeque {

    public static void main(String[] args) {
        System.out.println("==================================================================");
        System.out.println("   ARRAYDEQUE: DUAL STACK & QUEUE WORKLOADS & BENCHMARK DEMO      ");
        System.out.println("                         Author: raysi                            ");
        System.out.println("==================================================================\n");

        verifyArrayDequeAsStack();
        verifyArrayDequeAsQueue();
        verifyNullRejection();
        runStackAndQueueBenchmark();
    }

    /**
     * Demonstrates using ArrayDeque as a modern LIFO Stack.
     */
    private static void verifyArrayDequeAsStack() {
        System.out.println("--- 1. ArrayDeque as a LIFO Stack ---");
        Deque<String> stack = new ArrayDeque<>();

        // Push pushes onto the head of the deque (addFirst)
        stack.push("Frame_0_Main");
        stack.push("Frame_1_CalculateTax");
        stack.push("Frame_2_FetchRate");

        System.out.println("Stack state after pushes: " + stack);
        System.out.println("Top element (peek): " + stack.peek()); // Frame_2_FetchRate

        // Pop removes from the head of the deque (removeFirst)
        while (!stack.isEmpty()) {
            System.out.println("Popped execution frame: " + stack.pop());
        }
        System.out.println("Stack isEmpty(): " + stack.isEmpty() + "\n");
    }

    /**
     * Demonstrates using ArrayDeque as a high-throughput FIFO Queue.
     */
    private static void verifyArrayDequeAsQueue() {
        System.out.println("--- 2. ArrayDeque as a FIFO Queue ---");
        Queue<String> queue = new ArrayDeque<>();

        // Enqueue at tail via offer (offerLast)
        queue.offer("Packet_1");
        queue.offer("Packet_2");
        queue.offer("Packet_3");

        System.out.println("Queue state after offers: " + queue);
        System.out.println("Front element (peek): " + queue.peek()); // Packet_1

        // Dequeue from head via poll (pollFirst)
        while (!queue.isEmpty()) {
            System.out.println("Polled packet: " + queue.poll());
        }
        System.out.println("Queue isEmpty(): " + queue.isEmpty() + "\n");
    }

    /**
     * Demonstrates that ArrayDeque unconditionally rejects null elements.
     */
    private static void verifyNullRejection() {
        System.out.println("--- 3. ArrayDeque Null Rejection Verification ---");
        Deque<String> deque = new ArrayDeque<>();

        try {
            System.out.println("Attempting deque.addFirst(null)...");
            deque.addFirst(null);
        } catch (NullPointerException e) {
            System.out.println("  -> Successfully rejected null with NullPointerException on addFirst()");
        }

        try {
            System.out.println("Attempting deque.push(null)...");
            deque.push(null);
        } catch (NullPointerException e) {
            System.out.println("  -> Successfully rejected null with NullPointerException on push()");
        }

        try {
            System.out.println("Attempting deque.offer(null)...");
            deque.offer(null);
        } catch (NullPointerException e) {
            System.out.println("  -> Successfully rejected null with NullPointerException on offer()\n");
        }
    }

    /**
     * Runs high-volume benchmarks comparing ArrayDeque vs. LinkedList
     * for both Stack (LIFO) and Queue (FIFO) workloads.
     */
    private static void runStackAndQueueBenchmark() {
        System.out.println("--- 4. Performance Benchmark: ArrayDeque vs. LinkedList ---");
        final int iterations = 3_000_000;
        System.out.printf("Operations count per test: %,d elements\n\n", iterations);

        // Warm up JIT compiler thoroughly
        for (int i = 0; i < 5; i++) {
            benchmarkQueue(new ArrayDeque<>(100_000), 100_000);
            benchmarkQueue(new LinkedList<>(), 100_000);
            benchmarkStack(new ArrayDeque<>(100_000), 100_000);
            benchmarkStack(new LinkedList<>(), 100_000);
        }

        // Benchmark 1: FIFO Queue (offer + poll)
        System.out.println("[Benchmark A: FIFO Queue (offer + poll)]");
        long adQueueTime = benchmarkQueue(new ArrayDeque<>(iterations), iterations);
        long llQueueTime = benchmarkQueue(new LinkedList<>(), iterations);

        System.out.printf("  ArrayDeque Time: %,8d ms\n", adQueueTime);
        System.out.printf("  LinkedList Time: %,8d ms\n", llQueueTime);
        System.out.printf("  Speedup: ArrayDeque is %.2fx faster as a Queue!\n\n", (double) llQueueTime / Math.max(1, adQueueTime));

        // Benchmark 2: LIFO Stack (push + pop)
        System.out.println("[Benchmark B: LIFO Stack (push + pop)]");
        long adStackTime = benchmarkStack(new ArrayDeque<>(iterations), iterations);
        long llStackTime = benchmarkStack(new LinkedList<>(), iterations);

        System.out.printf("  ArrayDeque Time: %,8d ms\n", adStackTime);
        System.out.printf("  LinkedList Time: %,8d ms\n", llStackTime);
        System.out.printf("  Speedup: ArrayDeque is %.2fx faster as a Stack!\n\n", (double) llStackTime / Math.max(1, adStackTime));

        System.out.println("Hardware Architecture Commentary:");
        System.out.println("  1. Memory Locality: ArrayDeque accesses sequential slots in a single flat array,");
        System.out.println("     allowing L1/L2 hardware prefetchers to cache 16 references per 64-byte cache line.");
        System.out.println("  2. GC Allocation: LinkedList allocated 3,000,000 Node objects (72 MB heap churn),");
        System.out.println("     stalling the CPU on YoungGen allocations and pointer dereferences.");
        System.out.println("  3. ArrayDeque allocated a single array, producing near-zero GC pressure.\n");
    }

    private static long benchmarkQueue(Queue<Integer> queue, int count) {
        long start = System.currentTimeMillis();
        for (int i = 0; i < count; i++) {
            queue.offer(i);
        }
        for (int i = 0; i < count; i++) {
            queue.poll();
        }
        return System.currentTimeMillis() - start;
    }

    private static long benchmarkStack(Deque<Integer> stack, int count) {
        long start = System.currentTimeMillis();
        for (int i = 0; i < count; i++) {
            stack.push(i);
        }
        for (int i = 0; i < count; i++) {
            stack.pop();
        }
        return System.currentTimeMillis() - start;
    }
}
