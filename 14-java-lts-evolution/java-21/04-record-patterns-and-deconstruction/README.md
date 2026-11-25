# Java 21 — Record Patterns & Nested Deconstruction (JEP 440)

## ⚡ 2-Minute Revision
- **What It Is:** Instead of only matching `case Order o -> o.customer().name()`, you **deconstruct the record components directly inside `switch` or `instanceof`**:
  `case Order(String id, Customer(var name, var tier), double amt) -> ...`
- **Nested Arbitrary Depth:** Deconstruct records *inside* records cleanly in a single pattern!
- **`var` Component Inference:** Use `var` for component types cleanly (`Order(var id, var total)`) or explicit types (`Order(String id, double total)`).
- **Generic Record Type Inference:** `Box<String> box` -> `if (box instanceof Box(var s))` automatically infers `s` as `String`!
- **Remainder / Null Hazard:** While a top-level `case null` catches `null`, a matched `Order` whose nested `customer` field itself is `null` fails nested deconstruction `Customer(...)` without throwing NPE — instead `switch` rejects that arm (matching a broader `case Order o` or throwing `MatchException` if unmatched!).

## 📂 Contents
- [`notes/record-patterns-deep-dive.md`](notes/record-patterns-deep-dive.md)
- [`codes/lts/java21/recordpatterns/NestedRecordPatternsDemo.java`](codes/lts/java21/recordpatterns/NestedRecordPatternsDemo.java)
- [`interview/INTERVIEW_QUESTIONS.md`](interview/INTERVIEW_QUESTIONS.md)
