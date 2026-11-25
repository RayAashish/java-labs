# CompletableFuture: Async Pipelines, DAGs & Resilient Error Handling
> **Author:** raysi  
> **Package:** `com.raysi.concurrency.phase4_executors_and_completablefuture`  
> **Module:** Asynchronous Non-Blocking Pipelines, Completion Stage Callbacks, and DAG Composition

---

# 1. Hardware & JVM Architectural Mental Model

Before Java 8, asynchronous programming relied on `java.util.concurrent.Future`. The fatal flaw of classic `Future` was that retrieving a result required calling `future.get()`, which **synchronously blocks the calling thread** until the computation completes. If an application calls 10 microservices sequentially, 10 OS threads are blocked, consuming CPU stacks and causing thread pool starvation.

`CompletableFuture` implements the **Reactive Callback / Promise Pattern** using a **Directed Acyclic Graph (DAG)** of execution stages. Instead of blocking, a thread registers a callback that triggers automatically when the upstream task completes.

```
┌────────────────────────────────────────────────────────────────────────┐
│                 COMPLETABLEFUTURE ASYNC DAG EXECUTION                  │
│                                                                        │
│   supplyAsync(fetchUser, ioPool)                                       │
│                │                                                       │
│                ▼                                                       │
│   ┌──────────────────────────┐                                         │
│   │  Stage 1: User Profile   │                                         │
│   └────────────┬─────────────┘                                         │
│                │                                                       │
│      thenApply │ (Transform)                                           │
│                ▼                                                       │
│   ┌──────────────────────────┐                                         │
│   │  Stage 2: Format Data    │ ──┐                                     │
│   └──────────────────────────┘   │                                     │
│                                  │ thenCombine                         │
│                                  │ (Zip both results                   │
│   supplyAsync(fetchOrders, ioPool│  asynchronously)                    │
│                │                 │                                     │
│                ▼                 │                                     │
│   ┌──────────────────────────┐   │                                     │
│   │  Stage 3: Order History  │ ◄─┘                                     │
│   └────────────┬─────────────┘                                         │
│                │                                                       │
│                ▼                                                       │
│   ┌──────────────────────────┐                                         │
│   │ Stage 4: Aggregated Page │                                         │
│   └──────────────────────────┘                                         │
└────────────────────────────────────────────────────────────────────────┘
```

---

# 2. Under the Hood: JDK Source Code & Bytecode Mechanics

### Internal Treiber Stack of `Completion` Nodes
Inside OpenJDK `java.util.concurrent.CompletableFuture`:
```java
public class CompletableFuture<T> implements Future<T>, CompletionStage<T> {
    volatile Object result;       // Holds result T or AltResult (Exception)
    volatile Completion stack;    // Lock-free Treiber stack of dependent callbacks

    // Evaluates dependent stage upon upstream completion:
    final void postComplete() {
        // Pops Completion nodes from Treiber stack via CAS and pushes them to executor!
    }
}
```
When you chain `.thenApply(fn)`, if the future is not yet finished, the JVM allocates a `UniApply` completion node and pushes it onto the atomic `stack` using CAS. When `complete(val)` is eventually called, `postComplete()` pops and executes each registered callback.

### The `thenApply` vs `thenCompose` vs `thenCombine` Trinity
1. **`thenApply(Function<T, U>)`**: Analogous to `Stream.map`. Synchronously transforms output `T` into output `U` on the same thread:
   $$\text{CompletableFuture}\langle T\rangle \xrightarrow{\text{thenApply}} \text{CompletableFuture}\langle U\rangle$$
2. **`thenCompose(Function<T, CompletableFuture<U>>)`**: Analogous to `Stream.flatMap`. Avoids nested futures (`CompletableFuture<CompletableFuture<U>>`):
   $$\text{CompletableFuture}\langle T\rangle \xrightarrow{\text{thenCompose}} \text{CompletableFuture}\langle U\rangle$$
3. **`thenCombine(CompletionStage<U>, BiFunction<T, U, V>)`**: Zips two independent asynchronous futures together:
   $$\text{CF}\langle T\rangle + \text{CF}\langle U\rangle \xrightarrow{\text{thenCombine}} \text{CF}\langle V\rangle$$

---

# 3. Step-by-Step State Transitions & Concurrency Traps

### 1. The `ForkJoinPool.commonPool()` Starvation Trap
```java
// DANGEROUS IN PRODUCTION:
CompletableFuture.supplyAsync(() -> fetchRemoteMicroservice());
```
If you omit the `Executor` argument, `CompletableFuture` defaults to `ForkJoinPool.commonPool()`.
- The common pool has a parallelism level equal to $(N_{\text{CPU Cores}} - 1)$. On a 4-core container, that is only 3 threads!
- If 3 HTTP requests block on a slow database, **the entire JVM's common pool is paralyzed**, breaking parallel streams and other system components.
- **Rule:** Always pass a dedicated, bounded custom `Executor` for any network/disk I/O!

### 2. Thread Hopping: `thenApply` vs `thenApplyAsync`
- `thenApply(fn)`: Executes on the **thread that completed the prior stage**. If the prior stage was already complete when `thenApply` was called, it executes immediately on the **calling thread**!
- `thenApplyAsync(fn, executor)`: **Always** submits the callback to the specified executor, guaranteeing execution on an independent thread pool.

### 3. Resilient Error Handling Patterns
In asynchronous programming, standard `try-catch` blocks cannot catch exceptions thrown on worker threads. Use:
- **`exceptionally(ex -> fallbackValue)`**: Catches any upstream exception and provides a fallback value.
- **`handle((result, ex) -> ...)`**: Bi-function always invoked regardless of whether computation succeeded or failed.
- **`completeOnTimeout(defaultVal, 500, TimeUnit.MILLISECONDS)`** (Java 9+): Returns fallback value if timeout expires.

---

# 4. Comparison & Trade-Off Matrix

| Feature | Classic `Future<T>` | `CompletableFuture<T>` | Reactive Streams (`Flux`/`Mono`) |
| :--- | :--- | :--- | :--- |
| **Non-blocking Callbacks** | No (`get()` blocks) | **Yes** (`thenApply`, etc.) | **Yes** (Push-based) |
| **Pipeline Composition** | Manual looping | **Rich DAG Operators** | Comprehensive Functional Operators|
| **Backpressure Support** | None | **None (Can overflow memory)** | **Built-in Reactive Backpressure** |
| **JDK Standard** | Yes (Java 5+) | **Yes (Java 8+)** | Requires external library (Reactor)|
| **Exception Propagation** | Throws `ExecutionException` | Encapsulates in `AltResult` | Signal onError event |

---

# 5. Runnable Java Verification Code (`author: raysi`)

The companion lab for this module is located at:
[`CompletableFutureMicroserviceAggregator.java`](../../codes/concurrency/phase4_executors_and_completablefuture/CompletableFutureMicroserviceAggregator.java)

Run via:
```bash
java -cp target/classes com.raysi.concurrency.phase4_executors_and_completablefuture.codes.CompletableFutureMicroserviceAggregator
```

---

# 6. Top 8 High-Signal Concurrency Interview Questions & Answers

### Q1: What is the exact difference between `thenApply` and `thenCompose`?
**Answer:** `thenApply` takes a function that returns an ordinary value `U` and wraps it into `CompletableFuture<U>` (mapping). `thenCompose` takes a function that returns another `CompletableFuture<U>` and flattens it directly into `CompletableFuture<U>` (flat-mapping), avoiding nested `CompletableFuture<CompletableFuture<U>>` structures.

### Q2: What is the difference between `thenApply` and `thenApplyAsync`?
**Answer:** `thenApply` executes the transformation function on whichever thread completed the previous stage (or synchronously on the current thread if the stage is already complete). `thenApplyAsync` forces the transformation function to be dispatched asynchronously as a new task to the provided `Executor` (or `ForkJoinPool.commonPool()` if none provided).

### Q3: Why is using `ForkJoinPool.commonPool()` with `CompletableFuture` considered an anti-pattern in microservices?
**Answer:** The common pool is sized strictly for CPU-intensive parallel computations ($N_{\text{cores}} - 1$). If used for blocking I/O (REST APIs, JDBC calls), worker threads block on network sockets. The common pool quickly becomes starved, freezing parallel streams and other critical JVM tasks across the application.

### Q4: How does `CompletableFuture.allOf()` work and what is its return type?
**Answer:** `allOf(CompletableFuture<?>... cfs)` returns a `CompletableFuture<Void>`. It completes only when **all** input futures have completed. Because its return type is `Void`, you must manually extract each individual future's result using `.join()` after `allOf()` completes.

### Q5: What is the difference between `future.get()` and `future.join()`?
**Answer:** Both block the calling thread until the future completes. However, `get()` is declared on `Future` and throws checked exceptions (`InterruptedException` and `ExecutionException`). `join()` is declared on `CompletableFuture` and throws unchecked exceptions (`CompletionException`), making it cleaner for use inside functional lambdas and streams.

### Q6: How do you handle timeouts in `CompletableFuture` without blocking?
**Answer:** Java 9 introduced `orTimeout(timeout, unit)` and `completeOnTimeout(fallback, timeout, unit)`. `orTimeout()` completes the future exceptionally with `TimeoutException` if not finished in time. `completeOnTimeout()` gracefully completes the future with a specified fallback value, without throwing an exception.

### Q7: What is the internal representation of an exception in `CompletableFuture`?
**Answer:** When an exception is thrown inside a computation, `CompletableFuture` wraps the `Throwable` inside an internal class called `AltResult` and assigns it to the `volatile Object result` field. Dependent stages check if `result instanceof AltResult`; if so, standard completion stages bypass execution and propagate the error down the DAG until an `exceptionally()` or `handle()` stage is encountered.

### Q8: How can you cancel a `CompletableFuture`?
**Answer:** Calling `future.cancel(mayInterruptIfRunning)` transitions the future to a completed state with a `CancellationException`. However, unlike standard threads, `CompletableFuture` **does not interrupt the underlying thread** executing the task unless explicitly wired with custom thread interruption logic.
