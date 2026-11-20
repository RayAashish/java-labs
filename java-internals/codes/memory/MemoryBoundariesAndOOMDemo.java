package com.raysi.internals.memory;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryType;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * Architectural Demonstration: JVM Memory Boundaries, Stack Limits & Off-Heap Buffers.
 * <p>
 * Key Concepts Demonstrated:
 * 1. Simulating and recovering from {@link StackOverflowError} (calculating max call depth).
 * 2. Off-Heap Direct Memory allocation bypassing the Java Heap via {@link ByteBuffer#allocateDirect(int)}.
 * 3. Inspecting all underlying HotSpot memory pools (Eden, Survivor, Old Gen, Metaspace, CodeCache).
 */
public class MemoryBoundariesAndOOMDemo {

    private static int callDepth = 0;

    public static void main(String[] args) {
        System.out.println("================================================================================");
        System.out.println("   JVM MEMORY BOUNDARIES & REGION PROFILING");
        System.out.println("================================================================================");

        // 1. Inspect HotSpot Memory Pools
        System.out.println("\n[1] Active JVM Memory Pools:");
        List<MemoryPoolMXBean> pools = ManagementFactory.getMemoryPoolMXBeans();
        for (MemoryPoolMXBean pool : pools) {
            System.out.printf("   -> Pool: %-30s | Type: %-8s | Used: %8d KB | Max: %8d KB%n",
                    pool.getName(),
                    pool.getType(),
                    pool.getUsage().getUsed() / 1024,
                    pool.getUsage().getMax() / 1024);
        }

        // 2. Test Stack Overflow Depth
        System.out.println("\n[2] Testing Thread Stack Frame Depth Limit (-Xss):");
        try {
            recursiveCall();
        } catch (StackOverflowError e) {
            System.out.println("   [!] Caught expected StackOverflowError!");
            System.out.println("   -> Maximum Call Stack Depth Reached: " + callDepth + " frames");
        }

        // 3. Off-Heap Direct Memory Allocation (Zero Copy)
        System.out.println("\n[3] Off-Heap Direct Memory Allocation:");
        int directBufferSize = 50 * 1024 * 1024; // 50 MB
        System.out.printf("   -> Allocating %d MB outside the Java Heap via ByteBuffer.allocateDirect()...%n",
                directBufferSize / (1024 * 1024));

        long beforeHeap = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        ByteBuffer directBuffer = ByteBuffer.allocateDirect(directBufferSize);
        long afterHeap = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();

        // Write some data into off-heap memory
        directBuffer.putInt(0xCAFE);
        directBuffer.putInt(0xBABE);
        directBuffer.flip();

        System.out.printf("   -> Direct Buffer Allocated at off-heap capacity: %d bytes%n", directBuffer.capacity());
        System.out.printf("   -> First two 32-bit ints read: 0x%X 0x%X%n", directBuffer.getInt(), directBuffer.getInt());
        System.out.printf("   -> Heap Delta: %d KB (Heap remained virtually unchanged because allocation is OFF-HEAP!)%n",
                (afterHeap - beforeHeap) / 1024);

        System.out.println("\n================================================================================");
        System.out.println("   Memory boundary demonstrations completed successfully.");
        System.out.println("================================================================================");
    }

    private static void recursiveCall() {
        callDepth++;
        recursiveCall();
    }
}
