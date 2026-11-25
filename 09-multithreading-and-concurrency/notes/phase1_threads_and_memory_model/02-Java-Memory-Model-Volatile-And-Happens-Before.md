# Java Memory Model (JMM): Volatile, Memory Barriers & Happens-Before
> **Author:** raysi  
> **Package:** `com.raysi.concurrency.phase1_threads_and_memory_model`  
> **Module:** CPU Cache Hierarchy, MESI Protocol, Instruction Reordering, Memory Barriers, and JMM Formal Semantics

---

# 1. Hardware & JVM Architectural Mental Model

The Java Memory Model (JMM, defined in JSR-133) exists because modern multi-core computers prioritize execution speed over sequential consistency. To understand visibility and reordering bugs, we must trace memory operations down to the physical silicon.

### The Physical CPU Cache Hierarchy
Modern CPUs execute instructions in under 1 nanosecond (~3-4 GHz), while accessing Main RAM takes 50-100 nanoseconds. To prevent the CPU from stalling, hardware designers added hierarchical SRAM caches:
- **L1 Cache:** ~32KB to 64KB per core, access time ~1 ns (~4 cycles).
- **L2 Cache:** ~512KB to 1MB per core, access time ~3-4 ns (~14 cycles).
- **L3 Cache:** ~16MB to 64MB shared across cores, access time ~10-20 ns (~50 cycles).
- **Main RAM:** Multi-gigabyte DRAM, access time ~60-100 ns (~200 cycles).

```
┌───────────────────────────────────────┐       ┌───────────────────────────────────────┐
│              CPU CORE 0               │       │              CPU CORE 1               │
│  ┌─────────────────────────────────┐  │       │  ┌─────────────────────────────────┐  │
│  │ Execution Engine (ALU / Regs)   │  │       │  │ Execution Engine (ALU / Regs)   │  │
│  └───────────────┬─────────────────┘  │       │  └───────────────┬─────────────────┘  │
│                  ▼                    │       │                  ▼                    │
│  ┌─────────────────────────────────┐  │       │  ┌─────────────────────────────────┐  │
│  │   Store Buffer (Async Writes)   │  │       │  │   Store Buffer (Async Writes)   │  │
│  └───────────────┬─────────────────┘  │       │  └───────────────┬─────────────────┘  │
│                  ▼                    │       │                  ▼                    │
│  ┌─────────────────────────────────┐  │       │  ┌─────────────────────────────────┐  │
│  │      L1 Data Cache (Private)    │  │       │  │      L1 Data Cache (Private)    │  │
│  │      [running = true cached]    │  │       │  │      [running = false written]  │  │
│  └───────────────┬─────────────────┘  │       │  └───────────────┬─────────────────┘  │
│                  ▼                    │       │                  ▼                    │
│  ┌─────────────────────────────────┐  │       │  ┌─────────────────────────────────┐  │
│  │         L2 Cache (Private)      │  │       │  │         L2 Cache (Private)      │  │
│  └───────────────┬─────────────────┘  │       │  └───────────────┬─────────────────┘  │
└──────────────────┼────────────────────┘       └──────────────────┼────────────────────┘
                   │                                               │
                   ▼                                               ▼
         ┌──────────────────────────────────────────────────────────────────┐
         │                    L3 Cache (Shared across Cores)                │
         │                    [MESI Cache Coherence Bus Snooping]           │
         └─────────────────────────────────┬────────────────────────────────┘
                                           ▼
         ┌──────────────────────────────────────────────────────────────────┐
         │                       MAIN PHYSICAL MEMORY (DRAM)                │
         │                            [Shared Heap Data]                    │
         └──────────────────────────────────────────────────────────────────┘
```

### The Visibility Bug: Store Buffers & Loop Hoisting
Why did the worker thread loop forever in our demo without `volatile`?
1. **Hardware Level:** Core 1 writes `running = false`. It writes this value into its local **Store Buffer** to avoid stalling. It does not immediately flush to the Shared L3 Cache or RAM. Meanwhile, Core 0 reads `running` directly out of its private L1 cache, observing stale data.
2. **JIT Compiler Optimization (Loop Hoisting):** HotSpot's C2 JIT compiler observes that inside the tight loop `while (running) {}`, `running` is never modified by the local thread. The compiler assumes sequential execution and optimizes the bytecode:
   ```java
   // What you wrote:
   while (running) { iterations++; }

   // What the C2 JIT Compiler generates:
   if (running) {
       while (true) { iterations++; } // Infinite loop in CPU registers!
   }
   ```

---

# 2. Under the Hood: JDK Source Code & Bytecode Mechanics

### The Four Hardware Memory Barriers
A **Memory Barrier** (or Memory Fence) is a CPU hardware instruction that forces the CPU and compiler to preserve memory operation ordering across the fence:
- **`LoadLoad`:** Guarantees all preceding reads are completed before subsequent reads execute.
- **`StoreStore`:** Guarantees all preceding writes are flushed to cache before subsequent writes execute.
- **`LoadStore`:** Guarantees preceding reads complete before subsequent writes execute.
- **`StoreLoad`:** The heavyweight barrier. Flushes the local Store Buffer to cache and invalidates stale entries, ensuring preceding writes are visible before any subsequent reads execute. On x86, this compiles to `lock addl` or `mfence`.

### Volatile Bytecode & JIT Emission
When a field is declared `volatile`, the compiler records the `ACC_VOLATILE` flag in the class file's `FieldInfo`. HotSpot's JIT compiler inserts memory barriers around reads and writes:

```
  [Preceding Ordinary/Volatile Stores]
                  │
          ┌───────▼───────┐
          │  StoreStore   │  (Prevents reordering prior writes with volatile write)
          └───────┬───────┘
                  ▼
         [ VOLATILE WRITE ]
                  │
          ┌───────▼───────┐
          │   StoreLoad   │  (Flushes write buffer to RAM; prevents reordering with later reads)
          └───────┬───────┘
                  ▼
  [Subsequent Volatile/Ordinary Reads]
```

### The Formal "Happens-Before" (HB) Guarantee
Under the JMM, memory operations are defined by partial orders. If Action $A$ **happens-before** Action $B$ ($A \xrightarrow{hb} B$), the runtime guarantees that all memory writes performed by $A$ are completely visible to Action $B$:
1. **Program Order Rule:** Each action in a single thread happens-before every action that appears later in program order.
2. **Monitor Lock Rule:** An unlock on an intrinsic monitor or `Lock` happens-before every subsequent lock on that same monitor.
3. **Volatile Variable Rule:** A write to a `volatile` field happens-before every subsequent read of that same field.
4. **Thread Start Rule:** Calling `thread.start()` happens-before any action in the started thread.
5. **Thread Join Rule:** All actions in a thread happen-before any other thread successfully returns from `join()` on that thread.
6. **Transitivity:** If $A \xrightarrow{hb} B$ and $B \xrightarrow{hb} C$, then $A \xrightarrow{hb} C$.

---

# 3. Step-by-Step State Transitions & Concurrency Traps

### The Classic Double-Checked Locking (DCL) Broken Singleton
Prior to understanding the JMM, developers wrote the following pattern for lazy singleton initialization:

```java
public class BrokenSingleton {
    private static BrokenSingleton instance; // BUG: Missing volatile!

    public static BrokenSingleton getInstance() {
        if (instance == null) {                         // Check 1
            synchronized (BrokenSingleton.class) {
                if (instance == null) {                 // Check 2
                    instance = new BrokenSingleton();   // The Trap!
                }
            }
        }
        return instance;
    }
}
```

#### Why it fails without `volatile`:
The statement `instance = new BrokenSingleton()` is **not atomic**. The JVM decomposes it into 3 bytecode steps:
1. `allocate`: Allocate heap memory block for `BrokenSingleton`.
2. `ctor`: Invoke constructor `<init>` to initialize instance fields.
3. `assign`: Assign the memory address pointer to the static variable `instance`.

Because step 2 and step 3 are independent in single-threaded analysis, the CPU or JIT compiler is free to reorder them:
$$1 \rightarrow 3 \rightarrow 2$$
If Thread A executes $1 \rightarrow 3$, `instance` is now **non-null**, but the object is uninitialized. If Thread B arrives at Check 1 (`if (instance == null)`), it sees `instance != null`, skips synchronization, and returns a half-baked object, leading to catastrophic `NullPointerException`s in production!

#### The Fix:
Declaring `private static volatile BrokenSingleton instance;` inserts a `StoreStore` barrier, guaranteeing constructor initialization (step 2) completes before pointer assignment (step 3).

---

# 4. Comparison & Trade-Off Matrix

| Dimension | `volatile` Variable | `AtomicInteger` / CAS | `synchronized` Block | `ReentrantLock` |
| :--- | :--- | :--- | :--- | :--- |
| **Visibility Guarantee** | Yes (StoreLoad barrier) | Yes (Internal volatile `value`) | Yes (Monitor exit flushes cache) | Yes (AQS volatile `state`) |
| **Atomicity for `count++`**| **NO (Race Condition)** | **YES (Lock-free `cmpxchg`)** | **YES (Mutual exclusion)** | **YES (Mutual exclusion)** |
| **Execution Cost** | Minimal (Memory fence only)| Low-Medium (CPU spin loop) | Medium-High (Mutex park/unpark)| Medium-High (OS park/unpark) |
| **Thread Suspension?** | Never | Never (Spins on collision) | Yes (Enters `BLOCKED`) | Yes (Enters `WAITING`) |
| **Primary Use Case** | Status flags, shutdown triggers | Counters, non-blocking stacks | Compound mutations, simple mutex| Timed lock, fair queue, conditions|

---

# 5. Runnable Java Verification Code (`author: raysi`)

The companion lab for this module is located at:
[`VolatileCpuCacheVisibilityBugDemo.java`](../../codes/concurrency/phase1_threads_and_memory_model/VolatileCpuCacheVisibilityBugDemo.java)

Run via:
```bash
java -cp target/classes com.raysi.concurrency.phase1_threads_and_memory_model.codes.VolatileCpuCacheVisibilityBugDemo
```

---

# 6. Top 8 High-Signal Concurrency Interview Questions & Answers

### Q1: Why does `volatile` guarantee visibility but not atomicity?
**Answer:** `volatile` guarantees that reads and writes go directly through memory barriers to coherent cache/RAM. However, compound operations like `count++` involve three distinct CPU operations: (1) read value into register, (2) increment register, (3) write register back to memory. If two threads read `10` simultaneously, both increment to `11` and write back `11`. One update is permanently lost. Atomicity requires an indivisible hardware primitive (`lock cmpxchg`).

### Q2: What is the MESI Cache Coherence Protocol?
**Answer:** MESI is a hardware protocol used across CPU caches over a snooping bus. Every cache line (typically 64 bytes) is marked with one of four states:
- **Modified (M):** Exists only in the current cache and is dirty (differs from RAM).
- **Exclusive (E):** Exists only in the current cache and is clean (matches RAM).
- **Shared (S):** Exists in multiple caches and is clean.
- **Invalid (I):** The cache line data is obsolete; subsequent reads must reload from L3/RAM.

### Q3: What is False Sharing and how does `@jdk.internal.vm.annotation.Contended` fix it?
**Answer:** Multi-core CPUs operate on cache lines of 64 bytes. If two independent variables (e.g., `thread1Counter` and `thread2Counter`) reside in the same 64-byte line, Core 0 writing to variable 1 repeatedly invalidates Core 1's cache line for variable 2. The cores constantly bounce the cache line back and forth over the bus (Cache Line Bouncing). In Java 8+, `@Contended` injects 128 bytes of padding to force variables into separate cache lines.

### Q4: Explain the difference between Compiler Reordering and CPU Out-of-Order Execution.
**Answer:**
- **Compiler Reordering:** The JIT compiler rearranges instructions during bytecode-to-machine-code compilation if it detects no data dependency within a single thread (as-if-serial semantics).
- **CPU Out-of-Order Execution:** The physical CPU execution pipeline reorders instructions dynamically at runtime to keep functional units busy when waiting on slow memory loads.

### Q5: Can a 64-bit `long` or `double` read/write produce word tearing in Java?
**Answer:** Under the JMM specification (prior to Java 8 on 32-bit JVMs), 64-bit `long` and `double` writes were allowed to be split into two separate 32-bit writes. If two threads wrote concurrently, a reader could observe the top 32 bits from Thread A and bottom 32 bits from Thread B (Word Tearing). Declaring the field `volatile long` guarantees atomic 64-bit access. (On 64-bit JVMs, word tearing does not occur in practice).

### Q6: How does the Piggybacking (Roach Motel) pattern work in JMM?
**Answer:** Because of the Transitivity rule of Happens-Before, an ordinary (non-volatile) write that occurs *before* a volatile write is also guaranteed to be visible to any thread that reads that same volatile variable later. The volatile write acts as an anchor that "flushes" all preceding non-volatile writes along with it.

### Q7: What is an Invalidate Queue in CPU hardware?
**Answer:** When Core 0 sends an invalidation message to Core 1, Core 1 doesn't wait to actually evict the cache line before sending an ACK. Instead, it places the request into an **Invalidate Queue** and immediately returns ACK. The CPU then processes the invalidation asynchronously. Memory barriers force the CPU to process its Invalidate Queue before proceeding with subsequent memory reads.

### Q8: Why does adding a `System.out.println()` inside a `while (!flag)` loop sometimes make it terminate even without `volatile`?
**Answer:** `PrintStream.println()` contains an internal `synchronized` block! Entering and exiting a `synchronized` block requires the JVM to acquire and release an intrinsic monitor, which enforces memory barriers (Monitor Enter = Acquire fence, Monitor Exit = Release fence). This inadvertently causes the CPU to invalidate cached registers and reload `flag` from memory. However, relying on this is an architectural anti-pattern and undefined behavior!
