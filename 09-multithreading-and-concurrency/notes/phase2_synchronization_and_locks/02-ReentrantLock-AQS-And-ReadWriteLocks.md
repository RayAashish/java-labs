# ReentrantLock, AQS Framework & ReadWriteLocks
> **Author:** raysi  
> **Package:** `com.raysi.concurrency.phase2_synchronization_and_locks`  
> **Module:** AbstractQueuedSynchronizer (AQS), CLH Queue, Fairness, ReentrantReadWriteLock & StampedLock

---

# 1. Hardware & JVM Architectural Mental Model

Before Java 5, developers were restricted to intrinsic `synchronized` blocks. This posed severe limitations:
1. No timeout capability (`synchronized` blocks indefinitely).
2. Cannot be interrupted cooperatively during lock acquisition.
3. No concept of fairness (threads can be starved indefinitely).
4. No distinction between concurrent readers and exclusive writers.

Doug Lea designed **`AbstractQueuedSynchronizer` (AQS)** as the foundational engine powering `ReentrantLock`, `ReentrantReadWriteLock`, `Semaphore`, `CountDownLatch`, and `FutureTask`. AQS moves synchronization management entirely into Java user-space, avoiding kernel transitions unless thread suspension is strictly required.

```
┌────────────────────────────────────────────────────────────────────────┐
│               ABSTRACTQUEUEDSYNCHRONIZER (AQS) ARCHITECTURE            │
│                                                                        │
│   ┌────────────────────────────────────────────────────────────────┐   │
│   │  volatile int state;                                           │   │
│   │  (0 = Unlocked; >0 = Locked / Recursion Count / Permit Count)  │   │
│   └────────────────────────────────┬───────────────────────────────┘   │
│                                    │ Mutated via CAS                   │
│   ┌────────────────────────────────▼───────────────────────────────┐   │
│   │ ExclusiveOwnerThread = Thread[Thread-1, 5, main]               │   │
│   └────────────────────────────────────────────────────────────────┘   │
│                                                                        │
│   CLH DOUBLY-LINKED FIFO WAIT QUEUE:                                   │
│   ┌──────────────┐     ┌──────────────┐     ┌──────────────┐           │
│   │     HEAD     │ ──► │    NODE 1    │ ──► │    NODE 2    │ (TAIL)    │
│   │ (Dummy Sentinel)   │ [Thread-2]   │     │ [Thread-3]   │           │
│   │              │ ◄── │ status=SIGNAL│ ◄── │ status=0     │           │
│   └──────────────┘     └──────┬───────┘     └──────────────┘           │
│                               │                                        │
│                               ▼                                        │
│                 LockSupport.park(Thread-2)                             │
└────────────────────────────────────────────────────────────────────────┘
```

---

# 2. Under the Hood: JDK Source Code & Bytecode Mechanics

### The Core AQS Primitives
Looking inside OpenJDK `java.util.concurrent.locks.AbstractQueuedSynchronizer`:
```java
public abstract class AbstractQueuedSynchronizer {
    private volatile int state; // Synchronization state
    private transient volatile Node head;
    private transient volatile Node tail;

    // Mutated via atomic VarHandle / Unsafe CAS:
    protected final boolean compareAndSetState(int expect, int update) {
        return STATE.compareAndSet(this, expect, update);
    }
}
```

### The Acquisition Path (`acquire(1)`):
```java
public final void acquire(int arg) {
    if (!tryAcquire(arg) &&
        acquireQueued(addWaiter(Node.EXCLUSIVE), arg))
        selfInterrupt();
}
```
1. **`tryAcquire(arg)`**: The subclass (`Sync` in `ReentrantLock`) attempts an immediate CAS on `state` from $0 \rightarrow 1$. If successful, it claims ownership with zero parking!
2. **`addWaiter(Node.EXCLUSIVE)`**: If CAS fails, the thread constructs a `Node` containing `Thread.currentThread()` and inserts it at the `tail` of the CLH FIFO queue using a CAS loop.
3. **`acquireQueued()`**: The enqueued thread tries one more time if its predecessor is `head`. If still blocked, its predecessor sets `waitStatus = Node.SIGNAL` (meaning *"I will wake you when I release"*), and the thread parks via `LockSupport.park(this)`.

### Bit-Packing in `ReentrantReadWriteLock`
How does a single 32-bit `int state` track both Shared Read Locks and Exclusive Write Locks?
It splits the 32-bit integer into two 16-bit halves:
```
 31                            16 15                             0
┌────────────────────────────────┬────────────────────────────────┐
│   SHARED READ LOCK COUNT       │   EXCLUSIVE WRITE RECURSION    │
│   (state >>> 16)               │   (state & 0xFFFF)             │
└────────────────────────────────┴────────────────────────────────┘
```
- **Read Lock:** Increments the high 16 bits via `state + (1 << 16)`.
- **Write Lock:** Increments the low 16 bits via `state + 1` (only valid when high 16 bits are 0).

---

# 3. Step-by-Step State Transitions & Concurrency Traps

### Fair vs. Non-Fair Locks: The Barging Advantage
In `ReentrantLock`:
```java
ReentrantLock unfair = new ReentrantLock();     // Default: Non-Fair
ReentrantLock fair   = new ReentrantLock(true); // Fair Lock
```

- **Fair Lock:** When a new thread arrives, it inspects `hasQueuedPredecessors()`. If any thread is already queued ahead of it, it queues up at the tail. **Throughput drops dramatically** due to constant thread parking and unparking context switches.
- **Non-Fair Lock (Barging):** A newly arrived thread attempts an immediate CAS on `state` before checking the queue. If the lock was just released by the previous owner, the incoming thread (already hot in the CPU cache) grabs the lock immediately, saving thousands of clock cycles! Non-fair locks can achieve up to **10x higher throughput** than fair locks.

### The StampedLock Optimistic Reading Trap
`StampedLock` provides `tryOptimisticRead()`. It returns a non-zero version stamp:
```java
long stamp = sl.tryOptimisticRead();
int curX = this.x;
int curY = this.y;
if (!sl.validate(stamp)) { // If writer intervened, stamp is invalid!
    stamp = sl.readLock(); // Fallback to pessimistic read lock
    try {
        curX = this.x;
        curY = this.y;
    } finally {
        sl.unlockRead(stamp);
    }
}
```
**CRITICAL CAVEATS:**
1. **Never use StampedLock with reentrancy!** It is NOT reentrant. A thread holding a read lock that calls another method attempting a write lock will self-deadlock.
2. Optimistic reads do not insert memory barriers. Ensure all state read optimistically is copied to local stack variables before validating the stamp!

---

# 4. Comparison & Trade-Off Matrix

| Feature | `synchronized` | `ReentrantLock` | `ReentrantReadWriteLock` | `StampedLock` |
| :--- | :--- | :--- | :--- | :--- |
| **Reentrancy** | Yes | Yes | Yes | **NO (Self-deadlocks!)** |
| **Optimistic Reads** | No | No | No | **Yes (`tryOptimisticRead`)** |
| **Fairness Support** | No (JVM chooses) | Yes | Yes | No |
| **Condition Variables** | 1 (wait/notify) | Multiple (`Condition`) | Multiple (WriteLock only)| None |
| **Throughput (Read-Heavy)**| Medium | Medium | High | **Extremely High** |
| **Interruptible Locks** | No | Yes (`lockInterruptibly`)| Yes | Yes |

---

# 5. Runnable Java Verification Code (`author: raysi`)

Companion labs for this module:
- [`ProducerConsumerWaitNotifyVsCondition.java`](../../codes/concurrency/phase2_synchronization_and_locks/ProducerConsumerWaitNotifyVsCondition.java)
- [`ReentrantReadWriteAndStampedLockDemo.java`](../../codes/concurrency/phase2_synchronization_and_locks/ReentrantReadWriteAndStampedLockDemo.java)

---

# 6. Top 8 High-Signal Concurrency Interview Questions & Answers

### Q1: What is the internal architecture of AQS?
**Answer:** AQS relies on three core components: (1) A `volatile int state` variable representing the lock/permit state, mutated atomically via CAS. (2) A FIFO doubly-linked CLH variant queue composed of `Node` objects holding parked threads. (3) `LockSupport.park()` and `unpark()` system primitives used to suspend and resume threads without spinning.

### Q2: Why is the Non-Fair locking policy faster in `ReentrantLock`?
**Answer:** Because it permits "barging." When a thread releases a lock, unparking the queued head thread takes several microseconds (OS context switch). A non-fair lock allows any newly arrived thread currently running on a CPU core to acquire the lock immediately. By the time the unparked thread wakes up, the barging thread may have already finished, drastically increasing CPU pipeline throughput.

### Q3: How does `Condition.await()` work under the hood in AQS?
**Answer:** Each `ConditionObject` maintains its own FIFO singly-linked condition queue. When `await()` is called: (1) The thread releases the lock completely (`fullyRelease`), recording the recursion depth. (2) It enqueues itself as a `Node` in the Condition queue. (3) It parks itself via `LockSupport.park()`. When `signal()` is called, the signaling thread transfers the node from the Condition queue to the AQS sync queue, where it re-competes for the lock.

### Q4: What is Lock Downgrading vs Lock Upgrading in `ReentrantReadWriteLock`?
**Answer:**
- **Lock Downgrading IS supported:** A thread holding a write lock can acquire a read lock, and then release its write lock, successfully downgrading to a read lock without leaving a window for other writers.
- **Lock Upgrading is NOT supported:** A thread holding a read lock cannot acquire a write lock; doing so causes an immediate deadlock because both readers would wait for the other to release.

### Q5: What is the purpose of `LockSupport.park()` and how does it differ from `wait()`?
**Answer:** `LockSupport.park()` operates on thread permits rather than object monitors. It can be called without any enclosing synchronized block. Furthermore, `unpark(thread)` can be called *before* `park()`! The permit is stored, so the subsequent `park()` returns immediately without suspending the thread.

### Q6: How does `StampedLock` avoid writer starvation?
**Answer:** In `ReentrantReadWriteLock`, continuous streams of readers can completely starve a waiting writer. In `StampedLock`, optimistic readers do not increment any shared counter, meaning writers are never blocked by optimistic readers. Additionally, its internal CLH queue interleaves reader and writer nodes more aggressively.

### Q7: Why must `unlock()` always be placed in a `finally` block for `ReentrantLock`?
**Answer:** Unlike `synchronized`, where the JVM bytecode exception table automatically executes `monitorexit`, explicit locks have no automatic cleanup. If an unchecked exception occurs before `unlock()` is called outside a `finally` block, the lock remains held forever, causing all subsequent threads to stall in a permanent deadlock.

### Q8: What happens when an interrupted thread calls `lock.lock()` vs `lock.lockInterruptibly()`?
**Answer:** `lock.lock()` ignores interruption during the queue wait: it continues queuing and parking until the lock is acquired, and only sets the thread's interrupt flag upon return. In contrast, `lock.lockInterruptibly()` immediately aborts the lock acquisition attempt, removes the node from the AQS queue, and throws an `InterruptedException`.
