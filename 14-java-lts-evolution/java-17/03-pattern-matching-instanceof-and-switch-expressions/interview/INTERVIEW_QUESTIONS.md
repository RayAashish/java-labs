# Interview Questions — Pattern Matching `instanceof` & `switch` Expressions

## 🟢 Beginner

### Q1. How do `switch` expressions (Java 14/17) improve on traditional `switch` statements?
1. Arrow cases (`->`) **never fall through** implicitly (eliminating forgotten `break;` bugs).
2. Multiple constants share a comma list (`case SAT, SUN -> ...`).
3. A `switch` expression returns a value directly and **enforces compile-time exhaustiveness**.

---

## 🟡 Intermediate

### Q2. Compile check: Does `if (obj instanceof String s || s.length() > 3)` compile? Why?
**Compile Error!** Flow scoping binds `s` *only where `obj instanceof String` is definitely true*. In `||`, the right-hand side executes when the left-hand side was `false`, so `s` would be undefined. With **`&&`**, the right side only runs when the left side is `true`, so `s` is valid!

### Q3. Why shouldn't you casually add a `default:` branch to an `enum` `switch` expression?
If you omit `default:` and list every `enum` constant explicitly, adding a new constant to the `enum` later causes `javac` to fail compilation cleanly wherever you forgot to handle the new value! Adding `default:` silently swallows future enum cases at runtime.

---

## 🔴 Senior

### Q4. Explain how `yield` differs from `return` inside a `switch` expression.
`yield val;` exits ONLY the enclosing **`switch` expression**, returning `val` as the result of the `switch`! Using `return val;` inside a `switch` expression is disallowed because it tries to exit the surrounding *method* rather than supplying a value for the expression.
