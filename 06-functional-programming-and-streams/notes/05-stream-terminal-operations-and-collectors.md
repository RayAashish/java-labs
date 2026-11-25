# 05. Stream Terminal Operations & `Collectors` Mastery

A terminal operation consumes the Stream and produces a final value, collection, or side effect.

---

## 1. Complete Terminal Operations Reference

| Method | Return Type | Short-Circuit? | Purpose & Notes |
|---|---|---|---|
| **`forEach(Consumer)`** | `void` | No | Side-effect iteration (**does NOT guarantee order** on parallel streams!) |
| **`forEachOrdered(Consumer)`** | `void` | No | Preserves encounter order even in parallel streams (slower) |
| **`toList()`** *(Java 16+)* | `List<T>` | No | Unmodifiable list, **allows `null`**, concise replacement for `collect(toList())` |
| **`toArray()` / `toArray(String[]::new)`** | `Object[]` / `T[]` | No | Always pass `Type[]::new` for typed arrays |
| **`count()`** | `long` | No | Returns element count ($O(1)$ on `SIZED` sources if no filtering) |
| **`min(Comparator)` / `max(Comparator)`** | `Optional<T>` | No | Empty `Optional` if stream is empty |
| **`anyMatch` / `allMatch` / `noneMatch`** | `boolean` | **YES** | Stops as soon as outcome is decided (Watch out: `allMatch` on empty stream = **`true`** vacuously!) |
| **`findFirst()` / `findAny()`** | `Optional<T>` | **YES** | `findAny()` is faster on parallel streams (no encounter-order lock) |
| **`reduce(...)`** | `Optional<T>` / `T` / `U` | No | Folds elements into a single immutable result (see 3 overloads below!) |
| **`collect(Collector)`** | `R` | No | Mutable reduction into Lists, Sets, Maps, Strings, Statistics |

---

## 2. Mastering All 3 `reduce()` Overloads

1. **1-Arg `reduce(BinaryOperator<T> accumulator)` -> returns `Optional<T>`:**
   No identity value, so empty stream returns `Optional.empty()`.
   ```java
   Optional<Integer> sum = List.of(1, 2, 3).stream().reduce(Integer::sum);
   ```
2. **2-Arg `reduce(T identity, BinaryOperator<T> accumulator)` -> returns `T`:**
   `identity` MUST be a true associative identity (`0` for `+`, `1` for `*`, `""` for concat).
   ```java
   int product = List.of(2, 3, 4).stream().reduce(1, (a, b) -> a * b); // 24
   ```
3. **3-Arg `reduce(U identity, BiFunction<U,T,U> accumulator, BinaryOperator<U> combiner)`:**
   Allows accumulating **`T` into a different result type `U`** (e.g., summing `Employee.salary()` into `Double`) AND supplies `combiner` to merge partial results across **parallel stream** threads!
   ```java
   double totalSalary = employees.parallelStream()
           .reduce(0.0, (acc, emp) -> acc + emp.salary(), Double::sum);
   ```

---

## 3. Complete `Collectors` Mastery Matrix

| Collector | What It Produces | Critical Gotchas & Best Practices |
|---|---|---|
| `toList()` / `toUnmodifiableList()` | `List<T>` | `toUnmodifiableList()` (Java 10+) throws NPE on `null`; `Stream.toList()` allows `null`! |
| `toSet()` / `toUnmodifiableSet()` | `Set<T>` | Unordered (`HashSet`); use `toCollection(LinkedHashSet::new)` to keep order |
| `toCollection(TreeSet::new)` | Custom `Collection` | Control exact concrete backing implementation |
| `toMap(keyFn, valFn)` | `Map<K,V>` | **CRASHES (`IllegalStateException`) on duplicate keys!** |
| `toMap(k, v, mergeFn, mapSupplier)` | `Map<K,V>` | Pass `(oldV, newV) -> newV` + e.g. `LinkedHashMap::new`! Also crashes if `valFn` returns `null`! |
| `joining(", ", "[", "]")` | `String` | Clean delimiter + prefix + suffix in 1 pass |
| `counting()` / `summingInt` / `averagingDouble` | Number | Downstream aggregators inside `groupingBy` |
| `summarizingInt/Long/Double(fn)` | `IntSummaryStatistics` | Single-pass `count`, `sum`, `min`, `max`, `average`! |
| `groupingBy(classifier)` | `Map<K, List<T>>` | **3-arg overload** `groupingBy(k, TreeMap::new, downstream)` sorts groups! |
| `partitioningBy(predicate)` | `Map<Boolean, List<T>>` | Always contains BOTH `true` and `false` keys (even if one list is empty!) |
| `mapping(fn, downstream)` | Transformed downstream | `groupingBy(Emp::dept, mapping(Emp::name, toList()))` |
| `flatMapping(fn, downstream)` *(Java 9)* | Flattened downstream | `groupingBy(Order::customer, flatMapping(o -> o.items().stream(), toSet()))` |
| `filtering(pred, downstream)` *(Java 9)* | Filtered group buckets | Keeps empty groups (unlike calling `.filter()` before `groupingBy`!) |
| `collectingAndThen(collector, finisher)` | Post-transformed result | e.g., `collectingAndThen(toList(), Collections::unmodifiableList)` |
| `teeing(c1, c2, merger)` *(Java 12)* | Combined dual aggregate | Runs **two collectors simultaneously in 1 stream pass** (e.g., min + max)! |
