# 06. Top 30 High-Frequency Interview Questions & Answers

---

### Q1: What is the underlying data structure of `HashMap` in Java?
**Answer**:
In Java 8+, `HashMap` is based on an **Array of Buckets (`Node<K,V>[] table`)**, where collisions are resolved using **Separate Chaining** with **Singly Linked Lists**, which are dynamically transformed into **Red-Black Trees (`TreeNode<K,V>`)** when collision chains exceed `TREEIFY_THRESHOLD = 8` and table capacity is $\ge 64$. In Java 7, it relied solely on an array of singly linked lists (`Entry<K,V>[]`).

---

### Q2: What are the average and worst-case time complexities of `HashMap` operations?
**Answer**:
- **Average-Case**: $O(1)$ for `get()`, `put()`, `remove()`, and `containsKey()`, assuming a uniform hash distribution.
- **Worst-Case in Java 8+**: $O(\log n)$ when all keys collide into the same bucket, because the bucket treeifies into a Red-Black Tree.
- **Worst-Case in Java 7**: $O(n)$ when all keys collide into a singly linked list.

---

### Q3: What is the purpose of the perturbation function `(h = key.hashCode()) ^ (h >>> 16)`?
**Answer**:
`key.hashCode()` produces a 32-bit integer. Because `HashMap` tables typically have small capacities (e.g., 16, 32, 64), the bucket index calculation `(n - 1) & hash` only considers the lowest few bits (e.g., lowest 4 bits for $n=16$). 

If multiple keys have identical low 16 bits but completely different high 16 bits, they would all collide. Shifting the high 16 bits down (`>>> 16`) and XORing them (`^`) blends the entropy from the top 16 bits into the lower 16 bits, drastically reducing collisions with minimal CPU cost.

---

### Q4: Why is the bucket index calculated as `(n - 1) & hash` instead of `hash % n`?
**Answer**:
Two key reasons:
1. **Performance**: Bitwise AND (`&`) takes **1 CPU clock cycle**, whereas the integer modulo/division operator (`%` / `idiv`) requires **15 to 40 CPU clock cycles**.
2. **Negative Hash Codes**: In Java, integer hashes can be negative. `(-15) % 16 = -15` (invalid array index), whereas `(16 - 1) & (-15) = 1` (always a non-negative, valid index within $[0, n-1]$).

---

### Q5: Why MUST the capacity of `HashMap` always be a power of two?
**Answer**:
If $n$ is a power of two ($2^k$), then $n - 1$ in binary is guaranteed to be a solid sequence of $1$s (`...0001111_2`). This ensures that every bit position contributes to indexing:
1. It guarantees that $\text{hash} \pmod n \equiv \text{hash} \ \& \ (n - 1)$.
2. If $n$ were not a power of two (e.g., $10$, so $n - 1 = 9 = 1001_2$), bit 1 and bit 2 would always be $0$. Indices like 2, 3, 4, 5, 6, 7 could never be reached, wasting $>60\%$ of table buckets and causing massive collisions in the remaining buckets.

---

### Q6: If I need to store 1,000 items in a `HashMap`, what initial capacity should I specify?
**Answer**:
You should specify **2,048**.

**Formula**:
$$\text{Required Capacity} = \left\lceil \frac{\text{Expected Elements}}{\text{Load Factor}} \right\rceil = \left\lceil \frac{1000}{0.75} \right\rceil \approx 1334$$

Rounding $1334$ up to the next highest power of two yields **$2048$**. 
Setting the initial capacity to $2048$ ensures the map will hold all 1,000 items without triggering a single expensive resizing or rehashing operation.

---

### Q7: Why is the default Load Factor 0.75 and not 0.5 or 1.0?
**Answer**:
The load factor represents an optimal **space-time tradeoff**:
- A lower factor (e.g., $0.5$) wastes excessive heap memory and triggers frequent resizing.
- A higher factor (e.g., $1.0$ or $1.25$) conserves memory but increases collisions, lengthening lookup chains.
- Under uniform random hashing, bucket distribution follows a **Poisson distribution** ($\lambda \approx 0.5$). At load factor $0.75$, the probability of any bucket chain reaching depth 8 is $< 10^{-7}$ ($0.000006\%$), making collisions negligible while maintaining $>75\%$ array utilization.

---

### Q8: What are the exact conditions for a bucket to be treeified into a Red-Black tree?
**Answer**:
Two conditions must **both** be met:
1. The number of colliding elements in that bucket reaches `TREEIFY_THRESHOLD = 8`.
2. The total capacity of the HashMap table is at least `MIN_TREEIFY_CAPACITY = 64`.

If a bucket reaches 8 elements but the table capacity is less than 64, HashMap calls `resize()` to double the array instead of treeifying.

---

### Q9: Why is `UNTREEIFY_THRESHOLD` set to 6 instead of 8?
**Answer**:
To introduce **hysteresis** and prevent **thrashing**. If both thresholds were 8, an application repeatedly adding and removing a single element near the boundary would constantly toggle the bucket between a Linked List and a Red-Black Tree, causing massive CPU and memory allocation overhead. A buffer gap of 2 elements (between 6 and 8) stabilizes bucket structure.

---

### Q10: Why does `HashMap` use a Red-Black Tree instead of an AVL Tree for treeification?
**Answer**:
- AVL Trees are strictly balanced ($|height(L) - height(R)| \le 1$), which provides slightly faster lookups, but makes insertions and deletions slow due to frequent recursive tree rotations.
- Red-Black Trees are loosely balanced (longest path is at most twice the shortest path), requiring at most **2 rotations on insertion** and **3 on deletion**.
- Since a `HashMap` experiences continuous read-write operations, Red-Black Trees offer significantly superior overall write throughput.

---

### Q11: How does `HashMap` handle `null` keys?
**Answer**:
`HashMap` permits exactly **one `null` key**. It is hardcoded to hash to `0`:
```java
return (key == null) ? 0 : ...
```
Because `hash = 0`, the bucket index is `(n - 1) & 0 = 0`. Thus, a `null` key is always stored in **bucket 0**.

---

### Q12: What happens if `equals()` is overridden but `hashCode()` is NOT?
**Answer**:
It violates the `Object` contract:
1. Two logically equal objects (`o1.equals(o2) == true`) will return different hash codes based on their JVM memory addresses.
2. They will map to different buckets.
3. If you insert an entry using `o1`, calling `map.get(o2)` will compute a different bucket and return `null`!
4. Furthermore, calling `map.put(o2, ...)` will insert duplicate logical keys into the map.

---

### Q13: What happens if `hashCode()` is overridden but `equals()` is NOT?
**Answer**:
1. Two distinct objects with identical field values will return identical hash codes and map to the exact same bucket.
2. However, because `equals()` was not overridden, it inherits `Object.equals()`, which checks reference identity (`this == obj`).
3. Calling `map.get(new Key(...))` will find the bucket, but will fail the identity check and return `null`.

---

### Q14: What happens if a key's fields are mutated after being inserted into a `HashMap`?
**Answer**:
This causes a catastrophic **silent memory leak**:
1. When inserted, the key is placed in Bucket $X$ based on its original hash.
2. When mutated, its `hashCode()` changes.
3. Subsequent calls to `map.get(key)` or `map.remove(key)` calculate a new bucket index $Y$, where the key is not found (returns `null`).
4. The entry remains trapped in Bucket $X$ forever, unreachable by key and uncollectible by Garbage Collection.

---

### Q15: Why is `String` the most commonly used and recommended key in `HashMap`?
**Answer**:
1. **Immutability**: `String` is immutable; its state and hash code can never change after creation.
2. **Hash Caching**: `String` contains a private `int hash` field. Its hash code is computed lazily once and cached. All subsequent calls to `hashCode()` return immediately in $O(1)$ without re-evaluating characters.
3. **Optimized `equals()`**: Compares reference identity first (`this == obj`), followed by string lengths, making lookups extremely fast.

---

### Q16: How does the resizing optimization work in Java 8 compared to Java 7?
**Answer**:
In Java 7, resizing required recalculating bucket indices for every node (`indexFor()`) and inserting them at the head of the new bucket, reversing chain order.

In Java 8, capacity doubles by a factor of 2. HashMap uses the **High-Bit Test**:
- It evaluates `(e.hash & oldCap) == 0`.
- If `0`: The element stays at its **original index** (`oldIndex`).
- If `!= 0`: The element moves to `oldIndex + oldCap`.
- Nodes are split into two chains (`loHead` and `hiHead`) using tail insertion, maintaining original relative ordering with **zero re-computation of hashes**.

---

### Q17: What was the Java 7 HashMap race condition infinite loop bug?
**Answer**:
In Java 7, resizing used **head insertion** in `transfer()`. When two threads resized concurrently:
1. Thread 1 paused after reading `e = A` and `next = B`.
2. Thread 2 finished the entire resize, reversing the chain to `B -> A`.
3. When Thread 1 resumed, `B.next` was already pointing to `A`. Thread 1 set `A.next = B` and `B.next = A`, creating a **circular linked list** (`A <-> B`).
4. Any subsequent `get()` request on that bucket entered an infinite loop, driving CPU usage to 100%.

---

### Q18: Does Java 8 fix all multithreading issues in `HashMap`?
**Answer**:
**No.** While Java 8 eliminates the circular linked list during resizing by using tail insertion:
1. **Silent Data Loss**: Simultaneous `put()` calls on the same bucket will overwrite and lose entries.
2. **Corrupted Size**: The `size` field is not atomic, leading to incorrect counts.
3. **Tree Pointer Corruption**: Concurrent inserts or deletes in a treeified bucket can corrupt Red-Black tree links, causing infinite loops during tree traversal.

---

### Q19: How does `ConcurrentHashMap` achieve thread safety in Java 8+?
**Answer**:
1. **Empty Bucket Insertion via CAS**: When a bucket is empty, a node is inserted using atomic Compare-And-Swap (`sun.misc.Unsafe` / `VarHandle`) with **zero lock acquisition**.
2. **Bucket-Level Synchronized**: When a collision occurs, only the **first node** of that specific bucket is locked (`synchronized(f)`). All other buckets remain open for concurrent writes.
3. **Lock-Free Reads**: Reads (`get()`) require **no locks** because the node's `val` and `next` references are declared `volatile`, ensuring visibility across threads.

---

### Q20: Why do `ConcurrentHashMap` and `Hashtable` disallow `null` keys and `null` values?
**Answer**:
To eliminate ambiguity in concurrent environments. 
In a single-threaded map, if `map.get(key) == null`, you can call `map.containsKey(key)` to determine whether the key is absent or mapped to `null`. 
In a concurrent environment, another thread could insert or remove the key between your `get()` and `containsKey()` calls, making it impossible to know the true state. Disallowing `null` ensures that a `null` return strictly means "key not present".

---

### Q21: What is a Fail-Fast iterator and how does it detect concurrent modifications?
**Answer**:
A fail-fast iterator immediately throws `ConcurrentModificationException` if the collection is structurally modified during iteration:
1. The map maintains an internal `transient int modCount` incremented on every structural change (`put`, `remove`, `resize`).
2. When an iterator is created, it captures `expectedModCount = modCount`.
3. On every call to `next()` or `remove()`, it verifies `modCount == expectedModCount`. If mismatched, it throws `ConcurrentModificationException`.

---

### Q22: What is the difference between Fail-Fast and Fail-Safe (Weakly Consistent) iterators?
**Answer**:
- **Fail-Fast** (`HashMap`, `ArrayList`): Operates directly on the backing data structure. Immediately throws `ConcurrentModificationException` on concurrent modification.
- **Fail-Safe / Weakly Consistent** (`ConcurrentHashMap`, `CopyOnWriteArrayList`): Operates on an internal snapshot or traverses the live structure safely without throwing `ConcurrentModificationException`. It guarantees to reflect the state at creation and may or may not reflect subsequent modifications.

---

### Q23: What is the difference between `HashMap` and `LinkedHashMap`?
**Answer**:
`LinkedHashMap` extends `HashMap` and adds a **doubly linked list** connecting all entries:
- `HashMap` offers no order guarantees.
- `LinkedHashMap` maintains either **Insertion-Order** (default) or **Access-Order** (moves touched nodes to the tail on `get`/`put`).
- `LinkedHashMap` is commonly used to implement LRU caches via `removeEldestEntry()`.

---

### Q24: What is the difference between `HashMap` and `TreeMap`?
**Answer**:
- `HashMap` is hash-table-based, unordered, provides $O(1)$ operations, and permits a null key.
- `TreeMap` is Red-Black-Tree-based, keeps keys sorted by natural order (`Comparable`) or a custom `Comparator`, provides guaranteed $O(\log n)$ operations, implements `NavigableMap`, and disallows null keys with natural ordering.

---

### Q25: What is `IdentityHashMap` and when would you use it?
**Answer**:
`IdentityHashMap` intentionally violates the general `Map` contract:
- Compares keys by **reference identity (`k1 == k2`)** rather than `equals()`.
- Uses `System.identityHashCode()` instead of `hashCode()`.
- Useful for graph serialization, deep copying, or proxy object registries where two distinct objects with identical content must be treated as separate keys.

---

### Q26: What is `WeakHashMap` and how does it prevent memory leaks?
**Answer**:
`WeakHashMap` stores its keys inside `java.lang.ref.WeakReference`. If a key has no other strong references outside the map, the Garbage Collector reclaims it during the next GC cycle. The map uses a `ReferenceQueue` to purge the reclaimed entries, preventing memory leaks in caches.

---

### Q27: How does `HashMap` compare keys when searching or inserting into a treeified bucket?
**Answer**:
1. First, compares hashes: `hashA < hashB` or `hashA > hashB`.
2. If hashes are equal and keys implement `Comparable<K>`, it calls `keyA.compareTo(keyB)`.
3. If keys do not implement `Comparable` or `compareTo() == 0` for different objects, it invokes the fallback `tieBreakOrder()` using `System.identityHashCode()`.

---

### Q28: Can we make `HashMap` synchronized without replacing it with `ConcurrentHashMap`?
**Answer**:
Yes, using `Collections.synchronizedMap(map)`:
```java
Map<String, String> syncMap = Collections.synchronizedMap(new HashMap<>());
```
This wraps the map with synchronized methods locking on a shared `mutex`. However, iterating over `syncMap` still requires manual user synchronization on the map instance.

---

### Q29: What happens if an object's `hashCode()` method always returns a constant (e.g., `return 1;`)?
**Answer**:
1. Every inserted key hashes to the same value, routing all entries into **Bucket 1**.
2. Up to 8 elements, the bucket behaves as a singly linked list ($O(n)$ search).
3. Once size reaches 8 and table capacity reaches 64, the bucket treeifies into a Red-Black Tree ($O(\log n)$ search).
4. The map degrades from an $O(1)$ hash table into an $O(\log n)$ binary tree, consuming double the memory per node.

---

### Q30: What is the difference between `map.get(key) != null` and `map.containsKey(key)`?
**Answer**:
If a key is explicitly mapped to `null` (`map.put("key", null)`):
- `map.get("key")` returns `null`.
- `map.containsKey("key")` returns `true`.
- For an absent key, `map.get("absent")` returns `null`, and `map.containsKey("absent")` returns `false`.
`containsKey()` is necessary to distinguish between a key that doesn't exist and one that exists with a `null` value.
