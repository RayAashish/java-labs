package ionio.phase0_beginner;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

/**
 * Beginner-friendly guide to modern NIO.2 file I/O using Path and Files.
 */
public class BeginnerFileReadWriteDemo {

    public static void main(String[] args) throws IOException {
        Path tempFile = Files.createTempFile("java-labs-io-", ".txt");
        try {
            // 1. Write UTF-8 text cleanly in one line
            Files.writeString(tempFile, "Line 1: Core Java\n", StandardCharsets.UTF_8);

            // 2. Append another line
            Files.writeString(tempFile, "Line 2: Modern NIO.2\n",
                    StandardCharsets.UTF_8, StandardOpenOption.APPEND);

            // 3. Read small file cleanly
            System.out.println("=== File Content ===");
            System.out.print(Files.readString(tempFile, StandardCharsets.UTF_8));
            System.out.println("Size in bytes: " + Files.size(tempFile));
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }
}
