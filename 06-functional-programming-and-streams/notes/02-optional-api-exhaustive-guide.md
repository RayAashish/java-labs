# 02. `Optional<T>` Exhaustive Guide — Every Method, Rules & Anti-Patterns

---

## 🟢 Layer 1 — Noob Intuition: Modeling "May Be Absent" Honestly

Before Java 8, methods returned `null` when nothing was found (`userRepo.findById(99) -> null`), forcing callers to remember defensive `if (user != null)` checks everywhere — forgetting one caused **`NullPointerException` (The Billion Dollar Mistake)**.

**`Optional<T>`** is an immutable single-element box that forces the caller to acknowledge absence explicitly at compile time!

---

## 🟡 Layer 2 — Complete `Optional<T>` Method Reference

### 1. Creating an `Optional`
| Method | Behavior on `null` | When to Use |
|---|---|---|
| `Optional.of(val)` | **Throws `NullPointerException` immediately!** | When `val` MUST never be null (fail-fast invariant) |
| `Optional.ofNullable(val)` | Returns `Optional.empty()` cleanly | Wrapping nullable returns from DB/APIs/Maps |
| `Optional.empty()` | Empty box | Representing "not found" cleanly |

### 2. Inspecting & Executing Side Effects
- `opt.isPresent()` / `opt.isEmpty()` *(Java 11+)* -> Avoid pairing with `.get()`; prefer functional methods below!
- `opt.ifPresent(consumer)` -> Runs consumer only if present.
- `opt.ifPresentOrElse(consumer, emptyAction)` *(Java 9+)* -> Handles both branches in one fluent call!

### 3. Transforming & Filtering (`Optional` Pipelines)
- **`opt.map(mapper)`:** Transforms value if present; propagates `Optional.empty()` if absent or if mapper returns `null`.
- **`opt.flatMap(mapperReturningOptional)`:** Unwraps nested `Optional<Optional<R>>` into flat `Optional<R>`!
- **`opt.filter(predicate)`:** Keeps value if predicate is `true`, otherwise turns into `Optional.empty()`.
- **`opt.or(() -> backupOptional)` *(Java 9+)*:** Lazily supplies a fallback `Optional<T>` if empty!
- **`opt.stream()` *(Java 9+)*:** Converts to a 0-or-1 element `Stream<T>` (great for `stream.flatMap(Optional::stream)`!).

### 4. Extracting Values Safely
| Method | Behavior When Empty | Pitfall / Best Practice |
|---|---|---|
| `opt.orElse(defaultVal)` | Returns `defaultVal` | **EAGER TRAP:** Evaluates `defaultVal` expression EVEN WHEN `opt` is present! Only pass constants! |
| `opt.orElseGet(supplier)` | Calls `supplier.get()` | **LAZY:** Preferred whenever fallback allocates or runs logic/I/O! |
| `opt.orElseThrow()` *(Java 10+)* | Throws `NoSuchElementException` | Replaces `.get()` cleanly to show explicit intent |
| `opt.orElseThrow(exSupplier)` | Throws custom exception | Standard REST pattern: `.orElseThrow(() -> new NotFoundException(id))` |

---

## 🔴 Layer 3 — Senior Code-Review Rules (5 Anti-Patterns)

1. **❌ NEVER use `Optional` as a class/entity field:** Not `Serializable`, adds 16B+ heap wrapper per field. Keep fields nullable + return `Optional<T>` from getters if desired.
2. **❌ NEVER use `Optional` as a method parameter:** Forces callers to write `find(Optional.of("a"), Optional.empty())` and still allows `find(null, null)`. Use overloaded methods!
3. **❌ NEVER return `null` from a method returning `Optional<T>`:** Always return `Optional.empty()`.
4. **❌ NEVER write `if (opt.isPresent()) return opt.get(); else return "fallback";`:** Replace with `opt.orElse("fallback")`.
5. **❌ NEVER call `.orElse(createHeavyObject())`:** Always `.orElseGet(() -> createHeavyObject())`.
