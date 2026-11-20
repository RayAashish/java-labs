# Complete Guide to Java HashMap: Architecture, Internals, and Mastery

A comprehensive, interview-grade masterclass on Java's `java.util.HashMap`. This guide covers everything from core architectural theory, bitwise math, hash functions, and resizing mechanics, to treeification, thread-safety pitfalls, comparative analysis, and high-frequency interview questions.

---

## 📚 Table of Contents

1. [Architectural Overview & Data Layout](01_hashmap_architecture_and_internals.md)
   - Core Design Philosophy & Time Complexities
   - Memory Structure: `Node<K,V>[] table`, Buckets & TreeNodes
   - Key Constants (`DEFAULT_INITIAL_CAPACITY`, `LOAD_FACTOR`, `TREEIFY_THRESHOLD`, etc.)
   - Hash Function & Perturbation (`(h = key.hashCode()) ^ (h >>> 16)`)
   - Bucket Index Math (`(n - 1) & hash`) & Power-of-Two Table Sizing
   - Why 0.75 Load Factor? (Poisson Distribution Proof)

2. [The Lifecycle of Put, Get, and Resize](02_put_get_resize_lifecycle.md)
   - Step-by-Step Flow of `put(K key, V value)`
   - Null Key Handling (Bucket 0)
   - Step-by-Step Flow of `get(Object key)`
   - Resizing & Rehashing Deep Dive
   - Java 8 High-Bit Partition Optimization (`(e.hash & oldCap) == 0`)
   - Order Preservation: Java 7 Head Insertion vs. Java 8 Tail Insertion

3. [Treeification & Collision Resolution](03_treeification_and_collision_handling.md)
   - Collision Strategies: Separate Chaining vs. Open Addressing
   - Red-Black Tree Transformation (`TREEIFY_THRESHOLD = 8` & `MIN_TREEIFY_CAPACITY = 64`)
   - Untreeification (`UNTREEIFY_THRESHOLD = 6`) and Hysteresis
   - Anatomy of `TreeNode<K,V>`
   - Total Ordering in Red-Black Trees (`Comparable` & `tieBreakOrder`)

4. [The hashCode() & equals() Contract & Immutability](04_hashcode_equals_contract_and_immutability.md)
   - Object Contract Definition & Mathematical Axioms
   - The 4 Behavioral Scenarios (Both overridden, one overridden, neither overridden)
   - The Mutable Key Hazard & Memory Leaks
   - Why `String` and Java Records are the Ideal Map Keys

5. [Concurrency, Fail-Fast Iterators, & Map Alternatives](05_concurrency_failfast_and_alternatives.md)
   - Why `HashMap` is Not Thread-Safe
   - The Java 7 Infinite Loop Bug (Circular Linked List)
   - Java 8 Silent Data Loss & Tree Corruption
   - Fail-Fast Iterators & `ConcurrentModificationException` (`modCount`)
   - Detailed Comparison Matrix:
     - `HashMap` vs. `Hashtable`
     - `HashMap` vs. `Collections.synchronizedMap`
     - `HashMap` vs. `ConcurrentHashMap` (Java 7 Segments vs. Java 8 CAS + Synchronized)
     - `HashMap` vs. `LinkedHashMap` (Insertion Order & LRU Cache)
     - `HashMap` vs. `TreeMap` (Natural Sorting & `NavigableMap`)
     - `HashMap` vs. `IdentityHashMap` & `WeakHashMap`

6. [Top 30 High-Frequency Interview Questions & Answers](06_top_interview_questions_and_answers.md)
   - Curated Q&A categorized into Basic, Intermediate, Advanced, and Edge-Case questions.

---

## ⚡ Quick Reference Summary

| Parameter / Feature | Value / Behavior | Explanation / Reason |
| :--- | :--- | :--- |
| **Default Initial Capacity** | `16` (`1 << 4`) | Must be a power of 2 to enable bitwise mask indexing. |
| **Maximum Capacity** | `1 << 30` ($1,073,741,824$) | Largest power of 2 representable in a 32-bit signed int array. |
| **Default Load Factor** | `0.75f` | Optimal balance between time complexity and space overhead. |
| **Treeification Threshold** | `8` | Bucket depth at which linked list converts to Red-Black tree. |
| **Untreeification Threshold**| `6` | Bucket depth at which Red-Black tree converts back to linked list. |
| **Min Treeify Capacity** | `64` | Minimum table capacity required before treeifying a bucket. |
| **Index Calculation** | `(n - 1) & hash` | Replaces expensive `%` with 1-cycle bitwise AND. |
| **Hash Spreading** | `h ^ (h >>> 16)` | XORs upper 16 bits into lower 16 bits to prevent small-table collisions. |
| **Average Time Complexity**| $O(1)$ | Lookup, Insert, Delete with uniform hash distribution. |
| **Worst-Case Time (Java 8)**| $O(\log n)$ | When all keys collide, Red-Black tree bounds search time. |
| **Worst-Case Time (Java 7)**| $O(n)$ | When all keys collide in a singly linked list. |
| **Null Key Support** | Yes (1 null key at index 0)| Hash is hardcoded to `0`. |
| **Thread Safety** | No | Unsynchronized; use `ConcurrentHashMap` for concurrent environments. |

---

## 🧩 Architectural Diagram

```
HashMap Table (Node<K, V>[] table)
   Index
  +-----+
0 |  *  | ---> [ Node: "Apple" | hash: 65123 | val: 10 ] -> null
  +-----+
1 | null|
  +-----+
2 |  *  | ---> [ Node: "Banana" ] -> [ Node: "Cherry" ] -> [ Node: "Date" ] -> null
  +-----+      (Collision resolved via Singly Linked List)
3 | null|
  +-----+
4 |  *  | ---> [ TreeNode: "Root" (Black) ]
  +-----+             /                \
5 | null|     [ TreeNode (Red) ]   [ TreeNode (Black) ]
  +-----+      (Treeified bucket when chain >= 8 and table >= 64)
...
15| null|
  +-----+
```

---

## 💻 Accompanying Code Examples

All documented code implementations are available in the [`codes/`](../codes/) directory:
- [`codes/custom/MyHashMap.java`](../codes/custom/MyHashMap.java): Full custom implementation of `HashMap` from scratch with generic types, bucket chaining, resizing, perturbation hash, and standard map operations.
- [`codes/custom/MyHashMapTest.java`](../codes/custom/MyHashMapTest.java): Comprehensive test suite verifying correctness, collisions, resizing, and edge cases.
- [`codes/demo/InternalMechanicsDemo.java`](../codes/demo/InternalMechanicsDemo.java): Interactive visualization of hash spreading, bucket index calculations, and dynamic rehashing.
- [`codes/demo/HashCodeEqualsContractDemo.java`](../codes/demo/HashCodeEqualsContractDemo.java): Concrete demonstration of contract violations and the danger of mutable keys.
- [`codes/demo/ConcurrencyIssueDemo.java`](../codes/demo/ConcurrencyIssueDemo.java): Multithreaded simulation demonstrating data loss in `HashMap` vs safety in `ConcurrentHashMap`.
- [`codes/demo/MapImplementationsComparisonDemo.java`](../codes/demo/MapImplementationsComparisonDemo.java): Practical comparison of `HashMap`, `LinkedHashMap` (LRU cache), `TreeMap`, and `IdentityHashMap`.
