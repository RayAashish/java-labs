# 01. Lambdas & Functional Interfaces — Deep Dive

---

## 🟢 Layer 1 — Noob Intuition: Functions as First-Class Data

Before Java 8, you could easily pass numbers (`42`) and text (`"Java"`) into methods — **but you couldn't pass an action/behavior** without wrapping it inside a clunky anonymous inner class:

```java
// ❌ OLD PRE-JAVA 8 WAY (6 lines just to pass 1 line of logic!)
Comparator<String> byLengthOld = new Comparator<String>() {
    @Override
    public int compare(String a, String b) {
        return Integer.compare(a.length(), b.length());
    }
};

// ✅ MODERN LAMBDA WAY (clean, readable, zero ceremony)
Comparator<String> byLength = (a, b) -> Integer.compare(a.length(), b.length());
// Or even cleaner with Method Reference:
Comparator<String> byLengthRef = Comparator.comparingInt(String::length);
```

A **Lambda expression** is simply a concise anonymous function `(parameters) -> body` that implements a **`@FunctionalInterface`** (an interface with **exactly ONE abstract method**).

---

## 🟡 Layer 2 — Practitioner Catalog: `java.util.function`

Instead of inventing custom single-method interfaces everywhere, Java provides **43 standard functional interfaces** in `java.util.function`. Master these core archetypes and you know all 43:

| Core Interface | Signature | Purpose | Common Use Site |
|---|---|---|---|
| **`Predicate<T>`** | `boolean test(T t)` | Boolean condition / test | `stream.filter(...)`, `list.removeIf(...)` |
| **`BiPredicate<T,U>`** | `boolean test(T t, U u)` | Two-arg condition | Validation rules |
| **`Function<T, R>`** | `R apply(T t)` | Transform `T` to `R` | `stream.map(...)`, `map.computeIfAbsent(...)` |
| **`BiFunction<T,U,R>`** | `R apply(T t, U u)` | Combine two inputs into `R` | `map.merge(...)`, `map.replaceAll(...)` |
| **`UnaryOperator<T>`** | `T apply(T t)` | Transform `T -> T` (same type) | `list.replaceAll(String::trim)` |
| **`BinaryOperator<T>`** | `T apply(T a, T b)` | Fold two `T` into one `T` | `stream.reduce(0, Integer::sum)` |
| **`Consumer<T>`** | `void accept(T t)` | Perform side effect (no return) | `list.forEach(...)`, `opt.ifPresent(...)` |
| **`BiConsumer<T,U>`** | `void accept(T t, U u)` | Side effect on 2 args | `map.forEach((k, v) -> ...)` |
| **`Supplier<T>`** | `T get()` | Lazy generator / factory (0 args) | `opt.orElseGet(...)`, `Stream.generate(...)` |

### Primitive Specializations (Avoid Autoboxing Overhead!)
If processing millions of primitives, `Function<Integer, Integer>` boxes every `int` into a 16-byte Heap `Integer`! Use:
- `IntPredicate`, `LongPredicate`, `DoublePredicate`
- `IntFunction<R>`, `ToIntFunction<T>`, `IntUnaryOperator`, `IntBinaryOperator`
- `IntConsumer`, `IntSupplier`, `BooleanSupplier`

### The 4 Method Reference (`::`) Forms
1. **Static method:** `Integer::parseInt` $\equiv$ `s -> Integer.parseInt(s)`
2. **Bound instance method:** `System.out::println` $\equiv$ `x -> System.out.println(x)`
3. **Unbound instance method (1st param is target):** `String::toUpperCase` $\equiv$ `s -> s.toUpperCase()`
4. **Constructor reference:** `ArrayList::new` $\equiv$ `() -> new ArrayList<>()` (or `int[]::new` $\equiv$ `size -> new int[size]`)

---

## 🔴 Layer 3 — Senior / JVM Internals: `invokedynamic` & Effectively Final

### 1. Why Lambdas Beat Anonymous Inner Classes in JVM Performance
- **Anonymous Inner Class:** `javac` generates a physical `Outer$1.class` file on disk, loads a separate `Class<?>` into Metaspace, and executes `new Outer$1()` on every call. Inside it, **`this` refers to the anonymous class**, not the enclosing class.
- **Lambda Expression:** `javac` compiles the lambda body into a private synthetic method (`lambda$main$0`) inside the enclosing class and emits **`invokedynamic`** calling `LambdaMetafactory.metafactory(...)` once at first invocation. Zero extra `.class` files on disk, stateless lambdas cache a **single singleton instance**, and **`this` refers to the enclosing outer class**!

### 2. Why Captured Local Variables Must Be `final` or `Effectively Final`
Local variables live on the **Thread Stack** and vanish when the method exits. A lambda can outlive the method (e.g., submitted to an `ExecutorService`), so it captures local variables **by value** into synthetic arguments. Disallowing mutation (`count++` inside lambda) prevents the Stack copy and Heap/lambda copy from diverging into race conditions! *(Instance/static fields CAN be modified because `this` or the class pointer is captured instead.)*
