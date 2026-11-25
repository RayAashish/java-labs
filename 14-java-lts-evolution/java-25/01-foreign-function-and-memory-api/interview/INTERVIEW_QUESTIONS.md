# Interview Questions — Foreign Function & Memory (FFM) API

## 🟢 Beginner

### Q1. What does the FFM API replace in Java, and why is it better?
It replaces both **`sun.misc.Unsafe`** (off-heap raw pointer arithmetic) and **JNI** (Java Native Interface requiring handwritten `.c`/`.h` glue files and platform native builds). FFM offers safe bounds/lifetime-checked off-heap memory AND pure-Java C library calls.

---

## 🟡 Intermediate

### Q2. Compare `Arena.ofConfined()`, `Arena.ofShared()`, and `Arena.ofAuto()`.
- **`ofConfined()`:** Owned strictly by the creating thread; freed immediately & deterministically when `.close()` runs (`try-with-resources`). Fast single-thread speed.
- **`ofShared()`:** Thread-safe shared off-heap access; freed deterministically on `.close()` safely using JVM thread handshakes so no thread can hit use-after-free!
- **`ofAuto()`:** Thread-safe, reclaimed non-deterministically by GC (like `ByteBuffer.allocateDirect`, except supporting 64-bit `long` sizes & zero 2 GB limit).

### Q3. Why is accessing a closed `MemorySegment` safe in Java whereas C crashes with `SIGSEGV`?
Every `MemorySegment` binds to an `Arena`'s `Scope`. Once `arena.close()` marks the scope closed, any read/write on that segment immediately throws **`IllegalStateException: Already closed`** cleanly before touching deallocated native RAM!

---

## 🔴 Senior

### Q4. Why does `Linker.downcallHandle` require `--enable-native-access` in modern JDKs?
Although `MemorySegment` protects pure-Java off-heap reads with bounds & scope checks, calling arbitrary C pointers (`strlen` / native DLLs) steps outside JVM sandbox invariants (an incorrect `FunctionDescriptor` or bad native pointer inside C itself can corrupt memory). JEP 472 gates restricted native linking behind explicit startup trust flags (`--enable-native-access=<module>`).
