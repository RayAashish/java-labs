# 01. Lambdas, Functional Interfaces & `invokedynamic`

---

## 🟢 Layer 1 — Noob Track: Passing Behavior as Data

Before Java 8, passing a small behavior (like sorting rule or click listener) required writing a verbose **anonymous inner class**:
```java
Runnable r = new Runnable() {
    @Override
    public void run() {
        System.out.println("Running task!");
    }
};
```
A **Lambda expression** removes that boilerplate because `Runnable` has **only one abstract method**:
```java
Runnable r = () -> System.out.println("Running task!");
```

### The Big 4 Built-in Functional Interfaces (`java.util.function`)
| Interface | Abstract Method | Purpose | Example |
|---|---|---|---|
| `Predicate<T>` | `boolean test(T t)` | Filter / validation | `s -> s.length() > 3` |
| `Function<T, R>` | `R apply(T t)` | Transform `T -> R` | `String::length` |
| `Consumer<T>` | `void accept(T t)` | Side effect (print, log, save) | `System.out::println` |
| `Supplier<T>` | `T get()` | Lazy factory / generator | `ArrayList::new` |

*(Plus `UnaryOperator<T>` = `Function<T,T>`, `BinaryOperator<T>` = `BiFunction<T,T,T>`, and primitive specializations `IntPredicate`, `ToIntFunction` to avoid boxing!)*

---

## 🟡 Layer 2 — Practitioner Track: Method References & Composition

### 4 Kinds of Method References (`::`)
1. **Static method:** `Integer::parseInt` (`s -> Integer.parseInt(s)`)
2. **Bound instance method:** `System.out::println` (`x -> System.out.println(x)`)
3. **Unbound instance method:** `String::toUpperCase` (`s -> s.toUpperCase()`)
4. **Constructor reference:** `ArrayList::new` (`() -> new ArrayList<>()`)

### Composing Functions & Predicates
```java
Predicate<String> nonNull = Objects::nonNull;
Predicate<String> nonBlank = s -> !s.isBlank();
Predicate<String> validText = nonNull.and(nonBlank);

Function<Integer, Integer> add2 = x -> x + 2;
Function<Integer, Integer> times3 = x -> x * 3;
add2.andThen(times3).apply(5); // (5 + 2) * 3 = 21
add2.compose(times3).apply(5); // (5 * 3) + 2 = 17
```

---

## 🔴 Layer 3 — Senior / JVM Internals Track: `invokedynamic` & `this` Semantics

| Dimension | Anonymous Inner Class | Lambda Expression |
|---|---|---|
| **Bytecode / `.class` Files** | Generates `Outer$1.class` on disk | **No extra `.class` file**; emits `invokedynamic` + `LambdaMetafactory` |
| **Memory Allocation** | Allocates a new object every time executed | Stateless lambdas reuse a **single cached singleton** instance |
| **Meaning of `this`** | Refers to the anonymous class instance itself | Refers to the **enclosing outer class** instance |
| **Shadowing** | Can declare variables shadowing outer locals | Shares enclosing scope; cannot redeclare enclosing local variable names |
| **State** | Can hold mutable instance fields & implement multi-method interfaces | Must target a `@FunctionalInterface` (exactly 1 abstract method) |
