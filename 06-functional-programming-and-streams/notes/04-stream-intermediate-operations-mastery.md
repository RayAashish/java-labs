# 04. Stream Intermediate Operations — Complete Reference & Gotchas

Every intermediate operation is **lazy** and returns a new `Stream`.

---

## 📋 Complete Intermediate Method Matrix

| Method | Kind | Java Version | What It Does | Key Gotcha / Tip |
|---|---|---|---|---|
| **`filter(Predicate)`** | Stateless | Java 8 | Keeps elements matching predicate | Put cheapest/most-selective filters **first**! |
| **`map(Function)`** | Stateless | Java 8 | 1-to-1 transform `T -> R` | Avoid heavy side effects inside `map` |
| **`mapToInt/Long/Double`** | Stateless | Java 8 | Maps `Stream<T>` -> primitive stream | Eliminates boxing overhead; unlocks `.sum()`, `.average()` |
| **`flatMap(Function<T, Stream<R>>)`** | Stateless | Java 8 | 1-to-Many + flattens nested streams | Essential for `Order -> items.stream()` or `Optional::stream` |
| **`mapMulti(BiConsumer<T, Consumer<R>>)`** | Stateless | **Java 16** | Imperative 0/1/Many push without creating sub-`Stream`s | Faster than `flatMap` when emitting 0 or 1 element (`Stream.of` alloc skipped!) |
| **`distinct()`** | Stateful | Java 8 | Deduplicates using `equals()` & `hashCode()` | Uses `LinkedHashSet` for ordered streams; **ensure `hashCode/equals` exist**! |
| **`sorted()` / `sorted(Comparator)`** | Stateful | Java 8 | Sorts elements (TimSort) | Buffers ALL elements in memory — never call on infinite streams! |
| **`peek(Consumer)`** | Stateless | Java 8 | Observes elements flowing past (debugging) | **TRAP:** May be skipped completely by Java 9+ `.count()` short-circuiting! |
| **`limit(long maxSize)`** | Short-circuit Stateful | Java 8 | Truncates stream after `n` elements | Essential to bound infinite `generate`/`iterate` streams |
| **`skip(long n)`** | Stateful | Java 8 | Discards first `n` elements | Combine `skip` + `limit` for in-memory pagination |
| **`takeWhile(Predicate)`** | Short-circuit | **Java 9** | Stops the moment predicate becomes `false` | Unlike `filter()`, assumes **ordered/sorted** input and halts early! |
| **`dropWhile(Predicate)`** | Stateful | **Java 9** | Drops leading elements while `true`, emits ALL remaining | Once condition flips `false`, stops checking predicate for rest of stream! |
| **`boxed()`** | Stateless | Java 8 | Converts `IntStream` -> `Stream<Integer>` | Needed before `.collect(Collectors.toList())` |
| **`mapToObj(IntFunction)`** | Stateless | Java 8 | Maps primitive `int` directly to object `R` | Avoids intermediate `Integer` boxing |
| **`gather(Gatherer)`** | Stateful / Custom | **Java 24/25** | Extensible intermediate ops (`windowFixed`, `windowSliding`, `scan`) | Replaces clunky manual batching/chunking loops! |

---

## 🎯 Deep Dive: `flatMap` vs `mapMulti` & `filter` vs `takeWhile`

### 1. `filter()` vs `takeWhile()` (Java 9)
Suppose `nums = [2, 4, 6, 7, 8, 10]`:
- `nums.stream().filter(n -> n % 2 == 0)` -> **`[2, 4, 6, 8, 10]`** (scans all elements).
- `nums.stream().takeWhile(n -> n % 2 == 0)` -> **`[2, 4, 6]`** (aborts immediately upon hitting `7`!).
- `nums.stream().dropWhile(n -> n % 2 == 0)` -> **`[7, 8, 10]`** (drops `2, 4, 6`, emits everything from `7` onward).

### 2. `flatMap()` vs `mapMulti()` (Java 16)
When expanding 1 element into 0 or 1 result (or conditional casting):
- `flatMap` creates a brand-new `Stream` object (`Stream.of(...)` / `Stream.empty()`) for **every** element.
- `mapMulti` passes a `Consumer` down the pipeline directly with **zero temporary Stream allocations**:
  ```java
  List<String> stringsOnly = mixedList.stream()
          .<String>mapMulti((obj, downstream) -> {
              if (obj instanceof String s) downstream.accept(s.toUpperCase());
          })
          .toList();
  ```
