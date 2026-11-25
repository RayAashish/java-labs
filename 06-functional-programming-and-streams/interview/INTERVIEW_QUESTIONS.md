# 06. Functional Programming, Lambdas, Optional & Streams — Interview Questions & Answers

---

## 🟢 Beginner Questions

### Q1. What is a `@FunctionalInterface`? Can it have `default` or `static` methods?
An interface with **exactly ONE abstract method** (`SAM`). Yes, it can have any number of `default`, `static`, `private` helper methods, plus methods inherited from `java.lang.Object` (`equals`, `toString`, `hashCode` don't count against the 1 abstract method rule — e.g. `Comparator<T>`!).

### Q2. Name the 4 core functional interfaces in `java.util.function`.
- `Predicate<T>`: `boolean test(T t)`
- `Function<T, R>`: `R apply(T t)`
- `Consumer<T>`: `void accept(T t)`
- `Supplier<T>`: `T get()`

### Q3. Why can't you reuse a `Stream` after a terminal operation runs?
Streams represent a single-pass computation pipeline over a data source, not a storage container. Calling a second terminal operation throws `IllegalStateException: stream has already been operated upon or closed`.

### Q4. What is the difference between Intermediate and Terminal operations?
- **Intermediate (`filter`, `map`, `flatMap`, `sorted`, `distinct`, `peek`):** Return another `Stream` and are **lazy** (do not execute until a terminal operation runs).
- **Terminal (`toList`, `collect`, `count`, `reduce`, `findFirst`, `forEach`, `anyMatch`):** Trigger pipeline execution and produce a final result or side effect.

---

## 🟡 Intermediate Questions

### Q5. `map()` vs `flatMap()` in Streams and `Optional`?
- `map`: **1-to-1** transformation (`Stream<String>` -> `Stream<Integer>` via `String::length`).
- `flatMap`: **1-to-Many + flattening** (`Stream<List<String>>` -> `Stream<String>` via `List::stream`). In `Optional`, `flatMap` prevents nested `Optional<Optional<T>>`.

### Q6. Explain the difference between `Optional.orElse(...)` and `Optional.orElseGet(...)`.
`orElse(value)` **eagerly evaluates its argument EVERY time**, even when the `Optional` IS present (dangerous if passing a method call or DB lookup!). `orElseGet(supplier)` executes the `Supplier` **lazily** ONLY when the `Optional` is empty.

### Q7. Why should you NEVER use `Optional` as a class field or method parameter?
1. `Optional` is **not `Serializable`** (breaks frameworks/caches serializing entities).
2. Adds unnecessary Heap allocation overhead per field.
3. As a method parameter, callers can still pass `null`! Return `Optional<T>` from methods only.

### Q8. `Stream.toList()` (Java 16+) vs `Collectors.toList()` vs `Collectors.toUnmodifiableList()`?
- `Collectors.toList()`: Returns a mutable list (usually `ArrayList`), allows `null`.
- `Collectors.toUnmodifiableList()` (Java 10+): Unmodifiable list, **rejects `null` elements** (throws NPE!).
- `Stream.toList()` (Java 16+): Unmodifiable list, **permits `null` elements**, slightly lower memory/CPU overhead (`ImmutableCollections.listFromTrustedArrayNullsAllowed`).

### Q9. What happens if `Collectors.toMap(k, v)` encounters duplicate keys?
Throws `IllegalStateException: Duplicate key`. Always provide the 3rd merge-function argument: `Collectors.toMap(User::id, User::name, (existing, replacement) -> replacement)`.

### Q10. Why shouldn't you use `Stream.peek()` in production business logic?
`peek()` exists strictly for debugging. Under Java 9+, short-circuiting terminal operations like `count()` on a `SIZED` source (`list.stream().peek(...).count()`) optimize away intermediate `peek()` execution completely!

---

## 🔴 Senior / JVM Internals & Coding Questions

### Q11. How do Lambdas differ from Anonymous Inner Classes in bytecode & memory?
- Anonymous inner classes generate physical `Outer$1.class` files, allocate a new object each time, and bind `this` to the inner instance.
- Lambdas emit **`invokedynamic`** (`LambdaMetafactory`), create **no extra `.class` files**, reuse cached singleton instances when stateless, and bind `this` to the **enclosing outer class**!

### Q12. Why are local variables captured by a Lambda required to be `final` or `effectively final`?
Local variables live on the **Thread Stack**; a lambda may outlive that stack frame (e.g., passed to an `ExecutorService`). The JVM captures local variables **by value** (copying into the synthetic lambda), so disallowing mutation guarantees Stack and Heap copies can never diverge. *(Instance fields CAN be mutated via captured `this`.)*

### Q13. Explain Stateful vs Stateless intermediate ops & `parallelStream()` pitfalls.
- **Stateless (`filter`, `map`):** Process each element independently -> parallelize cleanly.
- **Stateful (`sorted`, `distinct`, `limit`):** Require buffering/coordination across threads.
- **Parallel Trap:** All `parallelStream()` calls share the single JVM-wide **`ForkJoinPool.commonPool()`** (`cores - 1` threads). Running blocking HTTP/DB I/O inside `parallelStream()` starves the entire server! Also, `LinkedList` and `Stream.iterate` split poorly compared to `ArrayList`/`IntStream.range`.

### Q14. `findFirst()` vs `findAny()`?
In sequential streams both return the first element. In **parallel streams**, `findFirst()` enforces strict encounter-order coordination across worker threads, whereas `findAny()` returns whichever worker finishes first -> significantly faster!

### Q15. What are Java 24/25 Stream Gatherers (`JEP 485`)?
`Stream.gather(Gatherer)` adds extensible custom **intermediate** operations (just like `Collector` added extensible terminal operations in Java 8!). Built-ins include `Gatherers.windowFixed(n)`, `windowSliding(n)`, `fold(...)`, `scan(...)`, and `mapConcurrent(...)`.

---

## 💻 Classic Stream Coding Interview Queries (Know These Cold!)

1. **Second highest salary:**
   `employees.stream().map(Emp::salary).distinct().sorted(Comparator.reverseOrder()).skip(1).findFirst()`
2. **Frequency map of characters in a String:**
   `str.chars().mapToObj(c -> (char) c).collect(Collectors.groupingBy(Function.identity(), Collectors.counting()))`
3. **First non-repeating character (preserving order via `LinkedHashMap`):**
   `str.chars().mapToObj(c -> (char) c).collect(Collectors.groupingBy(Function.identity(), LinkedHashMap::new, Collectors.counting())).entrySet().stream().filter(e -> e.getValue() == 1).map(Map.Entry::getKey).findFirst()`
4. **Group employees by department & pick highest paid in each:**
   `employees.stream().collect(Collectors.groupingBy(Emp::dept, Collectors.maxBy(Comparator.comparingDouble(Emp::salary))))`
5. **Compute min and max in a single stream pass:**
   `nums.stream().collect(Collectors.teeing(Collectors.minBy(Integer::compare), Collectors.maxBy(Integer::compare), (min, max) -> ...))`
