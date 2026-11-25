# Interview Questions — `ScopedValue` & `StructuredTaskScope` (Java 25)

## 🟢 Beginner

### Q1. What problem does `StructuredTaskScope` solve compared to `CompletableFuture.allOf` or raw `ExecutorService.submit()`?
If subtask A fails in `ExecutorService.submit()`, subtask B keeps running in the background wasting threads/DB sockets as an **orphan thread**! `StructuredTaskScope` binds child virtual tasks strictly to the lifetime of a `try`-with-resources block — automatically canceling slower/remaining subtasks immediately on failure or winner!

---

## 🟡 Intermediate

### Q2. Why is `ScopedValue` superior to `ThreadLocal` — especially with Virtual Threads?
1. **No memory leaks:** `ScopedValue` is automatically unbound when the `.run()` / `.call()` lexical block exits (cannot forget `.remove()`!).
2. **Immutable:** Callees cannot secretly mutate caller state (`set` doesn't exist).
3. **O(1) Child Inheritance:** Inside `StructuredTaskScope`, child virtual threads read parent `ScopedValue`s cleanly without `InheritableThreadLocal`'s expensive map cloning!

### Q3. How did `StructuredTaskScope` syntax change between JDK 21 (first preview) and JDK 25 (`JEP 505`)?
JDK 21 used `new StructuredTaskScope.ShutdownOnFailure()` / `ShutdownOnSuccess()` + `.throwIfFailed()`. JDK 25 replaced constructors with a flexible factory: **`StructuredTaskScope.open(Joiner.awaitAllSuccessfulOrThrow())`** and **`StructuredTaskScope.open(Joiner.anySuccessfulResultOrThrow())`**.

---

## 🔴 Senior

### Q4. Can a `ScopedValue` be rebound inside a nested call, and what happens to an unstructured `new Thread()`?
- **Nested rebinding:** Yes! `ScopedValue.where(KEY, newVal).call(...)` temporarily shadows the outer value within that subtree and **automatically restores** the previous outer binding on exit (even if an exception is thrown).
- **Unstructured threads (`new Thread(...)` / external `ExecutorService`):** Do **NOT** automatically inherit `ScopedValue` bindings — inheritance happens intentionally **only** for structured subtasks forked inside `StructuredTaskScope`, preserving strict lifetime bounds!
