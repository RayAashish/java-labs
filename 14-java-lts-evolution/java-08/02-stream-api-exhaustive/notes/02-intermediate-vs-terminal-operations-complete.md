# 02. Every Intermediate & Terminal Operation Explained

---

## 1. All Intermediate Operations (Return `Stream<T>`, Always Lazy)

| Method | Kind | Short-Circuit? | Signature & Purpose | Senior Trap / Note |
|---|---|---|---|---|
| `filter(Predicate)` | Stateless | No | Keeps elements where predicate returns `true` | Clears `SIZED` flag (downstream no longer knows output count) |
| `map(Function<T, R>)` | Stateless | No | 1-to-1 transformation (`T -> R`) | Preserves `SIZED`; use `mapToInt`/`mapToLong`/`mapToDouble` for primitives |
| `flatMap(Function<T, Stream<R>>)` | Stateless | No | 1-to-Many flattening (`Order -> Stream<LineItem>`) | Clears `SIZED`; returning `null` yields empty behavior cleanly vs NPE depending on flatMap; since Java 16 **`mapMulti`** avoids creating tiny inner `Stream` objects! |
| `distinct()` | **Stateful** | No | Removes duplicates using `Object.equals`/`hashCode` | Uses `LinkedHashSet` if `ORDERED`, otherwise `ConcurrentHashMap`; holds seen elements in RAM! |
| `sorted()` / `sorted(Comparator)` | **Stateful Barrier** | No | Sorts elements | **Full Barrier!** Buffers *all* elements into an array (`Arrays.parallelSort` if parallel) before passing element #1 downstream. Never put after an infinite stream without `.limit()` first! |
| `peek(Consumer)` | Stateless | No | Debugging / logging hook as elements flow | Designed for debugging — **do NOT put critical business side effects in `peek`** (e.g. `.count()` in Java 9+ elides `peek` completely when size is `SIZED`!). |
| `limit(long n)` | **Stateful** | **YES** | Truncates stream to at most `n` elements | Massive speedup with `findFirst`/unsorted sources; on **parallel ordered streams** can allocate buffers across threads! |
| `skip(long n)` | **Stateful** | No | Discards first `n` elements | On parallel ordered streams, coordinates slice index across chunks |
| `takeWhile(Predicate)` *(Java 9)* | Stateful | **YES** | Emits while true, **stops permanently at first false** | Works like `break` (unlike `filter` which checks all elements) |
| `dropWhile(Predicate)` *(Java 9)* | Stateful | No | Drops while true, then emits ALL remaining | Useful for skipping sorted log headers |

---

## 2. `map` vs `flatMap` — Crystal-Clear Visual Comparison

Suppose `orders` has 2 orders with `[item1, item2]` and `[item3]`:
- `orders.stream().map(Order::getItems)` -> produces `Stream<List<Item>>` (still nested: `[[i1, i2], [i3]]`)!
- `orders.stream().flatMap(o -> o.getItems().stream())` -> **flattens** into `Stream<Item>` (`[i1, i2, i3]`).

---

## 3. All Terminal Operations (Trigger Execution, Return Non-Stream / `void`)

| Method | Return Type | Short-Circuit? | Key Behavior & Gotcha |
|---|---|---|---|
| `forEach(Consumer)` | `void` | No | On **parallel streams**, encounter order is **not guaranteed** |
| `forEachOrdered(Consumer)` | `void` | No | Forces strict encounter order even in parallel (hurts throughput) |
| `toArray()` / `toArray(IntFunction)` | `Object[]` / `A[]` | No | Always prefer `stream.toArray(String[]::new)` |
| `collect(Collector)` / 3-arg `collect` | `R` (List/Map/etc.) | No | **Mutable reduction** into containers (`Collectors.*`) |
| `reduce(...)` (3 overloads!) | `Optional<T>` / `T` / `U` | No | **Immutable fold reduction** combining values (never mutate accumulator!) |
| `count()` | `long` | No | In Java 9+, if stream is `SIZED` and has no filtering ops, returns size in **$O(1)$** and **skips `peek()`/`map()`**! |
| `min(Comparator)` / `max(Comparator)` | `Optional<T>` | No | Returns empty `Optional` on empty stream |
| `anyMatch(Predicate)` | `boolean` | **YES** | Stops as soon as **first** match found (`false` on empty stream) |
| `allMatch(Predicate)` | `boolean` | **YES** | Stops on first `false` (**vacuously `true` on empty stream!**) |
| `noneMatch(Predicate)` | `boolean` | **YES** | Stops on first `true` (**vacuously `true` on empty stream!**) |
| `findFirst()` | `Optional<T>` | **YES** | Deterministic first element (even in parallel — extra sync cost) |
| `findAny()` | `Optional<T>` | **YES** | Any matching element — **faster than `findFirst()` in parallel streams** |

---

## 4. The 3 `reduce()` Overloads Explained (High Interview Frequency!)

1. **`Optional<T> reduce(BinaryOperator<T> accumulator)`**
   - No identity -> returns `Optional<T>` (empty stream yields `Optional.empty()`).
   - Example: `nums.stream().reduce(Integer::sum)`
2. **`T reduce(T identity, BinaryOperator<T> accumulator)`**
   - Starts with `identity` (`0` for sum, `1` for product) -> always returns non-null `T` (`identity` on empty stream).
   - **Rule:** `identity` MUST be a mathematical identity (`op(identity, x) == x`) AND `accumulator` MUST be **associative** (`(a + b) + c == a + (b + c)`). Subtraction `0 - x` breaks in parallel!
3. **`<U> U reduce(U identity, BiFunction<U, ? super T, U> accumulator, BinaryOperator<U> combiner)`**
   - Used when input type `T` (`User`) differs from accumulated result type `U` (`Integer` total length/score).
   - **GOTCHA:** On a **sequential** stream, `combiner` is **never called**! On a **parallel** stream, each worker accumulates a partial `U` using `identity + accumulator`, then `combiner` merges partial `U`s across threads!
