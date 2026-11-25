# Interview Questions — Java 25 Stream Gatherers (`Stream.gather`)

## 🟢 Beginner

### Q1. What gap did Stream Gatherers (`JEP 485`) fill in Java's Stream API?
Since Java 8, `Collector` allowed custom **terminal** reductions, while **intermediate** ops (`filter`, `map`, `flatMap`) were locked to the JDK! `Stream.gather(Gatherer)` adds custom, composable, stateful **intermediate** stream operations cleanly without breaking lazy pipelines.

---

## 🟡 Intermediate

### Q2. Compare `Gatherers.scan` vs `Gatherers.fold` vs `Stream.reduce`.
- **`Stream.reduce`:** **Terminal** op — consumes the stream and produces one final aggregate value directly.
- **`Gatherers.fold`:** **Intermediate** many-to-1 reduction — collapses the stream into a 1-element `Stream<R>` so more `.map()`/`.filter()` stages can continue afterward!
- **`Gatherers.scan`:** **Intermediate** prefix scan — emits EVERY intermediate running accumulator value (`[10, 30, 60, ...]`).

### Q3. How do `windowFixed(3)` and `windowSliding(3)` differ?
Given `[1, 2, 3, 4, 5]`:
- `windowFixed(3)` -> `[[1, 2, 3], [4, 5]]` (disjoint batches, partial tail emitted by `finisher`).
- `windowSliding(3)` -> `[[1, 2, 3], [2, 3, 4], [3, 4, 5]]` (overlapping sliding windows).

---

## 🔴 Senior

### Q4. Why does `Gatherer.Integrator` return a `boolean`, and what is `Integrator.ofGreedy`?
Returning `false` signals to the Stream pipeline that no more elements should be pushed (either because this gatherer itself reached its limit or because `downstream.push(...)` returned `false` due to downstream `.limit()`). Using `Integrator.ofGreedy(...)` asserts this stage never initiates short-circuiting on its own, allowing the pipeline to optimize inner loops.
