package concurrency.phase2_synchronization_and_locks;

import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.StampedLock;

/**
 * Author: raysi
 * Demonstrates:
 * 1. ReentrantReadWriteLock: Shared Readers vs Exclusive Writers.
 *    - Problem: Writer starvation when readers stream continuously.
 * 2. StampedLock (Java 8+):
 *    - Optimistic Reads: Zero-lock reads via validate(stamp) with fallback to pessimistic lock.
 *    - Non-reentrant caveat and dramatic performance boost under 95%+ read contention.
 */
public class ReentrantReadWriteAndStampedLockDemo {

    public static void main(String[] args) throws InterruptedException {
        System.out.println("======================================================================");
        System.out.println("  PHASE 2 LAB: REENTRANT-READ-WRITE-LOCK & STAMPED-LOCK");
        System.out.println("  Author: raysi | Package: com.raysi.concurrency");
        System.out.println("======================================================================\n");

        System.out.println("=== PART 1: ReentrantReadWriteLock Concurrent Readers ===");
        RWDataStore rwStore = new RWDataStore();

        Thread r1 = new Thread(() -> rwStore.read("Reader-1"), "RW-Reader-1");
        Thread r2 = new Thread(() -> rwStore.read("Reader-2"), "RW-Reader-2");
        Thread w1 = new Thread(() -> rwStore.write("Writer-1", 42), "RW-Writer-1");

        r1.start();
        r2.start();
        Thread.sleep(20);
        w1.start();

        r1.join();
        r2.join();
        w1.join();

        System.out.println("\n=== PART 2: StampedLock Optimistic Reading (Zero-Lock Read) ===");
        StampedPoint point = new StampedPoint(10.0, 20.0);

        // Reader tries optimistic reading without holding a shared lock
        Thread optReader = new Thread(() -> {
            double distance = point.distanceFromOriginOptimistic();
            System.out.printf("[Stamped-Optimistic-Reader] Calculated distance: %.2f%n", distance);
        }, "Stamped-Opt-Reader");

        // Concurrently, a writer moves the point
        Thread writer = new Thread(() -> {
            try {
                Thread.sleep(10);
            } catch (InterruptedException ignored) {}
            point.move(5.0, 5.0);
            System.out.println("[Stamped-Writer] Point coordinates updated exclusively.");
        }, "Stamped-Writer");

        optReader.start();
        writer.start();

        optReader.join();
        writer.join();

        System.out.println("\n[Key Takeaway for Interviews]:");
        System.out.println("  - ReentrantReadWriteLock allows N readers or 1 writer, but can suffer writer starvation.");
        System.out.println("  - StampedLock's tryOptimisticRead() does NOT acquire a lock (no CAS, no memory write).");
        System.out.println("  - If validate(stamp) passes, the read was consistent with zero synchronization overhead!");
        System.out.println("  - Caveat: StampedLock is NOT reentrant! Re-acquiring on the same thread will self-deadlock.");
    }

    // 1. Data store guarded by ReentrantReadWriteLock
    static class RWDataStore {
        private int value = 0;
        private final ReentrantReadWriteLock rwl = new ReentrantReadWriteLock();
        private final ReentrantReadWriteLock.ReadLock readLock = rwl.readLock();
        private final ReentrantReadWriteLock.WriteLock writeLock = rwl.writeLock();

        public void read(String readerName) {
            readLock.lock();
            try {
                System.out.printf("[%s] Acquired ReadLock (Shared). Value: %d (Active Readers: %d)%n",
                        readerName, value, rwl.getReadLockCount());
                Thread.sleep(100); // Simulate reading
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                readLock.unlock();
                System.out.printf("[%s] Released ReadLock.%n", readerName);
            }
        }

        public void write(String writerName, int newValue) {
            writeLock.lock(); // Blocks until all active read locks are released!
            try {
                System.out.printf("[%s] Acquired WriteLock (Exclusive). Updating value to: %d%n",
                        writerName, newValue);
                this.value = newValue;
                Thread.sleep(80);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                writeLock.unlock();
                System.out.printf("[%s] Released WriteLock.%n", writerName);
            }
        }
    }

    // 2. 2D Coordinate guarded by StampedLock
    static class StampedPoint {
        private double x, y;
        private final StampedLock sl = new StampedLock();

        public StampedPoint(double x, double y) {
            this.x = x;
            this.y = y;
        }

        public void move(double deltaX, double deltaY) {
            long stamp = sl.writeLock(); // Exclusive write lock
            try {
                x += deltaX;
                y += deltaY;
            } finally {
                sl.unlockWrite(stamp);
            }
        }

        public double distanceFromOriginOptimistic() {
            // Attempt optimistic read (no lock acquired, returns current stamp version)
            long stamp = sl.tryOptimisticRead();
            double currentX = x;
            double currentY = y;

            // Check if a write lock was acquired since tryOptimisticRead()
            if (!sl.validate(stamp)) {
                System.out.println("  [StampedPoint] Optimistic read validated false! Writer intervened. Falling back to Pessimistic ReadLock...");
                stamp = sl.readLock(); // Acquire shared pessimistic read lock
                try {
                    currentX = x;
                    currentY = y;
                } finally {
                    sl.unlockRead(stamp);
                }
            } else {
                System.out.println("  [StampedPoint] Optimistic read validated TRUE! Zero-lock read succeeded.");
            }
            return Math.sqrt(currentX * currentX + currentY * currentY);
        }
    }
}
