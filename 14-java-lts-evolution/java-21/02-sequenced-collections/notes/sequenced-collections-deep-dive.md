# Sequenced Collections (JEP 431) — Deep Dive

---

## 🟢 Layer 1 — Noob Track: Before vs After Chaos

| Collection Type | Old Last Element Access | Old Reverse Iteration | Java 21 JEP 431 Unified |
|---|---|---|---|
| `ArrayList` | `list.get(list.size() - 1)` | `listIterator(size())` | `list.getLast()`, `list.reversed()` |
| `ArrayDeque` | `deque.getLast()` | `descendingIterator()` | `deque.getLast()`, `deque.reversed()` |
| `TreeSet` | `set.last()` | `descendingSet()` | `set.getLast()`, `set.reversed()` |
| `LinkedHashSet` | Iterate whole set! $O(N)$ | Copy to List & reverse! | `set.getLast()`, `set.reversed()` ($O(1)$!) |

---

## 🟡 Layer 2 — Practitioner Track: Hierarchy, `LinkedHashMap` & View Traps

### 1. Hierarchy Integration
- `SequencedCollection<E> extends Collection<E>`
- `List<E> extends SequencedCollection<E>`
- `Deque<E> extends SequencedCollection<E>`
- `SequencedSet<E> extends SequencedCollection<E>, Set<E>` (`LinkedHashSet`, `SortedSet` -> `NavigableSet`)
- `SequencedMap<K, V> extends Map<K, V>` (`LinkedHashMap`, `SortedMap` -> `NavigableMap`)

### 2. Subtle Behavior Traps Every Developer Must Know!
1. **`UnsupportedOperationException` on `TreeSet` / `TreeMap` `addFirst` / `putFirst`:**
   - Sorted collections determine order strictly by `Comparable`/`Comparator`! Calling `treeSet.addFirst("z")` throws `UnsupportedOperationException`, whereas `LinkedHashSet.addFirst("z")` succeeds and **moves `"z"` to the front** if already present!
2. **`List.of().getLast()` & Unmodifiable Lists:**
   - Calling `getFirst()`/`getLast()` works great on immutable `List.of(...)` — while calling `getFirst()` on an **empty** collection throws **`NoSuchElementException`** (cleaner than `IndexOutOfBoundsException`!).
3. **`reversed()` Is a LIVE View (Not a Snapshot Copy!):**
   - `var rev = list.reversed(); rev.addFirst("NEW");` appends `"NEW"` to the **end** of the original `list`!

---

## 🔴 Layer 3 — Senior / JVM Track

- `Collections.unmodifiableSequencedCollection(...)`, `unmodifiableSequencedSet(...)`, and `unmodifiableSequencedMap(...)` were added so exposing read-only ordered collections doesn't degrade back to plain unordered `Collection`!
- **`LinkedHashMap` `putFirst` / `putLast` Power-Up:** Building custom LRU eviction or pinned-header caches becomes clean 1-line operations (`cache.putFirst(key, val)` promotes key cleanly without `remove + put`!).
- **Entry Immutability on `SequencedMap.firstEntry()`:** Entries returned by `firstEntry()` / `lastEntry()` are **unmodifiable snapshots** (`entry.setValue(...)` throws `UnsupportedOperationException` to protect underlying map structure invariants, especially sorted maps!).
