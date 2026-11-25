# Java 17 — Pattern Matching `instanceof` (JEP 394) & `switch` Expressions (JEP 361)

## ⚡ 2-Minute Revision
- **`instanceof` Pattern Matching (Java 16/17 JEP 394):** `if (obj instanceof String s && !s.isBlank())` combines type test + cast + variable binding cleanly!
- **Flow Scoping Rule (Interview Favorite!):** Pattern variable `s` is ONLY in scope where `javac` **guarantees it matched**:
  - Works: `if (obj instanceof String s && s.length() > 3)` ✅
  - Fails compile: `if (obj instanceof String s || s.length() > 3)` ❌ (`s` might be unassigned on right side of `||`!)
  - Guard Inversion Trick: `if (!(obj instanceof String s)) return; System.out.println(s.length());` ✅ (`s` stays in scope for the rest of the method!)
- **`switch` Expressions (Java 14/17 JEP 361):**
  - Arrow syntax `case MON, FRI -> 6;` eliminates catastrophic missing-`break` fall-through bugs forever!
  - Comma-separated labels, expression return values, and **`yield`** for multi-statement blocks (`case SAT -> { log(); yield 8; }`).
  - Exhaustiveness enforced at compile time!

## 📂 Contents
- [`notes/instanceof-pattern-and-switch-expr.md`](notes/instanceof-pattern-and-switch-expr.md)
- [`codes/lts/java17/flow/SwitchExprAndInstanceofPatternDemo.java`](codes/lts/java17/flow/SwitchExprAndInstanceofPatternDemo.java)
- [`interview/INTERVIEW_QUESTIONS.md`](interview/INTERVIEW_QUESTIONS.md)
