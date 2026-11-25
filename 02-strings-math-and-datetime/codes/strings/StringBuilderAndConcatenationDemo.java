package strings;

import java.util.List;
import java.util.StringJoiner;

/**
 * STRING CONCATENATION, StringBuilder, StringBuffer & StringJoiner
 *
 * WHAT & WHY:
 *   Strings are immutable. Building strings with `+` inside a loop copies all previous
 *   characters on EVERY iteration -> O(N^2) allocations!
 *
 * IN THIS FILE:
 *   1. BAD CODE:  `String +=` inside a loop (O(N^2) time & garbage).
 *   2. GOOD CODE: `StringBuilder` (O(N) amortized), `String.join()`, `StringJoiner`,
 *                 and explaining when `StringBuffer` (synchronized) differs.
 */
public class StringBuilderAndConcatenationDemo {

    public static void main(String[] args) {
        int n = 20_000;

        System.out.println("=== 1. BAD Loop String += vs GOOD StringBuilder ===");
        long t1 = System.nanoTime();
        String bad = "";
        for (int i = 0; i < n; i++) {
            bad += "a"; // Allocates 20,000 progressively larger char/byte arrays!
        }
        long badMs = (System.nanoTime() - t1) / 1_000_000;

        long t2 = System.nanoTime();
        StringBuilder sb = new StringBuilder(n); // Pre-sizing capacity eliminates internal resizes!
        for (int i = 0; i < n; i++) {
            sb.append("a");
        }
        String good = sb.toString();
        long goodMs = (System.nanoTime() - t2) / 1_000_000;

        System.out.println("BAD  String += loop (" + bad.length() + " chars):  " + badMs + " ms");
        System.out.println("GOOD StringBuilder  (" + good.length() + " chars):  " + goodMs + " ms");

        System.out.println("\n=== 2. StringBuffer (Thread-Safe Legacy) & StringJoiner ===");
        StringBuffer threadSafeBuffer = new StringBuffer("Sync");
        threadSafeBuffer.append("-Buffer");
        System.out.println(threadSafeBuffer);

        String csv = String.join(" | ", List.of("Java", "Spring", "Kafka"));
        System.out.println("String.join():  " + csv);

        StringJoiner joiner = new StringJoiner(", ", "[", "]");
        joiner.add("Alpha").add("Beta").add("Gamma");
        System.out.println("StringJoiner:   " + joiner);
    }
}
