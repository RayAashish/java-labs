package lts.java11.stringsfiles;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/**
 * Java 11 LTS — Modern String APIs, Files.readString/writeString & Immutable Collection Factories.
 */
public class StringFilesAndCollectionsDemo {

    public static void main(String[] args) throws IOException {
        System.out.println("=== Java 11 String, Files & Immutable Collection Factories ===");

        // -------------------------------------------------------------
        // 1. String strip() vs trim(), isBlank(), lines(), repeat(), Predicate.not()
        // -------------------------------------------------------------
        String unicodeText = "\u2005\u2005Modern Java\u2005\u2005"; // U+2005 Unicode space
        System.out.printf("[1] rawLen=%d | trim()Len=%d (MISSES U+2005!) | strip()Len=%d%n",
                unicodeText.length(), unicodeText.trim().length(), unicodeText.strip().length());

        String configDump = "  # Header  \n\n  host=localhost  \n  port=8080  \n";
        List<String> activeSettings = configDump.lines()
                .filter(Predicate.not(String::isBlank)) // Java 11 Predicate.not
                .map(String::strip)
                .toList();
        System.out.println("    lines() + Predicate.not(isBlank) -> " + activeSettings);
        System.out.println("    " + "-".repeat(35));

        // -------------------------------------------------------------
        // 2. Files.writeString / Files.readString (1-line UTF-8 File I/O)
        // -------------------------------------------------------------
        Path tmp = Files.createTempFile("j11-config-", ".properties");
        try {
            Files.writeString(tmp, "app.env=prod\napp.region=ap-south-1\n");
            String content = Files.readString(tmp);
            System.out.println("[2] Files.readString:\n" + content.indent(4).stripTrailing());
        } finally {
            Files.deleteIfExists(tmp);
        }

        // -------------------------------------------------------------
        // 3. List.of / Map.of vs Arrays.asList Traps + toArray(IntFunction)
        // -------------------------------------------------------------
        List<String> backedByArray = Arrays.asList("A", "B");
        backedByArray.set(0, "MUTATED"); // Succeeds on Arrays.asList!

        List<String> strictlyImmutable = List.of("Spring", "Micronaut", "Quarkus");
        try {
            strictlyImmutable.set(0, "Fail");
        } catch (UnsupportedOperationException expected) {
            System.out.println("\n[3] List.of strictly rejects set()! Arrays.asList mutated -> " + backedByArray);
        }

        // Map.ofEntries scales past 10 pairs and rejects duplicates & nulls
        Map<String, Integer> statusCodes = Map.ofEntries(
                Map.entry("OK", 200),
                Map.entry("CREATED", 201),
                Map.entry("NOT_FOUND", 404)
        );
        String[] frameworkArr = strictlyImmutable.toArray(String[]::new); // Java 11 Collection.toArray
        System.out.println("    statusCodes=" + statusCodes + " | arrayLen=" + frameworkArr.length);
    }
}
