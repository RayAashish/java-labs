# 01. HashMap Architecture and Internals

## 1. Core Data Structure Overview

At its foundation, `java.util.HashMap` in Java 8+ is a **Hash Table backed by an Array of Buckets**, where collisions are resolved using **Separate Chaining** combined with **Self-Balancing Red-Black Trees (Treeification)**.

```
       +-------------------------------------------------------------+
       |                  HashMap Instance in Memory                 |
       +-------------------------------------------------------------+
       |  size: int                |  Number of key-value mappings   |
       |  threshold: int           |  capacity * loadFactor          |
       |  loadFactor: float        |  Fraction full before resize    |
       |  modCount: int            |  Structural modification count  |
       |  table: Node<K,V>[]       |  Array of bucket heads          |
       +-------------------------------------------------------------+
```

### Node Representation in Memory

In standard chained buckets, elements are stored as instances of the internal static class `Node<K, V>`:

```java
static class Node<K,V> implements Map.Entry<K,V> {
    final int hash;    // Cached 32-bit hash value of key
    final K key;       // Immutable key reference
    V value;           // Stored value
    Node<K,V> next;    // Pointer to next node in the bucket chain

    Node(int hash, K key, V value, Node<K,V> next) {
        this.hash = hash;
        this.key = key;
        this.value = value;
        this.next = next;
    }
}
```

When a bucket exceeds the treeification threshold, nodes are converted to `TreeNode<K, V>`, which extends `LinkedHashMap.Entry<K, V>` (which in turn extends `HashMap.Node<K, V>`):

```java
static final class TreeNode<K,V> extends LinkedHashMap.Entry<K,V> {
    TreeNode<K,V> parent;  // Red-black tree links
    TreeNode<K,V> left;
    TreeNode<K,V> right;
    TreeNode<K,V> prev;    // Needed to unlink upon deletion
    boolean red;           // Color property of Red-Black node
}
```

---

## 2. Key Internal Constants

`java.util.HashMap` defines several critical tuning constants:

```java
// The default initial capacity - MUST be a power of two.
static final int DEFAULT_INITIAL_CAPACITY = 1 << 4; // 16

// The maximum capacity, used if a higher value is implicitly specified by either
// of the constructors with arguments. MUST be a power of two <= 1<<30.
static final int MAXIMUM_CAPACITY = 1 << 30; // 1,073,741,824

// The load factor used when none specified in constructor.
static final float DEFAULT_LOAD_FACTOR = 0.75f;

// The bin count threshold for using a tree rather than list for a bin.
static final int TREEIFY_THRESHOLD = 8;

// The bin count threshold for untreeifying a (split) bin during a resize operation.
static final int UNTREEIFY_THRESHOLD = 6;

// The smallest table capacity for which bins may be treeified.
// (Otherwise the table is resized if too many nodes in a bin).
static final int MIN_TREEIFY_CAPACITY = 64;
```

---

## 3. The Perturbation Hash Function: `hash(Object key)`

A key's raw `hashCode()` is a 32-bit signed integer. However, HashMap's table capacity is typically small (e.g., $16, 32, 64$). If we simply mapped `key.hashCode()` to bucket index using the low bits, keys that differ only in their higher 16 bits would collide persistently.

To resolve this, Java 8 uses a **perturbation function** (hash spreading):

```java
static final int hash(Object key) {
    int h;
    return (key == null) ? 0 : (h = key.hashCode()) ^ (h >>> 16);
}
```

### Why `(h ^ (h >>> 16))`?

1. **`h >>> 16`**: Unsigned right shift by 16 bits shifts the upper 16 bits into the lower 16-bit position.
2. **`^` (XOR)**: Blends the high bits into the low bits.
3. **Why XOR?** Bitwise XOR provides an unbiased $50\%$ probability for each bit ($0 \oplus 0 = 0$, $0 \oplus 1 = 1$, $1 \oplus 0 = 1$, $1 \oplus 1 = 0$). Unlike `&` (which biases toward 0) or `|` (which biases toward 1), XOR preserves entropy evenly.

### Bitwise Breakdown Example

Suppose `key.hashCode()` is `0b0101_1100_1010_1111_0000_1111_0000_1111`:

```
h:          0101 1100 1010 1111   0000 1111 0000 1111
h >>> 16:   0000 0000 0000 0000   0101 1100 1010 1111
------------------------------------------------------
hash = h ^ (h >>> 16):
            0101 1100 1010 1111   0101 0011 1010 0000
```

Now, when the table size is small (e.g., $n = 16$), the bucket index calculation uses only the lowest 4 bits. Because of the XOR operation, those lowest 4 bits incorporate variation from the top 16 bits, dramatically reducing bucket collisions!

---

## 4. Bucket Index Calculation: `(n - 1) & hash`

In a classic hash table, the bucket index is computed using the modulo operator:

$$\text{index} = \text{hash} \pmod n$$

In `java.util.HashMap`, this is replaced with:

$$\text{index} = (n - 1) \ \& \ \text{hash}$$

### Mathematical Proof of Equivalence

When $n$ is a power of 2 ($n = 2^k$):
- $n - 1$ in binary consists of $k$ ones in the least significant bit positions, and zeros everywhere else.
- For example, if $n = 16 = 2^4$:
  $$n - 1 = 15 = 00000000\ 00000000\ 00000000\ 00001111_2$$
- Performing bitwise AND with $(n - 1)$ simply extracts the lowest $k$ bits of `hash`.
- By definition of binary modular arithmetic:
  $$\text{hash} \pmod{2^k} \equiv \text{hash} \ \& \ (2^k - 1)$$

### Why Bitwise AND Instead of Modulo `%`?

1. **Execution Speed (CPU Clock Cycles)**:
   - The `%` operator translates to an integer division CPU instruction (`idiv` on x86/x64). Integer division typically takes **15 to 40 CPU clock cycles**.
   - The bitwise `&` operator translates to a single CPU instruction (`and`), which executes in **1 CPU clock cycle**.
   - In high-throughput systems performing millions of lookups per second, this results in significant performance gains.
2. **Handling Negative Hash Codes**:
   - In Java, `hashCode()` can be negative.
   - `(-15) % 16` yields `-15` (an invalid array index that throws `ArrayIndexOutOfBoundsException` unless manually handled with `Math.abs()`).
   - Bitwise `(16 - 1) & (-15)` automatically clears the sign bit and yields `1`, which is always a valid index in the range $[0, n-1]$.

### Why Capacity MUST Be a Power of Two

What if capacity $n$ were an odd or non-power-of-two number?
Suppose $n = 10$. Then $n - 1 = 9 = 00001001_2$.

Notice the bit pattern of 9:
- Bit 1 is `0`.
- Bit 2 is `0`.
- Bit 3 is `1`.
- Any bitwise `hash & 9` can **never produce an index where bit 1 or bit 2 is set**!
- Indices like 2 (`0010`), 3 (`0011`), 4 (`0100`), 5 (`0101`), 6 (`0110`), 7 (`0111`) could **never be reached**.
- This would waste over $60\%$ of the table's allocated buckets and create massive collision hotspots in the remaining buckets.

When $n = 2^k$, $n - 1$ is guaranteed to be a solid sequence of $1$s (`1111...`), ensuring that **every bit position contributes evenly**, yielding a completely uniform distribution.

### Table Size Forcing

Even if a developer passes a non-power-of-two capacity in the constructor, e.g.:

```java
Map<String, String> map = new HashMap<>(25);
```

HashMap internally rounds it up to the next highest power of two using `tableSizeFor()`:

```java
static final int tableSizeFor(int cap) {
    int n = -1 >>> Integer.numberOfLeadingZeros(cap - 1);
    return (n < 0) ? 1 : (n >= MAXIMUM_CAPACITY) ? MAXIMUM_CAPACITY : n + 1;
}
```
*(In Java 8, this was implemented with 5 consecutive bit-shifts: `n |= n >>> 1; n |= n >>> 2; n |= n >>> 4; n |= n >>> 8; n |= n >>> 16; return n + 1;`)*

Thus, `new HashMap<>(25)` allocates a table of capacity **32**.

---

## 5. Why Is the Default Load Factor 0.75?

The load factor defines the threshold at which the table capacity doubles:

$$\text{threshold} = \text{capacity} \times \text{loadFactor}$$

For the default capacity $16$ and load factor $0.75$:
$$\text{threshold} = 16 \times 0.75 = 12$$

When the 13th element is inserted, the table resizes to $32$.

### The Space-Time Tradeoff

- **Higher Load Factor (e.g., 1.0 or 1.25)**:
  - Conserves memory by keeping the array compact.
  - Increases collision frequency. Linked list chains become longer, increasing average lookup time from $O(1)$ toward $O(n)$ or $O(\log n)$.
- **Lower Load Factor (e.g., 0.5)**:
  - Minimizes collisions and speeds up lookups.
  - Doubles memory overhead and triggers frequent costly resizing passes.
- **0.75 is the Empirical "Sweet Spot"**:
  - Provides roughly $O(1)$ constant time performance while wasting very little unused bucket space.

### The Poisson Distribution Proof

The official OpenJDK source code documentation explains why $0.75$ and treeification threshold $8$ were chosen, based on probability theory.

Under a uniform random hash distribution, the probability that a given bucket contains $k$ items follows the **Poisson Distribution**:

$$P(k) = \frac{e^{-\lambda} \cdot \lambda^k}{k!}$$

With a default load factor of $0.75$, the average bucket occupancy $\lambda \approx 0.5$. Calculating the probability of a bucket reaching depth $k$:

| Bucket Depth ($k$) | Probability $P(k)$ | Scientific Notation |
| :---: | :---: | :---: |
| 0 | 0.60653066 | $6.06 \times 10^{-1}$ |
| 1 | 0.30326533 | $3.03 \times 10^{-1}$ |
| 2 | 0.07581633 | $7.58 \times 10^{-2}$ |
| 3 | 0.01263606 | $1.26 \times 10^{-2}$ |
| 4 | 0.00157952 | $1.58 \times 10^{-3}$ |
| 5 | 0.00015795 | $1.58 \times 10^{-4}$ |
| 6 | 0.00001316 | $1.32 \times 10^{-5}$ |
| 7 | 0.00000094 | $9.40 \times 10^{-7}$ |
| 8 | 0.00000006 | $6.00 \times 10^{-8}$ |

**Conclusion**: The chance of any bucket reaching a depth of 8 under normal conditions is less than **1 in 10 million** ($0.000006\%$). 

Therefore, treeification at threshold 8 is an extreme fallback designed to prevent Denial-of-Service (Hash Collision DoS) attacks or poor custom `hashCode()` implementations, without penalizing standard application memory.
