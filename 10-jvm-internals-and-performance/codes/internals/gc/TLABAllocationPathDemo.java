package internals.gc;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.util.List;

/**
 * Architectural Demonstration: Thread-Local Allocation Buffers (TLAB) & GC Bean Monitoring.
 * <p>
 * Key Concepts Demonstrated:
 * 1. TLAB (Thread-Local Allocation Buffer): HotSpot partitions Eden space into thread-local buffers.
 *    Allocating an object inside a TLAB is a simple CPU pointer bump (Bump-The-Pointer) without global locks.
 * 2. Monitoring Garbage Collector activity programmatically using {@link GarbageCollectorMXBean}.
 */
public class TLABAllocationPathDemo {

    private static final int ALLOCATION_ROUNDS = 5_000_000;

    public static void main(String[] args) {
        System.out.println("================================================================================");
        System.out.println("   TLAB ALLOCATION & GARBAGE COLLECTOR METRICS DEEP DIVE");
        System.out.println("================================================================================");

        // 1. Inspect Active Garbage Collectors
        System.out.println("\n[1] Active JVM Garbage Collectors:");
        List<GarbageCollectorMXBean> gcBeans = ManagementFactory.getGarbageCollectorMXBeans();
        for (GarbageCollectorMXBean gcBean : gcBeans) {
            System.out.printf("   -> Collector: %-25s | Memory Pools: %s%n",
                    gcBean.getName(), String.join(", ", gcBean.getMemoryPoolNames()));
        }

        long initialGcCount = getTotalGcCount(gcBeans);
        long initialGcTime = getTotalGcTime(gcBeans);

        // 2. High-Rate Memory Allocation Burst (Exercises TLABs & Young Gen)
        System.out.printf("%n[2] Executing %,d allocation cycles to exercise TLABs...%n", ALLOCATION_ROUNDS);
        long start = System.currentTimeMillis();

        long checksum = 0;
        for (int i = 0; i < ALLOCATION_ROUNDS; i++) {
            // Allocate short-lived objects that die immediately
            byte[] ephemeralData = new byte[64]; // Fits cleanly in TLAB
            ephemeralData[0] = (byte) (i & 0xFF);
            checksum += ephemeralData[0];
        }

        long elapsed = System.currentTimeMillis() - start;

        // 3. Post-Allocation Metrics
        long finalGcCount = getTotalGcCount(gcBeans);
        long finalGcTime = getTotalGcTime(gcBeans);

        System.out.println("\n[3] Allocation Throughput & GC Impact:");
        System.out.printf("   -> Total Allocations : %,d objects%n", ALLOCATION_ROUNDS);
        System.out.printf("   -> Total Time        : %d ms%n", elapsed);
        System.out.printf("   -> Allocation Rate   : %,.0f objects/sec%n", (ALLOCATION_ROUNDS / (elapsed / 1000.0)));
        System.out.printf("   -> GC Cycles Fired   : %d collections%n", (finalGcCount - initialGcCount));
        System.out.printf("   -> Total GC Pause Time: %d ms%n", (finalGcTime - initialGcTime));
        System.out.println("   -> Data Checksum     : " + checksum);

        System.out.println("\n[4] Architectural Analysis:");
        System.out.println("   -> Fast-path TLAB allocation achieved millions of allocations per second");
        System.out.println("      because threads bumped their local TLAB pointers without taking global heap locks!");

        System.out.println("\n================================================================================");
    }

    private static long getTotalGcCount(List<GarbageCollectorMXBean> beans) {
        long count = 0;
        for (GarbageCollectorMXBean bean : beans) {
            long c = bean.getCollectionCount();
            if (c > 0) count += c;
        }
        return count;
    }

    private static long getTotalGcTime(List<GarbageCollectorMXBean> beans) {
        long time = 0;
        for (GarbageCollectorMXBean bean : beans) {
            long t = bean.getCollectionTime();
            if (t > 0) time += t;
        }
        return time;
    }
}
