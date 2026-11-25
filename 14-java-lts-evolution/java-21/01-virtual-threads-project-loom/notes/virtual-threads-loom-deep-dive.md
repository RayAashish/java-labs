# Virtual Threads / Project Loom (JEP 444) — Deep Dive

---

## 🟢 Layer 1 — Noob Track: Simple Analogy

Think of a call center:
- **Platform Threads (1:1 OS):** You hire 1 agent (`OS Thread`), assign them a desk, and if the customer puts them on hold for 500 ms waiting on a database check, the **agent sits paralyzed staring at the phone** doing nothing! To handle 10,000 simultaneous calls, you'd need 10,000 desks -> OutOfMemoryError!
- **Virtual Threads (M:N JVM):** You keep only **8 expert agents (`Carrier Threads`)** on 8 CPU cores. Whenever Customer #1 goes on hold (`Socket.read()`, `Thread.sleep()`, `BlockingQueue.take()`), the agent places a sticky note (`Continuation` stack frame) into memory and immediately answers Customer #2, #3, #4... #10,000! When the DB replies for Customer #1, ANY free agent picks up the sticky note cleanly!

---

## 🟡 Layer 2 — Practitioner Track: APIs, Concurrency Control & Traps

### 1. Creating Virtual Threads
```java
// Option A: Fire-and-wait Executor (Java 19+ ExecutorService extends AutoCloseable!)
try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
    executor.submit(() -> callExternalHttpApi());
} // Auto-closes & waits for all submitted tasks!

// Option B: Direct Builder
Thread vt = Thread.ofVirtual().name("worker-", 0).start(() -> doIo());
```

### 2. Three Classic Virtual Thread Anti-Patterns
1. **❌ Pooling Virtual Threads (`Executors.newFixedThreadPool(50, Thread.ofVirtual().factory())`):**
   - Virtual threads cost ~a few hundred bytes (NOT MBs). Pooling them makes zero sense and accidentally creates deadlocks if code relies on thread-local caching across unrelated tasks!
   - **✅ Fix:** Always create a fresh virtual thread per task; if you need backpressure (max 20 concurrent Postgres queries), use a **`Semaphore(20)`**!
2. **❌ Large `ThreadLocal` Caches (`SimpleDateFormat`, byte arrays, DB connections):**
   - With 200 platform threads, caching a 100 KB buffer in a `ThreadLocal` wastes 20 MB. With 500,000 virtual threads, it allocates **50 GB -> OOM**!
   - **✅ Fix:** Migrate per-request context (`traceId`, `Tenant`) to **`ScopedValue`** (finalized Java 25)!
3. **❌ Java 21 Pinning (`synchronized` + Blocking I/O):**
   - In **Java 21**, native `synchronized` frames stay tied to the OS thread — sleeping or waiting for a socket inside `synchronized` **pins** the carrier thread (`-Djdk.tracePinnedThreads=full`).
   - **✅ Fix on Java 21:** Replace `synchronized` around I/O with **`ReentrantLock`**. *(Good news: **JDK 24/25 JEP 491** upgraded the JVM monitor implementation so `synchronized` unmounts cleanly without pinning too!)*

---

## 🔴 Layer 3 — Senior / JVM Architect Track: Inside `Continuation`

- **Scheduler:** `ForkJoinPool` in FIFO async mode (`jdk.virtualThreadScheduler.parallelism` defaults to CPU core count). Virtual threads are ALWAYS **daemon threads** (`setDaemon(false)` throws `IllegalArgumentException`) and ignore `setPriority()`.
- **How Unmounting Works:** When a JDK blocking point (`LockSupport.park()` underneath `Thread.sleep`, `ReentrantLock`, `SocketInputStream`, `CompletableFuture.get`) executes on a virtual thread, `Continuation.yield()` copies only the **used** top stack frames from the carrier stack into a heap object (`StackChunk`) on the Java heap and resumes the carrier's scheduler loop. Zero OS `pthread` context switches!
- **Note on CPU-bound Code:** Virtual threads still only have as many CPU cores as physical hardware! Running `while (true) { hash(); }` on 10,000 virtual threads yields **no speedup** (and never calls `yield` unless preempted externally at IO/locks) — keep heavy CPU crunching on bounded `ForkJoinPool` / parallel streams.
