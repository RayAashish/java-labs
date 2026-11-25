# Interview Questions — Java 21 Virtual Threads (Project Loom)

## 🟢 Beginner

### Q1. How do Virtual Threads differ from traditional Platform Threads?
- **Platform Thread:** 1-to-1 wrapper around an OS thread (~1 MB native stack reservation, expensive kernel context switches -> limits concurrent blocking threads to a few thousand).
- **Virtual Thread:** Lightweight user-mode thread managed by the JVM (**M:N scheduling** onto a small pool of OS carrier threads), allowing hundreds of thousands or millions of concurrent I/O tasks with simple blocking code!

### Q2. Can Virtual Threads speed up heavy CPU-bound calculations (e.g., matrix multiplication / video encoding)?
**No.** A JVM still only has as many physical CPU cores as the machine provides. Virtual threads speed up **throughput for blocking I/O workloads** (HTTP calls, DB queries, queues) where OS threads normally sit idle waiting.

---

## 🟡 Intermediate

### Q3. Why should you NEVER put Virtual Threads inside an `Executors.newFixedThreadPool(...)`? What should you use instead to avoid overwhelming a database?
Creating a virtual thread costs negligible memory, so pooling them defeats their lightweight throwaway design and risks stale `ThreadLocal` leakage! To cap concurrency to a downstream database or rate-limited API, use **`java.util.concurrent.Semaphore`** (e.g., `new Semaphore(20)`) inside virtual tasks instead!

### Q4. Why is placing a large buffer or `SimpleDateFormat` in a `ThreadLocal` dangerous with Virtual Threads?
Because you might spawn 500,000 concurrent virtual threads — each gets its own copy of every `ThreadLocal`, causing massive GC pressure or `OutOfMemoryError`. Prefer immutable shared instances or **`ScopedValue`** (finalized Java 25)!

---

## 🔴 Senior

### Q5. What was "Carrier Thread Pinning" in Java 21, and what changed in Java 24/25?
In **Java 21**, blocking while holding an intrinsic **`synchronized`** lock (or inside a native JNI frame) **pinned** the virtual thread to its OS carrier thread, blocking the OS thread instead of unmounting cleanly. Workaround in Java 21: replace `synchronized` around I/O with **`ReentrantLock`** (diagnose via `-Djdk.tracePinnedThreads=full`). **JEP 491 (Java 24/25)** redesigned object monitor locking so virtual threads now unmount cleanly even inside `synchronized` blocks!
