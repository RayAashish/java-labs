# 10. JVM Internals, Memory, GC & Performance — Interview Questions & Answers

---

## 🟢 Beginner Questions

### Q1. Explain the difference between JDK, JRE, and JVM.
- **JVM:** Abstract engine (implemented by HotSpot) that loads `.class` bytecode, manages Heap/GC, and executes code via Interpreter + JIT.
- **JRE:** JVM + Standard Java Class Libraries (`java.base`, `java.sql`, etc.).
- **JDK:** JRE + developer tools (`javac`, `javap`, `jcmd`, `jstack`, `jmap`, `jfr`).

### Q2. What is the difference between Stack and Heap memory?
- **Thread Stack:** Per-thread private memory storing method stack frames, local primitives, and object references. LIFO, auto-cleaned on return (`StackOverflowError` on unbounded recursion).
- **Heap:** Shared across all threads, holds every `new` object instance and array, managed by Garbage Collection (`OutOfMemoryError: Java heap space`).

### Q3. Where do `static` variables and class metadata live?
Since **Java 8**, class metadata, bytecode, `vtable`/`itable` pointers, and constant pools moved from PermGen to **Metaspace** (allocated in **native OS memory**, not Java Heap!). `static` variable references and the String Pool reside on the Heap (reachable from `Class<?>`).

---

## 🟡 Intermediate Questions

### Q4. Explain the 3 Phases of Class Loading (`Loading -> Linking -> Initialization`).
1. **Loading:** ClassLoader locates `.class` bytes and creates `java.lang.Class` instance.
2. **Linking:**
   - **Verify:** Bytecode verifier guarantees stack/type safety (`StackMapTable`).
   - **Prepare:** Allocates static fields with **default zero values** (`0`, `null`, `false`).
   - **Resolve:** Replaces symbolic references in Constant Pool with direct pointers.
3. **Initialization:** Executes `<clinit>` (assigns actual `static` values and runs `static {}` blocks).

### Q5. Explain the Parent Delegation Model of ClassLoaders, and when is it broken?
`Application -> Platform -> Bootstrap`: A ClassLoader delegates `loadClass()` **UP** to its parent first, loading locally only if all parents return `ClassNotFoundException`. This prevents user code from spoofing `java.lang.String`!
**When broken:** Servlet containers (**Tomcat** loads webapp `WEB-INF/lib` first for isolation), OSGi plugins, and **`DriverManager` (`ServiceLoader` via Thread Context ClassLoader)** where Bootstrap-loaded `java.sql` must instantiate App-classpath MySQL drivers!

### Q6. `ClassNotFoundException` vs `NoClassDefFoundError`?
- **`ClassNotFoundException` (Checked Exception):** Dynamic class loading (`Class.forName(...)`) could not locate the `.class` at runtime.
- **`NoClassDefFoundError` (Error):** The class **existed at compile time**, so callers compiled cleanly, **but is missing at runtime** (or previously failed during `<clinit>` static initializer!).

### Q7. How much memory does an empty `new Object()` occupy on a 64-bit JVM?
**16 bytes** (with Compressed Oops enabled): `8B` Mark Word + `4B` Compressed Klass Pointer + `4B` alignment padding (multiple of 8 bytes). *(With Java 24/25 `-XX:+UseCompactObjectHeaders`, Mark + Klass merge into 8 bytes!)*

### Q8. Explain the Weak Generational Hypothesis and Heap Regions (`Eden`, `S0`, `S1`, `Old`).
Most objects die shortly after allocation (iterators, DTOs, request strings). New objects allocate fast in **`Eden`** (via lock-free **TLABs**); survivors copy back-and-forth between **`S0` / `S1` Survivor spaces**, incrementing GC age until reaching `MaxTenuringThreshold` (max 15, 4-bit counter in Mark Word) -> promoted to **Old / Tenured Gen**.

---

## 🔴 Senior / Principal Architect Questions

### Q9. How does HotSpot JIT Tiered Compilation work (`Interpreter -> C1 -> C2`)?
1. **Tier 0 (Interpreter):** Instant startup, profiles method invocation & loop back-edge counters.
2. **Tier 1–3 (C1 Client Compiler):** Fast native compilation with light optimizations + profiling instrumentation.
3. **Tier 4 (C2 Server Compiler):** Aggressive optimizations on hot paths:
   - **Method Inlining** (eliminates call overhead & unlocks downstream opts)
   - **Escape Analysis** -> **Scalar Replacement** (breaks non-escaping objects into CPU registers -> **0 Heap allocation!**) and **Lock Elision** (removes `synchronized` on thread-local objects)
   - **Deoptimization (`uncommon trap`):** If a speculative assumption (monomorphic call site) is violated later by loading a subclass, JVM seamlessly falls back to Interpreter!

### Q10. Compare G1 GC vs Generational ZGC vs Parallel GC.
- **Parallel GC:** Maximum raw throughput, multi-threaded STW pauses (good for offline batch jobs).
- **G1 GC (Default):** Splits Heap into equal-sized regions (1–32 MB), tracks cross-region references via **Card Tables + Write Barriers + Remembered Sets (RSet)**, and collects garbage-richest regions first to meet pause targets (`-XX:MaxGCPauseMillis=200`).
- **Generational ZGC (`-XX:+UseZGC -XX:+ZGenerational`):** Sub-millisecond pauses (< 1 ms) regardless of Heap size (GBs to 16 TB) using **Colored Pointers** (metadata bits inside 64-bit pointers) and **Load Barriers** for concurrent compaction while app threads run!

### Q11. What is a **TLAB** (Thread-Local Allocation Buffer)?
Instead of contending on a global Heap lock for every `new Object()`, each thread carves out a private slice of Eden called a **TLAB**. Allocation becomes a single **pointer bump (`top += size`)** (~2–5 CPU cycles, faster than C `malloc`!).

### Q12. What is False Sharing, and how does `@Contended` fix it?
CPU cores cache RAM in **64-byte Cache Lines** via the MESI protocol. If Thread A writes `var1` and Thread B writes `var2` and both sit inside the same 64-byte cache line, every write invalidates the other core's L1 cache (**cache-line ping-pong**), destroying multi-core scaling! Padding variables apart (like `@jdk.internal.vm.annotation.Contended` inside `LongAdder.Cell` and `ForkJoinPool`) eliminates false sharing.

### Q13. List the 5 common `OutOfMemoryError` flavors and how to diagnose each!
1. **`Java heap space`:** Memory leak (unbounded cache / `ThreadLocal` / static collection) or `-Xmx` too small -> `jcmd <pid> GC.heap_dump` + Eclipse MAT Dominator Tree.
2. **`GC overhead limit exceeded`:** >98% CPU spent in GC recovering <2% Heap.
3. **`Metaspace`:** Dynamic classloader/CGLIB proxy generation leak -> inspect `-XX:MaxMetaspaceSize` and classloader leak.
4. **`Direct buffer memory`:** Off-heap `ByteBuffer.allocateDirect` leak -> check `-XX:MaxDirectMemorySize`.
5. **`unable to create native thread`:** OS `ulimit` / RAM exhausted by too many Platform Threads -> switch to bounded pools or Virtual Threads!

### Q14. What is a JVM **Safepoint** and Time-To-Safepoint (TTSP)?
A point where all application threads pause with consistent object-graph states (needed for GC roots, thread dumps, deoptimization). Threads poll a safepoint page at method exits and counted-loop back-edges. Long uncounted loops (`for (long i = 0; ...)` in Java 8) delay TTSP, stalling **all** threads waiting for one straggler!

### Q15. How do you profile a slow production JVM service with near-zero overhead?
Use **Java Flight Recorder (JFR)** (`jcmd <pid> JFR.start duration=60s settings=profile filename=app.jfr`) or **`async-profiler`** (uses hardware `perf_events` + `AsyncGetCallTrace` to avoid Safepoint Bias when generating CPU & Allocation Flamegraphs!).
