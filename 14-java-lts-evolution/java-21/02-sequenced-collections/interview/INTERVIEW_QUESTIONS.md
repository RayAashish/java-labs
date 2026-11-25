# Interview Questions — Java 21 Sequenced Collections (JEP 431)

## 🟢 Beginner

### Q1. Why did Java 21 introduce `SequencedCollection`, `SequencedSet`, and `SequencedMap`?
Even ordered collections previously lacked uniform methods to get/add/remove first & last elements or walk backward (`list.get(size()-1)` vs `deque.getLast()` vs `set.last()`, and `LinkedHashSet` had no direct last/reverse API!). JEP 431 unifies all encounter-ordered collections cleanly.

---

## 🟡 Intermediate

### Q2. Does `.reversed()` create a reversed copy of the collection?
**No!** `reversed()` returns an **$O(1)$ live reverse view** backed by the original collection. Changes made to the original appear immediately in the reversed view, and structural edits on the reversed view (`reversed.addFirst(x)`) modify the underlying collection (`original.addLast(x)`).

### Q3. What happens if you call `addFirst("X")` on a `LinkedHashSet` vs on a `TreeSet`?
- **`LinkedHashSet`:** Succeeds! If `"X"` was already in the set, it **repositions `"X"` to the front**!
- **`TreeSet` (`SortedSet`):** Throws **`UnsupportedOperationException`**, because insertion order in a `TreeSet` is governed strictly by element sorting (`Comparator` / `Comparable`), not caller placement!

---

## 🔴 Senior

### Q4. Can you modify a map value via `sequencedMap.firstEntry().setValue(99)`?
**No** in standard `SequencedMap` implementations (like `LinkedHashMap` / `TreeMap` snapshots) — `firstEntry()` / `lastEntry()` return unmodifiable entry snapshots whose `setValue(...)` throws `UnsupportedOperationException` to preserve structural integrity!
