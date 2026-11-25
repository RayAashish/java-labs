package concurrency.phase1_threads_and_memory_model;

/**
 * Author: raysi
 * Demonstrates the 6 distinct lifecycle states of a Java Thread (java.lang.Thread.State)
 * and verifies their transitions programmatically:
 * 1. NEW
 * 2. RUNNABLE (Maps to both OS Ready and OS Running)
 * 3. BLOCKED (Waiting to acquire an intrinsic monitor lock)
 * 4. WAITING (Waiting indefinitely for notify(), join(), or LockSupport.park())
 * 5. TIMED_WAITING (Waiting with a timeout: sleep(), wait(ms), join(ms), parkNanos())
 * 6. TERMINATED (Completed execution)
 */
public class ThreadStatesLifecycleInspector {

    private static final Object lock = new Object();

    public static void main(String[] args) throws InterruptedException {
        System.out.println("======================================================================");
        System.out.println("  PHASE 1 LAB: JVM THREAD LIFECYCLE & STATE TRANSITIONS INSPECTOR");
        System.out.println("  Author: raysi | Package: com.raysi.concurrency");
        System.out.println("======================================================================\n");

        // 1. NEW State: Instantiated but start() has not yet been invoked
        Thread target = new Thread(() -> {
            try {
                // Will enter TIMED_WAITING
                Thread.sleep(300);

                // Will enter BLOCKED (lock held by main thread)
                synchronized (lock) {
                    // Will enter WAITING
                    lock.wait();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "Target-Worker-Thread");

        printState("1. After new Thread()", target, Thread.State.NEW);

        // 2. RUNNABLE State: start() is called; thread is ready or actively executing bytecode
        target.start();
        printState("2. Immediately after start()", target, Thread.State.RUNNABLE);

        // 3. TIMED_WAITING State: target enters Thread.sleep(300)
        Thread.sleep(100);
        printState("3. During Thread.sleep(300)", target, Thread.State.TIMED_WAITING);

        // 4. BLOCKED State: main thread holds 'lock', while target wakes up and tries to enter synchronized(lock)
        synchronized (lock) {
            // Wait for target to finish sleep and attempt lock acquisition
            Thread.sleep(300);
            printState("4. Attempting to enter synchronized(lock)", target, Thread.State.BLOCKED);

            // 5. WAITING State: We let target acquire the lock and call lock.wait()
            // We sleep briefly after notify to verify WAITING before notification
        }

        // Give target time to acquire the lock and call lock.wait()
        Thread.sleep(100);
        printState("5. After calling lock.wait()", target, Thread.State.WAITING);

        // Wake target thread up
        synchronized (lock) {
            lock.notify();
        }

        // Wait for target to finish execution
        target.join();

        // 6. TERMINATED State: run() method has completed
        printState("6. After run() completion & join()", target, Thread.State.TERMINATED);

        System.out.println("\n[Key Takeaway for Interviews]:");
        System.out.println("  - Java has 6 Thread states defined in Thread.State enum.");
        System.out.println("  - In Java, 'RUNNABLE' encompasses BOTH OS Ready (in run queue) and OS Running (on CPU).");
        System.out.println("  - 'BLOCKED' is ONLY used when waiting for a synchronized monitor lock.");
        System.out.println("  - 'WAITING'/'TIMED_WAITING' are entered via wait(), join(), LockSupport.park(), or ReentrantLock.");
    }

    private static void printState(String stage, Thread thread, Thread.State expected) {
        Thread.State actual = thread.getState();
        String status = (actual == expected) ? "[PASS]" : "[FAIL]";
        System.out.printf("%-42s | Expected: %-13s | Actual: %-13s %s%n",
                stage, expected, actual, status);
    }
}
