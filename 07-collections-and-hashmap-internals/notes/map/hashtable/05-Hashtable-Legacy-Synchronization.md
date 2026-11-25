# Hashtable — Legacy Synchronization & Architectural Evolution
> Author: raysi

---

## 1. Architectural Overview & Hierarchy

`java.util.Hashtable<K,V>` is one of Java's original legacy collection classes, introduced in JDK 1.0—prior to the introduction of the modern Java Collections Framework in JDK 1.2. In JDK 1.2, it was retrofitted to implement the `Map<K,V>` interface, but it retains its obsolete base class and monolithic synchronization model for backwards compatibility.

### Exact Class Signature

```java
package java.util;

public class Hashtable<K,V>
    extends Dictionary<K,V>
    implements Map<K,V>, Cloneable, java.io.Serializable
```

### ASCII Inheritance & Hierarchy Diagram

```
       ┌────────────────────────┐         ┌────────────────────────┐
       │ java.util.Dictionary   │ (JDK 1.0│     java.util.Map      │ (JDK 1.2)
       │ (Obsolete Abstract Cls)│  Legacy)│   (Modern Interface)   │
       └───────────┬────────────┘         └───────────┬────────────┘
                   │                                  │
                   └─────────────────┬────────────────┘
                                     │ extends & implements
                                     ▼
                      ┌─────────────────────────────┐
                      │    java.util.Hashtable<K,V> │
                      └─────────────────────────────┘
```

### Why Does This Class Exist? What Problem Did It Solve?

In the early design of Java 1.0 (1995/1996), multi-threaded applets and network applications required thread-safe dictionary structures. At that time:
1. `Hashtable` was designed to provide thread-safe key-value mappings by declaring **every single public accessor and mutator method as `synchronized`**.
2. The concept of separating concurrent concerns from single-threaded performance had not yet matured in the language.
3. In modern Java, `Hashtable` is considered an **architectural anti-pattern**:
   - For single-threaded workloads, the synchronization overhead imposes unnecessary CPU monitor acquisition penalties (even with biased locking / thin locks). Use `java.util.HashMap`.
   - For multi-threaded workloads, its coarse table-level lock creates an intolerable contention bottleneck. Use `java.util.concurrent.ConcurrentHashMap`.

---

## 2. Internal Data Structure & Memory Layout (JDK Source Code Level)

Unlike modern `HashMap` (which transitions buckets into Red-Black trees when collision thresholds exceed 8), `Hashtable` remains a strict array of singly linked lists (`Entry<K,V>[]`) throughout its entire lifecycle.

### Exact Internal Fields from JDK Source Code

```java
// The table data, an array of singly linked list buckets
private transient Entry<?,?>[] table;

// The total number of key-value entries in the hash table
private transient int count;

// The table is rehashed when its size exceeds this threshold (capacity * loadFactor)
private int threshold;

// The load factor for the hashtable (default: 0.75f)
private float loadFactor;

// The number of times this Hashtable has been structurally modified
private transient int modCount = 0;
```

### The Internal `Entry<K,V>` Node

```java
private static class Entry<K,V> implements Map.Entry<K,V> {
    final int hash;
    final K key;
    V value;
    Entry<K,V> next;

    protected Entry(int hash, K key, V value, Entry<K,V> next) {
        this.hash = hash;
        this.key =  key;
        this.value = value;
        this.next = next;
    }
    // getters, setters, equals, hashCode omitted
}
```

### ASCII Memory Layout Diagram

```
 Hasthable Instance Object Header (Mark Word: 8B, Klass Word: 4B)
 ┌────────────────────────────────────────────────────────┐
 │ table: [Ref to Entry[]] (4B)                           │
 │ count: 5                (4B)                           │
 │ threshold: 8            (4B)                           │
 │ loadFactor: 0.75f       (4B)                           │
 │ modCount: 5             (4B)                           │
 └───────────────────────────┬────────────────────────────┘
                             │
                             ▼
  table Array (Capacity = 11, default)
 ┌────┬────┬────┬────┬────┬────┬────┬────┬────┬────┬────┐
 │ 0  │ 1  │ 2  │ 3  │ 4  │ 5  │ 6  │ 7  │ 8  │ 9  │ 10 │
 └─┬──┴─┬──┴────┴────┴────┴─┬──┴────┴────┴────┴────┴────┘
   │    │                   │
   │    │                   ▼
   │    │            ┌───────────────────────────┐
   │    │            │ Entry (0x3000)            │
   │    │            │ hash: 1492                │
   │    │            │ key: "Port" -> val: "8080"│
   │    │            │ next: null                │
   │    │            └───────────────────────────┘
   │    ▼
   │  ┌───────────────────────────┐
   │  │ Entry (0x2000)            │
   │  │ hash: 8812                │
   │  │ key: "Host" -> val: "App" │
   │  │ next: null                │
   │  └───────────────────────────┘
   ▼
 ┌───────────────────────────┐
 │ Entry (0x1000)            │
 │ hash: 1100                │
 │ key: "Env" -> val: "Prod" │
 │ next: [Ref 0x1050]        │
 └─────────────┬─────────────┘
               │ Collision (Singly linked chain)
               ▼
 ┌───────────────────────────┐
 │ Entry (0x1050)            │
 │ hash: 1111                │
 │ key: "Region" -> "US-East"│
 │ next: null                │
 └───────────────────────────┘
```

### CPU Cache & Memory Overhead Analysis

#### Object Layout (64-bit JVM, Compressed OOPs)
- **`Hashtable.Entry` Instance:**
  - Header: 12 bytes (8-byte Mark Word + 4-byte Klass Word)
  - Fields: `hash` (int, 4 bytes), `key` (ref, 4 bytes), `value` (ref, 4 bytes), `next` (ref, 4 bytes) = 16 bytes
  - Total = 12 + 16 = 28 bytes $\rightarrow$ padded to **32 bytes per Entry node**.
- **The Modulo Operator Hardware Cost:**
  - `HashMap` enforces power-of-two table capacities ($2^k$), allowing bucket indexing via bitwise AND: `index = (n - 1) & hash`. In assembly, bitwise `AND` executes in **1 CPU clock cycle**.
  - `Hashtable` default capacity is **11** (not a power of two). It calculates index via:
    ```java
    int index = (hash & 0x7FFFFFFF) % tab.length;
    ```
    The integer division/modulo operator compiles to the x86 `idiv` instruction, which takes **10 to 25 CPU clock cycles**. Every lookup and insertion in `Hashtable` incurs this mathematical CPU penalty.

---

## 3. Step-by-Step Internal Working of Core Operations

### 3.1 Instantiation & Default Capacity

```java
public Hashtable() {
    this(11, 0.75f);
}

public Hashtable(int initialCapacity, float loadFactor) {
    if (initialCapacity < 0)
        throw new IllegalArgumentException("Illegal Capacity: " + initialCapacity);
    if (loadFactor <= 0 || Float.isNaN(loadFactor))
        throw new IllegalArgumentException("Illegal Load: " + loadFactor);

    if (initialCapacity == 0)
        initialCapacity = 1;
    this.loadFactor = loadFactor;
    table = new Entry<?,?>[initialCapacity]; // Eager allocation!
    threshold = (int)Math.min(initialCapacity * loadFactor, MAX_ARRAY_SIZE + 1);
}
```
*Key Architectural Observation:* `Hashtable` allocates its underlying array **eagerly** at construction time (`table = new Entry<?,?>[11]`). In contrast, modern `HashMap` allocates lazily on the first invocation of `put()`.

### 3.2 Insertion (`put(K key, V value)`)

```java
public synchronized V put(K key, V value) {
    // 1. Explicit Null Value Rejection
    if (value == null) {
        throw new NullPointerException();
    }

    // 2. Implicit Null Key Rejection
    // If key is null, key.hashCode() throws NullPointerException immediately
    Entry<?,?> tab[] = table;
    int hash = key.hashCode();
    int index = (hash & 0x7FFFFFFF) % tab.length;

    // 3. Search existing bucket chain for existing key
    @SuppressWarnings("unchecked")
    Entry<K,V> entry = (Entry<K,V>)tab[index];
    for (; entry != null ; entry = entry.next) {
        if ((entry.hash == hash) && entry.key.equals(key)) {
            V old = entry.value;
            entry.value = value;
            return old;
        }
    }

    // 4. Add new Entry (rehash if threshold reached)
    addEntry(hash, key, value, index);
    return null;
}
```

#### Why `hash & 0x7FFFFFFF`?
In Java, `hashCode()` returns a signed 32-bit `int`. A negative integer modulo table length yields a **negative index** in Java:
```java
-15 % 11 == -4 // Array index out of bounds!
```
The bitmask `0x7FFFFFFF` (binary `01111111 11111111 11111111 11111111`) clears the sign bit (most significant bit), guaranteeing that the dividend is always non-negative $[0, 2^{31}-1]$.

#### The `addEntry()` Helper:
```java
private void addEntry(int hash, K key, V value, int index) {
    Entry<?,?> tab[] = table;
    if (count >= threshold) {
        // Rehash the table if the threshold is exceeded
        rehash();

        tab = table;
        hash = key.hashCode();
        index = (hash & 0x7FFFFFFF) % tab.length;
    }

    // Creates new Entry at head of chain (Bucket Head insertion)
    @SuppressWarnings("unchecked")
    Entry<K,V> e = (Entry<K,V>) tab[index];
    tab[index] = new Entry<>(hash, key, value, e);
    count++;
    modCount++;
}
```

### 3.3 Rehash Mechanics (`rehash()`)

```java
protected void rehash() {
    int oldCapacity = table.length;
    Entry<?,?>[] oldMap = table;

    // Formula: (oldCapacity << 1) + 1  ==>  2 * oldCapacity + 1
    int newCapacity = (oldCapacity << 1) + 1;
    if (newCapacity - MAX_ARRAY_SIZE > 0) {
        if (oldCapacity == MAX_ARRAY_SIZE)
            return;
        newCapacity = MAX_ARRAY_SIZE;
    }
    Entry<?,?>[] newMap = new Entry<?,?>[newCapacity];

    modCount++;
    threshold = (int)Math.min(newCapacity * loadFactor, MAX_ARRAY_SIZE + 1);
    table = newMap;

    // Full table migration: Recomputes modulo for every single entry!
    for (int i = oldCapacity ; i-- > 0 ;) {
        for (Entry<K,V> old = (Entry<K,V>)oldMap[i] ; old != null ; ) {
            Entry<K,V> e = old;
            old = old.next;

            int index = (e.hash & 0x7FFFFFFF) % newCapacity;
            e.next = (Entry<K,V>)newMap[index];
            newMap[index] = e;
        }
    }
}
```

#### Why Does `rehash()` Use `2 * oldCapacity + 1`?
If `oldCapacity = 11`:
$$\text{newCapacity} = (11 \times 2) + 1 = 23$$
$$\text{nextCapacity} = (23 \times 2) + 1 = 47$$
$$\text{nextCapacity} = (47 \times 2) + 1 = 95$$
1. **Preserving Odd Numbers:** Starting from 11, $2n + 1$ guarantees the capacity is **always an odd number**.
2. **Hash Distribution Quality:** When a hash table capacity is not a power of two, using an odd (or prime) capacity significantly reduces common divisors between the table length and typical object hash codes, spreading entries more evenly across buckets.
3. **Contrast with HashMap:** `HashMap` doubles capacity ($2 \times n$), which enables the brilliant high-bit masking optimization (`e.hash & oldCap == 0 ? low : high`) without recomputing hash modulo, but requires a secondary hash-spreading function (`h ^ (h >>> 16)`) to mitigate poor low-bit entropy.

### 3.4 Retrieval & Deletion

- **`public synchronized V get(Object key)`:** Acquires the instance monitor (`this`), computes index via `(key.hashCode() & 0x7FFFFFFF) % tab.length`, and linearly traverses the singly linked list.
- **`public synchronized V remove(Object key)`:** Acquires the instance monitor (`this`), traverses the chain, splices the node out by linking `prev.next = e.next`, decrements `count`, increments `modCount`, and returns `e.value`.

### 3.5 Enumeration vs Iterator Dual Support

`Hashtable` exposes two distinct iteration APIs:
1. **Legacy JDK 1.0 Enumerations:** `elements()` and `keys()`.
   - Returns an internal `Enumerator<T>`.
   - **NOT fail-fast!** It does NOT verify `modCount`. If another thread mutates the table during iteration, the `Enumeration` does not throw `ConcurrentModificationException`, but may enter infinite loops, skip entries, or return stale data.
2. **Modern JDK 1.2 Iterators:** `keySet().iterator()`, `values().iterator()`, `entrySet().iterator()`.
   - Also implemented by `Enumerator<T>`, but configured with `iterator = true`.
   - **STRICTLY fail-fast:** Checks `modCount != expectedModCount` on `next()` and `remove()`. Throws `ConcurrentModificationException`.

---

## 4. Complexity & Behavioral Matrix

| Operation | Average Time | Worst Case Time | Amortized Time | Synchronization Lock Scope |
| :--- | :--- | :--- | :--- | :--- |
| `get(K)` | $O(1)$ | $O(n)$ (bucket collision chain) | $O(1)$ | Synchronized on `this` instance |
| `containsKey(K)` | $O(1)$ | $O(n)$ | $O(1)$ | Synchronized on `this` instance |
| `put(K, V)` | $O(1)$ | $O(n)$ (rehash or long chain) | $O(1)$ | Synchronized on `this` instance |
| `remove(K)` | $O(1)$ | $O(n)$ | $O(1)$ | Synchronized on `this` instance |
| `size()` | $O(1)$ | $O(1)$ | $O(1)$ | Synchronized on `this` instance |
| `contains(V)` | $O(n)$ | $O(n)$ (full table scan) | $O(n)$ | Synchronized on `this` instance |
| Full Iteration | $O(n + \text{capacity})$ | $O(n + \text{capacity})$ | $O(1)$ per step | Locks during acquisition of collection view |

### Comprehensive Architectural Comparison: `Hashtable` vs `HashMap` vs `ConcurrentHashMap`

| Dimension | `Hashtable` (JDK 1.0) | `HashMap` (JDK 1.2 / 8+) | `ConcurrentHashMap` (JDK 8+) |
| :--- | :--- | :--- | :--- |
| **Thread Safety** | Thread-safe (coarse-grained synchronized) | Not thread-safe | Thread-safe (lock-free reads, CAS + bucket lock) |
| **Lock Granularity** | Entire table (`this` monitor) | None | Per-bucket head node (`synchronized(node)`) |
| **Null Keys** | **Disallowed** (Throws NPE) | **Allowed** (Stored in bucket index 0) | **Disallowed** (Throws NPE) |
| **Null Values** | **Disallowed** (Throws NPE) | **Allowed** | **Disallowed** (Throws NPE) |
| **Default Initial Capacity** | **11** | **16** | **16** |
| **Capacity Growth** | $(2 \times n) + 1$ (Always odd) | $2 \times n$ (Power of two) | $2 \times n$ (Power of two) |
| **Bucket Index Math** | `(hash & 0x7FFFFFFF) % capacity` | `(capacity - 1) & hash` | `(capacity - 1) & spread(hash)` |
| **Collision Resolution** | Singly linked list forever ($O(n)$ worst) | Singly linked list $\rightarrow$ Red-Black Tree ($O(\log n)$) | Singly linked list $\rightarrow$ Red-Black Tree ($O(\log n)$) |
| **Iterator Traversal** | Fail-fast (Iterators) / Non-fail-fast (Enumerations) | Fail-fast (`ConcurrentModificationException`) | Weakly consistent (No CME, reflects updates) |
| **Allocation Timing** | Eager (at construction) | Lazy (on first `put()`) | Lazy (on first `put()`) |

---

## 5. Concurrency & Thread-Safety Mechanics

### The Monolithic Monitor Bottleneck

Every public method in `Hashtable` is decorated with the `synchronized` keyword:
```java
public synchronized int size() { return count; }
public synchronized boolean isEmpty() { return count == 0; }
public synchronized V get(Object key) { ... }
public synchronized V put(K key, V value) { ... }
public synchronized V remove(Object key) { ... }
```

#### Why This Crushes Multithreaded Scalability:
1. **Read-Read Contention:** If Thread A calls `get("key1")`, Thread B cannot concurrently execute `get("key2")`—even though both threads are merely reading completely different buckets! Thread B is suspended by the OS scheduler.
2. **Context-Switching & Futex Storms:** As the number of competing threads exceeds the number of physical CPU cores, threads spend the majority of CPU time transitioning between user space and kernel space to sleep and wake via OS futexes.
3. **Compound Operation Vulnerability (Race Conditions):** Even though individual methods are thread-safe, **compound check-then-act operations are completely broken** without external client-side synchronization:
   ```java
   // DANGEROUS: Race condition exists!
   if (!hashtable.containsKey("user_123")) {
       // Another thread can execute put("user_123", ...) RIGHT HERE!
       hashtable.put("user_123", new UserProfile());
   }
   ```
   To make this safe, the client MUST externally synchronize on the `hashtable` instance:
   ```java
   synchronized (hashtable) {
       if (!hashtable.containsKey("user_123")) {
           hashtable.put("user_123", new UserProfile());
       }
   }
   ```
   In contrast, `ConcurrentHashMap` provides atomic primitives (`putIfAbsent`, `computeIfAbsent`, `merge`) that eliminate client-side locking entirely.

---

## 6. Production-Grade Java Code Verification

```java
package com.raysi.jcf.map.hashtable.notes;

import java.util.*;

/**
 * Verification of Hashtable legacy quirks:
 *  1. Null rejection on keys and values.
 *  2. Enumeration vs Iterator fail-fast differences.
 *  3. The legacy contains() method trap.
 * 
 * @author raysi
 */
public class HashtableInternalVerification {

    public static void main(String[] args) {
        Hashtable<String, String> table = new Hashtable<>();

        System.out.println("=== 1. Null Rejection Verification ===");
        try {
            table.put(null, "Test");
        } catch (NullPointerException e) {
            System.out.println("-> Null Key Rejected with NullPointerException: key.hashCode() fails");
        }

        try {
            table.put("ValidKey", null);
        } catch (NullPointerException e) {
            System.out.println("-> Null Value Rejected with NullPointerException: explicit null check fails");
        }

        System.out.println("\n=== 2. The dangerous contains() Trap ===");
        table.put("Alpha", "FirstLetter");
        table.put("Beta", "SecondLetter");

        // Developer thinks contains() checks for KEYS (like containsKey)
        // In reality, contains() delegates to containsValue()!
        boolean foundKey = table.contains("Alpha"); 
        boolean foundVal = table.contains("FirstLetter");
        System.out.println("table.contains(\"Alpha\") (Checking key via legacy method): " + foundKey); // FALSE!
        System.out.println("table.contains(\"FirstLetter\") (Legacy method checks VALUE): " + foundVal); // TRUE!

        System.out.println("\n=== 3. Enumeration vs Iterator Mutability ===");
        // Add more elements
        table.put("Gamma", "ThirdLetter");

        // A. Modern Iterator (Fail-Fast)
        Iterator<String> it = table.keySet().iterator();
        try {
            while (it.hasNext()) {
                String k = it.next();
                if (k.equals("Alpha")) {
                    table.put("Delta", "FourthLetter"); // Structural modification!
                }
            }
        } catch (ConcurrentModificationException e) {
            System.out.println("-> Modern Iterator caught ConcurrentModificationException as expected!");
        }

        // B. Legacy Enumeration (NOT Fail-Fast)
        // Reset and test Enumeration
        table.remove("Delta");
        Enumeration<String> en = table.elements();
        System.out.println("Iterating via legacy Enumeration while mutating...");
        int count = 0;
        while (en.hasMoreElements()) {
            String val = en.nextElement();
            count++;
            if (count == 1) {
                // Mutating table during enumeration traversal
                table.put("Omega", "LastLetter"); 
            }
        }
        System.out.println("-> Legacy Enumeration completed without CME! (Count seen: " + count + ")");
    }
}
```

---

## 7. Top 7 High-Signal Interview Questions & Deep Answers

### Q1: Why does `Hashtable` calculate bucket index as `(hash & 0x7FFFFFFF) % tab.length`, whereas `HashMap` uses `(n - 1) & hash`?
**Deep Answer:**  
`HashMap` mandates that its internal table capacity must always be a power of two ($n = 2^k$). When $n$ is a power of two, the modulo operation $hash \pmod n$ is mathematically identical to masking with $n - 1$:
$$hash \pmod{2^k} \equiv hash \ \& \ (2^k - 1)$$
Bitwise `AND` requires only **1 CPU cycle**, while modulo (`idiv`) costs **10 to 25 cycles**. 
`Hashtable`, however, does not constrain its capacity to powers of two. Its default capacity is 11, and rehash expands to $2n + 1$. Because $n$ is not a power of two, bitwise masking cannot be used; it must execute hardware modulo. Furthermore, because Java signed integers can have negative hash codes (where `negative % length` produces a negative array index), it masks with `0x7FFFFFFF` to strip the sign bit before performing modulo.

### Q2: Why does `Hashtable` throw `NullPointerException` on `null` keys and `null` values, whereas `HashMap` permits both?
**Deep Answer:**  
- **For `null` keys:** In `Hashtable.put()`, the code directly invokes `int hash = key.hashCode();` without a null check, throwing `NullPointerException`. In `HashMap`, the key hash function explicitly handles null: `(key == null) ? 0 : (h = key.hashCode()) ^ (h >>> 16)`, routing null keys to bucket 0.
- **For `null` values:** `Hashtable.put()` contains an explicit guard: `if (value == null) throw new NullPointerException();`. 
- **Architectural Rationale:** `Hashtable` was designed for concurrent usage in JDK 1.0. As Doug Lea explained, permitting `null` values in concurrent maps creates critical ambiguity: if `map.get(k)` returns `null`, does it mean the key is absent, or does it mean the key is mapped to `null`? In a single-threaded `HashMap`, one can disambiguate with `map.containsKey(k)`. In a concurrent map, between `get()` and `containsKey()`, another thread can insert or delete the key! To prevent this race condition, null keys and values are strictly prohibited.

### Q3: What is the behavioral difference between `Hashtable.elements()` and `Hashtable.values().iterator()`?
**Deep Answer:**  
- `Hashtable.elements()` returns an `Enumeration<V>`. This is a legacy JDK 1.0 interface. The implementation does not track or check `modCount`. If the table is structurally mutated during iteration, it will not throw `ConcurrentModificationException`. It may iterate over newly added elements, miss existing ones, or exhibit undefined behavior.
- `Hashtable.values().iterator()` returns an `Iterator<V>` complying with the JDK 1.2 Collections Framework. It captures `expectedModCount = modCount` and validates it on every call to `next()` and `remove()`. Any structural modification triggers an immediate `ConcurrentModificationException` (fail-fast behavior).

### Q4: Why does `Hashtable.rehash()` use the formula `(oldCapacity << 1) + 1` instead of simply doubling the capacity?
**Deep Answer:**  
Because `Hashtable` relies on the modulo operator `% capacity` rather than bitwise masking, its bucket distribution depends heavily on the divisor having no common factors with the generated hash codes. 
If capacity were an even number (e.g., doubling 10 to 20), keys with even hash codes would map only to even bucket indices, leaving half the buckets empty and doubling collisions. 
The formula `(oldCapacity << 1) + 1` computes $2n + 1$. An odd number multiplied by 2 is always even; adding 1 guarantees the new capacity is **strictly odd**. This increases the likelihood that capacity is coprime with many hash codes, producing a more uniform distribution.

### Q5: Why is `Hashtable.contains(Object value)` considered a dangerous method in production code?
**Deep Answer:**  
In modern collections, developers expect `contains()` to check for the presence of a **key** (as in `Collection.contains()` or `Set.contains()`). 
However, in `Hashtable`, `contains(Object value)` is an obsolete JDK 1.0 method that searches for the **value**, not the key (it is identical to `containsValue()`). 
This creates two severe hazards:
1. **Semantic Bug:** Developers calling `table.contains("admin")` expecting to check if the user exists as a key will receive `false` if "admin" is only a key, leading to authorization or lookup flaws.
2. **Performance Degradation:** Searching for a value requires an exhaustive, full-table $O(n)$ scan across all bucket chains, holding the coarse monitor lock throughout the entire traversal.

### Q6: If both `Hashtable` and `Collections.synchronizedMap(new HashMap<>())` use coarse-grained locking, why is `Collections.synchronizedMap` preferred?
**Deep Answer:**  
1. **Separation of Concerns:** `HashMap` can be used without synchronization when thread safety is not needed, avoiding unnecessary lock overhead.
2. **Lock Customization:** `Collections.synchronizedMap(map)` allows specifying a custom mutex object (`Collections.synchronizedMap(map, customLock)`), enabling client code to coordinate map access with other synchronized resources. In `Hashtable`, the lock is permanently bound to `this`.
3. **Internal Architecture:** `HashMap` features JDK 8+ Red-Black treeification ($O(\log n)$ worst-case collision handling) and fast bitwise index calculation, whereas `Hashtable` suffers from $O(n)$ linked list degradation and slow CPU modulo operations.
4. **Standardization:** `Hashtable` carries obsolete JDK 1.0 baggage (`Dictionary`, `Enumeration`) and non-standard method names (`contains`).

### Q7: Can race conditions occur in a multi-threaded application even if all operations use `Hashtable`?
**Deep Answer:**  
**Yes.** `Hashtable` guarantees thread safety only for **individual method invocations**, not for sequences of operations. 
Common compound operations like:
- **Check-Then-Act:** `if (!table.containsKey(k)) table.put(k, v);`
- **Read-Modify-Write:** `Integer count = table.get(k); table.put(k, count + 1);`
are prone to race conditions. 
Between the `get()` and the `put()`, another thread can intervene and overwrite the value, leading to lost updates. 
To achieve thread safety with `Hashtable`, callers must wrap compound blocks in an explicit `synchronized (table)` block. Alternatively, using `ConcurrentHashMap` with atomic methods (`computeIfAbsent`, `merge`, `putIfAbsent`) solves this cleanly without explicit locks.
