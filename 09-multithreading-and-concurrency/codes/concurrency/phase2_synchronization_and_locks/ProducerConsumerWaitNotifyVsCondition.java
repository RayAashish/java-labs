package concurrency.phase2_synchronization_and_locks;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Author: raysi
 * Demonstrates:
 * 1. Classic Producer-Consumer using Intrinsic Monitors (synchronized + wait() + notifyAll()).
 *    - Why single wait-set requires notifyAll() to avoid waking the wrong thread role.
 * 2. Explicit Producer-Consumer using ReentrantLock + Dual Conditions (notFull + notEmpty).
 *    - Targeted wakeups via signal() eliminates herd effect and reduces context switches.
 */
public class ProducerConsumerWaitNotifyVsCondition {

    public static void main(String[] args) throws InterruptedException {
        System.out.println("======================================================================");
        System.out.println("  PHASE 2 LAB: PRODUCER-CONSUMER (INTRINSIC VS DUAL CONDITIONS)");
        System.out.println("  Author: raysi | Package: com.raysi.concurrency");
        System.out.println("======================================================================\n");

        System.out.println("=== TEST 1: Intrinsic Monitor Queue (Single Wait-Set) ===");
        IntrinsicBlockingQueue<Integer> intrinsicQueue = new IntrinsicBlockingQueue<>(3);

        Thread prod1 = new Thread(() -> produceTo(intrinsicQueue, "Intrinsic-Producer-1", 5), "Prod-1");
        Thread cons1 = new Thread(() -> consumeFrom(intrinsicQueue, "Intrinsic-Consumer-1", 5), "Cons-1");

        prod1.start();
        cons1.start();
        prod1.join();
        cons1.join();

        System.out.println("\n=== TEST 2: ReentrantLock Dual Conditions (notFull & notEmpty) ===");
        ConditionBlockingQueue<Integer> conditionQueue = new ConditionBlockingQueue<>(3);

        Thread prod2 = new Thread(() -> produceTo(conditionQueue, "Condition-Producer-1", 5), "CondProd-1");
        Thread cons2 = new Thread(() -> consumeFrom(conditionQueue, "Condition-Consumer-1", 5), "CondCons-1");

        prod2.start();
        cons2.start();
        prod2.join();
        cons2.join();

        System.out.println("\n[Key Takeaway for Interviews]:");
        System.out.println("  - Intrinsic synchronized locks have only ONE wait-set per object.");
        System.out.println("  - notify() might wake another producer when space is full, causing deadlocks.");
        System.out.println("  - ReentrantLock supports multiple Condition queues, allowing targeted signals");
        System.out.println("    (e.g., consumers signal notFull, producers signal notEmpty).");
    }

    private static void produceTo(CustomQueue<Integer> queue, String name, int count) {
        try {
            for (int i = 1; i <= count; i++) {
                queue.put(i);
                System.out.printf("[%s] Produced item: %d (Queue Size: %d)%n", name, i, queue.size());
                Thread.sleep(50);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static void consumeFrom(CustomQueue<Integer> queue, String name, int count) {
        try {
            for (int i = 1; i <= count; i++) {
                int item = queue.take();
                System.out.printf("[%s] Consumed item: %d (Queue Size: %d)%n", name, item, queue.size());
                Thread.sleep(80);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    interface CustomQueue<T> {
        void put(T item) throws InterruptedException;
        T take() throws InterruptedException;
        int size();
    }

    // 1. Implementation using Intrinsic Monitor
    static class IntrinsicBlockingQueue<T> implements CustomQueue<T> {
        private final Queue<T> queue = new LinkedList<>();
        private final int capacity;

        public IntrinsicBlockingQueue(int capacity) {
            this.capacity = capacity;
        }

        @Override
        public synchronized void put(T item) throws InterruptedException {
            // Rule: Always check condition in a while loop (Spurious Wakeup protection)
            while (queue.size() == capacity) {
                wait();
            }
            queue.add(item);
            // Must use notifyAll() because notify() might wake another Producer!
            notifyAll();
        }

        @Override
        public synchronized T take() throws InterruptedException {
            while (queue.isEmpty()) {
                wait();
            }
            T item = queue.poll();
            notifyAll();
            return item;
        }

        @Override
        public synchronized int size() {
            return queue.size();
        }
    }

    // 2. Implementation using ReentrantLock + Two Conditions
    static class ConditionBlockingQueue<T> implements CustomQueue<T> {
        private final Queue<T> queue = new LinkedList<>();
        private final int capacity;
        private final ReentrantLock lock = new ReentrantLock();
        private final Condition notFull = lock.newCondition();
        private final Condition notEmpty = lock.newCondition();

        public ConditionBlockingQueue(int capacity) {
            this.capacity = capacity;
        }

        @Override
        public void put(T item) throws InterruptedException {
            lock.lock();
            try {
                while (queue.size() == capacity) {
                    notFull.await(); // Producer waits on notFull condition queue
                }
                queue.add(item);
                // Targeted wake up: ONLY wakes a waiting consumer from notEmpty queue!
                notEmpty.signal();
            } finally {
                lock.unlock();
            }
        }

        @Override
        public T take() throws InterruptedException {
            lock.lock();
            try {
                while (queue.isEmpty()) {
                    notEmpty.await(); // Consumer waits on notEmpty condition queue
                }
                T item = queue.poll();
                // Targeted wake up: ONLY wakes a waiting producer from notFull queue!
                notFull.signal();
                return item;
            } finally {
                lock.unlock();
            }
        }

        @Override
        public int size() {
            lock.lock();
            try {
                return queue.size();
            } finally {
                lock.unlock();
            }
        }
    }
}
