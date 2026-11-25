# TreeMap — Red-Black Tree Internals
> Author: raysi

---

## 1. Architectural Overview & Hierarchy

`java.util.TreeMap<K,V>` is an ordered, navigation-capable associative key-value map backed by a Red-Black tree. Introduced in JDK 1.2 and significantly enhanced in JDK 6 with the `NavigableMap` interface, it guarantees $O(\log n)$ time complexity for search, insertion, and deletion operations, while maintaining keys in strictly sorted order.

### Exact Class Signature

```java
package java.util;

public class TreeMap<K,V>
    extends AbstractMap<K,V>
    implements NavigableMap<K,V>, Cloneable, java.io.Serializable
```

### ASCII Inheritance & Interface Hierarchy

```
                       ┌────────────────────────┐
                       │     java.util.Map      │
                       └───────────┬────────────┘
                                   │
                                   ▼
                       ┌────────────────────────┐
                       │   java.util.SortedMap  │
                       └───────────┬────────────┘
                                   │
                                   ▼
                       ┌────────────────────────┐
                       │ java.util.NavigableMap │
                       └───────────┬────────────┘
                                   │
       ┌────────────────────────┐  │
       │ java.util.AbstractMap  │  │
       └───────────┬────────────┘  │
                   │               │
                   ▼               ▼
         ┌───────────────────────────────────┐
         │       java.util.TreeMap<K,V>      │
         └───────────────────────────────────┘
```

### Why Does This Class Exist? What Problem Does It Solve?

Standard hash-based associative containers (`HashMap`, `Hashtable`) provide average $O(1)$ lookup performance, but they suffer from fundamental architectural trade-offs:
1. **Total Absence of Key Ordering:** Hash codes distribute keys uniformly across buckets; chronological, alphabetical, or numerical order is completely destroyed.
2. **Inability to Perform Range or Nearest-Neighbor Queries:** A `HashMap` cannot efficiently answer: *"Find the highest transaction amount less than \$500"* or *"Retrieve all log events recorded between 10:00:00 and 10:05:00"*. Answering these on a `HashMap` requires an exhaustive $O(n)$ full table scan.
3. **Worst-Case Hash Flooding Degradation:** Under adversarial hash collisions or poorly designed `hashCode()` methods, a standard hash table can degrade toward linear search (mitigated by treeification in JDK 8+, but still non-deterministic in capacity and overhead).

`TreeMap` solves these problems by providing:
- **Strict Key Ordering:** Iteration returns keys in Natural Order (`Comparable`) or according to an explicitly provided `Comparator`.
- **Bidirectional Navigability:** Methods such as `floorKey`, `ceilingKey`, `lowerKey`, `higherKey`, `subMap`, `headMap`, and `tailMap` execute in deterministic $O(\log n)$ time.
- **Strict Worst-Case Guarantees:** Regardless of input distribution or adversarial inputs, a Red-Black tree remains balanced with height $h \le 2 \log_2(n + 1)$, ensuring guaranteed $O(\log n)$ operations.

---

## 2. Internal Data Structure & Memory Layout (JDK Source Code Level)

`TreeMap` is implemented as a symmetric Red-Black Tree (a self-balancing Binary Search Tree where each node stores an additional bit representing color).

### Exact Internal Fields from JDK Source Code

```java
// Comparator used to maintain order in this tree map, or null if it uses natural ordering
private final Comparator<? super K> comparator;

// Pointer to the root node of the Red-Black tree
private transient Entry<K,V> root;

// The number of key-value mappings contained in the map
private transient int size = 0;

// The number of structural modifications made to the tree
private transient int modCount = 0;

// Internal color bit representations
private static final boolean RED   = false;
private static final boolean BLACK = true;
```

### The Internal `Entry<K,V>` Node

```java
static final class Entry<K,V> implements Map.Entry<K,V> {
    K key;
    V value;
    Entry<K,V> left;
    Entry<K,V> right;
    Entry<K,V> parent;
    boolean color = BLACK;

    Entry(K key, V value, Entry<K,V> parent) {
        this.key = key;
        this.value = value;
        this.parent = parent;
    }

    public K getKey() { return key; }
    public V getValue() { return value; }
    public V setValue(V value) {
        V oldValue = this.value;
        this.value = value;
        return oldValue;
    }
    // equals, hashCode, toString omitted for brevity
}
```

### Red-Black Tree Invariants

Every valid `TreeMap` state strictly satisfies five mathematical axioms:
1. **Node Color:** Every node is either `RED` or `BLACK`.
2. **Root Property:** The `root` node is always `BLACK`.
3. **Leaf Property:** Every leaf (`null` sentinel) is implicitly considered `BLACK`.
4. **Red Property:** If a node is `RED`, both of its children must be `BLACK` (no two consecutive `RED` nodes along any simple path from root to leaf).
5. **Black-Height Property:** For each node, all simple paths from the node to descendant leaves contain the exact same number of `BLACK` nodes (uniform Black-Height, denoted $bh(x)$).

### ASCII Memory & Pointer Architecture

```
                                   ┌──────────────────────┐
                                   │     TreeMap Instance │
                                   │  root: [Ref 0x1000]  │
                                   │  size: 5             │
                                   │  comparator: null    │
                                   └──────────┬───────────┘
                                              │
                                              ▼
                                 ┌───────────────────────────┐
                                 │ Entry (0x1000) [BLACK]    │
                                 │ Key: 50, Val: "Fifty"     │
                                 │ Parent: null              │
                                 │ Left: 0x2000, Right: 0x3000│
                                 └───────┬───────────┬───────┘
                                         │           │
                    ┌────────────────────┘           └────────────────────┐
                    ▼                                                     ▼
     ┌───────────────────────────┐                         ┌───────────────────────────┐
     │ Entry (0x2000) [RED]      │                         │ Entry (0x3000) [BLACK]    │
     │ Key: 25, Val: "TwentyFive"│                         │ Key: 75, Val: "SeventyFive│
     │ Parent: 0x1000            │                         │ Parent: 0x1000            │
     │ Left: 0x4000, Right: null │                         │ Left: null, Right: 0x5000 │
     └───────┬───────────────────┘                         └───────────────────┬───────┘
             │                                                                 │
             ▼                                                                 ▼
┌───────────────────────────┐                                     ┌───────────────────────────┐
│ Entry (0x4000) [BLACK]    │                                     │ Entry (0x5000) [RED]      │
│ Key: 10, Val: "Ten"       │                                     │ Key: 90, Val: "Ninety"    │
│ Parent: 0x2000            │                                     │ Parent: 0x3000            │
│ Left: null, Right: null   │                                     │ Left: null, Right: null   │
└───────────────────────────┘                                     └───────────────────────────┘
```

### CPU Cache & Memory Overhead Analysis

On a standard 64-bit HotSpot JVM with Compressed OOPs (`-XX:+UseCompressedOops`, default up to 32GB heap):
- **Object Header:** 12 bytes (8-byte Mark Word + 4-byte Klass Word).
- **References:** 5 references (`key`, `value`, `left`, `right`, `parent`) $\times$ 4 bytes = 20 bytes.
- **Primitive Field:** 1 `boolean` (`color`) = 1 byte.
- **Padding:** 12 + 20 + 1 = 33 bytes $\rightarrow$ 8-byte alignment padding requires 7 bytes $\rightarrow$ **40 bytes per Entry node**.
- *(Without Compressed OOPs: 16-byte header + 5 $\times$ 8 bytes = 40 bytes + 1 byte boolean = 57 bytes $\rightarrow$ padded to **64 bytes per Entry node**).*

#### Comparison with `HashMap.Node`:
| Metric | `HashMap.Node` | `TreeMap.Entry` | Overhead Difference |
| :--- | :--- | :--- | :--- |
| **Object Header** | 12 bytes | 12 bytes | Identical |
| **Fields** | `hash` (4B), `key` (4B), `value` (4B), `next` (4B) = 16B | `key` (4B), `val` (4B), `left` (4B), `right` (4B), `parent` (4B), `color` (1B) = 21B | `TreeMap` has 1 extra pointer + boolean |
| **Total Memory (Compressed Oops)** | **32 bytes** | **40 bytes** | **+25% memory per node** |
| **CPU Cache Locality** | Poor (pointer chasing across chains) | Extremely Poor (pointer chasing in non-contiguous heap space) | `TreeMap` induces constant L1/L2 cache misses |

#### CPU Cache Pre-fetching Degradation
In a contiguous array (such as `ArrayList` or flat hash table), CPU hardware prefetchers stream cache lines (64 bytes) ahead of execution. In a `TreeMap`, every node lookup traverses heap-allocated `Entry` instances scattered throughout the tenured or young generation. Traversal of a tree of depth $h = 20$ elements causes up to 20 random memory dereferences and almost certain L3/DRAM bus stalls.

---

## 3. Step-by-Step Internal Working of Core Operations

### 3.1 Instantiation & Constructors

`TreeMap` provides 4 constructors:
1. `public TreeMap()`: Natural ordering (`Comparable`). `comparator = null`.
2. `public TreeMap(Comparator<? super K> comparator)`: Custom ordering.
3. `public TreeMap(Map<? extends K, ? extends V> m)`: Natural ordering; inserts all elements using standard `putAll()` in $O(n \log n)$.
4. `public TreeMap(SortedMap<K, ? extends V> m)`: **Optimized linear-time $O(n)$ build**. Uses internal `buildFromSorted()` method which reads sorted entries and builds a perfectly balanced Red-Black tree bottom-up without running rotation/rebalancing logic.

### 3.2 Insertion (`put(K key, V value)`)

```java
public V put(K key, V value) {
    Entry<K,V> t = root;
    if (t == null) {
        addEntryToEmptyMap(key, value);
        return null;
    }
    int cmp;
    Entry<K,V> parent;
    // Split comparator vs comparable search paths for performance
    Comparator<? super K> cpr = comparator;
    if (cpr != null) {
        do {
            parent = t;
            cmp = cpr.compare(key, t.key);
            if (cmp < 0)
                t = t.left;
            else if (cmp > 0)
                t = t.right;
            else {
                V oldValue = t.value;
                t.value = value;
                return oldValue;
            }
        } while (t != null);
    } else {
        Objects.requireNonNull(key); // Throws NPE on null key
        @SuppressWarnings("unchecked")
        Comparable<? super K> k = (Comparable<? super K>) key;
        do {
            parent = t;
            cmp = k.compareTo(t.key);
            if (cmp < 0)
                t = t.left;
            else if (cmp > 0)
                t = t.right;
            else {
                V oldValue = t.value;
                t.value = value;
                return oldValue;
            }
        } while (t != null);
    }
    Entry<K,V> e = new Entry<>(key, value, parent);
    if (cmp < 0)
        parent.left = e;
    else
        parent.right = e;
    fixAfterInsertion(e);
    size++;
    modCount++;
    return null;
}
```

#### Rebalancing: `fixAfterInsertion(Entry<K,V> x)`
When a new node `x` is inserted, it is colored `RED` (`color = RED`). If its parent is also `RED`, Property 4 (no adjacent red nodes) is violated. The fix runs a maximum of $O(\log n)$ recolorings and at most **two rotations**:

Let `x` be the newly inserted node, `p = x.parent`, `g = p.parent`, and `u` be the uncle of `x`:
- **Case 1: Uncle `u` is RED (Recoloring):**
  - Set `color(p) = BLACK`
  - Set `color(u) = BLACK`
  - Set `color(g) = RED`
  - Set `x = g` and propagate checks up toward the root.
- **Case 2: Uncle `u` is BLACK and `x` forms a triangle (Inner Child):**
  - If `p` is left child of `g` and `x` is right child of `p`: call `rotateLeft(p)` $\rightarrow$ transforms to Case 3.
  - If `p` is right child of `g` and `x` is left child of `p`: call `rotateRight(p)` $\rightarrow$ transforms to Case 3.
- **Case 3: Uncle `u` is BLACK and `x` forms a line (Outer Child):**
  - Set `color(p) = BLACK`
  - Set `color(g) = RED`
  - Call `rotateRight(g)` (if left child) or `rotateLeft(g)` (if right child).
  - Tree is now strictly balanced. Loop terminates immediately!
- **Root Termination:** Finally, set `color(root) = BLACK` to preserve Property 2.

#### Rotations Explained at Pointer Level

```
     Left Rotation at P                   Right Rotation at Q
         P                                     Q
        / \                                   / \
       A   Q       === rotateLeft(P) ==>     P   C
          / \      <== rotateRight(Q)==     / \
         B   C                             A   B
```

Pointer manipulations in `rotateLeft(Entry<K,V> p)`:
1. `Entry<K,V> r = p.right;`
2. `p.right = r.left;`
3. `if (r.left != null) r.left.parent = p;`
4. `r.parent = p.parent;`
5. If `p.parent == null`, `root = r;` else if `p.parent.left == p`, `p.parent.left = r;` else `p.parent.right = r;`
6. `r.left = p;`
7. `p.parent = r;`

### 3.3 Deletion (`remove(Object key)`)

Deletion in `TreeMap` proceeds in two stages:
1. **Standard BST Removal:**
   - If node $p$ has two children: replace $p$ with its in-order successor $s$ (the leftmost node in $p$'s right subtree). Copy $s$'s key and value into $p$, and set $p = s$. Node $p$ now has at most one child.
   - Let $replacement$ be $p.left \ne null ? p.left : p.right$.
   - Splice out $p$ by connecting $replacement.parent$ directly to $p.parent$.
2. **Rebalancing: `fixAfterDeletion(Entry<K,V> x)`:**
   - Rebalancing is ONLY required if the spliced node was `BLACK`. Splicing a `BLACK` node destroys the uniform Black-Height (Property 5), introducing a "double-black" deficit on node $x$.
   - `fixAfterDeletion` resolves the deficit through at most **three rotations**:
     - *Case 1:* Sibling $w$ is RED $\rightarrow$ recolor and rotate to turn sibling BLACK.
     - *Case 2:* Sibling $w$ is BLACK and both of $w$'s children are BLACK $\rightarrow$ recolor $w$ to RED, move deficit up to parent.
     - *Case 3:* Sibling $w$ is BLACK, $w$'s outer child is BLACK, inner child is RED $\rightarrow$ recolor and rotate to transform into Case 4.
     - *Case 4:* Sibling $w$ is BLACK, $w$'s outer child is RED $\rightarrow$ recolor, rotate at parent, clear deficit. Terminate.

### 3.4 GC Leak Prevention on Node Deletion

When an `Entry` is unlinked in `deleteEntry(Entry<K,V> p)`:
```java
p.left = null;
p.right = null;
p.parent = null;
p.key = null;
p.value = null;
```
Explicitly nullifying `left`, `right`, and `parent` breaks cyclic references and prevents a discarded node from retaining an entire subtree in memory should an external reference to `p` persist (e.g., via an unclosed iterator).

### 3.5 Navigable Queries Mechanics

- **`floorKey(K key)` / `ceilingKey(K key)`:** Traverses down the tree. At each node:
  - If $node.key == key$, returns immediately.
  - For `floorKey`: if $key < node.key$, answer must be in left subtree; if $key > node.key$, current node is a candidate, search right subtree. If right subtree yields nothing $\le key$, return current node.
- **`subMap(fromKey, fromInclusive, toKey, toInclusive)`:** Does NOT copy data. Returns an `AscendingSubMap` or `DescendingSubMap` view wrapping the original `TreeMap`. Range validity is checked lazily during lookups and eagerly during mutations ($O(1)$ view construction).

---

## 4. Complexity & Behavioral Matrix

| Operation | Average Time | Worst Case Time | Amortized Time | Space Complexity |
| :--- | :--- | :--- | :--- | :--- |
| `get(K)` | $O(\log n)$ | $O(\log n)$ | $O(\log n)$ | $O(1)$ |
| `containsKey(K)` | $O(\log n)$ | $O(\log n)$ | $O(\log n)$ | $O(1)$ |
| `put(K, V)` | $O(\log n)$ | $O(\log n)$ | $O(\log n)$ | $O(1)$ |
| `remove(K)` | $O(\log n)$ | $O(\log n)$ | $O(\log n)$ | $O(1)$ |
| `firstKey()` / `lastKey()` | $O(\log n)$ | $O(\log n)$ | $O(\log n)$ | $O(1)$ |
| `floorKey()` / `ceilingKey()` | $O(\log n)$ | $O(\log n)$ | $O(\log n)$ | $O(1)$ |
| `subMap()` view creation | $O(1)$ | $O(1)$ | $O(1)$ | $O(1)$ |
| Full Tree Iteration | $O(n)$ | $O(n)$ | $O(1)$ per step | $O(\log n)$ call stack / iterator state |

### Behavioral Characteristics
- **Ordering:** Sorted according to Natural Ordering (`Comparable`) or custom `Comparator`.
- **Null Keys:** **STRICTLY DISALLOWED**. Throws `NullPointerException` (JDK 7+).
- **Null Values:** **ALLOWED**. Multiple keys may map to `null`.
- **Duplicates:** Keys must be unique according to `compare()` or `compareTo()`. If `compare(k1, k2) == 0`, the old value is overwritten.
- **Iterator Type:** Fail-Fast. Any structural modification (`modCount` mismatch) during iteration throws `ConcurrentModificationException`.

---

## 5. Concurrency & Thread-Safety Mechanics

`TreeMap` is **not thread-safe**. 

### The `modCount` and `ConcurrentModificationException` Mechanics

Every structural mutation (`put`, `remove`, `clear`) increments the internal field:
```java
protected transient int modCount = 0;
```
When an iterator is constructed (e.g., via `entrySet().iterator()`), it captures:
```java
int expectedModCount = modCount;
```
Before advancing in `next()`, it evaluates:
```java
if (modCount != expectedModCount)
    throw new ConcurrentModificationException();
```

### Catastrophic Failure Modes Under Unsynchronized Concurrent Access
If multiple threads mutate a `TreeMap` without external synchronization:
1. **Tree Corruption:** Race conditions during pointer reassignment in `rotateLeft` or `rotateRight` can orphan subtrees or create cyclic child-parent loops.
2. **Infinite Loops:** An unsynchronized rotation can cause a node's child pointer to point back to an ancestor, causing subsequent `get()` or `put()` operations to loop infinitely at 100% CPU utilization.
3. **Silent Data Loss:** Concurrent `fixAfterInsertion` calls overwrite color flags simultaneously, violating black-height invariants and corrupting future search paths.

### Concurrent Alternatives
- **`Collections.synchronizedSortedMap(new TreeMap<>(...))`:** Wraps the tree with a synchronized monitor lock (`synchronized(mutex)`). Every single operation blocks all other threads, creating severe thread contention under multi-threaded read/write workloads.
- **`java.util.concurrent.ConcurrentSkipListMap<K,V>`:** The standard production-grade concurrent alternative. Backed by a lock-free SkipList using CAS (Compare-And-Swap) operations. Scales linearly across high core counts while providing identical $O(\log n)$ NavigableMap semantics.

---

## 6. Production-Grade Java Code Verification

The following self-contained Java class demonstrates:
1. Red-Black Tree invariant enforcement.
2. NavigableMap boundary semantics (`floor`, `ceiling`, `lower`, `higher`).
3. Range-view isolation and boundary validation exceptions.
4. The critical architectural trap: `Comparable` consistency with `equals()`.

```java
package com.raysi.jcf.map.treemap.notes;

import java.util.*;

/**
 * Verification of TreeMap Red-Black Tree behavior, NavigableMap semantics,
 * and key comparison subtleties.
 * 
 * @author raysi
 */
public class TreeMapInternalVerification {

    // Domain object with deliberately flawed equals vs compareTo
    static final class StockOrder implements Comparable<StockOrder> {
        final String orderId;
        final double price;

        StockOrder(String orderId, double price) {
            this.orderId = orderId;
            this.price = price;
        }

        @Override
        public int compareTo(StockOrder o) {
            // Compares ONLY price, ignoring unique orderId
            return Double.compare(this.price, o.price);
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof StockOrder other)) return false;
            return Double.compare(this.price, other.price) == 0 &&
                   Objects.equals(this.orderId, other.orderId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(orderId, price);
        }

        @Override
        public String toString() {
            return "[" + orderId + " @ $" + price + "]";
        }
    }

    public static void main(String[] args) {
        System.out.println("=== 1. NavigableMap Boundary Verifications ===");
        NavigableMap<Integer, String> tree = new TreeMap<>();
        tree.put(10, "Node 10");
        tree.put(20, "Node 20");
        tree.put(30, "Node 30");
        tree.put(40, "Node 40");
        tree.put(50, "Node 50");

        // Exact match vs strict inequalities
        System.out.println("floorKey(20)  (<= 20): " + tree.floorKey(20));   // 20
        System.out.println("lowerKey(20)  (< 20) : " + tree.lowerKey(20));   // 10
        System.out.println("ceilingKey(20)(>= 20): " + tree.ceilingKey(20)); // 20
        System.out.println("higherKey(20) (> 20) : " + tree.higherKey(20));  // 30

        System.out.println("\n=== 2. SubMap Range View Modification Trap ===");
        // Create range [20, 40]
        NavigableMap<Integer, String> sub = tree.subMap(20, true, 40, true);
        System.out.println("SubMap contents: " + sub);

        // Modifying the submap directly modifies backing tree
        sub.put(25, "Node 25");
        System.out.println("Backing tree after sub.put(25): " + tree);

        try {
            // Attempting to insert an element OUTSIDE the bounded submap
            sub.put(55, "Illegal Node");
        } catch (IllegalArgumentException e) {
            System.out.println("Caught expected boundary violation: key out of range!");
        }

        System.out.println("\n=== 3. The compareTo() vs equals() Contract Trap ===");
        // In a Map, equals() should define equivalence.
        // In TreeMap, compareTo() EXCLUSIVELY determines key identity!
        Map<StockOrder, String> orderBook = new TreeMap<>();
        StockOrder orderA = new StockOrder("ORD-001", 150.50);
        StockOrder orderB = new StockOrder("ORD-002", 150.50); // Same price, different ID

        System.out.println("orderA.equals(orderB): " + orderA.equals(orderB)); // false
        System.out.println("orderA.compareTo(orderB): " + orderA.compareTo(orderB)); // 0

        orderBook.put(orderA, "Trader Alice");
        orderBook.put(orderB, "Trader Bob"); // Overwrites orderA because compareTo == 0!

        System.out.println("OrderBook size: " + orderBook.size()); // 1, NOT 2!
        System.out.println("Current order in map: " + orderBook.keySet().iterator().next());
        System.out.println("Mapped Value: " + orderBook.get(orderA)); // Returns "Trader Bob"
    }
}
```

---

## 7. Top 7 High-Signal Interview Questions & Deep Answers

### Q1: Why did the JDK architects choose Red-Black Trees over AVL Trees for `TreeMap`?
**Deep Answer:**  
Both Red-Black Trees and AVL Trees are self-balancing binary search trees guaranteeing $O(\log n)$ operations. However:
- **AVL Trees** are strictly balanced: for every node, the height difference between left and right subtrees is at most 1 ($h \approx 1.44 \log_2 n$). Lookup is slightly faster because the tree is shallower.
- **Red-Black Trees** are more loosely balanced: the longest path from root to leaf can be up to twice the length of the shortest path ($h \le 2 \log_2(n + 1)$).
- **The Decisive Factor (Rotations on Mutation):**
  - In an AVL Tree, deletion can trigger up to $O(\log n)$ rotations propagating all the way to the root.
  - In a Red-Black Tree, insertion requires at most **2 rotations**, and deletion requires at most **3 rotations**. Rebalancing is dominated by cheap bitwise color changes rather than costly tree pointer updates.
  - For general-purpose workloads involving frequent insertions and deletions, Red-Black Trees consistently yield superior throughput.

### Q2: How does `TreeMap` violate the core `java.util.Map` contract regarding object equality?
**Deep Answer:**  
The `java.util.Map` specification states that a map defines equality based on `equals()`: a map contains a key `k` if `Objects.equals(key, k)`. 
`TreeMap` explicitly departs from this contract. It defines key equivalence **strictly via `Comparable.compareTo()` or `Comparator.compare()`**. If `compare(k1, k2) == 0`, `TreeMap` considers `k1` and `k2` to be identical keys, even if `k1.equals(k2)` evaluates to `false`. 
This causes severe bugs if a comparator only compares a subset of fields (e.g., sorting employees by salary alone will cause any two employees with identical salaries to overwrite each other).

### Q3: Why does `new TreeMap<String, String>().put(null, "val")` throw `NullPointerException` even when the map is completely empty?
**Deep Answer:**  
In JDK 1.6 and earlier, calling `put(null, "val")` on an empty `TreeMap` with natural ordering succeeded because `root == null` bypassed key comparison. However, the very next call to `get()`, `put()`, or `containsKey()` threw `NullPointerException` because comparing any new key against the root's `null` key failed.
In JDK 7 (via JDK-5045147), the architects fixed this latent defect by adding an explicit null check in `put()`:
```java
if (cpr == null) {
    Objects.requireNonNull(key);
    Comparable<? super K> k = (Comparable<? super K>) key;
    k.compareTo(key); // Forces fail-fast NullPointerException on root insertion
}
```
Thus, `TreeMap` fails immediately at the point of insertion rather than corrupting the root state.

### Q4: Walk through the exact rebalancing steps when keys are inserted in strictly ascending order: 10, 20, 30.
**Deep Answer:**  
1. **Insert 10:** Tree is empty. Node 10 becomes `root`. Colored `BLACK`.
2. **Insert 20:** $20 > 10$. Inserted as right child of 10. Colored `RED`. Invariants hold (Parent 10 is `BLACK`).
3. **Insert 30:** $30 > 20$. Inserted as right child of 20. Colored `RED`.
   - **Violation:** Node 30 is `RED` and its parent 20 is `RED` (Property 4 violated).
   - **Uncle Check:** Uncle of 30 is `null` (which is `BLACK`).
   - **Shape Check:** 10 $\rightarrow$ 20 $\rightarrow$ 30 forms a straight right-line (Case 3: Outer Child).
   - **Action:** Recolor parent (20) to `BLACK`, recolor grandparent (10) to `RED`.
   - Execute `rotateLeft(10)`.
   - Node 20 becomes the new `root` (`BLACK`), with 10 (`RED`) on its left and 30 (`RED`) on its right.
   - Result: Perfectly balanced tree of height 1.

### Q5: How does in-order iteration achieve amortized $O(1)$ time per entry, despite tree height being $O(\log n)$?
**Deep Answer:**  
In-order traversal is executed by repeatedly calling the internal `successor(Entry<K,V> t)` method. 
While finding the successor of a single node may take up to $O(\log n)$ steps (e.g., climbing up multiple parent pointers from a rightmost leaf back to the root), traversing the entire tree visits every edge **at most twice**: once traversing down and once traversing up.
A tree with $n$ nodes has exactly $n - 1$ edges. The total number of edge traversals across an entire tree iteration is $2(n - 1)$. 
Dividing total work by $n$:
$$\text{Amortized Cost per Step} = \frac{2n - 2}{n} \approx 2 = O(1)$$
Thus, full `TreeMap` iteration runs in deterministic $O(n)$ time with $O(1)$ amortized steps.

### Q6: How do `subMap()`, `headMap()`, and `tailMap()` views prevent range escape without copying data?
**Deep Answer:**  
`TreeMap` creates an inner class instance of `AscendingSubMap` containing `fromKey`, `toKey`, and boolean flags `fromInclusive` and `toInclusive`. 
Every query and mutation passes through boundary check guards: `inRange(key)`:
```java
final boolean inRange(Object key) {
    return !tooLow(key) && !tooHigh(key);
}
```
If a caller attempts `subMap.put(outOfRangeKey, value)`, the view throws `IllegalArgumentException: key out of range`. 
Because mutations write directly to the underlying `TreeMap.this`, modifications to the submap are instantly visible in the master tree and vice versa.

### Q7: Why is `ConcurrentSkipListMap` chosen as the concurrent alternative to `TreeMap` instead of a Concurrent Red-Black Tree?
**Deep Answer:**  
Balancing a Red-Black tree requires rotations that modify pointers across up to 3 to 5 distinct nodes simultaneously (child, parent, grandparent, uncle, sibling). 
In a concurrent environment:
- Atomically updating multiple disparate memory addresses requires multi-word compare-and-swap (MCAS) or coarse-grained subtree locking, which causes massive thread contention and complex deadlock handling.
- A **SkipList**, by contrast, is a probabilistic linked hierarchy. Node insertion and deletion can be performed using standard, single-word CAS operations on local linked list pointers (`AtomicMarkableReference` or `VarHandle`).
- SkipLists allow lock-free, non-blocking reads and fine-grained localized mutations, delivering exceptional linear scalability on multi-core systems while preserving identical $O(\log n)$ ordered dictionary semantics.
