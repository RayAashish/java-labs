package com.raysi.internals.memory;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;

/**
 * Architectural Demonstration: Object Header Layout, Memory Footprint & 8-Byte Alignment.
 * <p>
 * Key Concepts Demonstrated:
 * 1. Physical structure of an object on 64-bit HotSpot JVM:
 *    - Mark Word: 8 bytes (64 bits).
 *    - Klass Word: 4 bytes (with CompressedClassPointers) or 8 bytes.
 *    - Instance Fields: Packed primitives & references.
 *    - Alignment Padding: Padded to multiples of 8 bytes.
 * 2. Identity HashCode calculation inserting hash bits into the Mark Word.
 * 3. Array memory overhead (Array Length field: 4 bytes).
 */
public class ObjectHeaderAndMemoryLayoutDemo {

    public static void main(String[] args) {
        System.out.println("================================================================================");
        System.out.println("   OBJECT HEADER & MEMORY LAYOUT DEEP DIVE");
        System.out.println("================================================================================");

        MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
        System.out.println("[1] Initial Heap Memory Usage: " + memoryBean.getHeapMemoryUsage());

        // 1. Inspect Basic Empty Object
        Object emptyObject = new Object();
        System.out.println("\n[2] Empty Object Anatomy:");
        System.out.println("   -> Class: " + emptyObject.getClass().getName());
        System.out.println("   -> Standard Header (Mark Word: 8B + Compressed Klass: 4B) = 12 Bytes");
        System.out.println("   -> 8-Byte Alignment Padding = 4 Bytes");
        System.out.println("   -> Total Physical Shallow Size = 16 Bytes");

        // 2. Identity HashCode Insertion into Mark Word
        System.out.println("\n[3] Mark Word Mutation via Identity HashCode:");
        int identityHashBefore = System.identityHashCode(emptyObject);
        System.out.printf("   -> Generated Identity HashCode (31 bits inserted into Mark Word): 0x%08X (%d)%n",
                identityHashBefore, identityHashBefore);

        // 3. Array Header Anatomy
        int[] intArray = new int[10];
        System.out.println("\n[4] Primitive Array Header Anatomy (int[10]):");
        System.out.println("   -> Mark Word: 8 Bytes");
        System.out.println("   -> Klass Pointer: 4 Bytes (Compressed)");
        System.out.println("   -> Array Length: 4 Bytes (Required for bounds checks)");
        System.out.println("   -> Array Elements: 10 * 4 Bytes = 40 Bytes");
        System.out.println("   -> Header (16B) + Elements (40B) = 56 Bytes (Exact multiple of 8, 0B padding)");

        // 4. Custom Entity Memory Calculation
        System.out.println("\n[5] Custom Domain Entity Memory Layout:");
        CustomerRecord customer = new CustomerRecord(101L, "Alice", 29, true);
        System.out.println("   -> CustomerRecord Instance: " + customer);
        System.out.println("   -> Mark Word: 8 Bytes");
        System.out.println("   -> Klass Pointer: 4 Bytes");
        System.out.println("   -> long id: 8 Bytes");
        System.out.println("   -> int age: 4 Bytes");
        System.out.println("   -> boolean active: 1 Byte");
        System.out.println("   -> Reference name: 4 Bytes (Compressed OOP)");
        System.out.println("   -> Subtotal = 8 + 4 + 8 + 4 + 1 + 4 = 29 Bytes");
        System.out.println("   -> Alignment Padding (to reach multiple of 8) = 3 Bytes");
        System.out.println("   -> Total Physical Shallow Size on Heap = 32 Bytes");

        System.out.println("\n================================================================================");
        System.out.println("   Memory layout calculation complete.");
        System.out.println("================================================================================");
    }

    public static class CustomerRecord {
        private final long id;          // 8 bytes
        private final String name;      // 4 bytes (reference pointer)
        private final int age;          // 4 bytes
        private final boolean active;   // 1 byte

        public CustomerRecord(long id, String name, int age, boolean active) {
            this.id = id;
            this.name = name;
            this.age = age;
            this.active = active;
        }

        @Override
        public String toString() {
            return String.format("CustomerRecord[id=%d, name='%s', age=%d, active=%b]", id, name, age, active);
        }
    }
}
