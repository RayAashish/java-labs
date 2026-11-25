# 03. Exhaustive `Collectors`, Primitive Streams & Parallel Streams

---

## 1. Complete `java.util.stream.Collectors` Catalog

### Basic Collection Builders
- `Collectors.toList()` -> mutable `ArrayList` (unspecified type, allows `null` elements).
- `Collectors.toUnmodifiableList()` *(Java 10)* -> unmodifiable `List`, rejects `null`s (`NullPointerException`).
- `Stream.toList()` *(Java 16)* -> shortest syntax, unmodifiable `List`, allows `null` elements!
- `Collectors.toSet()` / `Collectors.toUnmodifiableSet()`
- `Collectors.toCollection(TreeSet::new)` -> control exact backing implementation (`LinkedHashSet`, `ArrayDeque`, etc.).

### Map Builders (`toMap` — Watch for Traps!)
1. **2-arg `toMap(keyMapper, valMapper)`:** **THROWS `IllegalStateException: Duplicate key` on duplicates AND NPE on `null` value!**
2. **3-arg `toMap(keyMapper, valMapper, mergeFn)`:** Safe merge function for duplicate keys: `(oldV, newV) -> oldV` or `BigDecimal::add`.
3. **4-arg `toMap(keyMapper, valMapper, mergeFn, mapFactory)`:** Choose map class (`LinkedHashMap::new` to preserve order, `TreeMap::new` for sorted keys, `EnumMap::new`).

### Grouping & Partitioning
- `groupingBy(classifier)` -> `Map<K, List<T>>` (note: throws NPE if classifier returns `null`! Replace `null` with sentinel string).
- `groupingBy(classifier, downstreamCollector)` -> e.g.:
  - `groupingBy(Order::region, counting())`
  - `groupingBy(Order::region, summingDouble(Order::amount))`
  - `groupingBy(Order::region, mapping(Order::id, toList()))`
  - `groupingBy(Order::region, filtering(o -> o.amount() > 100, toList()))` *(Java 9 — preserves empty region buckets!)*
- `groupingBy(classifier, mapFactory, downstreamCollector)` -> custom `TreeMap::new`.
- `partitioningBy(predicate)` -> always produces `Map<Boolean, List<T>>` with **both `true` and `false` keys pre-populated**, even if one side is empty!

### Reduction, Strings & Chaining
- `joining()`, `joining(", ")`, `joining(", ", "[", "]")`
- `counting()`, `summingInt/Long/Double`, `averagingInt/Long/Double`, `summarizingDouble` (one pass gives count + sum + min + avg + max!)
- `maxBy(comp)` / `minBy(comp)`
- `collectingAndThen(downstream, finisher)` -> e.g. wrap result into immutable collection or unwrap `Optional`.
- `teeing(c1, c2, merger)` *(Java 12)* -> compute TWO independent collectors in a **single pass** (e.g. `min` and `max` simultaneously, or `sum / count`)!

### Writing a Custom Collector: `Collector<T, A, R>`
A collector consists of 4 functions (+ optional `Characteristics`):
1. `supplier()`: creates fresh mutable container `A` (`StringBuilder::new`).
2. `accumulator()`: folds one element `T` into `A`.
3. `combiner()`: merges two partial containers `(a1, a2) -> a1` for parallel execution.
4. `finisher()`: converts `A` -> final result `R` (or `IDENTITY_FINISH`).

---

## 2. Primitive Streams (`IntStream`, `LongStream`, `DoubleStream`)

Using `Stream<Integer>` for numeric math allocates wrapper `Integer` objects (16 bytes each + pointer indirection!).
- Convert object stream -> primitive stream: `mapToInt`, `mapToLong`, `mapToDouble`, `flatMapToInt`.
- Convert primitive stream -> object stream: `.boxed()` or `.mapToObj(...)`.
- Extra primitive-only terminal ops: `.sum()`, `.average()`, `.summaryStatistics()` (`min`, `max`, `sum`, `count`, `average` in 1 pass!).

---

## 3. Parallel Streams (`parallelStream()`): Architecture & 5 Golden Traps

Under the hood, parallel streams use **one global pool**: `ForkJoinPool.commonPool()` (default threads = `Runtime.getRuntime().availableProcessors() - 1`).

### 5 Traps Every Senior Developer Knows
1. **Shared Mutable State Race Condition (Data Corruption):**
   - **BAD:** `List<Integer> out = new ArrayList<>(); nums.parallelStream().forEach(out::add);` -> throws `ArrayIndexOutOfBoundsException` or loses items!
   - **GOOD:** Always use `.collect(...)` or `.toList()`.
2. **Thread-Pool Starvation via Blocking I/O:**
   - **BAD:** Running DB/HTTP calls inside `parallelStream()` hijacks `commonPool()`, starving EVERY other parallel stream and `CompletableFuture.supplyAsync()` across the entire microservice!
3. **Poor `Spliterator` Data Structures (`LinkedList`):**
   - `ArrayList`, arrays, `IntStream.range` split in **$O(1)$**. `LinkedList` and `Stream.iterate` split poorly ($O(N)$), running slower in parallel than sequentially!
4. **Small N or Trivial Operations:**
   - **Brian Goetz's $N \times Q$ rule:** Parallel only beats sequential when `(Number of Elements N) * (CPU cost per element Q) > ~10,000+` enough to amortize fork/join task overhead.
5. **Custom Pool Isolation (when genuinely needed for CPU tasks):**
   `customForkJoinPool.submit(() -> data.parallelStream()...collect(...)).get();` (note: JVM still executes `ForkJoinTask` on the enclosing pool if called from within a `ForkJoinWorkerThread`).
