package lts.java17.text;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Java 17 LTS (covering Java 12-17) — Text Blocks (JEP 378), formatted(), Collectors.teeing() & mapMulti().
 */
public class TextBlocksAndStreamAdditionsDemo {

    record MinMaxStats(int min, int max, int spread) {}

    public static void main(String[] args) {
        System.out.println("=== Java 17 Text Blocks, teeing(), toList() & mapMulti() ===");

        // -------------------------------------------------------------
        // 1. Text Blocks + .formatted() + `\` (join line) & `\s` (keep trailing space)
        // -------------------------------------------------------------
        String jsonPayload = """
                {
                  "service": "%s",
                  "ltsVersion": %d,
                  "status": "HEALTHY"
                }""".formatted("order-api", 17);
        System.out.println("[1] Formatted Text Block JSON:\n" + jsonPayload);

        String singleLineLongSql = """
                SELECT id, total \
                FROM orders \
                WHERE status = 'PAID'""";
        System.out.println("\n[2] Collapsed via line continuation `\\`: " + singleLineLongSql);

        String fixedWidth = """
                A\s\s
                B\s\s""";
        System.out.println("    Trailing spaces preserved via \\s -> line lengths = " +
                fixedWidth.lines().map(String::length).toList());

        // -------------------------------------------------------------
        // 3. Java 12 Collectors.teeing() — Single-Pass Dual Aggregation
        // -------------------------------------------------------------
        List<Integer> latencies = List.of(12, 45, 9, 120, 33);
        MinMaxStats range = latencies.stream().collect(Collectors.teeing(
                Collectors.minBy(Integer::compareTo),
                Collectors.maxBy(Integer::compareTo),
                (minOpt, maxOpt) -> {
                    int min = minOpt.orElse(0);
                    int max = maxOpt.orElse(0);
                    return new MinMaxStats(min, max, max - min);
                }
        ));
        System.out.println("\n[3] Single-pass Collectors.teeing -> " + range);

        // -------------------------------------------------------------
        // 4. Java 16 Stream.mapMulti() (Zero inner Stream allocation)
        // -------------------------------------------------------------
        List<Object> mixed = List.of(1, "skip", 2, List.of(3, 4));
        List<Integer> flattenedInts = mixed.stream()
                .<Integer>mapMulti((item, downstream) -> {
                    if (item instanceof Integer i) {
                        downstream.accept(i);
                    } else if (item instanceof List<?> list) {
                        for (Object inner : list) {
                            if (inner instanceof Integer nested) {
                                downstream.accept(nested);
                            }
                        }
                    }
                })
                .toList(); // Java 16 unmodifiable list
        System.out.println("[4] Zero-alloc mapMulti extracted ints -> " + flattenedInts);
    }
}
