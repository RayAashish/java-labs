# Thread Lifecycle & OS Kernel Mapping
> **Author:** raysi  
> **Package:** `com.raysi.concurrency.phase1_threads_and_memory_model`  
> **Module:** OS Threads, 1:1 Mapping, HotSpot `java.lang.Thread.State`, and Context Switch Mechanics

---

# 1. Hardware & JVM Architectural Mental Model

To understand multithreading without confusion, one must distinguish between the operating system's execution abstractions and the JVM's virtual abstractions:

### Junior Mental Model: Process vs. Thread
- **Process (Heavyweight):** An isolated execution container provided by the OS kernel. It owns an independent virtual memory address space (Page Tables), file descriptors, network sockets, and security tokens. Inter-Process Communication (IPC) requires expensive OS kernel pipes, shared memory, or sockets.
- **Thread (Lightweight):** The smallest schedulable unit of CPU execution inside a process. Threads of the same process share the **Heap**, **Metaspace**, and open file descriptors, but each thread possesses its own private **Java Thread Stack** (call frames, local primitives, object reference variables) and **Program Counter (PC) Register**.

```
┌────────────────────────────────────────────────────────────────────────┐
│                          OPERATING SYSTEM PROCESS                      │
│                                                                        │
│   ┌────────────────────────────────────────────────────────────────┐   │
│   │               SHARED MEMORY SPACE: HEAP & METASPACE            │   │
│   │             (Objects, Static Fields, Class Metadata)           │   │
│   └────────────────────────────────────────────────────────────────┘   │
│                                                                        │
│    ┌─────────────────────────┐                ┌─────────────────────┐  │
│    │     THREAD 1 STACK      │                │   THREAD 2 STACK    │  │
│    │  ┌───────────────────┐  │                │  ┌───────────────┐  │  │
│    │  │ Frame 2: bar()    │  │                │  │Frame 1: run() │  │  │
│    │  ├───────────────────┤  │                │  └───────────────┘  │  │
│    │  │ Frame 1: foo()    │  │                │  [PC Register: 0x14]│  │
│    │  └───────────────────┘  │                └──────────▲──────────┘  │
│    │  [PC Register: 0x8A]    │                           │             │
│    └────────────▲────────────┘                           │             │
└─────────────────┼────────────────────────────────────────┼─────────────┘
                  │                                        │
══════════════════╪════════════════════════════════════════╪══════════════ [User Space / Kernel Boundary]
                  │                                        │
┌─────────────────┼────────────────────────────────────────┼─────────────┐
│                 ▼                                        ▼             │
│       OS Kernel Thread (kthread 1024)          OS Kernel Thread (kthread 1025)
│       [1:1 Mapping via pthread_create]         [Managed by OS Scheduler]
│                                                                        │
│                 ┌────────────────────────────────────────┐             │
│                 │      OS CPU SCHEDULER (CFS / Mach)     │             │
│                 └──────────────────┬─────────────────────┘             │
└────────────────────────────────────┼───────────────────────────────────┘
                                     ▼
                ┌────────────────────────────────────────┐
                │   PHYSICAL CPU CORES (Core 0, Core 1)  │
                └────────────────────────────────────────┘
```

### The HotSpot 1:1 Threading Model
Standard Java (prior to Java 21 Virtual Threads) implements a **1:1 Threading Model**. When you call `new Thread().start()`, the HotSpot JVM makes a direct system call (`pthread_create` on POSIX/Linux/macOS or `CreateThread` on Windows) via native C++ code (`os::create_thread`).
- Every single Java platform thread corresponds directly to an OS kernel thread.
- The OS Kernel Scheduler allocates CPU time slices (quanta) using algorithms like the Linux Completely Fair Scheduler (CFS).
- **The Context Switch Penalty:** When the CPU switches from Thread A to Thread B, it must:
  1. Save CPU registers (SP, PC, general purpose) into Thread A's Thread Control Block (TCB).
  2. Load Thread B's registers.
  3. Flush and invalidate the Translation Lookaside Buffer (TLB) and L1/L2 CPU caches (causing cache misses).
  4. Average cost: **1 to 5 microseconds (~10,000 CPU cycles lost)**!

---

# 2. Under the Hood: JDK Source Code & Bytecode Mechanics

### JVM Thread States vs. OS Thread States
The HotSpot JVM exposes 6 states via the `java.lang.Thread.State` enum. These 6 states map to the underlying OS states:

```
  JVM STATE                                   OS KERNEL STATE
 ┌──────────┐                                ┌───────────────┐
 │   NEW    │                                │ (Unregistered)│
 └────┬─────┘                                └───────┬───────┘
      │ target.start()                               │ sys_clone / pthread_create
      ▼                                              ▼
 ┌──────────┐                                ┌───────────────┐
 │          │ ◄────────────────────────────► │     READY     │ (In Run Queue)
 │ RUNNABLE │                                ├───────────────┤
 │          │                                │    RUNNING    │ (Active on Core)
 └────┬─────┘                                └───────┬───────┘
      │                                              │
      ├──────────────────────┬───────────────────────┤
      │ synchronized         │ wait(), join(),       │ sleep(ms), wait(ms),
      │ contention           │ LockSupport.park()    │ LockSupport.parkNanos()
      ▼                      ▼                       ▼
 ┌──────────┐           ┌──────────┐            ┌───────────────┐
 │ BLOCKED  │           │ WAITING  │            │ TIMED_WAITING │
 └────┬─────┘           └────┬─────┘            └───────┬───────┘
      │                      │                          │
      └──────────────────────┼──────────────────────────┘
                             ▼
                      ┌──────────────┐
                      │  TERMINATED  │ (Dying / Zombie / Reaped)
                      └──────────────┘
```

### HotSpot Native Source Code Inspection
When `thread.start()` executes:
```c++
// hotspot/src/share/vm/runtime/thread.cpp
void Thread::start(Thread* thread) {
  trace("start", thread);
  os::start_thread(thread); // Calls native OS wrapper
}
```
On Linux, `os::create_thread` calls:
```c
pthread_create(&tid, &attr, &thread_native_entry, thread);
```
Each thread receives a native memory stack allocation (default `-Xss1024k` = 1MB per thread). If you spin up 4,000 platform threads, that consumes $4{,}000 \times 1\text{ MB} = 4\text{ GB}$ of RAM **just for thread call stacks**, excluding the Java Heap!

---

# 3. Step-by-Step State Transitions & Concurrency Traps

### 1. `RUNNABLE` Does Not Mean "Running"
A common junior misconception: *"If a thread state is `RUNNABLE`, it is executing code on the CPU."*
- **Reality:** In Java, `RUNNABLE` means the thread is **eligible** to run. It could be actively crunching numbers on Core 0, or it could be sitting idle in the OS Run Queue waiting for a CPU time slice because the host CPU is pegged at 100%.

### 2. The `BLOCKED` vs. `WAITING` Confusion
- `BLOCKED`: A thread is waiting specifically to enter or re-enter a `synchronized` block/method because another thread currently holds that object's intrinsic monitor.
- `WAITING`: A thread is waiting voluntarily due to calling `Object.wait()`, `Thread.join()`, or `LockSupport.park()`. **Waiting for a `java.util.concurrent.locks.ReentrantLock` places the thread in `WAITING` or `TIMED_WAITING`, NOT `BLOCKED`**, because `ReentrantLock` uses `LockSupport.park()`!

### 3. The `InterruptedException` Clearing Trap
When a thread is blocked in `Thread.sleep()`, `Object.wait()`, or `Thread.join()`, another thread can invoke `target.interrupt()`.
- The JVM aborts the wait and throws `InterruptedException`.
- **CRITICAL TRAP:** When `InterruptedException` is thrown, the JVM **automatically clears the thread's interrupt status bit back to `false`**!
- If your catch block swallows it (`catch (InterruptedException ignored) {}`), upstream callers will never know that shutdown was requested. Always restore the flag:
  ```java
  catch (InterruptedException e) {
      Thread.currentThread().interrupt(); // Restore flag!
      // Handle graceful shutdown
  }
  ```

---

# 4. Comparison & Trade-Off Matrix

| Metric / Dimension | `Thread.sleep(ms)` | `Object.wait(ms)` | `Thread.yield()` | `LockSupport.park()` |
| :--- | :--- | :--- | :--- | :--- |
| **Lock Ownership** | Keeps holding all locks | **Releases** the synchronized monitor | Keeps holding all locks | Keeps holding all locks |
| **Target Mechanism** | OS Timer / Clocks | Monitor wait-set of object | OS Scheduler hint | Low-level thread unpark permit |
| **Target State** | `TIMED_WAITING` | `WAITING` or `TIMED_WAITING` | `RUNNABLE` | `WAITING` or `TIMED_WAITING` |
| **Spurious Wakeup?** | No | **Yes** (must check in `while`) | N/A | **Yes** (must verify condition) |
| **Interruption Handling**| Throws `InterruptedException` | Throws `InterruptedException` | Ignores interrupt flag | Returns cleanly, flag is `true` |

---

# 5. Runnable Java Verification Code (`author: raysi`)

The companion lab for this module is located at:
[`ThreadStatesLifecycleInspector.java`](../../codes/concurrency/phase1_threads_and_memory_model/ThreadStatesLifecycleInspector.java)

Run via:
```bash
java -cp target/classes com.raysi.concurrency.phase1_threads_and_memory_model.codes.ThreadStatesLifecycleInspector
```

---

# 6. Top 8 High-Signal Concurrency Interview Questions & Answers

### Q1: What is the exact difference between `Thread.start()` and `Thread.run()`?
**Answer:** Calling `run()` simply executes the method synchronously on the **current calling thread** like any regular Java method; no new OS thread is created. Calling `start()` makes a native JNI call to the OS kernel (`pthread_create`), allocates an OS Thread Control Block and stack space, transitions the JVM state to `RUNNABLE`, and schedules `run()` asynchronously on the newly spawned OS thread.

### Q2: Why does `Thread.sleep()` keep holding locks while `Object.wait()` releases them?
**Answer:** `sleep()` is an OS-level pause for the current execution flow; it has no concept of synchronization context, so releasing locks could violate invariants that other threads might observe mid-computation. Conversely, `wait()` is an intrinsic monitor coordination primitive: its sole purpose is to say *"I cannot proceed until a condition is true; I release the monitor lock so another thread can enter, modify state, and call `notify()`."*

### Q3: Why is `Thread.stop()` deprecated and never permitted in production?
**Answer:** `Thread.stop()` immediately halts a thread by throwing an asynchronous `ThreadDeath` error up the stack. This unlocks all acquired intrinsic monitors unconditionally, leaving data structures in partially mutated, corrupted, inconsistent states (e.g., half-inserted linked nodes). Java requires **cooperative cancellation** via `interrupt()`.

### Q4: How does HotSpot map a Java thread to an OS thread on Linux?
**Answer:** HotSpot uses a 1:1 mapping model via the POSIX threads (`pthreads`) library. Calling `Thread.start()` invokes `JVM_StartThread` in C++, which invokes `os::create_thread()`, issuing the `clone(2)` system call with flags `CLONE_VM | CLONE_FS | CLONE_FILES | CLONE_SIGHAND`.

### Q5: What is the difference between a User Thread and a Daemon Thread?
**Answer:** The JVM process terminates when **all User Threads** (non-daemon threads) finish execution. Daemon threads are background housekeeping threads (e.g., GC threads, finalizers). When the last user thread terminates, the JVM exits immediately—**daemon thread `finally` blocks are NOT guaranteed to execute**, and unwritten buffers may be dropped.

### Q6: How do you take and inspect a Thread Dump in production?
**Answer:** Using JDK CLI tools:
1. `jcmd <PID> Thread.print` or `jstack -l <PID>`
2. Inspect the output for threads stuck in `BLOCKED (on object monitor)` or `WAITING (parking)`. Look at the bottom of the dump for `Found one Java-level deadlock:`.

### Q7: If a thread's state is `BLOCKED`, what does that tell you about your architecture?
**Answer:** It proves that multiple threads are contending for an intrinsic monitor (`synchronized`). It indicates a synchronization bottleneck where threads are queuing up, burning context switch cycles instead of doing productive work. Remedy: replace with finer-grained locks, `ConcurrentHashMap`, `LongAdder`, or lock-free atomics.

### Q8: What is Spurious Wakeup and how must you protect against it?
**Answer:** An OS thread in a waiting state can wake up without any `notify()` or `signal()` call due to kernel interrupt signals or race conditions. Therefore, you must **NEVER check a waiting condition inside an `if` statement**. Always wrap it inside a `while` loop:
```java
synchronized (lock) {
    while (!conditionSatisfied) {
        lock.wait(); // Re-evaluates condition upon waking!
    }
}
```
