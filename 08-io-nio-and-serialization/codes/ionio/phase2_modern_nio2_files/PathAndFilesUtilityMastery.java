package ionio.phase2_modern_nio2_files;

import java.io.IOException;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Set;

/**
 * Author: raysi
 * Package: com.raysi.ionio.phase2_modern_nio2_files.codes
 *
 * Demonstrates:
 * 1. Path manipulation: normalize(), resolve(), relativize().
 * 2. Files atomic file operations (StandardCopyOption.ATOMIC_MOVE -> Linux rename() syscall).
 * 3. File attributes: BasicFileAttributes (batch kernel metadata query in 1 syscall).
 * 4. Exception-driven error handling vs java.io.File's silent boolean false failures.
 * 5. Memory-safe directory iteration: DirectoryStream (lazy cursor) vs Files.list().
 */
public class PathAndFilesUtilityMastery {

    public static void main(String[] args) throws IOException {
        System.out.println("==========================================================================");
        System.out.println(">>> DEMO: Modern NIO.2 Path & Files Utility Mastery <<<");
        System.out.println(">>> Author: raysi | Replacing java.io.File with Path/Files");
        System.out.println("==========================================================================\n");

        Path baseDir = Files.createTempDirectory("nio2_mastery_");
        System.out.println("[Step 1] Working in temporary workspace: " + baseDir);

        try {
            // Part 1: Path Manipulation (Virtual / Non-I/O Path Arithmetic)
            System.out.println("\n--- Part 1: Modern Path Manipulation ---");
            Path basePath = Path.of("/var/log/app");
            Path relativePath = Path.of("service/../metrics/jvm.log");
            Path resolved = basePath.resolve(relativePath);
            Path normalized = resolved.normalize();

            System.out.println("   ├── Base:       " + basePath);
            System.out.println("   ├── Unresolved: " + resolved);
            System.out.println("   ├── Normalized: " + normalized);

            Path pathA = Path.of("/var/log/serviceA/app.log");
            Path pathB = Path.of("/var/log/serviceB/metrics.log");
            System.out.println("   └── Relativize (A to B): " + pathA.relativize(pathB));

            // Part 2: Atomic Moves (Kernel rename() syscall)
            System.out.println("\n--- Part 2: Atomic File Move (ATOMIC_MOVE) ---");
            Path stagingFile = baseDir.resolve("payload.tmp");
            Path targetFile = baseDir.resolve("payload.committed");

            Files.writeString(stagingFile, "TRANSACTION_PAYLOAD_COMMIT_HASH_9918\n",
                    StandardOpenOption.CREATE, StandardOpenOption.WRITE);
            System.out.println("   ├── Written staging file: " + stagingFile + " (Size: " + Files.size(stagingFile) + " B)");

            // Atomic rename guarantees zero half-written state seen by other threads or OS processes
            Files.move(stagingFile, targetFile,
                    StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING);
            System.out.println("   └── Atomically moved staging -> target via kernel rename() syscall!");

            // Part 3: Metadata in 1 System Call (BasicFileAttributes)
            System.out.println("\n--- Part 3: Efficient Metadata Query (BasicFileAttributes) ---");
            // java.io.File called stat() 4 separate times for length(), isDirectory(), lastModified(), etc.
            // NIO.2 reads the entire stat buffer in ONE single kernel syscall!
            BasicFileAttributes attrs = Files.readAttributes(targetFile, BasicFileAttributes.class);
            System.out.println("   ├── File Size:      " + attrs.size() + " bytes");
            System.out.println("   ├── Creation Time:  " + attrs.creationTime());
            System.out.println("   ├── Last Modified:  " + attrs.lastModifiedTime());
            System.out.println("   ├── Is Regular:     " + attrs.isRegularFile());
            System.out.println("   └── Is Symlink:     " + attrs.isSymbolicLink());

            // Part 4: Exception-Driven Error Reporting vs Silent Boolean false
            System.out.println("\n--- Part 4: Exception-Driven Error Handling ---");
            Path nonExistent = baseDir.resolve("non_existent_file.txt");
            try {
                Files.delete(nonExistent);
            } catch (NoSuchFileException e) {
                System.out.println("   └── Caught explicit " + e.getClass().getSimpleName()
                        + ": File does not exist! (java.io.File.delete() would silently return false)");
            }

            // Part 5: Memory-Safe Directory Traversal via DirectoryStream
            System.out.println("\n--- Part 5: Memory-Safe Directory Traversal (DirectoryStream) ---");
            // Create 5 dummy files
            for (int i = 1; i <= 5; i++) {
                Files.createFile(baseDir.resolve("log_" + i + ".log"));
            }

            System.out.println("   Iterating over directory without loading entire list into heap memory:");
            // DirectoryStream uses an OS cursor (opendir/readdir), streaming entries lazily
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(baseDir, "*.log")) {
                for (Path entry : stream) {
                    System.out.println("      ├── Found log entry: " + entry.getFileName());
                }
            }

        } finally {
            // Recursive deletion cleanup
            try (var walk = Files.walk(baseDir)) {
                walk.sorted((a, b) -> b.compareTo(a)) // reverse order to delete children first
                    .forEach(p -> {
                        try { Files.deleteIfExists(p); } catch (IOException ignored) {}
                    });
            }
        }

        System.out.println("\n==========================================================================");
        System.out.println(">>> SUMMARY <<<");
        System.out.println("NIO.2 Path/Files resolves all java.io.File flaws: rich metadata queries in");
        System.out.println("1 syscall, true atomic moves, informative exceptions, and lazy streaming.");
        System.out.println("==========================================================================");
    }
}
