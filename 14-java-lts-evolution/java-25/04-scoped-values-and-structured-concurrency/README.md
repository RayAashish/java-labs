# Java 25 — `ScopedValue` (JEP 506 Finalized) & Structured Concurrency (JEP 505 Preview)

## ⚡ 2-Minute Revision
- **`ScopedValue<T>` (Finalized Java 25 JEP 506):** Replaces fragile `ThreadLocal<T>` for passing immutable per-request context (`Tenant`, `TraceId`, `SecurityContext`) safely across deep call stacks AND child threads!
  - **Bounded Lexical Scope:** Automatically unbound when `ScopedValue.where(KEY, val).run(...)` / `.call(...)` returns — **impossible to forget `.remove()` and leak across thread pools!**
  - **Immutable + Dynamic Rebinding:** Inner nested `where(...)` shadows the outer value temporarily without mutating outer state!
  - **$O(1)$ Inheritance:** Virtual-thread subtasks spawned inside `StructuredTaskScope` automatically inherit bound `ScopedValue`s with zero map copying!
- **`StructuredTaskScope` (JDK 25 JEP 505 Preview):** Treats concurrent subtasks forked from a `try`-block as **one atomic unit of work**:
  - JDK 25 modernized `ShutdownOnFailure`/`ShutdownOnSuccess` to **`StructuredTaskScope.open(Joiner.awaitAllSuccessfulOrThrow())`** and **`Joiner.anySuccessfulResultOrThrow()`**!
  - If one subtask fails, sibling subtasks receive automatic interruption/cancellation immediately — **zero orphan background thread leaks**!

## 📂 Contents
- [`notes/scoped-values-and-structured-concurrency.md`](notes/scoped-values-and-structured-concurrency.md)
- [`codes/lts/java25/concurrency/ScopedValuesDeepDiveDemo.java`](codes/lts/java25/concurrency/ScopedValuesDeepDiveDemo.java) *(Finalized JEP 506 — runs directly with zero preview flags)*
- [`codes/lts/java25/concurrency/StructuredTaskScopeJoinerDemo.java`](codes/lts/java25/concurrency/StructuredTaskScopeJoinerDemo.java) *(Preview JEP 505 — compile & run with `--enable-preview --release 25`)*
- [`interview/INTERVIEW_QUESTIONS.md`](interview/INTERVIEW_QUESTIONS.md)
