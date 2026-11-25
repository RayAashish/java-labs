# Deadlock, Livelock, Starvation & Thread Dumps
> **Author:** raysi  
> **Package:** `com.raysi.concurrency.phase2_synchronization_and_locks`  
> **Module:** Coffman's Conditions, Lock Ordering, Livelock Mechanics, Thread Dumps, and Production Diagnostics

---

# 1. Hardware & JVM Architectural Mental Model

Concurrency bugs fall into two broad classifications: **Safety Bugs** (Race Conditions where data becomes corrupted) and **Liveness Bugs** (Deadlocks/Livelocks where threads make zero progress).

```
┌────────────────────────────────────────────────────────────────────────┐
│                      THE CIRCULAR WAIT DEADLOCK GRAPH                  │
│                                                                        │
│                ┌───────────────────────────┐                           │
│                │  RESOURCE A (Account 101) │ ◄────────────┐            │
│                └─────────────┬─────────────┘              │            │
│                              │                            │            │
│                    Holds Lock│                 Waiting For│            │
│                              ▼                            │            │
│                ┌───────────────────────────┐              │            │
│                │   THREAD 1 (Transfer A-B) │              │            │
│                └─────────────┬─────────────┘              │            │
│                              │                            │            │
│                   Waiting For│                            │            │
│                              ▼                            │            │
│                ┌───────────────────────────┐              │            │
│                │  RESOURCE B (Account 202) │              │            │
│                └─────────────┬─────────────┘              │            │
│                              │                            │            │
│                    Holds Lock│                            │            │
│                              ▼                            │            │
│                ┌───────────────────────────┐              │            │
│                │   THREAD 2 (Transfer B-A) │ ─────────────┘            │
│                └───────────────────────────┘                           │
└────────────────────────────────────────────────────────────────────────┘
```

### Coffman's 4 Conditions for Deadlock:
A deadlock can occur **if and only if all four** of the following conditions hold simultaneously:
1. **Mutual Exclusion:** At least one resource must be held in a non-shareable mode.
2. **Hold and Wait:** A thread must be holding at least one resource while requesting another held by another thread.
3. **No Preemption:** Resources cannot be forcibly seized; only the holding thread can release them.
4. **Circular Wait:** A closed chain of threads exists such that each thread holds a resource needed by the next.

**The Golden Engineering Rule:** To prevent deadlocks, eliminate **Circular Wait** by enforcing a **Global Canonical Lock Ordering** (e.g. acquire locks in order of their database ID or memory hashcode).

---

# 2. Under the Hood: JDK Source Code & Bytecode Mechanics

### Thread Dump Anatomy in HotSpot
When a deadlock occurs, running `jstack <PID>` or `jcmd <PID> Thread.print` analyzes the internal HotSpot `ObjectMonitor` and AQS queues:

```text
"Deadlock-Thread-1" #24 prio=5 os_prio=31 tid=0x00007f9c10 nid=0x6703 waiting for monitor entry [0x0000700001]
   java.lang.Thread.State: BLOCKED (on object monitor)
        at com.raysi.concurrency.codes.DeadlockDemo.lambda$0(DeadlockDemo.java:55)
        - waiting to lock <0x000000076ad01108> (a java.lang.Object)
        - locked <0x000000076ad010f8> (a java.lang.Object)

"Deadlock-Thread-2" #25 prio=5 os_prio=31 tid=0x00007f9c20 nid=0x6803 waiting for monitor entry [0x0000700002]
   java.lang.Thread.State: BLOCKED (on object monitor)
        at com.raysi.concurrency.codes.DeadlockDemo.lambda$1(DeadlockDemo.java:65)
        - waiting to lock <0x000000076ad010f8> (a java.lang.Object)
        - locked <0x000000076ad01108> (a java.lang.Object)

Found 1 deadlock.
```

### `ThreadMXBean` Programmatic Diagnostics
Inside `java.lang.management.ThreadMXBean`:
```java
ThreadMXBean bean = ManagementFactory.getThreadMXBean();
long[] deadlockedIds = bean.findDeadlockedThreads(); // Inspects monitors AND ownable synchronizers
```
`findDeadlockedThreads()` traverses the wait-for graph looking for cycles, making it suitable for automated health probes and test harnesses.

---

# 3. Step-by-Step State Transitions & Concurrency Traps

### 1. Deadlock vs. Livelock vs. Starvation
- **Deadlock:** Threads are permanently suspended (`BLOCKED` or `WAITING`). CPU utilization is **0%**. No progress is ever made without killing the process.
- **Livelock:** Threads are constantly running on the CPU (`RUNNABLE`), actively responding to each other by releasing and re-acquiring locks, but making **zero forward business progress**. CPU utilization is **100%**!
  - *Analogy:* Two polite people in a hallway stepping side-to-side in unison, continuously blocking each other.
  - *Fix:* Introduce **Randomized Jitter / Exponential Backoff** so threads don't retry in phase.
- **Starvation:** A thread is ready to run, but greedy high-priority threads or unfair locks repeatedly acquire the resource, leaving the victim waiting indefinitely.

### 2. Lock Ordering Solution for Bank Transfers
```java
// Transfer money between two accounts safely:
public void transfer(Account from, Account to, BigDecimal amount) {
    Account firstLock  = from.getId() < to.getId() ? from : to;
    Account secondLock = from.getId() < to.getId() ? to : from;

    synchronized (firstLock) {
        synchronized (secondLock) {
            from.debit(amount);
            to.credit(amount);
        }
    }
}
```
Because all threads acquire the smaller ID first, a circular wait chain is mathematically impossible.

---

# 4. Comparison & Trade-Off Matrix

| Dimension | Deadlock | Livelock | Starvation |
| :--- | :--- | :--- | :--- |
| **Thread State** | `BLOCKED` or `WAITING` | `RUNNABLE` (Active Loop) | `BLOCKED` or `WAITING` |
| **CPU Utilization**| 0% on affected threads | **100% on affected cores** | Normal (other threads busy) |
| **Root Cause** | Circular lock acquisition | Symmetrical backoff/retry | Greedy threads / Unfair lock |
| **Detection Method**| `jstack`, `ThreadMXBean` | Profilers (high CPU, no progress)| Latency percentiles ($p99$) |
| **Standard Fix** | Strict Lock Ordering, `tryLock`| Randomized Jitter backoff | Fair queues, Priority Aging |

---

# 5. Runnable Java Verification Code (`author: raysi`)

The companion lab for this module is located at:
[`DeadlockDetectionAndPreventionDemo.java`](../../codes/concurrency/phase2_synchronization_and_locks/DeadlockDetectionAndPreventionDemo.java)

Run via:
```bash
java -cp target/classes com.raysi.concurrency.phase2_synchronization_and_locks.codes.DeadlockDetectionAndPreventionDemo
```

---

# 6. Top 8 High-Signal Concurrency Interview Questions & Answers

### Q1: What are Coffman's 4 conditions and how do you break them?
**Answer:** The conditions are Mutual Exclusion, Hold and Wait, No Preemption, and Circular Wait. You break Circular Wait by enforcing global resource ordering. You break Hold and Wait by acquiring all locks simultaneously or using `tryLock()`. You break No Preemption by releasing locks when a timeout expires.

### Q2: What is the difference between `findDeadlockedThreads()` and `findMonitorDeadlockedThreads()` in `ThreadMXBean`?
**Answer:** `findMonitorDeadlockedThreads()` only detects deadlocks caused by intrinsic `synchronized` object monitors. `findDeadlockedThreads()` detects both intrinsic monitors and explicit `java.util.concurrent.locks` (AQS synchronizers).

### Q3: How do you identify a thread pool starvation deadlock?
**Answer:** Thread pool starvation occurs when tasks running inside a thread pool submit sub-tasks to the **same** thread pool and synchronously block waiting for their completion (`future.get()`). If all pool threads are occupied waiting for tasks sitting in the queue, no thread is free to execute the queued tasks. Diagnosis: Thread dump shows all worker threads in `WAITING` at `Future.get()`. Remedy: Use separate dedicated thread pools for parent and child tasks, or use Java 21 Virtual Threads.

### Q4: How does randomized exponential backoff resolve a Livelock?
**Answer:** When two threads collide and both drop their locks to retry, adding a randomized delay (e.g., $10\text{ ms} + \text{random}(0, 20\text{ ms})$) breaks the symmetry. One thread acquires the locks and finishes before the other thread wakes up to retry.

### Q5: Can a deadlock occur with only one thread?
**Answer:** Yes, if a thread attempts to acquire a non-reentrant lock (such as `StampedLock` or a custom non-reentrant mutex) while already holding it. The thread will wait on itself forever.

### Q6: How do you interpret a `jstack` output when an application freezes?
**Answer:**
1. Check the summary at the bottom for `Found X deadlock`.
2. Search for threads in the `BLOCKED` state.
3. Trace the lock address: find the thread holding `locked <0x0000000X>` and see what it is doing (often waiting on a slow database call or network socket without a timeout).

### Q7: Why should you avoid synchronizing on String literals or boxed Primitives (`Integer`)?
**Answer:** String literals are interned in the JVM String Pool (`Metaspace/Heap`). Boxed integers between -128 and 127 are cached by `IntegerCache`. Synchronizing on `"LOCK"` or `Integer.valueOf(1)` means unrelated third-party libraries or classes in the same JVM might synchronize on the exact same instance, causing mysterious cross-application deadlocks. Always synchronize on a `new Object()`.

### Q8: What is Lock Contention Profiling in production?
**Answer:** Contention profiling tracks time threads spend waiting to acquire locks. HotSpot provides Java Flight Recorder (JFR) event `jdk.JavaMonitorEnter` and `jdk.ThreadPark`. In production, APM tools alert when `lock_wait_time` exceeds threshold limits, highlighting high-contention synchronization bottlenecks.
