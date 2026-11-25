# Virtual Threads, Continuations & Carrier Thread Mounting
> **Author:** raysi  
> **Package:** `com.raysi.concurrency.phase5_virtual_threads_project_loom`  
> **Module:** Project Loom, M:N Fiber Architecture, `jdk.internal.vm.Continuation`, and ForkJoinPool Carrier Scheduling

---

# 1. Hardware & JVM Architectural Mental Model

For nearly 30 years, Java used a **1:1 Threading Model**: every `java.lang.Thread` corresponded directly to an OS kernel thread (`pthread_create`). This model imposed two severe physical bottlenecks:

1. **Memory Ceiling:** Every OS thread requires a dedicated native stack allocation (default `-Xss1024k` = 1MB). Spawning 4,000 threads consumes 4 GB of RAM solely for empty thread stacks!
2. **Context Switching Overhead:** Schedulers (like Linux CFS) thrash when scheduling thousands of threads. Switching between OS threads incurs 1 to 5 µs of CPU overhead, flushing TLBs and L1/L2 caches.

Consequently, backend architectures were forced into complex, unreadable reactive pipelines (`RxJava`, `Project Reactor`, `CompletableFuture`) just to avoid blocking threads.

**Java 21 Virtual Threads (Project Loom)** re-architects Java concurrency using an **$M:N$ User-Space Fiber Model**: millions of lightweight Virtual Threads are scheduled onto a tiny pool of physical OS **Carrier Threads**.

```
┌────────────────────────────────────────────────────────────────────────┐
│               PROJECT LOOM: VIRTUAL THREAD MOUNTING ARCHITECTURE       │
│                                                                        │
│   1,000,000 VIRTUAL THREADS (Heap Allocated Fibers, ~few hundred bytes)│
│   ┌──────┐ ┌──────┐ ┌──────┐ ┌──────┐ ┌──────┐ ┌──────┐ ┌──────┐       │
│   │ VT 1 │ │ VT 2 │ │ VT 3 │ │ VT 4 │ │ VT 5 │ │ VT 6 │ │...  │       │
│   └───┬──┘ └───┬──┘ └───┬──┘ └──────┘ └──────┘ └──────┘ └──────┘       │
│       │        │        │                                              │
│       │ MOUNT  │ MOUNT  │ MOUNT                                        │
│       ▼        ▼        ▼                                              │
│   ┌────────────────────────────────────────────────────────────────┐   │
│   │       CARRIER THREAD POOL (Standard FIFO ForkJoinPool)         │   │
│   │       Size = Runtime.getRuntime().availableProcessors()        │   │
│   │                                                                │   │
│   │  ┌───────────────────────┐          ┌───────────────────────┐  │   │
│   │  │ Carrier Thread 1 (OS) │          │ Carrier Thread 2 (OS) │  │   │
│   │  └───────────┬───────────┘          └───────────┬───────────┘  │   │
│   └──────────────┼──────────────────────────────────┼──────────────┘   │
│                  │                                  │                  │
│                  ▼                                  ▼                  │
│   ┌────────────────────────────────────────────────────────────────┐   │
│   │                     PHYSICAL CPU CORES                         │   │
│   │                 [Core 0]                 [Core 1]              │   │
│   └────────────────────────────────────────────────────────────────┘   │
│                                                                        │
│   CONTINUATION UNMOUNTING ON BLOCKING I/O:                             │
│   VT 1 calls Socket Read / Thread.sleep(100)                           │
│   1. Continuation.yield() suspends bytecode execution                  │
│   2. Stack frames copied from Carrier Thread to Java HEAP              │
│   3. Carrier Thread 1 is IMMEDIATELY FREE to mount VT 4!               │
│   4. When I/O completes, OS epoll notifies JVM -> VT 1 rescheduled!   │
└────────────────────────────────────────────────────────────────────────┘
```

---

# 2. Under the Hood: JDK Source Code & Bytecode Mechanics

### The Core Engine: `jdk.internal.vm.Continuation`
Under the hood, a Virtual Thread is essentially a `java.lang.Thread` wrapper around a native JVM primitive called a **`Continuation`**:

```java
// java.lang.VirtualThread
package java.lang;

final class VirtualThread extends BaseVirtualThread {
    private final Executor scheduler;           // Default: ForkJoinPool
    private final Continuation cont;            // Suspends & resumes stack
    private volatile int state;                 // NEW, STARTED, RUNNING, PARKING, YIELDING...

    private void runContinuation() {
        cont.run(); // Mounts onto current carrier thread and runs bytecode
    }
}
```

### The Unmounting Mechanism (`Continuation.yield()`)
When a virtual thread executes a blocking operation (e.g. `Socket.getInputStream().read()`, `Thread.sleep()`, or `ReentrantLock.lock()`):
1. The JDK non-blocking I/O layer registers the native file descriptor with the JVM's background **Poller (`epoll` on Linux, `kqueue` on macOS)**.
2. The virtual thread invokes `Continuation.yield(VTHREAD_SCOPE)`:
   - The HotSpot execution engine copies the virtual thread's active call frames off the native carrier thread stack and serializes them into chunked objects on the **Java Heap**.
   - The virtual thread transitions to `PARKED`.
   - The underlying Carrier Thread is released back to the `ForkJoinPool` run queue.
3. When the operating system's network card receives packets, the native `epoll` thread wakes up the JVM, which resubmits the virtual thread to the carrier pool.
4. An idle carrier thread picks up the virtual thread, restores its stack frames from heap to native stack, and resumes execution seamlessly!

---

# 3. Step-by-Step State Transitions & Concurrency Traps

### 1. The Cardinal Anti-Pattern: NEVER POOL VIRTUAL THREADS!
In traditional Java, creating threads was expensive, so engineers pooled them using `ThreadPoolExecutor`.
- **THE TRAP:** Developers attempt `Executors.newFixedThreadPool(100, Thread.ofVirtual().factory())`.
- **Why this is wrong:** Virtual Threads are ephemeral, disposable tasks. They cost virtually nothing to create (~few hundred bytes). Pooling them wastes memory and limits concurrency!
- **The Correct Pattern:** Create a new Virtual Thread per task:
  ```java
  try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
      executor.submit(() -> handleRequest());
  }
  ```

### 2. How to Rate-Limit Downstream Services Without Thread Pools
If you have 50,000 virtual threads making database calls, they will overwhelm a database configured for only 100 concurrent connections.
- **Wrong Solution:** Restrict virtual threads with a pool.
- **Senior Solution:** Use a `java.util.concurrent.Semaphore` or a bounded connection pool (e.g., HikariCP) to guard the resource:
  ```java
  private final Semaphore dbThrottle = new Semaphore(100);

  public void queryDatabase() {
      dbThrottle.acquire();
      try {
          executeJdbc();
      } finally {
          dbThrottle.release();
      }
  }
  ```

### 3. CPU-Bound Tasks Do NOT Benefit from Virtual Threads
Virtual Threads provide near-infinite concurrency **only for I/O-bound tasks** (network requests, disk writes, lock waits). If your workload is CPU-bound (hashing, JSON parsing, video encoding), virtual threads will not make it faster, because physical CPU execution units remain limited by the number of hardware cores!

---

# 4. Comparison & Trade-Off Matrix

| Dimension | Classic Platform Thread | Java 21+ Virtual Thread |
| :--- | :--- | :--- |
| **OS Mapping** | 1:1 with OS Kernel Thread | $M:N$ over Carrier Threads |
| **Stack Memory** | Fixed ~1MB off-heap per thread | Dynamic (~few hundred bytes on Heap)|
| **Max Concurrency** | ~2,000 to 5,000 threads | **1,000,000+ threads** |
| **Context Switch Cost** | 1-5 µs (Kernel mode transition) | ~10-50 ns (User-space frame swap)|
| **Blocking Behavior** | Freezes OS thread on blocking I/O | **Cooperatively unmounts from Carrier**|
| **Best Workload** | Heavy CPU computation | High-throughput blocking I/O (Web/DB)|

---

# 5. Runnable Java Verification Code (`author: raysi`)

The companion lab for this module is located at:
[`PlatformVsVirtualThreads100kBenchmark.java`](../../codes/concurrency/phase5_virtual_threads_project_loom/PlatformVsVirtualThreads100kBenchmark.java)

Run via:
```bash
java --enable-preview -cp target/classes com.raysi.concurrency.phase5_virtual_threads_project_loom.codes.PlatformVsVirtualThreads100kBenchmark
```

---

# 6. Top 8 High-Signal Concurrency Interview Questions & Answers

### Q1: Why do platform threads hit a scalability wall at a few thousand threads?
**Answer:** Two reasons: (1) **Memory footprint:** Each platform thread allocates a native 1MB stack via `-Xss`; 5,000 threads require 5GB of off-heap RAM. (2) **OS Scheduler saturation:** Operating system schedulers context-switch threads by saving CPU registers and invalidating TLB caches, causing severe cache misses when scheduling thousands of threads.

### Q2: What is the relationship between a Virtual Thread and a Carrier Thread?
**Answer:** A Carrier Thread is a standard OS platform thread managed by a ForkJoinPool inside the JVM. A Virtual Thread is a user-mode continuation. When a virtual thread has CPU work to do, the JVM mounts it onto a Carrier Thread. When the virtual thread blocks on I/O, it unmounts from the carrier, allowing the carrier thread to execute other virtual threads.

### Q3: What happens to a Virtual Thread's call stack when it blocks on I/O?
**Answer:** HotSpot's `Continuation.yield()` copies the execution frames from the carrier thread's native stack into variable-sized chunk objects stored on the standard **Java Heap**. When the I/O event completes, the JVM copies the frames back onto an available carrier thread stack and resumes execution.

### Q4: Why should you NEVER pool Virtual Threads?
**Answer:** Pooling was created to amortize the high creation and memory cost of platform threads. Virtual Threads are designed to be ephemeral and lightweight (~1KB). Pooling them introduces unnecessary complexity, limits concurrency, and contradicts the design philosophy of "one thread per task".

### Q5: Can Virtual Threads speed up CPU-bound tasks?
**Answer:** No. CPU-bound tasks are constrained by the physical number of CPU cores and arithmetic logic units (ALUs). Adding 100,000 virtual threads to compute mathematical hashes will only add scheduling overhead without increasing CPU throughput.

### Q6: How does the JVM know when a blocked Virtual Thread's I/O is ready to resume?
**Answer:** OpenJDK re-architected standard `java.net` and `java.nio` socket channels to be virtual-thread-aware. When a virtual thread blocks, its file descriptor is registered with an OS event-notification mechanism (`epoll` on Linux, `kqueue` on macOS, `IOCP` on Windows). When ready, the notification thread resubmits the virtual thread to the carrier pool.

### Q7: How many Carrier Threads does the default scheduler use?
**Answer:** By default, the carrier `ForkJoinPool` is sized to `Runtime.getRuntime().availableProcessors()`. It can be tuned using the JVM flag `-Djdk.virtualThreadScheduler.parallelism=N`.

### Q8: What happens if a Virtual Thread calls `Thread.sleep()`?
**Answer:** In Java 21+, `Thread.sleep()` has been rewritten to integrate directly with `VirtualThread.park()`. Instead of suspending the OS thread, the virtual thread unmounts from its carrier thread and schedules a timer. The carrier thread immediately runs another task.
