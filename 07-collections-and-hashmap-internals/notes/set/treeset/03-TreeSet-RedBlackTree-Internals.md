# TreeSet — Deep Dive
> Author: raysi

---

## 1. Architectural Overview & Hierarchy

`TreeSet` is the total-ordering implementation of the `Set` and `NavigableSet` interfaces in the Java Collections Framework. Backed internally by a self-balancing binary search tree (a **Red-Black Tree** via `TreeMap`), `TreeSet` guarantees $O(\log N)$ time complexity for fundamental operations (`add`, `remove`, `contains`) while providing a comprehensive suite of range-slicing and nearest-match queries.

### Exact Package & Class Signature

```java
package java.util;

public class TreeSet<E>
    extends AbstractSet<E>
    implements NavigableSet<E>, Cloneable, java.io.Serializable
```

### ASCII Inheritance Hierarchy

```
                             java.lang.Iterable<E>
                                       │
                            java.util.Collection<E>
                                       │
                              java.util.Set<E>
                             /                \
             java.util.AbstractSet<E>     java.util.SortedSet<E>
                    │                                  │
                    │                      java.util.NavigableSet<E>
                    │                                  │
                    └───────────> java.util.TreeSet<E> <┘
```

### Why Does This Class Exist? What Problem Does It Solve?

1. **Guaranteed Total Ordering**:
   Neither `HashSet` (unordered) nor `LinkedHashSet` (insertion-ordered) sorts elements by their inherent magnitude. `TreeSet` maintains elements in strictly ascending order according to their natural ordering (`Comparable<E>`) or a custom supplied comparator (`Comparator<? super E>`).
2. **Guaranteed $O(\log N)$ Worst-Case Boundary**:
   While hash tables offer $O(1)$ average time, poor hash functions or HashDoS collision attacks degrade them to $O(N)$ (or $O(\log N)$ in treeified buckets). `TreeSet`'s Red-Black tree strictly enforces tree balancing invariants, guaranteeing that search, insertion, and deletion **never exceed $2 \log_2(N + 1)$ comparisons**.
3. **Rich Relational and Range-Based Queries**:
   Applications frequently require finding boundary thresholds (e.g., "find the highest sensor reading $\le 100$" via `floor()`, or "find all timestamps between $T_1$ and $T_2$" via `subSet()`). `TreeSet` provides live views and closest-match queries with zero data duplication.
4. **The TreeMap-Adapter Architecture**:
   Just as `HashSet` wraps `HashMap`, `TreeSet` acts as an adapter around a `NavigableMap<E, Object>` (specifically `TreeMap<E, Object>`). Elements are stored as **Keys**, and a static sentinel dummy object (`PRESENT`) is mapped as the **Value**.

---

## 2. Internal Data Structure & Memory Layout (JDK Source Code Level)

### Exact Internal Fields from JDK Source Code

```java
// JDK Source Code: java.util.TreeSet
public class TreeSet<E> extends AbstractSet<E>
    implements NavigableSet<E>, Cloneable, java.io.Serializable {

    // The backing NavigableMap
    private transient NavigableMap<E,Object> m;

    // Dummy value to associate with an Object in the backing Map
    private static final Object PRESENT = new Object();

    // Package-private constructor used to wrap sub-maps and descending views
    TreeSet(NavigableMap<E,Object> m) {
        this.m = m;
    }
}
```

### The Backing Engine: `TreeMap.Entry<K,V>` (Red-Black Tree Node)

Inside the backing `TreeMap`, every element is encapsulated within an internal node:

```java
// JDK Source Code: java.util.TreeMap.Entry
static final class Entry<K,V> implements Map.Entry<K,V> {
    K key;
    V value; // Points to shared static PRESENT dummy
    Entry<K,V> left;
    Entry<K,V> right;
    Entry<K,V> parent;
    boolean color = BLACK; // true = BLACK, false = RED
    
    Entry(K key, V value, Entry<K,V> parent) {
        this.key = key;
        this.value = value;
        this.parent = parent;
    }
}
```

### ASCII Red-Black Tree Memory Layout

```
TREE TOPOLOGY & NODE REFERENCES:
====================================================================================================
[ TreeSet Instance ] (16 bytes on 64-bit JVM)
  └── m ───────> [ TreeMap Instance ] (~48 bytes)
                   ├── comparator = null (Natural Order)
                   ├── size = 4
                   └── root ───> [ Entry: 20 (BLACK) ]
                                   ├── parent = null
                                   ├── val ───> [ static PRESENT ]
                                   ├── left ──> [ Entry: 10 (BLACK) ]
                                   │              ├── parent ──> [ Entry: 20 ]
                                   │              ├── val ────> [ static PRESENT ]
                                   │              ├── left ───> null
                                   │              └── right ──> [ Entry: 15 (RED) ]
                                   │                              ├── parent ──> [ Entry: 10 ]
                                   │                              ├── val ────> [ static PRESENT ]
                                   │                              ├── left ───> null
                                   │                              └── right ──> null
                                   └── right ─> [ Entry: 30 (BLACK) ]
                                                  ├── parent ──> [ Entry: 20 ]
                                                  ├── val ────> [ static PRESENT ]
                                                  ├── left ───> null
                                                  └── right ──> null
====================================================================================================
```

### Red-Black Tree Invariants (HotSpot Implementation)
To guarantee logarithmic height, the HotSpot `TreeMap` enforces 5 structural invariants:
1. **Node Coloring**: Every node is either `RED` (`color == false`) or `BLACK` (`color == true`).
2. **Root Property**: The `root` node is always `BLACK`.
3. **Leaf Property**: Every leaf (`null` sentinel) is considered `BLACK`.
4. **Red Invariant (No Consecutive Reds)**: If a node is `RED`, both of its children must be `BLACK`.
5. **Black Height Invariant**: For every node, every simple path from that node down to any descendant `null` leaf contains the **exact same number of black nodes** (the Black-Height $bh(x)$).

> [!NOTE]
> **Mathematical Consequence**: Because no path can contain two consecutive red nodes, the longest path from root to leaf (alternating red and black) is at most twice the length of the shortest path (all black). Hence, for $N$ elements:
> $$\text{Height} \le 2 \log_2(N + 1)$$
> This guarantees that `add`, `remove`, and `contains` require at most $O(\log N)$ operations.

### CPU Cache & Memory Overhead Analysis

#### Object Layout Breakdown (64-bit JVM, Compressed OOPs enabled):
- `TreeSet` Wrapper: 16 bytes.
- Backing `TreeMap` Wrapper: 48 bytes.
- `TreeMap.Entry<K,V>` per Element:
  - Object Header: 12 bytes (8B Mark + 4B Klass)
  - `K key` ref: 4 bytes
  - `V value` ref (points to `PRESENT`): 4 bytes
  - `Entry left` ref: 4 bytes
  - `Entry right` ref: 4 bytes
  - `Entry parent` ref: 4 bytes
  - `boolean color`: 1 byte
  - Alignment Padding: 7 bytes
  - Total: **40 bytes per entry**.

#### CPU Cache Penalty:
- **Random Heap Indirection**: While arrays store consecutive elements in contiguous memory lines, binary tree nodes are allocated individually over the application lifespan. Traversing down a tree of height 20 requires up to 20 random pointer dereferences across disjoint heap memory locations.
- **Cache-Line Misses**: Each node traversal almost certainly triggers an L1/L2/L3 cache miss because child nodes are rarely colocated within the parent's 64-byte hardware cache line. For pure search throughput, flat sorted arrays with binary search (`Arrays.binarySearch`) outperform `TreeSet` by 3x–5x due to hardware CPU prefetching.

---

## 3. Step-by-Step Internal Working of Core Operations

### A. Instantiation & Fast-Path Linear Build

`TreeSet` provides 4 public constructors:

```java
// 1. Natural ordering
public TreeSet() {
    this(new TreeMap<>());
}

// 2. Custom comparator
public TreeSet(Comparator<? super E> comparator) {
    this(new TreeMap<>(comparator));
}

// 3. Collection constructor: O(N log N)
public TreeSet(Collection<? extends E> c) {
    this();
    addAll(c);
}

// 4. SortedSet constructor: O(N) FAST-PATH BUILD!
public TreeSet(SortedSet<E> s) {
    this(s.comparator());
    addAll(s);
}
```

> [!TIP]
> **Source-Code Optimization (`buildFromSorted`)**:
> When instantiating `TreeSet(SortedSet<E> s)`, `TreeMap` detects that the source collection is already sorted with an identical or compatible comparator. Instead of executing repeated $O(\log N)$ insertions (which would cost $O(N \log N)$), `TreeMap` invokes its internal recursive `buildFromSorted()` algorithm, constructing a perfectly balanced Red-Black tree in strictly **linear $O(N)$ time**!

### B. Insertion (`add(E e)`)

```java
public boolean add(E e) {
    return m.put(e, PRESENT) == null;
}
```

#### Step-by-Step Mechanical Walkthrough in Backing `TreeMap`:
1. **Null Validation**:
   In Java 7+, `TreeMap` strictly prohibits `null` keys. If `e == null`, attempting to compare it throws `NullPointerException` immediately.
2. **Binary Search Traversal**:
   Starting from `root`, it traverses downward:
   - Evaluates `cmp = comparator.compare(e, t.key)` (or `((Comparable)e).compareTo(t.key)`).
   - If `cmp < 0`: traverses left (`t = t.left`).
   - If `cmp > 0`: traverses right (`t = t.right`).
3. **Duplicate Rejection via `compare() == 0`**:
   - If `cmp == 0`, a match is found!
   - `TreeMap.put()` replaces the existing value with `PRESENT` and returns the **old value** (`PRESENT`).
   - `TreeSet.add()` evaluates `PRESENT == null` $\rightarrow$ **`false`**.
   - **CRITICAL ARCHITECTURAL DISTINCTION**:
     `TreeSet` **never invokes `equals()`**! Two elements are considered duplicates if and only if `compare(a, b) == 0`. If `a.equals(b)` is false, but `compare(a, b) == 0`, `TreeSet` will drop `b`!
4. **Node Attachment & Balancing**:
   - If a leaf is reached, a new `Entry<K,V>(e, PRESENT, parent)` is created with color `RED`.
   - Attaches as left or right child of `parent`.
   - Invokes `fixAfterInsertion(Entry<K,V> x)`:
     - Handles color flips and **Tree Rotations** (Left-Rotate and Right-Rotate) to restore Red-Black invariants.
     - Forces `root.color = BLACK`.
   - Returns `null` $\rightarrow$ `TreeSet.add()` returns **`true`**.

### C. Tree Rotations Mechanics

When consecutive red nodes occur during insertion, tree rotations preserve the binary search invariant ($\text{left} < \text{node} < \text{right}$) while rebalancing path lengths:

```
     LEFT ROTATION ON NODE P                      RIGHT ROTATION ON NODE Q
     -----------------------                      ------------------------
            P                                                Q
           / \                                              / \
          A   Q         ──[ Left-Rotate P ]──>             P   C
             / \                                          / \
            B   C       <──[ Right-Rotate Q ]──          A   B
```

- **Left-Rotate(P)**: Node $Q$ rises to become the parent; $P$ becomes $Q$'s left child; $Q$'s original left subtree ($B$) is reassigned as $P$'s right child.
- **Right-Rotate(Q)**: Node $P$ rises to become the parent; $Q$ becomes $P$'s right child; $P$'s original right subtree ($B$) is reassigned as $Q$'s left child.

### D. Retrieval, Nearest-Match & Range Views

```java
// Nearest-match queries
public E lower(E e)   { return m.lowerKey(e); }   // Highest element < e
public E floor(E e)   { return m.floorKey(e); }   // Highest element <= e
public E ceiling(E e) { return m.ceilingKey(e); } // Lowest element >= e
public E higher(E e)  { return m.higherKey(e); }  // Lowest element > e

// Polling
public E pollFirst() {
    Map.Entry<E,?> e = m.pollFirstEntry();
    return (e == null) ? null : e.getKey();
}

// Live range views
public NavigableSet<E> subSet(E fromElement, boolean fromInclusive,
                              E toElement,   boolean toInclusive) {
    return new TreeSet<>(m.subMap(fromElement, fromInclusive, toElement, toInclusive));
}
```

#### How Range Views (`subSet`, `headSet`, `tailSet`) Work:
- They return a new `TreeSet` wrapping a `TreeMap.AscendingSubMap` or `DescendingSubMap`.
- **Zero Copy**: No nodes are duplicated.
- **Live Bidirectional Reflection**:
  - Mutating the view mutates the original `TreeSet`.
  - Mutating the original `TreeSet` within the range immediately reflects in the view.
- **Boundary Validation**:
  Inserting an element outside the defined view range immediately throws `IllegalArgumentException("key out of range")`.

---

## 4. Complexity & Behavioral Matrix

| Operation / Metric | Average Complexity | Worst-Case Complexity | Memory Cost | Invariant Enforced |
| :--- | :--- | :--- | :--- | :--- |
| **`add(e)`** | $O(\log N)$ | $O(\log N)$ | 40 bytes per entry | `compare(a, b) != 0` |
| **`contains(o)`** | $O(\log N)$ | $O(\log N)$ | Zero | BST navigation via `compare()` |
| **`remove(o)`** | $O(\log N)$ | $O(\log N)$ | Zero | BST delete + RB rebalancing |
| **`first()` / `last()`** | $O(\log N)$ | $O(\log N)$ | Zero | Walk to leftmost / rightmost node |
| **`floor()` / `ceiling()`**| $O(\log N)$ | $O(\log N)$ | Zero | Bound candidate search |
| **`pollFirst()` / `pollLast()`**| $O(\log N)$ | $O(\log N)$ | Zero | Unlinks minimum / maximum entry |
| **`iterator()` Traversal**| $O(N)$ | $O(N)$ | Zero | In-order traversal (left-root-right) |

### Behavioral Attributes:
- **Ordering Guarantee**: **Strict Total Ordering** (Ascending natural or Comparator order).
- **Null Allowance**: **Strictly Prohibited**. Throws `NullPointerException` on insertion of `null`.
- **Duplicate Elements**: Disallowed based exclusively on `compare(a, b) == 0`.
- **Fail-Fast Iterator**: Iteration is guarded by `modCount`. Any structural modification outside `iterator.remove()` throws `ConcurrentModificationException`.

---

## 5. Concurrency & Thread-Safety Mechanics

### Lack of Thread Safety
`TreeSet` is completely **non-thread-safe**. Concurrent mutations without external synchronization lead to catastrophic corruptions:
1. **Broken Tree Pointers**: Two threads performing insertions or rotations simultaneously can cause dangling left/right references or cyclic parent-child links, resulting in endless CPU spins (`while (x != null) x = x.left`).
2. **Violated Black-Height Invariants**: Simultaneous color adjustments violate invariant 5, leaving the tree permanently unbalanced.

### Why Red-Black Trees Are Rarely Concurrent
Creating a lock-free or fine-grained concurrent Red-Black tree is notoriously difficult because tree rotations require modifying multiple pointers across **three generations of nodes** (child, parent, and grandparent) atomically. A single rotation affects wide subtrees, making fine-grained lock striping impractical.

### Modern Concurrent Alternative: `ConcurrentSkipListSet<E>`

```
Requirement                             Recommended Class                       Mechanism
-----------                             -----------------                       ---------
Concurrent Sorted / Navigable Set       ConcurrentSkipListSet<E>               Lock-Free Skip List via CAS on Forward Pointers
Synchronized Coarse-Grained Wrapper     Collections.synchronizedNavigableSet() Single reentrant lock (high thread contention)
```

> [!IMPORTANT]
> Always prefer **`ConcurrentSkipListSet<E>`** over `Collections.synchronizedSortedSet(new TreeSet<>())`. Skip lists provide $O(\log N)$ expected time using lock-free CAS (Compare-And-Swap) operations on horizontal forward pointers, enabling multi-threaded reads and writes to proceed concurrently without locking out the entire tree.

---

## 6. Production-Grade Java Code Verification

The following runnable class demonstrates all `NavigableSet` methods (`lower`, `floor`, `ceiling`, `higher`, `pollFirst`, `pollLast`), shows that sub-range views are live and enforce runtime boundaries, proves the subtle `compareTo() == 0` collision trap, and demonstrates custom comparator navigation:

```java
package com.raysi.jcf.set.treeset.codes;

import java.util.*;

/**
 * Production-Grade Deep Dive: TreeSet, Red-Black Tree Internals & NavigableSet Operations
 * Author: raysi
 */
public class TreeSetRangeViewsDemo {

    public static void main(String[] args) {
        System.out.println("================================================================================");
        System.out.println("          TREESET: RED-BLACK TREE INTERNALS & NAVIGABLESET RANGE VIEWS          ");
        System.out.println("================================================================================");

        demoNavigableBoundaryQueries();
        demoPollingOperations();
        demoLiveSubSetViewsAndBoundaryValidation();
        demoDescendingSetView();
        demoCompareToEqualsInconsistencyTrap();
        demoNullRejectionAndComparableRequirement();
        demoCaseInsensitiveComparatorTreeSet();

        System.out.println("\nAll TreeSet Internals & Behavioral Invariants Verified Successfully.");
    }

    /**
     * 1. NavigableSet boundary queries: lower, floor, ceiling, higher.
     */
    private static void demoNavigableBoundaryQueries() {
        System.out.println("\n--- 1. Boundary Queries: lower, floor, ceiling, higher ---");

        NavigableSet<Integer> tree = new TreeSet<>(Arrays.asList(10, 20, 30, 40, 50));
        System.out.println("Underlying TreeSet: " + tree);

        // Queries around 30 (exact match present)
        System.out.println("lower(30)   [strictly < 30]  : " + tree.lower(30));   // 20
        System.out.println("floor(30)   [closest <= 30]  : " + tree.floor(30));   // 30
        System.out.println("ceiling(30) [closest >= 30]  : " + tree.ceiling(30)); // 30
        System.out.println("higher(30)  [strictly > 30]  : " + tree.higher(30));  // 40

        // Queries around 25 (non-existent key)
        System.out.println("lower(25)   [strictly < 25]  : " + tree.lower(25));   // 20
        System.out.println("floor(25)   [closest <= 25]  : " + tree.floor(25));   // 20
        System.out.println("ceiling(25) [closest >= 25]  : " + tree.ceiling(25)); // 30
        System.out.println("higher(25)  [strictly > 25]  : " + tree.higher(25));  // 30
    }

    /**
     * 2. Polling Operations: pollFirst() and pollLast().
     */
    private static void demoPollingOperations() {
        System.out.println("\n--- 2. Polling Operations: pollFirst() & pollLast() ---");

        NavigableSet<Integer> queue = new TreeSet<>(Arrays.asList(45, 12, 89, 3, 67));
        System.out.println("TreeSet Before Polling: " + queue);

        Integer min = queue.pollFirst();
        Integer max = queue.pollLast();

        System.out.println("pollFirst() returned minimum: " + min);
        System.out.println("pollLast()  returned maximum: " + max);
        System.out.println("TreeSet After Polling:        " + queue);
    }

    /**
     * 3. SubSet / HeadSet / TailSet Live Views & Boundary Enforcement.
     */
    private static void demoLiveSubSetViewsAndBoundaryValidation() {
        System.out.println("\n--- 3. Live Range Views & Boundary Enforcement ---");

        NavigableSet<Integer> tree = new TreeSet<>(Arrays.asList(10, 20, 30, 40, 50, 60, 70));
        System.out.println("Original Base TreeSet: " + tree);

        // Create a bounded view: [20, 50] inclusive
        NavigableSet<Integer> sub = tree.subSet(20, true, 50, true);
        System.out.println("Created subSet [20, 50]: " + sub);

        // Mutation 1: Mutating the view reflects in the original set
        sub.add(35);
        System.out.println("Added 35 via subSet view.");
        System.out.println("Base TreeSet reflects addition: " + tree);

        // Mutation 2: Mutating the base set within range reflects in the view
        tree.add(25);
        System.out.println("Added 25 to base TreeSet.");
        System.out.println("subSet view reflects addition:  " + sub);

        // Mutation 3: Out-of-bounds insertion attempt on the view
        try {
            sub.add(15); // Violates range: 15 < 20
            throw new AssertionError("Failed: Out-of-bounds element was allowed!");
        } catch (IllegalArgumentException iae) {
            System.out.println("Confirmed: subSet rejected out-of-range key 15: " + iae.getMessage());
        }

        try {
            sub.add(55); // Violates range: 55 > 50
            throw new AssertionError("Failed: Out-of-bounds element was allowed!");
        } catch (IllegalArgumentException iae) {
            System.out.println("Confirmed: subSet rejected out-of-range key 55: " + iae.getMessage());
        }
    }

    /**
     * 4. DescendingSet View: Bidirectional In-Order Traversal.
     */
    private static void demoDescendingSetView() {
        System.out.println("\n--- 4. DescendingSet View ---");

        NavigableSet<String> words = new TreeSet<>(Arrays.asList("Alpha", "Beta", "Gamma", "Delta"));
        System.out.println("Natural Ascending Order: " + words);

        NavigableSet<String> reversed = words.descendingSet();
        System.out.println("descendingSet() Order:   " + reversed);

        // Mutations in descendingSet reflect in the original set
        reversed.add("Omega");
        System.out.println("Added 'Omega' via descendingSet.");
        System.out.println("Original TreeSet reflects addition: " + words);
    }

    /**
     * 5. The Fatal compareTo() Inconsistency Trap:
     * TreeSet uses compare() == 0, NOT equals()!
     */
    private static void demoCompareToEqualsInconsistencyTrap() {
        System.out.println("\n--- 5. The compareTo() != equals() Inconsistency Trap ---");

        // A class where compareTo only checks length, but equals checks content!
        class Word implements Comparable<Word> {
            final String text;

            Word(String text) {
                this.text = text;
            }

            @Override
            public int compareTo(Word o) {
                // Buggy ordering: compares length only!
                return Integer.compare(this.text.length(), o.text.length());
            }

            @Override
            public boolean equals(Object o) {
                if (this == o) return true;
                if (!(o instanceof Word word)) return false;
                return Objects.equals(text, word.text);
            }

            @Override
            public int hashCode() {
                return Objects.hash(text);
            }

            @Override
            public String toString() {
                return text;
            }
        }

        Word word1 = new Word("cat");
        Word word2 = new Word("dog"); // Same length (3), but equals() is FALSE!

        System.out.println("word1.equals(word2): " + word1.equals(word2));
        System.out.println("word1.compareTo(word2): " + word1.compareTo(word2));

        // HashSet uses equals() -> stores both words!
        Set<Word> hashSet = new HashSet<>();
        hashSet.add(word1);
        hashSet.add(word2);
        System.out.println("HashSet size: " + hashSet.size() + " " + hashSet + " (correctly treats as distinct)");

        // TreeSet uses compareTo() -> drops word2 because compareTo == 0!
        Set<Word> treeSet = new TreeSet<>();
        boolean added1 = treeSet.add(word1);
        boolean added2 = treeSet.add(word2);
        System.out.println("TreeSet added word1: " + added1);
        System.out.println("TreeSet added word2: " + added2 + " (SILENT DATA LOSS!)");
        System.out.println("TreeSet size: " + treeSet.size() + " " + treeSet);
        System.out.println("ARCHITECTURAL LESSON: compareTo() MUST be consistent with equals()!");
    }

    /**
     * 6. Null Rejection & Comparable ClassCastException.
     */
    private static void demoNullRejectionAndComparableRequirement() {
        System.out.println("\n--- 6. Null Rejection & Non-Comparable Rejection ---");

        TreeSet<String> set = new TreeSet<>();

        // Rejection of null
        try {
            set.add(null);
            throw new AssertionError("Failed: TreeSet allowed null!");
        } catch (NullPointerException npe) {
            System.out.println("Confirmed: TreeSet throws NullPointerException on null addition.");
        }

        // Non-Comparable insertion without comparator
        class UncomparableItem {
            final int id;
            UncomparableItem(int id) { this.id = id; }
        }

        TreeSet<UncomparableItem> brokenTree = new TreeSet<>();
        try {
            brokenTree.add(new UncomparableItem(1));
            throw new AssertionError("Failed: Non-comparable element allowed!");
        } catch (ClassCastException cce) {
            System.out.println("Confirmed: Inserting non-Comparable element throws ClassCastException: " +
                    cce.getMessage());
        }
    }

    /**
     * 7. Custom Comparator: Case-Insensitive TreeSet.
     */
    private static void demoCaseInsensitiveComparatorTreeSet() {
        System.out.println("\n--- 7. Custom Comparator: Case-Insensitive TreeSet ---");

        TreeSet<String> caseInsensitiveSet = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        caseInsensitiveSet.add("Apple");
        caseInsensitiveSet.add("banana");
        caseInsensitiveSet.add("CHERRY");

        System.out.println("Case-Insensitive TreeSet: " + caseInsensitiveSet);

        // Attempting to add "apple" (lowercase) collides with "Apple"
        boolean added = caseInsensitiveSet.add("apple");
        System.out.println("Adding 'apple' (lowercase) returned: " + added + " (rejected as duplicate!)");
        System.out.println("contains('APPLE'): " + caseInsensitiveSet.contains("APPLE") + " (matched case-insensitively)");
    }
}
```

---

## 7. Top 7 High-Signal Interview Questions & Deep Answers

### Q1: Why does `TreeSet.contains()` rely exclusively on `compareTo()` / `compare()` instead of `equals()`?
**Answer:**
`TreeSet` delegates to `TreeMap`, which implements a Red-Black binary search tree. In a binary search tree, navigation decisions at each node are ternary:
- Go left if target is less than current node ($< 0$).
- Go right if target is greater than current node ($> 0$).
- Terminate search with success if target equals current node ($== 0$).
`equals()` returns only a binary `true`/`false` and provides **zero directional information**. A BST cannot guide traversal without knowing whether the target lies in the left or right subtree.
Consequently, `TreeSet` **never calls `equals()`**.
This introduces the rule: **If `compareTo()` is inconsistent with `equals()`**, `TreeSet` violates the general contract of `java.util.Set` (which specifies uniqueness via `equals()`).

### Q2: Why does `TreeSet` throw `NullPointerException` when inserting `null`, while `HashSet` permits it?
**Answer:**
`HashSet` computes `hash = (key == null) ? 0 : ...` and places `null` into bucket index 0 without invoking any methods on it.
Conversely, `TreeSet` must insert elements into a binary search tree. When inserting an element $e$:
1. If a comparator is present, it evaluates `comparator.compare(e, node.key)`.
2. If natural ordering is used, it evaluates `((Comparable) e).compareTo(node.key)`.
In both cases, comparing against `null` (or attempting to cast `null` to `Comparable` and invoke `.compareTo()`) triggers a `NullPointerException`.
*(Historical Note: In Java 1.2–6, adding `null` to an empty `TreeSet` succeeded because no comparisons were needed for the root node. However, the next insertion threw an NPE while comparing against the root null. Java 7 introduced an explicit null check on the first insertion to eliminate this confusing deferred bug).*

### Q3: How do `TreeSet` sub-range views (`subSet`, `headSet`, `tailSet`) enforce boundaries at runtime?
**Answer:**
They return instances of private nested classes (`TreeMap.AscendingSubMap` or `DescendingSubMap`). These views store bounding keys (`fromKey`, `toKey`) and boolean boundary flags (`fromInclusive`, `toInclusive`).
When any mutation method (such as `add(e)`) is invoked on the view:
1. The sub-map runs an internal check: `inRange(e)`.
2. It verifies $from \le e \le to$ (taking inclusion flags into account).
3. If the element violates the range, it throws `IllegalArgumentException("key out of range")`.
4. If valid, it delegates directly to the underlying `TreeMap.put(e, PRESENT)`.
All reads and iterators simply start traversing the tree at `getCeilingEntry(fromKey)` and terminate as soon as a node exceeds `toKey`.

### Q4: Explain the 5 invariants of a Red-Black tree and prove why maximum height is $\le 2 \log_2(N + 1)$.
**Answer:**
The 5 Invariants:
1. Every node is either RED or BLACK.
2. The root is always BLACK.
3. Every leaf (`null`) is BLACK.
4. If a node is RED, both its children are BLACK (no consecutive red nodes).
5. For each node, all paths to descendant leaves contain the same number of black nodes (Black-Height $bh(x)$).

**Proof of Maximum Height**:
- By induction, a subtree rooted at node $x$ contains at least $2^{bh(x)} - 1$ internal nodes.
- For the root node with black-height $bh$: $N \ge 2^{bh} - 1 \implies bh \le \log_2(N + 1)$.
- By Invariant 4, no path can have two red nodes in a row; therefore, at most half the nodes on any path can be red.
- Thus, the total height $h$ is at most twice the black height:
  $$h \le 2 \cdot bh \le 2 \log_2(N + 1)$$
This bounds the worst-case time of lookup, insertion, and deletion strictly to $O(\log N)$.

### Q5: Why is `ConcurrentSkipListSet` preferred over a concurrent Red-Black tree for multi-threaded sorted sets?
**Answer:**
Balancing a Red-Black tree during insertions or deletions requires **rotations** (Left-Rotate, Right-Rotate) and color flips that alter pointers across the target node, its parent, its sibling, and its grandparent. Locking such a wide, shifting portion of a tree atomically requires coarse-grained locking, destroying concurrency.
A **Skip List** (used by `ConcurrentSkipListSet`), by contrast, is a probabilistic, multi-level linked list. Modifications only require updating localized forward pointers at each level using non-blocking **CAS (Compare-And-Swap)** instructions. Skip lists allow concurrent readers to traverse lock-free while writers update pointers independently without global tree rebalancing.

### Q6: What is the algorithmic time complexity of `new TreeSet<>(sortedSet)` vs `new TreeSet<>(unsortedCollection)`?
**Answer:**
- `new TreeSet<>(unsortedCollection)`: Runs in **$O(N \log N)$** time. It iterates over the collection and invokes `add(e)` for each element, requiring $N$ separate tree traversals and balancing fix-ups.
- `new TreeSet<>(sortedSet)`: Runs in strictly **linear $O(N)$ time** via `TreeMap.buildFromSorted()`. Because elements are guaranteed to arrive in strictly ascending order with compatible comparator, `TreeMap` constructs the Red-Black tree bottom-up recursively, assigning red and black colors mathematically based on binary depth without executing a single tree rotation or binary search.

### Q7: How does `TreeSet.descendingSet()` provide reverse-order traversal without copying the tree?
**Answer:**
`descendingSet()` returns a new `TreeSet` wrapping a `TreeMap.DescendingSubMap` view.
- In `TreeMap`, standard iteration uses `successor(e)` (finding the next higher node: left-most node in the right subtree, or lowest ancestor whose left child is also an ancestor).
- The descending submap simply overrides iteration to call `predecessor(e)` (finding the next lower node: right-most node in the left subtree, or lowest ancestor whose right child is also an ancestor).
- All reads, range views, and operations are mapped symmetrically (e.g., calling `subSet()` on a descending set swaps the `from` and `to` arguments on the underlying tree). Zero data is copied, and time complexity remains $O(\log N)$ for operations and $O(N)$ for full reverse traversal.
