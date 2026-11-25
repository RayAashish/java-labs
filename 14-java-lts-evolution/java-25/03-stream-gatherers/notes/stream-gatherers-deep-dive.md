# Stream Gatherers (JEP 485, Finalized Java 24/25) — Deep Dive

---

## 🟢 Layer 1 — Noob Track: Think "`Collectors`, But for the Middle of a Stream!"

In Java 8:
- **End of stream (`.collect(...)`):** Completely extensible via custom `Collector`.
- **Middle of stream (`.filter()`, `.map()`):** Fixed permanently! Couldn't even write `.batch(50)` or `.runningSum()` cleanly without awkward workarounds.

Java 24/25 introduces **`stream.gather(...)`**:
```java
List<List<Integer>> pages = Stream.of(1, 2, 3, 4, 5)
    .gather(Gatherers.windowFixed(2)) // [[1, 2], [3, 4], [5]]
    .toList();
```

---

## 🟡 Layer 2 — Practitioner Track: All 5 `Gatherers` + Custom `Gatherer`

### Built-In `java.util.stream.Gatherers`

| Factory | Behavior | Example `[1, 2, 3, 4]` |
|---|---|---|
| `Gatherers.windowFixed(2)` | Non-overlapping fixed-size chunks | `[[1, 2], [3, 4]]` (last truncated if remainder) |
| `Gatherers.windowSliding(3)` | Sliding window shifting by 1 | `[[1, 2, 3], [2, 3, 4]]` |
| `Gatherers.scan(() -> 0, Integer::sum)` | Running prefix state emitted per element | `[1, 3, 6, 10]` |
| `Gatherers.fold(() -> 0, Integer::sum)` | Combines entire stream to 1 element mid-pipeline | `[10]` (then can `.map(...)` further!) |
| `Gatherers.mapConcurrent(max, fn)` | Runs `fn` on up to `max` virtual threads concurrently, preserving encounter order | Ideal for batched I/O inside pipelines! |

### Anatomy of a Custom `Gatherer<T, A, R>`
Mirror image of `Collector`, enhanced with **`Downstream<? super R>` push & short-circuit control**:
1. **`initializer()`** (`Supplier<A>`): allocates mutable intermediate state (e.g., `HashSet<K>` for `distinctBy`, or running `AtomicInteger`).
2. **`integrator()`** (`(state, element, downstream) -> boolean`):
   - Call `downstream.push(out)` **0, 1, or many times**!
   - Return `true` to keep consuming, or **`false` to short-circuit cleanly** (like `limit` / `takeWhile`!).
3. **`combiner()`** (`BinaryOperator<A>`): merges partial states for parallel execution *(omit using `ofSequential` when order matters!)*.
4. **`finisher()`** (`(state, downstream) -> void`): flushes any trailing incomplete batch when upstream ends!

---

## 🔴 Layer 3 — Senior / JVM Track

- **`Integrator.Greedy`:** Just like `Characteristics.UNORDERED` helps `Collector`, marking an integrator via `Gatherer.Integrator.ofGreedy(...)` notifies HotSpot / the Stream pipeline that the stage **never short-circuits early**, letting the runtime fuse push loops without per-element boolean termination checks!
- **Gatherer Composition (`g1.andThen(g2)`):** Two gatherers compose algebraically into a single fused `Gatherer`, preserving encapsulation cleanly across libraries.
- **`mapConcurrent` Structured Cancellation:** Under the hood, `Gatherers.mapConcurrent(maxConcurrency, fn)` manages virtual-thread task bounds cleanly — canceling remaining in-flight tasks if an exception occurs or a downstream `limit()` short-circuits!
