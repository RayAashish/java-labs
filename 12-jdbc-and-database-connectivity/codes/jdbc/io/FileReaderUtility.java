package jdbc.io;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Utility class providing safe, memory-conscious file reading mechanisms.
 * <p>
 * Preserved and enhanced from 'jdbc-with-IO/FileReaderExample.java'.
 * <p>
 * <b>Senior Engineering Note on File I/O:</b>
 * - {@link Files#readAllLines(Path)} loads the entire file into a {@code List<String>} in JVM memory.
 *   This is safe for small files (< 5MB), but triggers OutOfMemoryError on multi-gigabyte log or CSV files.
 * - {@link Files#lines(Path)} or {@link BufferedReader#readLine()} streams line-by-line via an iterator,
 *   maintaining an O(1) constant memory footprint regardless of whether the file has 10 lines or 10,000,000 lines.
 */
public final class FileReaderUtility {

    private FileReaderUtility() {
    }

    /**
     * Resolves the given relative path from current directory or project root.
     * Works for both existing files and write targets whose parent directories exist.
     */
    public static Path resolvePath(String relativeOrAbsolutePath) {
        Path path = Paths.get(relativeOrAbsolutePath);
        if (Files.exists(path)) {
            return path;
        }
        // Fallback search inside jdbc/ directory if running from java-labs root
        Path insideJdbc = Paths.get("jdbc", relativeOrAbsolutePath);
        if (Files.exists(insideJdbc)) {
            return insideJdbc;
        }
        // If file does not exist yet, check if parent directory exists under insideJdbc
        if (insideJdbc.getParent() != null && Files.exists(insideJdbc.getParent())) {
            return insideJdbc;
        }
        return path;
    }

    /**
     * Reads all lines into a List (suitable for small configuration and seed data files).
     */
    public static List<String> readAllLines(String filePath) throws IOException {
        Path path = resolvePath(filePath);
        return Files.readAllLines(path, StandardCharsets.UTF_8);
    }

    /**
     * Memory-efficient line streaming for large files.
     * Processes each line sequentially through the consumer callback without retaining prior lines in heap.
     */
    public static void processLinesStream(String filePath, Consumer<String> lineConsumer) throws IOException {
        Path path = resolvePath(filePath);
        try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                lineConsumer.accept(line);
            }
        }
    }

    public static void main(String[] args) {
        System.out.println("=== Testing FileReaderUtility ===");
        try {
            System.out.println("Streaming 'data/students.txt':");
            processLinesStream("data/students.txt", line -> System.out.println("  [LINE] " + line));

            System.out.println("\nStreaming 'data/laptop.txt':");
            processLinesStream("data/laptop.txt", line -> System.out.println("  [LINE] " + line));
        } catch (IOException e) {
            System.err.println("[ERROR] Failed to read files: " + e.getMessage());
        }
    }
}
