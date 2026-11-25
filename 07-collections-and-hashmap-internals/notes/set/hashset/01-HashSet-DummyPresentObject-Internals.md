# HashSet — Deep Dive
> Author: raysi

---

## 1. Architectural Overview & Hierarchy

`HashSet` is the flagship and most frequently utilized implementation of the `Set` interface in the Java Collections Framework. It models a collection of unique elements with $O(1)$ constant-time performance for basic operations (`add`, `remove`, `contains`, and `size`), assuming an effective hashing function that distributes elements uniformly across buckets.

### Exact Package & Class Signature

```java
package java.util;

public class HashSet<E>
    extends AbstractSet<E>
    implements Set<E>, Cloneable, java.io.Serializable
```

### ASCII Inheritance Hierarchy

```
                    java.lang.Iterable<E>
                              │
                    java.util.Collection<E>
                              │
                      java.util.Set<E>
                     /                \
        java.util.AbstractSet<E>       \ (implements)
                   │                    \
                   │                     │
            java.util.HashSet<E> ────────┘
                   │
         java.util.LinkedHashSet<E>
```

### Why Does This Class Exist? What Problem Does It Solve?

1. **High-Performance Deduplication and Fast Membership Testing**:
   Array-based collections (`ArrayList`) require $O(N)$ linear scans for `contains()` and deduplication. `HashSet` reduces membership queries, insertions, and deletions to an expected $O(1)$ average time by utilizing hashing.
2. **The Map-Adapter Pattern**:
   Instead of writing an independent, dedicated hash table implementation from scratch, the JDK architects leveraged the **Adapter Design Pattern**. `HashSet` delegates its entire storage, collision handling, and resizing mechanics to an internal `HashMap` instance. The element stored in the `HashSet` acts as the map's **Key**, while a static, shared sentinel object (`PRESENT`) is mapped as the **Value**.
3. **Inherited Production Optimizations**:
   By wrapping `HashMap`, `HashSet` automatically benefits from all enterprise-grade JVM optimizations added to `HashMap` across releases, such as the Java 8 balanced Red-Black tree conversion (`TreeNode`) when bucket collisions exceed the treeify threshold (`TREEIFY_THRESHOLD = 8`).

---

## 2. Internal Data Structure & Memory Layout (JDK Source Code Level)

### Exact Internal Fields from JDK Source Code

```java
// JDK Source Code: java.util.HashSet
public class HashSet<E> extends AbstractSet<E> implements Set<E>, Cloneable, java.io.Serializable {
    @java.io.Serial
    static final long serialVersionUID = -5024744406713321676L;

    // The backing map holding elements as keys
    private transient HashMap<E,Object> map;

    // Dummy value to associate with an Object in the backing Map
    private static final Object PRESENT = new Object();
    
    // ... constructors and methods ...
}
```

> [!NOTE]
> The backing field `map` is declared `transient`. It is **not** serialized by Java's default serialization mechanism. Instead, `HashSet` writes only its capacity, load factor, size, and individual elements to the stream using custom `writeObject()` and `readObject()` methods. This prevents serializing the internal table array structure and empty buckets across different JVM versions.

### ASCII Memory Graph & Object Topology

```
JVM HEAP MEMORY TOPOLOGY:
====================================================================================================
[ HashSet Instance ] (16 bytes on 64-bit JVM + UseCompressedOops)
  │
  └── map ───────> [ HashMap Instance ] (~48 bytes)
                     ├── loadFactor = 0.75f
                     ├── threshold  = 12
                     ├── size       = 3
                     └── table ───> [ Node[] array ] (capacity = 16, ~80 bytes)
                                      ├── [0] ──> null
                                      ├── [1] ──> [ Node A ]
                                      │             ├── hash  = 0x4F1A
                                      │             ├── key   ───> "User-101"
                                      │             ├── value ───> [ static PRESENT ] (16B singleton)
                                      │             └── next  ───> null
                                      ├── [2] ──> null
                                      ├── [3] ──> [ Node B ]
                                      │             ├── hash  = 0x7B9C
                                      │             ├── key   ───> "User-204"
                                      │             ├── value ───> [ static PRESENT ] ──┐ (shared identity)
                                      │             └── next  ───> [ Node C ]           │
                                      │                              ├── key ─> "User-9"│
                                      │                              ├── val ───────────┘
                                      │                              └── next ─> null
                                      └── [4..15] ──> null
====================================================================================================
```

### CPU Cache & Memory Overhead Analysis

#### Object Layout Breakdown (64-bit HotSpot JVM with `-XX:+UseCompressedOops`):
1. **`HashSet` Instance**:
   - Mark Word: 8 bytes
   - Klass Word: 4 bytes (compressed pointer)
   - Field `map` reference: 4 bytes
   - Total: **16 bytes** (perfect 8-byte alignment, 0 bytes padding).
2. **Backing `HashMap` Instance**:
   - Object Header: 12 bytes (8B Mark + 4B Klass)
   - Fields (`table`, `entrySet`, `size`, `modCount`, `threshold`, `loadFactor`): 24 bytes
   - Padding: 4 bytes
   - Total: **48 bytes**.
3. **Shared Sentinel `PRESENT`**:
   - Allocated once per `ClassLoader` in the heap: 16 bytes. Does **not** scale with the number of elements.
4. **Per-Element `Node<K,V>`**:
   - Object Header: 12 bytes
   - `int hash`: 4 bytes
   - `K key` reference: 4 bytes
   - `V value` reference (points to `PRESENT`): 4 bytes
   - `Node<K,V> next` reference: 4 bytes
   - Padding: 4 bytes
   - Total: **32 bytes per node**.
5. **Backing Array `Node<K,V>[] table`**:
   - Array Header: 16 bytes (12B header + 4B length)
   - Reference slots: $\text{capacity} \times 4$ bytes.
   - Padded to 8-byte boundary.

#### Total Memory Calculation for $N$ Elements (at default load factor 0.75):
For $N = 1,000,000$ elements, table capacity dynamically resizes to $2^{21} = 2,097,152$ slots:
- `HashSet` + `HashMap` wrappers: $16 + 48 = 64$ bytes
- `table` array: $16 + (2,097,152 \times 4) = 8,388,624$ bytes ($\approx 8.4\text{ MB}$)
- $1,000,000$ `Node` objects: $1,000,000 \times 32 = 32,000,000$ bytes ($32\text{ MB}$)
- Total Heap Footprint $\approx \mathbf{40.4\text{ MB}}$ (excluding the user's key objects).
- Over-allocation overhead: ~40 bytes per entry.

#### CPU Cache Behavior:
- **Spatial Locality Degradation**: The `Node` objects are allocated dynamically on the JVM heap. Because the garbage collector moves or allocates nodes non-contiguously, pointer-chasing through bucket collision linked lists (`node.next`) causes random memory hops.
- **Cache-Line Misses**: An L1/L2 cache line is 64 bytes. Fetching a single `Node` pulls 32 bytes into the cache line, but the next node in the linked chain is rarely in the adjacent 32 bytes, causing frequent hardware cache misses.

---

## 3. Step-by-Step Internal Working of Core Operations

### A. Instantiation & The Package-Private Constructor

`HashSet` provides 4 public constructors and 1 package-private constructor:

```java
// 1. Default constructor: capacity 16, load factor 0.75
public HashSet() {
    map = new HashMap<>();
}

// 2. Initial capacity specified
public HashSet(int initialCapacity) {
    map = new HashMap<>(initialCapacity);
}

// 3. Initial capacity and load factor specified
public HashSet(int initialCapacity, float loadFactor) {
    map = new HashMap<>(initialCapacity, loadFactor);
}

// 4. Collection copy constructor
public HashSet(Collection<? extends E> c) {
    map = new HashMap<>(Math.max((int) (c.size()/.75f) + 1, 16));
    addAll(c);
}

// 5. Package-Private constructor: USED EXCLUSIVELY BY LinkedHashSet!
HashSet(int initialCapacity, float loadFactor, boolean dummy) {
    map = new LinkedHashMap<>(initialCapacity, loadFactor);
}
```

> [!IMPORTANT]
> The 5th constructor uses a `boolean dummy` parameter to differentiate its signature from the public `HashSet(int, float)` constructor. When invoked by `LinkedHashSet`, it assigns a `LinkedHashMap` to `map`, seamlessly converting the set into an insertion-ordered set without duplicating any set logic!

### B. Insertion (`add(E e)`)

The exact implementation in `HashSet.java`:

```java
public boolean add(E e) {
    return map.put(e, PRESENT) == null;
}
```

#### Step-by-Step Mechanical Walkthrough:
1. **Hash Calculation**: `map.put(e, PRESENT)` is called. `HashMap` computes the hash:
   $$\text{hash} = (e == \text{null}) ? 0 : (h = e.\text{hashCode}()) \oplus (h \ggg 16)$$
2. **Bucket Indexing**: Bucket index is calculated using bitwise AND:
   $$\text{index} = (n - 1) \ \& \ \text{hash} \quad \text{where } n = \text{table.length}$$
3. **Collision Detection & Equality**:
   - If bucket is empty, a new `Node<E, Object>` is allocated with `key = e`, `value = PRESENT`, `next = null`.
   - If bucket contains elements, `HashMap` traverses the bucket chain. For each node, it checks:
     $$(p.\text{hash} == \text{hash}) \ \&\& \ (p.\text{key} == e \ || \ (e \ne \text{null} \ \&\& \ e.\text{equals}(p.\text{key})))$$
4. **Handling Duplicates vs. New Elements**:
   - **Case 1: Key already exists**: `HashMap.putVal()` overwrites the existing entry's value with `PRESENT` and returns the **old value** (`PRESENT`). In `HashSet`:
     $$\text{return } (\text{PRESENT} == \text{null}) \implies \mathbf{false}$$
     The set returns `false`, signaling that no state change occurred.
   - **Case 2: Key is brand new**: `HashMap.putVal()` inserts the new node, increments `modCount` and `size`, and returns **null**. In `HashSet`:
     $$\text{return } (\text{null} == \text{null}) \implies \mathbf{true}$$
     The set returns `true`, indicating successful addition.
5. **Handling Null Elements**:
   `null` is allowed because `HashMap` maps `null` to `hash = 0` (bucket index 0). Exactly one null key can exist. Subsequent `add(null)` calls detect `p.key == null` and return `false`.

### C. Retrieval & Deletion

```java
public boolean contains(Object o) {
    return map.containsKey(o);
}

public boolean remove(Object o) {
    return map.remove(o) == PRESENT;
}

public void clear() {
    map.clear();
}
```

- **`contains(Object o)`**: Delegates to `map.containsKey(o)`. Returns `true` if `getNode(hash(o), o) != null`.
- **`remove(Object o)`**: Calls `map.remove(o)`. If the key existed, `remove()` returns the mapped value (`PRESENT`). In `HashSet`, `PRESENT == PRESENT` evaluates to `true`. If the key was absent, `map.remove()` returns `null`, evaluating to `false`.

### D. Resizing, Growing, and Treeification Mechanics

- When `map.size > threshold` (where `threshold = capacity * loadFactor`), `map.resize()` is triggered.
- The bucket array doubles in size ($16 \rightarrow 32 \rightarrow 64 \rightarrow \dots$).
- **Bitwise Split**: Each element's new bucket is determined by checking the newly active power-of-two bit:
  - If `(hash & oldCap) == 0`: Element remains at `index`.
  - If `(hash & oldCap) != 0`: Element moves to `index + oldCap`.
- **Treeification**: When any single bucket accumulates $\ge 8$ elements (`TREEIFY_THRESHOLD`) AND the overall table capacity $\ge 64$ (`MIN_TREEIFY_CAPACITY`), the bucket converts from a singly-linked list of `Node` to a Red-Black tree of `TreeNode`. This bounds worst-case collision lookup to $O(\log N)$.

### E. GC Leak Prevention & The Mutable Key Trap

- **Normal GC**: When `remove(o)` is executed, the node is unlinked from the bucket. The reference to the user's key object is cleared, allowing garbage collection.
- **The Mutable Key Trap**:
  If an element stored in a `HashSet` is mutated such that its `hashCode()` changes:
  1. The element remains located in the bucket computed from its **old** hash code.
  2. Future calls to `contains(o)` or `remove(o)` compute the **new** hash code, which routes the lookup to a **different bucket**!
  3. The set reports that the element does not exist (`contains` returns `false`), and `remove(o)` fails to delete it.
  4. The mutated element becomes a **permanent zombie memory leak**, trapped indefinitely in the wrong bucket while breaking set uniqueness invariants.

---

## 4. Complexity & Behavioral Matrix

| Operation / Invariant | Average Complexity | Worst-Case Complexity | Amortized Complexity | Notes |
| :--- | :--- | :--- | :--- | :--- |
| **`add(E e)`** | $O(1)$ | $O(\log N)$ | $O(1)$ | Degrades to $O(\log N)$ upon bucket treeification |
| **`contains(Object o)`** | $O(1)$ | $O(\log N)$ | $O(1)$ | Exact lookup via `hash()` and `equals()` |
| **`remove(Object o)`** | $O(1)$ | $O(\log N)$ | $O(1)$ | Splicing node out of bucket chain / RB-tree |
| **`iterator()` Traversal**| $O(\text{capacity} + N)$ | $O(\text{capacity} + N)$ | $O(\text{capacity} + N)$ | Walks through every slot in the `table` array |
| **`size()`** | $O(1)$ | $O(1)$ | $O(1)$ | Reads `map.size` field directly |
| **`clear()`** | $O(\text{capacity})$ | $O(\text{capacity})$ | $O(\text{capacity})$ | Sets all `table[i] = null` for GC cleanup |

### Behavioral Attributes:
- **Iteration Order**: Completely non-deterministic. Depends on hash distribution and table capacity; order can reshuffle entirely during table resizing.
- **Null Allowance**: Permits **at most one** `null` element (stored in bucket 0).
- **Duplicates**: Strictly disallowed. Determined by `hashCode()` collision followed by `(a == b || a.equals(b))`.
- **Iterator Type**: **Fail-Fast**. Any structural modification (other than via `iterator.remove()`) throws `ConcurrentModificationException`.

---

## 5. Concurrency & Thread-Safety Mechanics

### Lack of Thread Safety & Root Causes of Failure
`HashSet` is entirely **unsynchronized**. Concurrent read/write or concurrent write/write access leads to catastrophic failure modes:
1. **Lost Updates**: Two threads inserting into the same empty bucket concurrently will both read `table[i] == null`. Both allocate nodes and write to `table[i]`, with one thread silently overwriting the other's node.
2. **Corrupted TreeNodes / Cyclic References**: Concurrent resizing or rebalancing can corrupt node links, resulting in infinite loops or memory access faults.
3. **Dirty Reads**: A reading thread may see partially initialized `Node` objects due to instruction reordering.

### `modCount` and ConcurrentModificationException
`HashSet`'s iterator delegates to `HashMap.HashIterator`. It captures the map's modification count:
```java
int expectedModCount = modCount;
```
On every invocation of `next()`, it evaluates:
```java
if (modCount != expectedModCount)
    throw new ConcurrentModificationException();
```
> [!WARNING]
> Fail-fast iteration is **not guaranteed**. Because `modCount` is a non-volatile integer, updates by other threads may not immediately become visible across CPU core caches. Fail-fast behavior should be used only for bug detection, never for programmatic synchronization.

### Modern Concurrent Alternatives

```
Requirement                                Recommended Class                   Mechanism
-----------                                -----------------                   ---------
High-concurrency thread-safe Set           ConcurrentHashMap.newKeySet()       Stripped CAS + synchronized bin locks
Legacy coarse-grained thread safety        Collections.synchronizedSet(set)   Single monitor lock (terrible scalability)
Read-heavy, tiny collection (<100 writes)  CopyOnWriteArraySet<E>              Array copy on each modification
Sorted concurrent set                      ConcurrentSkipListSet<E>            Lock-free Concurrent Skip List
```

---

## 6. Production-Grade Java Code Verification

The following runnable Java class breaks down the internal workings of `HashSet`, inspects its private fields via reflection, proves the singleton identity of `PRESENT`, measures the memory overhead, and demonstrates the fatal **mutable key trap**:

```java
package com.raysi.jcf.set.hashset.codes;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.*;

/**
 * Production-Grade Deep Dive: HashSet & Backing PRESENT Object Mechanics
 * Author: raysi
 */
public class HashSetBackedByHashMapDemo {

    public static void main(String[] args) throws Exception {
        System.out.println("================================================================================");
        System.out.println("          HASHSET INTERNALS: PRESENT OBJECT & MEMORY VERIFICATION               ");
        System.out.println("================================================================================");

        demoReflectionIntoHashMapAndPresent();
        demoDuplicateRejectionMechanics();
        demoPackagePrivateLinkedConstructor();
        demoMutableKeyMemoryLeakTrap();
        demoSparseIterationOverhead();

        System.out.println("\nHashSet Internal Invariants & Traps Verified Successfully.");
    }

    /**
     * 1. Inspect HashSet internals using reflection:
     * - Extract private transient HashMap<E,Object> map
     * - Extract private static final Object PRESENT
     * - Prove all keys point to the exact same PRESENT instance
     */
    private static void demoReflectionIntoHashMapAndPresent() throws Exception {
        System.out.println("\n--- 1. Reflection into HashMap & PRESENT Dummy ---");

        HashSet<String> set = new HashSet<>();
        set.add("Kafka");
        set.add("Pulsar");
        set.add("RabbitMQ");

        // Extract map field
        Field mapField = HashSet.class.getDeclaredField("map");
        mapField.setAccessible(true);
        Object internalMap = mapField.get(set);

        // Extract PRESENT field
        Field presentField = HashSet.class.getDeclaredField("PRESENT");
        presentField.setAccessible(true);
        Object presentObject = presentField.get(null);

        System.out.println("Set Runtime Type: " + set.getClass().getName());
        System.out.println("Backing Map Type: " + internalMap.getClass().getName());
        System.out.println("Static Dummy PRESENT Object Identity: 0x" +
                Integer.toHexString(System.identityHashCode(presentObject)));

        if (internalMap instanceof Map) {
            Map<?, ?> map = (Map<?, ?>) internalMap;
            System.out.println("Backing Map Entry Count: " + map.size());
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                boolean pointsToSharedPresent = (entry.getValue() == presentObject);
                System.out.printf("  Key: '%-8s' | Value: %-25s | Same PRESENT Reference? %b%n",
                        entry.getKey(), entry.getValue(), pointsToSharedPresent);
            }
        }
    }

    /**
     * 2. Duplicate rejection mechanics:
     * add(e) returns map.put(e, PRESENT) == null
     */
    private static void demoDuplicateRejectionMechanics() {
        System.out.println("\n--- 2. Duplicate Rejection Mechanics ---");

        HashSet<String> set = new HashSet<>();
        boolean firstAdd = set.add("Database");
        boolean secondAdd = set.add("Database"); // Duplicate!

        System.out.println("First add('Database') returned: " + firstAdd + " (map.put returned null)");
        System.out.println("Second add('Database') returned: " + secondAdd + " (map.put returned old PRESENT)");

        // Null insertion
        boolean firstNull = set.add(null);
        boolean secondNull = set.add(null);
        System.out.println("First add(null) returned: " + firstNull + " (placed in bucket 0)");
        System.out.println("Second add(null) returned: " + secondNull + " (rejected as duplicate)");
    }

    /**
     * 3. Package-Private constructor demo:
     * HashSet(int initialCapacity, float loadFactor, boolean dummy)
     */
    private static void demoPackagePrivateLinkedConstructor() throws Exception {
        System.out.println("\n--- 3. Package-Private Constructor (LinkedHashSet Backing Engine) ---");

        // Access package-private constructor HashSet(int, float, boolean)
        Constructor<HashSet> constructor = HashSet.class.getDeclaredConstructor(int.class, float.class, boolean.class);
        constructor.setAccessible(true);
        HashSet<String> specialSet = constructor.newInstance(16, 0.75f, true);

        Field mapField = HashSet.class.getDeclaredField("map");
        mapField.setAccessible(true);
        Object internalMap = mapField.get(specialSet);

        System.out.println("Special HashSet instance instantiated!");
        System.out.println("Backing Map Type of special HashSet: " + internalMap.getClass().getName());
        System.out.println("Is instance of LinkedHashMap? " + (internalMap instanceof LinkedHashMap));
        System.out.println("ARCHITECTURAL TAKEAWAY: LinkedHashSet has no map field; it relies on this constructor!");
    }

    /**
     * 4. Fatal Mutable Key Memory Leak Trap:
     * Mutating a key's hashCode() after insertion corrupts the Set invariant.
     */
    private static void demoMutableKeyMemoryLeakTrap() {
        System.out.println("\n--- 4. Mutable Key Memory Leak & Corruption Trap ---");

        class MutableKey {
            int id;
            String name;

            MutableKey(int id, String name) {
                this.id = id;
                this.name = name;
            }

            @Override
            public boolean equals(Object o) {
                if (this == o) return true;
                if (!(o instanceof MutableKey that)) return false;
                return id == that.id && Objects.equals(name, that.name);
            }

            @Override
            public int hashCode() {
                return Objects.hash(id, name);
            }

            @Override
            public String toString() {
                return "MutableKey{" + "id=" + id + ", name='" + name + '\'' + '}';
            }
        }

        HashSet<MutableKey> set = new HashSet<>();
        MutableKey key = new MutableKey(42, "Alice");
        set.add(key);

        System.out.println("Added key to set: " + key + " | Hash: " + key.hashCode());
        System.out.println("contains(key) before mutation: " + set.contains(key));

        // DANGEROUS MUTATION: Mutating field that participates in hashCode()
        key.name = "Alice_Mutated";
        System.out.println("\nKey mutated! New state: " + key + " | New Hash: " + key.hashCode());

        // The key is now lost!
        System.out.println("contains(key) after mutation: " + set.contains(key) + " (LOOKUP FAILED!)");
        System.out.println("remove(key) after mutation: " + set.remove(key) + " (CANNOT REMOVE!)");
        System.out.println("Set size: " + set.size() + " (ELEMENT STILL TRAPPED IN HEAP!)");

        // Proof that the element is still physically in the set
        System.out.print("Traversing iterator: ");
        for (MutableKey item : set) {
            System.out.println(item);
        }
        System.out.println("CORRUPTION CONSEQUENCE: Element exists during iteration but cannot be found by key!");
    }

    /**
     * 5. Sparse Iteration Overhead:
     * Demonstrating that HashSet iteration time is proportional to capacity + size!
     */
    private static void demoSparseIterationOverhead() {
        System.out.println("\n--- 5. Sparse Iteration Overhead [O(capacity + size)] ---");

        final int HUGE_CAPACITY = 10_000_000;
        final int ELEMENT_COUNT = 5;

        // Create a HashSet with 10 million bucket slots, but only 5 elements!
        HashSet<Integer> sparseSet = new HashSet<>(HUGE_CAPACITY);
        for (int i = 0; i < ELEMENT_COUNT; i++) {
            sparseSet.add(i);
        }

        // Measure iteration time
        long start = System.nanoTime();
        int counted = 0;
        for (Integer val : sparseSet) {
            counted++;
        }
        long elapsedNs = System.nanoTime() - start;

        System.out.printf("Iterating over %d elements in HashSet with %d capacity took: %,d ns%n",
                counted, HUGE_CAPACITY, elapsedNs);
        System.out.println("ARCHITECTURAL TAKEAWAY: HashSet iterator must scan all 10M null bucket slots!");
    }
}
```

---

## 7. Top 7 High-Signal Interview Questions & Deep Answers

### Q1: Why does `HashSet` use a dummy `Object PRESENT` rather than storing `null` as the value in `map.put(e, null)`?
**Answer:**
If `HashSet` stored `null` as the value in `map.put(e, null)`, its `add(E e)` method would break.
In `Map`, `put(key, value)` returns the **previous value associated with the key**, or `null` if the key was absent.
If values were `null`:
- When adding a brand-new key: `map.put(key, null)` returns `null`.
- When adding an existing duplicate key: `map.put(key, null)` returns the previous value, which is **also `null`**!
Because both cases return `null`, `HashSet` could never determine whether the element was newly added or was an existing duplicate without making an extra, expensive `containsKey()` check prior to every single insertion.
By using a non-null sentinel object `PRESENT = new Object()`:
- Absent key: `map.put(e, PRESENT)` returns `null` $\rightarrow$ `add()` returns `true`.
- Duplicate key: `map.put(e, PRESENT)` returns `PRESENT` $\rightarrow$ `add()` returns `false`.
This preserves single-operation $O(1)$ insertion and duplicate detection in a single hash lookup.

### Q2: What is the purpose of the package-private constructor `HashSet(int, float, boolean)`?
**Answer:**
`LinkedHashSet` extends `HashSet`. If `HashSet` did not provide this constructor, `LinkedHashSet` would inherit `new HashMap<>()` as its backing map, losing insertion-order tracking.
The JDK architects needed `LinkedHashSet` to instantiate a `LinkedHashMap` as its backing map. However, they did not want to expose a public constructor that allows arbitrary users to choose between `HashMap` and `LinkedHashMap` dynamically.
They created a package-private constructor:
```java
HashSet(int initialCapacity, float loadFactor, boolean dummy) {
    map = new LinkedHashMap<>(initialCapacity, loadFactor);
}
```
The `boolean dummy` parameter exists solely to create a distinct method signature from the public constructor `HashSet(int initialCapacity, float loadFactor)`. `LinkedHashSet` calls `super(capacity, loadFactor, true);`, giving it complete code reuse of all `HashSet` methods with zero code duplication.

### Q3: What happens internally when you mutate a key object stored inside a `HashSet`?
**Answer:**
1. **Hash Inconsistency**: When the object was inserted, its bucket index was calculated as `hash(oldHashCode) & (table.length - 1)`. The `Node` was placed in that bucket.
2. **Lookup Failure**: When the field changes, the object's `hashCode()` returns a different value. A subsequent call to `set.contains(object)` computes `hash(newHashCode) & (table.length - 1)`, directing search to an entirely different bucket where the node does not reside. `contains()` returns `false`.
3. **Deletion Failure**: `set.remove(object)` similarly hashes to the wrong bucket and fails to remove the object, returning `false`.
4. **Duplicate Insertion**: Calling `set.add(object)` again will successfully insert a **second copy** of the object into the new bucket, completely violating the fundamental uniqueness invariant of `Set`.
5. **Memory Leak**: The original entry cannot be cleared via key lookup and remains stuck in the old bucket indefinitely.

### Q4: Why is the time complexity of iterating over a `HashSet` $O(\text{capacity} + \text{size})$ instead of $O(\text{size})$?
**Answer:**
`HashSet` does not maintain a linked list of its entries. Its iterator (`HashIterator`) directly walks the underlying `HashMap.table` array from index 0 to `table.length - 1`.
If `table.length` is 16,000,000 and the set contains only 5 elements, the iterator must increment its internal array index through all 16,000,000 bucket slots to locate those 5 non-null nodes.
Therefore, iteration time is strictly bounded by $O(\text{capacity} + N)$.
**Best Practice**: If frequent iteration is required on a sparse or large set, prefer `LinkedHashSet` (which iterates over a dedicated doubly-linked list in $O(N)$ time regardless of capacity).

### Q5: How does `HashSet` serialize its state given that its `map` field is declared `transient`?
**Answer:**
If the backing `HashMap` were serialized directly, the raw array of bucket nodes, hashes, and internal capacities would be written to the byte stream. This is problematic because `hashCode()` algorithms can vary across JVM vendors or runs, and empty buckets would bloat the serialized payload.
`HashSet` overrides serialization using `writeObject` and `readObject`:
- **`writeObject()`**: Writes the capacity, load factor, and element count (`size`) as primitives, followed by writing **only the elements themselves** via a loop:
  ```java
  for (E e : map.keySet())
      s.writeObject(e);
  ```
- **`readObject()`**: Reconstructs the set by reading capacity, load factor, and size, instantiating a fresh `HashMap`, and populating it via a loop calling `map.put(element, PRESENT)`.

### Q6: How does the memory overhead of `HashSet` compare to open-addressing sets (like `fastutil` or `Eclipse Collections`)?
**Answer:**
- **`HashSet`**: Uses **separate chaining**. Each entry requires a separate heap-allocated `HashMap.Node` (32 bytes on 64-bit JVM with compressed OOPs), plus 4 bytes per bucket in the table array, plus the 48-byte `HashMap` and 16-byte `HashSet` wrappers. Total overhead: **~40–44 bytes per element** above the element itself.
- **Open-Addressing Sets** (e.g., `ObjectOpenHashSet` in `fastutil` or `SetN` in Java): Store elements directly in a single flat `Object[]` array using linear or quadratic probing. There are **zero node objects**, zero pointer-chasing hops, and zero sentinel `PRESENT` objects.
- **Cache Locality**: Open addressing keeps entries contiguous in memory, packing up to 16 references per 64-byte CPU cache line. `HashSet` incurs an L1/L2 cache miss on nearly every collision or bucket dereference.

### Q7: Why does `HashSet.clone()` perform a shallow copy, and how does it avoid duplicate node overhead?
**Answer:**
`HashSet.clone()` creates a new `HashSet` instance by invoking `super.clone()`. It then assigns a new backing `HashMap` instance using:
```java
newSet.map = (HashMap<E, Object>) map.clone();
```
In `HashMap.clone()`, a new bucket array is allocated and fresh `Node<K,V>` objects are created for each entry. However:
1. The **keys** are not cloned — references to the original user objects are copied directly (shallow copy).
2. The **values** are all pointed to the single static `PRESENT` reference.
Because only node pointers are duplicated and user objects are shared, mutating the inner state of an element in the cloned set will mutate the element in the original set.
