# 03. Stream API Architecture, Creation & Lazy Pipeline Model

---

## 🟢 Layer 1 — Noob Intuition: Warehouse vs. Conveyor Belt

- A **Collection (`List`, `Set`)** is a **warehouse** — it holds all items in memory at once.
- A **Stream (`java.util.stream.Stream`)** is a **processing conveyor belt** — it does NOT store elements, NEVER mutates the original source collection, and computes values **lazily on demand** when a terminal operation pulls data!

### The 3 Parts of Every Stream Pipeline
```text
[1. SOURCE]            ──► [2. INTERMEDIATE OPS (Lazy)]        ──► [3. TERMINAL OP (Trigger)]
list.stream()              .filter(...) .map(...) .sorted()        .toList() / .collect(...) / .count()
Arrays.stream(arr)         Returns a NEW Stream; does 0 work!       Executes the whole pipeline ONCE!
```

> [!IMPORTANT]
> **Single-Use Rule:** Once a terminal operation finishes, the Stream is **consumed/closed**. Reusing the same `Stream` reference throws `IllegalStateException: stream has already been operated upon or closed`.

---

## 🟡 Layer 2 — All 10 Ways to Create a Stream

1. **From Collections:** `list.stream()` or `list.parallelStream()`
2. **From Arrays:** `Arrays.stream(intArray)` or `Arrays.stream(strArray, startIdx, endIdx)`
3. **From Explicit Values:** `Stream.of("A", "B", "C")` *(Trap: `Stream.of(intArray)` creates `Stream<int[]>` of size 1, NOT `IntStream`! Use `Arrays.stream(intArray)`!)*
4. **Nullable Single Value (Java 9+):** `Stream.ofNullable(possiblyNull)` (0 or 1 element safely!)
5. **Numeric Ranges (`IntStream` / `LongStream`):** `IntStream.range(0, 5)` (`0..4`) vs `IntStream.rangeClosed(1, 5)` (`1..5`)
6. **Infinite Iterative Stream:** `Stream.iterate(1, n -> n * 2).limit(10)` or Java 9 bounded `Stream.iterate(1, n -> n <= 100, n -> n + 10)`!
7. **Infinite Supplier Stream:** `Stream.generate(UUID::randomUUID).limit(5)` *(Always pair with `.limit()` or `OutOfMemoryError`/infinite loop occurs!)*
8. **Builder:** `Stream.<String>builder().add("A").add("B").build()`
9. **Empty Stream:** `Stream.empty()` (better than returning `null`)
10. **I/O & Regex:** `Files.lines(path)` (MUST wrap in `try-with-resources`!), `Pattern.compile(",").splitAsStream("a,b,c")`, `"hello".chars()` (`IntStream` of UTF-16 code units).

---

## 🔴 Layer 3 — Senior / JVM Internals: `Spliterator` & Loop Fusion

1. **Vertical Loop Fusion (Not Horizontal Buffering!):**
   Beginners assume `list.stream().filter(...).map(...).findFirst()` filters all 1,000,000 items into list A, maps all matches into list B, then grabs element 0. **Wrong!** Streams pull **one element vertically** through `filter -> map -> findFirst`. Once item #1 passes, elements #2..1,000,000 are **never visited**!
2. **Stateless vs Stateful Stages:**
   - **Stateless (`filter`, `map`, `flatMap`):** Need zero cross-element memory ($O(1)$ space).
   - **Stateful (`sorted`, `distinct`, `limit`):** Must buffer elements internally (`sorted()` buffers everything into an array before downstream ops see item #1!).
3. **`Spliterator` Characteristics:** Sources advertise flags (`SIZED`, `SUBSIZED`, `ORDERED`, `SORTED`, `DISTINCT`, `IMMUTABLE`). For example, `list.stream().count()` on a `SIZED` source in Java 9+ skips intermediate stateless ops or returns size in $O(1)$!
