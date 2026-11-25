# ThreadPoolExecutor Internals, Bit-Packing & Sizing Mathematics
> **Author:** raysi  
> **Package:** `com.raysi.concurrency.phase4_executors_and_completablefuture`  
> **Module:** Thread Pool Lifecycle, `ctl` Bit-Packing, Task Routing Mechanics, and Sizing Formulations

---

# 1. Hardware & JVM Architectural Mental Model

Creating and destroying an OS thread for every incoming HTTP request or database query is catastrophic to JVM stability. Each thread consumes ~1MB of off-heap stack memory and incurs ~1-5 µs of kernel context switch latency.

A **Thread Pool** decouples task submission from task execution:
1. It maintains a bounded set of pre-warmed worker threads waiting for work.
2. It buffers excess tasks in a thread-safe blocking queue.
3. It balances CPU utilization against physical memory constraints.

```
┌────────────────────────────────────────────────────────────────────────┐
│                   THREADPOOLEXECUTOR TASK ROUTING PIPELINE             │
│                                                                        │
│                      [ Incoming Task: execute(task) ]                  │
│                                     │                                  │
│                 Are Active Workers < corePoolSize?                     │
│                             /               \                          │
│                       YES  /                 \  NO                     │
│                           ▼                   ▼                        │
│                ┌─────────────────────┐   Can task fit in               │
│                │ Spawn new CoreWorker│   BlockingQueue?                │
│                │   (Run Immediately) │        /        \               │
│                └─────────────────────┘  YES  /          \  NO          │
│                                             ▼            ▼             │
│                                      ┌───────────┐  Are Active Workers │
│                                      │ Enqueue in│  < maximumPoolSize? │
│                                      │ Queue     │      /       \      │
│                                      └───────────┘ YES /         \ NO  │
│                                                       ▼           ▼    │
│                                              ┌─────────────┐ ┌────────┐│
│                                              │ Spawn Non-  │ │ REJECT ││
│                                              │ Core Worker │ │ HANDLER││
│                                              └─────────────┘ └────────┘│
└────────────────────────────────────────────────────────────────────────┘
```

---

# 2. Under the Hood: JDK Source Code & Bytecode Mechanics

### The Genius of `ctl`: Bit-Packed AtomicInteger
To avoid holding locks whenever reading the thread pool's state or worker count, Doug Lea packed **both** the Pool Lifecycle State and the Worker Thread Count into a **single 32-bit `AtomicInteger ctl`**:

```java
// OpenJDK java.util.concurrent.ThreadPoolExecutor
private final AtomicInteger ctl = new AtomicInteger(ctlOf(RUNNING, 0));
private static final int COUNT_BITS = Integer.SIZE - 3; // 29 bits
private static final int COUNT_MASK = (1 << COUNT_BITS) - 1; // 0x1FFFFFFF (max 536,870,911 threads)

// Top 3 bits = runState; Bottom 29 bits = workerCount
private static int runStateOf(int c)     { return c & ~COUNT_MASK; }
private static int workerCountOf(int c)  { return c & COUNT_MASK; }
```

```
 31 30 29 28                                                    0
┌────────┬────────────────────────────────────────────────────────┐
│ STATE  │               WORKER COUNT (29 bits)                   │
└────────┴────────────────────────────────────────────────────────┘
 [111] -> RUNNING    (Accepts new tasks and processes queued tasks)
 [000] -> SHUTDOWN   (Refuses new tasks, processes queued tasks)
 [001] -> STOP       (Refuses new tasks, halts in-flight tasks)
 [010] -> TIDYING    (All tasks terminated, workerCount is zero)
 [011] -> TERMINATED (terminated() hook has completed)
```
Because both values reside in a single word, transitioning states and updating worker counts requires only a single atomic CAS instruction!

### Anatomy of `Worker`
The inner class `Worker extends AbstractQueuedSynchronizer implements Runnable`:
- Implements a non-reentrant mutex lock via AQS to distinguish between a thread actively running a task vs an idle thread waiting in `getTask()`.
- Executes a continuous loop:
  ```java
  while (task != null || (task = getTask()) != null) {
      w.lock();
      try {
          beforeExecute(wt, task);
          task.run();
          afterExecute(task, null);
      } finally {
          w.unlock();
      }
  }
  ```
- If the thread is an extra worker beyond `corePoolSize`, `getTask()` uses `workQueue.poll(keepAliveTime, NANOSECONDS)`. If the timeout expires with no work, the worker thread terminates.

---

# 3. Step-by-Step State Transitions & Concurrency Traps

### 1. The `Executors.newFixedThreadPool()` Production OutOfMemoryError Trap
```java
// DO NOT USE IN PRODUCTION!
ExecutorService pool = Executors.newFixedThreadPool(10);
```
Looking at its source code:
```java
return new ThreadPoolExecutor(nThreads, nThreads, 0L, TimeUnit.MILLISECONDS,
                              new LinkedBlockingQueue<Runnable>()); // Capacity = Integer.MAX_VALUE!
```
Because the queue is practically unbounded ($2^{31}-1 \approx 2.14 \text{ billion}$ items), the active worker count **never exceeds `corePoolSize`** and tasks pile up indefinitely if downstream services slow down. The heap is exhausted, throwing `java.lang.OutOfMemoryError: Java heap space`.

### 2. The `Executors.newCachedThreadPool()` OS Thread Exhaustion Trap
```java
// DO NOT USE IN PRODUCTION!
ExecutorService pool = Executors.newCachedThreadPool();
```
Uses `SynchronousQueue` with `maximumPoolSize = Integer.MAX_VALUE`. Under a sudden traffic spike, it spawns thousands of OS threads until the operating system refuses new threads:
`java.lang.OutOfMemoryError: unable to create new native thread`

---

# 4. Thread Pool Sizing Mathematics: The Production Formulas

Never guess thread pool sizes. Sizing depends strictly on the **nature of the workload**:

### 1. CPU-Bound Tasks (Hashing, Crypto, Compression, JSON Parsing):
Tasks spend 100% of their execution time crunching instructions on the physical core without blocking for I/O. Adding threads beyond the core count adds only context-switching overhead:
$$N_{\text{threads}} = N_{\text{CPU Cores}} + 1$$
*(The $+1$ compensates for an occasional page fault or OS context switch).*

### 2. I/O-Bound Tasks on Platform Threads (DB Queries, REST, Disk I/O):
Threads spend the vast majority of time blocked waiting for network packets or disk heads. The ratio of Wait Time ($W$) to Compute Time ($C$) governs concurrency:
$$N_{\text{threads}} = N_{\text{CPU Cores}} \times \left(1 + \frac{W}{C}\right)$$

#### Real-World Example:
- CPU Cores: 8
- Profiling shows: 90 ms network wait ($W$), 10 ms CPU serialization ($C$).
- $\frac{W}{C} = \frac{90}{10} = 9$.
- $N_{\text{threads}} = 8 \times (1 + 9) = 80 \text{ Platform Threads}$.

---

# 5. Comparison & Trade-Off Matrix of Rejection Policies

| Policy | Behavior Under Saturation | Best Use Case | Risk / Pitfall |
| :--- | :--- | :--- | :--- |
| **`AbortPolicy`** (Default)| Throws `RejectedExecutionException` | Critical business tasks | Crashes caller if unhandled |
| **`CallerRunsPolicy`** | Caller thread executes the task | Microservices requiring backpressure| Slows down upstream HTTP pipeline |
| **`DiscardPolicy`** | Silently drops the task | Non-essential metrics / telemetry | Silent data loss |
| **`DiscardOldestPolicy`** | Drops head of queue and retries | Real-time sensor / stock tick data | Drops older valid transactions |
| **Custom Handler** | Log to Kafka / Dead-Letter Queue | High-reliability enterprise finance | Requires fallback infrastructure |

---

# 6. Runnable Java Verification Code (`author: raysi`)

The companion lab for this module is located at:
[`CustomThreadPoolExecutorRejectionDemo.java`](../../codes/concurrency/phase4_executors_and_completablefuture/CustomThreadPoolExecutorRejectionDemo.java)

Run via:
```bash
java -cp target/classes com.raysi.concurrency.phase4_executors_and_completablefuture.codes.CustomThreadPoolExecutorRejectionDemo
```

---

# 7. Top 8 High-Signal Concurrency Interview Questions & Answers

### Q1: In what exact sequence does `ThreadPoolExecutor` route a submitted task?
**Answer:**
1. If `activeWorkerCount < corePoolSize`, spawn a new worker thread to execute the task.
2. If `activeWorkerCount >= corePoolSize`, attempt to enqueue the task in the `BlockingQueue`.
3. If the queue is full and `activeWorkerCount < maximumPoolSize`, spawn a new non-core worker thread.
4. If the queue is full and `activeWorkerCount >= maximumPoolSize`, invoke the `RejectedExecutionHandler`.

### Q2: Why does `ThreadPoolExecutor` fill the queue BEFORE creating threads up to `maximumPoolSize`?
**Answer:** Threads are expensive in memory and CPU scheduling. The pool assumes `corePoolSize` represents steady-state capacity; transient spikes should be buffered in the queue. Only when the buffer overflows does the pool spin up expensive extra threads up to `maximumPoolSize`.

### Q3: How does `ctl` avoid race conditions during state transitions?
**Answer:** By packing both the 3-bit lifecycle state and 29-bit worker count into a single 32-bit `AtomicInteger`. Any update to either the state or the worker count is executed as a single atomic CAS instruction (`compareAndSet(c, ctlOf(targetState, targetCount))`), preventing torn reads and eliminating synchronization locks.

### Q4: What is the difference between `shutdown()` and `shutdownNow()`?
**Answer:** `shutdown()` initiates a graceful shutdown: transitions state to `SHUTDOWN`, stops accepting new tasks, but allows currently running and queued tasks to complete. `shutdownNow()` transitions state to `STOP`, attempts to interrupt all actively executing worker threads, drains the queue, and returns the list of unexecuted tasks.

### Q5: What happens if a task submitted via `execute()` throws an unchecked exception vs via `submit()`?
**Answer:** With `execute()`, the unhandled exception terminates the worker thread, triggers the `UncaughtExceptionHandler`, and the pool replaces the dead worker with a new one. With `submit()`, the exception is caught and silently encapsulated inside the returned `Future`; it is only exposed when the caller invokes `future.get()` (wrapped in `ExecutionException`).

### Q6: How does `CallerRunsPolicy` provide natural backpressure in Spring Boot / Netty microservices?
**Answer:** When the thread pool and queue are saturated, the calling thread (e.g. the HTTP acceptor or servlet thread) executes the task itself. While the caller thread is executing the long-running task, it cannot accept new incoming HTTP requests or read from socket buffers, effectively throttling upstream traffic until the pool recovers.

### Q7: Can core threads in a `ThreadPoolExecutor` ever time out and terminate?
**Answer:** Yes, by calling `executor.allowsCoreThreadTimeOut(true)`. By default, core threads remain alive indefinitely, but enabling this flag applies `keepAliveTime` to core threads as well when the pool is idle.

### Q8: Why does `ForkJoinPool` use Work-Stealing while `ThreadPoolExecutor` uses a single shared queue?
**Answer:** In `ThreadPoolExecutor`, all worker threads compete for a single shared `BlockingQueue`, causing intense lock contention on the queue's head and tail. In `ForkJoinPool`, each worker has its own private deque. Workers pop their own tasks without contention; only idle workers steal from peers' heads, drastically reducing synchronization bottlenecks for divide-and-conquer workloads.
