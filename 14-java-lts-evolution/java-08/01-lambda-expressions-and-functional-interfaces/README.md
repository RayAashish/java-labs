# Java 8 — Lambda Expressions & Functional Interfaces

## ⚡ 2-Minute Revision
- **What:** Concise syntax `(params) -> body` to pass **behavior as data** without 6-line anonymous inner class boilerplate.
- **Requirement:** Target type MUST be a **Functional Interface** (exactly **1** abstract method, optionally marked `@FunctionalInterface`). `default`, `static`, and `Object` (`equals`, `hashCode`, `toString`) overrides do NOT count toward the 1-SAM limit.
- **4 Core Families:** `Predicate<T>` (`T -> boolean`), `Function<T,R>` (`T -> R`), `Consumer<T>` (`T -> void`), `Supplier<T>` (`() -> T`), plus `UnaryOperator<T>`, `BinaryOperator<T>`, `Bi*`, and primitive unboxed forms (`IntPredicate`, `ToIntFunction<T>`, etc.).
- **Method References (4 kinds):** Static (`Class::staticMethod`), Unbound Instance (`Class::instanceMethod`), Bound Instance (`instance::method`), Constructor (`Class::new`).
- **JVM Under the Hood:** Uses **`invokedynamic`** + `LambdaMetafactory` instead of eagerly generating `Foo$1.class` files on disk, keeping memory footprint small and JIT inlining fast.

## 📂 Contents
- [`notes/lambda-and-functional-interfaces-deep-dive.md`](notes/lambda-and-functional-interfaces-deep-dive.md)
- [`codes/lts/java08/lambdas/LambdaAndFunctionalInterfacesDemo.java`](codes/lts/java08/lambdas/LambdaAndFunctionalInterfacesDemo.java)
- [`interview/INTERVIEW_QUESTIONS.md`](interview/INTERVIEW_QUESTIONS.md)
