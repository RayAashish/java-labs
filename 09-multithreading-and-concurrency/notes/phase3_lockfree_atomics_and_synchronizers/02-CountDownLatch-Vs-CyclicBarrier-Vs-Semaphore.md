# CountDownLatch vs. CyclicBarrier vs. Semaphore
> **Author:** raysi  
> **Package:** `com.raysi.concurrency.phase3_lockfree_atomics_and_synchronizers`  
> **Module:** Thread Coordination Synchronizers, AQS Shared Mode, and Barrier Mechanics

---

# 1. Hardware & JVM Architectural Mental Model

Coordinating multiple parallel threads requires structured barrier and permit abstractions. In multi-core computing, tasks often have synchronization dependencies: waiting for worker pools to initialize, synchronizing stages of a parallel algorithm, or rate-limiting access to finite hardware/network resources.

```
┌────────────────────────────────────────────────────────────────────────┐
│                   THE THREE COORDINATION PRIMITIVES                    │
│                                                                        │
│ 1. COUNTDOWNLATCH (One-Shot Countdown)                                 │
│    Tasks finish -> countDown()                                         │
│    Worker 1 ──┐                                                        │
│    Worker 2 ──┼──► [ Count = 3 ──► 2 ──► 1 ──► 0 ] ──► [ GATE OPENS! ] │
│    Worker 3 ──┘                                        (Cannot Reset!) │
│                                                                        │
│ 2. CYCLICBARRIER (Reusable Lockstep Barrier)                           │
│    Thread 1 ──┐                                                        │
│    Thread 2 ──┼──► [ 3 Arrived? ] ──► Run BarrierAction ──► [ RESET &  │
│    Thread 3 ──┘    (await())                                 PROCEED]  │
│                                                                        │
│ 3. SEMAPHORE (Permit Throttle)                                         │
│    Incoming Requests                                                   │
│    R1, R2, R3, R4 ──► [ Permits = 2 ] ──► Allowed (R1, R2)             │
│                           ▲               Queued  (R3, R4)             │
│                           │ release()                                  │
└────────────────────────────────────────────────────────────────────────┘
```

---

# 2. Under the Hood: JDK Source Code & Bytecode Mechanics

### 1. `CountDownLatch`: Built on AQS Shared Mode
Inside `java.util.concurrent.CountDownLatch`:
```java
private static final class Sync extends AbstractQueuedSynchronizer {
    Sync(int count) {
        setState(count); // Initial AQS state = N
    }

    protected int tryAcquireShared(int acquires) {
        // Gate opens when state == 0; threads block when state > 0
        return (getState() == 0) ? 1 : -1;
    }

    protected boolean tryReleaseShared(int releases) {
        for (;;) {
            int c = getState();
            if (c == 0) return false;
            int nextc = c - 1;
            if (compareAndSetState(c, nextc)) // Atomic decrement
                return nextc == 0; // Returns true when count reaches 0!
        }
    }
}
```
When `countDown()` reduces `state` to 0, AQS unparks all waiting threads in the CLH queue via shared node propagation.

### 2. `CyclicBarrier`: Built on `ReentrantLock` & `Condition`
Unlike `CountDownLatch`, `CyclicBarrier` is **not** an AQS subclass. It encapsulates an internal `ReentrantLock` and a single `Condition trip = lock.newCondition()`:
- Each arriving thread calls `trip.await()`.
- The last arriving thread executes the `barrierCommand` runnable and invokes `trip.signalAll()`.
- It initializes a new `Generation` object, resetting `count = parties` for the next cycle.

### 3. `Semaphore`: Built on AQS Shared Mode
Maintains `state = permits`:
- `acquire()`: Decrements `state` via CAS. If `state < 0`, thread enqueues in AQS as a `SHARED` node and parks.
- `release()`: Increments `state` via CAS and unparks waiting threads.

---

# 3. Step-by-Step State Transitions & Concurrency Traps

### 1. The Hanging `CountDownLatch` Trap
If a worker thread encounters an unhandled exception before calling `latch.countDown()`, the latch counter will **never reach zero**!
- The main thread waiting on `latch.await()` will hang forever.
- **Remedy:** Always place `latch.countDown()` inside a `finally` block, and always use `await(timeout, unit)`:
  ```java
  try {
      doWork();
  } finally {
      latch.countDown(); // Guaranteed execution!
  }
  ```

### 2. The `BrokenBarrierException` Trap in `CyclicBarrier`
If **any single thread** is interrupted or times out while waiting at a `CyclicBarrier`, the barrier breaks!
- All other waiting threads wake up immediately and throw `BrokenBarrierException`.
- The barrier remains permanently broken until `barrier.reset()` is explicitly invoked.

### 3. Semaphore Permit Leaks
A `Semaphore` does not track which thread acquired a permit. Any thread can call `semaphore.release()`.
- If an exception occurs before `release()` is called in a `finally` block, permits are permanently leaked, leading to thread pool starvation.
- If `release()` is accidentally called twice, permits artificially increase beyond initial capacity!

---

# 4. Comparison & Trade-Off Matrix

| Feature | `CountDownLatch` | `CyclicBarrier` | `Semaphore` |
| :--- | :--- | :--- | :--- |
| **Primary Purpose** | Wait for $N$ events | Wait for $N$ threads at a phase | Limit concurrent access to $N$ resources |
| **Internal Mechanism**| AQS Shared `state` | `ReentrantLock` + `Condition` | AQS Shared `state` |
| **Reusability** | **No (One-shot only)** | **Yes (Resets automatically)**| **Yes (Continuous)** |
| **Who Waits?** | Calling thread(s) | The worker threads themselves | Any thread acquiring permits |
| **Action on Trip?** | No | Yes (`Runnable barrierAction`) | No |
| **Failure Mode** | Indefinite wait if skipped | `BrokenBarrierException` | Permit exhaustion / leak |

---

# 5. Runnable Java Verification Code (`author: raysi`)

The companion lab for this module is located at:
[`CountDownLatchCyclicBarrierSemaphoreDemo.java`](../../codes/concurrency/phase3_lockfree_atomics_and_synchronizers/CountDownLatchCyclicBarrierSemaphoreDemo.java)

Run via:
```bash
java -cp target/classes com.raysi.concurrency.phase3_lockfree_atomics_and_synchronizers.codes.CountDownLatchCyclicBarrierSemaphoreDemo
```

---

# 6. Top 8 High-Signal Concurrency Interview Questions & Answers

### Q1: Can a `CountDownLatch` be reset and reused after reaching zero?
**Answer:** No. Once the internal AQS state reaches zero, all subsequent calls to `await()` return immediately and `countDown()` has no effect. If you need a reusable countdown latch, use `CyclicBarrier` or `Phaser`.

### Q2: How does `CyclicBarrier` differ fundamentally from `CountDownLatch`?
**Answer:** `CountDownLatch` is designed for threads waiting for events (a thread can call `countDown()` multiple times without waiting). `CyclicBarrier` is designed for a fixed set of threads waiting for **each other** at a common barrier point. Furthermore, `CyclicBarrier` is reusable across iterations and supports an optional barrier action.

### Q3: What is Java's `Phaser` (Java 7+) and how does it relate to Latch and Barrier?
**Answer:** `Phaser` is a more flexible, dynamic synchronizer that unifies `CountDownLatch` and `CyclicBarrier`. Unlike `CyclicBarrier`, the number of registered parties in a `Phaser` can dynamically register and deregister at any time (`register()`, `arriveAndDeregister()`).

### Q4: Can a `Semaphore` initialized with 0 permits be used as a signaling rendezvous?
**Answer:** Yes. A thread can block calling `semaphore.acquire()`. Another thread performing initialization can call `semaphore.release()`, generating a permit and waking up the waiting thread. This functions similarly to a binary event signal.

### Q5: What happens if `release()` is called on a `Semaphore` without prior `acquire()`?
**Answer:** `Semaphore` does not track ownership. Calling `release()` simply increments the internal permit counter via CAS. If called repeatedly without `acquire()`, `availablePermits()` can exceed the initially configured permit count!

### Q6: What causes a `BrokenBarrierException` in `CyclicBarrier`?
**Answer:** It is thrown when a thread attempts to wait on a barrier that is currently in a broken state, or when another waiting thread was interrupted or timed out while waiting at the barrier.

### Q7: How does AQS shared node propagation work when `CountDownLatch` hits zero?
**Answer:** When `countDown()` sets `state = 0`, it calls `doReleaseShared()`. The head node unparks its successor. When that successor wakes up, it calls `setHeadAndPropagate()`, which immediately unparks the *next* waiting shared node in the queue without waiting for another release call, causing a rapid cascading unpark across all waiting threads.

### Q8: How can a `Semaphore` with 1 permit be used as a Mutex, and how does it differ from `ReentrantLock`?
**Answer:** A `Semaphore(1)` allows only one thread inside the critical section. However, unlike `ReentrantLock`, it is **non-reentrant** (a thread calling `acquire()` twice will deadlock itself) and **lacks ownership** (any thread can release the permit, even if it was not the thread that acquired it).
