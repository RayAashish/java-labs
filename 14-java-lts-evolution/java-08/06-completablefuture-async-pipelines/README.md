# Java 8 — `CompletableFuture` Async Pipelines

## ⚡ 2-Minute Revision
- **Why Better Than Java 5 `Future`:** Legacy `future.get()` **blocks** the caller thread, cannot chain callbacks cleanly, cannot combine two futures without manual coordination, and has no declarative error recovery.
- **Core Methods:**
  - Start: `supplyAsync(supplier, executor)` (returns value), `runAsync(runnable, executor)` (void).
  - Transform (`map`): `thenApply(fn)`
  - Flatten chained async calls (`flatMap`): `thenCompose(val -> nextFuture)`
  - Fan-out + merge 2 independent async calls: `f1.thenCombine(f2, (a, b) -> ...)`
  - Wait for N futures: `CompletableFuture.allOf(f1, f2, ...)` / `anyOf(...)`
  - Error recovery: `exceptionally(ex -> fallback)`, `handle((val, ex) -> ...)` (always runs!), `whenComplete(...)` (side-effect pass-through).
  - Java 9+ timeouts: `orTimeout(2, SECONDS)`, `completeOnTimeout(fallback, 2, SECONDS)`.
- **Critical Production Rule:** NEVER omit the `Executor` argument when running blocking HTTP/JDBC tasks — omitting it uses **`ForkJoinPool.commonPool()`**, starving CPU threads across the whole JVM!

## 📂 Contents
- [`notes/completablefuture-async-pipelines.md`](notes/completablefuture-async-pipelines.md)
- [`codes/lts/java08/async/CompletableFuturePipelinesDemo.java`](codes/lts/java08/async/CompletableFuturePipelinesDemo.java)
- [`interview/INTERVIEW_QUESTIONS.md`](interview/INTERVIEW_QUESTIONS.md)
