# Interview Questions — Java 8 Lambdas & Functional Interfaces

## 🟢 Beginner

### Q1. What makes an interface a Functional Interface?
It must have **exactly one abstract method** (SAM). It can still contain any number of `default` methods, `static` methods, and public `Object` methods (`equals`, `hashCode`, `toString`). Adding `@FunctionalInterface` instructs `javac` to reject a second abstract method at compile time.

### Q2. Can a Lambda access non-final local variables? Why or why not?
No — captured local variables must be **`final` or effectively final** (initialized once and never reassigned). Local variables live on the thread stack, which may vanish before the lambda runs on another thread, so Java captures a copy of the variable's value. Allowing mutation would create confusing race conditions and stale-copy bugs. (Instance/static fields on the heap CAN be read/mutated, though care must be taken for thread safety.)

---

## 🟡 Intermediate

### Q3. Why does `Comparator<T>` have BOTH `compare` and `equals`, yet still qualify as `@FunctionalInterface`?
Because `equals(Object)` overrides a public method of `java.lang.Object`. Every implementing class implicitly inherits `Object.equals(Object)`, so the Java Language Specification explicitly excludes public `Object` methods from the single-abstract-method count.

### Q4. Explain the difference between `Function.andThen(after)` and `Function.compose(before)`.
- `f.andThen(g)` runs `f` **first**, then feeds `f`'s result into `g`: `g(f(x))`.
- `f.compose(g)` runs `g` **first**, then feeds `g`'s result into `f`: `f(g(x))`.

### Q5. What is the difference between a bound and an unbound instance method reference?
- **Bound (`sysout::println`):** Receiver object is already fixed; all arguments to the functional method become arguments to `println`.
- **Unbound (`String::toUpperCase` or `String::concat`):** The **first** parameter of the functional method becomes the receiver object (`s -> s.toUpperCase()`, or `(a, b) -> a.concat(b)`).

---

## 🔴 Senior / Internals

### Q6. Does Java compile every lambda into an anonymous inner class (`$1.class`)? How does it actually work?
**No.** Generating synthetic `.class` files for every lambda would cause massive permgen/metaspace classloading bloat. Instead, `javac` compiles the lambda body into a private synthetic method (`lambda$...`) and emits an **`invokedynamic`** bytecode instruction calling `LambdaMetafactory.metafactory`. At runtime, the JVM links the call site lazily, reuses a singleton instance for stateless non-capturing lambdas (zero allocation), and lets C2 JIT inline aggressively.

### Q7. How does `this` behave inside a lambda vs inside an anonymous inner class?
In an anonymous inner class, `this` refers to the **anonymous class instance itself**. In a lambda, lambdas do **not** introduce a new lexical object scope — `this` refers to the **enclosing outer object**, and a lambda cannot declare a local variable with the same name as one in the enclosing method.

### Q8. Tricky Output Check: What happens with non-capturing vs capturing instance identity?
For two non-capturing lambdas evaluated in HotSpot (`Runnable r = () -> {};`), HotSpot returns the **same cached singleton** across loop iterations. However, the JLS intentionally leaves lambda object identity (`r1 == r2`) unspecified — never synchronize on or compare lambda references with `==`!
