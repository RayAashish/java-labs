# ⚡ Java-Labs — The 15-Minute Pre-Interview Rapid Revision Engine

> **Use this right before interviews or deep-work sessions.** Each section packs the highest-signal truths, exact JVM numbers, and classic traps from its corresponding module.

---

## 01. Core Foundations & Memory
- **Strictly Pass-by-Value:** Java copies primitive values and reference **addresses** by value. Reassigning a parameter never updates the caller; mutating fields through that pointer does.
- **`Integer` Cache:** `-128..127` cached by `Integer.valueOf()`. `127 == 127` is `true`; `128 == 128` is `false`. ALWAYS `.equals()`!
- **Promotion:** `byte`/`short`/`char` promote to `int` in arithmetic (`b += 1` adds implicit cast; `b = b + 1` fails).
- **2D Arrays:** `clone()` / `Arrays.copyOf()` is **shallow** on 2D arrays (inner rows shared).
- **Empty Object Size (64-bit Compressed Oops):** `16B` (`8B` Mark + `4B` Klass + `4B` padding; `8B` total with `-XX:+UseCompactObjectHeaders`).

## 02. Strings, Math & Date/Time
- **Compact Strings (Java 9+):** `byte[] value` + `byte coder` (`LATIN1=0`, `UTF16=1`) cuts Heap size ~40–50%.
- **String Pool:** Lives in **Main Heap** (since Java 7, GC-able). `new String("a").intern()` returns pooled ref.
- **`BigDecimal`:** Always `new BigDecimal("0.1")`, NEVER `new BigDecimal(0.1)`. Use `compareTo() == 0` for numeric equality (`equals()` checks scale too!) and pass `RoundingMode.HALF_EVEN` to `divide()`.
- **`java.time`:** Immutable & thread-safe (`SimpleDateFormat` is NOT!). Use `Instant` for UTC DB storage, `ZonedDateTime` for UI display, `Period` for dates, `Duration` for elapsed seconds/nanos.

## 03. OOP & Dispatch
- **Init Order:** Static (`Parent -> Child`, once at class load) -> Instance fields/blocks + Constructor (`Parent`) -> Instance fields/blocks + Constructor (`Child`).
- **Overload Resolution Ladder:** Exact Match -> Primitive Widening -> Autoboxing -> Varargs. (**Widening beats Boxing!**)
- **Static & Fields:** NOT polymorphic! Static methods are **hidden**, fields are **shadowed**, resolved by **compile-time reference type**.
- **Overriding Invariants:** Identical signature, covariant return, wider/equal access, narrower/fewer checked exceptions.
- **Immutability Recipe:** `final` class, `private final` fields, no setters, defensive copies on constructor input & getter output.

## 04. Exceptions
- **Never `return` or `throw` in `finally`:** Silently swallows `try`/`catch` exceptions!
- **Try-With-Resources:** Closes `AutoCloseable` in **reverse order**; keeps `try` exception as primary and attaches `close()` failures via `getSuppressed()`.
- **Performance:** `new Exception()` calls JNI `fillInStackTrace()` ($O(\text{stack depth})$). Use `if-else` for control flow, or override `fillInStackTrace() { return this; }` for stackless control signals.

## 05. Generics, Reflection & Proxies
- **PECS:** **P**roducer **`extends`** (read), **C**onsumer **`super`** (write).
- **Invariance:** `List<Integer>` is NOT a `List<Number>` (unlike covariant arrays `Integer[]` -> `ArrayStoreException`).
- **Erasure:** `<T>` erased to `Object` (or leftmost bound); `javac` inserts **bridge methods** for polymorphic generic overrides.
- **Spring Proxies:** JDK `Proxy` requires interfaces; CGLIB subclasses concrete classes (why `final` classes/methods bypass `@Transactional`).

## 06. Functional & Streams
- **Lambdas != Anonymous Classes:** Emit `invokedynamic` (no `.class` files on disk, `this` is enclosing class). Local captures must be **effectively final**.
- **`Optional`:** Never use as fields or parameters. Prefer `.orElseGet(supplier)` over `.orElse(expensiveCall())`!
- **Streams:** Lazy until terminal op; single-use. Never put blocking I/O inside `parallelStream()` (starves global `ForkJoinPool.commonPool()`). Java 24/25 adds `Stream.gather(Gatherers.windowFixed(...))`.

## 07. Collections & `HashMap`
- **Default Picks:** `ArrayList` (over `LinkedList`), `ArrayDeque` (over legacy `Stack`/`Vector`), `EnumSet`/`EnumMap` (for enum keys).
- **`HashMap` Specs:** Initial cap `16`, load factor `0.75f` (size `N` -> cap `nextPow2(ceil(N / 0.75))`), `TREEIFY_THRESHOLD = 8`, `UNTREEIFY_THRESHOLD = 6`, `MIN_TREEIFY_CAPACITY = 64`. Index = `(n - 1) & (h ^ (h >>> 16))`.
- **`ConcurrentHashMap` (Java 8+):** CAS on empty slots + `synchronized(bucketHead)` on collision + `volatile` reads. No `null` keys/values.

## 08. I/O, NIO & Zero-Copy
- **NIO.2:** Replace `java.io.File` with `Path`/`Files`. Stream huge files via `try (var s = Files.lines(path))`.
- **`ByteBuffer`:** `flip()` (write -> read: `limit=pos; pos=0`), `clear()` (read -> write), `compact()` (keep unread bytes).
- **Zero-Copy:** `FileChannel.transferTo()` invokes OS `sendfile(2)` (Kernel Page Cache -> NIC directly, bypassing JVM Heap).

## 09. Concurrency, JMM & Virtual Threads
- **`volatile`:** Guarantees **Visibility + Ordering**, NOT **Atomicity** (`count++` still races -> use `AtomicInteger` / `LongAdder`).
- **Pool Sizing:** CPU-bound = $N_{\text{cores}} + 1$; Platform I/O-bound = $N_{\text{cores}} \times (1 + W/C)$. Bounded `ArrayBlockingQueue` + `CallerRunsPolicy`!
- **Virtual Threads (Java 21–25):** **NEVER pool** virtual threads! Bound downstream DB/HTTP resources via `Semaphore`. Prefer Java 25 **`ScopedValue`** over `ThreadLocal` to avoid pool memory leaks.

## 10. JVM Internals & GC
- **ClassLoaders:** App -> Platform -> Bootstrap (delegates UP first).
- **JIT (C1 -> C2):** Method Inlining, Escape Analysis -> Scalar Replacement (0 heap alloc!) & Lock Elision.
- **GC:** **G1** (default balanced pause targets) vs **Generational ZGC** (colored pointers + load barriers, <1 ms pauses on multi-TB heaps). **TLAB** gives fast lock-free Eden pointer-bump allocation.

## 11. Networking & Sockets
- Always set `SO_TIMEOUT`. NIO `Selector` uses Linux `epoll(7)` for single-thread multiplexing. WebSockets upgrade via HTTP `101 Switching Protocols` + SHA-1 `Sec-WebSocket-Accept`.

## 12. JDBC
- Always use Type 4 Driver + `PreparedStatement` (prevents SQL Injection + caches plans) + `try-with-resources` + bounded batch chunks (`executeBatch`) + `DataSource` connection pool (HikariCP).

## 13. Design Patterns & SOLID
- **DCL Singleton:** `private static volatile` prevents reading half-constructed objects; **`enum INSTANCE`** is reflection/serialization-proof.
- **JDK:** Builder (`HttpRequest.newBuilder`), Adapter (`InputStreamReader`), Decorator (`BufferedInputStream`), Strategy (`Comparator`), Chain (`FilterChain`).

## 14. Java 8 → 25 LTS
- `8` (Lambdas/Streams/`java.time`) -> `11` (`HttpClient`/lambda `var`) -> `17` (`record`/`sealed`/Text Blocks) -> `21` (Virtual Threads/`switch` patterns/`SequencedCollection`) -> `25` (`FFM Arena`, `_` unnamed vars, `Gatherers`, `ScopedValue`, `JEP 491` monitor unpinning).

## 15. Testing, JPMS & Diagnostics
- Prefer **Fakes** over heavy mocking. JPMS `exports` exposes public API; `opens` allows deep reflection.
- Triage commands: `jcmd <pid> Thread.print` (deadlocks), `jcmd <pid> GC.heap_dump` (leaks), `jcmd <pid> JFR.start` (live profiling), `javap -c -p` (bytecode).
