package ionio.phase3_nio_buffers_channels.channels_and_zerocopy;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Random;

/**
 * Author: raysi
 * Package: com.raysi.ionio.phase3_nio_buffers_channels.channels_and_zerocopy.codes
 *
 * Demonstrates:
 * 1. Traditional 4-Copy / 4-Context-Switch I/O vs Linux sendfile(2) Zero-Copy.
 * 2. FileChannel.transferTo() / transferFrom() mechanics.
 * 3. Benchmark on a 15 MB payload comparing:
 *    - Traditional ByteBuffer chunk loop (User Space intermediary)
 *    - OS Zero-Copy transferTo() (Kernel-level DMA bypass)
 */
public class FileChannelTransferToZeroCopyDemo {

    private static final int FILE_SIZE_BYTES = 15 * 1024 * 1024; // 15 MB
    private static final int CHUNK_SIZE = 64 * 1024; // 64 KB

    public static void main(String[] args) throws IOException {
        System.out.println("==========================================================================");
        System.out.println(">>> DEMO: Linux OS Zero-Copy via FileChannel.transferTo() <<<");
        System.out.println(">>> Author: raysi | DMA Hardware Transfer & sendfile(2) Syscall");
        System.out.println("==========================================================================\n");

        Path sourceFile = Files.createTempFile("zerocopy_src_", ".bin");
        Path destTraditional = Files.createTempFile("dest_trad_", ".bin");
        Path destZeroCopy = Files.createTempFile("dest_zerocopy_", ".bin");

        try {
            // Step 1: Generate test data
            System.out.println("[Step 1] Generating " + (FILE_SIZE_BYTES / (1024 * 1024)) + " MB source binary file...");
            generatePayload(sourceFile, FILE_SIZE_BYTES);
            System.out.println("   └── Created source: " + sourceFile + " (" + Files.size(sourceFile) + " bytes)\n");

            // Step 2: Traditional Channel Copy (User-Space ByteBuffer Loop)
            System.out.println("[Step 2] Executing Traditional Channel Copy (ByteBuffer loop)...");
            long startTrad = System.nanoTime();
            long tradBytes = copyTraditional(sourceFile, destTraditional);
            long tradDurationMs = (System.nanoTime() - startTrad) / 1_000_000;
            System.out.printf("   ├── Traditional Copy Finished: %d ms (%d bytes copied)%n", tradDurationMs, tradBytes);
            System.out.println("   └── Context Switches: 4 per chunk | Data Copies: 4 (Disk -> Kernel -> User -> Kernel -> Disk)\n");

            // Step 3: Zero-Copy Transfer via FileChannel.transferTo()
            System.out.println("[Step 3] Executing Zero-Copy Transfer via FileChannel.transferTo()...");
            long startZero = System.nanoTime();
            long zeroBytes = copyZeroCopy(sourceFile, destZeroCopy);
            long zeroDurationMs = (System.nanoTime() - startZero) / 1_000_000;
            System.out.printf("   ├── Zero-Copy Finished:        %d ms (%d bytes copied)%n", zeroDurationMs, zeroBytes);
            System.out.println("   └── Context Switches: 2 total   | Data Copies: 0 CPU Copies (Direct DMA Transfer)\n");

            // Step 4: Summary & Speedup
            System.out.println("==========================================================================");
            System.out.println(">>> PERFORMANCE COMPARISON <<<");
            System.out.println("==========================================================================");
            System.out.printf("Traditional Channel Duration: %6d ms%n", tradDurationMs);
            System.out.printf("Zero-Copy transferTo Duration: %6d ms (Speedup: %.2fx)%n",
                    zeroDurationMs, (double) tradDurationMs / Math.max(1, zeroDurationMs));
            System.out.println("--------------------------------------------------------------------------");
            System.out.println("Why Zero-Copy Wins:");
            System.out.println("1. Under Linux/Unix, transferTo() invokes the sendfile() or copy_file_range()");
            System.out.println("   system call. Data never enters JVM user-space memory!");
            System.out.println("2. Hardware DMA moves data directly between page cache pages or to NIC socket");
            System.out.println("   buffers using Scatter-Gather DMA descriptors. Zero CPU cycles wasted!");
            System.out.println("==========================================================================");

        } finally {
            Files.deleteIfExists(sourceFile);
            Files.deleteIfExists(destTraditional);
            Files.deleteIfExists(destZeroCopy);
        }
    }

    private static long copyTraditional(Path src, Path dst) throws IOException {
        long totalWritten = 0;
        try (FileChannel inChannel = FileChannel.open(src, StandardOpenOption.READ);
             FileChannel outChannel = FileChannel.open(dst, StandardOpenOption.CREATE, StandardOpenOption.WRITE)) {

            ByteBuffer buffer = ByteBuffer.allocateDirect(CHUNK_SIZE);
            while (inChannel.read(buffer) != -1) {
                buffer.flip(); // Prepare to read from buffer
                while (buffer.hasRemaining()) {
                    totalWritten += outChannel.write(buffer);
                }
                buffer.clear(); // Reset pointers for next read
            }
        }
        return totalWritten;
    }

    private static long copyZeroCopy(Path src, Path dst) throws IOException {
        long totalTransferred = 0;
        try (FileChannel inChannel = FileChannel.open(src, StandardOpenOption.READ);
             FileChannel outChannel = FileChannel.open(dst, StandardOpenOption.CREATE, StandardOpenOption.WRITE)) {

            long size = inChannel.size();
            long position = 0;

            // In Linux sendfile(), transfer sizes may be capped at 2GB (or 32MB chunks by OS limits)
            while (position < size) {
                long transferred = inChannel.transferTo(position, size - position, outChannel);
                if (transferred <= 0) break;
                position += transferred;
                totalTransferred += transferred;
            }
        }
        return totalTransferred;
    }

    private static void generatePayload(Path path, int size) throws IOException {
        byte[] chunk = new byte[CHUNK_SIZE];
        new Random(77).nextBytes(chunk);
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
