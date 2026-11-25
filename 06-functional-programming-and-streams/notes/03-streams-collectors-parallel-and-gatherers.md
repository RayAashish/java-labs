# 03. Streams API, Collectors, Parallel Traps & Java 25 Gatherers

---

## 🟢 Layer 1 — Noob Track: Assembly Line Processing

Think of a **Collection** (`List`, `Set`) as a **storage warehouse**, and a **Stream** as a **conveyor belt**:
- A Stream **does not store data** and **never mutates** the original list.
- You attach steps to the belt (**intermediate operations**: `filter`, `map`, `sorted`, `distinct`, `limit`).
- Nothing actually moves until you press the **Start button** (**terminal operation**: `toList()`, `forEach()`, `count()`, `reduce()`, `collect()`).

```java
List<String> topNames = employees.stream()
        .filter(e -> e.salary() > 100_000)   // keep high earners
        .map(Employee::name)                 // extract names
        .sorted()                            // sort alphabetically
        .toList();                           // collect into unmodifiable List (Java 16+)
```

---

## 🟡 Layer 2 — Practitioner Track: `Collectors`, `flatMap`, `reduce` & Java 25 Gatherers

### 1. `map` vs `flatMap`
- `map`: **1-to-1** transformation (`Stream<Order> -> Stream<Customer>`).
- `flatMap`: **1-to-Many + flatten** (`Stream<Order> -> Stream<OrderItem>`, flattening `List<List<T>>` into `Stream<T>`).

### 2. Essential `Collectors`
- `Collectors.toUnmodifiableList()` / `.toList()` (Note: `Stream.toList()` since Java 16 allows `null` elements and returns unmodifiable list; `Collectors.toList()` returns mutable `ArrayList`).
- `Collectors.toMap(keyFn, valFn, (oldVal, newVal) -> newVal)` -> **ALWAYS supply a merge function** if keys can duplicate, otherwise `IllegalStateException`!
- `Collectors.groupingBy(Employee::dept, Collectors.counting())` -> downstream aggregation!
- `Collectors.partitioningBy(predicate)` -> always creates a 2-key map (`true` and `false`).
- `Collectors.teeing(c1, c2, merger)` (Java 12+) -> computes two aggregates in a **single pass** (e.g., `min` and `max` simultaneously).

### 3. Java 24/25 `Stream.gather(...)` (`JEP 485`)
Previously, intermediate operations were fixed (`map`, `filter`, `flatMap`). **Stream Gatherers** let you perform custom stateful intermediate ops like fixed windowing, sliding windows, and running scans:
```java
List<List<Integer>> batches = Stream.of(1, 2, 3, 4, 5)
        .gather(Gatherers.windowFixed(2))
        .toList(); // [[1, 2], [3, 4], [5]]
```

---

## 🔴 Layer 3 — Senior / JVM Track: Laziness, Fusion & Parallel Stream Traps

### 1. Loop Fusion & Short-Circuiting
Because intermediate operations are **lazy**, a pipeline of `filter -> map -> findFirst()` processes element-by-element vertically rather than materializing entire intermediate collections! When `findFirst()` finds 1 match, the remaining 10,000,000 elements are never touched.

### 2. `Spliterator` & Why `LinkedList` Parallelizes Terribly
Parallel streams split work via `Spliterator.trySplit()`:
- `ArrayList` / `IntStream.range`: **O(1) balanced index splits** (`SIZED`, `SUBSIZED`) -> scales well on multi-core CPUs.
- `LinkedList` / `Stream.iterate`: Sequential pointer chasing -> terrible split overhead; parallel mode is often **slower** than sequential!

### 3. The `ForkJoinPool.commonPool()` Production Trap
All `parallelStream()` calls share **one global `ForkJoinPool.commonPool()`** sized to `CPU cores - 1`:
- **NEVER run blocking I/O (HTTP, DB, File reads) inside `parallelStream()`** — you will starve every unrelated parallel stream and `CompletableFuture` across the entire microservice!
- Only use `parallelStream()` when: **large $N$ ($\ge 10,000+$), pure CPU-bound math, splittable source (`ArrayList`/array), stateless non-interfering lambdas**.
