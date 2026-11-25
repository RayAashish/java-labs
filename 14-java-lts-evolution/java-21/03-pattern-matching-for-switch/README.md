# Java 21 — Pattern Matching for `switch` (JEP 441)

## ⚡ 2-Minute Revision
- **What Changed:** `switch` is no longer restricted to integers, `String`, and `enum` constants — it now switches directly on **types, sealed hierarchies, qualified `enum`s across subtypes, guarded `when` conditions, and `null`**!
- **4 Game-Changing Rules:**
  1. **`case null`:** Handled safely inside `switch` (`case null -> ...` or `case null, default -> ...`) — **if omitted, switching on `null` still throws `NullPointerException`!**
  2. **Guarded `when` Clauses:** `case Order o when o.amount() >= 1000 -> ...`
  3. **Dominance Order Enforced:** General unguarded cases MUST come **AFTER** guarded/specific cases (`case String s` before `case String s when s.isEmpty()` fails compilation with *"dominated by a preceding case label"*!).
  4. **Compile-Time Exhaustiveness + `MatchException`:** On a `sealed` supertype, covering all permitted subtypes requires ZERO `default`! If a separate JAR compiles a mismatched subclass binary later, Java throws **`MatchException`** (NOT `IncompatibleClassChangeError` silently).
- **JVM Under the Hood:** Uses `invokedynamic` + **`SwitchBootstraps.typeSwitch`**!

## 📂 Contents
- [`notes/switch-pattern-matching-deep-dive.md`](notes/switch-pattern-matching-deep-dive.md)
- [`codes/lts/java21/switchpatterns/SwitchPatternMatchingDeepDiveDemo.java`](codes/lts/java21/switchpatterns/SwitchPatternMatchingDeepDiveDemo.java)
- [`interview/INTERVIEW_QUESTIONS.md`](interview/INTERVIEW_QUESTIONS.md)
