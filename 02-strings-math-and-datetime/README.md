# 02. Strings, Arbitrary-Precision Math & Modern Date/Time (`java.time`)

> **Why this module exists:** Every real backend app manipulates text, money, and timestamps. Using `==` on Strings, `+` inside loops, `double` for currency, or `SimpleDateFormat` across threads are four of the most common production bugs in Java.

---

## ⚡ 2-Minute Rapid Revision Card

- **`String` Immutability:** `String` is `final`, backed by `private final byte[] value` (Java 9+ **Compact Strings**: `LATIN1` 1 byte/char or `UTF16` 2 bytes/char, cutting Heap usage up to 50%). Thread-safe, cacheable `hash`, safe for String Constant Pool & Map keys.
- **String Pool (`intern()`):** Literals `"Java"` live in the String Table (inside Heap since Java 7, not PermGen). `new String("Java")` forces a separate Heap object; `.intern()` returns the canonical pooled reference.
- **`String` vs `StringBuilder` vs `StringBuffer`:** `String` (immutable), `StringBuilder` (mutable, non-synchronized, fastest — use in single-threaded loops), `StringBuffer` (mutable, `synchronized` methods — legacy).
- **`BigDecimal` Golden Rules:** ALWAYS construct from `String` (`new BigDecimal("0.1")`) or `BigDecimal.valueOf(0.1)` — NEVER `new BigDecimal(0.1)`! Use `.compareTo()` for numeric equality (`1.0` vs `1.00`: `equals()` is `false`, `compareTo()` is `0`).
- **`java.time` (JSR-310):** All classes (`LocalDate`, `Instant`, `ZonedDateTime`, `DateTimeFormatter`) are **immutable and thread-safe**. Store/transmit UTC `Instant`; convert to `ZonedDateTime` only at display edges.

---

## 📂 Contents

| Note | Companion Runnable Labs |
|---|---|
| [`01-strings-pool-compact-strings-and-builders.md`](notes/01-strings-pool-compact-strings-and-builders.md) | [`StringPoolAndCompactStringsDemo.java`](codes/strings/StringPoolAndCompactStringsDemo.java)<br>[`StringBuilderAndConcatenationDemo.java`](codes/strings/StringBuilderAndConcatenationDemo.java)<br>[`ModernStringMethodsDemo.java`](codes/strings/ModernStringMethodsDemo.java) |
| [`02-regex-pattern-matcher-and-redos.md`](notes/02-regex-pattern-matcher-and-redos.md) | [`RegexAndTextBlockDemo.java`](codes/strings/RegexAndTextBlockDemo.java) |
| [`03-bigdecimal-biginteger-and-financial-math.md`](notes/03-bigdecimal-biginteger-and-financial-math.md) | [`BigDecimalFinancialPrecisionDemo.java`](codes/math/BigDecimalFinancialPrecisionDemo.java) |
| [`04-modern-datetime-jsr310.md`](notes/04-modern-datetime-jsr310.md) | [`JavaTimeApiMasteryDemo.java`](codes/datetime/JavaTimeApiMasteryDemo.java) |
| **Interview Bank** | 👉 [`interview/INTERVIEW_QUESTIONS.md`](interview/INTERVIEW_QUESTIONS.md) |

---

## 🚀 Compile & Run

```bash
javac -d out $(find 02-strings-math-and-datetime/codes -name "*.java")
java -cp out strings.StringPoolAndCompactStringsDemo
java -cp out strings.StringBuilderAndConcatenationDemo
java -cp out strings.ModernStringMethodsDemo
java -cp out strings.RegexAndTextBlockDemo
java -cp out math.BigDecimalFinancialPrecisionDemo
java -cp out datetime.JavaTimeApiMasteryDemo
```
