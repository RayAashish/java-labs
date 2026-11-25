# Interview Questions — Java 8 Stream API (Exhaustive)

## 🟢 Beginner

### Q1. What is the difference between a `Collection` and a `Stream`?
- **Collection:** Eager in-memory data structure holding elements; supports add/remove mutation; can be traversed repeatedly.
- **Stream:** Lazy computational view/pipeline over a source; never mutates the underlying collection; single-use (`IllegalStateException` if reused).

### Q2. What is the difference between `map` and `flatMap`?
- **`map(T -> R)`:** 1-to-1 transformation (`Stream<User>` -> `Stream<String>` names).
- **`flatMap(T -> Stream<R>)`:** 1-to-Many flattening (`Stream<Order>` where each order has `List<Item>` -> single flat `Stream<Item>`).

### Q3. Why shouldn't you use `peek()` for production business logic?
`peek()` is meant strictly for debugging. In Java 9+, if the JVM proves the stream size is already known (`SIZED`) and no filter/limit changes it, `.count()` short-circuits in $O(1)$ and **never invokes `peek()` at all**!

---

## 🟡 Intermediate

### Q4. Explain `Stream.of(new int[]{1, 2, 3})` vs `Arrays.stream(new int[]{1, 2, 3})`.
Because Java Generics do not support primitives, `Stream.of(int[])` treats the entire `int[]` array as a **single element** (`Stream<int[]>` of size 1!). `Arrays.stream(int[])` has dedicated primitive overloads returning `IntStream` of size 3.

### Q5. What happens when `Collectors.toMap(User::getCity, User::getName)` encounters duplicate keys?
It throws **`IllegalStateException: Duplicate key`**! Always pass the 3rd-arg merge function when duplicates are possible: `Collectors.toMap(k, v, (existing, replacement) -> replacement)`.

### Q6. Compare `Collectors.groupingBy(p -> cond)` vs `Collectors.partitioningBy(pred)`.
- `groupingBy` returns `Map<K, List<T>>` with keys only for groups present in the data, and **throws `NullPointerException`** if the classifier returns `null`.
- `partitioningBy` always returns `Map<Boolean, List<T>>` containing **both `true` and `false` entries** (even if one list is empty).

### Q7. How does `Collectors.filtering` (Java 9) differ from `.filter(...).collect(groupingBy(...))`?
If you call `.filter(active)` *before* `groupingBy(department)`, any department with zero active employees disappears from the output map completely! Putting `Collectors.filtering(active, toList())` **downstream inside `groupingBy`** preserves the department key mapped to an empty `List` `[]`.

---

## 🔴 Senior / Architecture & Internals

### Q8. Explain Stream Loop Fusion and Stateless vs Stateful Operations.
Streams process elements **vertically element-by-element** down a chain of wrapped `Sink` objects — no intermediate lists are built between `filter` and `map`. However, **stateful barrier operations** like `.sorted()` MUST collect all upstream elements into an internal buffer before emitting the first sorted element downstream. Put cheap `filter()` calls *before* `sorted()`!

### Q9. What happens with the identity value in `stream.parallel().reduce(10, Integer::sum)`?
**Silent bug!** The identity `0` is correct for addition (`0 + x == x`). Passing `10` means **every worker thread** starts its partial reduction with `10`, so for 4 workers `[1, 2, 3, 4]` produces `44` instead of `14`!

### Q10. Why is `parallelStream()` dangerous inside Spring Boot / Tomcat web request handlers?
`parallelStream()` shares the JVM-wide **`ForkJoinPool.commonPool()`** (`cores - 1` threads). Blocking I/O (HTTP, JDBC, `Thread.sleep`) inside a parallel stream stalls `commonPool()` workers, starving unrelated parallel streams and `CompletableFuture.supplyAsync()` tasks across the entire server.

### Q11. Tricky Output Puzzle:
```java
long n = List.of("a", "b", "c").stream()
    .peek(System.out::println)
    .count();
```
**Output on JDK 9–25:** Prints **NOTHING**, `n = 3`! `count()` detects `SIZED` without size-changing ops (`filter`, `distinct`, `flatMap`, `limit`, `skip`), returns `3` in $O(1)$, and elides `peek`. On JDK 8 it printed `a`, `b`, `c`.
