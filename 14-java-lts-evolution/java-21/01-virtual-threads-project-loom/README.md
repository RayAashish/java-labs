# Java 21 — Virtual Threads / Project Loom (JEP 444)

## ⚡ 2-Minute Revision
- **Problem Solved:** Classic **Platform Threads** map **1:1** to OS threads (~1 MB reserved stack each, expensive OS kernel context switches -> servers cap at a few thousand concurrent blocking threads). Reactive/WebFlux fixed scalability at the cost of unreadable callback chains & broken stack traces.
- **Virtual Threads (`Thread.ofVirtual()` / `Executors.newVirtualThreadPerTaskExecutor()`):** Managed by the **JVM (M:N scheduling)** atop a tiny pool of carrier OS threads (`ForkJoinPool`). When blocking I/O occurs, the virtual thread's stack chunk (**`Continuation`**) **unmounts** onto the Java Heap cleanly, freeing the OS carrier for another task!
- **Throughput Not Latency:** Virtual threads make blocking I/O **cheap** — they do NOT speed up CPU-bound computation (for CPU math, still use `ForkJoinPool` / parallel streams!).
- **Top 3 Production Rules:**
  1. **NEVER pool virtual threads** — spawn one per task (`newVirtualThreadPerTaskExecutor`), discard when done!
  2. Use **`Semaphore`** (not thread pool size) to bound concurrency toward downstream DBs/APIs!
  3. **Pinning Trap (Java 21):** Holding a `synchronized` monitor during blocking I/O pins the carrier OS thread on Java 21 (use `ReentrantLock` there — fixed in **Java 24/25 JEP 491**!).

## 📂 Contents
- [`notes/virtual-threads-loom-deep-dive.md`](notes/virtual-threads-loom-deep-dive.md)
- [`codes/lts/java21/loom/VirtualThreadsDeepDiveDemo.java`](codes/lts/java21/loom/VirtualThreadsDeepDiveDemo.java)
- [`interview/INTERVIEW_QUESTIONS.md`](interview/INTERVIEW_QUESTIONS.md)
