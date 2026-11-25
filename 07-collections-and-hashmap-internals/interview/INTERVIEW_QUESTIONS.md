# 07. Collections Framework & `HashMap` Internals — Interview Questions & Answers

> Covers **Core Collections (`List`, `Queue`, `Set`, `Map`, `SequencedCollection`)**, **Concurrent Collections**, and **30 Deep HashMap Internals Questions**.

---

## Part A: Collections Framework Architecture & Data Structures

### Q1. Why does `Map` NOT extend `Collection` or `Iterable`?
`Collection` models a group of individual elements (`add(E)`, `iterator()`), whereas `Map<K,V>` models key-to-value associations (`put(K,V)`). Instead of extending `Iterable`, `Map` exposes three collection views: `keySet()`, `values()`, and `entrySet()`.

### Q2. `ArrayList` vs `LinkedList` — Which is faster and why?
`ArrayList` wins in nearly all real-world workloads:
- `ArrayList` backs elements with a contiguous `Object[]` -> **$O(1)$ random access**, single CPU cache-line friendly sequential iteration, and minimal memory per slot (4B compressed reference). Grows by **$1.5\times$** (`oldCap + (oldCap >> 1)`).
- `LinkedList` allocates a 24B+ `Node<E>` wrapper object per item with `prev`/`next` pointers -> **$O(N)$ index lookup**, heavy GC pressure, and L1/L2 CPU cache misses on traversal.

### Q3. Why are `Vector` and `Stack` legacy, and what replaces them?
`Vector` and `Stack` synchronize **every single method** on `this` monitor (even single-threaded loops pay lock overhead, and compound check-then-act isn't atomic anyway!), and `Stack` extends `Vector` (exposing arbitrary `insertElementAt` breaking LIFO!). Replace `Vector` with `ArrayList` (or `CopyOnWriteArrayList` / `Collections.synchronizedList`) and replace `Stack` with **`ArrayDeque`** (or `LinkedBlockingDeque` concurrently).

### Q4. How does `ArrayDeque` work under the hood?
`ArrayDeque` is a resizable **circular ring buffer** backed by a power-of-two array (`Object[]`). Instead of modulo `%`, head and tail wrap around in 1 CPU cycle using bitwise masking: `(head - 1) & (elements.length - 1)`. Null elements are forbidden (used as empty sentinel).

### Q5. `Comparable` vs `Comparator`?
- `Comparable<T>` (`java.lang`): Natural ordering implemented **inside** the domain class via `int compareTo(T other)` (e.g., `String`, `Integer`).
- `Comparator<T>` (`java.util`): External, decoupled comparison strategy via `int compare(T o1, T o2)` — supports multiple sort orders (`Comparator.comparing(Emp::dept).thenComparingDouble(Emp::salary)`).
*(Trap: Never implement `compare(a, b)` as `return a - b;` for integers — integer overflow breaks transitivity when subtracting large negative/positive numbers! Always use `Integer.compare(a, b)`.)*

### Q6. Explain the `ArrayList.subList()` Memory Leak Trap.
`list.subList(0, 5)` returns a **live view** backed by the original huge `ArrayList` array, NOT an independent copy! Keeping that tiny 5-element sublist alive prevents GC from reclaiming a 10-million-element backing array. Furthermore, structural modification of the parent invalidates the sublist (`ConcurrentModificationException`). Copy via `new ArrayList<>(list.subList(...))` if detaching.

### Q7. How does `CopyOnWriteArrayList` work, and when should it be avoided?
Every mutating write (`add`, `set`, `remove`) acquires a `ReentrantLock`, **copies the entire backing array**, mutates the copy, and atomically swings a `volatile Object[]` pointer. Readers (`get`, `iterator`) traverse a **frozen snapshot lock-free** with zero `ConcurrentModificationException`! Ideal for **99% reads / rare writes** (listener lists, config rules); catastrophic for write-heavy lists ($O(N)$ copying).

### Q8. How does `PriorityQueue` work internally?
An unbounded binary **min-heap** stored in a flat `Object[]` array (parent of index `k` is `(k - 1) >>> 1`; children are `2k + 1` and `2k + 2`). `offer()` / `poll()` run in $O(\log N)$ (`siftUp` / `siftDown`); `peek()` is $O(1)$. Not thread-safe (use `PriorityBlockingQueue`) and rejects `null`. Notice `iterator()` does **NOT** yield elements in sorted order — only repeated `poll()` does!

### Q9. How does `HashSet` work internally?
`HashSet<E>` is literally a thin wrapper over **`HashMap<E, Object>`**, storing elements as map **keys** paired with a single shared dummy constant `private static final Object PRESENT = new Object();`! `LinkedHashSet` wraps `LinkedHashMap`, and `TreeSet` wraps `TreeMap`.

### Q10. What are `EnumSet` and `EnumMap`, and why are they so fast?
- `EnumSet`: Stored as a **`long` bitmask** (`RegularEnumSet` for $\le 64$ constants, `JumboEnumSet` `long[]` above 64) -> $O(1)$ bitwise set operations (`contains`, `add`, `union`) with near-zero memory!
- `EnumMap`: Backed by a plain **`Object[]` array indexed by `enum.ordinal()`** -> $O(1)$ direct array lookup with zero hashing overhead!

### Q11. What did Java 21 Sequenced Collections (`JEP 431`) add?
Unified interfaces `SequencedCollection`, `SequencedSet`, and `SequencedMap` adding standard `getFirst()`, `getLast()`, `addFirst()`, `addLast()`, `removeFirst()`, `removeLast()`, and `reversed()` across `List`, `Deque`, `LinkedHashSet`, `TreeSet`, `LinkedHashMap`, and `TreeMap`.

---

## Part B: Top 30 `HashMap` Deep-Dive Interview Questions

### Q12. What is the underlying data structure of `HashMap` in Java?
In Java 8+, `HashMap` uses an **Array of Buckets (`Node<K,V>[] table`)**, resolving collisions via **Separate Chaining** with **Singly Linked Lists**, dynamically treeifying into **Red-Black Trees (`TreeNode<K,V>`)** when a bin reaches `TREEIFY_THRESHOLD = 8` and table capacity $\ge 64$.

### Q13. What are the average and worst-case time complexities of `HashMap` operations?
- **Average-Case:** $O(1)$ for `get()`, `put()`, `remove()`, `containsKey()`.
- **Worst-Case in Java 8+:** $O(\log n)$ (Red-Black Tree bin).
- **Worst-Case in Java 7:** $O(n)$ (linked list only -> vulnerable to Hash-DoS attacks).

### Q14. What is the purpose of the perturbation function `(h = key.hashCode()) ^ (h >>> 16)`?
Because table capacity `n` is typically small (e.g., 16), `(n - 1) & hash` only uses the lowest few bits. XORing the top 16 bits down into the lower 16 bits spreads upper-bit entropy into bucket selection with a 2-instruction cost.

### Q15. Why is the bucket index calculated as `(n - 1) & hash` instead of `hash % n`?
1. Bitwise `&` takes **1 CPU cycle** vs `15–40` cycles for integer `%`.
2. Negative hashes (`-15 % 16 == -15`) fail with `%`, while `(n - 1) & hash` always yields a valid index in $[0, n - 1]$.

### Q16. Why MUST the capacity of `HashMap` always be a power of two?
When $n = 2^k$, $n - 1$ is all binary `1`s (`00001111`), ensuring uniform distribution across every bucket bit and guaranteeing `(n - 1) & hash == hash mod n` (for non-negative hashes). Non-power-of-two capacities leave bits permanently `0`, making $>60\%$ of buckets unreachable!

### Q17. If I need to store 1,000 items in a `HashMap`, what initial capacity should I specify?
**2,048** (or use Java 19+ `HashMap.newHashMap(1000)`!).  
$\lceil 1000 / 0.75 \rceil = 1334$, and rounding up to the next power of two gives **`2048`**, avoiding any resize/rehash!

### Q18. Why is the default Load Factor `0.75` and not `0.5` or `1.0`?
Balances Heap overhead vs collision chains. Under uniform hashing (Poisson distribution $\lambda \approx 0.5$), bucket depth reaching 8 occurs with probability $< 10^{-7}$ while maintaining high array density.

### Q19. What are the exact conditions for a bucket to be treeified?
**Both** must hold:
1. Bucket collision chain length reaches `TREEIFY_THRESHOLD = 8`.
2. Total `table.length >= MIN_TREEIFY_CAPACITY` (`64`). Otherwise, `HashMap` calls `resize()` to double the array instead of treeifying!

### Q20. Why is `UNTREEIFY_THRESHOLD` set to `6` instead of `8`?
**Hysteresis** — a gap between 6 and 8 prevents constant thrashing (repeated treeify/untreeify allocations) when insertions and deletions oscillate around depth 8.

### Q21. Why does `HashMap` use a Red-Black Tree instead of an AVL Tree?
AVL trees are more strictly balanced (faster lookup) but incur frequent rotations on writes. Red-Black trees bound worst-case depth to $2 \log_2 n$ with at most 2 rotations on insert / 3 on delete, optimizing mixed read-write throughput.

### Q22. How does `HashMap` handle `null` keys?
Permits **one `null` key**, always hashed to `0` (`hash(null) == 0`), stored at **bucket `0`**.

### Q23. What happens if `equals()` is overridden without `hashCode()` (or vice versa)?
- **`equals` only:** Logically equal objects get different default `identityHashCode()`s -> land in different buckets -> `map.get(new Key(...))` returns `null` and `put()` creates duplicates!
- **`hashCode` only:** Keys land in the same bucket, but `Object.equals()` falls back to `==` identity -> `map.get(new Key(...))` still misses!

### Q24. What happens if a key's fields mutate after insertion into `HashMap`?
**Silent Memory Leak:** The entry sits in Bucket $X$ (old hash), while future `get()`/`remove()` searches Bucket $Y$ (new hash) — returning `null` while the entry stays pinned in memory forever! Always use immutable keys (`String`, `record`, `UUID`).

### Q25. Why is `String` an ideal `HashMap` key?
Immutable, caches its `hashCode()`, and optimizes `equals()` via identity + length + byte-array fast paths.

### Q26. How does Java 8 resizing work compared to Java 7?
Because capacity doubles ($n \to 2n$), each node either stays at `oldIndex` if `(e.hash & oldCap) == 0` OR moves to `oldIndex + oldCap` if `!= 0` — zero hash recomputation, plus tail insertion preserves relative order!

### Q27. What was the Java 7 `HashMap` infinite loop bug, and did Java 8 fix thread-safety?
Java 7 used **head insertion** during concurrent `resize()`, creating a circular linked list (`A <-> B`) and 100% CPU infinite loops in `get()`. Java 8 switched to tail insertion (fixing the resize cycle), **but `HashMap` is STILL NOT thread-safe** (concurrent `put()` causes lost writes, wrong `size`, and corrupted `TreeNode` links).

### Q28. How does `ConcurrentHashMap` achieve thread safety in Java 8+?
Dropped Java 7 `Segment` locks! Uses **CAS** for empty bucket initialization/insertion, fine-grained **`synchronized(bucketHeadNode)`** only when a bin collides, and **`volatile` `val`/`next` reads** (lock-free `get()`).

### Q29. Why do `ConcurrentHashMap` and `Hashtable` forbid `null` keys and values?
Eliminates the `get(k) == null` race ambiguity ("key absent" vs "mapped to null" between `get()` and `containsKey()` under concurrent updates).

### Q30. Explain Fail-Fast (`modCount`) vs Fail-Safe / Weakly Consistent iterators.
- **Fail-Fast (`ArrayList`, `HashMap`):** Captures `expectedModCount = modCount`; any structural modification outside `iterator.remove()` throws `ConcurrentModificationException` immediately.
- **Weakly Consistent / Snapshot (`ConcurrentHashMap`, `CopyOnWriteArrayList`):** Never throws `ConcurrentModificationException`; iterates safely on a snapshot or concurrent table.

### Q31. Compare `LinkedHashMap`, `TreeMap`, `IdentityHashMap`, and `WeakHashMap`.
- `LinkedHashMap`: Doubly-linked bucket overlay preserving insertion order or access order (ideal for **$O(1)$ LRU Cache** via `removeEldestEntry`).
- `TreeMap`: Red-Black tree keeping keys sorted ($O(\log N)$) + `NavigableMap` (`floorKey`, `ceilingKey`).
- `IdentityHashMap`: Compares keys by reference equality (`==`) and `System.identityHashCode()` — used for object graph/proxy registries.
- `WeakHashMap`: Wraps keys in `WeakReference` + `ReferenceQueue` so GC automatically evicts entries once keys have no strong external references.

### Q32. How does a treeified bucket compare two colliding keys if their hashes are identical and they don't implement `Comparable`?
Falls back to `tieBreakOrder(a, b)` using class name comparison and `System.identityHashCode()`. Implementing `Comparable<K>` ensures fast deterministic tree ordering.

### Q33. What is the difference between `map.get(k) != null` and `map.containsKey(k)`?
In `HashMap` (which permits `null` values), `map.put("k", null)` causes `map.get("k")` to return `null` even though the key exists (`containsKey("k") == true`).
