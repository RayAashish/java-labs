# `Optional<T>` — Complete Guide & Anti-Patterns (Java 8+)

---

## 🟢 Layer 1 — Noob Track: Plain-English Intuition

Think of `Optional<T>` as a sealed cardboard box returned by a method:
- Either the box contains a real non-null value, or the box itself is explicitly marked empty (`Optional.empty()`).
- Instead of opening the box recklessly (`opt.get()` — crashes with `NoSuchElementException`), you politely chain `.map(...)`, `.filter(...)`, `.orElseGet(...)`.

---

## 🟡 Layer 2 — Practitioner Track: Full API & Critical Rules

### 1. Construction Methods
- `Optional.empty()` — empty singleton instance.
- `Optional.of(value)` — throws `NullPointerException` immediately if `value == null` (fail-fast!).
- `Optional.ofNullable(value)` — returns `Optional.of(value)` if non-null, else `Optional.empty()`.

### 2. Transformation & Extraction
- **`map(fn)` vs `flatMap(fn)`:**
  - `map(User::getName)`: function returns plain `R`, wrapped automatically into `Optional<R>`.
  - `flatMap(User::getAddress)`: function ALREADY returns `Optional<Address>` — avoids nested `Optional<Optional<Address>>`!
- **`filter(pred)`:** turns present value into `Optional.empty()` if predicate is `false`.
- **Extraction (Ordered Safe -> Risky):**
  1. `orElse(defaultVal)` — returns `defaultVal` if empty (**EAGERLY** evaluated!).
  2. `orElseGet(Supplier)` — invokes `Supplier` **ONLY IF empty** (**LAZY** — always use when fallback invokes a method or allocates!).
  3. `orElseThrow()` *(Java 10)* / `orElseThrow(Supplier<X>)` — throws custom domain exception.
  4. `ifPresent(Consumer)` / `ifPresentOrElse(Consumer, Runnable)` *(Java 9)*.
  5. `opt.stream()` *(Java 9)* — turns `Optional<T>` into 0-or-1 element `Stream<T>` for clean `stream.flatMap(Optional::stream)`.
  6. ❌ Avoid `.get()` directly in application code!

### 3. The 6 Golden `Optional` Anti-Patterns
1. ❌ `if (opt.isPresent()) return opt.get(); else return fallback;` — exact equivalent of `if (x != null)`! Use `opt.orElse(fallback)`.
2. ❌ `private Optional<String> middleName;` — `Optional` is **not `Serializable`** (breaks Java Serialization & creates extra heap allocations per entity) — store nullable field and return `Optional.ofNullable(middleName)` from getter!
3. ❌ Passing `Optional<T>` as a method parameter — forces callers to wrap arguments in boxes instead of method overloading.
4. ❌ Returning `Optional<List<Order>>` — just return `List.of()` (empty collection)!
5. ❌ Returning `null` from a method typed `Optional<T>` — defeats the entire contract.
6. ❌ Using `orElse(dbLookup())` instead of `orElseGet(() -> dbLookup())`.

---

## 🔴 Layer 3 — Senior / JVM Track

- `Optional.empty()` returns one canonical static `EMPTY` singleton (`new Optional<>(null)`).
- Every non-empty `Optional<T>` allocates a tiny 16-byte wrapper object on the heap unless C2 JIT scalar-replaces it via **Escape Analysis** when used purely inside a local pipeline. (Returning `Optional` in tight numeric loops allocates wrapper objects — prefer `OptionalInt`, `OptionalLong`, `OptionalDouble` for primitive math results.)
- Future Project Valhalla (`Value Classes`) aims to make `Optional` a heap-allocation-free value class!
