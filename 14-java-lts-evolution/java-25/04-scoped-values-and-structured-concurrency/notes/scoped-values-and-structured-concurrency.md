# `ScopedValue` (JEP 506) & `StructuredTaskScope` (JEP 505) — Deep Dive

---

## 🟢 Layer 1 — Noob Track: Why `ThreadLocal` Breaks in Modern Java

For 25 years, Spring Security (`SecurityContextHolder`), MDC logging, and tracing frameworks stuffed request context into a `ThreadLocal`. That created three painful bugs:
1. **Unbounded Lifetime Leak:** Forget `.remove()` in a `finally` block -> pooled Tomcat thread #7 handles the NEXT customer's request while still wearing User #1's admin credentials!
2. **Unconstrained Mutability:** Any helper method deep in the call stack can call `tl.set(garbage)`.
3. **Expensive Copies with Virtual Threads:** `InheritableThreadLocal` copies the parent thread's internal `ThreadLocalMap` for *every child thread* — a disaster when spawning 500,000 Virtual Threads!

**`ScopedValue`** solves all three: it is **immutable**, **strictly scoped** to a lambda/block, and **shared in $O(1)$** with structured child threads!

---

## 🟡 Layer 2 — Practitioner Track: `ScopedValue` vs `ThreadLocal` & JDK 25 `Joiner`s

### 1. `ScopedValue` vs `ThreadLocal` Comparison Table

| Dimension | Legacy `ThreadLocal<T>` | Modern `ScopedValue<T>` (Java 25) |
|---|---|---|
| Mutability | Mutable (`set` / `remove` anywhere) | **Immutable** once bound for the scope |
| Cleanup | Manual (`try { ... } finally { remove(); }`) | **Automatic** when `.run()` / `.call()` exits! |
| Child inheritance | `InheritableThreadLocal` copies map ($O(N)$) | Free **$O(1)$ sharing** inside `StructuredTaskScope`! |
| Virtual-thread scale | High memory cost across 1M fibers | Built specifically for Project Loom million-scale fibers! |

### 2. Evolution of `StructuredTaskScope` (JDK 21 -> JDK 25 JEP 505)
- **JDK 21–24 syntax (older guides):** `new StructuredTaskScope.ShutdownOnFailure()` / `ShutdownOnSuccess()` + `.throwIfFailed()`.
- **JDK 25 JEP 505 syntax:** Static factory `StructuredTaskScope.open(Joiner)` + `scope.join()`:
  - `Joiner.awaitAllSuccessfulOrThrow()`: waits until all succeed, or cancels siblings on first failure and throws!
  - `Joiner.<T>anySuccessfulResultOrThrow()`: returns first winning result immediately and cancels slower siblings!
  - `Joiner.allSuccessfulOrThrow()` -> collects stream of succeeded `Subtask`s.

---

## 🔴 Layer 3 — Senior / JVM Architect Track

- **`ScopedValue` Dynamic Scope Stack:** Bound references live cleanly in a compact per-thread pointer cache (`ScopedValue.Carrier`), NOT mutating long-lived thread hashes. Nested `where(KEY, innerVal).run(...)` pushes a temporary binding frame and restores the enclosing outer value unconditionally even on exception!
- **Invariant Check inside `StructuredTaskScope`:** Before exiting `try (var scope = StructuredTaskScope.open(...))`, `close()` asserts that all forked virtual threads have terminated cleanly — structurally guaranteeing **observability & stack-trace hierarchy** (`jcmd <pid> Thread.dump_to_file` displays clean parent->child trees!).
- **Flag reminder:** `ScopedValue` requires **NO `--enable-preview`** on Java 25 (`JEP 506` finalized!). Only `StructuredTaskScope` (`JEP 505` fifth preview in JDK 25) needs `--enable-preview`.
