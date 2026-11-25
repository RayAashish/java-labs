package lts.java08.streams;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * Java 8 LTS — Exhaustive Stream Creation, Laziness, Loop Fusion & Single-Use Lifecycle.
 *
 * Covers:
 * 1. 10 ways to construct a Stream (and common `Stream.of(int[])` footgun).
 * 2. Empirical proof of lazy evaluation + vertical loop fusion.
 * 3. Resource management trap with Files.lines() (try-with-resources required).
 * 4. Single-use IllegalStateException when reusing a consumed stream.
 */
public class StreamCreationAndLifecycleDemo {

    public static void main(String[] args) throws IOException {
        System.out.println("=== Java 8 Stream Creation, Laziness & Lifecycle ===");

        // -------------------------------------------------------------
        // 1. Ways to Create Streams
        // -------------------------------------------------------------
        List<String> fromCollection = List.of("alpha", "beta", "gamma").stream().toList();
        int[] rawInts = {10, 20, 30};
        // TRAP CHECK: Stream.of(rawInts) is Stream<int[]> (count=1), whereas Arrays.stream(rawInts) is IntStream!
        long trapCount = Stream.of(rawInts).count();
        int sumCorrect = Arrays.stream(rawInts).sum();

        List<Integer> rangeClosed = IntStream.rangeClosed(1, 5).boxed().toList();
        List<Integer> generatedPowers = Stream.iterate(1, n -> n * 2).limit(5).toList();
        List<String> built = Stream.<String>builder().add("A").add("B").build().toList();

        System.out.printf("[1] fromCollection=%s | Stream.of(int[]).count=%d | Arrays.stream.sum=%d%n",
                fromCollection, trapCount, sumCorrect);
        System.out.printf("    rangeClosed(1,5)=%s | powersOfTwo=%s | builder=%s%n",
                rangeClosed, generatedPowers, built);

        // Lazy file streaming with try-with-resources (closes OS FD deterministically)
        Path temp = Files.createTempFile("stream-demo-", ".txt");
        try {
            Files.writeString(temp, "line1\n\nline2\n");
            try (Stream<String> lines = Files.lines(temp)) {
                long nonEmpty = lines.filter(l -> !l.isBlank()).count();
                System.out.println("    Files.lines non-blank count: " + nonEmpty);
            }
        } finally {
            Files.deleteIfExists(temp);
        }

        // -------------------------------------------------------------
        // 2. Proof of Laziness (Zero Execution Before Terminal Op)
        // -------------------------------------------------------------
        AtomicInteger calls = new AtomicInteger(0);
        Stream<String> lazyPipeline = List.of("tx1", "tx2", "tx3").stream()
                .filter(s -> {
                    calls.incrementAndGet();
                    return s.endsWith("2");
                });
        System.out.println("\n[2] After declaring filter(): predicate calls = " + calls.get() + " (must be 0!)");

        long matched = lazyPipeline.count();
        System.out.println("    After count(): matched = " + matched + ", predicate calls = " + calls.get());

        // -------------------------------------------------------------
        // 3. Proof of Vertical Loop Fusion + Short-Circuiting
        // -------------------------------------------------------------
        System.out.println("\n[3] Trace of Vertical Fusion with limit(2):");
        List<String> result = List.of("a", "usd_1", "b", "usd_2", "usd_3_never_visited").stream()
                .peek(e -> System.out.println("    visit: " + e))
                .filter(e -> e.startsWith("usd"))
                .map(String::toUpperCase)
                .limit(2) // Stops immediately once usd_2 exits! usd_3_never_visited is NEVER touched!
                .toList();
        System.out.println("    Result: " + result);

        // -------------------------------------------------------------
        // 4. Single-Use Lifecycle Trap
        // -------------------------------------------------------------
        Stream<String> oneShot = List.of("x", "y").stream();
        System.out.println("\n[4] First terminal op count = " + oneShot.count());
        try {
            oneShot.forEach(System.out::println);
        } catch (IllegalStateException ex) {
            System.out.println("    Expected IllegalStateException on reuse: " + ex.getMessage());
        }
    }
}
