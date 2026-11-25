package ionio.phase2_modern_nio2_files;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

/**
 * Author: raysi
 * Package: com.raysi.ionio.phase2_modern_nio2_files.codes
 *
 * Demonstrates:
 * 1. The OutOfMemoryError trap of Files.readAllLines(Path) on large files.
 * 2. Constant-memory lazy streaming via Files.lines(Path, StandardCharsets.UTF_8).
 * 3. JVM Heap memory consumption monitoring during stream processing.
 * 4. Why Files.lines() MUST be inside a try-with-resources block to prevent File Descriptor leaks.
 */
public class LazyStreamLargeFileLinesDemo {

    private static final int LINE_COUNT = 250_000;

    public static void main(String[] args) throws IOException {
        System.out.println("==========================================================================");
        System.out.println(">>> DEMO: Lazy File Streaming (Files.lines()) vs Heap Exhaustion <<<");
        System.out.println(">>> Author: raysi | Processing " + LINE_COUNT + " Log Lines Safely");
        System.out.println("==========================================================================\n");

        Path tempLogFile = Files.createTempFile("server_access_log_", ".log");

        try {
            // Step 1: Generate synthetic 250,000-line log file
            System.out.println("[Step 1] Generating " + LINE_COUNT + " synthetic log lines on disk...");
            generateLargeLogFile(tempLogFile, LINE_COUNT);
            long fileSizeBytes = Files.size(tempLogFile);
            System.out.printf("   └── Log file created: %s (%.2f MB on disk)%n%n",
                    tempLogFile, (double) fileSizeBytes / (1024 * 1024));

            // Step 2: Lazy Streaming with Files.lines() inside try-with-resources
            System.out.println("[Step 2] Processing file via lazy Files.lines() stream...");
            System.gc(); // Hint GC before measurement
            long heapBeforeStream = getUsedMemory();

            AtomicLong errorCount = new AtomicLong(0);
            AtomicLong warnCount = new AtomicLong(0);
            long startStream = System.nanoTime();

            // CRITICAL: Files.lines() implements AutoCloseable! Must be in try-with-resources!
            try (Stream<String> lineStream = Files.lines(tempLogFile, StandardCharsets.UTF_8)) {
                lineStream.forEach(line -> {
                    if (line.contains("ERROR")) {
                        errorCount.incrementAndGet();
                    } else if (line.contains("WARN")) {
                        warnCount.incrementAndGet();
                    }
                });
            }

            long streamDurationMs = (System.nanoTime() - startStream) / 1_000_000;
            long heapAfterStream = getUsedMemory();
            long heapUsedByStreamMb = Math.max(0, (heapAfterStream - heapBeforeStream) / (1024 * 1024));

            System.out.printf("   ├── Processed: %d lines in %d ms%n", LINE_COUNT, streamDurationMs);
            System.out.printf("   ├── Found: %d ERRORs, %d WARNs%n", errorCount.get(), warnCount.get());
            System.out.printf("   └── Heap Memory Used: ~%d MB (Constant memory footprint!)%n%n", heapUsedByStreamMb);

            // Step 3: Demonstrating the memory overhead of Files.readAllLines()
            System.out.println("[Step 3] Demonstrating memory footprint of Files.readAllLines()...");
            System.gc();
            long heapBeforeEager = getUsedMemory();
            long startEager = System.nanoTime();

            // Loads the ENTIRE file into an in-memory ArrayList<String>!
            List<String> allLines = Files.readAllLines(tempLogFile, StandardCharsets.UTF_8);

            long eagerDurationMs = (System.nanoTime() - startEager) / 1_000_000;
            long heapAfterEager = getUsedMemory();
            long heapUsedByEagerMb = (heapAfterEager - heapBeforeEager) / (1024 * 1024);

            System.out.printf("   ├── Eager Loaded: %d lines in %d ms%n", allLines.size(), eagerDurationMs);
            System.out.printf("   └── Heap Memory Retained: ~%d MB (Dangerous on 10 GB files!)%n%n", heapUsedByEagerMb);

            // Comparison Summary
            System.out.println("==========================================================================");
            System.out.println(">>> ARCHITECTURAL VERDICT <<<");
            System.out.println("==========================================================================");
            System.out.println("1. Files.readAllLines() creates a java.lang.String instance for every line");
            System.out.println("   and retains all of them concurrently in an ArrayList on the JVM Heap.");
            System.out.println("   On a 10 GB file with 50M lines, this requires ~35 GB of heap memory,");
            System.out.println("   guaranteeing java.lang.OutOfMemoryError: Java heap space!");
            System.out.println("2. Files.lines() wraps a BufferedReader in a Stream<String>. Lines are");
            System.out.println("   fetched lazily one at a time and become immediately eligible for GC");
            System.out.println("   after the terminal operation processes them.");
            System.out.println("3. ALWAYS wrap Files.lines() in try-with-resources. Unclosed streams");
            System.out.println("   leak the underlying OS File Descriptor, leading to 'Too many open files'.");
            System.out.println("==========================================================================");

        } finally {
            Files.deleteIfExists(tempLogFile);
        }
    }

    private static void generateLargeLogFile(Path path, int lines) throws IOException {
        String[] levels = {"INFO", "DEBUG", "WARN", "ERROR"};
        try (BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            for (int i = 1; i <= lines; i++) {
                String level = levels[i % levels.length];
                writer.write("2026-10-18 14:32:05.123 [" + level + "] [worker-thread-" + (i % 16)
                        + "] com.raysi.service.OrderService - Request transaction processed id=" + i);
                writer.newLine();
            }
        }
    }

    private static long getUsedMemory() {
        Runtime rt = Runtime.getRuntime();
        return rt.totalMemory() - rt.freeMemory();
    }
}
