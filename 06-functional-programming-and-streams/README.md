# 06. Functional Programming, Lambdas, Optional & Streams API

> **Why this module exists:** Modern Java (8 -> 25) is deeply functional. Mastering Lambdas, Method References, `Optional`, and `Stream` pipelines (without hitting `orElse` eager-evaluation traps or `ForkJoinPool.commonPool()` starvation) separates clean expressive engineers from bug-prone code.

---

## ⚡ 2-Minute Rapid Revision Card

- **4 Core Functional Interfaces (`java.util.function`):** `Predicate<T>` (`T -> boolean`), `Function<T,R>` (`T -> R`), `Consumer<T>` (`T -> void`), `Supplier<T>` (`() -> T`).
- **Lambdas Under the Hood (`invokedynamic`):** Unlike anonymous inner classes (which emit `Outer$1.class` files and allocate per-call instances), lambdas compile to `invokedynamic` + `LambdaMetafactory`, avoiding extra `.class` bloat and caching stateless lambda instances!
- **Effectively Final Capture:** Lambdas capture local variables by **value** (copied into synthetic args), so local variables must be `final` or effectively final.
- **`Optional` Golden Rules:** Return `Optional<T>` from methods that may have no result; **NEVER** use `Optional` as a field, method parameter, or inside `List<Optional<T>>`. Prefer `.orElseGet(supplier)` over `.orElse(expensiveCall())`!
- **Streams Are Lazy & Single-Use:** Intermediate ops (`filter`, `map`, `flatMap`) build a pipeline without executing; only a terminal op (`toList`, `collect`, `reduce`, `count`) triggers traversal. Once consumed, a `Stream` cannot be reused (`IllegalStateException`).
- **Java 24/25 Stream Gatherers (`JEP 485`):** `stream.gather(Gatherers.windowFixed(3))` enables custom stateful intermediate operations natively!

---

## 📂 Contents

| File | Focus |
|---|---|
| [`notes/01-lambdas-functional-interfaces-and-invokedynamic.md`](notes/01-lambdas-functional-interfaces-and-invokedynamic.md) | `@FunctionalInterface`, Method References, `invokedynamic` vs Anonymous Classes |
| [`notes/02-optional-mastery-and-antipatterns.md`](notes/02-optional-mastery-and-antipatterns.md) | `Optional` composition (`map`/`flatMap`), `orElse` vs `orElseGet`, Anti-patterns |
| [`notes/03-streams-collectors-parallel-and-gatherers.md`](notes/03-streams-collectors-parallel-and-gatherers.md) | Lazy pipelines, `Spliterator`, `Collectors`, Parallel Stream traps, Java 25 `Gatherers` |
| [`codes/`](codes/) | 3 runnable labs: Lambdas/Composition, `Optional` Traps, Streams/Collectors/Gatherers |
| [`interview/INTERVIEW_QUESTIONS.md`](interview/INTERVIEW_QUESTIONS.md) | 30+ Functional & Stream interview Q&As + real coding queries |

---

## 🚀 Compile & Run

```bash
javac -d out $(find 06-functional-programming-and-streams/codes -name "*.java")
java -cp out functional.lambdas.LambdasAndMethodReferencesDemo
java -cp out functional.optional.OptionalBestPracticesDemo
java -cp out functional.streams.StreamsCollectorsAndGatherersDemo
```
