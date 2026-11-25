# Compare-And-Swap (CAS), Lock-Free Atomics & The ABA Problem
> **Author:** raysi  
> **Package:** `com.raysi.concurrency.phase3_lockfree_atomics_and_synchronizers`  
> **Module:** CPU Hardware Instruction `lock cmpxchg`, Unsafe / VarHandle, CAS Loops, and `AtomicStampedReference`

---

# 1. Hardware & JVM Architectural Mental Model

Traditional locking (such as `synchronized` or `ReentrantLock`) is **pessimistic**: it assumes that contention will occur, so it suspends competing threads via OS kernel context switches. This incurs a massive penalty (~1-5 µs).

Lock-free programming is **optimistic**: it assumes collisions are infrequent. Threads execute atomic operations without blocking by leveraging dedicated CPU hardware instructions known as **Compare-And-Swap (CAS)**.

```
┌────────────────────────────────────────────────────────────────────────┐
│                     HARDWARE CAS EXECUTION CYCLE                       │
│                                                                        │
│                ┌───────────────────────────────────┐                   │
│                │  Thread reads Current Value: E=10 │                   │
│                └─────────────────┬─────────────────┘                   │
│                                  │                                     │
│                ┌─────────────────▼─────────────────┐                   │
│                │ Calculates New Value: V = E + 1=11│                   │
│                └─────────────────┬─────────────────┘                   │
│                                  │                                     │
│                ┌─────────────────▼─────────────────┐                   │
│                │ CPU Instruction: 'lock cmpxchg'   │                   │
│                │ Compares Mem[Addr] with E:        │                   │
│                └─────────────────┬─────────────────┘                   │
│                                  │                                     │
│                  Is Mem[Addr] == E (still 10)?                         │
│                         /              \                               │
│                   YES  /                \  NO                          │
│                       ▼                  ▼                             │
│       ┌──────────────────────┐    ┌──────────────────────┐             │
│       │ Atomically write 11  │    │ CAS Fails!           │             │
│       │ Return TRUE          │    │ Reload Mem[Addr]     │             │
│       │ (SUCCESS)            │    │ (Retry in loop)      │             │
│       └──────────────────────┘    └──────────────────────┘             │
└────────────────────────────────────────────────────────────────────────┘
```

### The Hardware Primitive: `lock cmpxchg`
On x86/x64 architectures, the CAS operation maps directly to the assembly instruction:
```assembly
lock cmpxchg [rdx], rcx
```
The `lock` prefix locks the shared memory bus or asserts cache-line ownership via the MESI protocol, guaranteeing that the read-compare-write operation occurs as an **indivisible single clock cycle transaction** across all CPU cores.

---

# 2. Under the Hood: JDK Source Code & Bytecode Mechanics

### Anatomy of `AtomicInteger`
Inside OpenJDK `java.util.concurrent.atomic.AtomicInteger`:
```java
public class AtomicInteger extends Number implements java.io.Serializable {
    private static final jdk.internal.misc.Unsafe U = jdk.internal.misc.Unsafe.getUnsafe();
    private static final long VALUE = U.objectFieldOffset(AtomicInteger.class, "value");

    private volatile int value; // Memory visibility guarantee

    public final int incrementAndGet() {
        return U.getAndAddInt(this, VALUE, 1) + 1;
    }
}
```

### The Unsafe Spin Loop (`getAndAddInt`):
```c++
// hotspot/src/share/vm/prims/unsafe.cpp
public final int getAndAddInt(Object o, long offset, int delta) {
    int v;
    do {
        v = getIntVolatile(o, offset); // 1. Read current memory value
    } while (!weakCompareAndSetInt(o, offset, v, v + delta)); // 2. Hardware CAS retry loop!
    return v;
}
```
If 10 threads contend simultaneously, one thread succeeds instantly, while 9 threads retry the loop until they succeed. Because no thread is suspended into the OS kernel, throughput under low-to-medium contention is orders of magnitude higher than mutual exclusion locks.

---

# 3. Step-by-Step State Transitions & Concurrency Traps

### 1. The CAS Retry Storm (High Contention Degradation)
When thread contention escalates (e.g. 64 cores hammering `AtomicLong.incrementAndGet()`), CAS performance collapses:
- Only 1 core succeeds per iteration; 63 cores fail their CAS and spin in tight loops.
- Every successful CAS broadcasts an `Invalidate` bus message over the CPU interconnect, causing massive **Cache Line Bouncing**.
- **Solution:** Under extreme contention, use `LongAdder` (which stripes the counter across multiple cells).

### 2. The Classic ABA Problem
Naïve CAS checks: *"Is the current value still equal to $A$?"*
- Thread 1 reads value $A$.
- Thread 1 is preempted by the OS scheduler.
- Thread 2 changes $A \rightarrow B$, then changes $B \rightarrow A$.
- Thread 1 resumes and executes CAS($A \rightarrow C$).
- Because the memory location holds $A$, the CAS succeeds!
- **The Danger:** If $A$ was a pointer in a lock-free linked list or memory pool, intermediate node references may have been recycled or deallocated (e.g., node $B$ was unlinked), corrupting the data structure.

### 3. The Fix: `AtomicStampedReference`
`AtomicStampedReference` pairs the object reference with an integer version stamp:
```java
AtomicStampedReference<Node> ref = new AtomicStampedReference<>(nodeA, 1);
// CAS requires BOTH the expected reference AND the expected stamp!
ref.compareAndSet(nodeA, nodeC, expectedStamp, expectedStamp + 1);
```
Even if the value oscillates $A \rightarrow B \rightarrow A$, the stamp monotonically increments ($1 \rightarrow 2 \rightarrow 3$), causing the victim's CAS to fail safely.

---

# 4. Comparison & Trade-Off Matrix

| Feature | `synchronized` | `AtomicInteger` | `LongAdder` | `AtomicStampedReference` |
| :--- | :--- | :--- | :--- | :--- |
| **Locking Strategy** | Blocking Mutex | Lock-Free CAS | Stripped Lock-Free | Lock-Free CAS + Version |
| **Hardware Instruction**| `pthread_mutex` / Parker | `lock cmpxchg` | `lock cmpxchg` per cell| `lock cmpxchg8b` / `16b` |
| **Performance (Low Contention)** | Good | **Excellent** | Excellent | Good |
| **Performance (High Contention)**| Degrades (Context switch)| Degrades (CAS storm) | **Near-Linear Scaling** | Degrades (CAS storm) |
| **ABA Resistant?** | Yes (Locks prevent it)| **NO** | N/A | **YES** |
| **Memory Footprint**| Object header | 1 int field (4 bytes) | Dynamic `Cell[]` array | Two-reference wrapper object |

---

# 5. Runnable Java Verification Code (`author: raysi`)

The companion lab for this module is located at:
[`AtomicCASVsSynchronizedBenchmark.java`](../../codes/concurrency/phase3_lockfree_atomics_and_synchronizers/AtomicCASVsSynchronizedBenchmark.java)

Run via:
```bash
java -cp target/classes com.raysi.concurrency.phase3_lockfree_atomics_and_synchronizers.codes.AtomicCASVsSynchronizedBenchmark
```

---

# 6. Top 8 High-Signal Concurrency Interview Questions & Answers

### Q1: What is Compare-And-Swap (CAS) and what CPU instruction does it compile to?
**Answer:** CAS is an optimistic atomic instruction that compares the contents of a memory location with an expected value, and if they are equal, updates the location to a new value. On x86 architectures, it compiles to `lock cmpxchg`.

### Q2: Why is `AtomicInteger` faster than `synchronized` under low contention?
**Answer:** `AtomicInteger` executes purely in user space via a single atomic CPU instruction without invoking the OS kernel, avoiding thread state changes, kernel transitions, and context switch overheads (~1-5 µs).

### Q3: When does `AtomicInteger` become slower than `synchronized`?
**Answer:** Under extreme contention (e.g. dozens of threads hammering a single variable), CAS spin loops waste enormous CPU cycles continually retrying and failing. The repeated cache line invalidation saturates the memory bus. `synchronized` suspends waiting threads, freeing physical CPU cores to do useful work.

### Q4: Explain the ABA Problem and provide a concrete example.
**Answer:** The ABA problem occurs when a thread reads value A, pauses, another thread changes A to B and then back to A, and the original thread resumes and executes CAS expecting A. In a lock-free stack (Treiber stack), if node A's `next` pointer pointed to B, and B was freed while A was popped and repushed, the resuming thread will set top to B (a freed node), destroying the stack.

### Q5: How does `AtomicStampedReference` solve the ABA problem?
**Answer:** It binds an object reference to an integer version tag. A CAS operation only succeeds if both the expected reference matches AND the expected integer stamp matches. Each modification increments the stamp, detecting intermediate modifications even if the object reference returns to the original value.

### Q6: What is the difference between `AtomicMarkableReference` and `AtomicStampedReference`?
**Answer:** `AtomicStampedReference` associates an integer version stamp (enabling monotonic incrementing), while `AtomicMarkableReference` associates a single `boolean` flag (useful for marking a node as logically deleted before physical unlinking in lock-free linked lists).

### Q7: What is the difference between `lazySet()` and `set()` in atomic classes?
**Answer:** `set()` is a volatile write, inserting a heavyweight `StoreLoad` memory barrier that flushes the write to RAM immediately. `lazySet()` is an ordered write (inserts a `StoreStore` barrier only); it prevents reordering with prior writes but does not immediately force cache flushes to other cores, saving ~15-30 CPU cycles when immediate visibility is not strictly required.

### Q8: What are VarHandles (Java 9+) and how do they relate to `Unsafe`?
**Answer:** `VarHandle` is the standard, safe, strongly-typed replacement for `sun.misc.Unsafe`. It provides fine-grained memory access modes: Plain, Opaque, Acquire/Release, and Volatile, allowing developers to execute atomic CAS operations and memory fences directly on object fields without proprietary `Unsafe` access.
