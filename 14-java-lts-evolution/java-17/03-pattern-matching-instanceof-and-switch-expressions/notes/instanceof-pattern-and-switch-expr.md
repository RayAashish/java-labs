# Pattern Matching `instanceof` & `switch` Expressions — Deep Dive

---

## 🟢 Layer 1 — Noob Track: Eliminating Casts & Fall-Through Bugs

### 1. Eliminating Redundant Casts
- **Old Java 8:** `if (obj instanceof String) { String s = (String) obj; return s.length(); }`
- **Java 16/17:** `if (obj instanceof String s) { return s.length(); }`

### 2. Fixing Classic `switch` Bugs
- **Old Java 8:** Forgetting `break;` caused silent fall-through to the next `case`; omitting `default` left local variables uninitialized; multi-constant labels meant empty stacking (`case MON: case FRI:`).
- **Java 14/17:** Arrow labels `case MON, FRI -> 6;` NEVER fall through, group multiple constants with commas cleanly, and evaluate directly to a value assigned to a variable (and `javac` ensures ALL enum/domain values are covered!).

---

## 🟡 Layer 2 — Practitioner Track: Flow Scoping & `yield`

### Flow Scoping Rules (Must Know!)
`s` is bound ONLY along branches that guarantee `obj instanceof String` succeeded:
- ✅ `if (x instanceof String s && s.isEmpty())` — RHS of `&&` only runs if LHS was `true`.
- ❌ `if (x instanceof String s || s.isEmpty())` — **COMPILE ERROR**, because RHS of `||` runs when LHS was `false` (`s` is unbound!).
- ✅ Guard Clause Early Exit:
  ```java
  if (!(obj instanceof String s)) {
      return 0;
  }
  // Here s IS in scope cleanly because execution can only reach here if instanceof succeeded!
  return s.length();
  ```
- **Equals Trap:** Look how concise `Object.equals` becomes:
  `return (this == o) || (o instanceof Point other && x == other.x && y == other.y);`

### `switch` Statement vs `switch` Expression Exhaustiveness
- A classic `switch` *statement* discarding results doesn't force covering every enum constant.
- A **`switch` expression** (assigned or returned) MUST be **exhaustive** — missing even one enum constant causes an immediate compile error, catching future bugs when a teammate adds `Day.HOLIDAY`!
- Note on `enums` in switch expressions: avoid redundant `default:` on an `enum` switch if you WANT the compiler to warn you whenever a new enum constant is added! (`javac` even synthesizes a synthetic `$SwitchMap` throw for binary evolution.)

---

## 🔴 Layer 3 — Senior / JVM Track

- Unlike C#'s `yield return` (which streams generators), Java's **`yield`** is strictly a local control-transfer statement producing a value out of a `{ ... }` `switch` expression block! It is a **contextual keyword** (`int yield = 5;` outside switch still compiles).
- For `enum` switches, `javac` continues emitting a synthetic `$SwitchMap` jump-table class using `tableswitch`/`lookupswitch` O(1) dispatch bytecode — identical fast runtime performance with safer compile-time invariants.
