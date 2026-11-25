# 14. Java LTS Evolution (Java 8 -> 11 -> 17 -> 21 -> 25) — Interview Questions & Answers

---

## 🟢 Beginner Questions

### Q1. What does LTS mean in Java, and which versions are LTS?
**Long-Term Support** versions receive years of enterprise security & bug patches. Recent LTS releases: **Java 8 (2014), Java 11 (2018), Java 17 (2021), Java 21 (2023), Java 25 (2025)**.

### Q2. What are Text Blocks (`"""`) introduced in Java 15/17?
Multi-line string literals delimited by triple quotes `"""`, preserving formatting cleanly for JSON, SQL, and HTML without `\n` or `\"` escaping clutter, while automatically stripping incidental leading indentation.

### Q3. How do Records (`record`) reduce boilerplate in Java 17+?
`public record Point(int x, int y) {}` automatically generates `private final` fields, a canonical constructor, accessor methods (`x()`, `y()`), `equals()`, `hashCode()`, and `toString()`.

---

## 🟡 Intermediate Questions

### Q4. What are the limitations and rules of Java `record`s?
1. Implicitly **`final`** and implicitly extend `java.lang.Record` — **cannot extend any other class** (though they CAN implement interfaces!).
2. Components are **`private final`** (shallowly immutable — defensive copy mutable `List`/`Date` inside a **compact constructor** `public Order { items = List.copyOf(items); }`).
3. Cannot declare extra non-static instance fields (static fields and methods are allowed).

### Q5. What are Sealed Classes & Interfaces (`sealed ... permits ...`), and how do they pair with `switch`?
`sealed interface Payment permits Card, Upi, BankTransfer` locks down the exact set of allowed subtypes at compile time. When paired with Java 21 **Pattern Matching for `switch`**, `javac` verifies **exhaustiveness at compile time**, eliminating `default` fallback branches and guaranteeing that adding a new `Crypto` subtype triggers compile errors everywhere it wasn't handled!

### Q6. Explain Java 11 `HttpClient` vs legacy `HttpURLConnection`.
`java.net.http.HttpClient` supports HTTP/1.1 & HTTP/2 (multiplexed streams), native async (`sendAsync` returning `CompletableFuture`), reactive body subscribers/publishers, and native WebSockets without third-party dependencies.

### Q7. How did Java 11 improve `String` and `var`?
- `String`: `isBlank()`, `lines()`, `strip()`, `stripLeading()`, `stripTrailing()`, `repeat(n)`.
- `var`: Allowed inside lambda parameters (`(@NonNull var x, var y) -> ...`) so annotations can be placed on inferred parameters.
- Single-file execution: `java MyScript.java` directly without manual `javac`.

---

## 🔴 Senior / Modern Architecture Questions (Java 21–25)

### Q8. Explain Pattern Matching for `instanceof` (Java 16), `switch`, and Record Patterns (Java 21).
- **`instanceof` (Java 16):** `if (obj instanceof String s && !s.isEmpty())` eliminates redundant casting.
- **Record Patterns & Guarded `switch` (Java 21):** Deconstruct nested records directly inside `case`:
  ```java
  switch (shape) {
      case Rectangle(Point(int x, int y), int w, int h) when w == h -> ...
      case Circle(Point center, double r) -> ...
  }
  ```

### Q9. Compare **Virtual Threads (Java 21)**, **`ScopedValue` (Java 25)**, and **Structured Concurrency (Java 21–25)**.
- **Virtual Threads (`JEP 444`):** Lightweight JVM-scheduled threads allowing 100k–1M concurrent blocking I/O operations without reactive/WebFlux callback complexity.
- **`ScopedValue` (`JEP 506`, finalized Java 25):** Immutable, lexically scoped alternative to `ThreadLocal` with zero thread-pool leak risk and O(1) inheritance in structured subtasks.
- **Structured Concurrency (`StructuredTaskScope`):** Ties child task lifecycles to a `try-with-resources` block — if one fails, sibling tasks cancel immediately (no orphan threads!). Note: JDK 25 (`JEP 505`) modernized `ShutdownOnFailure`/`ShutdownOnSuccess` to `StructuredTaskScope.open(Joiner...)`.

### Q10. What changed with Virtual Thread Pinning in Java 24/25 (`JEP 491`)?
In Java 21, blocking I/O inside a `synchronized` block/method **pinned** the virtual thread to its OS carrier thread (blocking it). **JEP 491** (JDK 24/25) lets virtual threads unmount cleanly even while holding intrinsic `synchronized` monitors (leaving only JNI/FFI native frames as pinning cases!).

### Q11. What is the Foreign Function & Memory (FFM) API (`JEP 454`, Java 22+), and what does it replace?
`Arena`, `MemorySegment`, and `Linker` provide **type-safe, deterministic, bounds-checked off-heap memory** and C library invocation in pure Java — replacing fragile, leak-prone `sun.misc.Unsafe` and slow boilerplate **JNI** C headers!

### Q12. What are Java 24/25 Stream Gatherers (`JEP 485`) and Unnamed Variables `_` (`JEP 456`)?
- **Unnamed Variables `_`:** Explicitly marks ignored catch exceptions (`catch (NumberFormatException _)`), unused lambda parameters (`(_, val) -> ...`), and unused pattern components (`case Point(int x, _) -> x`).
- **Stream Gatherers:** Standard extensible intermediate stream ops (`Gatherers.windowFixed(n)`, `windowSliding(n)`, `scan(...)`, `mapConcurrent(...)`).

### Q13. What is Java 24/25 **Compact Object Headers (`JEP 450` / `JEP 519`)**?
Shrinking the 64-bit HotSpot object header from **12–16 bytes (64-bit Mark Word + 32-bit Klass Word) down to 8 bytes** (`-XX:+UseCompactObjectHeaders`), cutting microservice Heap usage by ~10–20% and reducing GC pauses!
