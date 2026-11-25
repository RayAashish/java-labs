# 02. Regular Expressions (`Pattern` & `Matcher`) and ReDoS Traps

---

## 🟢 Layer 1 — Noob Track: Searching & Validating Text

Java provides `java.util.regex.Pattern` (a compiled pattern blueprint) and `Matcher` (the engine that runs that pattern against an input string).

```java
Pattern emailPattern = Pattern.compile("^[\\w.-]+@[\\w.-]+\\.[a-zA-Z]{2,}$");
Matcher matcher = emailPattern.matcher("dev@example.com");
boolean isValid = matcher.matches(); // true
```

---

## 🟡 Layer 2 — Practitioner Track: Rules & Performance Best Practices

1. **Always pre-compile `Pattern` as `private static final`:**
   Calling `str.matches(regex)` or `str.split(regex)` recompiles the finite-state automaton on **every single call**.
2. **`matches()` vs `find()`:**
   - `matcher.matches()` must match the **entire** input string from start to end.
   - `matcher.find()` scans for the **next substring** matching the pattern (call in a `while (matcher.find())` loop).
3. **Named Capturing Groups (Java 7+):**
   `Pattern.compile("(?<year>\\d{4})-(?<month>\\d{2})")` lets you read `matcher.group("year")` cleanly instead of brittle positional numbers.
4. **Simple Delimiter Optimization:** For single-character splits (`a,b,c`), `String.split(",")` has a fast path in modern JDKs, whereas complex regexes invoke full `Pattern` state machines.

---

## 🔴 Layer 3 — Senior Track: Quantifiers & ReDoS (Catastrophic Backtracking)

| Quantifier | Meaning | Behavior |
|---|---|---|
| `.*` (Greedy) | 0 or more | Eats everything to the end, then **backtracks** char-by-char |
| `.*?` (Reluctant / Lazy) | 0 or more | Starts minimal and expands until match succeeds |
| `.*+` (Possessive) | 0 or more | Eats everything and **never backtracks** (fast fail!) |

> [!WARNING]
> **ReDoS (Regular Expression Denial of Service):** Nested quantifiers like `(a+)+b` on a non-matching input `"aaaaaaaaaaaaaaaaaaaaaaaaX"` cause $O(2^N)$ backtracking in Java's NFA regex engine, pinning a CPU core at 100% for seconds or minutes! Use possessive quantifiers (`++`), atomic groups `(?>...)`, or `Pattern` timeouts / non-backtracking validation.
