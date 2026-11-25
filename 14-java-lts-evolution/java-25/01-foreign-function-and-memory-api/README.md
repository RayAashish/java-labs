# Java 25 — Foreign Function & Memory (FFM) API (JEP 454)

## ⚡ 2-Minute Revision
- **What It Replaces:** Fragile, segfault-prone `sun.misc.Unsafe` memory pointers AND slow/painful **JNI** (writing C glue headers + `.so`/`.dylib` builds)!
- **4 Core Abstractions (`java.lang.foreign.*`):**
  1. **`Arena`:** Controls memory lifetime deterministically (`try (Arena a = Arena.ofConfined()) { ... }` frees OS memory immediately upon `}`!). Modes: `ofConfined()` (single-thread fast), `ofShared()` (multi-thread safe), `ofAuto()` (GC-managed), `global()` (never freed).
  2. **`MemorySegment`:** Bounds-checked, lifetime-checked slice of native off-heap OR Java heap memory. Accessing after `arena.close()` throws **`IllegalStateException`** (NOT a C JVM segfault!).
  3. **`MemoryLayout` / `StructLayout`:** Declaratively describe C structs (`MemoryLayout.structLayout(ValueLayout.JAVA_INT.withName("x"), ...)`), automatically respecting padding & endianness!
  4. **`Linker` & `SymbolLookup`:** Call native C libraries (e.g., `libc` `strlen`, DuckDB, OpenSSL, CUDA) safely in pure Java via compiled `MethodHandle` downcalls!

## 📂 Contents
- [`notes/ffm-api-deep-dive.md`](notes/ffm-api-deep-dive.md)
- [`codes/lts/java25/ffm/FfmMemoryAndDowncallDemo.java`](codes/lts/java25/ffm/FfmMemoryAndDowncallDemo.java)
- [`interview/INTERVIEW_QUESTIONS.md`](interview/INTERVIEW_QUESTIONS.md)
