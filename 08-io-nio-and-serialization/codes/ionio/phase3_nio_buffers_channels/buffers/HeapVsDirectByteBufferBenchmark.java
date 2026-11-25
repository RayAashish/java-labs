package ionio.phase3_nio_buffers_channels.buffers;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

/**
 * Author: raysi
 * Package: com.raysi.ionio.phase3_nio_buffers_channels.buffers.codes
 *
 * Demonstrates:
 * 1. HeapByteBuffer (JVM Heap, byte[] backing array) vs DirectByteBuffer (Off-Heap, native C-Heap memory).
 * 2. Benchmark 1: Allocation Overhead.
 *    - Heap allocation is instantaneous (TLAB pointer bump).
 *    - Direct allocation is expensive (invokes native malloc/mmap, Bits.reserveMemory, and Cleaner registration).
 * 3. Benchmark 2: Channel I/O Throughput.
 *    - HeapByteBuffer forces JVM JNI (IOUtil.java) to allocate a temporary native buffer and double-copy bytes!
 *    - DirectByteBuffer passes the native memory address directly to the OS kernel with zero copy!
 */
public class HeapVsDirectByteBufferBenchmark {

    private static final int BUFFER_SIZE = 64 * 1024; // 64 KB
    private static final int ALLOC_ITERATIONS = 20_000;
    private static final int IO_ITERATIONS = 5_000;

    public static void main(String[] args) throws IOException {
        System.out.println("==========================================================================");
        System.out.println(">>> BENCHMARK: HeapByteBuffer vs DirectByteBuffer (Off-Heap) <<<");
        System.out.println(">>> Author: raysi | Allocation vs Channel I/O Throughput");
        System.out.println("==========================================================================\n");

        // Part 1: Allocation Cost Benchmark
        System.out.println("--- Part 1: Allocation Speed Benchmark (" + ALLOC_ITERATIONS + " iterations of 64KB) ---");

        // Heap Allocation
        long startHeapAlloc = System.nanoTime();
        for (int i = 0; i < ALLOC_ITERATIONS; i++) {
            ByteBuffer buf = ByteBuffer.allocate(BUFFER_SIZE);
        }
        long timeHeapAllocMs = (System.nanoTime() - startHeapAlloc) / 1_000_000;
        System.out.printf("   ├── HeapByteBuffer Allocation Time:   %d ms (Instant TLAB pointer bump)%n", timeHeapAllocMs);

        // Direct Allocation
        long startDirectAlloc = System.nanoTime();
        for (int i = 0; i < ALLOC_ITERATIONS; i++) {
            ByteBuffer buf = ByteBuffer.allocateDirect(BUFFER_SIZE);
        }
        long timeDirectAllocMs = (System.nanoTime() - startDirectAlloc) / 1_000_000;
        System.out.printf("   └── DirectByteBuffer Allocation Time: %d ms (%.2fx slower - malloc/Bits check)%n%n",
                timeDirectAllocMs, (double) timeDirectAllocMs / Math.max(1, timeHeapAllocMs));

        // Part 2: Channel I/O Throughput Benchmark
        System.out.println("--- Part 2: Channel I/O Throughput Benchmark (" + IO_ITERATIONS + " writes of 64KB) ---");

        Path tempHeapFile = Files.createTempFile("io_bench_heap_", ".bin");
        Path tempDirectFile = Files.createTempFile("io_bench_direct_", ".bin");

        try {
            // Reusable buffers to isolate I/O speed from allocation speed
            ByteBuffer heapBuffer = ByteBuffer.allocate(BUFFER_SIZE);
            ByteBuffer directBuffer = ByteBuffer.allocateDirect(BUFFER_SIZE);

            // Populate dummy bytes
            for (int i = 0; i < BUFFER_SIZE; i++) {
                byte val = (byte) (i & 0x7F);
                heapBuffer.put(val);
                directBuffer.put(val);
            }

            // Benchmark Heap Buffer Channel I/O
            long timeHeapIoMs;
            try (FileChannel channel = FileChannel.open(tempHeapFile, StandardOpenOption.WRITE)) {
                long start = System.nanoTime();
                for (int i = 0; i < IO_ITERATIONS; i++) {
                    heapBuffer.position(0);
                    channel.write(heapBuffer);
                }
                timeHeapIoMs = (System.nanoTime() - start) / 1_000_000;
            }
            System.out.printf("   ├── HeapBuffer Channel Write Time:   %d ms (Incurs internal JNI double-copy)%n", timeHeapIoMs);

            // Benchmark Direct Buffer Channel I/O
            long timeDirectIoMs;
            try (FileChannel channel = FileChannel.open(tempDirectFile, StandardOpenOption.WRITE)) {
                long start = System.nanoTime();
                for (int i = 0; i < IO_ITERATIONS; i++) {
                    directBuffer.position(0);
                    channel.write(directBuffer);
                }
                timeDirectIoMs = (System.nanoTime() - start) / 1_000_000;
            }
            System.out.printf("   └── DirectBuffer Channel Write Time: %d ms (Zero JNI copy - DMA direct to OS)%n%n", timeDirectIoMs);

            // Part 3: Memory & GC Architecture Breakdown
            System.out.println("==========================================================================");
            System.out.println(">>> ARCHITECTURAL COMPARISON & GOLDEN RULES <<<");
            System.out.println("==========================================================================");
            System.out.println("1. Allocation:");
            System.out.println("   - HeapByteBuffer allocates on Java Heap inside thread-local TLAB in nanoseconds.");
            System.out.println("   - DirectByteBuffer calls Unsafe.allocateMemory(), tracks memory via");
            System.out.println("     Bits.reserveMemory(), and registers a java.lang.ref.Cleaner.");
            System.out.println("2. I/O Channel Execution (sun.nio.ch.IOUtil):");
            System.out.println("   - The OS kernel syscall (write) CANNOT accept a moving JVM Heap pointer");
            System.out.println("     because Garbage Collection compaction could shift the array mid-transfer!");
            System.out.println("   - Therefore, for HeapByteBuffer, the JVM MUST copy data to a temporary");
            System.out.println("     off-heap buffer first! DirectByteBuffer avoids this copy entirely.");
            System.out.println("3. Golden Rule: Always POOL DirectByteBuffers (e.g. Netty PooledByteBufAllocator)");
            System.out.println("   to amortize allocation cost while enjoying zero-copy I/O throughput.");
            System.out.println("==========================================================================");

        } finally {
            Files.deleteIfExists(tempHeapFile);
            Files.deleteIfExists(tempDirectFile);
        }
    }
}
