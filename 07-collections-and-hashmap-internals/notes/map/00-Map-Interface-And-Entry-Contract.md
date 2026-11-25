# <Map Interface & Entry Contract — Deep Dive>
> Author: raysi

---

## 1. Architectural Overview & Hierarchy

The Java Collections Framework (JCF) fundamentally bifurcates into two distinct branch hierarchies:
1. `java.util.Collection<E>`: Linear or bag-oriented collections of discrete elements (`List`, `Set`, `Queue`, `Deque`).
2. `java.util.Map<K, V>`: Key-Value associative mappings (functions mapping a domain $K$ to a range $V$).

### Exact Package and Interface Signature
```java
package java.util;

public interface Map<K, V> {
    // Nested interface
    interface Entry<K, V> { ... }
    ...
}
```

### ASCII Inheritance & Structural Diagram
```
                          ┌───────────────────────┐
                          │   java.util.Map<K,V>  │
                          └───────────┬───────────┘
                                      │
         ┌────────────────────────────┼───────────────────────────┐
         │                            │                           │
         ▼                            ▼                           ▼
┌──────────────────┐       ┌────────────────────┐     ┌──────────────────────┐
│  AbstractMap<K,V>│       │  SortedMap<K,V>    │     │  ConcurrentMap<K,V>  │
└────────┬─────────┘       └──────────┬─────────┘     └──────────┬───────────┘
         │                            │                          │
    ┌────┴───────────────┐            ▼                          ▼
    │                    │ ┌────────────────────┐    ┌──────────────────────┐
    ▼                    ▼ │ NavigableMap<K,V>  │    │ConcurrentHashMap<K,V>│
┌──────────────┐ ┌──────────────┐ └────────┬─────────┘    └──────────────────────┘
│ HashMap<K,V> │ │TreeMap<K,V>  │◄─────────┘
└───────┬──────┘ └──────────────┘
        │
        ▼
┌───────────────────┐
│LinkedHashMap<K,V> │
└───────────────────┘
```

### Why Does `Map<K, V>` NOT Extend `java.util.Collection<E>`?
This is one of the classic JVM design questions asked in senior engineering interviews. There are four foundational reasons why `Map` does not inherit from `Collection`:

1. **Semantic Incompatibility (Dimensionality Mismatch)**:
   - A `Collection` is a collection of discrete elements (`E`).
   - A `Map` is a mapping of key-value pairs (`K -> V`). It models a mathematical relation or dictionary, not a simple container.
   - If `Map` extended `Collection<Map.Entry<K, V>>`, the method signatures would become semantically corrupted:
     - `Collection.add(E e)` would become `Map.add(Map.Entry<K, V> entry)`. This violates the key uniqueness invariant of a map unless `add` throws exceptions on duplicate keys, breaking Liskov Substitution Principle (LSP).
     - `Collection.contains(Object o)` would be ambiguous: does it search for keys, values, or entries?
     - `Collection.remove(Object o)` would require creating synthetic `Map.Entry` instances just to query or remove an entry by key.

2. **The View Architectural Pattern**:
   Instead of forcing `Map` to be a `Collection`, the JDK architects introduced **Collection Views**. A `Map` exposes its underlying data through three synchronized views:
   - `Set<K> keySet()`: A set projection over keys (keys are unique).
   - `Collection<V> values()`: A bag/collection projection over values (values may contain duplicates).
   - `Set<Map.Entry<K, V>> entrySet()`: A set projection over key-value pair tuples.
   These collections are **backed by the map**: mutating the set or collection directly mutates the underlying map (e.g., `map.keySet().remove("foo")` purges `"foo"` from the map), while mutating the map reflects immediately in the view iterator.

3. **Template Method via `AbstractMap<K, V>`**:
   `AbstractMap<K, V>` provides a skeletal implementation of `Map` to minimize developer effort. To implement an unmodifiable map, a programmer only needs to extend `AbstractMap` and implement `entrySet()`. To implement a modifiable map, one must also override `put(K key, V value)` (which throws `UnsupportedOperationException` by default in `AbstractMap`) and ensure `entrySet().iterator()` implements `remove()`.

4. **Immutable Modern Map Factories (Java 9+ & 10+)**:
   - `Map.of()`, `Map.ofEntries()`, `Map.copyOf()` instantiate internal implementations (`ImmutableCollections.Map1`, `ImmutableCollections.MapN`).
   - Characteristics: Null-hostile (throws `NullPointerException` on null key or value), duplicate-hostile at construction (throws `IllegalArgumentException`), memory-dense, and randomized iteration order across JVM invocations to prevent algorithm-dependent ordering bugs.

---

## 2. Internal Data Structure & Memory Layout (JDK Source Code Level)

### The `Map.Entry<K, V>` Interface Contract
`Map.Entry<K, V>` models a key-value pair within the map.
```java
public interface Entry<K, V> {
    K getKey();
    V getValue();
    V setValue(V value);
    boolean equals(Object o);
    int hashCode();

    // Java 8 static combinators
    public static <K extends Comparable<? super K>, V> Comparator<Map.Entry<K, V>> comparingByKey() {
        return (Comparator<Map.Entry<K, V>> & Serializable)
            (c1, c2) -> c1.getKey().compareTo(c2.getKey());
    }

    public static <K, V extends Comparable<? super V>> Comparator<Map.Entry<K, V>> comparingByValue() {
        return (Comparator<Map.Entry<K, V>> & Serializable)
            (c1, c2) -> c1.getValue().compareTo(c2.getValue());
    }
}
```

#### Strict `Map.Entry` HashCode & Equals Specification
The JDK specification mandates an exact formula for `Map.Entry.hashCode()`:
$$\text{hashCode}(e) = (\text{e.getKey}() == \text{null} \ ?\ 0 : \text{e.getKey}().\text{hashCode}()) \oplus (\text{e.getValue}() == \text{null} \ ?\ 0 : \text{e.getValue}().\text{hashCode}())$$

Two entries $e_1$ and $e_2$ are equal if and only if:
$$(e_1.\text{getKey}() == \text{null} \ ?\ e_2.\text{getKey}() == \text{null} : e_1.\text{getKey}().\text{equals}(e_2.\text{getKey}())) \land (e_1.\text{getValue}() == \text{null} \ ?\ e_2.\text{getValue}() == \text{null} : e_1.\text{getValue}().\text{equals}(e_2.\text{getValue}()))$$

### Memory Footprint Comparison Across JVM Layouts (64-bit JVM with Compressed OOPs)

```
        HashMap.Node<K,V>                       LinkedHashMap.Entry<K,V>
 ┌─────────────────────────────┐        ┌─────────────────────────────┐
 │ Mark Word (8 bytes)         │        │ Mark Word (8 bytes)         │
 ├─────────────────────────────┤        ├─────────────────────────────┤
 │ Klass Pointer (4 bytes)     │        │ Klass Pointer (4 bytes)     │
 ├─────────────────────────────┤        ├─────────────────────────────┤
 │ int hash (4 bytes)          │        │ int hash (4 bytes)          │
 ├─────────────────────────────┤        ├─────────────────────────────┤
 │ OOP key (4 bytes)           │        │ OOP key (4 bytes)           │
 ├─────────────────────────────┤        ├─────────────────────────────┤
 │ OOP value (4 bytes)         │        │ OOP value (4 bytes)         │
 ├─────────────────────────────┤        ├─────────────────────────────┤
 │ OOP next (4 bytes)          │        │ OOP next (4 bytes)          │
 └─────────────────────────────┘        ├─────────────────────────────┤
     Total: 32 bytes                    │ OOP before (4 bytes)        │
                                        ├─────────────────────────────┤
                                        │ OOP after (4 bytes)         │
                                        └─────────────────────────────┘
                                            Total: 40 bytes
```

### CPU Cache & Memory Overhead Analysis
- **Indirection & Pointer Chasing**: Storing 1,000,000 entries in a `HashMap` creates at least 1,000,000 `Node` instances on the heap plus an underlying array (`Node[] table`).
- **Memory Overhead**: Each `HashMap.Node` consumes 32 bytes (excluding key and value objects themselves). The reference array consumes 4 bytes per bucket. A `HashMap` holding 1M boxed integers (`Integer -> Integer`) incurs:
  $$\text{Table (capacity 2,097,152)} \times 4\text{B} = 8.38\text{ MB}$$
  $$1,000,000 \times 32\text{B (Node)} = 32\text{ MB}$$
  $$1,000,000 \times 16\text{B (Key Integer)} = 16\text{ MB}$$
  $$1,000,000 \times 16\text{B (Value Integer)} = 16\text{ MB}$$
  $$\textbf{Total} \approx \mathbf{72.38\text{ MB}}\text{ to store 8 MB of raw primitive data!}$$
- **CPU Cache Misses**: Linked nodes are scattered randomly across the JVM heap. Traversing a collision bucket chain involves jumping between non-contiguous memory addresses, causing L1/L2/L3 cache misses.

---

## 3. Step-by-Step Internal Working of Core Operations

### 1. View Backing and Bi-directional Mutation
When calling `map.keySet()`, `map.values()`, or `map.entrySet()`, no copy of keys or values is created. Instead, an instance of an anonymous or private inner class (e.g., `HashMap.KeySet`) is returned.

```
       ┌────────────────────────┐
       │     Map Instance       │
       │  [Key A] -> [Val 1]    │
       │  [Key B] -> [Val 2]    │
       └───────────┬────────────┘
                   │ Backs
       ┌───────────▼────────────┐
       │     keySet() View      │
       │     ("A", "B")         │
       └────────────────────────┘
```
- **Removal**: `keySet.remove(k)` navigates the map's hash table, removes the matching entry, decrements map size, and returns `true`.
- **Addition**: `keySet.add(k)` unconditionally throws `UnsupportedOperationException` because the view cannot know what value should be paired with the key.

### 2. Java 8+ Functional Default Methods Mechanics
JDK 8 introduced default methods on `Map<K, V>` to allow functional, atomic-like in-place operations:

#### `getOrDefault(Object key, V defaultValue)`
- Direct lookup. If key exists and `value != null`, returns value. If key is missing or explicitly mapped to `null`, returns `defaultValue`.

#### `putIfAbsent(K key, V value)`
```java
default V putIfAbsent(K key, V value) {
    V v = get(key);
    if (v == null) {
        v = put(key, value);
    }
    return v;
}
```
*Edge Case Warning*: If a key is explicitly mapped to `null`, `putIfAbsent` **overwrites** the `null` with `value`.

#### `computeIfAbsent(K key, Function<? super K, ? extends V> mappingFunction)`
- If `key` is not present OR maps to `null`, evaluates `mappingFunction(key)`.
- If mapping function returns a non-null result, inserts the mapping and returns the computed value.
- If mapping function returns `null`, no entry is created and returns `null`.
- *Performance Edge Case*: Never perform expensive blocking operations inside `mappingFunction` when used on concurrent structures like `ConcurrentHashMap`, as it locks the target bin.

#### `computeIfPresent(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction)`
- If `key` is present and maps to a non-null value, executes `remappingFunction(key, oldValue)`.
- If the remapping function returns `null`, the entry is **removed** from the map!
- If the remapping function throws an exception, the map remains unaltered.

#### `compute(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction)`
- Executes `remappingFunction(key, oldValue)` where `oldValue` is `null` if the key is not present.
- If the function returns `null`, an existing entry for `key` is removed (or remains absent).
- If the function returns non-null, creates or updates the entry.

#### `merge(K key, V value, BiFunction<? super V, ? super V, ? extends V> remappingFunction)`
- If `key` is absent or mapped to `null`, maps `key` to `value`.
- If `key` is present and non-null, computes `newValue = remappingFunction(oldValue, value)`.
- If `remappingFunction` returns `null`, removes the key.
- Ideal for counters and aggregation: `map.merge(word, 1, Integer::sum);`.

### 3. Immutable Map Factories (`Map.of`, `Map.ofEntries`)
- **Capacity**: Pre-sized, flat-table arrays with linear probing or optimized collision handling.
- **Null Safety**: Rejects nulls immediately (`Objects.requireNonNull`).
- **Iteration Order**: Java randomizes the iteration order on each JVM startup to prevent developers from relying on non-guaranteed ordering.

---

## 4. Complexity & Behavioral Matrix

| Operation | `HashMap` (Avg) | `HashMap` (Worst) | `TreeMap` (Red-Black) | `LinkedHashMap` | `ConcurrentHashMap` |
|:---|:---:|:---:|:---:|:---:|:---:|
| `get(K)` | $O(1)$ | $O(\log N)$ or $O(N)^*$ | $O(\log N)$ | $O(1)$ | $O(1)$ |
| `put(K, V)` | $O(1)$ | $O(\log N)$ or $O(N)^*$ | $O(\log N)$ | $O(1)$ | $O(1)$ |
| `remove(K)` | $O(1)$ | $O(\log N)$ or $O(N)^*$ | $O(\log N)$ | $O(1)$ | $O(1)$ |
| `containsKey(K)` | $O(1)$ | $O(\log N)$ or $O(N)^*$ | $O(\log N)$ | $O(1)$ | $O(1)$ |
| `containsValue(V)` | $O(N)$ | $O(N)$ | $O(N)$ | $O(N)$ | $O(N)$ |
| Iteration Over All | $O(C + N)$ | $O(C + N)$ | $O(N)$ | $O(N)$ | $O(C + N)$ |

\* *Note: $O(\log N)$ in HashMap occurs when bin treeifies; $O(N)$ if colliding keys do not implement `Comparable`.*

### Behavioral Matrix

| Implementation | Ordering Guarantee | Null Keys? | Null Values? | Thread-Safe? | Iterator Failure Mode |
|:---|:---|:---:|:---:|:---:|:---|
| `HashMap` | None | Yes (1 null key) | Yes | No | Fail-Fast (`ConcurrentModificationException`) |
| `LinkedHashMap` | Insertion or Access | Yes (1 null key) | Yes | No | Fail-Fast (`ConcurrentModificationException`) |
| `TreeMap` | Natural or Comparator | No (throws NPE) | Yes | No | Fail-Fast (`ConcurrentModificationException`) |
| `ConcurrentHashMap` | None | **No** (throws NPE) | **No** (throws NPE) | Yes (Lock-free/CAS) | Weakly Consistent (never throws CME) |
| `IdentityHashMap` | Reference equality (`==`) | Yes | Yes | No | Fail-Fast |
| `Map.of()` / `MapN` | Randomized / Unspecified | **No** (throws NPE) | **No** (throws NPE) | Thread-Safe (Immutable)| Fail-Fast / Unmodifiable |

---

## 5. Concurrency & Thread-Safety Mechanics

### ModCount and Fail-Fast Iteration
- Implementations like `HashMap` and `TreeMap` maintain a `protected transient int modCount` field tracking structural modifications (additions, deletions, rehashes).
- When obtaining an iterator over `keySet()`, `values()`, or `entrySet()`, the iterator snapshots `expectedModCount = modCount`.
- On every call to `it.next()`, the iterator checks:
  ```java
  if (modCount != expectedModCount)
      throw new ConcurrentModificationException();
  ```
- *Key Gotcha*: In `LinkedHashMap` configured with `accessOrder = true`, calling `map.get(key)` is a **structural modification** because it shifts the accessed node to the tail of the doubly-linked list. Therefore, calling `get()` while iterating over an access-ordered `LinkedHashMap` triggers `ConcurrentModificationException`!

### Concurrent Collections & Views
- `Collections.synchronizedMap(map)`: Wraps the map with a synchronization mutex object (`final Object mutex`). Every read and write acquires this single object monitor, resulting in extreme lock contention under high multi-threaded read/write load.
- `ConcurrentMap<K, V>`: Introduces non-blocking atomic operations (`putIfAbsent`, conditional `remove`, conditional `replace`).
- `ConcurrentHashMap<K, V>`: Employs bin-level synchronized locks and CAS (Compare-And-Swap) operations on table array elements, entirely discarding table-wide locking.

---

## 6. Production-Grade Java Code Verification

See the runnable demonstration file:
- [`MapModernJava8MethodsDemo.java`](../../codes/collections/map/MapModernJava8MethodsDemo.java)

Key mechanics demonstrated in the code:
1. Backed collection views and mutating the underlying map via `keySet().remove()`.
2. Functional idioms (`merge` for atomic word counting, `computeIfAbsent` for caching).
3. Null-eviction side-effects: Returning `null` from `compute` or `merge` removes the entry.
4. Behavioral contrast between mutable `HashMap` and Java 9+ `Map.of()` immutable structures.

---

## 7. Top 7 High-Signal Interview Questions & Deep Answers

### Q1: Why does `Map<K, V>` not inherit from `Collection<E>`?
**Answer**:
A `Collection` models a container of discrete objects $E$ with operations like `add(E)` and `contains(Object)`. A `Map` models an associative mapping function $K \to V$. If `Map` extended `Collection<Entry<K, V>>`:
1. `add(Entry)` would break the fundamental Map invariant of key uniqueness unless it overwrote or threw exceptions, which breaks the Liskov Substitution Principle.
2. Signatures would be ambiguous: `contains(Object)` would be unclear whether it checks key, value, or pair.
3. Instead, the JCF uses the **View Pattern**: `keySet()`, `values()`, and `entrySet()` return backed collections that expose map data in standard collection formats without polluting the map interface.

### Q2: What happens under the hood when you mutate a `Map` collection view (e.g., `map.keySet().remove("key")` vs `map.keySet().add("key")`)?
**Answer**:
`keySet()` returns an inner class implementing `Set<K>`. Its methods delegate directly to the outer `Map` instance:
- `keySet.remove(o)` calls `map.remove(o)`. The entry is unlinked from the bucket, `size` decrements, `modCount` increments, and `true` is returned.
- `keySet.add(e)` unconditionally throws `UnsupportedOperationException` because a key cannot be added without an associated value; the view has no contract for default value synthesis.
- Clearing the view (`keySet.clear()`) calls `map.clear()`, wiping the entire map.

### Q3: How do `computeIfAbsent()` and `merge()` handle `null` values, and what is the removal trap?
**Answer**:
- `computeIfAbsent(key, fn)`: Executes `fn` only if the key is missing or mapped to `null`. If `fn` returns `null`, no entry is created. If `fn` returns non-null, it stores the result.
- `merge(key, value, remappingFn)`:
  - If the key is absent or currently mapped to `null`, it immediately associates the key with `value` (the remapping function is NOT called).
  - If the key is already present with a non-null value, it invokes `remappingFn(oldVal, newVal)`.
  - **The Removal Trap**: If `remappingFn` returns `null`, the entry is **completely removed** from the map! This is an intentional feature for decrement-and-prune patterns, but causes silent data loss if an engineer returns `null` accidentally.

### Q4: Explain the exact `equals` and `hashCode` contract required for `Map.Entry`. Why is this formula required?
**Answer**:
The specification requires:
$$\text{hashCode}(e) = (k == \text{null} \ ?\ 0 : k.\text{hashCode}()) \oplus (v == \text{null} \ ?\ 0 : v.\text{hashCode}())$$
This ensures that any two `Map.Entry` implementations (e.g., `AbstractMap.SimpleEntry`, `HashMap.Node`, `TreeMap.Entry`) evaluate to the exact same hash code and are equal under `.equals()`, regardless of the underlying map implementation. The XOR ($\oplus$) operator provides a symmetric, reversible combination of the key and value hashes.

### Q5: What makes Java 9+ `Map.of()` fundamentally different from `Collections.unmodifiableMap(new HashMap<>())`?
**Answer**:
1. **Memory Footprint**: `Map.of()` utilizes optimized, specialized flat representations (`Map1`, `Map2`, ..., `MapN`). For small sizes (0 to 2 entries), there is no array allocation or node pointer overhead. `Collections.unmodifiableMap` is a wrapper containing a reference to a fully allocated `HashMap` with its bucket table, `Node` objects, and overhead.
2. **True Immutability vs Read-Only View**: `Collections.unmodifiableMap` is merely a view. If the underlying `HashMap` reference is modified by another thread or method, the unmodifiable view reflects those mutations! `Map.of()` is genuinely immutable and cannot be mutated by any external reference.
3. **Null Rejection**: `Map.of()` rejects `null` keys and `null` values with `NullPointerException`. `HashMap` supports one `null` key and infinite `null` values.
4. **Iteration Order Randomization**: `Map.of()` randomizes iteration order across JVM runs to discourage relying on insertion order.

### Q6: What is the difference between `putIfAbsent(k, v)` and `computeIfAbsent(k, v -> compute())` in terms of evaluation semantics and performance?
**Answer**:
1. **Eager vs Lazy Evaluation**: In `putIfAbsent(k, computeExpensiveValue())`, the method argument `computeExpensiveValue()` is evaluated **eagerly**, every single time, before `putIfAbsent` is invoked, regardless of whether the key is already present! In `computeIfAbsent(k, key -> computeExpensiveValue())`, the lambda is executed **lazily** only if the key is missing or mapped to `null`.
2. **ConcurrentHashMap Locking**: In `ConcurrentHashMap`, `computeIfAbsent` executes atomically under the lock of the bucket bin. If the mapping function performs expensive computations or nested map calls, it can lead to thread starvation or deadlocks.

### Q7: What is the architectural hierarchy and purpose of `SortedMap` and `NavigableMap`?
**Answer**:
`SortedMap<K, V>` guarantees that keys are ordered either by their natural order (`Comparable`) or by an explicit `Comparator`.
`NavigableMap<K, V>` extends `SortedMap<K, V>` by adding closest-match navigation methods:
- `lowerEntry(key)` / `lowerKey(key)`: Strictly less than ($< k$).
- `floorEntry(key)` / `floorKey(key)`: Less than or equal to ($\le k$).
- `ceilingEntry(key)` / `ceilingKey(key)`: Greater than or equal to ($\ge k$).
- `higherEntry(key)` / `higherKey(key)`: Strictly greater than ($> k$).
- `pollFirstEntry()` / `pollLastEntry()`: Retrieves and removes minimum or maximum entry.
- `descendingMap()`: Returns a reverse-order view without copying data.
The primary implementation in standard library is `java.util.TreeMap` (Red-Black tree) and `java.util.concurrent.ConcurrentSkipListMap` (concurrent lock-free skip list).
