# 05. Concurrency, Fail-Fast Iterators, and Map Alternatives

## 1. Why `HashMap` Is Not Thread-Safe

`java.util.HashMap` contains **zero synchronization constructs**. Modifying a `HashMap` across multiple concurrent threads leads to race conditions, silent data corruption, inconsistent sizes, and CPU starvation.

### A. Race Condition on `put()`: Silent Overwrites
When two threads invoke `put()` simultaneously on the same bucket index:
1. Thread 1 calculates index `i` and reads `table[i] == null`.
2. Context switch: Thread 1 pauses before instantiating the node.
3. Thread 2 calculates index `i`, reads `table[i] == null`, creates `Node B`, and writes `table[i] = Node B`.
4. Thread 1 resumes: It already checked that `table[i]` was null, so it creates `Node A` and writes `table[i] = Node A`.
5. **Outcome**: `Node B` is overwritten and completely lost. No exception is thrown, causing silent data corruption.

### B. The Java 7 Infinite Loop Bug (Circular Linked List)

In Java 7, `resize()` invoked `transfer()`, which moved nodes into the new table using **Head Insertion** (reversing the bucket chain):

```java
// Java 7 transfer() method (Simplified)
void transfer(Entry[] newTable) {
    Entry[] src = table;
    int newCapacity = newTable.length;
    for (int j = 0; j < src.length; j++) {
        Entry<K,V> e = src[j];
        if (e != null) {
            src[j] = null;
            do {
                Entry<K,V> next = e.next; // Thread 1 paused here!
                int i = indexFor(e.hash, newCapacity);
                e.next = newTable[i];     // Head insertion reverses order!
                newTable[i] = e;
                e = next;
            } while (e != null);
        }
    }
}
```

#### How the Circular Dependency Forms:
1. Suppose Bucket 2 contains a linked list: `A -> B -> null`.
2. **Thread 1** begins resizing. It points `e = A` and `next = B`. Right after this, **Thread 1 is descheduled (sleeps)**.
3. **Thread 2** runs to completion. It resizes the table:
   - Node `A` is placed at `newTable[i]`.
   - Node `B` is placed at head: `B.next = A`, and `A.next = null`.
   - The chain is now reversed: `B -> A -> null`.
4. **Thread 1** resumes:
   - For Thread 1, `e = A` and `next = B`.
   - Thread 1 sets `A.next = newTable[i]` and inserts `A`.
   - In the next iteration, `e = B`. Thread 1 reads `next = e.next`. But in memory, `B.next` was already changed by Thread 2 to point to `A`!
   - Thread 1 sets `B.next = A`, while `A.next` already points to `B`!
5. **Result**: A circular reference is formed:
   $$\text{A} \longrightarrow \text{B} \longrightarrow \text{A} \longrightarrow \text{B} \dots$$
6. **Catastrophe**: Any subsequent thread calling `map.get("someKey")` that maps to this bucket enters an infinite `while (e != null)` loop, pinning the CPU core to **100% utilization**.

#### Does Java 8 Fix This?
- Java 8 replaces head insertion with **tail insertion** using `loTail` and `hiTail`, preserving original chain ordering and preventing this specific circular loop during resize.
- **However, Java 8 is still NOT thread-safe**:
  - Concurrent writes can corrupt tree pointers in Red-Black Tree buckets, causing infinite loops during tree traversal.
  - `size++` is not atomic, leading to incorrect size reporting.
  - Silent data loss still occurs.

---

## 2. Fail-Fast Iterators and `ConcurrentModificationException`

Iterators in `HashMap` are **fail-fast**. If the map is structurally modified (an entry added, removed, or table resized) at any time after the iterator is created—in any way except through the iterator's own `remove()` method—the iterator throws `ConcurrentModificationException`.

```java
Map<String, Integer> map = new HashMap<>();
map.put("A", 1);
map.put("B", 2);

for (String key : map.keySet()) {
    if ("A".equals(key)) {
        map.remove(key); // Throws ConcurrentModificationException!
    }
}
```

### How Fail-Fast Is Implemented Internally

1. `HashMap` maintains an internal counter:
   ```java
   transient int modCount;
   ```
2. Any structural mutation (`put`, `remove`, `clear`, `resize`) increments `modCount++`.
3. When an iterator is initialized, it captures this counter:
   ```java
   int expectedModCount = modCount;
   ```
4. On every call to `iterator.next()` or `iterator.remove()`, it verifies:
   ```java
   if (modCount != expectedModCount)
       throw new ConcurrentModificationException();
   ```

### Safe Modification During Iteration

To safely delete elements while iterating, use the iterator's own `remove()` method:

```java
Iterator<Map.Entry<String, Integer>> it = map.entrySet().iterator();
while (it.hasNext()) {
    Map.Entry<String, Integer> entry = it.next();
    if ("A".equals(entry.getKey())) {
        it.remove(); // SAFE! Synchronizes expectedModCount = modCount
    }
}
```

Or in modern Java 8+:
```java
map.entrySet().removeIf(entry -> "A".equals(entry.getKey()));
```

---

## 3. Comprehensive Map Implementations Comparison

```
                           Map (Interface)
                                 |
      +--------------------------+-------------------------+
      |                          |                         |
   HashMap                  SortedMap (Interface)       Hashtable (Legacy)
      |                          |
+-----+-----+               NavigableMap (Interface)
|           |                    |
LinkedHashMap IdentityHashMap  TreeMap
```

### Detailed Feature Comparison Matrix

| Feature | `HashMap` | `Hashtable` | `Collections.synchronizedMap` | `ConcurrentHashMap` (Java 8+) | `LinkedHashMap` | `TreeMap` |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| **Underlying Structure** | Array + LinkedList + Red-Black Tree | Array + LinkedList | Wrapper around any Map | Array + LinkedList + Tree + CAS | HashMap + Doubly LinkedList | Red-Black Tree |
| **Thread Safety** | ❌ No | ✅ Yes (`synchronized` methods) | ✅ Yes (`mutex` object) | ✅ Yes (Lock-free reads, CAS + sync on bucket) | ❌ No | ❌ No |
| **Null Keys / Values** | 1 Null Key, Any Null Values | ❌ Disallows both | Depends on wrapped map | ❌ Disallows both | 1 Null Key, Any Null Values | ❌ Null keys disallowed (natural order); null values OK |
| **Ordering** | ❌ Unordered | ❌ Unordered | Unordered | ❌ Unordered | ✅ Insertion Order or Access Order (LRU) | ✅ Sorted Order (Natural / Comparator) |
| **Time Complexity** | $O(1)$ avg, $O(\log n)$ worst | $O(1)$ avg, $O(n)$ worst | $O(1)$ avg | $O(1)$ avg, $O(\log n)$ worst | $O(1)$ avg | $O(\log n)$ guaranteed |
| **Iterator Type** | Fail-fast | Enumeration / Fail-fast | Fail-fast (requires external sync) | **Weakly Consistent** (never throws CME) | Fail-fast | Fail-fast |
| **Concurrency Model** | None | Full table lock on every method | Full table lock on `mutex` | Bucket-level CAS + fine-grained sync | None | None |

---

## 4. Deep-Dive on Map Alternatives

### 1. `ConcurrentHashMap` (The Modern Concurrent Standard)
- **Java 7**: Segment-based locking (16 `Segment` locks extending `ReentrantLock`).
- **Java 8+**:
  - **Bucket-Level CAS**: If the bucket is empty, a new node is inserted via atomic CAS (`Compare-And-Swap`) with **zero locking**.
  - **Bucket-Level `synchronized`**: If a bucket is occupied, only the **head node** of that individual bucket is synchronized (`synchronized(f)`). Threads writing to other buckets execute simultaneously without contention.
  - **Lock-Free Reads**: Reads (`get()`) execute with **zero locks** because node values and next pointers are declared `volatile`.
  - **Why nulls are disallowed**: In concurrent environments, `map.get(key) == null` creates an unsolvable ambiguity: Does the key not exist, or is it mapped to `null`? In single-threaded maps, `containsKey()` resolves this. In multi-threaded maps, a concurrent thread could insert or remove the key between `get()` and `containsKey()`.

### 2. `LinkedHashMap` (Predictable Order & LRU Cache)
- Extends `HashMap` and adds doubly-linked list pointers (`before` and `after`) to every node.
- Supports two ordering modes:
  1. **Insertion-Order** (Default): Iterates in the order keys were inserted.
  2. **Access-Order** (`accessOrder = true`): Whenever `get()` or `put()` accesses an entry, it is moved to the tail.
- Perfect for implementing an **LRU (Least Recently Used) Cache** by overriding `removeEldestEntry`:
  ```java
  class LRUCache<K, V> extends LinkedHashMap<K, V> {
      private final int maxEntries;
      public LRUCache(int maxEntries) {
          super(maxEntries, 0.75f, true); // true = access-order
          this.maxEntries = maxEntries;
      }
      @Override
      protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
          return size() > maxEntries;
      }
  }
  ```

### 3. `TreeMap` (Sorted & Range Operations)
- Backed by a Red-Black Tree.
- Keeps keys sorted either by their natural order (`Comparable`) or a custom `Comparator`.
- Implements `NavigableMap`, providing operations like `firstKey()`, `lastKey()`, `headMap()`, `tailMap()`, `ceilingKey()`, and `floorKey()`.
- Guaranteed $O(\log n)$ performance for all operations.

### 4. `IdentityHashMap`
- Violates the general `Map` contract: compares keys using **reference equality (`==`)** instead of `equals()`.
- Uses `System.identityHashCode()` for hash codes.
- Backed by an array using linear probing open-addressing.
- Ideal for graph cycle detection, object serialization frameworks, and compiler symbol tables.

### 5. `WeakHashMap`
- Uses `java.lang.ref.WeakReference` for its keys.
- When an object key no longer has any strong references in the application, the JVM Garbage Collector automatically reclaims it.
- During subsequent map operations, the dead entries are expunged from the map.
- Ideal for memory-sensitive caches and metadata tracking.
