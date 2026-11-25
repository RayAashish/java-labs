# <ArrayDeque — Circular Buffer Deep Dive>
> Author: raysi

## 1. Architectural Overview & Hierarchy

`java.util.ArrayDeque` is a resizable, contiguous circular array implementation of the `Deque` (Double-Ended Queue) interface. It has no capacity restrictions and grows dynamically to support usage as both a high-throughput **FIFO Queue** and an ultra-fast **LIFO Stack**.

```
                           java.lang.Iterable<E>
                                     │
                                     ▼
                           java.util.Collection<E>
                                     │
               ┌─────────────────────┴─────────────────────┐
               ▼                                           ▼
      java.util.Queue<E>                        java.util.AbstractCollection<E>
               │                                           │
               ▼                                           │
      java.util.Deque<E>                                   │
      (SequencedCollection)                                │
               │                                           │
               └─────────────────────┬─────────────────────┘
                                     │
                                     ▼
                          java.util.ArrayDeque<E>
                   (Contiguous Resizable Ring Buffer)
```

### Exact Type Signature (JDK 21+)

```java
package java.util;

public class ArrayDeque<E> extends AbstractCollection<E>
    implements Deque<E>, Cloneable, java.io.Serializable {

    @java.io.Serial
    private static final long serialVersionUID = 2340985798034038923L;

    // The array in which the elements of the deque are stored.
    transient Object[] elements;

    // The index of the element at the head of the deque.
    transient int head;

    // The index at which the next element would be added to the tail of the deque.
    transient int tail;

    private static final int MAX_ARRAY_SIZE = Integer.MAX_VALUE - 8;
}
```

### Why Does ArrayDeque Exist? What Problem Does It Solve?

1. **Replaces `LinkedList` for Deque/Queue workloads**:
   - `LinkedList` creates a new `Node` instance on every enqueue, resulting in massive heap fragmentation, GC allocation churn, and 24-byte pointer overhead per element.
   - `ArrayDeque` stores element references in a single contiguous array, eliminating per-element object allocations and maximizing CPU cache line utilization.
2. **Replaces `java.util.Stack` for LIFO Stack workloads**:
   - `Stack` synchronizes every method call on `this` via its `Vector` superclass, leaking arbitrary index access (`stack.set(2, val)`) and degrading performance.
   - `ArrayDeque` is unsynchronized, enforces proper stack semantics, and executes stack pushes/pops $3\times\text{--}5\times$ faster.
3. **True $O(1)$ Dual-Ended Performance**:
   - Standard arrays require $O(N)$ element shifting when inserting or removing from index 0.
   - `ArrayDeque` uses a **Circular Buffer (Ring Buffer)** where head and tail cursors move freely around array boundaries, providing true $O(1)$ amortized operations at both ends.

---

## 2. Internal Data Structure & Memory Layout (JDK Source Code Level)

### The Resizable Circular Buffer (Ring Buffer)

`ArrayDeque` is backed by a 0-indexed flat array `Object[] elements` with two cursor pointers:
- `head`: The index of the first element (returned by `peekFirst()` or `pollFirst()`).
- `tail`: The index where the *next* element will be inserted at the end (via `addLast()`).

```
Logical Circular Ring View:
                  [0] (E0)
            [7]             [1] (E1)
         (tail)             
        [6]                     [2] (E2)
                             
            [5]             [3] (E3)
                  [4] (head)
 
Physical Array View (Wrapped Circular State):
┌────────┬────────┬────────┬────────┬────────┬────────┬────────┬────────┐
│   E4   │   E5   │   E6   │  null  │   E0   │   E1   │   E2   │   E3   │
└────────┴────────┴────────┴────────┴────────┴────────┴────────┴────────┘
 Index 0  Index 1  Index 2  Index 3  Index 4  Index 5  Index 6  Index 7
                            ▲        ▲
                            │        │
                          tail=3   head=4
 
Elements in Deque order: [E0, E1, E2, E3, E4, E5, E6] (Length = 7)
```

---

### Index Wrapping: The Bitwise Modulo vs. Modern Increment/Decrement

#### 1. Historical JDK (JDK 8 through 16): Power-of-2 Bitwise Masking
In early JDKs, the capacity of `ArrayDeque` was strictly enforced to always be a **power of two** ($2^k$). This allowed replacing expensive CPU division/modulo arithmetic (`index % capacity`) with lightning-fast bitwise AND masking:
$$index \pmod{2^k} \equiv index \ \& \ (2^k - 1)$$

```java
// JDK 8 Source Code
head = (head - 1) & (elements.length - 1); // Wrap backwards
tail = (tail + 1) & (elements.length - 1); // Wrap forwards
```

*Example with capacity 16 ($16 - 1 = 15 = 0b00001111$)*:
- If `head = 0`, decrementing gives `-1` (`0xFFFFFFFF`).
- `(-1) & 15` evaluates directly to `15` (wraps to end of array in 1 CPU cycle).
- If `tail = 15`, incrementing gives `16` (`0b00010000`).
- `16 & 15` evaluates directly to `0` (wraps to index 0).

#### 2. Modern JDK (JDK 17 and 21+): Branch-Predicted `inc` and `dec`
Doug Lea redesigned `ArrayDeque` in modern Java, replacing the bitwise power-of-2 constraint with branch-predicted helper routines:

```java
// JDK 21 Source Code: java.util.ArrayDeque
static final int inc(int i, int modulus) {
    if (++i >= modulus) i = 0;
    return i;
}

static final int dec(int i, int modulus) {
    if (--i < 0) i = modulus - 1;
    return i;
}
```

> **Why the change in JDK 17+?**
> 1. Modern CPU Branch Target Buffers (BTB) predict loop conditions with $>99\%$ accuracy. The branch `if (++i >= modulus)` is practically free for modern CPUs.
> 2. Decoupling capacity from strict powers of two prevents wasteful over-allocation when sizing deques to exact collection sizes and simplifies dynamic growth logic.

---

### Memory Overhead & CPU Cache Efficiency

```
Memory Footprint: ArrayDeque vs. LinkedList (1,000,000 Elements)
┌────────────────────────────────────────────────────────┐
│ ArrayDeque: Flat Object[]                              │
│ ~4 MB array references + 32 bytes instance             │
│ Contiguous Memory -> Loaded in 64-byte L1 Cache Lines │
└────────────────────────────────────────────────────────┘
┌────────────────────────────────────────────────────────┐
│ LinkedList: 1,000,000 Heap Node Allocations            │
│ ~24 MB node scaffolding (headers + prev/next pointers) │
│ Pointer chasing -> L1/L2/L3 Cache Misses               │
└────────────────────────────────────────────────────────┘
```

Modern x86-64 CPU architectures load data into L1/L2/L3 caches in **64-byte cache lines**.
- In `ArrayDeque`, a single cache line fetch pulls **16 consecutive references** (under Compressed OOPs, 4 bytes each).
- When a loop consumes elements from `ArrayDeque`, 15 out of 16 reads are L1 cache hits ($\approx 1 \text{ ns}$ latency).
- In `LinkedList`, every `node.next` dereference jumps to a potentially disparate heap memory address, stalling the CPU instruction pipeline for $\approx 50\text{--}100 \text{ cycles}$ per element.

---

## 3. Step-by-Step Internal Working of Core Operations

### 1. Insertion at Head: `addFirst(E e)`

```java
// JDK 21 Source: java.util.ArrayDeque
public void addFirst(E e) {
    if (e == null)
        throw new NullPointerException();
    final Object[] es = elements;
    head = dec(head, es.length);
    es[head] = e;
    if (head == tail)
        grow(1);
}
```

```
Execution Trace:
1. Null verification: Throws NPE if e == null.
2. Decrement head cursor: dec(head, length). If head was 0, wraps to length - 1.
3. Store reference at elements[head].
4. Full Check: If head == tail, the circular array has completely wrapped and filled every slot.
5. Trigger grow(1) to expand storage.
```

---

### 2. Insertion at Tail: `addLast(E e)` / `offer(E e)`

```java
// JDK 21 Source: java.util.ArrayDeque
public void addLast(E e) {
    if (e == null)
        throw new NullPointerException();
    final Object[] es = elements;
    es[tail] = e;
    if (head == (tail = inc(tail, es.length)))
        grow(1);
}
```

```
Execution Trace:
1. Store reference at current tail: es[tail] = e.
2. Advance tail: tail = inc(tail, length). If tail reached end, wraps to 0.
3. Full Check: If updated tail == head, array is completely full.
4. Trigger grow(1).
```

---

### 3. Deletion at Head: `pollFirst()` / `poll()`

```java
// JDK 21 Source: java.util.ArrayDeque
public E pollFirst() {
    final Object[] es;
    final int h;
    E e = elementAt(es = elements, h = head);
    if (e != null) {
        es[h] = null; // Crucial: Clear reference to prevent Loitering Object Memory Leak!
        head = inc(h, es.length);
    }
    return e;
}
```

---

### 4. Deletion at Tail: `pollLast()`

```java
// JDK 21 Source: java.util.ArrayDeque
public E pollLast() {
    final Object[] es;
    final int t;
    if ((t = tail) != head) {
        int index = dec(t, (es = elements).length);
        E e = elementAt(es, index);
        es[index] = null; // Prevent memory leak!
        tail = index;
        return e;
    }
    return null;
}
```

---

### 5. Resizing & Growth Mechanics (`grow()`)

When `head == tail`, the circular buffer has zero free slots remaining.
In JDK 21, `grow(int needed)` expands the array and realigns the wrapped circular segments:

```java
// JDK 21 Source: java.util.ArrayDeque
private void grow(int needed) {
    final int oldCapacity = elements.length;
    int jump = (oldCapacity < 64) ? (oldCapacity + 2) : (oldCapacity >> 1);
    int newCapacity = newCapacity(needed, jump);
    final Object[] es = elements = Arrays.copyOf(elements, newCapacity);

    // Realign wrapped elements if the buffer had wrapped around!
    if (tail < head || (tail == head && es[head] != null)) {
        // Elements from head to oldCapacity must shift right into the newly expanded space
        int p = oldCapacity - head;
        System.arraycopy(es, head, es, head + (newCapacity - oldCapacity), p);
        Arrays.fill(es, head, head + (newCapacity - oldCapacity), null);
        head += newCapacity - oldCapacity;
    }
}
```

```
Array Realignment during grow() (Visual Walkthrough):
Before grow: Old Capacity = 8, head = 4, tail = 4 (Full buffer)
Physical slots: [E4, E5, E6, E7, E0, E1, E2, E3]
                 0   1   2   3   4   5   6   7

Step 1: Arrays.copyOf expands array to Capacity = 16:
[E4, E5, E6, E7, E0, E1, E2, E3, null, null, null, null, null, null, null, null]
 0   1   2   3   4   5   6   7   8     9     10    11    12    13    14    15

Step 2: Shift head slice [E0..E3] to the right boundary by (16 - 8 = 8):
[E4, E5, E6, E7, null, null, null, null, null, null, null, null, E0, E1, E2, E3]
 0   1   2   3   4     5     6     7     8     9     10    11    12  13  14  15
                 ▲                                               ▲
                 tail=4                                          new head = 12

Queue order is perfectly preserved without re-sorting!
```

---

### 6. Garbage Collection Leak Prevention ("Loitering Objects")

In a circular array, elements are overwritten as head and tail cycle.
If `pollFirst()` or `pollLast()` simply updated the integer index (`head = inc(head)`) without clearing the array slot (`elements[h] = null`), the reference to the dequeued object would linger in heap memory until that specific slot was overwritten.
If no further enqueues occur, these objects remain **reachable from GC roots** via the `elements[]` array, causing **loitering object memory leaks**.
`ArrayDeque` meticulously sets `es[h] = null` and `es[t] = null` on every extraction.

---

## 4. Complexity & Behavioral Matrix

| Operation | Average Case | Worst Case | Amortized | Cache Locality |
| :--- | :--- | :--- | :--- | :--- |
| **`addFirst(e)` / `push(e)`** | $O(1)$ | $O(N)$ (growth) | $O(1)$ | High (sequential array store) |
| **`addLast(e)` / `offer(e)`** | $O(1)$ | $O(N)$ (growth) | $O(1)$ | High (sequential array store) |
| **`pollFirst()` / `poll()` / `pop()`** | $O(1)$ | $O(1)$ | $O(1)$ | High (contiguous read + null out) |
| **`pollLast()`** | $O(1)$ | $O(1)$ | $O(1)$ | High (contiguous read + null out) |
| **`peekFirst()` / `peek()`** | $O(1)$ | $O(1)$ | $O(1)$ | High (direct array dereference) |
| **`peekLast()`** | $O(1)$ | $O(1)$ | $O(1)$ | High (direct array dereference) |
| **`remove(Object)`** | $O(N)$ | $O(N)$ | $O(N)$ | Requires circular scan + `System.arraycopy` shift |
| **`contains(Object)`** | $O(N)$ | $O(N)$ | $O(N)$ | Circular scan from head to tail |
| **`size()`** | $O(1)$ | $O(1)$ | $O(1)$ | Bitwise subtraction: `sub(tail, head, len)` |

### Behavioral Attributes
- **Random Access**: **NOT supported**. `ArrayDeque` does not implement `java.util.List`. Arbitrary access requires an $O(N)$ scan.
- **Null Allowance**: **Strictly Prohibited**. Throws `NullPointerException`.
- **Duplicates**: Permitted.
- **Encounter Order**: Strictly preserved (FIFO or LIFO).

---

## 5. Concurrency & Thread-Safety Mechanics

`ArrayDeque` is completely **unsynchronized** and not thread-safe.

### Fail-Fast Invariants & Concurrency Detection
Unlike `ArrayList` or `PriorityQueue`, `ArrayDeque` in modern JDK does **not maintain a separate `modCount` variable**.
Instead, it detects concurrent modifications through boundary assertion logic:
```java
// Iterator verification
if (elements[tail] != null || (head != tail && elements[head] == null))
    throw new ConcurrentModificationException();
```
Because `elements[tail]` must always be `null` in an uncorrupted circular deque, an unexpected non-null entry indicates concurrent manipulation across threads.

### Modern Concurrent Alternatives:
1. **`ConcurrentLinkedDeque<E>`**:
   - Wait-free reads, non-blocking lock-free writes using Compare-And-Swap (CAS) on bidirectional node pointers.
   - Ideal for concurrent work-stealing schedulers.
2. **`LinkedBlockingDeque<E>`**:
   - Synchronized via a single `ReentrantLock` with `notEmpty` and `notFull` conditions.
   - Ideal for bounded multi-threaded producer-consumer queues.

---

## 6. Production-Grade Java Code Verification

```java
// Basic validation snippet
ArrayDeque<String> deque = new ArrayDeque<>();
deque.addFirst("A");
deque.addLast("B");
assert deque.peekFirst().equals("A");
assert deque.peekLast().equals("B");
```

---

## 7. Top 7 High-Signal Interview Questions & Deep Answers

### Q1: Explain the bitwise AND trick `head = (head - 1) & (elements.length - 1)` used in JDK ArrayDeque. Why must capacity be a power of 2?
**Deep Answer**:
The modulo operation `x % N` computes the remainder of division, but hardware integer division (`idiv`) is one of the slowest CPU instructions (taking $\approx 20\text{--}40 \text{ clock cycles}$).
When the divisor $N$ is a power of two ($N = 2^k$), the binary representation of $N - 1$ consists of exactly $k$ set bits (`1`s) as the least significant bits:
$$N = 16 \implies 16 - 1 = 15 = 0b00001111$$
Performing a bitwise AND `x & (N - 1)` masks out all bits higher than $k-1$, which is mathematically identical to `x % N` for positive numbers, but executes in **1 CPU clock cycle**.
Furthermore, when decrementing past zero (`0 - 1 = -1` in two's complement = `0xFFFFFFFF`), bitwise ANDing with `N - 1` immediately evaluates to `N - 1`, seamlessly wrapping to the end of the array without branching!
This optimization requires capacity to be strictly a power of two because if $N$ is not a power of two, $N - 1$ contains zero bits in between (e.g., $10 - 1 = 9 = 0b1001$), causing incorrect bit truncation.

### Q2: Why did JDK 17/21 change the circular wrapping logic from bitwise AND to `inc()` and `dec()` methods?
**Deep Answer**:
Doug Lea updated `ArrayDeque` in JDK 17 for two principal reasons:
1. **Branch Predictor Performance**:
   Modern CPU Branch Predictors execute `if (++i >= modulus) i = 0` with virtually zero overhead. Since the condition `i >= modulus` evaluates to `false` for every element except the single boundary wrap, the hardware branch predictor achieves $>99.9\%$ branch accuracy, executing almost as fast as a bitwise instruction.
2. **Elimination of Power-of-2 Memory Waste**:
   Enforcing power-of-two capacities forced allocations to jump from 16 to 32, 64, 128, etc. If a caller instantiated `new ArrayDeque(65)`, the old implementation had to allocate an array of size 128 (nearly 100% wasted padding). With `inc()` and `dec()`, capacities can grow more flexibly, reducing memory bloat.

### Q3: Walk through the memory reallocation when `ArrayDeque` is full with wrapped elements (`tail < head`). How does `System.arraycopy` realign the circular array?
**Deep Answer**:
When `ArrayDeque` expands while full, the backing array has already wrapped around, meaning the logical head elements reside at the end of the physical array (`head` to `oldCapacity - 1`) and the logical tail elements reside at the front (`0` to `head - 1`).
In JDK 21:
1. A new array with increased capacity (e.g., $1.5\times$ or $+2$) is allocated via `Arrays.copyOf(elements, newCapacity)`.
2. This creates a larger array with empty `null` slots at the tail: `[0 ... oldCap - 1]` contains data, while `[oldCap ... newCap - 1]` is empty.
3. If left as-is, the logical sequence would be broken because the circular wrap expects `head` to wrap to 0.
4. To fix this, `ArrayDeque` invokes `System.arraycopy`:
   ```java
   int p = oldCapacity - head;
   System.arraycopy(es, head, es, head + (newCapacity - oldCapacity), p);
   Arrays.fill(es, head, head + (newCapacity - oldCapacity), null);
   head += newCapacity - oldCapacity;
   ```
   It shifts the head segment from its current position to the far right end of the newly extended array, clears the vacated middle slots to `null`, and advances `head` by `newCapacity - oldCapacity`. This creates a contiguous free gap in the middle where `tail` can advance cleanly.

### Q4: Why is `ArrayDeque` consistently faster than `LinkedList` for queue and stack workloads, despite both offering $O(1)$ operations?
**Deep Answer**:
While both have $O(1)$ asymptotic complexity, their real-world machine-level performance diverges dramatically due to three hardware factors:
1. **Memory Allocations & GC Churn**: `LinkedList` allocates a new `Node` object for every single `offer()` or `push()`. In high-throughput systems processing millions of messages/sec, this creates intense allocation pressure in the JVM Young Generation (Eden space), accelerating minor GC pauses. `ArrayDeque` creates an array once and amortizes growth across millions of operations.
2. **CPU Cache Line Hit Ratio**: `ArrayDeque` references are stored contiguously in virtual memory. Hardware CPU prefetchers detect linear array traversals and populate L1/L2 data cache lines in advance. `LinkedList` nodes are scattered randomly across heap pages, resulting in pointer chasing that stalls CPU execution units on memory bus reads.
3. **Memory Footprint**: `LinkedList` consumes 24 bytes of overhead per element for node headers and pointers, whereas `ArrayDeque` consumes only 4 bytes (under Compressed OOPs) per element reference.

### Q5: Why does `ArrayDeque` forbid `null` elements, and how does the internal empty check rely on this?
**Deep Answer**:
`ArrayDeque` enforces `Objects.requireNonNull(e)` on all insertions because `null` is utilized as an **internal sentinel value**:
1. In `pollFirst()`, the code reads `E e = (E) elements[head]`. If `e == null`, it immediately knows the deque is empty and returns `null` without needing an extra branch to inspect `head == tail`.
2. If `null` were allowed as an element, `pollFirst()` returning `null` would be ambiguous: does `null` mean the queue was empty, or did it contain a stored `null`?
3. Additionally, modern JDK `ArrayDeque` iterators assert invariants such as `elements[tail] == null` to detect concurrent corruption. Allowing `null` would compromise these invariant checks.

### Q6: Can `ArrayDeque` be used as a general-purpose `List`? Why does it not implement `List<E>` despite being backed by an array?
**Deep Answer**:
No. `ArrayDeque` intentionally does **not** implement `java.util.List`.
1. **Violates Positional Indexing Guarantees**: The `List` interface requires fast, deterministic positional access: `get(int index)`, `set(int index, E element)`, and indexed insertion `add(int index, E element)`.
2. **Circular Offset Cost**: In a circular buffer, mapping a logical user index `i` to a physical array index requires modulo/wrap arithmetic: `(head + index) % capacity`.
3. **Expensive Arbitrary Insertions**: Inserting an element into the middle of a circular buffer requires shifting either the head segment left or the tail segment right, depending on which is closer to the insertion point, to minimize copies. Exposing this as a `List` would mislead developers into expecting standard `ArrayList` contiguous array performance, whereas `ArrayDeque` is specialized solely for boundary operations.

### Q7: What are "loitering objects" in circular buffers, and how does `ArrayDeque` ensure they do not cause GC memory leaks?
**Deep Answer**:
A "loitering object" (or obsolete reference) is an object reference stored in a data structure that will never be dereferenced by application logic again, but remains reachable by the Garbage Collector because the internal collection holding it is still alive.
In an array-backed ring buffer:
When an element is dequeued via `pollFirst()`, advancing `head` cursor (`head++`) logically excludes the element from the queue. However, if the backing array slot `elements[oldHead]` still holds the reference pointer to the dequeued object, the JVM Garbage Collector cannot reclaim that object, even if the rest of the application has discarded all references to it!
In long-running backend services with large objects (e.g., heavy payloads, byte arrays), this causes substantial silent memory leaks.
`ArrayDeque` prevents this by explicitly setting the array slot to `null` on every dequeue:
```java
es[h] = null; // Clear reference for GC
```
This breaks reachability from the GC Root, allowing the garbage collector to immediately sweep the memory during the next GC cycle.
