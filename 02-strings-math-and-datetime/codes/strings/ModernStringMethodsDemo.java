package strings;

import java.util.List;

/**
 * MODERN STRING API (Java 11 → 21): strip, isBlank, lines, repeat, indent, transform, formatted
 */
public class ModernStringMethodsDemo {

    public static void main(String[] args) {
        System.out.println("=== 1. Unicode-Aware strip() & isBlank() (Java 11) ===");
        String unicodeSpace = "\u2005  Java 25 \u2005"; // Unicode EN space (\u2005 > '\u0020')
        System.out.println("Legacy trim() length:  " + unicodeSpace.trim().length() + " (misses Unicode whitespace!)");
        System.out.println("Modern strip() length: " + unicodeSpace.strip().length() + " -> '" + unicodeSpace.strip() + "'");
        System.out.println("'   \\t '.isBlank():    " + "   \t ".isBlank());

        System.out.println("\n=== 2. lines(), repeat(), indent(), transform() ===");
        String multiLine = "line1\nline2\nline3";
        List<String> lines = multiLine.lines().toList();
        System.out.println("lines(): " + lines);
        System.out.println("repeat(3): " + "Go!".repeat(3));

        String normalized = "  hello world  "
                .transform(String::strip)
                .transform(String::toUpperCase);
        System.out.println("transform() pipeline: " + normalized);
        System.out.print("indented:\n" + "item".indent(4));
    }
}
