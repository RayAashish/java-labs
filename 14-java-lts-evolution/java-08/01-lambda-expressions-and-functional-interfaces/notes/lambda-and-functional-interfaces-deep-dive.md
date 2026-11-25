# Lambda Expressions & Functional Interfaces — Deep Dive (Java 8)

---

## 🟢 Layer 1 — Noob Track: Plain-English Intuition

Before Java 8, Java was strictly noun-oriented: to pass *one line of behavior* (like how to sort two strings, or what happens on a button click), you had to invent an entire anonymous class:

```java
// Legacy Java 7: 6 lines just to compare lengths!
Collections.sort(names, new Comparator<String>() {
    @Override
    public int compare(String a, String b) {
        return Integer.compare(a.length(), b.length());
    }
});
```

A **Lambda Expression** strips away the ceremony and keeps only **(inputs) -> action**:

```java
// Java 8: 1 line!
names.sort((a, b) -> Integer.compare(a.length(), b.length()));
// Or with Comparator + Method Reference:
names.sort(Comparator.comparingInt(String::length));
```

---

## 🟡 Layer 2 — Practitioner Track: Rules, Catalog & Traps

### 1. `@FunctionalInterface` & Single Abstract Method (SAM) Rules
An interface is functional if it has **exactly one abstract method**.
Allowed alongside that 1 abstract method:
1. Any number of `default` methods (e.g., `Predicate.and`, `Predicate.negate`).
2. Any number of `static` methods (e.g., `Predicate.isEqual`).
3. Re-declarations of `java.lang.Object` public methods (`boolean equals(Object)` in `Comparator<T>` does NOT count as a second abstract method because every concrete class inherits it from `Object`).

Always annotate with `@FunctionalInterface` so `javac` fails compilation immediately if a teammate adds a second abstract method.

### 2. Core `java.util.function` Built-In Interfaces (No Need to Reinvent!)

| Interface | Signature | Primary Use Case | Composition Methods |
|---|---|---|---|
| `Predicate<T>` | `boolean test(T t)` | Filtering, validation | `.and()`, `.or()`, `.negate()` |
| `Function<T, R>` | `R apply(T t)` | Mapping / transformation | `.andThen()`, `.compose()`, `.identity()` |
| `Consumer<T>` | `void accept(T t)` | Side effects (logging, saving) | `.andThen()` |
| `Supplier<T>` | `T get()` | Lazy evaluation, factories, ID gen | — |
| `UnaryOperator<T>` | `T apply(T t)` | `Function<T, T>` same-type transform | `.andThen()`, `.compose()` |
| `BinaryOperator<T>` | `T apply(T t1, T t2)` | Reduction / combining two values | `.minBy()`, `.maxBy()` |
| `BiPredicate<T, U>` | `boolean test(T, U)` | 2-arg predicate | `.and()`, `.or()`, `.negate()` |
| `BiFunction<T, U, R>` | `R apply(T, U)` | Combining two inputs | `.andThen()` |
| `BiConsumer<T, U>` | `void accept(T, U)` | `Map.forEach((k, v) -> ...)` | `.andThen()` |
| **Primitive Variants** | `IntPredicate`, `ToIntFunction<T>`, `IntUnaryOperator`, `ObjIntConsumer<T>` | **Zero boxing overhead** in hot loops | Avoids `Integer` heap allocations |

### 3. Method References — The 4 Forms

| Form | Syntax | Equivalent Lambda | Example |
|---|---|---|---|
| **Static Method** | `ClassName::staticMethod` | `(x) -> ClassName.staticMethod(x)` | `Integer::parseInt` |
| **Bound Instance** | `instanceExpr::instanceMethod` | `(x) -> instanceExpr.method(x)` | `System.out::println` |
| **Unbound Instance** | `ClassName::instanceMethod` | `(obj, args...) -> obj.method(args...)` | `String::toUpperCase`, `String::concat` |
| **Constructor** | `ClassName::new` | `(args...) -> new ClassName(args...)` | `ArrayList::new`, `String[]::new` |

### 4. Anonymous Inner Class vs Lambda — Crucial Semantic Differences

| Aspect | Anonymous Inner Class | Lambda Expression |
|---|---|---|
| **`this` keyword** | Refers to the anonymous class instance | Refers to the **enclosing class** (`Outer.this`) |
| **Shadowing** | Can declare fields/locals shadowing enclosing variables | Shares enclosing lexical scope — **cannot shadow** enclosing local variable names |
| **State** | Can hold mutable instance fields | Stateless (can only capture enclosing vars) |
| **Arbitrary interfaces/abstract classes** | Works with multi-method interfaces & `abstract class` | Works **only** with Functional Interfaces |
| **Compiled artifact** | Generates `Outer$1.class` on disk | Emits `invokedynamic` call site, generated dynamically at runtime |

### 5. "Effectively Final" Capture Rule
A lambda can read local variables from the enclosing method **only if they are `final` or effectively final** (never reassigned after initialization).
- **Why?** Local variables live on the thread's **Stack Frame**, which disappears when the outer method returns. The lambda may execute *later* on a **different thread** (`ExecutorService`). Java captures a **value copy** into the synthetic lambda object — disallowing reassignment prevents invisible synchronization/stale-copy bugs.

---

## 🔴 Layer 3 — Senior / JVM Architect Track (`invokedynamic`)

When `javac` compiles a lambda:
1. Lambda body becomes a `private static` (non-capturing) or `private` instance (capturing `this`) synthetic method (`lambda$main$0`) inside the enclosing class.
2. At the call site, `javac` emits **`invokedynamic`** pointing to bootstrap method **`java.lang.invoke.LambdaMetafactory.metafactory(...)`**.
3. **First execution only:** The JVM dynamically links and defines the target functional implementation in memory:
   - **Non-capturing lambdas** (e.g., `s -> s.length()`) reuse a **single cached singleton** instance across all calls — **zero GC allocation** per call!
   - **Capturing lambdas** (e.g., `s -> s.startsWith(prefix)`) allocate a lightweight instance holding captured values, which C2 JIT **scalar replacement (Escape Analysis)** can eliminate completely on hot paths.
