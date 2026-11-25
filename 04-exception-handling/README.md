# 04. Exception Handling & JVM Stack Unwinding

> **Learning Method:** Start at **🟢 Noob Track** (`try-catch` intuition), master **🟡 Practitioner Track** (`try-with-resources`, checked vs unchecked, custom exceptions), and dig into **🔴 Senior Track** (JVM exception tables, `fillInStackTrace()` cost, LSP overriding contracts, and suppressed exceptions).

---

## ⚡ 2-Minute Rapid Revision Card

- **Hierarchy:** `Object` -> `Throwable` -> `Error` (unchecked JVM/hardware failures: `OutOfMemoryError`, `StackOverflowError` — don't catch!) & `Exception` -> Checked Exceptions (`IOException`, `SQLException` — compiler-enforced) + `RuntimeException` (unchecked bugs: `NullPointerException`, `IllegalArgumentException`).
- **`finally` Rules:** Always runs *unless* `System.exit()`, JVM crash/SIGKILL, or daemon thread death. **NEVER put `return` or `throw` inside `finally`** — it silently swallows exceptions!
- **Try-With-Resources (Java 7+):** Automatically closes `AutoCloseable` resources in **reverse declaration order** and preserves close errors via `Throwable.getSuppressed()`.
- **Overriding Rule (LSP):** Subclass override may throw **fewer or narrower** checked exceptions, NEVER broader! (Unchecked exceptions have no restriction).
- **Performance Secret:** `new Exception()` calls JNI `fillInStackTrace()`, walking the entire thread call stack ($O(\text{depth})$). Never use exceptions for normal `if-else` control flow; for pure control signaling, override `fillInStackTrace()` to return `this`.

---

## 🧭 Progressive Learning Path

| Phase | Level | Guide & Labs |
|---|---|---|
| **Phase 0 (Start Here)** | 🟢 Noob | [`notes/00-beginner-exception-basics.md`](notes/00-beginner-exception-basics.md) + `exceptions.phase0_beginner.ExceptionBasicsForBeginnersDemo` |
| **Phase 1** | 🟡/🔴 | Hierarchy & JVM Exception Tables ([`notes/phase1_hierarchy_and_jvm_internals/`](notes/phase1_hierarchy_and_jvm_internals/)) |
| **Phase 2** | 🟡/🔴 | Control Flow & `finally` Bytecode Traps ([`notes/phase2_control_flow_and_traps/`](notes/phase2_control_flow_and_traps/)) |
| **Phase 3** | 🟡/🔴 | `AutoCloseable` & Suppressed Exceptions ([`notes/phase3_autocloseable_and_suppressed/`](notes/phase3_autocloseable_and_suppressed/)) |
| **Phase 4** | 🟡/🔴 | OOP Overriding Rules & Checked Exceptions in Streams ([`notes/phase4_oop_inheritance_and_lambdas/`](notes/phase4_oop_inheritance_and_lambdas/)) |
| **Phase 5** | 🔴 Senior | Production Custom Exceptions, Chaining & REST Errors ([`notes/phase5_production_custom_exceptions/`](notes/phase5_production_custom_exceptions/)) |
| **Interview** | 🎯 All | [`interview/INTERVIEW_QUESTIONS.md`](interview/INTERVIEW_QUESTIONS.md) |

---

## 🚀 Compile & Run

```bash
javac -d out $(find 04-exception-handling/codes -name "*.java")
java -cp out exceptions.phase0_beginner.ExceptionBasicsForBeginnersDemo
java -cp out exceptions.phase1_hierarchy_and_jvm_internals.FillInStackTracePerformanceBenchmark
java -cp out exceptions.phase2_control_flow_and_traps.TryCatchFinallyReturnOverrideTrap
java -cp out exceptions.phase3_autocloseable_and_suppressed.SuppressedExceptionsRecoveryDemo
java -cp out exceptions.phase5_production_custom_exceptions.DomainExceptionChainingAndErrorCodeDemo
```
