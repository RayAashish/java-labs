# Interview Questions — Java 8 `Optional<T>`

## 🟢 Beginner

### Q1. When should you use `Optional.of(x)` vs `Optional.ofNullable(x)`?
- **`Optional.of(x)`:** Throws `NullPointerException` immediately if `x == null`. Use when null indicates a programming bug that should fail fast.
- **`Optional.ofNullable(x)`:** Returns `Optional.empty()` safely when `x == null`. Use when wrapping potentially missing return values.

### Q2. What is the difference between `Optional.map()` and `Optional.flatMap()`?
- `map` transforms a value via `T -> U` and automatically wraps `U` into `Optional<U>`.
- `flatMap` transforms via `T -> Optional<U>` and keeps it flat as `Optional<U>` instead of `Optional<Optional<U>>`.

---

## 🟡 Intermediate

### Q3. Why is `orElseGet(Supplier)` safer and faster than `orElse(methodCall())`?
`orElse(T)` evaluates its argument expression **eagerly before `orElse` is even invoked**. Even if the `Optional` already contains a value, `methodCall()` still runs (wasting DB/API/allocation work or triggering side effects!). `orElseGet(Supplier)` executes lazily **only if empty**. Note: for cheap pre-existing constants (`orElse("DEFAULT")`), `orElse` is fine because allocating a lambda would cost more than reusing the constant string.

### Q4. Why shouldn't `Optional` be used as a JPA `@Entity` field or method parameter?
1. `Optional` does **not** implement `java.io.Serializable` (breaking RMI, HTTP sessions, Hibernate caching).
2. Frameworks like Jackson / Hibernate expect nullable entity fields cleanly (extra wrapper object per row adds memory pressure).
3. As method parameters, `Optional<T>` forces callers to write `find(Optional.of("foo"))` and still lets someone pass `find(null)`. Prefer method overloads instead!

---

## 🔴 Senior

### Q5. `Optional.of(x).map(fn)` — what happens if `fn` itself returns `null`?
`Optional.map(fn)` checks the return value and safely converts a `null` return from `fn` into **`Optional.empty()`** rather than throwing NPE! By contrast, `flatMap(fn)` **throws `NullPointerException`** if the `fn` itself returns a `null` reference instead of `Optional.empty()`.
