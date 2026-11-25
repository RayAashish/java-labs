# Text Blocks, Formatting & Java 12–16 Stream Gems — Deep Dive

---

## 🟢 Layer 1 — Noob Track: Clean JSON & SQL Without `\n\"` Clutter

**Old Java 8 SQL:**
```java
String sql = "SELECT id, email\n" +
             "FROM users\n" +
             "WHERE status = \"ACTIVE\"";
```
**Java 15/17 Text Block:**
```java
String sql = """
        SELECT id, email
        FROM users
        WHERE status = "ACTIVE" AND tenant = '%s'
        """.formatted("acme");
```
Zero `+` concatenation, zero `\"` escapes, zero `\n` noise — AND it produces a plain ordinary compile-time **`java.lang.String`**!

---

## 🟡 Layer 2 — Practitioner Track: Whitespace Rules & Java 12–16 API Upgrades

### 1. How Closing `"""` Controls Indentation
`javac` aligns whitespace using the minimum indentation across all non-blank content lines AND the closing `"""` line:
- Putting closing `"""` **aligned with the content**: zero extra leading margin.
- Pushing closing `"""` **2 columns left of content**: adds 2 leading spaces to every line cleanly!
- Trailing spaces at the end of any line are **stripped automatically** by the compiler unless protected by `\s`.

### 2. Three High-Value Stream Upgrades Between Java 11 and Java 17
| Method | Version | Why It Beats Older Code |
|---|---|---|
| `Collectors.teeing(c1, c2, merger)` | Java 12 | Runs **two** downstream collectors (`min` + `max`, or `sum` + `count`) in a **single stream traversal** instead of consuming the list twice! |
| `stream.toList()` | Java 16 | Concise unmodifiable list directly on `Stream`; note `stream.toList()` **permits `null`** elements whereas `Collectors.toUnmodifiableList()` throws `NullPointerException` on `null`! |
| `stream.mapMulti((elem, consumer) -> ...)` | Java 16 | Emits `0`, `1`, or many values imperatively into a `Consumer` without allocating tiny intermediate `Stream.of(...)` objects per element like `flatMap`! |

---

## 🔴 Layer 3 — Senior / JVM Track

- **Compile-Time Zero Runtime Cost:** Text block stripping (`stripIndent()`) happens **inside `javac` during compilation** — the constant pool stores the final formatted UTF-8/UTF-16 string literal identically to normal string literals, eligible for String Pool interning!
- **`mapMulti` vs `flatMap` Performance:** Calling `.flatMap(x -> x == null ? Stream.empty() : Stream.of(x))` across 10 million elements allocates millions of transient `ReferencePipeline` instances. `mapMulti` pushes directly into the downstream `Sink` with **zero intermediate `Stream` allocations**!
