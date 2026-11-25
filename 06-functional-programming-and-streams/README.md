# 06. Functional Programming, `Optional` & Stream API Mastery

> **Complete Java 8 → 25 Reference:** Covers every functional interface in `java.util.function`, all 4 Method Reference forms, `Optional` API & anti-patterns, all 10 Stream creation patterns, **every** intermediate & terminal Stream method, **every** `Collectors` pattern, Primitive Streams, Parallel Stream pitfalls, and Java 24/25 **Stream Gatherers**.

---

## ⚡ 2-Minute Rapid Revision Card

- **4 Core Functional Interfaces:** `Predicate<T>` (`T -> boolean`), `Function<T,R>` (`T -> R`), `Consumer<T>` (`T -> void`), `Supplier<T>` (`() -> T`).
- **Lambdas Under the Hood:** Compile to **`invokedynamic` + `LambdaMetafactory`** (no `Outer$1.class` files; `this` points to outer class!). Local variables captured must be **effectively final**.
- **`Optional` Golden Rule:** Never use `Optional` as fields or parameters. Prefer `.orElseGet(supplier)` over `.orElse(expensiveCall())`!
- **Streams Are Lazy & Single-Use:** Intermediate ops (`filter`, `map`, `flatMap`, `mapMulti`, `takeWhile`, `dropWhile`, `sorted`, `distinct`, `gather`) build the conveyor belt lazily; terminal ops (`toList`, `collect`, `reduce`, `count`, `anyMatch`, `findFirst`) execute it once.
- **`Collectors` Traps:** 2-arg `toMap(k, v)` throws on duplicate keys (pass 3rd merge arg!); `toUnmodifiableList()` rejects `null` whereas `Stream.toList()` allows `null`.
- **Parallel Streams:** Never mutate shared `ArrayList` or run blocking I/O inside `parallelStream()` (starves JVM-wide `ForkJoinPool.commonPool()`).

---

## 📂 Notes & Dedicated Runnable Labs

### Part 1: Lambdas, Functional Interfaces & `Optional`
| Note | Companion Runnable Labs |
|---|---|
| [`01-lambdas-and-functional-interfaces-deep-dive.md`](notes/01-lambdas-and-functional-interfaces-deep-dive.md) | [`LambdasAndInvokedynamicDemo.java`](codes/functional/lambdas/LambdasAndInvokedynamicDemo.java)<br>[`FunctionalInterfacesCatalogDemo.java`](codes/functional/lambdas/FunctionalInterfacesCatalogDemo.java)<br>[`MethodReferencesFourFormsDemo.java`](codes/functional/lambdas/MethodReferencesFourFormsDemo.java) |
| [`02-optional-api-exhaustive-guide.md`](notes/02-optional-api-exhaustive-guide.md) | [`OptionalCompleteApiAndTrapsDemo.java`](codes/functional/optional/OptionalCompleteApiAndTrapsDemo.java) |

### Part 2: Complete Stream API & `Collectors` Mastery
| Note | Companion Runnable Labs |
|---|---|
| [`03-stream-api-architecture-and-creation.md`](notes/03-stream-api-architecture-and-creation.md) | [`StreamCreationAllWaysDemo.java`](codes/functional/streams/StreamCreationAllWaysDemo.java) |
| [`04-stream-intermediate-operations-mastery.md`](notes/04-stream-intermediate-operations-mastery.md) | [`StreamIntermediateOpsMasteryDemo.java`](codes/functional/streams/StreamIntermediateOpsMasteryDemo.java) |
| [`05-stream-terminal-operations-and-collectors.md`](notes/05-stream-terminal-operations-and-collectors.md) | [`StreamTerminalAndReduceDemo.java`](codes/functional/streams/StreamTerminalAndReduceDemo.java)<br>[`CollectorsCompleteMasteryDemo.java`](codes/functional/streams/CollectorsCompleteMasteryDemo.java)<br>[`PrimitiveStreamsAndGatherersDemo.java`](codes/functional/streams/PrimitiveStreamsAndGatherersDemo.java) |
| [`06-parallel-streams-performance-and-gotchas.md`](notes/06-parallel-streams-performance-and-gotchas.md) | [`ParallelStreamsPitfallsAndBenchmarkDemo.java`](codes/functional/streams/ParallelStreamsPitfallsAndBenchmarkDemo.java) |

### Part 3: Interview Preparation
- 👉 **[`interview/INTERVIEW_QUESTIONS.md`](interview/INTERVIEW_QUESTIONS.md)** — 30+ Functional/Stream Q&As + Top Stream Coding Queries.

---

## 🚀 Compile & Run

```bash
javac -d out $(find 06-functional-programming-and-streams/codes -name "*.java")

java -cp out functional.lambdas.LambdasAndInvokedynamicDemo
java -cp out functional.lambdas.FunctionalInterfacesCatalogDemo
java -cp out functional.lambdas.MethodReferencesFourFormsDemo
java -cp out functional.optional.OptionalCompleteApiAndTrapsDemo
java -cp out functional.streams.StreamCreationAllWaysDemo
java -cp out functional.streams.StreamIntermediateOpsMasteryDemo
java -cp out functional.streams.StreamTerminalAndReduceDemo
java -cp out functional.streams.CollectorsCompleteMasteryDemo
java -cp out functional.streams.PrimitiveStreamsAndGatherersDemo
java -cp out functional.streams.ParallelStreamsPitfallsAndBenchmarkDemo
```
