# 02. Strings, Arbitrary-Precision Math & Modern Date/Time (`java.time`)

> **Why this module exists:** Every real backend app manipulates text, money, and timestamps. Using `==` on Strings, `+` inside loops, `double` for currency, or `SimpleDateFormat` across threads are four of the most common production bugs in Java.

---

## ⚡ 2-Minute Rapid Revision Card

- **`String` Immutability:** `String` is `final`, backed by `private final byte[] value` (Java 9+ **Compact Strings**: `LATIN1` 1 byte/char or `UTF16` 2 bytes/char, replacing `char[]` to cut heap usage up to 50%). Thread-safe, cacheable `hash`, safe for String Constant Pool & Map keys.
- **String Pool (`intern()`):** Literals `"Java"` live in the String Table (inside Heap since Java 7, not PermGen). `new String("Java")` forces a separate Heap object; `.intern()` returns the canonical pooled reference.
- **`String` vs `StringBuilder` vs `StringBuffer`:** `String` (immutable), `StringBuilder` (mutable, non-synchronized, fastest — use in single-threaded loops), `StringBuffer` (mutable, `synchronized` methods — legacy).
- **`BigDecimal` Golden Rules:** ALWAYS construct from `String` (`new BigDecimal("0.1")`) or `BigDecimal.valueOf(0.1)` — NEVER `new BigDecimal(0.1)`! Use `.compareTo()` for numeric equality (`1.0` vs `1.00`: `equals()` is `false`, `compareTo()` is `0`).
- **`java.time` (JSR-310):** All classes (`LocalDate`, `Instant`, `ZonedDateTime`, `DateTimeFormatter`) are **immutable and thread-safe**. Store/transmit UTC `Instant`; convert to `ZonedDateTime` only at display edges.

---

## 🧭 Progressive Learning Path

- **🟢 Layer 1 (Noob):** Learn why `String` cannot be modified in place, how `StringBuilder` builds loops cleanly, why `0.1 + 0.2 != 0.3`, and how to use `LocalDate` & `LocalDateTime`.
- **🟡 Layer 2 (Practitioner):** Master `String` Text Blocks (`"""`), `Pattern`/`Matcher`, `BigDecimal` rounding modes (`HALF_EVEN`), `Period` vs `Duration`, and `ZonedDateTime` DST transitions.
- **🔴 Layer 3 (Senior / JVM):** Inspect Java 9+ Compact Strings (`coder` byte), `StringConcatFactory` (`invokedynamic` since Java 9 replacing `StringBuilder` bytecode), String deduplication (`-XX:+UseStringDeduplication`), and ReDoS backtracking traps.

---

## 📂 Contents

| File | Focus |
|---|---|
| [`notes/01-strings-pool-compact-strings-and-builders.md`](notes/01-strings-pool-compact-strings-and-builders.md) | Immutability, String Pool, Compact Strings, `StringBuilder`, Text Blocks |
| [`notes/02-regex-pattern-matcher-and-redos.md`](notes/02-regex-pattern-matcher-and-redos.md) | Regex API, pre-compiled `Pattern`, capturing groups, ReDoS prevention |
| [`notes/03-bigdecimal-biginteger-and-financial-math.md`](notes/03-bigdecimal-biginteger-and-financial-math.md) | IEEE-754 flaws, `BigDecimal` traps, `RoundingMode`, `BigInteger` |
| [`notes/04-modern-datetime-jsr310.md`](notes/04-modern-datetime-jsr310.md) | `Instant`, `ZonedDateTime`, `Period` vs `Duration`, `DateTimeFormatter` |
| [`codes/`](codes/) | 4 runnable Java labs verifying String Pool, Regex, `BigDecimal`, and `java.time` |
| [`interview/INTERVIEW_QUESTIONS.md`](interview/INTERVIEW_QUESTIONS.md) | 30+ Strings, Math & DateTime interview questions |

---

## 🚀 Compile & Run

```bash
javac -d out $(find 02-strings-math-and-datetime/codes -name "*.java")
java -cp out strings.StringPoolAndCompactStringsDemo
java -cp out strings.RegexAndTextBlockDemo
java -cp out math.BigDecimalFinancialPrecisionDemo
java -cp out datetime.JavaTimeApiMasteryDemo
```
