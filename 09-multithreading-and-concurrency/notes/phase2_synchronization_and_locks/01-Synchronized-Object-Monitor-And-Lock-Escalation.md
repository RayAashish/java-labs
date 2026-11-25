# Synchronized, Object Monitor & Lock Escalation
> **Author:** raysi  
> **Package:** `com.raysi.concurrency.phase2_synchronization_and_locks`  
> **Module:** JVM HotSpot Object Header, Mark Word Layout, Lock Escalation, and `monitorenter`/`monitorexit`

---

# 1. Hardware & JVM Architectural Mental Model

In early Java versions (1.0 - 1.4), the `synchronized` keyword was infamous for terrible performance because every single synchronization block immediately invoked the OS kernel to acquire a heavyweight POSIX mutex (`pthread_mutex_t`). This forced a full OS context switch (~1-5 microseconds) even when only a single thread ever used the object.

To solve this, HotSpot engineers introduced **Lock Escalation (Lock Invalidation/Inflation)**. The JVM dynamically adjusts the locking mechanism based on observed runtime contention, storing the lock state directly inside the **64-bit Mark Word** of the object header.

```
┌────────────────────────────────────────────────────────────────────────────────────────┐
│                        64-BIT HOTSPOT OBJECT HEADER (MARK WORD)                        │
├──────────────────────────────────────────────────────────┬───────┬──────┬──────────────┤
│                Bit Representation (64 bits)              │ State │ Tag  │ Locking Mode │
├──────────────────────────────────────────────────────────┼───────┼──────┼──────────────┤
│ [unused:25][identity_hashcode:31][unused:1][age:4][0]    │ Normal│  01  │ Unlocked     │
│ [pointer_to_displaced_mark_word_on_thread_stack:62]      │ Thin  │  00  │ Lightweight  │
│ [pointer_to_c++_object_monitor_in_native_heap:62]        │ Heavy │  010 │ Heavyweight  │
│ [pointer_to_marked_object_by_gc:62]                      │ GC    │  011 │ Marked for GC│
└──────────────────────────────────────────────────────────┴───────┴──────┴──────────────┘
```

### The Lock Escalation Flow:
```
  [New Object Instantiated]
             │
             ▼
      ┌──────────────┐
      │   UNLOCKED   │ (Tag 01)
      └──────┬───────┘
             │ Single thread encounters 'synchronized'
             ▼
      ┌──────────────┐
      │ LIGHTWEIGHT  │ (Tag 00: BasicObjectLock pushed on stack, CAS on Mark Word)
      │  (Thin Lock) │
      └──────┬───────┘
             │ Multiple threads contend simultaneously (CAS fails)
             ▼
      ┌──────────────┐
      │ HEAVYWEIGHT  │ (Tag 010: Inflated to native C++ ObjectMonitor,
      │ (OS Mutex)   │  contending threads enqueued in _cxq & parked by kernel)
      └──────────────┘
```

*(Note: Prior to Java 15, "Biased Locking" biased the lock toward the first thread, but it was deprecated in JEP 374 and disabled by default in Java 18+ due to complex revokation STW pauses).*

---

# 2. Under the Hood: JDK Source Code & Bytecode Mechanics

### Bytecode Generation: `monitorenter` and `monitorexit`
Consider this simple method:
```java
public void syncBlock() {
    synchronized (this) {
        doSomething();
    }
}
```
Decompiling with `javap -c -v` reveals:
```bytecode
public void syncBlock();
  Code:
     0: aload_0
     1: dup
     2: astore_1
     3: monitorenter        // 1. Acquire monitor (pushes lock record, updates Mark Word)
     4: invokevirtual #7    // Method doSomething:()V
     7: aload_1
     8: monitorexit         // 2. Normal exit: releases monitor
     9: goto          17
    12: astore_2
    13: aload_1
    14: monitorexit         // 3. Exception exit: guarantees lock is freed if an error occurs!
    15: aload_2
    16: athrow
    17: return
  Exception table:
     from    to  target type
         4     9    12   any   // Guarantees monitorexit on ANY uncaught Throwable!
```

### HotSpot Native C++ `ObjectMonitor` Structure
When a lock is inflated to heavyweight, HotSpot allocates a native C++ object (`src/hotspot/share/runtime/objectMonitor.hpp`):
```c++
class ObjectMonitor {
  volatile markWord _header;       // Displaced mark word
  void*             _owner;        // Pointer to owning thread (or NULL)
  intx              _recursions;   // Recursion depth counter for reentrancy
  ObjectWaiter*     _cxq;          // LIFO Contention Queue (new contenders CAS here)
  ObjectWaiter*     _EntryList;    // FIFO Entry List (threads ready to compete for lock)
  ObjectWaiter*     _WaitSet;      // Singly-linked list of threads that called wait()
}
```
1. Contending threads attempt an initial CAS spin (`Adaptive Spinning`).
2. If spinning fails, they are pushed onto `_cxq` and parked using the OS kernel primitive `pthread_mutex_lock` / `os::PlatformEvent::park()`.
3. When the owner calls `monitorexit`, it unparks a successor thread from `_EntryList` to become the next owner.

---

# 3. Step-by-Step State Transitions & Concurrency Traps

### 1. Lock Reentrancy Mechanics
Why can a thread call a `synchronized` method from inside another `synchronized` method on the same object without deadlocking itself?
- When the current thread encounters `monitorenter`, the JVM checks `_owner`.
- If `_owner == Thread::current()`, the JVM simply increments the `_recursions` counter!
- Each corresponding `monitorexit` decrements `_recursions`.
- The lock is only completely released to other contenders when `_recursions == 0`.

### 2. Can a Lock De-escalate?
In standard HotSpot JVM execution, **lock escalation is one-way**. Once an object's lock has inflated to Heavyweight, it does not downgrade back to Lightweight during normal application execution (it can only be reset during a full STW Garbage Collection cycle if the object monitor is deflated).

### 3. The `wait()` / `notifyAll()` Lost Wakeup Bug
When a thread calls `lock.wait()`:
1. It releases ownership of `ObjectMonitor` (`_owner = NULL`).
2. It pushes itself onto the `_WaitSet` list.
3. It parks itself.
If a producer calls `notify()`, **only one thread is moved from `_WaitSet` to `_EntryList`**. If you have both Producers and Consumers waiting on the same object, `notify()` might wake another Producer who cannot proceed because the buffer is still full, causing all threads to sleep forever (Lost Wakeup / System Hang). **Always use `notifyAll()` or `ReentrantLock` Conditions!**

---

# 4. Comparison & Trade-Off Matrix

| Feature | Lightweight Lock (Thin) | Heavyweight Lock (Fat / Monitor) | `ReentrantLock` |
| :--- | :--- | :--- | :--- |
| **Data Structure** | Stack BasicObjectLock | Native C++ `ObjectMonitor` | Java-level AQS Node Queue |
| **Contention Level** | Zero contention (1 thread) | Multi-thread contention | Low to High contention |
| **CPU Cost** | Single CAS (~5-10 ns) | OS Context Switch (~1-5 µs) | CAS + Parker (~15-50 ns / µs) |
| **Fairness Policy** | Unfair | Unfair (Barging allowed) | Configurable (`new ReentrantLock(true)`) |
| **Timeout Support** | No | No (`synchronized` waits forever)| Yes (`tryLock(timeout)`) |
| **Condition Queues** | 1 (`wait()` / `notify()`) | 1 (`wait()` / `notify()`) | Multiple (`lock.newCondition()`) |

---

# 5. Runnable Java Verification Code (`author: raysi`)

The companion lab for this module is located at:
[`ObjectHeaderMarkWordMonitorDemo.java`](../../codes/concurrency/phase2_synchronization_and_locks/ObjectHeaderMarkWordMonitorDemo.java)

Run via:
```bash
java -cp target/classes com.raysi.concurrency.phase2_synchronization_and_locks.codes.ObjectHeaderMarkWordMonitorDemo
```

---

# 6. Top 8 High-Signal Concurrency Interview Questions & Answers

### Q1: What is stored in the 64-bit Mark Word of a Java object?
**Answer:** The Mark Word contains: (1) Identity Hashcode (31 bits), (2) GC Generational Age (4 bits, max 15), (3) Biased Lock flag (1 bit), (4) Lock State Tag bits (2 bits: 01=Unlocked, 00=Lightweight, 10=Heavyweight, 11=Marked by GC). Under lightweight locking, it holds a pointer to the stack lock record. Under heavyweight locking, it holds a 62-bit pointer to the native C++ `ObjectMonitor`.

### Q2: Why is `synchronized` considered reentrant?
**Answer:** Because the JVM tracks the owning thread in `_owner` and maintains an integer field `_recursions` inside `ObjectMonitor`. If the thread acquiring the lock already owns it, the JVM merely increments `_recursions` and proceeds without blocking, preventing self-deadlock.

### Q3: What is Adaptive Spinning in HotSpot?
**Answer:** Before parking a thread onto the OS mutex queue (which costs a context switch), the JVM allows the thread to execute a busy-spin loop for a few dozen clock cycles, checking if the current lock holder releases the lock. "Adaptive" means the JVM dynamically increases the spin count if recent spins succeeded, or disables spinning if the lock holder is frequently blocked.

### Q4: Why must `wait()` and `notify()` be called strictly within a `synchronized` block?
**Answer:** Calling `wait()` or `notify()` requires access to the object's `ObjectMonitor` structure (`_WaitSet`). If called outside `synchronized`, the thread does not hold the monitor, resulting in `IllegalMonitorStateException`. More importantly, it avoids the **Lost Wakeup Race Condition** between checking the condition predicate and calling `wait()`.

### Q5: What happens if an unhandled Exception is thrown inside a `synchronized` block?
**Answer:** The compiler generates an explicit `Exception Table` in bytecode covering the entire synchronized region. When an exception is thrown, the execution jumps to the handler which executes `monitorexit` before re-throwing the exception (`athrow`). The lock is **never leaked**.

### Q6: Can two threads execute two different `synchronized` methods on the same object simultaneously?
**Answer:** No. Intrinsic locks are per **object instance**, not per method. Both methods require acquiring the monitor of `this`. One thread will acquire it; the other thread transitions to `BLOCKED`.

### Q7: What is Lock Coarsening and Lock Elision?
**Answer:** JIT compiler optimizations:
- **Lock Elision (Escape Analysis):** If the JIT proves an object never escapes the allocating thread (e.g. `StringBuffer sb = new StringBuffer()`), it completely strips out all synchronization bytecode.
- **Lock Coarsening:** If consecutive synchronized blocks operate on the same lock in a loop, the JIT merges them into a single outer synchronized block to eliminate repetitive CAS lock/unlock instructions.

### Q8: What was Biased Locking and why was it removed in modern Java?
**Answer:** Biased Locking assumed an object is only ever accessed by one thread, saving CAS operations by writing the thread ID into the Mark Word. However, revoking the bias when another thread contended required triggering an expensive Stop-The-World (STW) safepoint across all cores. In modern architectures with cheap atomic instructions, the STW revocation cost far outweighed the performance gain, leading to its removal in JEP 374.
