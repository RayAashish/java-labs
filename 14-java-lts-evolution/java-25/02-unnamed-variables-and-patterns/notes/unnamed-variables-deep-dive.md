# Unnamed Variables & Patterns (`_` — JEP 456) Deep Dive

---

## 🟢 Layer 1 — Noob Track: Declaring Intent to Readers

Compare:
- **Old Java 8–21:**
  ```java
  try {
      return Integer.parseInt(input);
  } catch (NumberFormatException ignored) {
      return 0;
  }
  ```
- **Modern Java 22–25:**
  ```java
  try {
      return Integer.parseInt(input);
  } catch (NumberFormatException _) {
      return 0;
  }
  ```
`_` is an explicit contract telling both maintainers and static analyzers: *"This variable's value is intentionally unused — do NOT accidentally reference it below!"*

---

## 🟡 Layer 2 — Practitioner Track: All 6 Locations & Traps

1. **`catch` Blocks (even multi-catch):** `catch (IOException | IllegalArgumentException _) { ... }`
2. **Lambda Expressions:** `cache.computeIfAbsent(key, _ -> new ArrayList<>());` and `map.forEach((_, val) -> process(val));`
3. **Loops:** `for (String _ : batch) totalProcessed++;` or `for (int i = 0, _ = initStep(); i < n; i++)`
4. **`try`-with-resources Guards (Super Clean!):** `try (var _ = MDC.putCloseable("reqId", id)) { handle(); }` — variable isn't referenced in body, yet `.close()` is guaranteed at exit!
5. **Local Assignment (Side-Effect Discard):** `var _ = deque.removeFirst();`
6. **Unnamed Patterns:** `case Transaction(String id, _, double amt)` (matches any middle component without declaring a variable) or `case Point(int x, int _)` (still checks runtime type if typed!).

### Traps to Avoid!
- ❌ **Method declarations cannot use `_` for parameter names** (`void foo(int _)` fails — public signatures require meaningful docs/names).
- ❌ **Reading `_` fails compile:** `System.out.println(_);` is a compile error (`unnamed variable cannot be referenced`).
- ❌ **Inside Record Patterns:** write bare **`_`** (`Point(var x, _)`) or typed **`int _`** — **`var _` is NOT legal syntax** in patterns!

---

## 🔴 Layer 3 — Senior / Language Design History

- Java 8 originally generated a deprecation warning when naming identifiers `_`, then **Java 9 strictly outlawed `_` as a variable name**, reserving the token purposefully so JEP 456 (Java 22+) could standardize `_` as a pure unnamed variable/pattern without breaking modern source code!
- Note subtle distinction inside Record Patterns:
  - **`_` (unconditional unnamed pattern):** matches ANY value cleanly without introducing a binding.
  - **`String _` (typed unnamed pattern):** still performs the `instanceof String` subtype check (for example rejecting `null`!) even though no variable is bound!
