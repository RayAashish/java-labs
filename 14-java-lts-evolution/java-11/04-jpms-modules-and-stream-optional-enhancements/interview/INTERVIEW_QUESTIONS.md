# Interview Questions — JPMS, Private Interface Methods & Java 9–11 Upgrades

## 🟢 Beginner

### Q1. Why can interfaces have `private` methods since Java 9?
When two or more `default` (or `static`) methods share helper logic, extracting a **`private`** method prevents duplicating code while keeping the helper hidden from implementing classes and external callers.

### Q2. What is the difference between `takeWhile` and `filter`?
`filter` checks **every single element** from start to end (`List.of(10, 40, 20)` filtered `< 30` yields `[10, 20]`). `takeWhile` acts like a `break` — it stops **permanently** the moment it hits an element failing the predicate (`[10]`).

---

## 🟡 Intermediate

### Q3. In `module-info.java`, what is the difference between `exports` and `opens`?
- **`exports com.app.api`:** Gives **compile-time + runtime public API access** (does NOT permit deep reflection into `private` fields/constructors).
- **`opens com.app.dto`:** Grants **runtime deep reflection access** (so Jackson/Hibernate/Spring can read private fields via `setAccessible(true)`), WITHOUT allowing other modules to compile against public types if omitted from `exports`.
- Use **`opens ... to ...`** (qualified opens) to restrict deep reflection exclusively to trusted framework modules.

### Q4. When should you use `requires transitive` in `module-info.java`?
When module `A` returns or accepts a type from module `B` in `A`'s **public API methods**! Otherwise callers of `A` would get compile errors unless they also manually add `requires B;`. (`java.sql` uses `requires transitive java.logging` for example.)

---

## 🔴 Senior

### Q5. Explain the Unnamed Module vs Automatic Module migration mechanism.
Jigsaw did not break the entire Java ecosystem overnight:
- JARs left on `--class-path` belong to the **Unnamed Module**, which reads all modules and exports everything (classic behavior).
- Any legacy non-modular JAR placed on `--module-path` becomes an **Automatic Module** whose name derives from its JAR filename (or `Automatic-Module-Name` manifest entry), allowing modular code to depend on legacy third-party libraries incrementally.
