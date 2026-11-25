# 02. `Optional<T>` Mastery & Anti-Patterns

---

## 🟢 Layer 1 — Noob Track: Eliminating Blind `NullPointerException`

`Optional<T>` is a **container box** that either holds **one non-null value** or is **empty**. Instead of silently returning `null` and hoping the caller remembers `if (user != null)`, returning `Optional<User>` forces the caller to explicitly handle the "not found" case!

```java
Optional<User> maybeUser = userRepo.findById(42);
String name = maybeUser
        .map(User::name)
        .orElse("Guest");
```

---

## 🟡 Layer 2 — Practitioner Track: Fluent Transformations & Traps

### 1. The `orElse` vs `orElseGet` Eager Trap (Classic Interview Question!)
```java
// TRAP: computeDefault() ALWAYS executes EVEN WHEN opt has a value!
String a = opt.orElse(computeDefault());

// FIX: Supplier executes LAZILY only when opt is empty!
String b = opt.orElseGet(() -> computeDefault());
```
Use `orElse(CONSTANT)` only for pre-existing constants (`"Guest"`, `0`, `List.of()`); use `orElseGet(...)` for anything that allocates or hits I/O!

### 2. `map()` vs `flatMap()`
- Use `map()` when the mapper returns a plain `R` (`user -> user.email()`).
- Use `flatMap()` when the mapper itself returns `Optional<R>` (`user -> user.findPrimaryAddress()`), preventing `Optional<Optional<Address>>`!

### 3. Java 9+ Ergonomics
- `opt.ifPresentOrElse(val -> ..., () -> ...)`
- `opt.or(() -> fallbackRepo.find(id))` (fallback `Optional`)
- `opt.stream()` (turns `Optional<T>` into a 0-or-1 element `Stream<T>`)
- `opt.orElseThrow()` (preferred over `.get()` since Java 10 — signals explicit intent!)

---

## 🔴 Layer 3 — Senior Track: 5 `Optional` Anti-Patterns in Code Reviews

1. **NEVER use `Optional` as an Entity / DTO field:** `Optional` is **not `Serializable`** and adds an extra 16-byte Heap wrapper per field.
2. **NEVER use `Optional` as a method parameter:** It forces callers to wrap arguments (`foo(Optional.of(x))`) and still permits `foo(null)`! Use overloaded methods or `@Nullable`.
3. **NEVER call `Optional.of(nullable)` without checking:** Throws NPE immediately; use `Optional.ofNullable(x)`.
4. **NEVER write `if (opt.isPresent()) return opt.get(); else return defaultVal;`:** That's just `null`-checking with extra steps! Use `map`/`orElseGet`/`orElseThrow`.
5. **NEVER return `null` from a method whose return type is `Optional<T>`:** Always return `Optional.empty()`.
