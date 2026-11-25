package functional.streams;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * ALL WAYS TO CREATE STREAMS IN JAVA (Plus Common Traps)
 *
 * Covers: Collection.stream(), Arrays.stream(), Stream.of(), Stream.ofNullable(),
 * Stream.iterate() (infinite & bounded), Stream.generate(), IntStream.range/rangeClosed,
 * Stream.builder(), Pattern.splitAsStream(), Stream.concat(), and single-use rule.
 */
public class StreamCreationAllWaysDemo {

    public static void main(String[] args) {
        System.out.println("1. From Collection:   " + List.of("A", "B", "C").stream().toList());

        int[] nums = {10, 20, 30};
        System.out.println("2. Arrays.stream:     " + Arrays.stream(nums).sum());

        // ❌ TRAP: Stream.of(nums) creates Stream<int[]> of size 1 instead of IntStream!
        System.out.println("   TRAP Stream.of(int[]): count=" + Stream.of(nums).count());

        System.out.println("3. Stream.ofNullable: " + Stream.ofNullable(null).count()
                + " vs non-null=" + Stream.ofNullable("Hello").count());

        System.out.println("4. IntStream.range(1,5):       " + IntStream.range(1, 5).boxed().toList());
        System.out.println("5. IntStream.rangeClosed(1,5): " + IntStream.rangeClosed(1, 5).boxed().toList());

        // Java 9 bounded iterate(seed, hasNext, next) — mirrors a for-loop cleanly!
        System.out.println("6. Bounded iterate:   " + Stream.iterate(1, n -> n <= 32, n -> n * 2).toList());

        // Infinite generator MUST be bounded with .limit(n)!
        System.out.println("7. generate + limit:  " + Stream.generate(() -> "ping").limit(3).toList());

        Stream<String> built = Stream.<String>builder().add("Init").add("Run").build();
        System.out.println("8. Stream.builder:    " + built.toList());

        List<String> csvTokens = Pattern.compile(",").splitAsStream("java,jvm,loom").toList();
        System.out.println("9. splitAsStream:     " + csvTokens);

        List<String> merged = Stream.concat(Stream.of("X", "Y"), Stream.of("Z")).toList();
        System.out.println("10. Stream.concat:    " + merged);

        System.out.println("\n=== Single-Use Stream Rule ===");
        Stream<String> singlePass = Stream.of("One", "Two");
        System.out.println("First terminal op count: " + singlePass.count());
        try {
            singlePass.toList(); // Throws IllegalStateException!
        } catch (IllegalStateException ex) {
            System.out.println("Reusing consumed stream throws: " + ex.getMessage());
        }
    }
}
