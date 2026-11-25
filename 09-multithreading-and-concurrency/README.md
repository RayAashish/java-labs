# 09. Multithreading, Concurrency, JMM & Virtual Threads

> **Learning Method:** Start at **🟢 Noob Track** (creating threads & seeing why `count++` races), move through **🟡 Practitioner Track** (`ExecutorService`, `synchronized` vs `ReentrantLock`, `CompletableFuture`, `ConcurrentHashMap`), and master **🔴 Senior Track** (Java Memory Model happens-before, CAS/ABA, AQS, `LongAdder`, Thread Pool Sizing Math, Java 21–25 Virtual Threads, `ScopedValue` & Structured Concurrency).

---

## ⚡ 2-Minute Rapid Revision Card

- **`Thread` vs `Runnable` vs `Callable`:** Prefer `Runnable` / `Callable<V>` submitted to an `ExecutorService` or Virtual Thread — don't subclass `Thread` (keeps inheritance free and decouples task from execution!).
- **`volatile` ≠ Thread-Safe Counter:** `volatile` guarantees **visibility** & prevents instruction reordering (Memory Barrier), **NOT atomicity** (`count++` is read-modify-write -> lost updates!). Use `AtomicInteger` (CAS) or `LongAdder` (striped cells under high contention).
- **`sleep()` vs `wait()`:** `Thread.sleep(ms)` **keeps holding any monitor lock**; `obj.wait()` **releases the monitor lock** and must be called inside `synchronized(obj)` inside a `while` loop (guards against spurious wakeups!).
- **Production `ThreadPoolExecutor`:** Never use unbounded `Executors.newFixedThreadPool` blindly (`LinkedBlockingQueue` of `Integer.MAX_VALUE` -> OOM!). Use bounded `ArrayBlockingQueue` + `CallerRunsPolicy`.
  - CPU-bound: $N_{\text{cores}} + 1$
  - I/O-bound (Platform Threads): $N_{\text{cores}} \times (1 + W / C)$
- **Java 21–25 Virtual Threads & `ScopedValue`:** **Never pool Virtual Threads!** Create one per task (`Executors.newVirtualThreadPerTaskExecutor()`). Prefer **`ScopedValue`** (finalized in Java 25) over `ThreadLocal` to avoid thread-pool memory leaks & mutable inherited state. (And on Java 24/25 `JEP 491`, `synchronized` no longer pins virtual threads to carrier OS threads!)

---

## 📂 Contents

| Phase | Level | Guide & Labs |
|---|---|---|
| **Phase 0 (Start Here)** | 🟢 Noob | [`notes/00-beginner-threads-intuition.md`](notes/00-beginner-threads-intuition.md) + `concurrency.phase0_beginner.BeginnerThreadAndRaceConditionDemo` |
| **Phase 1** | 🟡/🔴 | Thread Lifecycle, OS Mapping, JMM & `volatile` ([`notes/phase1_threads_and_memory_model/`](notes/phase1_threads_and_memory_model/)) |
| **Phase 2** | 🟡/🔴 | `synchronized` Mark Word Monitors, `ReentrantLock`, AQS, `StampedLock`, Deadlocks ([`notes/phase2_synchronization_and_locks/`](notes/phase2_synchronization_and_locks/)) |
| **Phase 3** | 🔴 Senior | Lock-Free CAS, ABA Problem, `LongAdder`, Latches/Barriers/Semaphores, `ThreadLocal` Leaks ([`notes/phase3_lockfree_atomics_and_synchronizers/`](notes/phase3_lockfree_atomics_and_synchronizers/)) |
| **Phase 4** | 🔴 Senior | `ThreadPoolExecutor` Sizing, `ForkJoinPool`, `CompletableFuture` Pipelines ([`notes/phase4_executors_and_completablefuture/`](notes/phase4_executors_and_completablefuture/)) |
| **Phase 5** | 🔴 Modern | Java 21–25 Virtual Threads, Pinning, `ScopedValue` & `StructuredTaskScope` ([`notes/phase5_virtual_threads_project_loom/`](notes/phase5_virtual_threads_project_loom/)) |
| **Interview** | 🎯 All | [`interview/INTERVIEW_QUESTIONS.md`](interview/INTERVIEW_QUESTIONS.md) |

---

## 🚀 Compile & Run

```bash
# Standard JDK 25 compile (includes preview StructuredTaskScope demo)
javac --enable-preview --release 25 -d out $(find 09-multithreading-and-concurrency/codes -name "*.java")

java -cp out concurrency.phase0_beginner.BeginnerThreadAndRaceConditionDemo
java -cp out concurrency.phase1_threads_and_memory_model.VolatileCpuCacheVisibilityBugDemo
java -cp out concurrency.phase2_synchronization_and_locks.DeadlockDetectionAndPreventionDemo
java -cp out concurrency.phase3_lockfree_atomics_and_synchronizers.LongAdderHighContentionCounterDemo
java -cp out concurrency.phase4_executors_and_completablefuture.CompletableFutureMicroserviceAggregator
java -cp out concurrency.phase5_virtual_threads_project_loom.PlatformVsVirtualThreads100kBenchmark
java -cp out concurrency.phase5_virtual_threads_project_loom.ScopedValueDemo
java --enable-preview -cp out concurrency.phase5_virtual_threads_project_loom.StructuredTaskScopeShutdownOnFailureDemo
```
