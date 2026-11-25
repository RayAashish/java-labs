package concurrency.phase2_synchronization_and_locks;

/**
 * Author: raysi
 * Demonstrates:
 * 1. JVM Object Header Layout (Mark Word 64-bit on 64-bit architectures).
 * 2. The Lock Escalation Lifecycle:
 *    Unlocked (001) -> Lightweight Lock (000, Stack BasicObjectLock + CAS)
 *    -> Heavyweight Lock (010, ObjectMonitor inflated with OS Mutex).
 * 3. Bytecode mechanics: 'monitorenter' and 'monitorexit' with exception handling tables.
 */
public class ObjectHeaderMarkWordMonitorDemo {

    // Simple lock object
    private static final Object lock = new Object();

    public static void main(String[] args) throws InterruptedException {
        System.out.println("======================================================================");
        System.out.println("  PHASE 2 LAB: OBJECT HEADER MARK WORD & LOCK ESCALATION");
        System.out.println("  Author: raysi | Package: com.raysi.concurrency");
        System.out.println("======================================================================\n");

        printObjectHeaderTheory();

        System.out.println("=== PART 1: Low Contention (Lightweight Lock / Thin Lock) ===");
        // When a single thread acquires a synchronized block with zero contention,
        // HotSpot avoids inflating an OS Mutex. It pushes a BasicObjectLock frame
        // onto the thread's stack and executes a CAS on the Object Header's Mark Word.
        synchronized (lock) {
            System.out.println("[Thread-Main] Holding lock without contention.");
            System.out.println("[Thread-Main] Mark Word points to stack record of Main thread (tag: 00).");
        }

        System.out.println("\n=== PART 2: High Contention (Inflation to Heavyweight ObjectMonitor) ===");
        // When multiple threads contend for the same lock simultaneously,
        // CAS spinning fails and the JVM 'inflates' the lock into a C++ ObjectMonitor:
        // - Allocates an ObjectMonitor in native C++ heap
        // - Mark Word tag becomes '010' pointing to the ObjectMonitor pointer
        // - Contending threads are enqueued into _cxq / _EntryList and suspended using OS Parker (pthread_mutex)

        Thread t1 = new Thread(() -> executeHeavySynchronizedWork("Contender-1"), "Contender-1");
        Thread t2 = new Thread(() -> executeHeavySynchronizedWork("Contender-2"), "Contender-2");

        t1.start();
        t2.start();

        t1.join();
        t2.join();

        System.out.println("\n=== PART 3: Bytecode Representation of synchronized ===");
        System.out.println("When compiled to bytecode, synchronized(obj) creates:");
        System.out.println("  1. 'monitorenter' instruction (increments recursion counter in MarkWord/ObjectMonitor)");
        System.out.println("  2. 'monitorexit' instruction on normal return");
        System.out.println("  3. An Exception Table mapping any Throwable to an alternate 'monitorexit' path!");
        System.out.println("     This guarantees locks are ALWAYS released even if an unhandled Exception occurs.");
    }

    private static void executeHeavySynchronizedWork(String threadName) {
        synchronized (lock) {
            System.out.printf("[%s] Acquired lock! Executing critical section (Lock inflated to Heavyweight ObjectMonitor).%n",
                    threadName);
            try {
                // Simulate intensive operation causing other thread to block on OS Mutex
                Thread.sleep(150);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            System.out.printf("[%s] Exiting critical section and releasing ObjectMonitor._owner.%n", threadName);
        }
    }

    private static void printObjectHeaderTheory() {
        System.out.println("--- JVM 64-bit HotSpot Object Header Architecture ---");
        System.out.println("Every Java Object on the Heap consists of:");
        System.out.println("1. Mark Word (64 bits / 8 bytes): Identity HashCode, GC Age, Biased/Lock Bits");
        System.out.println("2. Klass Word (64 bits or 32 bits with CompressedOOPs): Pointer to Metaspace Class");
        System.out.println("3. Instance Fields: Primitives and References");
        System.out.println("4. Alignment Padding: Pads object to multiple of 8 bytes");
        System.out.println("\nMark Word 2-bit State Tags:");
        System.out.println("  [001] -> Normal / Unlocked (Identity HashCode, Age=4 bits, Biased=0, Tag=01)");
        System.out.println("  [000] -> Lightweight Lock (Pointer to Stack Displaced Mark Word)");
        System.out.println("  [010] -> Heavyweight Lock (Pointer to C++ native ObjectMonitor structure)");
        System.out.println("  [011] -> Marked for Garbage Collection");
        System.out.println("------------------------------------------------------\n");
    }
}
