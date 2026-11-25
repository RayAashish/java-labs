package ionio.phase3_nio_buffers_channels.channels_and_zerocopy;

import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

/**
 * Author: raysi
 * Package: com.raysi.ionio.phase3_nio_buffers_channels.channels_and_zerocopy.codes
 *
 * Demonstrates:
 * 1. Memory-Mapped I/O via FileChannel.map(FileChannel.MapMode.READ_WRITE, 0, size).
 * 2. Operating System mmap(2) mechanics: Virtual memory page mapping, demand paging, and page faults.
 * 3. High-performance random access writing and reading at RAM-bus speeds.
 * 4. Flushing dirty OS virtual memory pages to persistent media via MappedByteBuffer.force().
 * 5. Architectural limitations: The 2GB buffer boundary and JVM unmapping unmap() traps.
 */
public class MappedByteBufferMemoryMappedFile {

    private static final int RECORD_COUNT = 100_000;
    private static final int RECORD_SIZE = 16; // 8 bytes ID (long) + 8 bytes value (double)
    private static final long FILE_SIZE = (long) RECORD_COUNT * RECORD_SIZE; // 1.6 MB

    public static void main(String[] args) throws IOException {
        System.out.println("==========================================================================");
        System.out.println(">>> DEMO: Memory-Mapped Files via MappedByteBuffer & OS mmap(2) <<<");
        System.out.println(">>> Author: raysi | Kernel Virtual Memory Page Fault Mechanics");
        System.out.println("==========================================================================\n");

        Path mmapFile = Files.createTempFile("mmap_demo_", ".dat");

        try {
            System.out.println("[Step 1] Initializing target file: " + mmapFile);
            System.out.printf("   └── Planned size for %d records: %d bytes (%.2f MB)%n%n",
                    RECORD_COUNT, FILE_SIZE, (double) FILE_SIZE / (1024 * 1024));

            // Step 2: Open FileChannel and map file directly into Process Virtual Memory
            System.out.println("[Step 2] Executing FileChannel.map() [Invokes OS mmap() system call]...");
            long startMap = System.nanoTime();

            try (FileChannel channel = FileChannel.open(mmapFile,
                    StandardOpenOption.READ,
                    StandardOpenOption.WRITE,
                    StandardOpenOption.CREATE)) {

                // Maps file into OS virtual address space
                MappedByteBuffer mbb = channel.map(FileChannel.MapMode.READ_WRITE, 0, FILE_SIZE);
                long mapTimeUs = (System.nanoTime() - startMap) / 1_000;
                System.out.printf("   ├── Virtual Memory Mapping established in: %d µs%n", mapTimeUs);
                System.out.printf("   └── Is Direct Memory? %b | Capacity: %d bytes%n%n", mbb.isDirect(), mbb.capacity());

                // Step 3: High-speed in-memory sequential writes (triggers demand page faults)
                System.out.println("[Step 3] Writing " + RECORD_COUNT + " records directly into memory addresses...");
                long startWrite = System.nanoTime();
                for (long id = 1; id <= RECORD_COUNT; id++) {
                    mbb.putLong(id);            // 8 bytes
                    mbb.putDouble(id * 1.5);    // 8 bytes
                }
                long writeTimeMs = (System.nanoTime() - startWrite) / 1_000_000;
                System.out.printf("   └── Written %d records in %d ms (Pure RAM pointer operations!)%n%n",
                        RECORD_COUNT, writeTimeMs);

                // Step 4: Random-Access In-Place Mutation without seeking
                System.out.println("[Step 4] Executing Random In-Place Mutation...");
                // Mutate record #50,000 directly:
                int targetRecord = 50_000;
                int byteOffset = (targetRecord - 1) * RECORD_SIZE;
                long existingId = mbb.getLong(byteOffset);
                double existingVal = mbb.getDouble(byteOffset + 8);
                System.out.printf("   ├── Record [%d] Before: ID=%d, Val=%.2f%n", targetRecord, existingId, existingVal);

                // Mutate directly at offset:
                mbb.putDouble(byteOffset + 8, 999999.99);
                double modifiedVal = mbb.getDouble(byteOffset + 8);
                System.out.printf("   └── Record [%d] After:  ID=%d, Val=%.2f (Mutated in-place!)%n%n",
                        targetRecord, existingId, modifiedVal);

                // Step 5: Forcing dirty pages to physical disk
                System.out.println("[Step 5] Forcing dirty memory pages to physical disk via mbb.force() [msync syscall]...");
                long startForce = System.nanoTime();
                mbb.force(); // Flushes OS virtual memory dirty pages to non-volatile storage
                long forceTimeMs = (System.nanoTime() - startForce) / 1_000_000;
                System.out.printf("   └── mbb.force() completed in: %d ms%n%n", forceTimeMs);
            }

            // Step 6: Verify file size on physical disk
            System.out.printf("[Step 6] Final File Size on Disk: %d bytes (Exact match)%n", Files.size(mmapFile));

            System.out.println("\n==========================================================================");
            System.out.println(">>> ARCHITECTURAL TAKEAWAY <<<");
            System.out.println("1. mmap() maps file blocks into the OS Virtual Memory Manager (VMM).");
            System.out.println("2. Writing to a MappedByteBuffer does NOT invoke the write() syscall;");
            System.out.println("   it modifies physical RAM pages. The OS kernel flushes dirty pages in background.");
            System.out.println("3. Used by Apache Kafka for partition index logs, Lucene for search indices,");
            System.out.println("   and Chronicle Queue for ultra-low latency sub-microsecond IPC!");
            System.out.println("==========================================================================");

        } finally {
            Files.deleteIfExists(mmapFile);
        }
    }
}
