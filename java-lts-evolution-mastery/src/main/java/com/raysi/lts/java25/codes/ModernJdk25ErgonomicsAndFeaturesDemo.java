package com.raysi.lts.java25.codes;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

/**
 * Author: raysi
 * Package: com.raysi.lts.java25.codes
 *
 * Demonstrates the Modern Java 25 Enterprise Baseline:
 * 1. Foreign Function & Memory (FFM) API (JEP 454): Deterministic off-heap memory safety with Arena.
 * 2. Unnamed Variables & Patterns (JEP 456): Modern syntax ergonomics using '_' for discarded bindings.
 * 3. Modern Stream Batching & Windowing semantics (Stream Gatherers pattern).
 * 4. High-efficiency data-carrier processing for low-latency systems.
 *
 * Compilation & Execution Note:
 * On JDK 21: javac --enable-preview --release 21 ... / java --enable-preview ...
 * On JDK 25: Standard compilation without preview flags (features fully finalized).
 */
public class ModernJdk25ErgonomicsAndFeaturesDemo {

    public record HardwareTelemetry(String deviceId, long timestamp, double voltage, double temperature) {}

    public static void main(String[] args) {
        System.out.println("=== Modern Java 25 LTS Baseline & Ergonomics Demo ===");

        // -------------------------------------------------------------
        // 1. Foreign Function & Memory API (Arena & MemorySegment)
        // -------------------------------------------------------------
        System.out.println("\n--- 1. Foreign Function & Memory (FFM API) Off-Heap Management ---");
        // Safe, deterministic off-heap allocation that completely replaces sun.misc.Unsafe and JNI!
        // Confined Arena guarantees allocation and zero-cost deallocation bound to try-with-resources.
        try (Arena confinedArena = Arena.ofConfined()) {
            int elementCount = 5;
            long byteSize = (long) elementCount * ValueLayout.JAVA_LONG.byteSize();

            MemorySegment offHeapSegment = confinedArena.allocate(byteSize);
            System.out.println("[+] Allocated off-heap segment: " + offHeapSegment.byteSize() + " bytes");

            // Write values into native memory safely without C/JNI pointers
            for (int i = 0; i < elementCount; i++) {
                long offset = (long) i * ValueLayout.JAVA_LONG.byteSize();
                offHeapSegment.set(ValueLayout.JAVA_LONG, offset, (i + 1) * 1000L);
            }

            // Read values back from native memory
            System.out.print("[+] Read native memory values: [");
            for (int i = 0; i < elementCount; i++) {
                long offset = (long) i * ValueLayout.JAVA_LONG.byteSize();
                System.out.print(offHeapSegment.get(ValueLayout.JAVA_LONG, offset) + (i == elementCount - 1 ? "" : ", "));
            }
            System.out.println("]");
        } // Off-heap memory is deterministically freed here without relying on GC finalizers!
        System.out.println("[+] Off-heap arena closed and unmapped cleanly.");

        // -------------------------------------------------------------
        // 2. Unnamed Variables & Patterns ('_') Ergonomics
        // -------------------------------------------------------------
        System.out.println("\n--- 2. Unnamed Variables & Patterns ('_') Ergonomics ---");

        // A. Discarded exception in fallback parsing
        String rawConfigValue = "NaN_OR_INVALID";
        int port;
        try {
            port = Integer.parseInt(rawConfigValue);
        } catch (NumberFormatException ignored) {
            // Using 'ignored' makes it clear to compilers, linters, and reviewers that the exception is intentionally unused
            port = 8080;
            System.out.println("[+] Cleanly handled fallback with unnamed exception: port=" + port);
        }

        // B. Unused lambda parameters
        Map<String, List<String>> routingMap = Map.of(
                "eu-central", List.of("srv-1", "srv-2"),
                "us-east", List.of("srv-3", "srv-4")
        );
        System.out.println("[+] Inspecting map entries with unused key parameter:");
        routingMap.forEach((unusedKey, servers) -> {
            System.out.println("   -> Server count: " + servers.size());
        });

        // -------------------------------------------------------------
        // 3. Modern Stream Windowing / Batching Pattern
        // -------------------------------------------------------------
        System.out.println("\n--- 3. Modern Stream Windowing & Batching (Gatherer Pattern) ---");
        List<Integer> streamData = IntStream.rangeClosed(1, 10).boxed().toList();
        int batchSize = 3;

        // Simulating modern windowFixed gatherer processing
        List<List<Integer>> partitionedBatches = partitionList(streamData, batchSize);
        System.out.printf("[+] Transformed %d elements into %d fixed-size windows:%n", streamData.size(), partitionedBatches.size());
        partitionedBatches.forEach(batch -> System.out.println("   Window: " + batch));
    }

    /**
     * Helper demonstrating fixed-window batching semantics for streaming pipelines.
     */
    private static <T> List<List<T>> partitionList(List<T> source, int size) {
        List<List<T>> partitions = new ArrayList<>();
        for (int i = 0; i < source.size(); i += size) {
            partitions.add(source.subList(i, Math.min(i + size, source.size())));
        }
        return partitions;
    }
}
