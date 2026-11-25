package concurrency.phase5_virtual_threads_project_loom;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Author: raysi
 * Demonstrates:
 * 1. Carrier Thread Mounting & Unmounting: How Virtual Threads hop across OS Carrier Threads
 *    (ForkJoinPool-1-worker-X) during blocking I/O calls.
 * 2. The Thread Pinning Trap: Why 'synchronized' blocks pin the Virtual Thread to the OS Carrier Thread.
 * 3. The Production Fix: Replacing 'synchronized' with 'ReentrantLock' to restore cooperative unmounting.
 * 4. Diagnostics: Explaining the JVM flag -Djdk.tracePinnedThreads=full.
 */
public class CarrierThreadMountingAndPinningTrapDemo {

    private static final Object intrinsicLock = new Object();
    private static final ReentrantLock explicitLock = new ReentrantLock();

    public static void main(String[] args) throws InterruptedException {
        System.out.println("======================================================================");
        System.out.println("  PHASE 5 LAB: CARRIER THREAD MOUNTING & THE PINNING TRAP");
        System.out.println("  Author: raysi | Package: com.raysi.concurrency");
        System.out.println("======================================================================\n");

        System.out.println("=== PART 1: Carrier Thread Mounting & Dynamic Carrier Hopping ===");
        CountDownLatch latch1 = new CountDownLatch(1);
        Thread vt1 = Thread.ofVirtual().name("VirtualWorker-1").start(() -> {
            printCurrentThreadState("Before blocking sleep");

            try {
                // When sleep() is invoked, continuation unmounts from carrier thread!
                Thread.sleep(100);
            } catch (InterruptedException ignored) {}

            // When waking up, the virtual thread may be mounted on a DIFFERENT carrier worker!
            printCurrentThreadState("After waking from sleep (Notice potential carrier change!)");
            latch1.countDown();
        });
        latch1.await();
        vt1.join();

        System.out.println("\n=== PART 2: The Thread Pinning Trap with 'synchronized' ===");
        System.out.println("[Warning] In Java 21, blocking inside a 'synchronized' block PINS the virtual thread");
        System.out.println("          to its OS Carrier Thread, preventing unmounting!");
        CountDownLatch latch2 = new CountDownLatch(1);
        Thread pinnedVt = Thread.ofVirtual().name("PinnedVirtualWorker").start(() -> {
            synchronized (intrinsicLock) {
                printCurrentThreadState("Inside synchronized block (PINNED to Carrier Thread!)");
                try {
                    // PINNING HAZARD: Carrier thread cannot be freed!
                    Thread.sleep(100);
                } catch (InterruptedException ignored) {}
            }
            latch2.countDown();
        });
        latch2.await();
        pinnedVt.join();

        System.out.println("\n=== PART 3: The Production Solution: Use ReentrantLock ===");
        CountDownLatch latch3 = new CountDownLatch(1);
        Thread unpinnedVt = Thread.ofVirtual().name("UnpinnedVirtualWorker").start(() -> {
            explicitLock.lock();
            try {
                printCurrentThreadState("Inside ReentrantLock (NOT pinned! Safely unmounts)");
                try {
                    // ReentrantLock uses LockSupport.park() -> Virtual thread unmounts cleanly!
                    Thread.sleep(100);
                } catch (InterruptedException ignored) {}
            } finally {
                explicitLock.unlock();
            }
            latch3.countDown();
        });
        latch3.await();
        unpinnedVt.join();

        System.out.println("\n[Key Takeaway for Interviews]:");
        System.out.println("  - A Virtual Thread is mounted on a Carrier Thread (ForkJoinPool worker) to execute bytecode.");
        System.out.println("  - Pinning happens in two cases: (1) synchronized block/method, (2) Native method / JNI call.");
        System.out.println("  - When pinned, the OS Carrier Thread is frozen while waiting, defeating Loom's scalability.");
        System.out.println("  - Detect pinning in production using JVM flag: -Djdk.tracePinnedThreads=full");
        System.out.println("  - Fix: Refactor synchronized blocks guarding blocking I/O into ReentrantLock.");
    }

    private static void printCurrentThreadState(String stage) {
        Thread current = Thread.currentThread();
        System.out.printf("[%s] Stage: %-45s | Current Thread: %s%n",
                current.getName(), stage, current);
    }
}
