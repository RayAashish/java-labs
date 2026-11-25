# Java 21 — Sequenced Collections (JEP 431)

## ⚡ 2-Minute Revision
- **Problem Solved:** Before Java 21, Java had NO unified API to get the first/last element or iterate backward consistently:
  - `List`: `list.get(list.size() - 1)`
  - `Deque`: `deque.getLast()`
  - `SortedSet`: `set.last()`
  - `LinkedHashSet`: **no direct `last()` at all — had to iterate the entire set!**
- **New Interfaces (Inserted Cleanly Into Collections Hierarchy):**
  - `SequencedCollection<E>` (`List`, `Deque`, `SequencedSet`): adds `addFirst`, `addLast`, `getFirst`, `getLast`, `removeFirst`, `removeLast`, **`reversed()`**.
  - `SequencedSet<E>` (`LinkedHashSet`, `SortedSet` / `TreeSet`): repositioning semantics if `addFirst`/`addLast` adds an existing element (`LinkedHashSet` moves it!).
  - `SequencedMap<K, V>` (`LinkedHashMap`, `SortedMap` / `TreeMap`): `firstEntry`, `lastEntry`, `pollFirstEntry`, `pollLastEntry`, `putFirst`, `putLast`, `reversed()`, `sequencedKeySet()`.
- **Crucial Property of `reversed()`:** Returns an **$O(1)$ live reverse-ordered VIEW** — zero copying, and mutating the reversed view mutates the original collection! (And `Collections.unmodifiableSequencedCollection/Set/Map` preserves safe read-only views.)

## 📂 Contents
- [`notes/sequenced-collections-deep-dive.md`](notes/sequenced-collections-deep-dive.md)
- [`codes/lts/java21/sequenced/SequencedCollectionsDeepDiveDemo.java`](codes/lts/java21/sequenced/SequencedCollectionsDeepDiveDemo.java)
- [`interview/INTERVIEW_QUESTIONS.md`](interview/INTERVIEW_QUESTIONS.md)
