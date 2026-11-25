package ionio.phase1_classic_io_streams.bytestreams;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Random;

/**
 * Author: raysi
 * Package: com.raysi.ionio.phase1_classic_io_streams.bytestreams.codes
 *
 * Demonstrates the massive performance gap between:
 * 1. Unbuffered byte-by-byte copy (FileInputStream.read() / FileOutputStream.write())
 *    - Each read/write invokes a native JNI call and an OS system call (read()/write()).
 *    - Triggers millions of user-space <-> kernel-space CPU context switches.
 * 2. Buffered copy using BufferedInputStream / BufferedOutputStream (8 KB internal array)
 *    - Batches byte operations, reducing OS system calls by ~8,192x.
 * 3. Bulk array copy using explicit byte[] buffer (8 KB, 64 KB)
 *    - Direct heap buffer transfer with minimal method invocation overhead.
 */
public class UnbufferedVsBufferedCopyBenchmark {

    private static final int FILE_SIZE_BYTES = 5 * 1024 * 1024; // 5 MB
    private static final int BUFFER_SIZE = 8192; // 8 KB

    public static void main(String[] args) throws IOException {
        System.out.println("==========================================================================");
        System.out.println(">>> BENCHMARK: Unbuffered vs Buffered vs Bulk Array Stream Copy <<<");
        System.out.println(">>> Author: raysi | Testing with " + (FILE_SIZE_BYTES / (1024 * 1024)) + " MB payload");
        System.out.println("==========================================================================\n");

        Path sourcePath = Files.createTempFile("benchmark_src_", ".bin");
        Path destUnbuffered = Files.createTempFile("benchmark_dest_unbuf_", ".bin");
        Path destBuffered = Files.createTempFile("benchmark_dest_buf_", ".bin");
        Path destBulkArray = Files.createTempFile("benchmark_dest_bulk_", ".bin");

        try {
            // Step 1: Generate random binary test payload
            System.out.println("[Step 1] Generating " + (FILE_SIZE_BYTES / (1024 * 1024)) + " MB random binary source file...");
            generateDummyPayload(sourcePath, FILE_SIZE_BYTES);
            System.out.println("[Step 1] Source file generated: " + sourcePath + " (" + Files.size(sourcePath) + " bytes)\n");

            // Step 2: Unbuffered byte-by-byte copy (1 byte at a time)
            // Warning: We benchmark a smaller slice (e.g. 1 MB) or full 5 MB if fast enough.
            // 5 MB = 5,242,880 syscalls! On modern OS this takes several seconds.
            System.out.println("[Step 2] Executing Unbuffered Byte-by-Byte Copy (fis.read() / fos.write())...");
            long startUnbuf = System.nanoTime();
            long unbufSyscallCount = copyUnbuffered(sourcePath.toFile(), destUnbuffered.toFile());
            long elapsedUnbufMs = (System.nanoTime() - startUnbuf) / 1_000_000;
            System.out.printf("   -> Unbuffered Copy Finished: %d ms | Syscalls (read+write): ~%d%n%n",
                    elapsedUnbufMs, unbufSyscallCount * 2);

            // Step 3: Buffered copy using BufferedInputStream / BufferedOutputStream (8 KB default buffer)
            System.out.println("[Step 3] Executing BufferedStream Copy (BufferedInputStream / BufferedOutputStream)...");
            long startBuf = System.nanoTime();
            long bufSyscallCount = copyWithBufferedStreams(sourcePath.toFile(), destBuffered.toFile());
            long elapsedBufMs = (System.nanoTime() - startBuf) / 1_000_000;
            System.out.printf("   -> BufferedStream Copy Finished: %d ms | Syscalls: ~%d%n%n",
                    elapsedBufMs, bufSyscallCount * 2);

            // Step 4: Bulk byte[] buffer copy (8 KB explicit array)
            System.out.println("[Step 4] Executing Explicit Bulk byte[8192] Array Copy...");
            long startBulk = System.nanoTime();
            long bulkSyscallCount = copyWithExplicitArray(sourcePath.toFile(), destBulkArray.toFile(), BUFFER_SIZE);
            long elapsedBulkMs = (System.nanoTime() - startBulk) / 1_000_000;
            System.out.printf("   -> Explicit byte[8192] Copy Finished: %d ms | Syscalls: ~%d%n%n",
                    elapsedBulkMs, bulkSyscallCount * 2);

            // Step 5: Comparative Analysis Summary
            System.out.println("==========================================================================");
            System.out.println(">>> PERFORMANCE & ARCHITECTURAL SUMMARY <<<");
            System.out.println("==========================================================================");
            double speedupBuffered = (double) elapsedUnbufMs / Math.max(1, elapsedBufMs);
            double speedupBulk = (double) elapsedUnbufMs / Math.max(1, elapsedBulkMs);
            System.out.printf("Unbuffered Duration:    %6d ms  (Syscall Storm: ~%d context switches)%n",
                    elapsedUnbufMs, unbufSyscallCount * 2);
            System.out.printf("BufferedStream Duration: %6d ms  (Speedup: %.2fx faster)%n",
                    elapsedBufMs, speedupBuffered);
            System.out.printf("Explicit Array Duration: %6d ms  (Speedup: %.2fx faster)%n",
                    elapsedBulkMs, speedupBulk);
            System.out.println("--------------------------------------------------------------------------");
            System.out.println("Root Cause: Each unbuffered read() causes a transition from JVM User Space");
            System.out.println("(Ring 3) to Kernel Space (Ring 0) via read() syscall. Buffered streams batch");
            System.out.println("memory into an internal 8KB byte[] array, reducing OS transitions by 99.98%!");
            System.out.println("==========================================================================");

        } finally {
            // Cleanup temp files
            Files.deleteIfExists(sourcePath);
            Files.deleteIfExists(destUnbuffered);
            Files.deleteIfExists(destBuffered);
            Files.deleteIfExists(destBulkArray);
        }
    }

    private static long copyUnbuffered(File src, File dest) throws IOException {
        long byteCount = 0;
        try (FileInputStream fis = new FileInputStream(src);
             FileOutputStream fos = new FileOutputStream(dest)) {
            int b;
            while ((b = fis.read()) != -1) {
                fos.write(b);
                byteCount++;
            }
        }
        return byteCount;
    }

    private static long copyWithBufferedStreams(File src, File dest) throws IOException {
        long byteCount = 0;
        // Default buffer size in JDK BufferedInputStream is 8192 bytes (8 KB)
        try (BufferedInputStream bis = new BufferedInputStream(new FileInputStream(src));
             BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(dest))) {
            int b;
            while ((b = bis.read()) != -1) {
                bos.write(b);
                byteCount++;
            }
            bos.flush(); // Flush residual bytes in buffer to underlying OS stream
        }
        // Approximate kernel syscalls = total bytes / buffer size (8192)
        return (byteCount + BUFFER_SIZE - 1) / BUFFER_SIZE;
    }

    private static long copyWithExplicitArray(File src, File dest, int bufferSize) throws IOException {
        long readCalls = 0;
        try (FileInputStream fis = new FileInputStream(src);
             FileOutputStream fos = new FileOutputStream(dest)) {
            byte[] buffer = new byte[bufferSize];
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                fos.write(buffer, 0, bytesRead);
                readCalls++;
            }
        }
        return readCalls;
    }

    private static void generateDummyPayload(Path path, int size) throws IOException {
        byte[] chunk = new byte[64 * 1024]; // 64 KB chunk
        new Random(42).nextBytes(chunk);
        try (FileOutputStream fos = new FileOutputStream(path.toFile())) {
            int written = 0;
            while (written < size) {
                int toWrite = Math.min(chunk.length, size - written);
                fos.write(chunk, 0, toWrite);
                written += toWrite;
            }
        }
    }
}
