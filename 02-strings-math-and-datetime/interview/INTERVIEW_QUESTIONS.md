# 02. Strings, Math & `java.time` — Interview Questions & Answers

---

## 🟢 Beginner Questions

### Q1. Why is `String` immutable in Java?
1. **String Pool sharing** (multiple references safely share one literal).
2. **HashMap key safety** (`hashCode()` cached once; mutation would lose entries).
3. **Security** (class names, file paths, DB URLs can't mutate after validation).
4. **Automatic thread-safety**.

### Q2. What is the difference between `String`, `StringBuilder`, and `StringBuffer`?
- `String`: Immutable. Every modification allocates a new object.
- `StringBuilder`: Mutable, **not synchronized**, fastest — standard choice inside methods/loops.
- `StringBuffer`: Mutable, **`synchronized` methods**, slower — legacy.

### Q3. What is the String Constant Pool? Where does it live?
A canonical cache of unique `String` literals ensuring `"Java" == "Java"` points to the same instance. Since **Java 7**, it lives inside the **Main Heap** (moved out of PermGen), so unused interned strings can be garbage-collected!

### Q4. Why shouldn't you use `double` or `float` for money?
IEEE-754 binary floating-point cannot represent base-10 fractions like `0.1` exactly (`0.1 + 0.2 == 0.30000000000000004`). Use **`BigDecimal`** (constructed from `String`) or integer minor units (`long cents`).

### Q5. Which `java.time` class should I use for a birthday vs a database timestamp vs a flight?
- Birthday (no time/zone): `LocalDate`
- Database timestamp / event log: `Instant` (UTC epoch)
- Flight arrival/departure across regions: `ZonedDateTime`

---

## 🟡 Intermediate Questions

### Q6. `String s = new String("abc");` — how many objects are created?
- If `"abc"` is NOT yet in the String Pool: **2 objects** (one pooled literal `"abc"` + one explicit Heap `String` object via `new`).
- If `"abc"` IS already in the String Pool: **1 object** (only the Heap copy).

### Q7. Why should you NEVER write `new BigDecimal(0.1)`?
Because `0.1` is already an imprecise IEEE-754 `double` before reaching the constructor, storing `0.10000000000000000555111...`. Always use `new BigDecimal("0.1")` or `BigDecimal.valueOf(0.1)` (which calls `Double.toString(0.1)`).

### Q8. What is the difference between `BigDecimal.equals()` and `BigDecimal.compareTo()`?
`equals()` compares **both numeric value AND scale** (`new BigDecimal("1.0").equals(new BigDecimal("1.00"))` is **`false`**!). `compareTo()` compares **numeric magnitude only** (`compareTo(...) == 0` is **`true`**). Note: Putting `BigDecimal` into a `TreeSet` (uses `compareTo`) behaves differently from `HashSet` (uses `equals`/`hashCode`)!

### Q9. What happens when you call `new BigDecimal("1").divide(new BigDecimal("3"))`?
Throws `ArithmeticException: Non-terminating decimal expansion`. Always pass scale and `RoundingMode` (prefer `RoundingMode.HALF_EVEN` — Banker's Rounding).

### Q10. Why is `SimpleDateFormat` dangerous in backend services, and how does `DateTimeFormatter` fix it?
`SimpleDateFormat` mutates an internal `Calendar` field during `format()`/`parse()` — sharing a `static` instance across threads silently scrambles dates or crashes! `DateTimeFormatter` is **immutable and thread-safe**.

### Q11. Explain `Period` vs `Duration` in `java.time`.
- `Period`: Date-based (`years, months, days`) respecting calendar months and leap years (`LocalDate`).
- `Duration`: Time-based (`seconds, nanos`) representing exact elapsed elapsed physical time (`Instant`, `LocalTime`, `LocalDateTime`).

### Q12. What is the difference between `String.trim()` and `String.strip()` (Java 11+)?
`trim()` only strips ASCII control chars `<= '\u0020'`. `strip()` (plus `stripLeading()`, `stripTrailing()`, `isBlank()`) is Unicode-aware using `Character.isWhitespace(...)`.

---

## 🔴 Senior / JVM Internals Questions

### Q13. What are Compact Strings (Java 9 JEP 254), and how do they impact GC?
Prior to Java 9, `String` used `char[]` (2 bytes per character). Java 9 switched to `byte[] value` + `byte coder` (`0 = LATIN1` 1B/char, `1 = UTF16` 2B/char). Since ~90% of enterprise strings (JSON keys, UUIDs, SQL, HTTP headers) are Latin-1, Heap occupancy for Strings drops by ~40–50%, reducing GC frequency and improving L1/L2 CPU cache density.

### Q14. How does `javac` compile `"a" + x + "b"` in Java 8 vs Java 9+?
- Java 8 desugared it to `new StringBuilder().append("a").append(x).append("b").toString()`.
- Java 9+ (`JEP 280`) emits **`invokedynamic`** targeting `StringConcatFactory.makeConcatWithConstants`, which pre-computes exact final byte length and copies directly into a single `byte[]` without intermediate `StringBuilder` resizing! *(Inside loops, however, `s += x` still allocates O(N^2) objects — always use an explicit `StringBuilder` in loops!)*

### Q15. What is ReDoS (Regular Expression Denial of Service) and how do you prevent it?
Java's `java.util.regex` uses an NFA backtracking engine. Evil patterns with nested quantifiers like `(a+)+$` take exponential $O(2^N)$ time on near-matching input, hanging threads at 100% CPU. Prevent by avoiding nested ambiguous quantifiers, using **possessive quantifiers** (`++`, `*+`) or atomic groups `(?>...)`, and pre-compiling `Pattern` constants.

### Q16. How does `String.hashCode()` work and what is the Java 15+ subtlety?
`s[0]*31^(n-1) + s[1]*31^(n-2) + ... + s[n-1]`, cached in `private int hash`. Notice if a string's true hash is `0` (like `""` or `"pollinating"` collisions), older JDKs recomputed it every time; Java 15+ added `boolean hashIsZero` so even `0` is cached!

### Q17. Why do `char[]` passwords sometimes get recommended over `String`, and does it matter in modern JVMs?
A `String` stays on the Heap (and possibly String Pool) until GC runs, exposing secrets in heap dumps (`jmap`), whereas a `char[]` or `byte[]` can be explicitly wiped with `Arrays.fill(pwd, '\0')` immediately after verification. (Though note GC compaction can still leave old copies unless zeroed carefully.)

---

## 🧩 Tricky Output Puzzles

1. `String s1 = "ab"; String s2 = "a" + "b"; System.out.println(s1 == s2);` -> **`true`** (`"a" + "b"` is folded at compile time into `"ab"`).
2. `String a = "a"; String s1 = "ab"; String s2 = a + "b"; System.out.println(s1 == s2);` -> **`false`** (`a` is not a compile-time constant, so `s2` creates a new Heap String).
3. `new BigDecimal("2.0").equals(new BigDecimal("2.00"))` -> **`false`** (different `scale`).
4. `LocalDate.of(2024, 1, 31).plusMonths(1)` -> **`2024-02-29`** (clamps cleanly to last valid day of leap-year Feb!).
5. `Pattern.compile("a*").matcher("aba").matches()` vs `.find()` -> `matches()` is **`false`** (must match full string); `find()` is **`true`** (even matches 0-length prefix!).
