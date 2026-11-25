# 05. Generics, Reflection & Annotations — Interview Questions & Answers

---

## 🟢 Beginner Questions

### Q1. What problem do Generics solve?
Generics provide **compile-time type safety** (`List<String>`) and eliminate manual `(String)` casts, catching type mismatches at compile time instead of runtime `ClassCastException`.

### Q2. Can we use primitive types (`List<int>`) in Generics?
No! Type erasure converts generic parameters to `Object` (or bounds), and primitives do not extend `Object`. Always use boxed wrapper types (`List<Integer>`) or primitive streams (`IntStream`).

### Q3. What is Reflection and when is it used?
`java.lang.reflect` lets code inspect classes, fields, methods, constructors, and annotations dynamically at runtime. Frameworks like **Spring, Hibernate, Jackson, and JUnit** rely heavily on it.

### Q4. What are `@Retention` and `@Target` meta-annotations?
- `@Retention` controls lifecycle (`SOURCE`, `CLASS`, `RUNTIME`). Only `RUNTIME` annotations can be read via Reflection.
- `@Target` restricts where an annotation may appear (`TYPE`, `METHOD`, `FIELD`, `PARAMETER`, etc.).

---

## 🟡 Intermediate Questions

### Q5. Why is `List<Integer>` NOT a subtype of `List<Number>` even though `Integer` extends `Number`?
Generics are **invariant**. If `List<Number> nums = ints;` were legal, you could call `nums.add(3.14)` and corrupt a `List<Integer>` with a `Double`. Arrays are covariant (`Number[] = new Integer[2]`), which causes runtime `ArrayStoreException`.

### Q6. Explain PECS (`? extends T` vs `? super T`) with a real code example.
**PECS = Producer Extends, Consumer Super**:
- Use `List<? extends Number>` when **reading** `Number` items out of a producer (`double sum(List<? extends Number> src)`). You **cannot add** to it (except `null`) because the concrete subtype (`List<Double>` vs `List<Integer>`) is unknown.
- Use `List<? super Integer>` when **writing** `Integer` items into a consumer (`void fill(List<? super Integer> dest)`). Reading from it only guarantees `Object`.

### Q7. Explain `<?>`, `<? extends Object>`, and raw `List`.
- `List<?>`: Unbounded wildcard — read-only `Object` view (cannot add anything except `null`); safe!
- Raw `List`: Opts out of generic type checking completely (unsafe, legacy pre-Java 5).

### Q8. `Class.forName("X")` vs `X.class` vs `obj.getClass()`?
`X.class` is resolved statically and does **not** trigger static class initialization (`<clinit>`). `Class.forName("X")` loads the class by name and **executes static initializers immediately** (unless using the 3-arg overload with `initialize = false`).

### Q9. `getMethods()` vs `getDeclaredMethods()`?
`getMethods()` returns all **`public`** methods including **inherited** ones (`Object.toString()`). `getDeclaredMethods()` returns **all visibility modifiers** (`private`, `protected`, package, `public`) declared **solely in that class** (no inherited methods).

### Q10. How do JDK Dynamic Proxies work (`Proxy.newProxyInstance`)?
`Proxy.newProxyInstance(loader, interfaces, invocationHandler)` dynamically generates a class implementing the specified **interfaces** at runtime. Every method invocation forwards to `InvocationHandler.invoke(proxy, method, args)`, letting frameworks wrap target calls with transactions, caching, retries, or security checks.

---

## 🔴 Senior / JVM Internals Questions

### Q11. What is Type Erasure, and what are Bridge Methods?
`javac` removes generic type arguments after checking them, replacing `<T>` with `Object` (or its leftmost bound `<T extends Comparable>` -> `Comparable`).
When `class User implements Comparable<User>` defines `compareTo(User u)`, erasure turns the interface method into `compareTo(Object o)`. To make polymorphism work at runtime, `javac` silently generates a synthetic **bridge method** `public int compareTo(Object o) { return compareTo((User) o); }`!

### Q12. Why are `new T()`, `new T[10]`, `static T field`, and `catch (MyGenericException<T> e)` illegal in Java?
- `new T()` / `new T[10]`: Erasure strips `T`, so runtime doesn't know what class or array type to allocate.
- `static T`: One static field is shared by `Box<String>` and `Box<Integer>` (same erased class!).
- `catch (GenericEx<T>)`: JVM catch blocks check runtime types via Exception Table, which cannot distinguish erased type arguments (throws/generics on `Throwable` subclasses are forbidden).

### Q13. What is Heap Pollution and when is `@SafeVarargs` valid?
Heap pollution happens when a variable of a parameterized type points to an object that isn't of that type — commonly triggered by mixing varargs (`T...`) with generics (`List<String>...`), because `javac` creates an underlying `Object[]` array. `@SafeVarargs` suppresses unchecked warnings on `final`, `static`, or `private` methods ONLY when the method never exposes the underlying array reference or stores wrong types into it.

### Q14. Why does `field.setAccessible(true)` fail on JDK classes in Java 17+ (`JEP 403`)?
**Strong Encapsulation of JDK Internals.** JPMS restricts reflective access into non-exported/non-opened JDK packages like `java.base/java.util`. Accessing private fields of JDK classes throws `InaccessibleObjectException` unless explicitly granted via `--add-opens java.base/java.util=ALL-UNNAMED`.

### Q15. JDK Dynamic Proxy vs CGLIB / ByteBuddy (Spring AOP Interview Classic!)
- **JDK Dynamic Proxy:** Built into JDK; works **only on interfaces**; delegates via `InvocationHandler`.
- **CGLIB / ByteBuddy:** Subclasses the target concrete class dynamically via bytecode generation — works without interfaces, **which is why `final` classes and `final` methods cannot be intercepted by Spring `@Transactional` / `@Async`**! (Note: Spring Boot 2+/3+ defaults to CGLIB `proxyTargetClass=true`.)

---

## 🧩 Tricky Output & Compile Puzzles

1. `new ArrayList<String>().getClass() == new ArrayList<Integer>().getClass()` -> **`true`** (same erased runtime `ArrayList.class`).
2. `void foo(List<String> a)` and `void foo(List<Integer> b)` in the same class -> **Compile Error** (both erase to `foo(List)`!).
3. `List<? extends Number> list = new ArrayList<Integer>(); list.add(1);` -> **Compile Error** (cannot add to `? extends` except `null`).
4. `if (obj instanceof List<String>)` -> **Compile Error** (`List<String>` is not reifiable; must write `obj instanceof List<?>`).
5. Calling `method.invoke(null, args)` -> Works ONLY if `method` is **`static`**; throws `NullPointerException` for instance methods!
