# `String`, `Files`, Compact Strings & Collection Factories Deep Dive

---

## 🟢 Layer 1 — Noob Track: Everyday Ergonomics Upgrade

Before Java 9–11, simple everyday tasks needed Apache Commons (`StringUtils`) or Guava (`ImmutableList.of`):
- Creating an unmodifiable list in Java 8: `Collections.unmodifiableList(Arrays.asList("a", "b"))`.
- Creating an unmodifiable list in Java 9+: `List.of("a", "b")`.
- Checking blank text: `str != null && str.trim().isEmpty()` -> `str.isBlank()`.
- Reading a small config file: `new String(Files.readAllBytes(p), StandardCharsets.UTF_8)` -> `Files.readString(p)`.

---

## 🟡 Layer 2 — Practitioner Track: Key Rules & Traps

### 1. `List.of` / `Arrays.asList` / `Collections.unmodifiableList` Comparison

| Property | `Arrays.asList("a", "b")` | `List.of("a", "b")` | `List.copyOf(list)` |
|---|---|---|---|
| `add()` / `remove()` | ❌ `UnsupportedOperationException` | ❌ `UnsupportedOperationException` | ❌ `UnsupportedOperationException` |
| `set(i, val)` | ✅ **Mutates backing array!** | ❌ Immutable | ❌ Immutable |
| Allows `null` elements | ✅ Allowed | ❌ Throws `NullPointerException` | ❌ Throws `NullPointerException` |
| Backed by source changes? | ✅ Writes pass through to source array! | ❌ Independent immutable copy | Skips copy if source is already unmodifiable! |

### 2. `strip()` vs `trim()` & `Map.of` > 10 Pairs
- `String.strip()` respects Unicode whitespace categories (such as `\u2005` Four-Per-Em Space and `\u3000` Ideographic Space), whereas `String.trim()` blindly checks `ch <= '\u0020'`.
- `Map.of(k1, v1, ..., k10, v10)` supports up to **10 pairs**; for >10 entries use **`Map.ofEntries(Map.entry(k, v), ...)`** (rejects duplicate keys with `IllegalArgumentException`!).

---

## 🔴 Layer 3 — Senior / JVM Track: Compact Strings (JEP 254)

- In Java 8, `String` wrapped `private final char value[]` (UTF-16: **every ASCII character wasted 2 bytes**, consuming 25–40% of typical Spring/JSON microservice heap in redundant zero upper bytes).
- **Java 9+ Internal Change:** `private final byte[] value; private final byte coder;`
  - If all characters fit in ISO-8859-1 (`<= 0xFF`), `coder = LATIN1 (0)` — stored at **1 byte/char**.
  - If any character exceeds Latin-1 (e.g., `"₹"`, emojis, CJK), `coder = UTF16 (1)` — 2 bytes/char cleanly handled by JVM intrinsics (`-XX:+CompactStrings` enabled by default).
- **G1 String Deduplication (`-XX:+UseStringDeduplication`):** Combined with G1GC, lets identical live `String` instances share the same underlying `byte[] value` array transparently!
