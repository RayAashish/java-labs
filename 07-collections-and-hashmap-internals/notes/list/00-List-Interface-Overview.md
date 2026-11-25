# <List Interface — Deep Dive>
> Author: raysi

---

## 1. Architectural Overview & Hierarchy

The `java.util.List<E>` interface represents an **ordered collection** (also known as a *sequence*). Unlike a general mathematical `Set`, the user of `List` has precise, zero-indexed positional control over where each element is inserted into the sequence, can access elements by their integer index, and can insert duplicate elements as well as `null` values (implementation permitting).

### Exact Package and Interface Signature

```java
package java.util;

public interface List<E> extends Collection<E>
```

`List<E>` is parameterized over the element reference type `E`. It extends `java.util.Collection<E>`, which in turn extends `java.lang.Iterable<E>`.

### ASCII Inheritance & Implementation Diagram

```
                             +-------------------+
                             | java.lang.Iterable|
                             +---------+---------+
                                       |
                                       v
                           +-----------------------+
                           |  java.util.Collection |
                           +-----------+-----------+
                                       |
                                       v
                             +-------------------+
                             |  java.util.List   |
                             +---------+---------+
                                       |
       +-------------------------------+-------------------------------+
       |                               |                               |
       v                               v                               v
+------------------+         +-------------------+         +------------------------+
| java.util.Random |         | java.util.        |         | java.util.concurrent.  |
| Access (Marker)  |         | AbstractList      |         | CopyOnWriteArrayList   |
+--------+---------+         +---------+---------+         +------------------------+
         |                             |
         |         +-------------------+-------------------+
         |         |                                       |
         |         v                                       v
         |   +-----------+                       +--------------------+
         |   | ArrayList |                       | AbstractSequential |
         |   +-----+-----+                       | List               |
         |         ^                             +---------+----------+
         +---------+                                       |
         |                                                 v
         |   +-----------+                       +--------------------+
         +-->|  Vector   |                       |     LinkedList     |
             +-----+-----+                       +--------------------+
                   |
                   v
             +-----------+
             |   Stack   |
             +-----------+
```

### Why Does `List` Exist? What Problem Does It Solve?

In computer science, linear collections split broadly into two fundamental access models:
1. **Unordered / Set-Theoretic Collections (`Set`, `Bag`):** Focus solely on membership testing, uniqueness, and bulk processing. Positional insertion is undefined.
2. **Sequential / Positional Collections (`List`):** Order of arrival, positional index, and explicit rearrangement matter fundamentally.

The `List` interface provides:
- **Zero-Indexed Positional Access:** Direct access, mutation, insertion, and deletion by integer index (`get(int)`, `set(int, E)`, `add(int, E)`, `remove(int)`).
- **Explicit Ordering Semantics:** Elements retain their explicit insertion order unless reordered via `sort()` or positional mutations.
- **Duplicate Support:** Allows multiple identical elements such that `e1.equals(e2)` evaluates to `true`.
- **Bidirectional Traversal:** Provides `listIterator()` producing `ListIterator<E>`, allowing bidirectional traversal (`hasPrevious()`, `previous()`), index queries (`nextIndex()`, `previousIndex()`), and inline mutations (`set(E)`, `add(E)`).
- **Range-View Operations:** `subList(int fromIndex, int toIndex)` allows windowed operations without copying underlying memory buffers.

---

## 2. Internal Data Structure & Memory Layout (JDK Source Code Level)

Because `List<E>` is an interface, it defines no instance fields directly. However, the JDK supplies two foundational skeletal implementations that establish the architectural blueprint for all standard lists: `AbstractList<E>` and `AbstractSequentialList<E>`.

### Exact Internal Fields from Skeletal Implementations

#### `java.util.AbstractList<E>`
```java
public abstract class AbstractList<E> extends AbstractCollection<E> implements List<E> {
    // Structural modification counter for fail-fast iterators
    protected transient int modCount = 0;
}
```

#### `java.util.AbstractSequentialList<E>`
```java
public abstract class AbstractSequentialList<E> extends AbstractList<E> {
    // Delegates all positional operations (get, set, add, remove)
    // to listIterator(index)
}
```

### ASCII Memory Diagram: Contiguous Arrays vs. Scattered Node References

```
ArrayList / Array-backed List (Contiguous Object Reference Array in JVM Heap):
+-------------------------------------------------------------------------+
| Array Object Header (12B/16B) | Array Length (4B) | Padding (0B/4B)     |
+-------------------------------------------------------------------------+
| elementData[0] | elementData[1] | elementData[2] | elementData[3] | ... |
+-------+----------------+----------------+----------------+--------------+
        |                |                |                |
        v                v                v                v
   +---------+      +---------+      +---------+      +---------+
   | E obj 0 |      | E obj 1 |      | E obj 2 |      | E obj 3 |
   +---------+      +---------+      +---------+      +---------+

LinkedList / Node-backed List (Pointer-chasing Heap Graph):
+---------------+       +---------------+       +---------------+
| Node Object 0 | <---> | Node Object 1 | <---> | Node Object 2 |
| - prev (ref)  |       | - prev (ref)  |       | - prev (ref)  |
| - next (ref)  |       | - next (ref)  |       | - next (ref)  |
| - item (ref)  |       | - item (ref)  |       | - item (ref)  |
+-------+-------+       +-------+-------+       +-------+-------+
        |                       |                       |
        v                       v                       v
   +---------+             +---------+             +---------+
   | E obj 0 |             | E obj 1 |             | E obj 2 |
   +---------+             +---------+             +---------+
```

### CPU Cache & Memory Overhead Analysis

1. **Spatial Locality & CPU Cache Line Prefetching:**
   - Modern x86-64 and ARM64 CPUs fetch memory into L1/L2/L3 caches in **64-byte cache lines**.
   - With Compressed OOPs (`-XX:+UseCompressedOops`, active up to a 32GB heap), each element reference in an array occupies **4 bytes**. A single 64-byte cache line brings in **16 contiguous object references** simultaneously.
   - Sequential traversal over an array-backed list triggers the CPU's hardware stream prefetcher, resulting in near-zero L1 cache miss stalls during iteration.
   - Conversely, node-backed lists allocate separate `Node` objects across fragmented JVM heap memory. Following `node.next` requires a pointer dereference that frequently misses L1/L2 caches, causing pipeline stalls (typically 150-250 CPU cycles per main memory access).

2. **Memory Footprint Overhead:**
   - **Array-backed list:** Minimum overhead is the internal `Object[]` reference plus array header (16 bytes) + capacity overhead (unused array slots). Per populated element: exactly 4 bytes (compressed ref) or 8 bytes (uncompressed ref).
   - **Node-backed list:** Every single element requires a distinct `Node` instance:
     - Object Header: 12 bytes (Mark Word: 8B, Klass Word: 4B with compressed class pointers)
     - Three references (`prev`, `next`, `item`): 3 * 4 = 12 bytes
     - 8-byte alignment padding: 0 bytes (total 24 bytes)
     - Plus the 4-byte reference to the node itself from the predecessor/successor.
     - Total overhead per element: **24 bytes minimum** (excluding the actual payload object), which is 6x the reference overhead of an array.

---

## 3. Step-by-Step Internal Working of Core Operations

### 1. Positional Access (`get`, `set`, `add`, `remove`)
- **Index Validation:** The `List` interface dictates that any index out of bounds must throw `IndexOutOfBoundsException`. Skeletal implementations enforce:
  ```java
  private void rangeCheck(int index) {
      if (index < 0 || index >= size)
          throw new IndexOutOfBoundsException(outOfBoundsMsg(index));
  }
  ```
  For insertion (`add(int, E)`), the upper bound is inclusive of `size`: `index < 0 || index > size`.
- **Template Method Dispatch:**
  - `AbstractList` defines `get(int index)` as abstract and leaves its implementation to array indexing (`elementData[index]`).
  - `AbstractSequentialList` implements `get(int index)` by calling `listIterator(index).next()`, incurring an O(n) traversal cost.

### 2. Search & Equality Semantics
- `indexOf(Object o)` and `lastIndexOf(Object o)` operate via linear scan with null-safe comparison:
  ```java
  if (o == null) {
      for (int i = 0; i < size; i++)
          if (elementData[i] == null) return i;
  } else {
      for (int i = 0; i < size; i++)
          if (o.equals(elementData[i])) return i;
  }
  ```
- Any class stored in a `List` must correctly implement `equals(Object)` to support `contains(Object)`, `remove(Object)`, and `indexOf(Object)`.

### 3. Range-View Semantics: `subList(int fromIndex, int toIndex)`
- `subList` does **not** create a shallow or deep copy of the underlying collection.
- It returns a **view** (`AbstractList.SubList` or `ArrayList.SubList`) referencing the **identical backing storage** of the parent list.
- **Fail-Fast Structural Synchronization:** The `SubList` captures `expectedModCount = root.modCount` at creation. If the parent list undergoes structural modification directly (e.g., `parent.add(...)`), the next operation on `subList` detects `root.modCount != this.modCount` and immediately throws `ConcurrentModificationException`.
- **Memory Retention Risk:** The `SubList` retains an object reference to the parent list. If a tiny `subList` (e.g., 2 elements) is retained long-term while the parent list holds 10,000,000 elements, the entire parent list cannot be garbage collected.

### 4. Java 9+ Immutable Factory Methods: `List.of()` and `List.copyOf()`
Introduced in JDK 9 via JEP 269:
- **Implementation Classes:** Hidden inside `java.util.ImmutableCollections`:
  - `List0` (zero elements, singleton instance)
  - `List12<E>` (stores 1 or 2 elements directly in fields `e0`, `e1` without an array!)
  - `ListN<E>` (stores N >= 3 elements in a compact `Object[]` array)
- **Null Hostility:** Calling `List.of(null)` or `List.of("a", null)` throws `NullPointerException` instantly.
- **True Immutability:** Any mutating call (`add`, `set`, `remove`, `clear`, `sort`) throws `UnsupportedOperationException`.
- **Thread Safety:** Inherently thread-safe due to immutable state and freeze semantics during construction.
- **Memory Optimization:** By eliminating array allocation for size 1 and 2, `List.of("a")` uses only 24 bytes compared to ~80 bytes for an unmodifiable `ArrayList`.

### 5. Java 8+ Default Methods: `sort()` and `replaceAll()`
- `default void sort(Comparator<? super E> c)`:
  Instead of copying to an array as `Collections.sort()` previously did, `List.sort()` allows random-access lists to sort in-place using dual-pivot Quicksort or Timsort directly over their internal buffer:
  ```java
  default void sort(Comparator<? super E> c) {
      Object[] a = this.toArray();
      Arrays.sort(a, (Comparator) c);
      ListIterator<E> i = this.listIterator();
      for (Object e : a) {
          i.next();
          i.set((E) e);
      }
  }
  ```
  `ArrayList` overrides this default method to sort `elementData` directly using `Arrays.sort(elementData, 0, size, c)`, avoiding array allocation.
- `default void replaceAll(UnaryOperator<E> operator)`:
  Iterates via `ListIterator` and performs atomic-in-spirit mutation using `i.set(operator.apply(i.next()))`.

---

## 4. Complexity & Behavioral Matrix

| Operation | `ArrayList` | `LinkedList` | `CopyOnWriteArrayList` | `Vector` |
| :--- | :--- | :--- | :--- | :--- |
| **Random Read (`get(i)`)** | $O(1)$ | $O(n)$ | $O(1)$ | $O(1)$ (synchronized) |
| **Write (`set(i, e)`)** | $O(1)$ | $O(n)$ | $O(n)$ | $O(1)$ (synchronized) |
| **Insert at End (`add(e)`)** | $O(1)$ Amortized ($O(n)$ Worst) | $O(1)$ | $O(n)$ | $O(1)$ Amortized |
| **Insert at Head (`add(0, e)`)** | $O(n)$ (array copy shift) | $O(1)$ | $O(n)$ | $O(n)$ |
| **Insert at Mid (`add(i, e)`)** | $O(n)$ (shift right) | $O(n)$ traversal + $O(1)$ link | $O(n)$ | $O(n)$ |
| **Remove at Index (`remove(i)`)** | $O(n)$ (shift left) | $O(n)$ traversal + $O(1)$ link | $O(n)$ | $O(n)$ |
| **Iteration Overhead** | $O(1)$ per step (sequential cache) | $O(1)$ per step (pointer chasing) | $O(1)$ per step (snapshot) | $O(1)$ (synchronized lock overhead) |
| **Memory per Element** | ~4 bytes (ref) | ~24 bytes (node + refs) | ~4 bytes (ref) | ~4 bytes (ref) |

### Implementation Characteristics Matrix

| Feature | `ArrayList` | `LinkedList` | `CopyOnWriteArrayList` | `List.of()` (`ImmutableCollections`) |
| :--- | :--- | :--- | :--- | :--- |
| **Ordering** | Insertion Order | Insertion Order | Insertion Order | Insertion Order |
| **Null Allowed?** | Yes | Yes | Yes | **NO (Throws NPE)** |
| **Duplicates Allowed?** | Yes | Yes | Yes | Yes |
| **RandomAccess Implemented?**| **Yes** | **No** | **Yes** | **Yes** |
| **Iterator Type** | Fail-Fast (`modCount`) | Fail-Fast (`modCount`) | Weakly Consistent (Snapshot) | Fail-Fast / Unmodifiable |
| **Thread Safe?** | No | No | Yes (Lock-on-write) | Yes (Immutable) |

### The `RandomAccess` Marker Interface Significance

`java.util.RandomAccess` contains **zero methods and zero fields**. It is a pure marker interface:
```java
public interface RandomAccess {}
```

#### Why it matters: Algorithmic Branching
Core algorithms in `java.util.Collections` inspect `instanceof RandomAccess` at runtime to choose between $O(1)$ indexed loops and $O(1)$ iterator stepping:

```java
// Snippet from java.util.Collections.binarySearch
public static <T> int binarySearch(List<? extends Comparable<? super T>> list, T key) {
    if (list instanceof RandomAccess || list.size() < BINARYSEARCH_THRESHOLD)
        return Collections.indexedBinarySearch(list, key);
    else
        return Collections.iteratorBinarySearch(list, key);
}
```
If `Collections.binarySearch` performed `indexedBinarySearch` on a `LinkedList`, repeated calls to `list.get(mid)` would degrade binary search from $O(\log n)$ to $O(n \log n)$ — worse than linear scan!

---

## 5. Concurrency & Thread-Safety Mechanics

### The `modCount` Contract and `ConcurrentModificationException`
Standard List implementations (`ArrayList`, `LinkedList`, `Vector`) inherit `protected transient int modCount` from `AbstractList`.
- Every structural modification (additions, removals, reallocations) increments `modCount++`.
- When an `Iterator` or `ListIterator` is created, it captures `expectedModCount = modCount`.
- On every call to `it.next()`, `it.remove()`, or `it.add()`, the iterator checks:
  ```java
  final void checkForComodification() {
      if (modCount != expectedModCount)
          throw new ConcurrentModificationException();
  }
  ```
- **Crucial Nuance:** `ConcurrentModificationException` does not require multiple threads! A single thread modifying a list while iterating over it will trigger CME immediately.

### Thread-Safety Analysis
- Standard lists (`ArrayList`, `LinkedList`) provide **no synchronization barriers**, memory fences, or volatile writes.
- Concurrent reads and writes result in race conditions:
  - Lost updates (overwriting elements at the same index)
  - Corrupted internal sizes (`size` out of sync with actual element count)
  - `ArrayIndexOutOfBoundsException` or null reference exposure during resizing.

### Modern Concurrent Alternatives
1. **`java.util.concurrent.CopyOnWriteArrayList`:** Copy-on-write semantics. Perfect for read-heavy observer patterns.
2. **`Collections.synchronizedList(List<T> list)`:** Wraps all operations inside a mutex block (`synchronized(mutex)`). Requires explicit synchronization during iteration:
   ```java
   List<String> syncList = Collections.synchronizedList(new ArrayList<>());
   synchronized (syncList) {
       for (String s : syncList) { /* process */ }
   }
   ```
3. **Immutability via `List.of()` or `List.copyOf()`:** Provides zero-synchronization, high-throughput concurrent reads across all threads.

---

## 6. Production-Grade Java Code Verification

The following self-contained class demonstrates non-obvious `List` contract nuances:
- Mutability differences between `List.of()`, `Arrays.asList()`, and `new ArrayList<>()`
- Structural modifications breaking `subList` views
- Accidental method overload resolution trap with `remove(int)` vs `remove(Object)`
- Algorithmic dispatch based on `RandomAccess`

```java
package com.raysi.jcf.list.codes;

import java.util.*;

/**
 * Verification of List interface behavioral semantics, factory differences,
 * subList fail-fast behavior, and method overload traps.
 *
 * @author raysi
 */
public class ListInterfaceMethodsDemo {

    public static void main(String[] args) {
        System.out.println("=== 1. Factory Creation & Mutability Comparison ===");
        verifyFactoryBehaviors();

        System.out.println("\n=== 2. SubList View Semantics & CME Trap ===");
        verifySubListSemantics();

        System.out.println("\n=== 3. remove(int) vs remove(Object) Overload Trap ===");
        verifyRemoveOverloadTrap();

        System.out.println("\n=== 4. RandomAccess Marker Impact on Algorithms ===");
        verifyRandomAccessDispatch();
    }

    private static void verifyFactoryBehaviors() {
        // A. Arrays.asList: Fixed-size view of an array.
        // Mutation of elements allowed, but structural addition/removal throws UnsupportedOperationException.
        String[] rawArray = new String[]{"Alpha", "Beta", "Gamma"};
        List<String> arraysAsList = Arrays.asList(rawArray);
        arraysAsList.set(0, "ALPHA_MUTATED");
        System.out.println("Arrays.asList mutated backing array: " + rawArray[0]); // Backing array modified!

        try {
            arraysAsList.add("Delta");
        } catch (UnsupportedOperationException e) {
            System.out.println("Arrays.asList correctly rejected add(): Fixed size.");
        }

        // B. List.of(): Truly immutable, null-hostile (Java 9+)
        try {
            List<String> nullHostile = List.of("A", null);
        } catch (NullPointerException e) {
            System.out.println("List.of rejected null element with NPE as per contract.");
        }

        List<String> immutableList = List.of("One", "Two");
        try {
            immutableList.set(0, "Mutate");
        } catch (UnsupportedOperationException e) {
            System.out.println("List.of rejected set() with UnsupportedOperationException.");
        }
    }

    private static void verifySubListSemantics() {
        List<String> master = new ArrayList<>(List.of("A", "B", "C", "D", "E"));
        List<String> sub = master.subList(1, 4); // Elements: "B", "C", "D"

        System.out.println("Initial master: " + master);
        System.out.println("Initial subList: " + sub);

        // 1. Mutation through subList writes through to master!
        sub.set(0, "B_MODIFIED");
        System.out.println("Master after subList.set(): " + master);

        // 2. Structural mutation on subList updates master size and bounds
        sub.add("C_INSERT");
        System.out.println("Master after subList.add(): " + master);

        // 3. Direct structural mutation on parent INVALIDATES subList
        master.add("F");
        try {
            sub.get(0); // Expected: ConcurrentModificationException
        } catch (ConcurrentModificationException e) {
            System.out.println("SubList threw CME after direct parent modification: expectedModCount mismatch!");
        }
    }

    private static void verifyRemoveOverloadTrap() {
        // High-stakes interview trap: remove(int index) vs remove(Object obj) with Integer
        List<Integer> intList = new ArrayList<>();
        intList.add(10);
        intList.add(20);
        intList.add(30);

        // Calling remove(1) calls remove(int index), NOT removing value 1!
        Integer removedByIndex = intList.remove(1);
        System.out.println("remove(1) removed element at index 1: " + removedByIndex); // Removes 20
        System.out.println("Remaining list: " + intList); // [10, 30]

        // To remove value 10 by object equality:
        boolean removedByObject = intList.remove(Integer.valueOf(10));
        System.out.println("remove(Integer.valueOf(10)) removed by object: " + removedByObject);
        System.out.println("Final list: " + intList); // [30]
    }

    private static void verifyRandomAccessDispatch() {
        List<Integer> arrayList = new ArrayList<>();
        List<Integer> linkedList = new LinkedList<>();

        for (int i = 0; i < 100_000; i++) {
            arrayList.add(i);
            linkedList.add(i);
        }

        System.out.println("arrayList instanceof RandomAccess: " + (arrayList instanceof RandomAccess));
        System.out.println("linkedList instanceof RandomAccess: " + (linkedList instanceof RandomAccess));

        // Binary search timing comparison
        long start = System.nanoTime();
        int idx1 = Collections.binarySearch(arrayList, 99_999);
        long arrayListDuration = System.nanoTime() - start;

        start = System.nanoTime();
        int idx2 = Collections.binarySearch(linkedList, 99_999);
        long linkedListDuration = System.nanoTime() - start;

        System.out.println("ArrayList binarySearch (RandomAccess): " + arrayListDuration + " ns");
        System.out.println("LinkedList binarySearch (Sequential): " + linkedListDuration + " ns");
        System.out.println("LinkedList slowdown factor: ~" + (linkedListDuration / Math.max(1, arrayListDuration)) + "x");
    }
}
```

---

## 7. Top 7 High-Signal Interview Questions & Deep Answers

### Q1: What is the exact technical difference between `Arrays.asList()`, `List.of()`, and `Collections.unmodifiableList(new ArrayList<>())`?
**Deep Answer:**
- **`Arrays.asList(T... a)`**: Returns an instance of the private static inner class `java.util.Arrays.ArrayList` (NOT `java.util.ArrayList`). It is an **array wrapper**. It does not allow structural modification (`add`, `remove` throw `UnsupportedOperationException` because the backing array length is fixed). However, it is **mutable in-place**: calling `set(i, val)` modifies the underlying array. Any changes to the original array reflect in the list and vice versa. It permits `null` values.
- **`List.of(E... elements)` (JDK 9+)**: Returns a specialized instance from `java.util.ImmutableCollections` (`List12`, `ListN`). It is **completely immutable** (mutating methods throw `UnsupportedOperationException`). It is **null-hostile** (throws NPE if any element is null). It does not wrap an external array (it clones on creation or uses compact instance fields for 1-2 elements). It consumes substantially less memory.
- **`Collections.unmodifiableList(list)`**: Returns a **decorator view** (`UnmodifiableList`) wrapping any existing `List`. The wrapper delegates read operations to the underlying list and blocks write operations. However, if code retains a reference to the underlying delegate `List`, modifying the delegate directly mutates the view. It is neither immutable nor snapshot-isolated.

### Q2: Why does `List.subList()` throw `ConcurrentModificationException` when the parent list is structurally modified, and what is its GC memory leak trap?
**Deep Answer:**
`subList` returns an inner class instance (`SubList`) that does not duplicate elements. It holds three critical fields: `parent`, `offset`, and `size`, along with an `expectedModCount` captured from `parent.modCount` at creation.
When any method on `SubList` is called, it verifies `if (parent.modCount != this.expectedModCount) throw new ConcurrentModificationException();`. If a structural mutation occurs directly on the parent list, `parent.modCount` increments, causing all subsequent operations on the previously created `subList` to fail fast.
**Memory Leak Trap:** The `SubList` holds a hard reference to its parent list (`private final AbstractList<E> root;`). If an application processes a 1GB `ArrayList`, extracts a 2-element `subList`, stores that `subList` in a cache or session, and lets the parent reference fall out of scope, the entire 1GB backing array remains rooted and **cannot be garbage collected** until the `subList` reference itself is reclaimed.

### Q3: Why is `RandomAccess` an empty marker interface, and how do core JDK algorithms exploit it?
**Deep Answer:**
`RandomAccess` is a marker interface (like `Serializable` and `Cloneable`) used to communicate at compile and runtime that a `List` implementation supports **constant-time $O(1)$ random access**.
In algorithms like `Collections.binarySearch(List, T)` or `Collections.shuffle(List)`:
```java
if (list instanceof RandomAccess || list.size() < BINARYSEARCH_THRESHOLD)
    return Collections.indexedBinarySearch(list, key);
else
    return Collections.iteratorBinarySearch(list, key);
```
Without this marker, traversing to `mid = (low + high) >>> 1` on an arbitrary `List` would require calling `list.get(mid)`. On a linked structure like `LinkedList`, `get(mid)` requires an $O(n)$ pointer traversal. Running binary search with $O(n)$ access per step results in $\sum_{k=1}^{\log n} \frac{n}{2^k} = O(n)$ or $O(n \log n)$ time, defeating the logarithmic advantage. `RandomAccess` allows the JDK to branch to iterator-based traversal or copy the list to an array before executing the algorithm.

### Q4: How does Java resolve the method call `list.remove(1)` when operating on a `List<Integer>`? How do you reliably remove the value `1`?
**Deep Answer:**
`List<E>` specifies two overloaded remove methods:
1. `E remove(int index)` (defined in `List`)
2. `boolean remove(Object o)` (inherited from `Collection`)

When the argument is the primitive literal `1`, the Java compiler applies the Java Language Specification (JLS §15.12.2) overload resolution rules:
- Subtyping without boxing/unboxing takes precedence over conversions requiring boxing.
- The primitive literal `1` is an exact match for the primitive type `int`.
- Therefore, the compiler resolves to `remove(int index)`. It removes the element at **index 1**, NOT the integer value `1`.
To remove the object representation of `1`, boxing must be forced explicitly: `list.remove(Integer.valueOf(1))` or `list.remove((Object) 1)`.

### Q5: Why was `default void sort(Comparator<? super E> c)` added directly to the `List` interface in Java 8 instead of relying on `Collections.sort(List, Comparator)`?
**Deep Answer:**
Prior to Java 8, `Collections.sort(list, c)` was the standard way to sort a list. Internally, `Collections.sort` dumped all elements from the list into an auxiliary `Object[]` array (`list.toArray()`), sorted the array via `Arrays.sort()`, and then walked the list with a `ListIterator` setting each element back.
For an `ArrayList`, this was redundant and slow: `ArrayList` already possesses an internal `Object[] elementData` array. Allocating a second array and repopulating via `ListIterator` incurred an extra $O(n)$ heap allocation and cache thrashing.
In Java 8, interface default methods enabled `List` to define `default void sort(Comparator<? super E> c)`. This allowed `ArrayList` to override `sort()` and execute:
```java
Arrays.sort((E[]) elementData, 0, size, c);
```
This sorts the contiguous backing array **in-place** with zero intermediate heap allocations, avoiding `ListIterator` overhead. `Collections.sort(list, c)` was then refactored to simply delegate to `list.sort(c)`.

### Q6: How does `ListIterator` differ from `Iterator` at the byte and structural level? How do `add()` and `set()` manipulate cursor state?
**Deep Answer:**
`Iterator` supports unidirectional traversal (`hasNext()`, `next()`) and element removal (`remove()`).
`ListIterator` expands this contract to:
1. **Bidirectional Traversal:** `hasPrevious()`, `previous()`, `nextIndex()`, `previousIndex()`.
2. **In-place Mutation:** `set(E e)` (replaces last element returned by `next()` or `previous()`), and `add(E e)` (inserts element immediately before the cursor).

**Internal Cursor Mechanics:**
A `ListIterator` maintains two conceptual states:
- `int cursor`: Pointer between elements (e.g., between `cursor - 1` and `cursor`).
- `int lastRet = -1`: Index of the last element returned by `next()` or `previous()`.
Calling `set(E)` replaces `elementData[lastRet]`. If `remove()` or `add()` is called, `lastRet` is reset to `-1`. Thus, calling `set()` twice or after `add()` throws `IllegalStateException`.
Calling `add(E e)` inserts at `cursor`, increments `cursor++`, and sets `lastRet = -1`. The inserted element will not be returned by the subsequent call to `next()`, but will be returned by `previous()`.

### Q7: How does Java 9's `List.of()` avoid heap pollution and memory bloat for small collections (0, 1, 2 elements)?
**Deep Answer:**
In traditional Java collections, creating a list of 1 element (`Collections.singletonList(x)`) or `Arrays.asList(x)` involves allocating an object header, integer fields, and an internal `Object[]` reference array (which itself has an object header, length field, and reference pointers).
In JDK 9+, JEP 269 created specialized unmodifiable classes:
- **`List0`:** A static singleton instance. Zero allocations on creation.
- **`List12<E>`:** Has two reference fields: `private final E e0;` and `private final E e1;`.
  When created via `List.of("alpha")`, `e0` stores the reference, and `e1` remains `null`. **No backing array (`Object[]`) is allocated.**
  Memory overhead is reduced to a single JVM object:
  - 12-byte header + two 4-byte compressed references = 20 bytes -> padded to 24 bytes total.
- **`ListN<E>`:** Only when size $\ge 3$ does the factory allocate an underlying `Object[]` array.
This optimization eliminates millions of small array allocations in high-throughput microservices and reduces GC pressure in applications caching small metadata lists.
