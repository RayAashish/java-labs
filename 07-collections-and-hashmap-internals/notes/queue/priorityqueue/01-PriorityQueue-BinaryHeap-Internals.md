# <PriorityQueue — Binary Heap Internals Deep Dive>
> Author: raysi

## 1. Architectural Overview & Hierarchy

`java.util.PriorityQueue` is an unbounded priority queue based on a **priority heap**. Elements of the priority queue are ordered according to their **natural ordering** (implementing `Comparable<E>`) or by a `Comparator<E>` provided at queue construction time.

```
                    java.lang.Iterable<E>
                              │
                              ▼
                    java.util.Collection<E>
                              │
                              ▼
                     java.util.Queue<E>
                              │
                              ▼
                  java.util.AbstractQueue<E>
                              │
                              ▼
                 java.util.PriorityQueue<E>
           (Backed by Array-Based Binary Min-Heap)
```

### Exact Type Signature (JDK 21+)

```java
package java.util;

public class PriorityQueue<E> extends AbstractQueue<E>
    implements java.io.Serializable {
    
    @java.io.Serial
    private static final long serialVersionUID = -7720805057305804111L;
    
    private static final int DEFAULT_INITIAL_CAPACITY = 11;
    
    // Internal backing array storing heap nodes
    transient Object[] queue;
    
    // Current number of elements in priority queue
    int size;
    
    // The comparator, or null if using natural ordering (Comparable)
    @SuppressWarnings("serial")
    private final Comparator<? super E> comparator;
    
    // Structural modification counter for fail-fast iterators
    transient int modCount;
}
```

### Why Does This Class Exist? What Problem Does It Solve?

1. **Priority-Driven Retrieval vs. Arrival-Driven Retrieval**: Standard FIFO queues (`ArrayDeque`, `LinkedList`) dequeue elements strictly in the order they arrived. In computational systems, tasks have differing urgencies:
   - OS Process Scheduling (Fair share, priority preemption).
   - Graph algorithms (Dijkstra's Single-Source Shortest Path, Prim's Minimum Spanning Tree).
   - Data compression (Huffman coding tree construction).
   - Streaming analytics (Finding Top-$K$ elements or running median).
2. **Optimal Amortized Bounds ($O(\log N)$ Insert/Extract vs. $O(N)$ Sorted List)**:
   - Maintaining a completely sorted array or linked list requires $O(N)$ insertion time.
   - An unsorted list allows $O(1)$ insertion but requires $O(N)$ linear scans to find and remove the minimum element.
   - `PriorityQueue` utilizes a **Binary Min-Heap**, establishing a partially ordered tree where root retrieval is $O(1)$, extraction is $O(\log N)$, and insertion is $O(\log N)$ amortized.

---

## 2. Internal Data Structure & Memory Layout (JDK Source Code Level)

### The Complete Binary Heap Mapped to an Array

The binary heap in `PriorityQueue` is a **complete binary tree** represented as a contiguous, 0-indexed flat array (`Object[] queue`). A binary tree is complete when every level is fully filled, except possibly the last level, which is filled from left to right.

```
Binary Tree Logical View:
                     [10] (Index 0 - Root / Minimum)
                   /      \
                 /          \
              [15]          [20]
             /    \        /    \
           [30]  [25]    [40]   [50]
          Index 1 Index 2 Index 3 Index 4 Index 5 Index 6

Array Physical Layout in Memory:
┌────────┬────────┬────────┬────────┬────────┬────────┬────────┬────────┬────────┐
│   10   │   15   │   20   │   30   │   25   │   40   │   50   │  null  │  null  │
└────────┴────────┴────────┴────────┴────────┴────────┴────────┴────────┴────────┘
 Index 0  Index 1  Index 2  Index 3  Index 4  Index 5  Index 6  Index 7  Index 8
 (Root)   (L-Child) (R-Child)
```

### Array Index Navigation Arithmetic (JDK Source Code)

For any element at index `k`:
- **Left Child Index**: `(k << 1) + 1` (equivalent to `2 * k + 1`)
- **Right Child Index**: `(k << 1) + 2` (equivalent to `2 * k + 2` or `(k + 1) << 1`)
- **Parent Index**: `(k - 1) >>> 1` (unsigned bit-shift right equivalent to `Math.floor((k - 1) / 2)`)

### Heap Invariant Property
For every node `k` other than the root:
$$\text{queue}[k] \le \text{queue}[2k + 1] \quad \text{and} \quad \text{queue}[k] \le \text{queue}[2k + 2]$$
> **Crucial Reality**: A heap is **NOT sorted**. It only guarantees that the parent is less than or equal to its children. Sibling nodes have no ordering relation whatsoever.

---

### Memory Overhead & CPU Cache Analysis

```
PriorityQueue Instance in JVM Heap (64-bit JVM, +UseCompressedOops)
┌────────────────────────────────────────────────────────┐
│ PriorityQueue Object Header: 12 bytes                  │
│ queue reference:              4 bytes                  │
│ size (int):                   4 bytes                  │
│ comparator reference:         4 bytes                  │
│ modCount (int):               4 bytes                  │
│ Padding to 8-byte boundary:   4 bytes                  │
│ Total Instance Size:         32 bytes                  │
└────────────────────────────────────────────────────────┘
          │
          ▼ references
┌────────────────────────────────────────────────────────┐
│ Object[] queue Array Header: 16 bytes                  │
│ Element References (4 bytes each * capacity)           │
│ Padding to 8-byte boundary                             │
└────────────────────────────────────────────────────────┘
```

1. **Cache Locality vs. Pointer Chasing**:
   - The backing array is contiguous, making linear iterations fast.
   - However, during `siftUp()` and `siftDown()`, tree traversal doubles or halves indices (`k -> 2k + 1`).
   - For heaps exceeding the L1/L2 cache size (e.g., $N > 65,536$ elements), index doubling jumps across different 64-byte hardware cache lines, resulting in non-sequential memory accesses and L3 cache misses during deep sift-down operations.
2. **Null Element Restriction**:
   - `PriorityQueue` throws `NullPointerException` if a `null` element is inserted.
   - Reason 1: Natural comparison `e.compareTo(other)` would throw NPE when dereferencing `null`.
   - Reason 2: `poll()` returns `null` as an out-of-band indicator that the queue is empty.

---

## 3. Step-by-Step Internal Working of Core Operations

### 1. Instantiation & Bulk Construction (Floyd's Heapify Algorithm)

When constructed from an arbitrary collection, `PriorityQueue` uses **Floyd's Heapify Algorithm**, running in **$O(N)$ time**, rather than inserting elements one by one ($O(N \log N)$):

```java
// JDK 21 Source: java.util.PriorityQueue
private void heapify() {
    final Object[] es = queue;
    int n = size, i = (n >>> 1) - 1;
    final Comparator<? super E> cmp;
    if ((cmp = comparator) == null)
        for (; i >= 0; i--)
            siftDownComparable(i, (E) es[i], es, n);
    else
        for (; i >= 0; i--)
            siftDownUsingComparator(i, (E) es[i], es, n, cmp);
}
```

> **Why Floyd's Algorithm starts at `(size >>> 1) - 1`**:
> In a complete binary tree of size $N$, all nodes from index $\lfloor N / 2 \rfloor$ to $N - 1$ are **leaf nodes**. Leaf nodes have no children, so they already satisfy the heap invariant. Floyd's algorithm starts at the last non-leaf parent (`(size / 2) - 1`) and sifts down to index 0.

---

### 2. Insertion: `offer(E e)` and `siftUp()`

```java
public boolean offer(E e) {
    if (e == null)
        throw new NullPointerException();
    modCount++;
    int i = size;
    if (i >= queue.length)
        grow(i + 1);
    siftUp(i, e);
    size = i + 1;
    return true;
}
```

#### Step-by-Step `siftUpComparable` Mechanics (No Unnecessary Swaps):

```java
private static <T> void siftUpComparable(int k, T x, Object[] es) {
    Comparable<? super T> key = (Comparable<? super T>) x;
    while (k > 0) {
        int parent = (k - 1) >>> 1;
        Object e = es[parent];
        if (key.compareTo((T) e) >= 0)
            break;
        es[k] = e; // Demote parent down to position k
        k = parent; // Move cursor up to parent position
    }
    es[k] = key; // Place inserted element into final resting slot
}
```

```
siftUp Visual Flow (Inserting value 5 into min-heap):
Step 1: Place 5 at index k = 6 (new leaf)
Step 2: Parent at (6-1)>>>1 = 2 has value 20.
        Since 5 < 20, shift 20 down to index 6. Cursor k becomes 2.
Step 3: Parent of 2 at (2-1)>>>1 = 0 has value 10.
        Since 5 < 10, shift 10 down to index 2. Cursor k becomes 0.
Step 4: k == 0 (root reached). Store 5 at index 0. Done!
```

---

### 3. Deletion: `poll()` and `siftDown()`

```java
public E poll() {
    final Object[] es;
    final E result;
    if ((result = (E) ((es = queue)[0])) != null) {
        modCount++;
        final int n;
        final E x = (E) es[(n = --size)];
        es[n] = null; // GC Leak Prevention!
        if (n > 0) {
            final Comparator<? super E> cmp;
            if ((cmp = comparator) == null)
                siftDownComparable(0, x, es, n);
            else
                siftDownUsingComparator(0, x, es, n, cmp);
        }
    }
    return result;
}
```

#### Step-by-Step `siftDownComparable` Mechanics:

```java
private static <T> void siftDownComparable(int k, T x, Object[] es, int n) {
    Comparable<? super T> key = (Comparable<? super T>)x;
    int half = n >>> 1; // Loop while a non-leaf node
    while (k < half) {
        int child = (k << 1) + 1; // Assume left child is smaller
        Object c = es[child];
        int right = child + 1;
        if (right < n && ((Comparable<? super T>) c).compareTo((T) es[right]) > 0)
            c = es[child = right]; // Right child exists and is smaller!
        if (key.compareTo((T) c) <= 0)
            break; // Target position found!
        es[k] = c; // Promote smaller child up
        k = child;
    }
    es[k] = key; // Settle element into final position
}
```

---

### 4. Arbitrary Deletion: `remove(Object o)` and `removeAt(int i)`

When removing an arbitrary element (not the root):
1. Linear scan: `indexOf(o)` searches $O(N)$ elements.
2. `removeAt(i)` executes:
   - If `i == size - 1` (last element), set `queue[--size] = null`.
   - If `i < size - 1`, the last element `moved = queue[size - 1]` replaces slot `i`.
   - **Crucial JDK Subtlety**: The JDK calls `siftDown(i, moved)`.
   - **If `queue[i] == moved`** (meaning `moved` did NOT sift down), it calls `siftUp(i, moved)`!

> **Why does `removeAt()` require `siftUp()`?**
> The node being replaced at index `i` can have a value larger than `moved`. Because `moved` was taken from the bottom leaf of the heap, it might be smaller than the parent of node `i`. Thus, it must float upwards via `siftUp()`!

---

### 5. Resizing Mechanics (`grow()`)

```java
private void grow(int minCapacity) {
    int oldCapacity = queue.length;
    // Double capacity if small (< 64), else grow by 50%
    int newCapacity = oldCapacity + ((oldCapacity < 64) ?
                                     (oldCapacity + 2) :
                                     (oldCapacity >> 1));
    // Overflow-conscious code
    if (newCapacity - MAX_ARRAY_SIZE > 0)
        newCapacity = hugeCapacity(minCapacity);
    queue = Arrays.copyOf(queue, newCapacity);
}
```

- If `oldCapacity < 64`: `newCapacity = oldCapacity + oldCapacity + 2` ($> 100\%$ expansion).
- If `oldCapacity >= 64`: `newCapacity = oldCapacity + (oldCapacity >> 1)` ($50\%$ expansion).
- Reason: Small queues expand aggressively to amortize initial allocation overhead. Large queues grow conservatively at $1.5\times$ to avoid memory thrashing and excessive JVM heap footprint.

---

## 4. Complexity & Behavioral Matrix

| Operation | Average Case | Worst Case | Amortized | Notes |
| :--- | :--- | :--- | :--- | :--- |
| **`peek()` / `element()`** | $O(1)$ | $O(1)$ | $O(1)$ | Direct read of `queue[0]`. |
| **`offer(E)` / `add(E)`** | $O(1)$ | $O(\log N)$ | $O(1)$ | $\approx 75\%$ of insertions settle in bottom 2 levels. |
| **`poll()` / `remove()`** | $O(\log N)$ | $O(\log N)$ | $O(\log N)$ | Sifts down root from level 0 to leaf. |
| **`remove(Object)`** | $O(N)$ | $O(N)$ | $O(N)$ | Requires $O(N)$ linear array scan + $O(\log N)$ sift. |
| **`contains(Object)`** | $O(N)$ | $O(N)$ | $O(N)$ | Heap is not a search tree; must do linear scan. |
| **Heapify (Bulk Build)** | $O(N)$ | $O(N)$ | $O(N)$ | Floyd’s algorithm runs in linear time. |
| **`iterator()` Traversal** | $O(N)$ | $O(N)$ | $O(N)$ | **Iterates array indices; NOT sorted order!** |

### Behavioral Attributes
- **Ordering**: Priority order at head only. No full sort.
- **Null Allowance**: **Strictly Forbidden** (`NullPointerException`).
- **Duplicates**: Permitted. If elements have equal priority, ordering is non-deterministic.
- **Iterator**: Fail-fast (`ConcurrentModificationException`). Does **NOT** yield elements in priority sequence.

---

## 5. Concurrency & Thread-Safety Mechanics

`PriorityQueue` is **not synchronized** and not thread-safe.

### Fail-Fast Iterators & `modCount`
- Any structural mutation (`offer`, `poll`, `remove`, `clear`) increments `modCount`.
- The iterator initializes `expectedModCount = modCount`.
- If an element is added or removed during iteration without using the iterator's own `remove()` method, the next `next()` call detects `modCount != expectedModCount` and immediately throws `ConcurrentModificationException`.

### Modern Concurrent Alternative: `PriorityBlockingQueue`

```java
package java.util.concurrent;

public class PriorityBlockingQueue<E> extends AbstractQueue<E>
    implements BlockingQueue<E>, java.io.Serializable {
    
    private final ReentrantLock lock = new ReentrantLock();
    private final Condition notEmpty = lock.newCondition();
    
    // CAS spinlock field for array reallocation without blocking readers
    private static final VarHandle ALLOCATIONSPINLOCK;
    private transient volatile int allocationSpinLock;
}
```

#### Why `PriorityBlockingQueue` Outperforms Naive Synchronized Wrappers:
1. **Separation of Allocation and Queue Locks**: During `grow()`, `PriorityBlockingQueue` releases its main `ReentrantLock` and uses a CAS on `allocationSpinLock`. This allows ongoing `poll()` and `peek()` operations to proceed on the old array while the new array is allocated by the growing thread!
2. **Blocking Dequeue**: Provides `take()` which parks the consumer thread via `notEmpty.await()` until a producer pushes an element.

---

## 6. Production-Grade Java Code Verification

```java
// Example verification snippet
PriorityQueue<Integer> pq = new PriorityQueue<>(Collections.reverseOrder());
pq.offer(42);
pq.offer(10);
assert pq.peek() == 42;
```

---

## 7. Top 7 High-Signal Interview Questions & Deep Answers

### Q1: Why does `PriorityQueue.iterator()` NOT return elements in sorted/priority order, and how must you iterate if priority order is required?
**Deep Answer**:
The binary heap invariant only guarantees that for any node at index $k$, its children at $2k+1$ and $2k+2$ are greater than or equal to the node. No ordering relationship exists between siblings or between nodes across different branches at the same tree depth. The iterator of `PriorityQueue` iterates sequentially over the internal flat array `queue[0]` through `queue[size-1]`.
Because an array-backed heap is only a **partially ordered tree**, walking indices $0 \to N-1$ produces a level-by-level breadth-first traversal of the heap tree, which is **not sorted**.
To consume elements in true priority order, the caller must destructively invoke `poll()` until empty (an $O(N \log N)$ operation), or copy the heap to an array and sort it (`Arrays.sort()`).

### Q2: Explain the growth formula of `PriorityQueue` (`oldCapacity < 64 ? double + 2 : 50%`) and why it changes at 64.
**Deep Answer**:
In JDK source code:
```java
int newCapacity = oldCapacity + ((oldCapacity < 64) ? (oldCapacity + 2) : (oldCapacity >> 1));
```
When `oldCapacity < 64`, the heap grows by `oldCapacity + 2`, effectively doubling capacity plus 2 (e.g., $11 \to 24 \to 50 \to 102$). At small capacities, the cost of array allocation and garbage collection is negligible, but reallocating frequently hurts CPU performance; doubling amortizes resizing overhead quickly.
Once capacity exceeds 64 elements, doubling would result in excessive memory over-allocation (e.g., jumping from 16M elements directly to 32M elements wastes 64 MB of heap). Growing by 50% (`oldCapacity >> 1`, identical to `ArrayList`) strikes an optimal balance between amortized $O(1)$ insertions and memory retention pressure.

### Q3: Why is building a `PriorityQueue` from an existing collection using Floyd's algorithm $O(N)$, whereas adding elements one-by-one is $O(N \log N)$?
**Deep Answer**:
- **Adding one-by-one (`siftUp`)**: Each added element is placed at a leaf and potentially sifts up the tree. The bottom level contains $\approx N / 2$ nodes, and each can traverse height $h = \log_2 N$. The summation is:
  $$\sum_{i=1}^N \log i \approx N \log N$$
- **Floyd's Heapify (`siftDown` bottom-up)**: Floyd's algorithm starts at $\lfloor N / 2 \rfloor - 1$ and works downward to index 0, performing `siftDown`.
  - The bottom level ($N/2$ nodes) does 0 swaps.
  - The level above ($N/4$ nodes) does at most 1 swap.
  - The level above that ($N/8$ nodes) does at most 2 swaps.
  - In general, at height $h$ from the bottom, there are $\le \lceil N / 2^{h+1} \rceil$ nodes, each traversing at most $h$ edges down:
  $$\text{Total Cost} = \sum_{h=1}^{\lfloor \log N \rfloor} \frac{N}{2^{h+1}} \cdot h = \frac{N}{2} \sum_{h=1}^\infty \frac{h}{2^h} = \frac{N}{2} \cdot 2 = O(N)$$
  Because the majority of nodes are near the bottom of the tree and move down very few levels, the total work is strictly linear $O(N)$.

### Q4: What happens if an object already inside a `PriorityQueue` has its mutated field changed? Does the heap rebalance automatically?
**Deep Answer**:
**No, the heap invariant is silently corrupted.**
`PriorityQueue` has no change listeners, reactive observers, or property change adapters on the objects stored in its array. It only compares elements during structural mutations (`offer`, `poll`, `remove`).
If an element at index 4 is mutated such that its priority becomes smaller than the root, `queue[4]` remains at index 4. `peek()` will continue returning the old root at `queue[0]`. Subsequent `poll()` operations will produce undefined or out-of-order extractions.
**Remedy**: If priority must change, the application must explicitly remove the object (`pq.remove(obj)`), update its fields, and re-insert it (`pq.offer(obj)`), or use an indexable priority queue / Fibonacci heap.

### Q5: In `removeAt(int i)`, why does the JDK implementation call `siftDown` first, and then conditionally call `siftUp`? Under what conditions is `siftUp` needed?
**Deep Answer**:
When an arbitrary element at index $i$ is removed, the vacant position must be filled by the last element of the heap, `moved = queue[size - 1]`.
Because `moved` was at the very bottom leaf of the heap:
1. It might be larger than its new children at position $i$. Calling `siftDown(i, moved)` pushes it down into the correct subtree.
2. However, if `siftDown` determined that `moved` was not larger than any child, `queue[i]` will simply be assigned `moved`. But wait! What if `moved` is **smaller than the parent of position $i$**?
Remember: `moved` came from a completely different leaf branch in the tree. There is no mathematical guarantee that an arbitrary leaf in branch B is greater than or equal to the parent node in branch A.
Therefore, if `queue[i] == moved` after `siftDown`, the JDK checks if `siftUp(i, moved)` is required. If `moved` floats up, `removeAt()` returns `moved` to inform the caller (such as an iterator) that an element was shifted ahead of the current scan cursor.

### Q6: Why does `PriorityQueue` prohibit `null` elements even though `Comparable` could theoretically be passed a custom Comparator that handles `null`?
**Deep Answer**:
There are two reasons:
1. **Contractual Ambiguity with `poll()`**: `Queue.poll()` returns `null` to indicate that the queue contains no elements. If `null` were an allowed payload, a consumer calling `poll()` could not distinguish between a queue with a single `null` task and an empty queue without querying `isEmpty()`.
2. **Defensive Comparison Guarantees**: A default `PriorityQueue` relies on `Comparable<? super E>`. If `null` were allowed, comparing `null.compareTo(x)` throws a JVM-level `NullPointerException` (null pointer dereference). Even if a custom `Comparator` were designed to order nulls (`Comparator.nullsFirst()`), supporting nulls selectively based on whether a comparator is null-safe would introduce fragile, inconsistent API semantics. Thus, `PriorityQueue.offer()` unconditionally executes `if (e == null) throw new NullPointerException()`.

### Q7: How does `PriorityBlockingQueue` optimize array growth concurrently without blocking consumers reading from the queue?
**Deep Answer**:
In `PriorityBlockingQueue`, enqueue and dequeue operations synchronize on a single `ReentrantLock`.
However, array resizing (`grow()`) can take significant time due to memory allocation (`new Object[newCap]`). If the main lock were held during array allocation, all consumer threads calling `poll()` or `take()` would be blocked from dequeuing existing elements from the old array!
To solve this, Doug Lea engineered a two-phase growth protocol:
1. The thread detecting capacity exhaustion **releases the main `ReentrantLock`**.
2. It attempts to acquire a spinlock using a CAS on the `allocationSpinLock` integer field:
   ```java
   if (allocationSpinLock == 0 && ALLOCATIONSPINLOCK.compareAndSet(this, 0, 1))
   ```
3. While holding this lightweight allocation lock (with the main lock released!), it allocates the new array `new Object[newCap]`.
4. It then **re-acquires the main `ReentrantLock`** and copies the elements from the old array to the new array using `System.arraycopy()`.
5. Finally, it resets `allocationSpinLock = 0`.
This allows concurrent consumers to continue reading and dequeuing from the old array uninterrupted while memory allocation is underway.
