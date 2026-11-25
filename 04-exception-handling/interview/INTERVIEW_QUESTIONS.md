# 04. Exception Handling — Interview Questions & Answers

---

## 🟢 Beginner Questions

### Q1. What is the parent class of all Errors and Exceptions?
`java.lang.Throwable` (which directly extends `Object`). Only `Throwable` subclasses can be thrown or caught.

### Q2. Compare `Error` vs `Exception`, and Checked vs Unchecked Exceptions.
- **`Error` (`OutOfMemoryError`, `StackOverflowError`):** Unchecked catastrophic JVM/resource failures — applications should normally NOT catch them.
- **Checked `Exception` (`IOException`, `SQLException`):** Recoverable external failures enforced at compile time (`try/catch` or `throws`).
- **Unchecked `RuntimeException` (`NullPointerException`, `IllegalArgumentException`):** Programming bugs or invalid preconditions; not checked at compile time.

### Q3. `throw` vs `throws` vs `Throwable`?
- `Throwable`: Root class in `java.lang`.
- `throw`: Keyword used inside a block to throw one exception object (`throw new MyException()`).
- `throws`: Method signature clause declaring checked exceptions callers must handle.

### Q4. `final` vs `finally` vs `finalize()`?
- `final`: Modifier (constant variable, non-overridable method, non-extendable class).
- `finally`: Block after `try/catch` for guaranteed cleanup.
- `finalize()`: Deprecated/removed-for-removal `Object` hook formerly invoked by GC before reclaiming an object (unpredictable, performance hazard — replaced by `try-with-resources` & `java.lang.ref.Cleaner`).

---

## 🟡 Intermediate Questions

### Q5. When does a `finally` block NOT execute?
1. `System.exit(int)` or `Runtime.getRuntime().halt(int)` is called inside `try`/`catch`.
2. JVM crashes fatally (`SIGKILL -9`, power failure, severe `VirtualMachineError`).
3. A **daemon thread** runs the `try` block and all non-daemon user threads exit before `finally` runs.
4. Infinite loop or permanent deadlock inside `try`/`catch`.

### Q6. What happens if `try` returns `1` and `finally` returns `2`?
The method returns **`2`**. Worse, if `try` threw an exception and `finally` executes `return`, the exception is **silently swallowed and lost forever**! NEVER place `return` or `throw` inside `finally`.

### Q7. How does `try-with-resources` work, and what are Suppressed Exceptions?
Any class implementing `AutoCloseable` (or `Closeable`) declared in `try (Res r = ...)` is automatically closed in **reverse order of declaration**. If BOTH the `try` body AND `r.close()` throw exceptions, the `try` exception remains the **primary exception**, and the `close()` exception is attached via `primary.addSuppressed(closeEx)` — retrievable with `e.getSuppressed()` (whereas classic `try-finally` lost the primary exception!).

### Q8. Compare `AutoCloseable` vs `Closeable`.
- `java.lang.AutoCloseable` (Java 7): `void close() throws Exception` (general resources).
- `java.io.Closeable` (Java 5, extends `AutoCloseable`): `void close() throws IOException` and is explicitly documented as **idempotent** (calling `close()` twice shouldn't fail).

### Q9. What are the rules for Multi-Catch (`catch (IOException | SQLException e)`)?
1. Caught exception types **cannot have an inheritance (parent-child) relationship** (cannot write `IOException | FileNotFoundException`).
2. The catch variable `e` is **implicitly `final`** and cannot be reassigned inside the block.

### Q10. Why must subclass catch blocks come BEFORE superclass catch blocks?
`catch` blocks are evaluated top-to-bottom. Placing `catch (Exception e)` before `catch (IOException e)` makes the second block **unreachable code** -> compile-time error!

---

## 🔴 Senior / JVM Internals Questions

### Q11. How do JVM Exception Tables work (Zero-Cost Exception Model)?
Normal execution pays **zero runtime overhead** for `try` blocks! Instead, `javac` attaches an **Exception Table** per method storing `(start_pc, end_pc, handler_pc, catch_type)`. Only when `athrow` actually fires does the JVM search the table for matching bytecode index ranges and unwind stack frames cleanly.

### Q12. Why is constructing exceptions expensive ($O(\text{Stack Depth})$), and how do you optimize high-throughput control-flow exceptions?
`new Exception()` calls `Throwable.fillInStackTrace()` (a JNI native call) which pauses the thread to walk every frame on the call stack capturing class names, methods, and line numbers.
- **Rule:** Never use exceptions for routine `if-else` validation in hot loops.
- **Optimization:** For pure signaling exceptions (like internal parser backtracking), override `fillInStackTrace()` to `return this;` (or pass `writableStackTrace = false` to `Throwable`'s 4-arg constructor) — **stackless exceptions** are 50x–100x faster!

### Q13. How do you handle Checked Exceptions inside Java 8 Streams (`Stream.map`)?
Standard functional interfaces (`Function<T, R>`, `Consumer<T>`) do not declare `throws Exception`. Approaches:
1. Wrap into a clean helper method returning `RuntimeException` (or domain exception) with root cause chained (`super(msg, e)`).
2. Return a `Result<T, E>` / `Either` carrier object so failures become data rather than aborting the stream.
3. *(Avoid sneaky-throws in public APIs — it breaks caller expectations by throwing undeclared checked exceptions!)*

### Q14. What should you ALWAYS do when catching `InterruptedException`?
Never swallow it! Either propagate `throws InterruptedException` OR restore the thread's interrupted status immediately before handling:
```java
catch (InterruptedException e) {
    Thread.currentThread().interrupt(); // Preserve interrupt signal for upstream schedulers!
}
```

### Q15. Explain Exception Chaining & Java 14+ Helpful `NullPointerException`s.
Always pass the caught cause (`throw new PaymentException("Gateway timeout", ex)`) so `getCause()` retains the full root stack trace. Since **Java 14 (`JEP 358`)**, the JVM analyzes the exact bytecode instruction that dereferenced `null` and prints the exact variable name (e.g., `Cannot invoke "Address.city()" because the return value of "User.getAddress()" is null`).

---

## 🧩 Tricky Output Puzzles

1. `try { return 1; } finally { return 2; }` -> **Returns `2`**.
2. `int x = 1; try { return x; } finally { x = 2; }` -> **Returns `1`** (`javac` copies primitive `1` into a temp local slot before executing `finally`!).
3. `StringBuilder sb = new StringBuilder("A"); try { return sb; } finally { sb.append("B"); }` -> **Returns `"AB"`** (temp slot copied the reference pointer; `finally` mutated the Heap object!).
4. `try { throw new RuntimeException("A"); } finally { throw new IllegalStateException("B"); }` -> **Throws `"B"`** (`"A"` is completely lost!).
5. `try { System.exit(0); } finally { System.out.println("Hi"); }` -> **Prints nothing** (`finally` never runs).
