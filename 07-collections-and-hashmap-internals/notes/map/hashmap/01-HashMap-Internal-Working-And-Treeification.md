# <HashMap — Deep Dive>
> Author: raysi

---

## 1. Architectural Overview & Hierarchy

`java.util.HashMap<K, V>` is the fundamental associative array implementation in the Java platform. It implements the hash table data structure, mapping keys to values using the key's hash code to determine bucket placement, providing average-case $O(1)$ time complexity for basic operations (`get` and `put`).

### Exact Package and Class Signature
```java
package java.util;

public class HashMap<K, V> extends AbstractMap<K, V>
    implements Map<K, V>, Cloneable, Serializable {
    @java.io.Serial
    private static final long serialVersionUID = 362498820763181265L;
    ...
}
```

### ASCII Inheritance & Type Hierarchy
```
                     ┌───────────────────────┐
                     │   java.util.Map<K,V>  │
                     └───────────┬───────────┘
                                 │ implements
                     ┌───────────▼───────────┐
                     │java.util.AbstractMap  │
                     └───────────┬───────────┘
                                 │ extends
        ┌────────────────────────┼────────────────────────┐
        │                        │                        │
┌───────▼──────────────┐  ┌──────▼──────┐          ┌──────▼──────┐
│  java.lang.Cloneable │  │HashMap<K,V> │          │ Serializable│
└──────────────────────┘  └──────┬──────┘          └─────────────┘
                                 │ extends
                     ┌───────────▼───────────┐
                     │LinkedHashMap<K,V>     │
                     └───────────────────────┘
```

### Why Does `HashMap` Exist? What Problem Does It Solve?
1. **Constant-Time Associative Lookup**: Standard arrays provide $O(1)$ random access via integer indices, but real-world domain keys are arbitrary types (Strings, UUIDs, domain models). `HashMap` maps arbitrary object spaces into contiguous array indices using hashing algorithms.
2. **Dynamic Collision Resolution**: Since the domain of keys is infinite while the hash table array is finite, collisions are mathematically inevitable (Pigeonhole Principle). `HashMap` resolves collisions via separate chaining using singly-linked lists, which dynamically treeify into balanced Red-Black Trees (`TreeNode`) under high collision loads (Java 8+), preventing algorithmic degradation and Hash-DoS security vulnerabilities.

---

## 2. Internal Data Structure & Memory Layout (JDK Source Code Level)

### Exact Internal Fields from JDK Source Code (`java.util.HashMap`)

```java
// The table, initialized on first use, and resized as necessary.
// Length is ALWAYS a power of two: 2^n.
transient Node<K,V>[] table;

// Holds cached entrySet(). Note that AbstractMap fields are used for keySet() and values().
transient Set<Map.Entry<K,V>> entrySet;

// The number of key-value mappings contained in this map.
transient int size;

// The number of times this HashMap has been structurally modified.
// Used for fail-fast iterators.
transient int modCount;

// The next size value at which to resize (capacity * load factor).
int threshold;

// The load factor for the hash table.
final float loadFactor;

// CORE CONSTANTS IN JDK SOURCE
static final int DEFAULT_INITIAL_CAPACITY = 1 << 4; // 16
static final int MAXIMUM_CAPACITY = 1 << 30;        // 1,073,741,824
static final float DEFAULT_LOAD_FACTOR = 0.75f;
static final int TREEIFY_THRESHOLD = 8;             // Bin count threshold to convert chain to tree
static final int UNTREEIFY_THRESHOLD = 6;           // Bin count threshold to convert tree back to chain
static final int MIN_TREEIFY_CAPACITY = 64;         // Minimum capacity needed before treeifying bins
```

### Bucket Node Implementations: `Node<K,V>` vs `TreeNode<K,V>`

```java
// Basic singly-linked list node used for normal bins
static class Node<K,V> implements Map.Entry<K,V> {
    final int hash;
    final K key;
    V value;
    Node<K,V> next;
}

// Red-Black tree node used for treeified bins (>= 8 nodes and capacity >= 64)
static final class TreeNode<K,V> extends LinkedHashMap.Entry<K,V> {
    TreeNode<K,V> parent;  // Red-black tree links
    TreeNode<K,V> left;
    TreeNode<K,V> right;
    TreeNode<K,V> prev;    // Needed to unlink next upon deletion
    boolean red;
}
```

### ASCII Memory & Bucket Layout

```
table = Node<K,V>[] (e.g. length = 16)
Index
[0]  ──► null
[1]  ──► [ Node: hash=17 | Key="A" | Val=1 | next ──► null ]
[2]  ──► null
[3]  ──► [ Node: Key="B" | next ] ──► [ Node: Key="R" | next ] ──► null  (Collision Chain)
...
[9]  ──► [ TreeNode (Root): Key="M", Color=BLACK ] (Treeified Bucket: >= 8 collisions)
             ├── Left:  [ TreeNode: Key="E", Color=RED ]
             │             ├── Left:  [ TreeNode: Key="C", Color=BLACK ]
             │             └── Right: [ TreeNode: Key="G", Color=BLACK ]
             └── Right: [ TreeNode: Key="T", Color=RED ]
                           ├── Left:  [ TreeNode: Key="P", Color=BLACK ]
                           └── Right: [ TreeNode: Key="Z", Color=BLACK ]
```

### CPU Cache & Memory Overhead Analysis
1. **Compressed OOPs (Ordinary Object Pointers)**:
   - On a 64-bit JVM with heap $< 32$ GB, HotSpot applies Compressed OOPs (`-XX:+UseCompressedOops`), reducing reference pointers from 8 bytes to 4 bytes.
   - **`HashMap.Node` Size**:
     - Object Header: 12 bytes (8 bytes Mark Word + 4 bytes Klass Pointer)
     - `int hash`: 4 bytes
     - `K key` reference: 4 bytes
     - `V value` reference: 4 bytes
     - `Node<K,V> next` reference: 4 bytes
     - Padding: 4 bytes (HotSpot aligns objects to 8-byte boundaries)
     - **Total per `Node` = 32 bytes**.
   - **`HashMap.TreeNode` Size**:
     - Extends `LinkedHashMap.Entry` which extends `HashMap.Node`.
     - Incurs extra fields: `before`, `after`, `parent`, `left`, `right`, `prev`, `boolean red`.
     - **Total per `TreeNode` = 56 to 64 bytes** (roughly double the memory of a standard `Node`).
2. **CPU Cache Line Behavior**:
   - Modern x86-64 CPUs fetch memory in **64-byte cache lines**.
   - A standard array of primitives benefits from spatial locality: iterating linearly hits the L1/L2 cache prefetcher.
   - In contrast, a `HashMap` incurs severe pointer indirection:
     1. Dereference `table` pointer $\to$ fetch bucket array reference.
     2. Dereference bucket index pointer $\to$ fetch `Node` or `TreeNode` object from arbitrary heap memory.
     3. Dereference `Node.key` and `Node.value` $\to$ additional cache misses.
   - Traversing a long linked list causes a pipeline stall on every node hop. This makes treeification crucial not just for algorithmic complexity ($O(N) \to O(\log N)$), but for bounding worst-case memory stall cycles.

---

## 3. Step-by-Step Internal Working of Core Operations

### 1. Instantiation (Constructors & `tableSizeFor`)
`HashMap` uses **lazy initialization**. Constructing `new HashMap<>()` does **NOT** allocate the bucket array `table`. It merely assigns `loadFactor = DEFAULT_LOAD_FACTOR (0.75)`. The array is allocated on the first `put()` invocation.

When an explicit capacity is provided, `tableSizeFor(int cap)` computes the smallest power of two greater than or equal to `cap`:
```java
static final int tableSizeFor(int cap) {
    int n = -1 >>> Integer.numberOfLeadingZeros(cap - 1);
    return (n < 0) ? 1 : (n >= MAXIMUM_CAPACITY) ? MAXIMUM_CAPACITY : n + 1;
}
```
*(In JDK 8, this was implemented with 5 bitwise shifts: `n |= n >>> 1; n |= n >>> 2; n |= n >>> 4; n |= n >>> 8; n |= n >>> 16; return n + 1;` which smears the highest set bit across all lower bits).*

### 2. The Hash Function: Bitwise Perturbation
```java
static final int hash(Object key) {
    int h;
    return (key == null) ? 0 : (h = key.hashCode()) ^ (h >>> 16);
}
```
- **Why `key == null` returns 0**: `null` keys are explicitly permitted in `HashMap` and always map to bucket `0`.
- **The Shift and XOR (`h ^ (h >>> 16)`)**:
  - The bucket index is calculated as `index = (n - 1) & hash`.
  - When table capacity `n` is small (e.g., 16), `n - 1` is `15` (`0b00000000_00000000_00000000_00001111`).
  - Without the perturbation function, only the lowest 4 bits of `key.hashCode()` participate in the index calculation. Any entropy or variation in the upper 16 or 28 bits is completely ignored!
  - By XORing the high 16 bits with the low 16 bits, the upper bit distribution cascades down to the lower bits, dramatically reducing collisions for sequential or patterned hash codes.

### 3. Insertion (`put()` / `putVal()`) Step-by-Step Execution Flow
```
put(K, V)
  │
  ▼
hash(key) -> computes perturbed hash
  │
  ▼
table == null or length == 0? ──► YES ──► resize() (allocates default capacity 16)
  │
  ▼ NO
index = (n - 1) & hash
  │
  ├── table[index] == null? ──► YES ──► table[index] = new Node(hash, key, value, null)
  │                                     size++; modCount++;
  │                                     if (size > threshold) resize();
  │                                     return null;
  ▼ NO (Collision Detected!)
  │
  ├── Check first node: (p.hash == hash && (p.key == key || key.equals(p.key)))
  │     ├── YES ──► Existing key found! Replace value, return oldValue.
  │
  ├── Node instanceof TreeNode?
  │     ├── YES ──► ((TreeNode)p).putTreeVal(this, tab, hash, key, value)
  │
  └── Walk Singly-Linked List:
        for (binCount = 0; ; ++binCount) {
            if (p.next == null) {
                p.next = new Node(hash, key, value, null);
                if (binCount >= TREEIFY_THRESHOLD - 1) { // 8th node inserted
                    treeifyBin(tab, hash);
                }
                break;
            }
            if (p.next.hash == hash && key.equals(p.next.key)) {
                // key found, break to overwrite
                break;
            }
            p = p.next;
        }
```

### 4. Treeification and De-Treeification Mechanics
- When a bucket chain length reaches `TREEIFY_THRESHOLD (8)`:
  - If `table.length < MIN_TREEIFY_CAPACITY (64)`: **Treeification does NOT occur!** Instead, `resize()` is invoked to double the table capacity. Resizing has a 50% probability of splitting the colliding bucket, resolving the congestion with lower memory overhead.
  - If `table.length >= 64`: `treeifyBin()` is executed. It converts the singly-linked `Node` chain into a doubly-linked `TreeNode` list, and then constructs a balanced Red-Black Tree.
- **Why 8 and 6? (Hysteresis & Poisson Distribution)**:
  - Under ideal uniform hash distributions, the probability of a bin holding $k$ elements follows the Poisson distribution:
    $$P(k) = \frac{e^{-\lambda} \lambda^k}{k!}$$
    For $\lambda = 0.5$ (average load with 0.75 load factor), $P(8) \approx 0.00000006$ (less than 1 in 10 million).
  - Chain length reaching 8 indicates an attack or a pathological hash code.
  - `UNTREEIFY_THRESHOLD = 6`: When nodes are removed and tree size drops to 6, `untreeify()` converts the `TreeNode` tree back to a linear `Node` list. The gap between 8 and 6 provides **hysteresis** (damping) to prevent rapid thrashing between treeification and untreeification upon alternating insertions and deletions.

### 5. Resizing (`resize()`) and the Bitwise Split Trick
When `size > threshold` (where `threshold = capacity * loadFactor`), `resize()` allocates a new table with double capacity (`newCap = oldCap << 1`).

#### The JDK 8+ `(hash & oldCap) == 0` Optimization
In JDK 7, every single entry had to be rehashed and recalculated via `indexFor(hash, newCap)`.
In JDK 8+, because capacity is always a power of two ($2^k$), doubling capacity simply activates **one single new bit** in the bitmask!
- Example: `oldCap = 16 (0b00010000)`, `newCap = 32 (0b00100000)`.
- `oldMask = 15 (0b00001111)`, `newMask = 31 (0b00011111)`.
- If the bit at position `oldCap` is `0`, `(hash & oldCap) == 0`:
  The new index is **identical to the old index**!
- If the bit at position `oldCap` is `1`, `(hash & oldCap) != 0`:
  The new index is **`oldIndex + oldCap`**!

```java
// JDK Source snippet from HashMap.resize()
Node<K,V> loHead = null, loTail = null; // Stays at old index
Node<K,V> hiHead = null, hiTail = null; // Moves to old index + oldCap
Node<K,V> next;
do {
    next = e.next;
    if ((e.hash & oldCap) == 0) {
        if (loTail == null) loHead = e;
        else loTail.next = e;
        loTail = e;
    } else {
        if (hiTail == null) hiHead = e;
        else hiTail.next = e;
        hiTail = e;
    }
} while ((e = next) != null);

if (loTail != null) {
    loTail.next = null;
    newTab[j] = loHead;
}
if (hiTail != null) {
    hiTail.next = null;
    newTab[j + oldCap] = hiHead;
}
```
**Benefits**:
1. Zero hash code re-evaluation or modulo operations.
2. Preserves relative element order (stable split), eliminating the notorious JDK 7 circular dependency race condition during concurrent transfer!

### 6. GC Leak Prevention in `clear()`
When `clear()` is invoked, it traverses the `table` array and sets every pointer to `null`:
```java
public void clear() {
    Node<K,V>[] tab;
    modCount++;
    if ((tab = table) != null && size > 0) {
        size = 0;
        for (int i = 0; i < tab.length; ++i)
            tab[i] = null;
    }
}
```
Setting `tab[i] = null` severs root references, allowing the Garbage Collector (ZGC, G1, or ParallelGC) to reclaim orphaned `Node` and `TreeNode` object graphs in the Young generation during the next scavenge.

---

## 4. Complexity & Behavioral Matrix

| Operation | Average Case | Worst Case (Treeified) | Worst Case (Non-Comparable Pathological) | Amortized Time |
|:---|:---:|:---:|:---:|:---:|
| `get(K)` | $O(1)$ | $O(\log N)$ | $O(N)$ | $O(1)$ |
| `put(K, V)` | $O(1)$ | $O(\log N)$ | $O(N)$ | $O(1)$ |
| `remove(K)` | $O(1)$ | $O(\log N)$ | $O(N)$ | $O(1)$ |
| `containsKey(K)`| $O(1)$ | $O(\log N)$ | $O(N)$ | $O(1)$ |
| `containsValue(V)`| $O(N)$ | $O(N)$ | $O(N)$ | $O(N)$ |
| `resize()` | $O(N)$ | $O(N)$ | $O(N)$ | $O(1)$ amortized |

### Behavioral Characteristics
- **Ordering**: No guarantee of ordering; order can change dynamically upon resizing.
- **Null Keys**: Allows **one** null key (always assigned hash 0, stored at index 0).
- **Null Values**: Allows multiple null values.
- **Duplicate Keys**: Not permitted; inserting an existing key overwrites the existing value and returns the old value.
- **Iterator Type**: **Fail-fast**. Iterators throw `ConcurrentModificationException` if `modCount` changes during iteration (except via the iterator's own `remove()` method).

---

## 5. Concurrency & Thread-Safety Mechanics

### ModCount and `ConcurrentModificationException`
Every structural mutation (`put`, `remove`, `clear`, `resize`) increments `modCount`:
```java
modCount++;
```
Iterators store `int expectedModCount = modCount;` upon creation. On every `next()` call, they evaluate:
```java
if (modCount != expectedModCount)
    throw new ConcurrentModificationException();
```
*Note*: Fail-fast behavior is best-effort and cannot be relied upon for concurrency control.

### Thread Safety Analysis & Failure Modes
`HashMap` is **strictly not thread-safe**. If accessed concurrently by multiple threads where at least one thread modifies it:
1. **JDK 7 Infinite Loop Bug**: In JDK 7, `transfer()` used head-insertion (reversing element order in the bucket). Concurrent resizing caused two threads to create a circular linked list reference (`A.next = B` and `B.next = A`). Subsequent `get()` calls on that bucket would loop infinitely, causing 100% CPU utilization.
2. **JDK 8+ Concurrency Hazards**: Although JDK 8 uses tail-insertion and the split trick (preventing circular lists in normal resize), concurrent access causes:
   - **Silent Data Loss**: Two threads executing `put()` concurrently in the same empty bucket evaluate `table[i] == null` simultaneously. Both write their node; the second thread overwrites the first thread's node, dropping data permanently.
   - **Size Inconsistency**: `size++` is not atomic (read-modify-write); updates to `size` will be lost.
   - **Tree Corruption**: Concurrent tree rebalancing (`rotateLeft`, `rotateRight`) corrupts the Red-Black tree invariant, causing infinite loops or JVM deadlocks during tree traversal.

### Modern Concurrent Alternative
- **`ConcurrentHashMap<K, V>`**:
  - Uses `volatile` reads, CAS (Compare-And-Swap) for empty bucket insertion, and synchronized locks at the specific bin head node for collision buckets.
  - Zero locking on reads (`get()` is 100% lock-free).
  - Iterators are **weakly consistent**: they reflect the state of the hash table at the time of creation or later, and never throw `ConcurrentModificationException`.

---

## 6. Production-Grade Java Code Verification

See the runnable demonstration file:
- [`HashMapCollisionAndTreeifyDemo.java`](../../../codes/collections/map/hashmap/HashMapCollisionAndTreeifyDemo.java)

Key mechanics demonstrated in the code:
1. Forcing hash collisions using custom keys with identical `hashCode()` but distinct identity.
2. Utilizing Java reflection to inspect `HashMap.table` at runtime.
3. Demonstrating bucket expansion from 16 to 32 to 64 before treeification triggers.
4. Observing `Node` turning into `TreeNode` at 8 elements once capacity reaches 64.
5. Observing `TreeNode` untreeifying back into `Node` when elements are deleted down to 6.

---

## 7. Top 7 High-Signal Interview Questions & Deep Answers

### Q1: Why did Java 8 replace bucket linked lists with Red-Black Trees (TreeNode)? What attack does this mitigate?
**Answer**:
Prior to Java 8, all collisions were chained as singly-linked lists. In the worst case (all keys hashing to the same bucket), `get()` and `put()` degraded to $O(N)$ linear scans.
This made Java web servers vulnerable to **Hash-DoS (Denial of Service) attacks**: an attacker could send a crafted HTTP POST request with thousands of parameter names having identical hash codes (e.g., colliding strings like `"Aa"`, `"BB"`). Processing this request caused the server's CPU to peg at 100% processing linked lists.
Java 8 converted buckets with $\ge 8$ collisions into Red-Black trees (`TreeNode`), bounding the worst-case lookup from $O(N)$ to $O(\log N)$. Even with millions of colliding keys, lookup requires at most $\approx 20$ comparisons.

### Q2: Why does `HashMap` require BOTH `TREEIFY_THRESHOLD = 8` AND `MIN_TREEIFY_CAPACITY = 64` before converting a bucket to a Red-Black Tree?
**Answer**:
1. **Memory Overhead**: A `TreeNode` is roughly $2\times$ the memory size of a standard `Node` (56–64 bytes vs 32 bytes).
2. **Resizing is Cheaper and More Beneficial**: When the table is small ($< 64$ capacity), having 8 elements in a single bucket is usually a coincidental artifact of small array size rather than an attack. Resizing doubles the table and splits the bucket using the `(hash & oldCap) == 0` trick with a 50% chance of dispersing elements evenly.
3. Treeification is therefore deferred until the table has reached a mature size ($\ge 64$), ensuring that Red-Black trees are reserved for genuine, persistent hash collision clusters.

### Q3: Explain why the capacity of `HashMap` is strictly enforced to be a power of two ($2^n$).
**Answer**:
1. **Fast Index Computation via Bitwise AND**: In a hash table, index is computed as `hash % capacity`. Modulo arithmetic (`%`) requires division at the CPU hardware level, costing roughly 10–30 CPU clock cycles. When capacity $n = 2^k$, $n - 1$ is a sequence of all 1s (e.g., $16 - 1 = 15 = \text{0b00001111}$). In binary arithmetic:
   $$\text{hash} \pmod n \equiv \text{hash} \ \& \ (n - 1)$$
   Bitwise AND (`&`) executes in **1 single CPU clock cycle**.
2. **Uniform Bit Masking**: If capacity is not a power of two (e.g., 15), $n - 1 = 14 = \text{0b00001110}$. The lowest bit is 0, which means any index calculation with this mask would **always yield an even number**, leaving all odd buckets completely empty! A power-of-two capacity ensures all bits of the mask are 1s, giving every bucket an equal probability of being addressed.

### Q4: Explain the `(hash & oldCap) == 0` trick during `resize()`. Why does JDK 8 no longer recompute bucket indices?
**Answer**:
Because capacity doubles ($2^k \to 2^{k+1}$), the bitmask expands by exactly 1 bit to the left (the bit representing `oldCap`).
- If an element's hash code has a `0` at that new bit position (`(hash & oldCap) == 0`), the new index is identical to `oldIndex`.
- If it has a `1` at that position (`(hash & oldCap) != 0`), the new index is `oldIndex + oldCap`.
Hence, the JVM partitions the bucket into two sub-chains (`lo` and `hi`) with a single bitwise test, without recalculating `hashCode()` or re-running index arithmetic.

### Q5: Why is `UNTREEIFY_THRESHOLD` set to 6 instead of 7 or 8?
**Answer**:
To introduce **hysteresis** (lag/damping). If `UNTREEIFY_THRESHOLD` were set to 8 (same as treeify threshold), adding an 8th element would treeify the bucket, and immediately deleting an element would de-treeify it back to a list. A program repeatedly inserting and removing an 8th element would thrash between Red-Black tree construction and linked list unlinking, destroying performance. The gap of 2 (8 vs 6) prevents this thrashing.

### Q6: Can `HashMap` enter an infinite loop in Java 8+ under concurrent modifications?
**Answer**:
In Java 7, concurrent resize caused circular references (`A <-> B`) due to head-insertion transfer, leading to guaranteed infinite loops on `get()`.
Java 8 eliminated circular lists during resize by using tail-insertion (`loHead`/`hiHead`) preserving order.
**However**, concurrent access in Java 8+ can STILL enter infinite loops or crash because of **Red-Black tree rebalancing**: if two threads simultaneously insert into a treeified bucket without synchronization, concurrent tree rotations (`rotateLeft`/`rotateRight`) can rewire parent/child pointers into cyclic graphs, causing subsequent `get()` or `put()` operations on that tree to traverse in an infinite loop, pinning the CPU at 100%.

### Q7: What role does `Comparable` play when keys collide in a treeified bucket? What happens if keys do NOT implement `Comparable`?
**Answer**:
A Red-Black Tree requires a total ordering ($\le, \ge$) to navigate left vs right child nodes during insertion and search.
1. When collisions occur, `HashMap` first checks if the key implements `Comparable<K>` via `comparableClassFor(k)`. If it does, it calls `compareComparables(k, x)`.
2. If keys have the same hash code and do **NOT** implement `Comparable` (or are different non-comparable classes):
   - The tree cannot determine whether to branch left or right based on natural ordering.
   - It performs a fallback: it searches **both left and right subtrees** recursively (`find()`).
   - If inserting a new entry and all else fails, it invokes `tieBreakOrder(Object a, Object b)` which uses `System.identityHashCode(a)compareTo(System.identityHashCode(b))` as an arbitrary tie-breaker to preserve tree structure.
   - Consequently, to achieve true $O(\log N)$ performance in treeified buckets, custom keys should implement `Comparable<K>`.
