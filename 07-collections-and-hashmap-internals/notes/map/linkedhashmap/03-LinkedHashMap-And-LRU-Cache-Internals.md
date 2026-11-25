# <LinkedHashMap & LRU Cache Internals — Deep Dive>
> Author: raysi

---

## 1. Architectural Overview & Hierarchy

`java.util.LinkedHashMap<K, V>` is a specialized subclass of `HashMap<K, V>` that combines the $O(1)$ amortized lookup performance of a hash table with the deterministic iteration order of a doubly-linked list.

### Exact Package and Class Signature
```java
package java.util;

public class LinkedHashMap<K, V> extends HashMap<K, V>
    implements Map<K, V>, SequencedMap<K, V> { // SequencedMap added in JDK 21
    @java.io.Serial
    private static final long serialVersionUID = 3801124242820219131L;
    ...
}
```

### ASCII Inheritance & Structural Diagram
```
                     ┌───────────────────────┐
                     │   java.util.Map<K,V>  │
                     └───────────┬───────────┘
                                 │
                     ┌───────────▼───────────┐
                     │  AbstractMap<K,V>     │
                     └───────────┬───────────┘
                                 │ extends
                     ┌───────────▼───────────┐
                     │    HashMap<K,V>       │
                     └───────────┬───────────┘
                                 │ extends
                     ┌───────────▼───────────┐   JDK 21+
                     │  LinkedHashMap<K,V>   │─────────────► SequencedMap<K,V>
                     └───────────────────────┘
```

### Why Does `LinkedHashMap` Exist? What Problem Does It Solve?
1. **Predictable Iteration Order**: A raw `HashMap` has undefined iteration order. Elements appear in pseudo-random bucket order, which scrambles completely whenever the table doubles during `resize()`. `LinkedHashMap` guarantees predictable iteration order:
   - **Insertion-Order** (Default): Elements are visited in the exact sequence they were inserted.
   - **Access-Order** (LRU Mode): Elements are visited from least-recently-accessed to most-recently-accessed.
2. **First-Class Foundation for LRU Caches**: By exposing a single hook method—`removeEldestEntry(Map.Entry<K,V> eldest)`—`LinkedHashMap` enables memory-bounded Least-Recently-Used (LRU) caches in just a few lines of code.
3. **Optimized Linear Iteration**: Iterating over a `HashMap` requires scanning every single slot in `Node[] table` ($O(\text{capacity} + \text{size})$). If a map has capacity 1,000,000 but only 10 elements, `HashMap` iteration inspects 1,000,000 slots. `LinkedHashMap` traverses its internal doubly-linked list directly ($O(\text{size})$), touching only active elements!

---

## 2. Internal Data Structure & Memory Layout (JDK Source Code Level)

### Exact Internal Fields from JDK Source Code (`java.util.LinkedHashMap`)

```java
// The head (eldest) of the doubly linked list.
transient LinkedHashMap.Entry<K,V> head;

// The tail (youngest) of the doubly linked list.
transient LinkedHashMap.Entry<K,V> tail;

// The iteration ordering method: true for access-order, false for insertion-order.
final boolean accessOrder;
```

### The Linked Entry Structure: `LinkedHashMap.Entry<K,V>`
`LinkedHashMap` extends `HashMap.Node<K,V>` to add bidirectional pointers:
```java
static class Entry<K,V> extends HashMap.Node<K,V> {
    Entry<K,V> before, after;
    Entry(int hash, K key, V value, Node<K,V> next) {
        super(hash, key, value, next);
    }
}
```

### Dual Data Structure ASCII Memory Diagram
`LinkedHashMap` simultaneously maintains **TWO independent pointer topologies** over the exact same objects in the heap:
1. **Hash Table Collision Chaining** via `Node.next` (for $O(1)$ key lookup).
2. **Global Doubly-Linked List** via `Entry.before` and `Entry.after` (for deterministic traversal).

```
  ┌─────────────────────────────────────────────────────────────────────────────┐
  │                            Node<K,V>[] table (Array)                        │
  └───────┬─────────────────────────────┬─────────────────────────────┬─────────┘
          │ Index 0                     │ Index 1                     │ Index 2
          ▼                             ▼                             ▼
    ┌───────────┐                 ┌───────────┐                 ┌───────────┐
    │  Entry A  │                 │  Entry B  │                 │  Entry C  │
    │  hash: 16 │                 │  hash: 33 │                 │  hash: 50 │
    │  key: "A" │                 │  key: "B" │                 │  key: "C" │
    │  val: 100 │                 │  val: 200 │                 │  val: 300 │
    │  next:null│                 │  next: ───┼──► [Entry D]    │  next:null│
    └─────▲─────┘                 └─────▲─────┘    │ next: null │ └─────▲─────┘
          │                             │          └─────▲──────┘       │
          │                             │                │              │
══════════╪═════════════════════════════╪════════════════╪══════════════╪═════════
   DOUBLY-LINKED LIST TOPOLOGY (Order of Insertion or Access)           │
══════════╪═════════════════════════════╪════════════════╪══════════════╪═════════
          │                             │                │              │
 head ──► └──► [Entry A] ◄──before──────┼────────────────┼──────────────┘
                    │                   │                │              ▲
                  after ──────────────► └──► [Entry B]   │              │
                                                │        │              │
                                              after ───► └──► [Entry C] │
                                                                 │      │
                                                               after ──►└──► [Entry D] ◄── tail
```

### CPU Cache & Memory Overhead Analysis
- **Entry Size on 64-bit JVM with Compressed OOPs**:
  - `HashMap.Node`: 32 bytes.
  - `LinkedHashMap.Entry`:
    - Base `HashMap.Node` fields: 32 bytes
    - `Entry before` pointer: 4 bytes
    - `Entry after` pointer: 4 bytes
    - **Total = 40 bytes** (a 25% memory increase over `HashMap.Node`).
- **Cache Locality vs Array Iteration**:
  - While iteration time is $O(\text{size})$, the nodes of the doubly-linked list are allocated across arbitrary heap addresses over time. Iteration requires dereferencing `.after` pointers, resulting in **CPU cache line invalidations** and L2/L3 cache misses compared to contiguous array iteration (such as `ArrayList`).
  - However, because it skips empty bucket indices in sparse hash tables, iteration latency is significantly lower than sparse `HashMap` scans.

---

## 3. Step-by-Step Internal Working of Core Operations

### 1. Instantiation (Constructors)
`LinkedHashMap` provides standard constructors, plus an access-order constructor:
```java
public LinkedHashMap(int initialCapacity, float loadFactor, boolean accessOrder) {
    super(initialCapacity, loadFactor);
    this.accessOrder = accessOrder;
}
```
- `accessOrder = false`: Default. Order represents **insertion order**.
- `accessOrder = true`: Order represents **access order** (least-recently-used to most-recently-used).

### 2. Insertion Hook: `newNode()` & `linkNodeLast()`
When `HashMap.putVal()` creates a new entry, it does not call `new Node<>()` directly. Instead, it delegates to the factory method `newNode()`:
```java
// Overridden in LinkedHashMap
Node<K,V> newNode(int hash, K key, V value, Node<K,V> e) {
    LinkedHashMap.Entry<K,V> p = new LinkedHashMap.Entry<>(hash, key, value, e);
    linkNodeLast(p);
    return p;
}

private void linkNodeLast(LinkedHashMap.Entry<K,V> p) {
    LinkedHashMap.Entry<K,V> last = tail;
    tail = p;
    if (last == null)
        head = p;
    else {
        p.before = last;
        last.after = p;
    }
}
```
Every new insertion is automatically wired to the tail of the global doubly-linked list.

### 3. Access Hook: `afterNodeAccess()`
When a key is accessed via `get()`, `getOrDefault()`, or updated via `put()` with an existing key, `HashMap` calls the template method hook `afterNodeAccess(Node<K,V> e)`.
In `LinkedHashMap`, if `accessOrder == true`:
```java
void afterNodeAccess(Node<K,V> e) {
    LinkedHashMap.Entry<K,V> last;
    if (accessOrder && (last = tail) != e) {
        LinkedHashMap.Entry<K,V> p = (LinkedHashMap.Entry<K,V>)e, b = p.before, a = p.after;
        p.after = null;
        // Unlink p from its current position
        if (b == null)
            head = a;
        else
            b.after = a;
        if (a != null)
            a.before = b;
        else
            last = b;
        // Relink p at tail
        if (last == null)
            head = p;
        else {
            p.before = last;
            last.after = p;
        }
        tail = p;
        ++modCount; // CRITICAL: Access modifies structural modCount!
    }
}
```
**Step-by-Step Effect**:
1. Node $P$ is unlinked from between $B$ and $A$.
2. $B$ and $A$ are bridged together (`b.after = a; a.before = b;`).
3. $P$ is appended after current `tail`.
4. `tail` is updated to point to $P$.
5. The accessed element is now marked as the **most-recently-used**!

### 4. Removal Hook: `afterNodeRemoval()`
When an entry is removed via `map.remove(key)`:
```java
void afterNodeRemoval(Node<K,V> e) {
    LinkedHashMap.Entry<K,V> p = (LinkedHashMap.Entry<K,V>)e, b = p.before, a = p.after;
    p.before = p.after = null; // Prevent memory retention / GC leak
    if (b == null)
        head = a;
    else
        b.after = a;
    if (a == null)
        tail = b;
    else
        a.before = b;
}
```
The node is spliced out of the doubly-linked list, ensuring GC can collect it immediately.

### 5. LRU Cache Mechanics: `removeEldestEntry()` & `afterNodeInsertion()`
At the end of every `put()` operation, `HashMap` calls `afterNodeInsertion(boolean evict)`.
```java
void afterNodeInsertion(boolean evict) { // possibly remove eldest
    LinkedHashMap.Entry<K,V> first;
    if (evict && (first = head) != null && removeEldestEntry(first)) {
        K key = first.key;
        removeNode(hash(key), key, null, false, true);
    }
}

// Extension point in LinkedHashMap:
protected boolean removeEldestEntry(Map.Entry<K,V> eldest) {
    return false; // Default: Never evict
}
```
To create a fixed-capacity LRU cache, a developer simply overrides this method:
```java
@Override
protected boolean removeEldestEntry(Map.Entry<K,V> eldest) {
    return size() > MAX_CAPACITY;
}
```
When `size() > MAX_CAPACITY`, `head` (the least recently used node) is automatically evicted!

### 6. JDK 21 `SequencedMap` Integration
In Java 21, `LinkedHashMap` implements `SequencedMap<K, V>`:
- `firstEntry()`: Returns `head` without removal ($O(1)$).
- `lastEntry()`: Returns `tail` without removal ($O(1)$).
- `pollFirstEntry()`: Retrieves and removes `head` ($O(1)$).
- `pollLastEntry()`: Retrieves and removes `tail` ($O(1)$).
- `putFirst(K, V)` / `putLast(K, V)`: Inserts or relocates mapping directly to the front or back of the iteration sequence.
- `reversed()`: Returns a reverse-ordered view of the map in $O(1)$ without copying data.

---

## 4. Complexity & Behavioral Matrix

| Operation | `LinkedHashMap` | `HashMap` | `TreeMap` |
|:---|:---:|:---:|:---:|
| `get(K)` | $O(1)$ amortized | $O(1)$ amortized | $O(\log N)$ |
| `put(K, V)` | $O(1)$ amortized | $O(1)$ amortized | $O(\log N)$ |
| `remove(K)` | $O(1)$ amortized | $O(1)$ amortized | $O(\log N)$ |
| `firstEntry()` / `lastEntry()` (JDK 21) | $O(1)$ | N/A | $O(\log N)$ |
| Complete Iteration over all entries | **$O(N)$** | **$O(C + N)$** | $O(N)$ |

### Behavioral Characteristics
- **Iteration Order**:
  - `accessOrder = false`: Insertion order. (Re-inserting an existing key does NOT alter insertion order).
  - `accessOrder = true`: Access order. (Reads and writes move entry to the tail).
- **Null Keys / Values**: Permits **one** null key and multiple null values (identical to `HashMap`).
- **Duplicates**: Overwrites value on duplicate key.
- **Iterator Type**: **Fail-fast**.

---

## 5. Concurrency & Thread-Safety Mechanics

### The Critical Gotcha: `get()` is a Structural Modification in Access-Order Mode!
In standard collections, reading via `get()` is an idempotent, read-only operation.
**In `LinkedHashMap(..., ..., true)` (Access-Order Mode), `get()` is a STRUCTURAL MUTATION!**
1. Calling `map.get(key)` invokes `afterNodeAccess()`.
2. `afterNodeAccess()` rewires pointers (`before`, `after`, `head`, `tail`) and executes:
   ```java
   ++modCount;
   ```
3. **Implications**:
   - **`ConcurrentModificationException` during iteration**: Calling `map.get()` while iterating over keys, values, or entries will throw `ConcurrentModificationException` on the very next `iterator.next()` call!
   - **Race Conditions**: Concurrent `get()` calls without synchronization can corrupt the doubly-linked list, creating orphaned loops or dropping pointers, resulting in JVM crashes or infinite loops.

### Thread Safety Best Practices
- `LinkedHashMap` is not synchronized.
- **Synchronized Wrapper**:
  ```java
  Map<K, V> lru = Collections.synchronizedMap(
      new LinkedHashMap<K, V>(16, 0.75f, true) {
          @Override
          protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
              return size() > MAX_ENTRIES;
          }
      }
  );
  ```
  *Crucial Concurrency Rule*: When iterating over a synchronized `LinkedHashMap`, the iteration block MUST be manually synchronized on the map instance:
  ```java
  synchronized (lru) {
      for (Map.Entry<K, V> entry : lru.entrySet()) {
          // Process entry
      }
  }
  ```

### Why Production Systems Use Caffeine Instead of Synchronized `LinkedHashMap`
Under high thread contention (e.g. 64 worker threads handling 100k requests/sec):
- Synchronized `LinkedHashMap` forces all threads to queue up on a single mutex lock even for reads, destroying throughput.
- Modern high-performance caches (like **Caffeine** or **Guava Cache**) replace the synchronized doubly-linked list with **Window TinyLFU (W-TinyLFU)** and ring-buffer-based lock-free read event logging, achieving tens of millions of operations per second across multiple cores.

---

## 6. Production-Grade Java Code Verification

See the runnable demonstration files:
- [`AccessOrderVsInsertionOrderDemo.java`](../../../codes/collections/map/linkedhashmap/AccessOrderVsInsertionOrderDemo.java)
- [`LRUCacheImplementation.java`](../../../codes/collections/map/linkedhashmap/LRUCacheImplementation.java)

Key mechanics demonstrated in the code:
1. Observing iteration sequence differences between `HashMap`, `LinkedHashMap(accessOrder=false)`, and `LinkedHashMap(accessOrder=true)`.
2. Demonstrating that `get()` rewires the access order and triggers `ConcurrentModificationException` if invoked during iteration.
3. Implementing a thread-safe, bounded LRU cache with eviction listeners using `removeEldestEntry()`.
4. JDK 21 `SequencedMap` methods: `reversed()`, `firstEntry()`, `lastEntry()`.

---

## 7. Top 7 High-Signal Interview Questions & Deep Answers

### Q1: Why does iterating over `LinkedHashMap` take $O(N)$ while `HashMap` takes $O(C + N)$?
**Answer**:
- In `HashMap`, entries are scattered across an array of buckets (`Node<K,V>[] table`) of capacity $C$. The iterator must inspect every single index from $0$ to $C-1$ to see if a bucket contains nodes. If $C = 1,000,000$ and $N = 5$, `HashMap` iterates over 1,000,000 array elements.
- In `LinkedHashMap`, all entries are linked together by the `before` and `after` pointers of the global doubly-linked list. The iterator simply starts at `head` and follows `node.after` exactly $N$ times until it reaches `tail` (`after == null`). It never touches the bucket array during iteration, executing strictly in $O(N)$ time.

### Q2: Why does calling `map.get(key)` throw `ConcurrentModificationException` on an access-ordered `LinkedHashMap` during iteration?
**Answer**:
When `accessOrder == true`, `get(key)` calls `afterNodeAccess()`, which moves the accessed node to the tail of the doubly-linked list to reflect that it was recently used.
Because this alters the structure of the linked list, the method increments `modCount`:
```java
++modCount;
```
If an iterator is traversing the map, its internal `expectedModCount` now diverges from `modCount`. The subsequent call to `iterator.next()` detects this divergence and throws `ConcurrentModificationException`. To read without triggering CME, either finish the iterator first or use an insertion-ordered map.

### Q3: How does `LinkedHashMap` intercept `HashMap` operations to maintain its doubly-linked list without copying or rewriting `HashMap` code?
**Answer**:
`LinkedHashMap` leverages the **Template Method Design Pattern** via empty hook methods provided specifically by `HashMap` for this purpose:
1. `newNode(...)` / `newTreeNode(...)`: Factory methods overridden to instantiate `LinkedHashMap.Entry` instead of `HashMap.Node` and append the new entry to `tail`.
2. `afterNodeAccess(Node<K,V> e)`: Called by `get()`, `put()`, and `replace()`. `LinkedHashMap` overrides it to move the entry to `tail` when `accessOrder == true`.
3. `afterNodeInsertion(boolean evict)`: Called after every `putVal()`. `LinkedHashMap` checks `removeEldestEntry(head)` and evicts `head` if true.
4. `afterNodeRemoval(Node<K,V> e)`: Called by `removeNode()`. `LinkedHashMap` unlinks the node from its `before` and `after` neighbors.

### Q4: What is the memory overhead of `LinkedHashMap` compared to `HashMap`?
**Answer**:
- On a 64-bit JVM with compressed OOPs:
  - `HashMap.Node`: 32 bytes (12B header + 4B hash + 4B key + 4B value + 4B next + 4B padding).
  - `LinkedHashMap.Entry`: Extends `HashMap.Node` and adds two reference fields: `Entry before` (4B) and `Entry after` (4B). Total = 40 bytes.
  - In addition, the `LinkedHashMap` object header holds two references: `head` (4B) and `tail` (4B), plus a boolean `accessOrder` (1B + 3B padding).
- Thus, each entry consumes 8 additional bytes, representing a 25% heap memory increase over `HashMap.Node`.

### Q5: What happens if `removeEldestEntry()` returns true on an empty map or during initialization?
**Answer**:
`afterNodeInsertion(boolean evict)` contains defensive guards:
```java
if (evict && (first = head) != null && removeEldestEntry(first))
```
1. If the map is empty, `head` is `null`, so `removeEldestEntry` is **never called**.
2. If `removeEldestEntry` returns `true`, `first` (`head`) is deleted. If `removeEldestEntry` unconditionally returned `true`, the map would effectively retain only elements added in that single step or remain capped at size 0/1 depending on the exact condition.

### Q6: Why is `LinkedHashMap` NOT recommended for high-concurrency LRU caching in modern microservices?
**Answer**:
Because `LinkedHashMap` requires synchronization across both reads and writes:
1. In access-order mode, every `get()` requires a write to the doubly-linked list (`afterNodeAccess`), meaning readers cannot operate concurrently.
2. Wrapping it with `Collections.synchronizedMap` serializes all requests behind a single mutex monitor lock, creating a massive throughput bottleneck under high core counts.
3. Modern caches like **Caffeine** decouple cache hits from cache eviction using lock-free read buffers (similar to Java's `Disruptor` ring buffer), recording access events asynchronously and performing amortized eviction in background maintenance batches.

### Q7: How does Java 21's `SequencedMap` enhance `LinkedHashMap`?
**Answer**:
Prior to Java 21, getting the oldest or newest entry from a `LinkedHashMap` required creating an iterator: `map.entrySet().iterator().next()` ($O(1)$ amortized but allocates an iterator object).
With `SequencedMap`, `LinkedHashMap` provides direct, zero-allocation access:
- `firstEntry()` and `lastEntry()` return the head and tail in $O(1)$.
- `pollFirstEntry()` and `pollLastEntry()` atomically remove and return head or tail.
- `reversed()` provides a zero-allocation, bidirectional reversed view where `keySet()` iterates in reverse order.
- `putFirst(k, v)` and `putLast(k, v)` allow precise repositioning of entries in the sequence.
