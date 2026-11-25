# <equals() and hashCode() Contract Mastery — Deep Dive>
> Author: raysi

---

## 1. Architectural Overview & Hierarchy

In the Java type system, every object inherits from `java.lang.Object`. Among the methods defined on `Object`, none have a more direct impact on JVM performance and collection correctness than:
```java
public boolean equals(Object obj);
public native int hashCode();
```

### Exact Method Signatures in `java.lang.Object`
```java
package java.lang;

public class Object {
    public boolean equals(Object obj) {
        return (this == obj); // Default: Reference equality
    }

    @IntrinsicCandidate
    public native int hashCode(); // Default: Identity hash code derived from object header / memory state
}
```

### ASCII Hashing & Key Lookup Pipeline
When an object is queried from a `HashMap` via `map.get(key)`, the JVM executes a strict multi-tier pipeline:

```
                  ┌────────────────────────┐
                  │       Target Key       │
                  └───────────┬────────────┘
                              │
                              ▼
                     key.hashCode() [32-bit int]
                              │
                              ▼
                    HashMap.hash(key) [Perturbation]
                      h ^ (h >>> 16)
                              │
                              ▼
                 Bucket Index: (n - 1) & hash
                              │
                              ▼
                 Access table[index] bucket
                              │
            ┌─────────────────┴─────────────────┐
            │ Is bucket null?                   │
            │   ├── YES ──► Return null         │
            │   └── NO                          │
            ▼                                   ▼
┌────────────────────────────────────────────────────────┐
│ Fast Primitive Equality Check:                         │
│   e.hash == hash                                       │
│   ├── FALSE ──► SKIP node! (.equals() is NEVER called) │
│   └── TRUE                                             │
└───────────────────────────┬────────────────────────────┘
                            │
                            ▼
┌────────────────────────────────────────────────────────┐
│ Identity or Deep Equality Check:                       │
│   e.key == key || (key != null && key.equals(e.key))   │
│   ├── TRUE  ──► MATCH FOUND! Return e.value            │
│   └── FALSE ──► Traverse next node in chain / tree     │
└────────────────────────────────────────────────────────┘
```

### Why Does This Contract Exist? What Problem Does It Solve?
Without a mathematical contract coupling equality and hashing, hash-based data structures (`HashMap`, `HashSet`, `LinkedHashMap`, `ConcurrentHashMap`) cannot guarantee consistency:
1. **Locatability**: The hash table must determine *which bucket* an object belongs to in $O(1)$ time. If two objects are conceptually equal (`a.equals(b) == true`) but produce different hash codes, they will be stored in or searched for in different buckets, making retrieval impossible.
2. **Defensive Collision Management**: Hash codes are finite (32-bit signed integers: $-2^{31}$ to $2^{31}-1$), while the universe of possible objects is unbounded. Collisions ($a \neq b$ but `a.hashCode() == b.hashCode()`) are unavoidable. The contract establishes how collections distinguish identical hash collisions using `equals()`.

---

## 2. Internal Data Structure & Memory Layout (JDK Source Code Level)

### Identity Hash Code vs Overridden `hashCode()`
By default, `Object.hashCode()` invokes the JVM intrinsic `System.identityHashCode(Object o)`.
In HotSpot JVM, the identity hash code is **not** the raw memory address (since objects move during Garbage Collection compaction). Instead, HotSpot lazily computes a 31-bit pseudo-random value when `identityHashCode()` is first invoked on an object and stores it directly inside the object's **Mark Word** in the object header.

### Object Header Layout (64-bit JVM with Compressed OOPs)

```
 0                                                          63
┌─────────────────────────────────────────────────────────────┐
│                       Mark Word (64 bits)                   │
├───────────────────────────────┬─────────────────────────────┤
│ Klass Word (32 bits / 4 bytes)│ (Pointer to Class Metadata) │
├───────────────────────────────┴─────────────────────────────┤
│ Fields / Instance Data ...                                  │
└─────────────────────────────────────────────────────────────┘
```

#### Detailed 64-bit Mark Word Bit Allocation (HotSpot):
```
|-----------------------------------------------------------------------------------|
|                               Mark Word (64 bits)                                 |
|-----------------------------------------------------------------------------------|
| unused:25 | identity_hashcode:31 | unused:1 | age:4 | biased_lock:0 | lock:01 (Normal)
|-----------------------------------------------------------------------------------|
```
- **31 bits** are allocated for `identity_hashcode`.
- Once generated, this value is permanently stored in the header so the identity hash code remains invariant across GC relocations.
- **Overriding `hashCode()`**: When a class overrides `hashCode()`, the mark word's identity hash code field is completely bypassed during map operations. The custom calculation is executed, typically reading instance fields from the heap.

### CPU Cache & Memory Overhead Analysis
- **Primitive Hash Comparison as CPU Short-Circuit**:
  Notice the exact code inside `HashMap.getNode()`:
  ```java
  if (first.hash == hash &&
      ((k = first.key) == key || (key != null && key.equals(k))))
  ```
  1. `first.hash == hash`: Compares two 32-bit primitive integers stored directly inside the CPU registers. Cost: **1 clock cycle**.
  2. `(k = first.key) == key`: Compares two 32-bit pointers (reference equality). Cost: **1 clock cycle**.
  3. `key.equals(k)`: Incurs a virtual method invocation table (`vtable`) lookup, pointer dereferences to field addresses, and branch evaluations. Cost: **10 to 50+ clock cycles**.
  The primitive `hash == first.hash` check short-circuits the evaluation and avoids costly virtual method calls for 99.9% of non-matching nodes.

---

## 3. Step-by-Step Internal Working of Core Operations

### The 3-Clause Contract of `hashCode()` (`java.lang.Object`)
1. **Consistency**: Whenever invoked on the same object more than once during an execution of a Java application, `hashCode()` must consistently return the same integer, provided no information used in `equals()` comparisons on the object is modified.
2. **Equality Implication**: If two objects are equal according to the `equals(Object)` method, then calling `hashCode()` on each of the two objects **MUST** produce the same integer result.
   $$\forall a, b: a.\text{equals}(b) \implies a.\text{hashCode}() == b.\text{hashCode}()$$
3. **Collision Tolerance**: If two objects are unequal according to `equals(Object)`, it is **NOT** required that their `hashCode()` values be distinct. However, generating distinct integers for unequal objects improves the performance of hash tables.

### The 5-Clause Equivalence Relation Contract of `equals()`
The `equals()` method implements an equivalence relation on non-null object references:
1. **Reflexive**: For any non-null reference value $x$, `x.equals(x)` must return `true`.
2. **Symmetric**: For any non-null reference values $x$ and $y$, `x.equals(y)` must return `true` if and only if `y.equals(x)` returns `true`.
3. **Transitive**: For any non-null reference values $x, y, z$, if `x.equals(y)` returns `true` and `y.equals(z)` returns `true`, then `x.equals(z)` must return `true`.
4. **Consistent**: For any non-null reference values $x$ and $y$, multiple invocations of `x.equals(y)` consistently return `true` or consistently return `false`, provided no information used in `equals` comparisons is modified.
5. **Non-nullity**: For any non-null reference value $x$, `x.equals(null)` must return `false`.

### What Happens When the Contract is Violated?

#### Case A: Overriding `equals()` but NOT `hashCode()`
Consider two objects `e1` and `e2` representing Employee with ID `101`.
```java
Employee e1 = new Employee(101, "Alice");
Employee e2 = new Employee(101, "Alice");
// e1.equals(e2) is true!
map.put(e1, "Engineer");
map.get(e2); // RETURNS NULL!
```
**Internal Trace**:
1. `map.put(e1, ...)` computes `e1.hashCode()`, which defaults to `System.identityHashCode(e1)` (e.g., `85268746`). It maps to **Bucket 10**.
2. `map.get(e2)` computes `e2.hashCode()`, which defaults to `System.identityHashCode(e2)` (e.g., `19385721`). It maps to **Bucket 3**.
3. Bucket 3 is empty! `map.get(e2)` returns `null`.
4. The map now contains duplicate entries for what should be the same logical key.

#### Case B: Mutable Key Mutated In-Place (The Silent Memory Leak)
Consider a key whose fields are modified *after* insertion into a `HashMap`:
```java
OrderKey key = new OrderKey("ORD-999");
map.put(key, orderDetails);

// Mutating key state in-place
key.setOrderId("ORD-888");

map.get(key); // RETURNS NULL!
map.containsKey(key); // RETURNS FALSE!
map.remove(key); // RETURNS NULL (Cannot be deleted!)
map.size(); // STILL 1!
```
**Internal Step-by-Step Mechanism**:
1. `key` is inserted with initial hash `hash1 = hash("ORD-999")` into `table[index1]`.
2. `key` state is mutated. Its hash code becomes `hash2 = hash("ORD-888")`.
3. When `map.get(key)` is invoked:
   - Index is calculated: `index2 = (n - 1) & hash2`.
   - If `index2 != index1`, `HashMap` searches the wrong bucket and returns `null`.
   - Even if `index2 == index1` by coincidence, `HashMap` checks `first.hash == hash2`. Because the stored node has cached `first.hash == hash1`, `first.hash == hash2` evaluates to **`false`**!
   - `.equals()` is never called.
   - The node is now **completely unreachable by key**, yet it remains pinned in memory by `table[index1]`. It cannot be garbage collected, creating a silent JVM heap memory leak!

---

## 4. Complexity & Behavioral Matrix

### Hash Code Distribution vs Time Complexity

| Hash Distribution Scenario | Average Lookup | Worst-case Lookup | Bucket Structure | Condition |
|:---|:---:|:---:|:---:|:---|
| **Ideal Uniform Distribution** | $O(1)$ | $O(1)$ | 0 or 1 Node per bucket | High-entropy hash function |
| **Moderate Collisions** | $O(1)$ | $O(\log N)$ | Red-Black Tree (`TreeNode`) | $N \ge 8$ with keys implementing `Comparable` |
| **Degenerate (Constant `hashCode = 42`)** | $O(\log N)$ | $O(\log N)$ | Red-Black Tree (`TreeNode`) | All keys collide, keys implement `Comparable` |
| **Pathological Collisions (No `Comparable`)**| $O(N)$ | $O(N)$ | Singly-linked list or un-ordered Tree | Keys do NOT implement `Comparable` |
| **Broken Contract (`equals` without `hashCode`)**| N/A | N/A | Silent data corruption | Equal keys hash to different buckets |

### Contract Implementation Matrix

| Implementation Type | `equals` Behavior | `hashCode` Behavior | `HashMap` Lookup Correctness | Memory Leak Risk |
|:---|:---|:---|:---:|:---:|
| **Default (`Object`)** | Reference equality (`==`) | Identity hash code (Mark Word) | Correct (Key = instance identity) | Low |
| **Overrode `equals` ONLY** | Value equality | Identity hash code | **BROKEN** (`get` returns null) | None |
| **Overrode `hashCode` ONLY** | Reference equality (`==`) | Value hash | **BROKEN** (Cannot find equal keys)| None |
| **Mutable Key (Both Overridden)**| Value equality | Value hash | **BROKEN upon mutation** | **CRITICAL (Heap Leak)** |
| **Immutable Key (Both Overridden)**| Value equality | Value hash | **100% Correct & Safe** | None |
| **Java 14+ `record`** | Field-by-field value equality| Field-by-field hash | **100% Correct** (if fields immutable)| None |

---

## 5. Concurrency & Thread-Safety Mechanics

### Immutability as the Ultimate Defense
Because `HashMap` relies on immutable hash codes:
1. **Keys MUST be deeply immutable**. All fields that participate in `equals()` and `hashCode()` should be declared `private final`.
2. Any collections or mutable objects stored in the key must be defensively copied upon construction and accessor calls.
3. If an object's state changes, its hash code changes, instantly corrupting the data structure.

### Thread-Safe Hash Caching Idiom (`java.lang.String` Design)
For immutable classes whose `hashCode()` calculation is expensive (e.g. `String`), the hash code can be computed lazily and cached in a private field:
```java
public final class ImmutableKey {
    private final String id;
    private final String tenant;
    private int hash; // Default 0

    public ImmutableKey(String id, String tenant) {
        this.id = Objects.requireNonNull(id);
        this.tenant = Objects.requireNonNull(tenant);
    }

    @Override
    public int hashCode() {
        int h = hash;
        if (h == 0) {
            h = Objects.hash(id, tenant);
            if (h == 0) h = 1; // Prevent recalculation on zero
            hash = h;
        }
        return h;
    }
}
```
*Concurrency Guarantee*: Because `int` reads and writes are atomic on 32/64-bit JVMs (JLS §17.7), and because the calculation is deterministic, multiple threads computing `hashCode()` concurrently will compute and write the exact same value. No synchronized blocks or volatile modifiers are required.

---

## 6. Production-Grade Java Code Verification

See the runnable demonstration files:
- [`MutableKeyMemoryLeakDemo.java`](../../../codes/collections/map/hashmap/MutableKeyMemoryLeakDemo.java)
- [`CustomImmutableKeyDesign.java`](../../../codes/collections/map/hashmap/CustomImmutableKeyDesign.java)

Key mechanics demonstrated in the code:
1. Mutating a key after insertion: `get()` returns `null`, `containsKey()` returns `false`, `remove()` fails, yet `size()` remains $> 0$.
2. The invisible memory leak: how orphaned entries permanently consume Old Generation heap space.
3. Designing an unshakeable immutable key using `final` fields, defensive copies, and cached hash codes.

---

## 7. Top 7 High-Signal Interview Questions & Deep Answers

### Q1: Why does `HashMap` check `p.hash == hash && ((k = p.key) == key || (key != null && key.equals(k)))` instead of simply calling `key.equals(p.key)`?
**Answer**:
This is a masterclass in JVM performance optimization:
1. **CPU Cycle Efficiency**: `p.hash == hash` compares two 32-bit primitive integers stored directly in CPU registers. This operation executes in **1 CPU clock cycle**.
2. **Short-Circuiting Virtual Dispatch**: In contrast, calling `key.equals(k)` requires a virtual method table (`vtable`) lookup, pointer dereferencing across heap boundaries, branch evaluations, and potential field dereferences. This takes **10 to 50+ CPU clock cycles**.
3. **Reference Equality Optimization**: `(k = p.key) == key` checks whether the references point to the exact same memory address. If they do, the method immediately knows they are equal without calling `equals()`.
Therefore, 99.9% of collisions or mismatched nodes are discarded in 1 clock cycle without ever invoking `equals()`.

### Q2: If you override `equals()`, why MUST you override `hashCode()`? Prove the exact line where `HashMap` fails.
**Answer**:
If you override `equals()` without overriding `hashCode()`, two instances with identical logical fields will inherit `Object.hashCode()`, which assigns each distinct object instance a different identity hash code.
In `HashMap.getNode(int hash, Object key)`:
```java
// Line 573 in JDK 21 java.util.HashMap
if (first.hash == hash && ((k = first.key) == key || (key != null && key.equals(k))))
```
When `map.get(newKey)` is called:
1. `newKey.hashCode()` generates `hashB`, whereas the stored entry was inserted under `hashA`.
2. First, `(n - 1) & hashB` navigates to the wrong bucket index.
3. Even if both hash codes collide to the same bucket index, the check `first.hash == hash` evaluates to `false` because `hashA != hashB`.
4. Therefore, `first.key.equals(newKey)` is **never even evaluated**. The lookup returns `null`, breaking the Map contract.

### Q3: Why is 31 used as the multiplier in `hashCode()` implementations (e.g., in `String` and `Arrays.hashCode`)?
**Answer**:
1. **Prime Multiplier**: Using a prime number reduces clustering and collisions during polynomial hashing:
   $$\text{hash} = s[0] \cdot 31^{n-1} + s[1] \cdot 31^{n-2} + \dots + s[n-1]$$
   If a composite number (like 32) were used, multiplying by 32 is equivalent to shifting left by 5 bits (`<< 5`), which means multiplying repeatedly would shift lower characters entirely out of the 32-bit integer, discarding all information from earlier characters!
2. **JIT Compiler Optimization**: 31 has a unique property:
   $$31 \times i = (i \ll 5) - i$$
   Modern CPU architectures perform bit shifts and subtractions in a single clock cycle, whereas integer multiplication historically took 3 to 10 clock cycles. The HotSpot C2 compiler automatically optimizes `31 * i` into a shift-and-subtract instruction.

### Q4: What happens if an object used as a `HashMap` key is mutated after insertion? Can `remove()` delete it?
**Answer**:
No, `map.remove(key)` will **fail and return null**.
When `remove(key)` is invoked:
1. It calculates `hash(key)`. Since the key was mutated, its hash code is now different from when it was inserted.
2. It looks in `table[(n - 1) & newHash]`. The original entry is sitting in `table[(n - 1) & oldHash]`.
3. The method fails to find the entry and returns `null`.
4. The entry remains trapped in the old bucket forever. `map.size()` continues to report its existence, but no key-based operation can access or delete it, resulting in a permanent heap memory leak. The only way to clean it is `map.clear()` or iterating over `map.entrySet().iterator()` and calling `iterator.remove()`.

### Q5: In `equals()`, why is `getClass() != o.getClass()` generally safer than `instanceof` when subclassing with additional state?
**Answer**:
Using `instanceof` violates the **Symmetry** contract of `equals()` when subclasses add state.
- Suppose `Point(int x, int y)` uses `instanceof Point`.
- Subclass `ColorPoint(int x, int y, Color c)` extends `Point` and checks `o instanceof ColorPoint`.
- If we compare `point.equals(colorPoint)`: `Point` checks `colorPoint instanceof Point` $\to$ returns `true`.
- If we compare `colorPoint.equals(point)`: `ColorPoint` checks `point instanceof ColorPoint` $\to$ returns `false`!
- Symmetry is broken: $a = b$ but $b \neq a$.
Using `getClass() != o.getClass()` strictly confines equality to objects of the exact same concrete class, guaranteeing symmetry and transitivity across inheritance hierarchies.

### Q6: Can a Java `record` type (Java 14+) suffer from `HashMap` memory leaks when used as a key?
**Answer**:
**Yes, if the record contains mutable components!**
While records are shallowly immutable (their fields are `final` and cannot be reassigned), if a record component references a mutable object (such as a `java.util.Date`, `List`, or custom mutable class):
```java
public record UserKey(String tenant, Date timestamp) {}
```
An external caller holding the `Date` reference can invoke `timestamp.setTime(...)`. This mutates the internal state of the `Date` object, altering `record.hashCode()` and triggering the exact same memory leak and lookup failure as a traditional mutable class. For records to be safe map keys, all components must themselves be strictly immutable.

### Q7: What is the difference between `System.identityHashCode(o)` and `o.hashCode()`? Can two different objects have the same `identityHashCode`?
**Answer**:
1. `o.hashCode()` is a virtual method call that executes whatever algorithm is defined in the class hierarchy.
2. `System.identityHashCode(o)` returns the default identity hash code generated by the JVM runtime, regardless of whether `o.getClass()` has overridden `hashCode()`.
3. **Collisions in Identity Hash Code**: Yes, two completely different objects can have the exact same `identityHashCode`. The identity hash code is a 31-bit integer ($2^{31} \approx 2.14 \text{ billion}$ distinct values). On a 64-bit JVM running large applications with hundreds of millions of objects, collisions are inevitable due to the Birthday Paradox. `System.identityHashCode` does NOT guarantee uniqueness; it only guarantees consistency for that specific object instance.
