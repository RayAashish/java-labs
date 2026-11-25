# Java 8 — Stream API (Exhaustive Mastery)

> **What it is:** A declarative, lazy, composable data-processing pipeline over a sequence of elements — turning nested `for` + `if` + temporary `List` boilerplate into readable, potentially parallel transformations.

## ⚡ 2-Minute Rapid Revision Card

```text
  Source  ──►  Intermediate Ops (LAZY)  ──►  Terminal Op (EAGER)
 (List,        filter, map, flatMap,          collect, reduce, count,
  Array,       distinct, sorted, peek,        forEach, anyMatch, min,
  File)        limit, skip                    findFirst, toArray
```

1. **Lazy Evaluation:** Intermediate operations record pipeline stages (`AbstractPipeline`); **nothing executes** until a terminal op triggers traversal.
2. **Vertical Loop Fusion:** Each element goes `filter -> map -> collect` one by one — intermediate collections are **not** allocated between stages!
3. **Stateless vs Stateful Intermediate Ops:** `filter`/`map` are stateless ($O(1)$ memory). `distinct`, `sorted`, `skip` can buffer elements and act as barriers!
4. **Short-Circuiting:** `limit`, `findFirst`, `anyMatch`/`allMatch`/`noneMatch` stop iterating early as soon as the answer is proven.
5. **Single-Use:** A `Stream` throws `IllegalStateException` if consumed twice.
6. **Parallel Streams Pitfall:** Never mutate shared `ArrayList`/`HashMap` inside `parallelStream()`, avoid `LinkedList`, and never run blocking I/O on `ForkJoinPool.commonPool()`.

## 📂 Contents

### Detailed Notes
1. [`notes/01-stream-architecture-and-creation.md`](notes/01-stream-architecture-and-creation.md) — Stream vs Collection, Loop Fusion, `Spliterator`/`Sink` internals, 10+ ways to create streams
2. [`notes/02-intermediate-vs-terminal-operations-complete.md`](notes/02-intermediate-vs-terminal-operations-complete.md) — Every Intermediate op, `map` vs `flatMap`, every Terminal op, all 3 `reduce()` overloads
3. [`notes/03-collectors-primitives-and-parallel-streams.md`](notes/03-collectors-primitives-and-parallel-streams.md) — Exhaustive `Collectors` catalog, Custom `Collector.of`, `IntStream`/`LongStream`/`DoubleStream`, Parallel Streams & `commonPool` traps

### Runnable Code Labs
1. [`codes/lts/java08/streams/StreamCreationAndLifecycleDemo.java`](codes/lts/java08/streams/StreamCreationAndLifecycleDemo.java)
2. [`codes/lts/java08/streams/StreamIntermediateAndTerminalOpsDemo.java`](codes/lts/java08/streams/StreamIntermediateAndTerminalOpsDemo.java)
3. [`codes/lts/java08/streams/StreamCollectorsAndPrimitivesDemo.java`](codes/lts/java08/streams/StreamCollectorsAndPrimitivesDemo.java)
4. [`codes/lts/java08/streams/ParallelStreamsPitfallsDemo.java`](codes/lts/java08/streams/ParallelStreamsPitfallsDemo.java)

### Interview Prep
- [`interview/INTERVIEW_QUESTIONS.md`](interview/INTERVIEW_QUESTIONS.md)

## 🚀 Compile & Run
```bash
javac -d out $(find 14-java-lts-evolution/java-08/02-stream-api-exhaustive/codes -name "*.java")
java -cp out lts.java08.streams.StreamCreationAndLifecycleDemo
java -cp out lts.java08.streams.StreamIntermediateAndTerminalOpsDemo
java -cp out lts.java08.streams.StreamCollectorsAndPrimitivesDemo
java -cp out lts.java08.streams.ParallelStreamsPitfallsDemo
```
