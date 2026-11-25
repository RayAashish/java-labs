# 09. Multithreading, Concurrency, JMM & Virtual Threads — Interview Questions & Answers

---

## 🟢 Beginner Questions

### Q1. What is the difference between a Process and a Thread?
A **Process** is an independent program execution with its own isolated virtual memory space (Heap, Metaspace, file handles). **Threads** live inside a process, sharing the **same Heap** while each owning a private **Call Stack & Program Counter**.

### Q2. `extends Thread` vs `implements Runnable` vs `Callable<V>`?
- `Runnable` (`void run()`): Preferred over extending `Thread` because Java allows only single class inheritance and decouples task definition from execution thread pooling.
- `Callable<V>` (`V call() throws Exception`): Returns a typed result `Future<V>` and can throw checked exceptions.

### Q3. `start()` vs `run()`?
Calling `thread.start()` asks the JVM/OS to spawn a new execution thread and invoke `run()` asynchronously. Calling `thread.run()` directly simply executes on the **current caller thread** synchronously!

### Q4. `Thread.sleep()` vs `Object.wait()`?
- `sleep(ms)`: Static `Thread` method; pauses current thread for a timed duration while **KEEPING all held `synchronized` locks**.
- `wait()`: `Object` method; **RELEASES the monitor lock** and suspends until `notify()`/`notifyAll()` (must be called inside `synchronized(lock)`!).

---

## 🟡 Intermediate Questions

### Q5. Explain `volatile` — why does `volatile int count = 0; count++;` STILL lose updates?
`volatile` guarantees **Visibility** (reads/writes bypass CPU L1/L2 cache directly to main memory coherence) and **Ordering** (prevents compiler/CPU instruction reordering across memory barriers). It does **NOT** guarantee **Atomicity**! `count++` is 3 steps (read -> add 1 -> write), so two threads can read the same value and overwrite each other. Use `AtomicInteger` or `LongAdder`.

### Q6. `synchronized` vs `ReentrantLock`?
Both are reentrant mutual-exclusion locks. `ReentrantLock` adds:
1. Timed / interruptible acquisition (`tryLock(timeout)`, `lockInterruptibly()` — prevents permanent deadlocks!).
2. Optional **Fairness** (`new ReentrantLock(true)` FIFO queue).
3. Multiple independent `Condition` queues (`notFull`, `notEmpty`) instead of a single `wait()` set.
4. `synchronized` auto-releases on exception; `ReentrantLock` **must** be unlocked inside `finally`.

### Q7. Why should `wait()` ALWAYS sit inside a `while` loop, NEVER an `if`?
Guards against **spurious wakeups** (OS kernel can wake threads spuriously without `notify()`) and **stolen wakeups** (another thread acquires the lock and consumes the item before the woken thread re-enters!).

### Q8. `CountDownLatch` vs `CyclicBarrier` vs `Semaphore`?
- **`CountDownLatch`:** One-shot gate (`count` counts down to `0` once; not reusable). Main thread(s) wait for N workers/services to initialize.
- **`CyclicBarrier`:** Reusable rendezvous point where N worker threads wait for **each other** before all proceed together (supports barrier action).
- **`Semaphore`:** Controls max concurrent access permits (`N` permits, e.g., DB connection throttling or rate limiting — unrelated threads can call `release()`).

### Q9. What are the 4 Coffman conditions for Deadlock, and how do you prevent/diagnose it?
Conditions: (1) Mutual Exclusion, (2) Hold & Wait, (3) No Preemption, (4) Circular Wait.
- **Prevention:** Acquire locks in a **strict global canonical order** (breaks Circular Wait), or use `tryLock(timeout)` with backoff.
- **Diagnosis:** Run `jstack <pid>` or `jcmd <pid> Thread.print` (or `ThreadMXBean.findDeadlockedThreads()`).

### Q10. Why should you NEVER use `Executors.newFixedThreadPool()` or `newCachedThreadPool()` blindly in production?
- `newFixedThreadPool(n)` uses an **unbounded `LinkedBlockingQueue`** (`Integer.MAX_VALUE`) -> slow downstream causes infinite task buildup -> **`OutOfMemoryError`**.
- `newCachedThreadPool()` creates **unlimited threads** (`Integer.MAX_VALUE`) -> burst traffic spawns 50,000 OS threads -> CPU thrashing / OS OOM!
Always construct `new ThreadPoolExecutor(...)` with a **bounded `ArrayBlockingQueue`** and an explicit `RejectedExecutionHandler` (`CallerRunsPolicy`, etc.).

---

## 🔴 Senior / JMM & Loom Architecture Questions

### Q11. Explain the Java Memory Model (JMM) Happens-Before guarantees.
Happens-before defines when writes by Thread A are guaranteed visible to Thread B despite CPU store buffers and JIT reordering:
- Program order within a single thread
- Monitor unlock happens-before subsequent lock on same monitor
- `volatile` write happens-before subsequent `volatile` read
- `Thread.start()` happens-before any action in started thread; thread actions happen-before `thread.join()` returns
- Final fields initialized in constructor (without leaking `this`) are safely visible without locks!

### Q12. Explain CAS, the ABA Problem, and `LongAdder` vs `AtomicLong`.
- **CAS (`CompareAndSwap` / `cmpxchg`):** Lock-free CPU hardware atomic instruction updating a value only if it still equals expected.
- **ABA Problem:** Value changes `A -> B -> A`; CAS sees `A` and falsely assumes nothing changed (fatal in lock-free linked stacks/pointers!). Fixed with **`AtomicStampedReference`** (pair of `[value, versionStamp]`).
- **`LongAdder` vs `AtomicLong`:** Under 64+ contending threads, `AtomicLong` causes CAS retry storms and CPU cache-line bouncing. `LongAdder` stripes increments across an array of padded `Cell[]` counters (`@Contended`) and sums them only when `.sum()` is called — 10x+ faster for metrics!

### Q13. Explain `synchronized` Monitor Escalation & `Mark Word` in HotSpot.
1. **Uncontended / Thin (Lightweight) Lock:** Thread CASes a pointer to its Lock Record into the object's 8-byte Mark Word.
2. **Contended / Heavyweight (`ObjectMonitor`):** On contention, inflates to a C++ `ObjectMonitor` (WaitSet + EntryList) involving OS mutex/parking.
*(Note: Biased Locking was deprecated in Java 15 and disabled/removed because modern uncontended CAS is faster without safepoint revocations!)*

### Q14. How do `ThreadLocal` memory leaks happen in Thread Pools, and how does Java 25 `ScopedValue` fix them?
Each `Thread` holds a `ThreadLocalMap` whose `Entry` extends `WeakReference<ThreadLocal<?>>`, **but the `value` is a STRONG reference**! In thread pools, threads live forever. If `tl` loses reference or isn't cleaned up, the strong `value` (and huge request objects) leaks until thread death. **Always call `tl.remove()` in `finally`**, OR in Java 25 use **`ScopedValue`**, which is immutable and strictly bounded to a `ScopedValue.where(...).run(...)` lexical block!

### Q15. Explain Java 21–25 Virtual Threads (Project Loom) & Pinning (`JEP 444` / `JEP 491`).
- **How they work:** Virtual threads are lightweight JVM-managed tasks backed by **Delimited Continuations** stored in Heap, scheduled onto a small `ForkJoinPool` of OS Carrier threads (`cores`). When a virtual thread hits blocking I/O (socket, `Lock.lock()`, `Thread.sleep()`), it **unmounts** cleanly so the OS carrier runs other virtual threads!
- **Rules:** **NEVER pool virtual threads** (spawn one per task; throttle downstream DB access with `Semaphore`!).
- **Pinning:** In Java 21, blocking I/O inside `synchronized` pinned the virtual thread to its OS carrier (fix: use `ReentrantLock`). In **Java 24/25 (`JEP 491`)**, `synchronized` monitors unmount cleanly without pinning!

### Q16. `CompletableFuture` `thenApply` vs `thenApplyAsync` & `thenCompose` vs `thenCombine`?
- `thenApply`: Runs on completing thread (or caller thread if already done); `thenApplyAsync`: Dispatches to `ForkJoinPool.commonPool()` (or custom `Executor` — always pass a custom `Executor` for I/O!).
- `thenCompose`: FlatMaps dependent async steps (`Future<A>` -> `A -> Future<B>`).
- `thenCombine`: Joins two **independent** parallel `CompletableFuture`s once both complete.

### Q17. Explain `ThreadPoolExecutor` Sizing Formulas.
- **CPU-Bound:** $N = N_{\text{cores}} + 1$
- **I/O-Bound (Platform Threads):** $N = N_{\text{cores}} \times \left(1 + \frac{\text{Wait Time}}{\text{Compute Time}}\right)$
- **Virtual Threads:** Unbounded creation (1 per request); bound *resources* (DB connections) via `Semaphore` / pool size, NOT threads.
