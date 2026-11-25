# Java 8 LTS (March 2014) — Feature-by-Feature Deep Mastery

> **Historical turning point:** Java 8 was the biggest programming-model shift in Java's history — bringing Functional Programming, Streams, `Optional`, modern Date/Time (`java.time`), `CompletableFuture`, Interface evolution, `ConcurrentHashMap` V8 overhaul, and Metaspace.

---

## ⚡ 2-Minute Rapid Revision Card

- **Lambdas:** Compiled via **`invokedynamic`** + `LambdaMetafactory`, NOT anonymous inner `.class` files (`$1.class`). Captured local variables must be **effectively final**.
- **Streams:** **Lazy** single-use pipelines (`Sink` chain) — intermediate ops do zero work until a terminal op triggers traversal. Vertical **loop fusion**, not horizontal collection copies!
- **`Optional<T>`:** Monadic container for return values. Use `orElseGet(Supplier)` instead of eager `orElse(val)`. Never use as fields, method params, or `Serializable` types.
- **`java.time` (JSR-310):** Immutable & thread-safe (`LocalDate`, `Instant`, `ZonedDateTime`, `DateTimeFormatter`) replacing mutable, non-thread-safe `Date` & `SimpleDateFormat`.
- **Interfaces:** `default` methods preserve binary compatibility when evolving interfaces (`Collection.stream()`); `static` methods provide helpers without inheritance.
- **`CompletableFuture`:** Non-blocking composable async DAGs (`thenApply`, `thenCompose`, `thenCombine`, `allOf`) — always supply a custom `Executor` for blocking I/O.
- **JVM / Concurrency:** PermGen → Native **Metaspace**; `HashMap` linked-list bins treeify at **8** (table capacity ≥ 64); `ConcurrentHashMap` drops Segments for **CAS + per-bin lock**.

---

## 📂 Feature Folders

| # | Feature Folder | Core Concepts Covered | Notes | Codes | Interview |
|---:|---|---|---|---|---|
| **01** | [**Lambda Expressions & Functional Interfaces**](01-lambda-expressions-and-functional-interfaces/) | `@FunctionalInterface`, all 43 `java.util.function` types, 4 Method Ref forms, `this` scoping, `invokedynamic` bytecode | [Note](01-lambda-expressions-and-functional-interfaces/notes/lambda-and-functional-interfaces-deep-dive.md) | [Code](01-lambda-expressions-and-functional-interfaces/codes/lts/java08/lambdas/LambdaAndFunctionalInterfacesDemo.java) | [Q&A](01-lambda-expressions-and-functional-interfaces/interview/INTERVIEW_QUESTIONS.md) |
| **02** | [**Stream API (Exhaustive Mastery)**](02-stream-api-exhaustive/) | Creation (10+ ways), Lazy loop fusion, `AbstractPipeline`/`Sink`, ALL Intermediate & Terminal ops, ALL `Collectors`, Primitives, Parallel pitfalls | [3 Notes](02-stream-api-exhaustive/notes/) | [4 Labs](02-stream-api-exhaustive/codes/lts/java08/streams/) | [Q&A](02-stream-api-exhaustive/interview/INTERVIEW_QUESTIONS.md) |
| **03** | [**Optional Class**](03-optional-class/) | `of`/`ofNullable`/`empty`, `map`/`flatMap`/`filter`, `orElse` vs `orElseGet`, anti-patterns | [Note](03-optional-class/notes/optional-complete-guide.md) | [Code](03-optional-class/codes/lts/java08/optional/OptionalCompleteGuideDemo.java) | [Q&A](03-optional-class/interview/INTERVIEW_QUESTIONS.md) |
| **04** | [**Date & Time API (JSR-310)**](04-date-and-time-api-jsr310/) | `LocalDate`/`Instant`/`ZonedDateTime`, `Period` vs `Duration`, DST, `Clock`, thread-safe formatting | [Note](04-date-and-time-api-jsr310/notes/java-time-jsr310-deep-dive.md) | [Code](04-date-and-time-api-jsr310/codes/lts/java08/datetime/DateAndTimeJsr310Demo.java) | [Q&A](04-date-and-time-api-jsr310/interview/INTERVIEW_QUESTIONS.md) |
| **05** | [**Interface Default & Static Methods**](05-interface-default-static-methods/) | Binary compatibility, 3 Diamond conflict resolution rules, `static` utilities, Abstract Class vs Interface | [Note](05-interface-default-static-methods/notes/default-and-static-interface-methods.md) | [Code](05-interface-default-static-methods/codes/lts/java08/interfaces/DefaultAndStaticMethodsDemo.java) | [Q&A](05-interface-default-static-methods/interview/INTERVIEW_QUESTIONS.md) |
| **06** | [**CompletableFuture Async Pipelines**](06-completablefuture-async-pipelines/) | `supplyAsync`, `thenApply` vs `thenCompose`, `thenCombine`, `allOf`/`anyOf`, error recovery, custom Executors | [Note](06-completablefuture-async-pipelines/notes/completablefuture-async-pipelines.md) | [Code](06-completablefuture-async-pipelines/codes/lts/java08/async/CompletableFuturePipelinesDemo.java) | [Q&A](06-completablefuture-async-pipelines/interview/INTERVIEW_QUESTIONS.md) |
| **07** | [**Base64, Metaspace, CHM V8 & Annotations**](07-nashorn-base64-and-jvm-metaspace/) | `Base64` (Std/URL/MIME), PermGen → Metaspace, `@Repeatable`, `HashMap` treeification, `ConcurrentHashMap` CAS, `LongAdder`, Nashorn lifecycle | [Note](07-nashorn-base64-and-jvm-metaspace/notes/metaspace-base64-chm-java8.md) | [Code](07-nashorn-base64-and-jvm-metaspace/codes/lts/java08/runtime/Java8Base64ConcurrencyAndAnnotationsDemo.java) | [Q&A](07-nashorn-base64-and-jvm-metaspace/interview/INTERVIEW_QUESTIONS.md) |
