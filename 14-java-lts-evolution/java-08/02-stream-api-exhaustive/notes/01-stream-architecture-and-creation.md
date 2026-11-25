# 01. Stream Architecture, Laziness & All Ways to Create a Stream

---

## 🟢 Layer 1 — Noob Track: Collection vs Stream Mental Model

- A **Collection** (`ArrayList`, `HashSet`) is like a **DVD / warehouse**: all data exists *in memory right now*, you store and mutate elements directly.
- A **Stream** (`java.util.stream.Stream<T>`) is like a **Netflix live stream / factory conveyor belt**: it computes elements on demand from a source, never stores elements, never modifies the source collection, and can be watched only **once**.

---

## 🟡 Layer 2 — Practitioner Track: 10 Ways to Create a Stream

| Source Category | Method | Notes / Pitfalls |
|---|---|---|
| Collection | `list.stream()` / `list.parallelStream()` | Most common; preserves Encounter Order if source is ordered (`List`) |
| Array | `Arrays.stream(arr)` / `Arrays.stream(arr, start, end)` | Uses primitive array specialization for `int[]`, `long[]`, `double[]` |
| Static Factory | `Stream.of(a, b, c)` | Careful: `Stream.of(intArray)` creates `Stream<int[]>` (1 element!), NOT `IntStream`! |
| Nullable Element | `Stream.ofNullable(x)` *(Java 9)* | Returns empty stream if `null`, 1-element stream otherwise |
| Empty Stream | `Stream.empty()` | Better than returning `null` from methods returning `Stream<T>` |
| Primitive Ranges | `IntStream.range(1, 5)` / `rangeClosed(1, 5)` | `range` excludes upper bound (`1..4`); `rangeClosed` includes it (`1..5`) |
| Infinite Iterate | `Stream.iterate(0, n -> n + 2)` | Unbounded unless `.limit(n)` is attached! (Java 9 added bounded 3-arg `iterate(seed, hasNext, next)`) |
| Infinite Generate | `Stream.generate(UUID::randomUUID)` | Takes a stateless `Supplier<T>`; must `.limit(...)` before stateful terminal ops |
| Builder | `Stream.<String>builder().add("a").add("b").build()` | Useful for dynamic programmatic assembly before returning |
| Strings & RegEx | `"abc".chars()`, `"a,b,c".lines()`, `Pattern.compile(",").splitAsStream(str)` | `lines()` streams lines lazily without loading an entire split array |
| Files / I/O | `Files.lines(path)`, `Files.walk(dir)` | **MUST wrap in `try-with-resources`** so underlying OS file descriptor closes! |
| Merging Streams | `Stream.concat(s1, s2)` | Merging >2 streams deeply via nested `concat` can hurt stack depth — prefer `Stream.of(s1, s2, s3).flatMap(Function.identity())` |

---

## 🔴 Layer 3 — Senior / JVM Architect Track: How Streams Execute Internally

### 1. Vertical Loop Fusion (Not Horizontal Lists!)
Beginners often think:
> `stream.filter(...).map(...).collect(...)` creates a temporary filtered list first, then maps that list into another list.

**FALSE.** Streams invert control via a **linked chain of `Sink`s** (`ReferencePipeline$Head -> StatelessOp -> StatelessOp -> ReduceOps.ReducingSink`):
1. Calling `.filter(...)` or `.map(...)` constructs a lightweight `StatelessOp` node pointing to `previousStage` and records bitmask **Stream Flags** (`ORDERED`, `DISTINCT`, `SORTED`, `SIZED`, `SHORT_CIRCUIT`).
2. When `.collect()` starts, `AbstractPipeline.wrapSink()` wraps sinks from back-to-front.
3. `Spliterator` pulls elements one by one from the source and pushes each element vertically down `sink.begin()` -> `filter` -> `map` -> `collector.accumulator().accept(...)`.
4. **Bonus optimization:** If a stage knows `SIZED` still holds (e.g., `map` only, no `filter`), `toArray` pre-allocates the exact target array size in one shot! And `.sorted()` checks if the `SORTED` flag is already set (e.g., `TreeSet.stream()`) and skips sorting completely!

### 2. Why `Spliterator` Replaced `Iterator` for Streams
`Iterator` only has `hasNext()` and `next()` — impossible to parallelize cleanly.
`java.util.Spliterator<T>` adds:
- `tryAdvance(Consumer)`: step one element.
- `forEachRemaining(Consumer)`: tight hot loop without repeated `hasNext()` overhead.
- `trySplit()`: cuts the remaining work roughly in half so another `ForkJoinPool` worker can process the second half!
- `characteristics()`: bitmask (`ORDERED`, `DISTINCT`, `SORTED`, `SIZED`, `SUBSIZED`, `CONCURRENT`, `IMMUTABLE`, `NONNULL`).
