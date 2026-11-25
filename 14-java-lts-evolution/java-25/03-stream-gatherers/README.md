# Java 25 — Stream Gatherers (`Stream.gather` — JEP 485)

## ⚡ 2-Minute Revision
- **Why Needed Since Java 8:** Java 8 added custom **terminal** ops (`Collector.of`), yet had **NO standard extension point for custom intermediate ops**! Simple tasks like *"split stream into batches of 3"*, *"sliding window of 2"*, or *"deduplicate by user email preserving encounter order"* required ugly index loops or hacks.
- **`Stream.gather(Gatherer<T, A, R>)` (Finalized in JDK 24 / Java 25 LTS):** Does for intermediate operations what `Collector` did for terminal operations!
- **All 5 Built-in `java.util.stream.Gatherers`:**
  1. `Gatherers.windowFixed(3)` -> batches elements into `List`s of size 3 (`[[1,2,3], [4,5]]`).
  2. `Gatherers.windowSliding(2)` -> sliding window (`[[1,2], [2,3], [3,4]]`).
  3. `Gatherers.scan(() -> 0, (sum, x) -> sum + x)` -> prefix running totals emitting EVERY intermediate state!
  4. `Gatherers.fold(() -> "", (acc, s) -> acc + s)` -> many-to-1 stream reduction *as an intermediate stage*!
  5. `Gatherers.mapConcurrent(maxConcurrency, fn)` -> runs concurrent `VirtualThread` work inside a stream with capped fan-out while preserving order!
- **Custom Gatherers:** Built via `Gatherer.ofSequential(initializer, integrator, finisher)` — supports stateful scanning AND **early short-circuit termination** cleanly!

## 📂 Contents
- [`notes/stream-gatherers-deep-dive.md`](notes/stream-gatherers-deep-dive.md)
- [`codes/lts/java25/gatherers/StreamGatherersMasteryDemo.java`](codes/lts/java25/gatherers/StreamGatherersMasteryDemo.java)
- [`interview/INTERVIEW_QUESTIONS.md`](interview/INTERVIEW_QUESTIONS.md)
