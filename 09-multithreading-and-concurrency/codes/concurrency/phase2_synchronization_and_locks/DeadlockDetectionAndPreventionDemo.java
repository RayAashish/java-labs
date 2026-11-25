package concurrency.phase2_synchronization_and_locks;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Author: raysi
 * Demonstrates:
 * 1. Reproducing a Classic Deadlock (Circular Wait condition).
 * 2. Programmatic Deadlock Detection using JVM ThreadMXBean.
 * 3. Prevention Strategies:
 *    a) Global Lock Ordering (Resource hierarchy).
 *    b) Timed Lock Acquisition with Backoff (tryLock(timeout)).
 */
public class DeadlockDetectionAndPreventionDemo {

    private static final Object lockA = new Object();
    private static final Object lockB = new Object();

    public static void main(String[] args) throws InterruptedException {
        System.out.println("======================================================================");
        System.out.println("  PHASE 2 LAB: DEADLOCK DETECTION & PREVENTION");
        System.out.println("  Author: raysi | Package: com.raysi.concurrency");
        System.out.println("======================================================================\n");

        System.out.println("=== PART 1: Deadlock Prevention via Global Lock Ordering ===");
        // When both threads acquire resources in the EXACT same canonical order (A -> B),
        // circular wait condition is mathematically broken! Deadlock is impossible.
        Thread orderedT1 = new Thread(() -> acquireInOrder("Ordered-Worker-1", lockA, lockB), "Ord-1");
        Thread orderedT2 = new Thread(() -> acquireInOrder("Ordered-Worker-2", lockA, lockB), "Ord-2");

        orderedT1.start();
        orderedT2.start();
        orderedT1.join();
        orderedT2.join();
        System.out.println("Ordered acquisition completed cleanly without deadlock!\n");

        System.out.println("=== PART 2: Deadlock Prevention via tryLock(timeout) with Backoff ===");
        ReentrantLock tryLockA = new ReentrantLock();
        ReentrantLock tryLockB = new ReentrantLock();

        Thread tryT1 = new Thread(() -> acquireWithTimedBackoff("Timed-Worker-1", tryLockA, tryLockB), "Timed-1");
        Thread tryT2 = new Thread(() -> acquireWithTimedBackoff("Timed-Worker-2", tryLockB, tryLockA), "Timed-2");

        tryT1.start();
        tryT2.start();
        tryT1.join();
        tryT2.join();
        System.out.println("Timed tryLock acquisition completed cleanly via backoff!\n");

        System.out.println("=== PART 3: Reproducing Deadlock & Programmatic ThreadMXBean Detection ===");
        Thread deadlockedT1 = new Thread(() -> {
            synchronized (lockA) {
                System.out.println("[Deadlock-Thread-1] Acquired Lock A. Waiting for Lock B...");
                sleep(100);
                synchronized (lockB) {
                    System.out.println("[Deadlock-Thread-1] Acquired Lock B.");
                }
            }
        }, "Deadlock-Thread-1");

        Thread deadlockedT2 = new Thread(() -> {
            synchronized (lockB) {
                System.out.println("[Deadlock-Thread-2] Acquired Lock B. Waiting for Lock A...");
                sleep(100);
                synchronized (lockA) {
                    System.out.println("[Deadlock-Thread-2] Acquired Lock A.");
                }
            }
        }, "Deadlock-Thread-2");

        deadlockedT1.start();
        deadlockedT2.start();

        // Give threads time to deadlock
        sleep(500);

        System.out.println("\n[Monitor] Inspecting JVM for Deadlocked Threads via ThreadMXBean...");
        ThreadMXBean mxBean = ManagementFactory.getThreadMXBean();
        long[] deadlockedThreadIds = mxBean.findDeadlockedThreads();

        if (deadlockedThreadIds != null && deadlockedThreadIds.length > 0) {
            System.err.println(">>> ALERT: DEADLOCK DETECTED! Total stuck threads: " + deadlockedThreadIds.length);
            ThreadInfo[] threadInfos = mxBean.getThreadInfo(deadlockedThreadIds);
            for (ThreadInfo info : threadInfos) {
                System.err.printf("  - Thread '%s' (ID: %d) is in state %s, blocked on lock '%s' held by '%s'%n",
                        info.getThreadName(), info.getThreadId(), info.getThreadState(),
                        info.getLockName(), info.getLockOwnerName());
            }
            System.out.println("\n[Note] In production, use 'jstack <PID>' or APM tools to dump threads when deadlocks occur.");
        } else {
            System.out.println("No deadlock detected.");
        }
    }

    private static void acquireInOrder(String name, Object first, Object second) {
        synchronized (first) {
            System.out.printf("[%s] Acquired first lock.%n", name);
            sleep(30);
            synchronized (second) {
                System.out.printf("[%s] Acquired second lock. Work done.%n", name);
            }
        }
    }

    private static void acquireWithTimedBackoff(String name, ReentrantLock first, ReentrantLock second) {
        boolean acquired = false;
        while (!acquired) {
            try {
                if (first.tryLock(50, TimeUnit.MILLISECONDS)) {
                    try {
                        System.out.printf("[%s] Acquired first lock. Attempting second...%n", name);
                        if (second.tryLock(50, TimeUnit.MILLISECONDS)) {
                            try {
                                System.out.printf("[%s] Acquired BOTH locks! Successfully executing work.%n", name);
                                acquired = true;
                            } finally {
                                second.unlock();
                            }
                        } else {
                            System.out.printf("[%s] Second lock busy! Releasing first lock and backing off...%n", name);
                        }
                    } finally {
                        first.unlock();
                    }
                }
                if (!acquired) {
                    Thread.sleep((long) (Math.random() * 50)); // Random jitter backoff
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    private static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
