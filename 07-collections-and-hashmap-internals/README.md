# 07. Java Collections Framework & `HashMap` Internals

> **Consolidated Supermodule:** Combines the full **Java Collections Framework (JCF)** curriculum (`List`, `Queue`, `Set`, `Map`, `Concurrent` collections) with the **HashMap Deep-Dive Lab** (custom `MyHashMap` built from scratch + collision treeification proofs).

---

## ⚡ 2-Minute Rapid Revision Card

- **`Map` is NOT a `Collection`:** `Iterable -> Collection -> {List, Queue/Deque, Set}`; `Map<K,V>` sits in its own hierarchy because it maps pairs rather than individual elements.
- **`ArrayList` vs `LinkedList`:** Use `ArrayList` 99% of the time ($O(1)$ random access, contiguous array cache locality, $1.5\times$ growth). `LinkedList` burns 24B+ per node and causes L1/L2 cache misses.
- **Stack & Queue Winner:** **NEVER use legacy `Stack` or `Vector`** (coarse `synchronized` overhead). Use **`ArrayDeque`** (power-of-two circular bitwise ring buffer, $O(1)$ amortized ends, no `null` allowed).
- **`HashMap` Numbers to Memorize:** Default capacity `16` (always power of 2), load factor `0.75f` (Poisson optimal), `TREEIFY_THRESHOLD = 8`, `UNTREEIFY_THRESHOLD = 6` (hysteresis), `MIN_TREEIFY_CAPACITY = 64`. Index = `(n - 1) & (h ^ (h >>> 16))`.
- **`ConcurrentHashMap` (Java 8+):** No `Segment` locks! Uses **CAS** on empty bucket slots and fine-grained **`synchronized(bucketHeadNode)`** on collisions + `volatile` reads. Disallows `null` keys & values.
- **Java 21+ Sequenced Collections:** `.getFirst()`, `.getLast()`, `.reversed()` unified across `List`, `Deque`, `LinkedHashSet`, `SortedSet`, and `LinkedHashMap`/`TreeMap` (`SequencedMap`).

---

## 🧭 Progressive Learning Path

- **🟢 Layer 1 (Noob):** Read [`notes/00-beginner-collections-selector.md`](notes/00-beginner-collections-selector.md) and run `collections.beginner.BeginnerCollectionsDemo` to learn which collection to pick in 30 seconds.
- **🟡 Layer 2 (Practitioner):** Explore [`notes/foundations/`](notes/foundations/), [`notes/list/`](notes/list/), [`notes/queue/`](notes/queue/), [`notes/set/`](notes/set/), [`notes/map/`](notes/map/), and run `SequencedCollectionsDemo`.
- **🔴 Layer 3 (Senior / Internals):** Dive into [`notes/map/hashmap_deep_dive/`](notes/map/hashmap_deep_dive/), inspect our from-scratch [`MyHashMap.java`](codes/collections/hashmap_deep_dive/custom/MyHashMap.java), and benchmark `ConcurrentHashMap` CAS vs `HashMap` data loss.
- **🎯 Interview:** [`interview/INTERVIEW_QUESTIONS.md`](interview/INTERVIEW_QUESTIONS.md) — 40+ JCF & HashMap questions.

---

## 🚀 Compile & Run

```bash
javac -d out $(find 07-collections-and-hashmap-internals/codes -name "*.java")

# Beginner & Modern Sequenced Collections
java -cp out collections.beginner.BeginnerCollectionsDemo
java -cp out collections.beginner.SequencedCollectionsDemo

# Custom HashMap from scratch & Deep Internals
java -cp out collections.hashmap_deep_dive.custom.MyHashMapTest
java -cp out collections.hashmap_deep_dive.demo.InternalMechanicsDemo
java -cp out collections.map.hashmap.HashMapCollisionAndTreeifyDemo
java -cp out collections.map.linkedhashmap.LRUCacheImplementation
java -cp out collections.map.concurrenthashmap.ConcurrentHashMapCASAndLockingDemo
```
