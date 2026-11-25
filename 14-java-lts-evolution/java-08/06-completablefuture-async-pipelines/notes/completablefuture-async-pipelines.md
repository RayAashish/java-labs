# `CompletableFuture` Asynchronous Pipelines — Deep Dive

---

## 🟢 Layer 1 — Noob Track: Plain-English Intuition

Suppose an API endpoint needs:
1. User Profile (200 ms)
2. User Wallet Balance (200 ms)
3. Loyalty Points (200 ms)

With blocking sequential code, you wait **600 ms**. With Java 5 `Future`, you still block calling `.get()` to combine them. With `CompletableFuture`, you launch all three **concurrently** (`~200 ms` total) and declare *"when Profile and Wallet finish, merge them into a Dashboard DTO — and if Loyalty times out, return `0` points instead of crashing the whole page!"*

---

## 🟡 Layer 2 — Practitioner Track: `thenApply` vs `thenCompose` vs `thenCombine`

| Scenario | Method | Equivalent in `Optional` / `Stream` |
|---|---|---|
| Synchronous value transform `T -> U` | `thenApply(fn)` | `Stream.map` |
| Dependent async call `T -> CompletableFuture<U>` | `thenCompose(fn)` | `Stream.flatMap` (prevents `CompletableFuture<CompletableFuture<U>>`!) |
| Two independent futures `f1` + `f2` -> `(A, B) -> R` | `f1.thenCombine(f2, biFn)` | Parallel fan-out + join |
| Race multiple mirrors (fastest wins) | `CompletableFuture.anyOf(f1, f2)` | Hedge requests |
| Aggregate N heterogeneous futures | `CompletableFuture.allOf(f1, f2, ...)` | Returns `CompletableFuture<Void>`; call `.join()` on each original future after `allOf.join()` |

### Exception Handling Trio
1. `exceptionally(Throwable -> fallback)` -> runs **only on failure** (recover to value of same type `T`).
2. `handle((result, ex) -> ...)` -> runs **on both success and failure** (can map type `T -> U`, acting like `try/catch` returning a new value).
3. `whenComplete((result, ex) -> ...)` -> runs on both success & failure for **logging/metrics without swallowing or altering** the exception or result!

---

## 🔴 Layer 3 — Senior / JVM Track: Thread Selection & Completion Stack

### 1. Where Does `thenApply` vs `thenApplyAsync` Execute?
- **Non-Async (`thenApply`):** Executes on the **thread that completed the previous stage** — OR if the stage was already complete when `thenApply` was attached, runs synchronously on the **caller (`main`) thread**!
- **`*Async` without executor (`thenApplyAsync(fn)`):** Always dispatches to **`ForkJoinPool.commonPool()`**.
- **`*Async` with executor (`thenApplyAsync(fn, ioExecutor)`):** Runs deterministically on your **custom bounded thread pool** (preventing `commonPool` starvation and CPU pool pollution!).

### 2. `.get()` vs `.join()`
- `.get()` throws checked `InterruptedException` + `ExecutionException` (clunky inside Stream lambdas).
- `.join()` wraps exceptions into unchecked **`CompletionException`** cleanly (ideal at pipeline boundaries or inside lambdas!).
- Unhandled exceptions inside `CompletionException` capture the cause stack trace — extract via `ex.getCause()` when inspecting original business exceptions.
