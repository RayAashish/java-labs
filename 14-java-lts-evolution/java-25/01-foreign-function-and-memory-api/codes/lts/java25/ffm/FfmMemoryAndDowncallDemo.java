package lts.java25.ffm;

import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemoryLayout.PathElement;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.StructLayout;
import java.lang.foreign.SymbolLookup;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.VarHandle;

/**
 * Java 25 LTS — Foreign Function & Memory (FFM) API (JEP 454, finalized JDK 22+).
 *
 * Covers:
 * 1. Deterministic off-heap memory allocation & auto-free with `Arena.ofConfined()`.
 * 2. Use-after-free & out-of-bounds safety (throws IllegalStateException instead of JVM SIGSEGV!).
 * 3. Declarative C StructLayout + type-safe VarHandle field access.
 * 4. Pure Java native C library invocation (invoking C `strlen` without writing C/JNI code).
 */
public class FfmMemoryAndDowncallDemo {

    public static void main(String[] args) throws Throwable {
        System.out.println("=== Java 25 FFM API (Arena, MemorySegment, StructLayout & C Linker) ===");

        // -------------------------------------------------------------
        // 1. Safe Structured Off-Heap Memory (`StructLayout` + `Arena.ofConfined()`)
        // -------------------------------------------------------------
        StructLayout orderStruct = MemoryLayout.structLayout(
                ValueLayout.JAVA_LONG.withName("orderId"),
                ValueLayout.JAVA_DOUBLE.withName("amount")
        );
        VarHandle orderIdHandle = orderStruct.varHandle(PathElement.groupElement("orderId"));
        VarHandle amountHandle = orderStruct.varHandle(PathElement.groupElement("amount"));

        MemorySegment escapedRef;
        try (Arena arena = Arena.ofConfined()) {
            // Allocate 16 bytes directly in native OS memory (0 Java GC overhead!)
            MemorySegment offHeapStruct = arena.allocate(orderStruct);
            escapedRef = offHeapStruct;

            orderIdHandle.set(offHeapStruct, 0L, 900_001L);
            amountHandle.set(offHeapStruct, 0L, 249.99);

            System.out.printf("[1] Off-heap C struct (%d bytes) -> orderId=%d, amount=%.2f%n",
                    offHeapStruct.byteSize(),
                    (long) orderIdHandle.get(offHeapStruct, 0L),
                    (double) amountHandle.get(offHeapStruct, 0L));

            // Allocate a primitive off-heap int[] array cleanly
            MemorySegment intArray = arena.allocateFrom(ValueLayout.JAVA_INT, 10, 20, 30, 40);
            System.out.println("    Off-heap array[2] = " + intArray.getAtIndex(ValueLayout.JAVA_INT, 2));

            // ---------------------------------------------------------
            // 2. Pure Java Downcall into Native C Standard Library (`strlen`)
            // ---------------------------------------------------------
            try {
                Linker linker = Linker.nativeLinker();
                SymbolLookup stdlib = linker.defaultLookup();
                var strlenSymbol = stdlib.find("strlen");
                if (strlenSymbol.isPresent()) {
                    MethodHandle strlen = linker.downcallHandle(
                            strlenSymbol.get(),
                            FunctionDescriptor.of(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS)
                    );
                    MemorySegment cStr = arena.allocateFrom("Java-25-FFM-Panama");
                    long nativeLen = (long) strlen.invokeExact(cStr);
                    System.out.println("[2] Native C `strlen('Java-25-FFM-Panama')` = " + nativeLen);
                }
            } catch (IllegalCallerException restrictedNotice) {
                System.out.println("[2] Native linker restricted unless --enable-native-access passed: "
                        + restrictedNotice.getMessage());
            }
        } // `arena.close()` runs deterministically here — native OS memory freed immediately!

        // 3. Proof of temporal safety: Use-after-free raises clean IllegalStateException, NEVER C segfault!
        try {
            escapedRef.get(ValueLayout.JAVA_LONG, 0);
        } catch (IllegalStateException safeGuard) {
            System.out.println("[3] Use-after-free caught safely without crash: " + safeGuard.getMessage());
        }
    }
}
