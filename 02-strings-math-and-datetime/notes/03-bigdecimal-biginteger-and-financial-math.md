# 03. Financial Precision Math: `BigDecimal`, `BigInteger` & IEEE-754 Traps

---

## 🟢 Layer 1 — Noob Track: Why `double` Breaks Money

In binary floating-point (IEEE-754), numbers like `0.1` and `0.2` are infinite repeating binary fractions (just like `1/3 = 0.3333...` in base 10):
```java
System.out.println(0.1 + 0.2); // Prints: 0.30000000000000004 !!
```
**Rule:** NEVER use `float` or `double` for money, tax, interest, or exact decimal invariants. Use **`BigDecimal`** (or integer minor units like `long cents`).

---

## 🟡 Layer 2 — Practitioner Track: The 4 Golden Rules of `BigDecimal`

1. **Always construct from `String` (or `BigDecimal.valueOf(double)`):**
   - `new BigDecimal(0.1)` captures the binary approximation: `0.1000000000000000055511151231257827021181583404541015625`!
   - `new BigDecimal("0.1")` stores exact `0.1` (`unscaledValue = 1`, `scale = 1`).
2. **Immutability:** `price.add(tax)` returns a **new** `BigDecimal` — it does NOT modify `price` in place!
3. **`equals()` vs `compareTo()` Trap:**
   - `new BigDecimal("1.0").equals(new BigDecimal("1.00"))` -> **`false`** (because scale `1 != 2`)!
   - `new BigDecimal("1.0").compareTo(new BigDecimal("1.00")) == 0` -> **`true`** (numerical equivalence!).
4. **Always pass `RoundingMode` to `divide()`:**
   `new BigDecimal("1").divide(new BigDecimal("3"))` throws `ArithmeticException: Non-terminating decimal expansion`! Always specify scale & rounding: `.divide(divisor, 2, RoundingMode.HALF_EVEN)` (Banker's Rounding eliminates upward bias).

---

## 🔴 Layer 3 — Senior Track: Internal Representation & Performance

- **Under the hood:** `BigDecimal` stores an arbitrary-precision `BigInteger intVal` (or a fast `long intCompact` when $\le 18$ digits!) + a 32-bit `int scale`.
- **When to use `long` cents vs `BigDecimal`:** High-frequency trading / payment ledgers often store integer minor units (`long cents`) for $O(1)$ CPU register math and zero heap allocation, converting to `BigDecimal` only for complex FX/tax/interest formulas.
- **`BigInteger`:** Arbitrary-size signed integer backed by `int[] mag` (magnitude words). Immutable; supports modular arithmetic (`modPow`, `gcd`, `isProbablePrime`) for RSA/cryptography.
