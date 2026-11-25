package collections.queue.priorityqueue;

import java.util.Collections;
import java.util.Comparator;
import java.util.PriorityQueue;

/**
 * Production-Grade Demonstration of PriorityQueue Binary Heap Mechanics.
 *
 * <p>Examines:
 * 1. Min-Heap (Natural Ordering) vs Max-Heap (Collections.reverseOrder()).
 * 2. The Iterator Ordering Trap: direct iteration traverses heap array order, NOT sorted order.
 * 3. The Mutable Element Corruption Trap: in-place mutations violate heap invariants without triggering re-heapify.
 * 4. Custom domain objects with multi-field Comparator and priority tie-breaking.
 *
 * @author raysi
 */
public class MinAndMaxHeapImplementation {

    public static void main(String[] args) {
        System.out.println("==================================================================");
        System.out.println("          PRIORITYQUEUE: BINARY HEAP INTERNALS & VERIFICATION     ");
        System.out.println("                             Author: raysi                        ");
        System.out.println("==================================================================\n");

        verifyMinAndMaxHeapOrdering();
        verifyIteratorVsPollOrderingTrap();
        verifyMutableElementHeapCorruption();
        verifyCustomDomainObjectPriority();
    }

    /**
     * Demonstrates the difference between natural min-heap ordering
     * and reverse max-heap ordering.
     */
    private static void verifyMinAndMaxHeapOrdering() {
        System.out.println("--- 1. Min-Heap (Natural) vs Max-Heap (Reverse Order) ---");

        // Min-Heap by default
        PriorityQueue<Integer> minHeap = new PriorityQueue<>();
        int[] data = {45, 12, 89, 5, 23, 1, 67};
        for (int num : data) {
            minHeap.offer(num);
        }

        System.out.print("Min-Heap drain via poll() (ascending):  ");
        while (!minHeap.isEmpty()) {
            System.out.print(minHeap.poll() + " ");
        }
        System.out.println();

        // Max-Heap via Collections.reverseOrder()
        PriorityQueue<Integer> maxHeap = new PriorityQueue<>(Collections.reverseOrder());
        for (int num : data) {
            maxHeap.offer(num);
        }

        System.out.print("Max-Heap drain via poll() (descending): ");
        while (!maxHeap.isEmpty()) {
            System.out.print(maxHeap.poll() + " ");
        }
        System.out.println("\n");
    }

    /**
     * Demonstrates the classic interview trap:
     * Iterating PriorityQueue does NOT yield elements in priority order!
     * Only draining via poll() guarantees priority ordering.
     */
    private static void verifyIteratorVsPollOrderingTrap() {
        System.out.println("--- 2. PriorityQueue Iterator Trap vs. poll() Drain ---");
        PriorityQueue<Integer> pq = new PriorityQueue<>();
        pq.offer(50);
        pq.offer(10);
        pq.offer(40);
        pq.offer(5);
        pq.offer(20);
        pq.offer(15);

        // Trap: Iteration shows the internal array layout, NOT sorted order!
        System.out.print("Iterating PriorityQueue directly (Heap Array order): ");
        for (int val : pq) {
            System.out.print(val + " ");
        }
        System.out.println("  <-- NOT SORTED!");

        // Correct way: poll() repeatedly
        System.out.print("Draining via poll() (True Priority order):           ");
        PriorityQueue<Integer> copy = new PriorityQueue<>(pq);
        while (!copy.isEmpty()) {
            System.out.print(copy.poll() + " ");
        }
        System.out.println("  <-- Guaranteed sorted!\n");
    }

    /**
     * Demonstrates what happens when an object already in the heap has its
     * priority mutated: the heap invariant is silently corrupted.
     */
    private static void verifyMutableElementHeapCorruption() {
        System.out.println("--- 3. In-Place Mutation Heap Corruption Trap ---");

        PriorityQueue<MutableTask> taskQueue = new PriorityQueue<>(
                Comparator.comparingInt(t -> t.priority)
        );

        MutableTask t1 = new MutableTask("Low Priority Task", 100);
        MutableTask t2 = new MutableTask("Medium Priority Task", 50);
        MutableTask t3 = new MutableTask("High Priority Task", 10);

        taskQueue.offer(t1);
        taskQueue.offer(t2);
        taskQueue.offer(t3);

        System.out.println("Top task before mutation: " + taskQueue.peek());

        // SILENT CORRUPTION: Mutate priority of t1 while it resides in the heap!
        // PriorityQueue has no listener or knowledge of external state mutation.
        t1.priority = 1; // Now it should logically be root, but the heap invariant is broken!
        System.out.println("Mutated t1 priority to 1 in-place. What is peek() now?");
        System.out.println("Top task after mutation: " + taskQueue.peek()); // Still t3! Heap is corrupted!

        System.out.println("Draining corrupted heap:");
        while (!taskQueue.isEmpty()) {
            System.out.println("  Popped: " + taskQueue.poll());
        }
        System.out.println("Notice that t1 (priority 1) did NOT come out first! In-place mutation breaks the heap.\n");
    }

    /**
     * Demonstrates multi-key priority ordering for custom domain records.
     */
    private static void verifyCustomDomainObjectPriority() {
        System.out.println("--- 4. Multi-Level Priority with Stable Secondary Keys ---");
        PriorityQueue<ServerJob> jobQueue = new PriorityQueue<>(
                Comparator.comparing(ServerJob::severity)
                          .thenComparingLong(ServerJob::timestampEpochMs)
        );

        long now = 1700000000000L;
        jobQueue.offer(new ServerJob("CRITICAL-DISK-FULL", Severity.CRITICAL, now + 100));
        jobQueue.offer(new ServerJob("WARN-CPU-HIGH", Severity.WARNING, now));
        jobQueue.offer(new ServerJob("CRITICAL-KERNEL-PANIC", Severity.CRITICAL, now + 50));
        jobQueue.offer(new ServerJob("INFO-HEARTBEAT", Severity.INFO, now + 20));

        System.out.println("Draining prioritized server jobs (Severity asc, then Timestamp asc):");
        while (!jobQueue.isEmpty()) {
            System.out.println("  " + jobQueue.poll());
        }
        System.out.println();
    }

    enum Severity {
        CRITICAL, WARNING, INFO
    }

    record ServerJob(String description, Severity severity, long timestampEpochMs) {
        @Override
        public String toString() {
            return String.format("[%s | severity=%s | timestamp=%d]", description, severity, timestampEpochMs);
        }
    }

    static class MutableTask {
        String name;
        int priority;

        public MutableTask(String name, int priority) {
            this.name = name;
            this.priority = priority;
        }

        @Override
        public String toString() {
            return "[" + name + " | priority=" + priority + "]";
        }
    }
}
