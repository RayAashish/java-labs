# ConcurrentHashMap — CAS, Bucket Locking & Memory Architecture
> Author: raysi

---

## 1. Architectural Overview & Hierarchy

`java.util.concurrent.ConcurrentHashMap<K,V>` is the industry-standard, high-throughput, thread-safe hash map implementation in the Java platform. Designed by Doug Lea, it provides full thread safety for all read and write operations while supporting massive concurrent throughput without ever locking the entire map.

### Exact Class Signature

```java
package java.util.concurrent;

public class ConcurrentHashMap<K,V>
    extends AbstractMap<K,V>
    implements ConcurrentMap<K,V>, Serializable
```

### ASCII Inheritance & Hierarchy Diagram

```
                       ┌────────────────────────┐
                       │     java.util.Map      │
                       └───────────┬────────────┘
                                   │
                                   ▼
                       ┌────────────────────────┐
                       │ java.util.concurrent.  │
                       │     ConcurrentMap      │
                       └───────────┬────────────┘
                                   │
       ┌────────────────────────┐  │
       │ java.util.AbstractMap  │  │
       └───────────┬────────────┘  │
                   │               │
                   └─────────┬─────┘
                             │ extends & implements
                             ▼
         ┌───────────────────────────────────────┐
         │ java.util.concurrent.ConcurrentHashMap│
         └───────────────────────────────────────┘
```

### Architectural Paradigm Shift: JDK 7 vs JDK 8+

To understand modern `ConcurrentHashMap`, one must recognize why the JDK 7 design was completely discarded in JDK 8:

```
 JDK 7 Architecture: Segment-Based Locking (Coarse Sharding)
 ┌────────────────────────────────────────────────────────┐
 │ ConcurrentHashMap                                      │
 │   segments[0]  --> ReentrantLock -> HashEntry[] table  │
 │   segments[1]  --> ReentrantLock -> HashEntry[] table  │
 │   ...                                                  │
 │   segments[15] --> ReentrantLock -> HashEntry[] table  │
 └────────────────────────────────────────────────────────┘
 Concurrency Level fixed at construction (default 16).
 At most 16 threads could write concurrently across the entire map!
 ──────────────────────────────────────────────────────────
 JDK 8+ Modern Architecture: CAS + Synchronized Bucket Head
 ┌────────────────────────────────────────────────────────┐
 │ ConcurrentHashMap                                      │
 │   table -> Node<K,V>[] (Flat table, lazy allocation)   │
 │                                                        │
 │   Bucket [0]: Empty     ==> CAS (Lock-Free Insertion)  │
 │   Bucket [1]: List Head ==> synchronized(headNode)     │
 │   Bucket [2]: TreeBin   ==> synchronized(treeBinRoot)  │
 │   Bucket [3]: FwdNode   ==> Hash = -1 (Resizing help)  │
 └────────────────────────────────────────────────────────┘
 Concurrency Level equals table capacity (e.g. 65,536 buckets = 65,536 concurrent writers!).
 Lock-free reads across the entire map using volatile reads.
```

| Dimension | JDK 7 Architecture | JDK 8+ Modern Architecture |
| :--- | :--- | :--- |
| **Locking Mechanism** | Segment-level `ReentrantLock` | CAS on empty buckets + `synchronized(firstNode)` on collision |
| **Max Concurrent Writers** | Capped at `concurrencyLevel` (default 16) | Theoretically unbounded (equals number of table buckets) |
| **Memory Footprint** | Extremely heavy (16 `Segment` objects + 16 inner tables) | Flat `Node[]` array; lazy allocation; low overhead |
| **Worst-Case Collisions** | Singly linked list degradation ($O(n)$) | Red-Black Treeification (`TreeBin`) ($O(\log n)$) |
| **Resizing Coordination** | Per-segment lock; single-threaded per segment | Multi-threaded cooperative parallel resizing (`helpTransfer`) |

---

## 2. Internal Data Structure & Memory Layout (JDK Source Code Level)

### Exact Internal Fields from JDK Source Code

```java
// Flat array of bucket chains/trees. Allocated lazily upon first insertion.
transient volatile Node<K,V>[] table;

// Transition table used exclusively during concurrent resizing.
private transient volatile Node<K,V>[] nextTable;

// Base counter used when there is no thread contention on size updates.
private transient volatile long baseCount;

// Control flag: Table initialization and resizing control.
//   -1: Table is currently being initialized by a thread.
//  -(1 + n): Table is resizing with n active helper threads.
//   0: Default initial state (uninitialized).
//  >0: Target capacity before initialization, or threshold after.
private transient volatile int sizeCtl;

// Striped counter array (LongAdder architecture) for high-contention sizing.
private transient volatile CounterCell[] counterCells;

// Special Hash Sentinels (Negative hash values reserved for control nodes):
static final int MOVED     = -1; // Bucket has been migrated; points to ForwardingNode
static final int TREEBIN   = -2; // Bucket is converted to Red-Black tree (TreeBin root)
static final int RESERVED  = -3; // Temporary reservation marker (computeIfAbsent)
static final int HASH_BITS = 0x7fffffff; // Mask to preserve positive 31 bits
```

### The 4 Internal Node Types

1. **`Node<K,V>`:** Basic singly linked list entry.
   ```java
   static class Node<K,V> implements Map.Entry<K,V> {
       final int hash;
       final K key;
       volatile V val;       // Volatile guarantees read visibility without locks
       volatile Node<K,V> next; // Volatile guarantees pointer chain visibility
   }
   ```
2. **`TreeNode<K,V>`:** Node used inside a Red-Black tree. Extends `Node<K,V>` with `parent`, `left`, `right`, and `red` fields.
3. **`TreeBin<K,V>`:** The root wrapper placed directly into `table[i]`. Contains the root of the `TreeNode` Red-Black tree and maintains a custom reader-writer state bitmask so tree rotations do not block concurrent readers.
4. **`ForwardingNode<K,V>`:** Placed into `table[i]` during resize (`hash = -1`). Holds a pointer to `nextTable`. When other threads encounter it, they assist the resize via `helpTransfer()`.

### ASCII Memory & Bucket Layout

```
 ConcurrentHashMap Instance (Heap)
 ┌────────────────────────────────────────────────────────┐
 │ table: [Ref 0x1000]                                    │
 │ baseCount: 1420                                        │
 │ sizeCtl: 24576 (Next resize threshold = 32768 * 0.75)   │
 │ counterCells: [Ref 0x2000] (Striped LongAdder cells)   │
 └──────────────────────────┬─────────────────────────────┘
                            │
                            ▼
  Node<K,V>[] table (Array of volatile references)
 ┌──────────┬──────────┬──────────┬──────────┬──────────┬──────────┐
 │ Bucket 0 │ Bucket 1 │ Bucket 2 │ Bucket 3 │ Bucket 4 │ Bucket 5 │
 └────┬─────┴────┬─────┴────┬─────┴────┬─────┴────┬─────┴────┬─────┘
      │          │          │          │          │          │
      ▼          ▼          ▼          ▼          │          ▼
    null      Node<K,V>   TreeBin   Forwarding    │       Node<K,V>
  (Unused)   (List Head)  (hash=-2) Node(hash=-1) │     (List Head)
                 │          │          │          │          │
   Thread A:     │          │          │          │          │
  Atomic CAS  synchronized │          │          │     synchronized
  to insert!   (head)      │          │          │       (head)
                 │          ▼          ▼          │          │
                 │      TreeNode    nextTable     ▼          ▼
                 │       (R-B)     (Resizing)    null      Node<K,V>
                 ▼                               (Unused)    │
              Node<K,V>                                      ▼
                 │                                         null
                 ▼
                null
```

### Memory Mechanics: Volatile Array References & VarHandle / Unsafe

In Java, declaring an array `volatile` (`volatile Node<K,V>[] table`) makes the **array reference itself volatile**, but does **NOT** make individual array elements volatile!
To achieve true volatile semantics on array elements, `ConcurrentHashMap` accesses buckets exclusively through `VarHandle` (or JVM `Unsafe`):
- **`tabAt(tab, i)`:** Performs a volatile read (`ARRAY.getVolatile(tab, i)`), guaranteeing acquire semantics without locking.
- **`casTabAt(tab, i, c, v)`:** Executes an atomic Compare-And-Swap on `tab[i]` (`ARRAY.compareAndSet(tab, i, c, v)`).
- **`setTabAt(tab, i, v)`:** Executes a volatile/release store (`ARRAY.setVolatile(tab, i, v)`).

### False Sharing & Cache Line Bouncing Prevention

When thousands of threads mutate entries concurrently, updating a single shared counter variable (`baseCount`) causes intense **cache line bouncing** across CPU cores (MESI protocol invalidation cycles).
`ConcurrentHashMap` uses the `CounterCell` array (identical to `LongAdder`):
```java
@jdk.internal.vm.annotation.Contended
static final class CounterCell {
    volatile long value;
    CounterCell(long x) { value = x; }
}
```
The `@Contended` annotation injects 128 bytes of padding around each `CounterCell`, isolating each thread's cell onto its own L1/L2 CPU cache line. This eliminates false sharing completely, allowing concurrent updates to scale linearly with CPU cores.

---

## 3. Step-by-Step Internal Working of Core Operations

### 3.1 Instantiation & Lazy Initialization

`new ConcurrentHashMap<>()` does **NOT** allocate the bucket array. The constructor merely sanitizes capacity arguments and sets `sizeCtl`:
```java
public ConcurrentHashMap(int initialCapacity, float loadFactor, int concurrencyLevel) {
    if (!(loadFactor > 0.0f) || initialCapacity < 0 || concurrencyLevel <= 0)
        throw new IllegalArgumentException();
    if (initialCapacity < concurrencyLevel)
        initialCapacity = concurrencyLevel;
    long size = (long)(1.0 + (long)initialCapacity / loadFactor);
    int cap = (size >= (long)MAXIMUM_CAPACITY) ? MAXIMUM_CAPACITY : tableSizeFor((int)size);
    this.sizeCtl = cap;
}
```
The array is allocated on the very first invocation of `putVal()` via `initTable()`:
```java
private final Node<K,V>[] initTable() {
    Node<K,V>[] tab; int sc;
    while ((tab = table) == null || tab.length == 0) {
        if ((sc = sizeCtl) < 0)
            Thread.yield(); // Lost initialization race; back off
        else if (U.compareAndSetInt(this, SIZECTL, sc, -1)) { // Acquire lock via CAS(-1)
            try {
                if ((tab = table) == null || tab.length == 0) {
                    int n = (sc > 0) ? sc : DEFAULT_CAPACITY;
                    @SuppressWarnings("unchecked")
                    Node<K,V>[] nt = (Node<K,V>[])new Node<?,?>[n];
                    table = tab = nt;
                    sc = n - (n >>> 2); // Threshold = 0.75 * n
                }
            } finally {
                sizeCtl = sc;
            }
            break;
        }
    }
    return tab;
}
```

### 3.2 Insertion (`putVal()`) Step-by-Step

When `put(key, value)` is called:
1. **Null Validation:** Rejects null keys and null values immediately:
   ```java
   if (key == null || value == null) throw new NullPointerException();
   ```
2. **Hash Spreading:** `int hash = spread(key.hashCode());`
   ```java
   static final int spread(int h) {
       return (h ^ (h >>> 16)) & HASH_BITS;
   }
   ```
   Reversible XOR spreads high-order bits into low-order bits, and `& HASH_BITS` ensures the hash is non-negative.
3. **The Infinite Retry Loop (`for (Node<K,V>[] tab = table;;)`):**
   - **Case A: Table Uninitialized:** Call `initTable()`.
   - **Case B: Bucket is Empty (`tabAt(tab, i) == null`):**
     - Attempt lock-free insertion using CAS:
       ```java
       if (casTabAt(tab, i, null, new Node<K,V>(hash, key, value)))
           break; // Successfully inserted without ANY locks!
       ```
     - If CAS fails (another thread won the race), loop retries.
   - **Case C: Bucket is Migrating (`f.hash == MOVED`):**
     - Another thread is resizing the table. Current thread cooperates by calling `helpTransfer(tab, f)`.
   - **Case D: Bucket Collision (`tabAt(tab, i) != null`):**
     - Acquire monitor lock **ONLY on the first node of the bucket chain**:
       ```java
       synchronized (f) {
           if (tabAt(tab, i) == f) { // Double-checked locking
               if (f.hash >= 0) {    // Singly linked list
                   binCount = 1;
                   for (Node<K,V> e = f;; ++binCount) {
                       K ek;
                       if (e.hash == hash && ((ek = e.key) == key || (ek != null && key.equals(ek)))) {
                           oldVal = e.val;
                           if (!onlyIfAbsent) e.val = value;
                           break;
                       }
                       Node<K,V> pred = e;
                       if ((e = e.next) == null) {
                           pred.next = new Node<K,V>(hash, key, value);
                           break;
                       }
                   }
               } else if (f instanceof TreeBin) { // Red-Black TreeBin
                   binCount = 2;
                   Node<K,V> p = ((TreeBin<K,V>)f).putTreeVal(hash, key, value);
                   if (p != null) {
                       oldVal = p.val;
                       if (!onlyIfAbsent) p.val = value;
                   }
               }
           }
       }
     ```
4. **Treeification Check:**
   - If `binCount >= TREEIFY_THRESHOLD (8)`:
     - Calls `treeifyBin(tab, i)`.
     - If `tab.length < MIN_TREEIFY_CAPACITY (64)`, it resizes the table rather than treeifying.
     - If capacity $\ge 64$, converts the list into a `TreeBin`.
5. **Increment Counter & Resize Trigger:**
   - Calls `addCount(1L, binCount)`. Adds count to striped `CounterCell` and checks if resizing is required.

### 3.3 Retrieval (`get()`) — 100% Lock-Free

`get(key)` never acquires a lock, never blocks, and never spins:
```java
public V get(Object key) {
    Node<K,V>[] tab; Node<K,V> e, p; int n, eh; K ek;
    int h = spread(key.hashCode());
    if ((tab = table) != null && (n = tab.length) > 0 &&
        (e = tabAt(tab, (n - 1) & h)) != null) { // Volatile read
        if ((eh = e.hash) == h) {
            if ((ek = e.key) == key || (ek != null && key.equals(ek)))
                return e.val; // First node match
        }
        else if (eh < 0) // Special node: TreeBin or ForwardingNode
            return (p = e.find(h, key)) != null ? p.val : null;
        while ((e = e.next) != null) { // Traverse volatile next pointers
            if (e.hash == h &&
                ((ek = e.key) == key || (ek != null && key.equals(ek))))
                return e.val;
        }
    }
    return null;
}
```
- Because `val` and `next` are `volatile`, writes made by any thread are instantly visible to reading threads according to the Java Memory Model happens-before rules.
- Concurrent reads execute in parallel with concurrent writes across different or even the same buckets.

### 3.4 Multi-Threaded Cooperative Resizing (`transfer()`)

When `table` capacity is exceeded, resizing allocates `nextTable = new Node[n << 1]`.
Resizing in `ConcurrentHashMap` is unique: it is **multi-threaded and cooperative**:
1. The table is split into "strides" (minimum 16 buckets).
2. The initial thread claims a stride by decrementing `transferIndex` via CAS.
3. For each bucket in its stride:
   - Locks bucket head `synchronized(f)`.
   - Splitting entries into `low` and `high` chains using `hash & n` (same as HashMap).
   - Writes `low` chain to `nextTable[i]` and `high` chain to `nextTable[i + n]`.
   - Overwrites the old bucket with a `ForwardingNode(nextTable)`.
4. Any other thread that attempts to insert or update an entry in a bucket marked with a `ForwardingNode` (`hash == MOVED`) calls `helpTransfer()`. It acquires its own stride and helps migrate the remaining buckets!
5. When all helper threads finish, `table` is set to `nextTable`.

### 3.5 Atomic Size Calculation (`mappingCount()` vs `size()`)

```java
final long sumCount() {
    CounterCell[] cs = counterCells;
    long sum = baseCount;
    if (cs != null) {
        for (CounterCell c : cs)
            if (c != null)
                sum += c.value;
    }
    return sum;
}
```
- **`size()`**: Returns `(int)sumCount()`, capped at `Integer.MAX_VALUE`.
- **`mappingCount()`**: Introduced in Java 8. Returns `long sumCount()`. Always prefer `mappingCount()` in modern high-scale distributed systems where maps exceed $2 \times 10^9$ entries.

---

## 4. Complexity & Behavioral Matrix

| Operation | Average Time | Worst Case Time | Amortized Time | Synchronization Mechanism |
| :--- | :--- | :--- | :--- | :--- |
| `get(K)` | $O(1)$ | $O(\log n)$ (TreeBin) | $O(1)$ | **100% Lock-Free** (Volatile Read) |
| `containsKey(K)` | $O(1)$ | $O(\log n)$ | $O(1)$ | **100% Lock-Free** (Volatile Read) |
| `put(K, V)` (Empty Bucket) | $O(1)$ | $O(1)$ | $O(1)$ | **Lock-Free** (Atomic CAS) |
| `put(K, V)` (Collision) | $O(1)$ | $O(\log n)$ | $O(1)$ | **Fine-grained Bucket Lock** (`synchronized(firstNode)`) |
| `remove(K)` | $O(1)$ | $O(\log n)$ | $O(1)$ | **Fine-grained Bucket Lock** (`synchronized(firstNode)`) |
| `computeIfAbsent(K, Fn)` | $O(1)$ | $O(\log n)$ | $O(1)$ | `synchronized(firstNode)` or `ReservationNode` |
| `mappingCount()` / `size()` | $O(\text{numCores})$ | $O(\text{numCores})$ | $O(\text{numCores})$ | Lock-Free sum of `baseCount` + `CounterCell[]` |
| Full Table Iteration | $O(n + \text{cap})$ | $O(n + \text{cap})$ | $O(1)$ per step | Weakly consistent, non-blocking |

### Behavioral Characteristics
- **Ordering:** Unordered. Bucket positions depend on 32-bit hash spreading.
- **Null Keys & Null Values:** **STRICTLY PROHIBITED**. Throws `NullPointerException`.
- **Duplicates:** Keys are unique; values overwrite unless atomic conditional methods (`putIfAbsent`, `merge`) specify otherwise.
- **Iterator Type:** **Weakly Consistent**. Iterators never throw `ConcurrentModificationException`, guarantee traversal of elements existing at creation, and may or may not reflect updates made after iterator creation.

---

## 5. Concurrency & Thread-Safety Mechanics

### Why Are Null Keys and Values Strictly Disallowed?

Doug Lea documented this architectural decision with precision:
In non-concurrent collections (`HashMap`), `map.get(key) == null` has two interpretations:
1. The key was never inserted.
2. The key was explicitly inserted with value `null`.
In `HashMap`, a caller distinguishes between these cases by calling `map.containsKey(key)`.

In a multi-threaded concurrent container, this check is **fatally broken**:
```java
// THREAD 1
if (map.containsKey(k)) {
    // Context switch: Thread 2 removes key 'k' RIGHT HERE!
    V val = map.get(k); // Returns null! Did it contain null, or was it removed?
}
```
Between `containsKey()` and `get()`, concurrent mutations destroy deterministic logic. By prohibiting `null`, `map.get(k) == null` **unambiguously means the key is absent**.

### Weakly Consistent Iterators

When `iterator()` is called on `keySet()`, `values()`, or `entrySet()`:
- The iterator advances through table buckets without locking the table or copying the underlying data.
- It will reflect the state of the map at the time of construction.
- It will **never throw `ConcurrentModificationException`**.
- Mutations that occur on buckets already traversed will not be seen; mutations on pending buckets may be seen.

### Atomic Compound Operations

To prevent check-then-act race conditions without external locking, `ConcurrentHashMap` provides built-in atomic methods executed under bucket-level synchronization:
1. `putIfAbsent(K key, V value)`: Atomically puts only if key is not mapped.
2. `computeIfAbsent(K key, Function<? super K, ? extends V> mappingFunction)`: Computes value lazily and atomically inserts it if absent.
3. `compute(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction)`: Atomically updates or removes an existing mapping.
4. `merge(K key, V value, BiFunction<? super V, ? super V, ? extends V> remappingFunction)`: Combines existing value with new value (e.g. counters, aggregations).

---

## 6. Production-Grade Java Code Verification

```java
package com.raysi.jcf.map.concurrenthashmap.notes;

import java.util.concurrent.*;
import java.util.*;

/**
 * Verification of ConcurrentHashMap internals:
 *  1. Weakly consistent iterator behavior under heavy concurrent mutation.
 *  2. Atomic computeIfAbsent and merge semantics.
 *  3. MappingCount vs size for 64-bit scale.
 * 
 * @author raysi
 */
public class ConcurrentHashMapInternalVerification {

    public static void main(String[] args) throws InterruptedException {
        ConcurrentHashMap<String, Integer> map = new ConcurrentHashMap<>();

        System.out.println("=== 1. Weakly Consistent Iterator Verification ===");
        map.put("A", 1);
        map.put("B", 2);
        map.put("C", 3);

        Iterator<Map.Entry<String, Integer>> it = map.entrySet().iterator();
        System.out.println("Starting iteration while concurrently inserting 'D'...");

        while (it.hasNext()) {
            Map.Entry<String, Integer> entry = it.next();
            System.out.println("Visited: " + entry.getKey() + " -> " + entry.getValue());
            if (entry.getKey().equals("A")) {
                // Concurrently insert while iterating!
                map.put("D", 4);
                map.put("E", 5);
            }
        }
        System.out.println("Iteration completed cleanly with NO ConcurrentModificationException!");
        System.out.println("Final Map: " + map);

        System.out.println("\n=== 2. Atomic Merge for Word Counting ===");
        ConcurrentHashMap<String, Long> wordCounts = new ConcurrentHashMap<>();
        int threads = 4;
        int wordsPerThread = 10_000;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch latch = new CountDownLatch(threads);

        for (int i = 0; i < threads; i++) {
            executor.submit(() -> {
                for (int j = 0; j < wordsPerThread; j++) {
                    // Atomically increments counter without race conditions!
                    wordCounts.merge("java", 1L, Long::sum);
                }
                latch.countDown();
            });
        }

        latch.await();
        executor.shutdown();

        System.out.println("Expected count: " + (threads * wordsPerThread));
        System.out.println("Actual count via merge(): " + wordCounts.get("java"));
        System.out.println("mappingCount() = " + wordCounts.mappingCount());
    }
}
```

---

## 7. Top 7 High-Signal Interview Questions & Deep Answers

### Q1: Why does `ConcurrentHashMap` prohibit `null` keys and `null` values while `HashMap` allows both?
**Deep Answer:**  
In `HashMap`, `get(key) == null` can be disambiguated from "key absent" by invoking `containsKey(key)`. 
In concurrent programming, `containsKey(k)` followed by `get(k)` creates an unavoidable race condition: another thread can delete or insert the key between the two calls. 
Doug Lea designed `ConcurrentHashMap` so that `map.get(key) == null` **unambiguously means the key does not exist**. 
Furthermore, permitting `null` values complicates concurrent lock-free reads and CAS protocols, where a `null` reference is universally used as a sentinel representing an uninitialized, empty, or deleted bucket.

### Q2: How do lock-free reads work in `ConcurrentHashMap`? Why is no lock or volatile read required on the map instance itself?
**Deep Answer:**  
Lock-free reads rely on two core Java Memory Model (JMM) guarantees:
1. **Volatile Array Elements via VarHandle/Unsafe:** When `get()` reads a bucket, it calls `tabAt(tab, index)`, which performs a volatile read (`ARRAY.getVolatile()`). This creates an acquire fence, ensuring that all writes to the bucket performed prior to or during insertion are visible to the reading thread.
2. **Volatile Node Fields:** Inside `Node<K,V>`, both `volatile V val` and `volatile Node<K,V> next` are volatile fields. When a writing thread appends a new node to the chain or updates a value, it issues a release write. The reader traverses these volatile pointers, establishing a happens-before relationship without ever acquiring a monitor lock or blocking.

### Q3: How does cooperative parallel resizing (`helpTransfer`) work in JDK 8+?
**Deep Answer:**  
Resizing a table with millions of buckets is expensive. In JDK 8+, resizing is parallelized across multiple threads:
1. When a thread triggers resize, it allocates `nextTable` (double capacity) and sets `sizeCtl` to a negative value encoding a generation stamp and helper count.
2. The old table is divided into strides (minimum 16 buckets). The thread claims a stride by atomically decrementing `transferIndex`.
3. As the thread migrates each bucket, it places a `ForwardingNode` (`hash = MOVED = -1`) pointing to `nextTable`.
4. If other threads attempt to write to a bucket marked with a `ForwardingNode`, they recognize that resizing is underway and invoke `helpTransfer()`.
5. Helper threads claim their own strides and transfer buckets concurrently. Resizing throughput scales linearly with the number of active threads.

### Q4: How does `size()` / `mappingCount()` calculate map size without locking all buckets? Explain `CounterCell` and `@Contended`.
**Deep Answer:**  
`ConcurrentHashMap` uses the same striped-cell design as `java.util.concurrent.atomic.LongAdder`:
- When thread contention is low, size updates are applied directly to `baseCount` using CAS.
- When CAS contention on `baseCount` is detected, the map initializes a `CounterCell[]` array and hashes each thread's thread-local probe (`ThreadLocalRandom.getProbe()`) to a specific `CounterCell`.
- Each `CounterCell` is annotated with `@jdk.internal.vm.annotation.Contended`, which instructs the JVM to pad the object with 128 bytes of empty memory. This isolates each cell onto its own CPU cache line, preventing **false sharing** (cache line ping-ponging across CPU cores).
- `mappingCount()` computes the sum: `baseCount + sum(counterCells[i].value)`. This is lock-free and returns a snapshot estimate of the collection size in $O(\text{cores})$ time.

### Q5: What is the purpose of `ReservationNode` in `computeIfAbsent()`?
**Deep Answer:**  
When `computeIfAbsent(key, mappingFunction)` is called on an empty bucket (`tabAt(tab, i) == null`):
If the mapping function is slow or computationally expensive (e.g., executing a database query or network call), leaving the bucket `null` would allow other threads to attempt concurrent insertions or computations on the same key.
To prevent this, `ConcurrentHashMap` allocates a lightweight `ReservationNode` (`hash = RESERVED = -3`), places it into `table[i]` via CAS, and synchronizes on that reservation node:
```java
ReservationNode<K,V> r = new ReservationNode<K,V>();
synchronized (r) {
    if (casTabAt(tab, i, null, r)) {
        // Run slow mappingFunction safely...
        // Replace reservation node with actual computed Node
    }
}
```
This reserves the bucket, blocks competing threads interested in that specific bucket, and prevents redundant execution of expensive functions (avoiding cache stampedes).

### Q6: Why did JDK 8 abandon the Segment-based architecture of JDK 7?
**Deep Answer:**  
1. **Concurrency Cap:** In JDK 7, concurrency was limited by the fixed number of `Segment` objects (default 16). More than 16 concurrent writers caused thread blocking. Increasing segments wasted memory.
2. **Excessive Memory Overhead:** Each `Segment` was an independent subclass of `ReentrantLock` containing its own inner hash table, lock fields, and queues. A small map incurred huge heap overhead.
3. **No Collision Defense:** JDK 7 segments used simple linked lists. Under hash flooding attacks, buckets degraded to $O(n)$ search time.
4. **JDK 8 Solution:** Uses a single flat array (`Node[]`). Concurrency scales naturally with bucket capacity. Uses native `synchronized` on bucket heads (leveraging HotSpot JVM optimizations like biased locking, lock coarsening, and adaptive spinning) and adds TreeBins ($O(\log n)$) for hash collision defense.

### Q7: What deadlock hazard exists when using `computeIfAbsent()` recursively in `ConcurrentHashMap`, and how does JDK 9+ handle it?
**Deep Answer:**  
Because `computeIfAbsent` executes the user-supplied lambda while holding the monitor lock on the target bucket head:
If the lambda function attempts to compute another key that hashes to the **exact same bucket**, or attempts an operation that requires the same bucket lock, a **classic circular wait deadlock** occurs:
```java
map.computeIfAbsent("KeyA", k -> {
    // If "KeyB" maps to the same bucket index as "KeyA", this hangs forever!
    return map.computeIfAbsent("KeyB", k2 -> 42); 
});
```
In JDK 8, this caused a permanent thread freeze. 
In JDK 9+ (via JDK-8062841), `ConcurrentHashMap` detects re-entrant computations on the same bucket and throws `IllegalStateException: Recursive update` to fail fast rather than deadlocking the application.
