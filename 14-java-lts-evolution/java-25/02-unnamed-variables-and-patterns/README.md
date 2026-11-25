# Java 25 — Unnamed Variables & Patterns `_` (JEP 456)

## ⚡ 2-Minute Revision
- **Problem Solved:** Java developers spent decades inventing dummy variable names (`ignored`, `unusedEx`, `unusedKey`, `e2`) for values forced by syntax (`catch`, lambdas, destructuring records, `try-with-resources`) — confusing readers and triggering IDE unused-variable warnings!
- **Syntax (`_` finalized Java 22+ / Java 25 LTS):** Use single underscore **`_`** cleanly!
- **6 Valid Contexts:**
  1. `catch (NumberFormatException _) { ... }`
  2. Lambda parameters: `map.forEach((k, _) -> log(k))`
  3. Enhanced `for` counter loops: `for (Order _ : orders) count++;`
  4. `try (var _ = LockScope.acquire()) { ... }` (auto-closed resource whose variable is unused in body!)
  5. Local variable side-effect calls: `int _ = queue.poll();`
  6. **Record Pattern Components:** `case Order(var id, _, Money(_, var amt))` or typed `Point(int x, int _)` *(note: CANNOT write `var _` — use bare `_`!)*.
- **Multiple `_` in Same Scope:** Allowed! Unlike named variables, `_` creates NO binding in scope, so you can write multiple `_` variables in the same block or pattern without name clashes (and you cannot read `_` later).

## 📂 Contents
- [`notes/unnamed-variables-deep-dive.md`](notes/unnamed-variables-deep-dive.md)
- [`codes/lts/java25/unnamed/UnnamedVariablesAndPatternsDemo.java`](codes/lts/java25/unnamed/UnnamedVariablesAndPatternsDemo.java)
- [`interview/INTERVIEW_QUESTIONS.md`](interview/INTERVIEW_QUESTIONS.md)
