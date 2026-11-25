# Interview Questions — Pattern Matching for `switch` (Java 21)

## 🟢 Beginner

### Q1. Does `default:` inside a Java 21 pattern `switch` catch `null`?
**No!** For backward compatibility with pre-Java 21 `switch` statements, passing `null` into a `switch` still throws `NullPointerException` UNLESS an explicit **`case null`** (or **`case null, default`**) arm is present!

---

## 🟡 Intermediate

### Q2. Why will placing `case Number n` above `case Integer i` fail compilation in Java 21?
Because of the **Pattern Dominance Rule**: `Integer` is a subtype of `Number`, so `case Number n` dominates (catches everything for) `Integer` unconditionally — making `case Integer i` unreachable dead code! Specific subtypes and guarded `when` cases MUST precede general unguarded cases.

### Q3. What is `java.lang.MatchException`?
Introduced in Java 21 for exhaustive pattern switches/deconstruction. If runtime reality violates compile-time exhaustiveness (such as binary mismatch where a new permitted class was compiled separately and wasn't recompiled against the switch, OR a `when` guard exception / null deconstruction remainder occurs), the JVM throws **`MatchException`**.

---

## 🔴 Senior

### Q4. How does HotSpot compile & execute pattern `switch` guards cleanly in bytecode?
Via **`invokedynamic`** bootstrapped by **`SwitchBootstraps.typeSwitch`**, passing a restart index slot inside a synthetic `tableswitch` loop! If `case String s when s.length() > 5` matches `String` type check (`idx = 1`) and calls the guard which returns `false`, execution loops back setting `restartIdx = 2` directly to continue evaluating subsequent `case` patterns cleanly without redundant type checks above it!
