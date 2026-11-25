# Interview Questions — Java 8 `CompletableFuture`

## 🟢 Beginner

### Q1. How is `CompletableFuture` superior to Java 5 `Future`?
`Future` only supports blocking `.get()`, `.isDone()`, and `.cancel()`. `CompletableFuture` implements both `Future` and `CompletionStage`, letting you register non-blocking callbacks, compose sequential and parallel async stages, combine multiple futures, handle errors declaratively, and complete a future manually (`complete(val)`).

---

## 🟡 Intermediate

### Q2. Compare `thenApply`, `thenCompose`, and `thenCombine`.
- `thenApply(T -> U)`: transforms result synchronously (`map`).
- `thenCompose(T -> CompletableFuture<U>)`: chains a second async step cleanly without nested `CompletableFuture<CompletableFuture<U>>` (`flatMap`).
- `thenCombine(otherFuture, (a, b) -> R)`: waits for two **independent** futures to finish and combines both results.

### Q3. What is the difference between `.get()` and `.join()`?
Both block until completion, but `.get()` throws checked `InterruptedException` & `ExecutionException`, while `.join()` throws unchecked `CompletionException`, making `.join()` much cleaner inside Stream pipelines and lambdas.

---

## 🔴 Senior

### Q4. Which thread runs `thenApply` vs `thenApplyAsync`?
- `thenApply`: runs either on the thread that completed the upstream stage OR synchronously on the caller thread if already completed!
- `thenApplyAsync(fn)`: always submits to **`ForkJoinPool.commonPool()`**.
- `thenApplyAsync(fn, executor)`: deterministically dispatches to your custom `executor`.

### Q5. Why is calling `CompletableFuture.supplyAsync(blockingDbCall)` without an `Executor` a production anti-pattern?
Without an explicit `Executor`, it runs inside `ForkJoinPool.commonPool()` (size `CPU cores - 1`). A handful of slow DB queries will occupy all common pool threads, freezing every other async pipeline AND every `parallelStream()` across the entire microservice! *(Also remember to upgrade I/O fan-outs to Java 21+ Virtual Threads or `StructuredTaskScope` where applicable!)*
