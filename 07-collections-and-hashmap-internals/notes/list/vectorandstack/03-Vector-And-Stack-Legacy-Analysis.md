# <Vector & Stack — Legacy Analysis & Architectural Pitfalls>
> Author: raysi

---

## 1. Architectural Overview & Hierarchy

`java.util.Vector<E>` and `java.util.Stack<E>` are legacy classes introduced in JDK 1.0 (1996), two years prior to the release of the unified Java Collections Framework (JCF) in JDK 1.2. In JDK 1.2, both classes were retrofitted to implement the `List<E>` interface and extend `AbstractList<E>`.

### Exact Package and Class Signatures

```java
package java.util;

public class Vector<E>
    extends AbstractList<E>
    implements List<E>, RandomAccess, Cloneable, java.io.Serializable
```

```java
package java.util;

public class Stack<E> extends Vector<E>
```

### ASCII Inheritance & Hierarchy Diagram

```
                 +-----------------------+
                 |  java.lang.Iterable   |
                 +-----------+-----------+
                             |
                             v
                 +-----------------------+
                 | java.util.Collection  |
                 +-----------+-----------+
                             |
                             v
                 +-----------------------+
                 |    java.util.List     |
                 +-----------+-----------+
                             |
                             v
                 +-----------------------+
                 | java.util.AbstractList|
                 +-----------+-----------+
                             |
         +-------------------+-------------------+
         |                   |                   |
         v                   v                   v
+------------------+ +----------------+ +------------------+
| java.util.       | | java.lang.     | | java.io.         |
| RandomAccess     | | Cloneable      | | Serializable     |
+--------+---------+ +-------+--------+ +--------+---------+
         |                   |                   |
         +-------------------+-------------------+
                             |
                             v
                 +-----------------------+
                 |   java.util.Vector    | (JDK 1.0)
                 +-----------+-----------+
                             |
                             v
                 +-----------------------+
                 |    java.util.Stack    | (JDK 1.0 Architectural Anti-Pattern)
                 +-----------------------+
```

### Why Do These Classes Exist? What Problem Were They Supposed to Solve?
- **JDK 1.0 Thread-Safety Strategy:** In early Java, concurrency was approached by wrapping entire data structures in method-level synchronized monitors. `Vector` was designed as the default dynamic resizable array, with every public method marked `synchronized`.
- **`Stack` as LIFO Storage:** `Stack` was built to provide a Last-In, First-Out collection offering `push()`, `pop()`, `peek()`, and `search()`.
- **Why They Are Obsolete Today:**
  1. **Coarse-Grained Synchronization Bottleneck:** Every method synchronizes on `this`, causing heavy monitor contention and preventing concurrent reads.
  2. **The "Inheritance Over Composition" Design Disaster:** `Stack` extends `Vector`. By inheriting from `Vector`, `Stack` exposes all positional methods (`add(index, e)`, `remove(index)`, `set(index, e)`), which completely violates the LIFO invariant.
  3. **The False Sense of Thread Safety:** Synchronizing individual methods does **not** make compound operations (e.g., check-then-act `if (!stack.isEmpty()) stack.pop()`) thread-safe without external locks.

---

## 2. Internal Data Structure & Memory Layout (JDK Source Code Level)

### Exact Internal Fields from JDK Source Code

```java
public class Vector<E> extends AbstractList<E> ... {

    /**
     * The array buffer into which the components of the vector are stored.
     * The capacity is the length of this array buffer.
     */
    protected Object[] elementData;

    /**
     * The number of valid components in this Vector object.
     */
    protected int elementCount;

    /**
     * The amount by which the capacity of the vector is automatically
     * incremented when its size becomes greater than its capacity.
     * If capacityIncrement <= 0, the capacity doubles on each expansion.
     */
    protected int capacityIncrement;
}
```

### ASCII Memory Diagram

```
[Vector Instance on JVM Heap] (32 bytes)
+--------------------------------------------------------------+
| Mark Word (64-bit / 8 bytes) [Holds Object Monitor / Lock]   |
+--------------------------------------------------------------+
| Klass Word (32-bit / 4 bytes)                                |
+--------------------------------------------------------------+
| modCount (int / 4 bytes) [From AbstractList]                 |
+--------------------------------------------------------------+
| elementCount (int / 4 bytes)                                 |
+--------------------------------------------------------------+
| capacityIncrement (int / 4 bytes)                            |
+--------------------------------------------------------------+
| elementData (compressed OOP reference / 4 bytes)              |
+--------------------------------------------------------------+
| [4-byte padding to align to 8-byte boundary]                 |
+------------------------------+-------------------------------+
                               |
                               v
[Object[] Array Buffer on JVM Heap] (16B header + 4B * capacity)
+--------------------------------------------------------------+
| Array Header: Mark Word (8B) + Klass Word (4B) + Length (4B) |
+--------------------------------------------------------------+
| elementData[0] ... elementData[elementCount-1]               |
+--------------------------------------------------------------+
| elementData[elementCount ... capacity-1] (null references)   |
+--------------------------------------------------------------+
```

### CPU Cache & Memory Overhead Analysis

#### 1. The 2x Expansion Penalty vs. ArrayList's 1.5x
- By default (`capacityIncrement <= 0`), `Vector` **doubles** its internal capacity upon overflow:
  ```java
  int newCapacity = oldCapacity + ((capacityIncrement > 0) ?
                                   capacityIncrement : oldCapacity);
  ```
- **Doubling progression:** $10 \rightarrow 20 \rightarrow 40 \rightarrow 80 \rightarrow 160 \rightarrow 320 \dots$
- **Memory Inefficiency:**
  - If a `Vector` holds 10,000,001 elements, capacity doubles from 10,000,000 to **20,000,000**.
  - That single insertion consumes an additional **40 MB** of heap memory just for unused array pointers.
  - In comparison, `ArrayList` grows by 1.5x ($10\text{M} \rightarrow 15\text{M}$), wasting only half as much memory.
- **Allocator Fragmentation:** As demonstrated by memory allocator theory, doubling prevents the memory allocator from ever reusing previously deallocated memory blocks, increasing system page table fragmentation.

#### 2. Synchronization Microarchitectural Tax (MESI Protocol & Bus Locking)
- Even in single-threaded execution, entering and exiting `synchronized` blocks requires checking and writing to the object's Mark Word.
- In multithreaded environments, multiple CPU cores competing for the object monitor trigger **cache coherence invalidation traffic** across the CPU interconnect (Intel UPI / AMD Infinity Fabric) via the MESI cache coherence protocol, thrashing L1/L2 caches across cores.

---

## 3. Step-by-Step Internal Working of Core Operations

### 1. Instantiation (Constructors)

```java
public Vector() {
    this(10);
}

public Vector(int initialCapacity) {
    this(initialCapacity, 0);
}

public Vector(int initialCapacity, int capacityIncrement) {
    super();
    if (initialCapacity < 0)
        throw new IllegalArgumentException("Illegal Capacity: " + initialCapacity);
    this.elementData = new Object[initialCapacity];
    this.capacityIncrement = capacityIncrement;
}
```
Unlike `ArrayList`, `Vector` does **NOT** use lazy initialization. `new Vector<>()` **immediately allocates** an `Object[10]` array on the heap.

### 2. Synchronization on Every Method

Every public method across `Vector` acquires the intrinsic monitor lock `synchronized(this)`:
```java
public synchronized boolean add(E e) {
    modCount++;
    add(e, elementData, elementCount);
    return true;
}

public synchronized E get(int index) {
    if (index >= elementCount)
        throw new ArrayIndexOutOfBoundsException(index);
    return elementData(index);
}

public synchronized int size() {
    return elementCount;
}
```

### 3. The `Stack` Subclass Mechanics

`Stack` adds five LIFO-specific methods atop `Vector`:

```java
public class Stack<E> extends Vector<E> {

    public E push(E item) {
        addElement(item); // synchronized method on Vector
        return item;
    }

    public synchronized E pop() {
        E obj;
        int len = size();
        obj = peek();
        removeElementAt(len - 1);
        return obj;
    }

    public synchronized E peek() {
        int len = size();
        if (len == 0)
            throw new EmptyStackException();
        return elementAt(len - 1);
    }

    public boolean empty() {
        return size() == 0;
    }

    public synchronized int search(Object o) {
        int i = lastIndexOf(o);
        if (i >= 0) {
            return size() - i; // Returns 1-based distance from top of stack!
        }
        return -1;
    }
}
```

### 4. The Fundamental Architectural Flaws of `Stack`

#### A. Violation of Liskov Substitution Principle (LSP)
Inheritance represents an **"IS-A"** relationship. A mathematical stack is *not* an arbitrary indexed list; it is a restricted sequence where insertions and removals are strictly permitted **only at the top**.
Because `Stack` extends `Vector`, it inherits:
- `add(int index, E element)`
- `remove(int index)`
- `insertElementAt(E obj, int index)`
- `set(int index, E element)`
Any client can call `stack.add(0, "AlienElement")` or `stack.remove(0)`, directly violating the LIFO invariant and corrupting stack algorithms.

#### B. The Strange 1-Based Offset in `search()`
`stack.search(Object o)` returns the **1-based position from the top of the stack**, where `1` represents the top element. This contradicts Java's universal 0-indexed convention used everywhere else in the language.

---

## 4. Complexity & Behavioral Matrix

| Operation | `Vector` | `Stack` | `ArrayList` | `ArrayDeque` (Modern Stack) |
| :--- | :--- | :--- | :--- | :--- |
| **Push / Append** | $O(1)$ amortized (synchronized) | $O(1)$ amortized (synchronized) | $O(1)$ amortized (unsynchronized) | $O(1)$ amortized (unsynchronized) |
| **Pop / Remove Tail** | $O(1)$ (synchronized) | $O(1)$ (synchronized) | $O(1)$ (unsynchronized) | $O(1)$ (unsynchronized) |
| **Peek Top** | $O(1)$ (synchronized) | $O(1)$ (synchronized) | $O(1)$ (unsynchronized) | $O(1)$ (unsynchronized) |
| **Get by Index** | $O(1)$ (synchronized) | $O(1)$ (synchronized) | $O(1)$ (unsynchronized) | Not Supported ($O(1)$ head/tail only) |
| **Insert at Head** | $O(n)$ (shift right + lock) | $O(n)$ (breaks LIFO!) | $O(n)$ (shift right) | $O(1)$ amortized (circular array) |
| **Expansion Factor** | **2.0x (Doubling)** | **2.0x (Doubling)** | **1.5x (50% Growth)** | **2.0x (Bitwise power-of-2)** |
| **Lazy Initialization**| **No** (Allocates 10 immediately) | **No** (Allocates 10 immediately) | **Yes** (0 length on creation) | **Yes** (Allocates 16 on first push) |
| **Synchronization** | Coarse-grained `synchronized` | Coarse-grained `synchronized` | None (Unsynchronized) | None (Unsynchronized) |

---

## 5. Concurrency & Thread-Safety Mechanics

### The Illusion of Thread Safety: The Compound Action Race Condition

The primary rationale cited for using `Vector` or `Stack` in legacy code is that *"they are thread-safe."*
In reality, **method-level synchronization does NOT protect compound (multi-step) actions!**

Consider a typical check-then-act pattern:
```java
// Thread 1 and Thread 2 executing simultaneously:
if (!stack.empty()) {
    // RACE WINDOW! Another thread can pop here!
    String item = stack.pop();
}
```

#### Race Execution Trace:
1. Stack has exactly 1 element.
2. **Thread 1** calls `stack.empty()`. It acquires lock, checks `size == 0` (false), releases lock, enters `if` body.
3. Context switch to **Thread 2**.
4. **Thread 2** calls `stack.empty()`. Acquires lock, checks `size == 0` (false), releases lock, enters `if` body.
5. **Thread 2** calls `stack.pop()`. Acquires lock, removes element, releases lock. Stack is now empty.
6. Context switch back to **Thread 1**.
7. **Thread 1** calls `stack.pop()`. Acquires lock, attempts to remove from empty stack $\rightarrow$ **`EmptyStackException` or `ArrayIndexOutOfBoundsException` crashes the application!**

To make compound operations safe on `Vector` or `Stack`, the client code **must wrap the entire compound block in an external lock on the collection instance**:
```java
synchronized (stack) {
    if (!stack.empty()) {
        String item = stack.pop();
    }
}
```
If external client-side synchronization is required anyway, the internal synchronization inside `Vector` is purely redundant, wasting CPU cycles on double-locking!

### Modern Replacements
1. **For General Sequential List:** Use `ArrayList`. If thread-safety is required:
   - Read-heavy: `CopyOnWriteArrayList`
   - High-concurrency queues: `ConcurrentLinkedQueue`
   - Synchronized wrapper: `Collections.synchronizedList(new ArrayList<>())`
2. **For LIFO Stack:** Use `ArrayDeque`:
   ```java
   Deque<String> stack = new ArrayDeque<>();
   stack.push("A");
   String top = stack.pop();
   ```
   `ArrayDeque` is circular-array backed, does not synchronize unnecessarily, and strictly encapsulates stack semantics without leaking index-based mutators.
3. **For Concurrent LIFO Stack:** Use `ConcurrentLinkedDeque` or `LinkedBlockingDeque`.

---

## 6. Production-Grade Java Code Verification

See the accompanying runnable class:
`com.raysi.jcf.list.vectorandstack.codes.LegacyStackPitfallsDemo`

Key points demonstrated in execution:
- How calling `stack.add(0, "ALIEN")` violates the LIFO contract.
- Benchmarking demonstrating the severe single-threaded lock acquisition tax of `Vector` vs `ArrayList`.
- Proper usage of `ArrayDeque` to enforce compile-time stack invariants.
- Compound action race hazards in multithreaded environments.

---

## 7. Top 7 High-Signal Interview Questions & Deep Answers

### Q1: Why is `java.util.Stack extends Vector` considered one of the worst object-oriented design violations in the Java Standard Library?
**Deep Answer:**
It is a classic violation of the **Liskov Substitution Principle (LSP)** and the principle of **"Composition over Inheritance"**.
In object-oriented design, inheritance implies an "IS-A" relationship: any operation permitted on the superclass must be semantically valid on the subclass. A mathematical stack is an abstract data type with strict access constraints: elements may only be inserted or extracted from the top ($O(1)$ LIFO).
Because `Stack` extends `Vector`, it publicly inherits over 40 list manipulation methods, such as `add(int index, E element)`, `remove(int index)`, `insertElementAt(E obj, int index)`, and `set(int index, E element)`. Any code receiving a `Stack` can insert or delete elements in the middle or bottom of the stack, completely breaking the LIFO invariant.
The correct design would have used **Composition**: an independent `Stack` class that encapsulates an internal private `ArrayDeque` or `ArrayList` and exposes only `push()`, `pop()`, `peek()`, and `size()`.

### Q2: Does the `synchronized` keyword on `Vector`'s methods guarantee thread-safe execution in multithreaded applications?
**Deep Answer:**
No. While individual method calls (such as `vector.get(i)` or `vector.add(e)`) are atomic, real-world business logic consists of **compound actions** (check-then-act, read-modify-write, or iteration).
For example:
```java
int lastIndex = vector.size() - 1;
Object obj = vector.get(lastIndex);
```
Between the execution of `vector.size()` and `vector.get()`, another thread can execute `vector.remove(lastIndex)`. The first thread then calls `get(lastIndex)` with an invalid index, throwing `ArrayIndexOutOfBoundsException`.
Similarly, iterating through a `Vector` via `for (Object o : vector)` or `Iterator` will throw `ConcurrentModificationException` if another thread structurally modifies the vector during traversal.
To ensure thread-safety for compound operations or iteration, caller code must manually acquire an external monitor lock over the entire sequence:
```java
synchronized (vector) {
    int lastIndex = vector.size() - 1;
    if (lastIndex >= 0) {
        Object obj = vector.get(lastIndex);
    }
}
```

### Q3: What is the technical difference in resizing strategy between `Vector` and `ArrayList`?
**Deep Answer:**
- **`Vector`:** Offers an explicit field `protected int capacityIncrement`. If `capacityIncrement > 0`, it grows by that exact amount. If `capacityIncrement <= 0` (the default), `Vector` doubles its capacity ($2.0\times$):
  `newCapacity = oldCapacity + oldCapacity;`
- **`ArrayList`:** Employs a strict 1.5x expansion rule using bitwise shift:
  `newCapacity = oldCapacity + (oldCapacity >> 1);`
- **Architectural Tradeoff:**
  A $2.0\times$ growth factor doubles the wasted slack memory upon each resize (e.g., resizing from 10M to 20M wastes up to 40MB of reference pointers). Furthermore, mathematically, when a buffer doubles, the newly allocated memory block is strictly larger than the sum of all previously deallocated blocks ($2^n > \sum_{i=0}^{n-1} 2^i = 2^n - 1$). The JVM/OS memory allocator can never reuse the freed buffers, leading to higher virtual memory fragmentation. A 1.5x factor allows memory allocator coalescing and drastically reduces unused heap slack.

### Q4: Why does `Stack.search(Object o)` return a 1-based index instead of a 0-based index?
**Deep Answer:**
`Stack.search(Object o)` is an artifact of pre-JCF design in JDK 1.0. It defines the return value as the **1-based distance from the top of the stack**.
- If the target object is at the very top of the stack, `search()` returns `1`.
- If it is the second item down, it returns `2`.
- If the object is not found, it returns `-1`.
Internally, the JDK implements this as:
```java
public synchronized int search(Object o) {
    int i = lastIndexOf(o);
    if (i >= 0) {
        return size() - i;
    }
    return -1;
}
```
This contradicts the standard zero-indexed convention used everywhere else in Java (`get(0)`, `indexOf()`, array indexing `a[0]`), creating confusion for developers who mistake `search()` for an index lookup.

### Q5: How does `Collections.synchronizedList(new ArrayList<>())` differ from `Vector`?
**Deep Answer:**
1. **Separation of Concerns:** `Vector` tightly couples the resizable array data structure with synchronization. `Collections.synchronizedList()` is a decorator pattern: it wraps any `List` implementation (e.g., `ArrayList`, `LinkedList`) and forwards calls inside `synchronized (mutex)` blocks.
2. **Custom Mutex Object:** In `Vector`, every method synchronizes on `this`. In `Collections.synchronizedList(list, mutex)`, you can pass a custom mutex object, allowing synchronization coordination across multiple related collections or domain components.
3. **Resizing Semantics:** `Collections.synchronizedList(new ArrayList<>())` inherits `ArrayList`'s 1.5x expansion factor, lazy array initialization, and cache-friendly layout, avoiding `Vector`'s 2.0x memory doubling and immediate array allocation.
4. **Iteration Requirement:** Both require explicit client-side synchronization during iteration to prevent `ConcurrentModificationException`.

### Q6: Why does modern Java strongly recommend `ArrayDeque` over `Stack`?
**Deep Answer:**
The official Javadoc for `Stack` explicitly states: *"A more complete and consistent set of LIFO stack operations is provided by the `Deque` interface and its implementations, which should be used in preference to this class. For example: `Deque<Integer> stack = new ArrayDeque<Integer>();`"*.
Reasons:
1. **Performance:** `ArrayDeque` is unsynchronized. It eliminates all monitor lock overhead on push/pop/peek.
2. **Encapsulation:** `ArrayDeque` implements `Deque`, which does not expose positional index methods (`add(index, e)`). It is impossible to inadvertently insert into the middle or bottom of the stack.
3. **Memory Efficiency:** `ArrayDeque` uses a circular array buffer that wraps head and tail pointers. It does not shift elements in memory when operating as a stack or queue.
4. **No Legacy baggage:** It has no obsolete methods like `elements()`, `capacityIncrement`, or 1-based `search()`.

### Q7: What is Lock Elision, and why does an unsynchronized `ArrayList` still beat a `Vector` even when the JIT compiler eliminates locks?
**Deep Answer:**
Modern HotSpot C2 JIT compilers perform **Escape Analysis** (JEP 99). If the compiler can prove that a `Vector` instance is confined entirely within a single method and its reference never "escapes" to other threads or the heap, the JIT performs **Lock Elision**—it strips out the `monitorenter` and `monitorexit` bytecode instructions entirely at runtime.
However, `ArrayList` still outperforms `Vector` because:
1. **Escape Analysis Limitations:** If the `Vector` is passed to another non-inlined method, stored in a field, returned from a method, or if the method exceeds JIT inlining thresholds, the compiler must assume it might escape and **cannot** elide the locks.
2. **Mark Word Contention:** Even when locks are uncontended, modern JDKs (JDK 15+) have disabled/deprecated Biased Locking (JEP 374). Uncontended locks still require atomic CAS instructions (like `LOCK CMPXCHG` on x86) to acquire and release lightweight locks, which flush store buffers and stall CPU instruction pipelines.
3. **Array Allocation:** `Vector` allocates `Object[10]` eagerly in its constructor, whereas `ArrayList` initializes lazily to a shared empty array.
