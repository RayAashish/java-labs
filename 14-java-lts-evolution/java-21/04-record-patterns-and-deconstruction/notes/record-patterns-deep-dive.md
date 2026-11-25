# Record Patterns & Nested Deconstruction (JEP 440) — Deep Dive

---

## 🟢 Layer 1 — Noob Track: "Unpacking Boxes Inside Boxes"

Without Record Patterns (Java 17):
```java
if (shape instanceof ColoredRectangle cr) {
    Point topLeft = cr.topLeft();
    int x = topLeft.x();
    int y = topLeft.y();
    ...
}
```
With Java 21 Record Patterns:
```java
if (shape instanceof ColoredRectangle(Point(int x, int y), Color color)) {
    // `x`, `y`, and `color` are already unpacked variables ready to use!
}
```

---

## 🟡 Layer 2 — Practitioner Track: Rules, Generics & Nested Null Behavior

### 1. Three Places Record Patterns Shine
1. **`instanceof` Guarded Extraction:** `if (resp instanceof ApiBox(Result(var code, var msg)) && code == 200)`
2. **`switch` Over Sealed Record ADTs:** Cleanly expresses AST evaluators, JSON visitors, billing/domain dispatch rules in zero boilerplate!
3. **Generic Type Inference:** Given `record Wrapper<T>(T value) {}` and a typed reference `Wrapper<Long> w`:
   - `if (w instanceof Wrapper(var n))` infers **`Long n`** directly (no need to repeat `Wrapper<Long>(Long n)`!).
   - Note: on raw `Object obj`, `obj instanceof Wrapper(var n)` only checks raw type `Wrapper` (due to erasure) -> wait: generic type arguments in patterns cannot break type soundness (`obj instanceof Wrapper<String>(var s)` is rejected if `obj` cannot safely prove `String` without unchecked cast!).

### 2. What Happens When a Nested Component Is `null`?
Suppose `Order(Customer c, double amt)` has `c == null`.
- `case Order(Customer(var name), var amt)` **DOES NOT MATCH** (because `null` is not a valid `Customer` to deconstruct) — and **DOES NOT throw `NullPointerException`**!
- If you add a fallback `case Order(var customer, var amt) -> ...` below it, that fallback matches with `customer == null` cleanly!
- If NO fallback matches inside an exhaustive `switch`, Java throws **`MatchException`**!

---

## 🔴 Layer 3 — Senior / JVM Track

- Unlike classic getter chains (`o.customer().address().zip()` which throws NPE mid-chain unless wrapped in `Optional`), Record Patterns invoke accessor methods generated for the `Record` bytecode descriptor inside a structured `try`-guarded or conditional pattern cascade (`SwitchBootstraps.typeSwitch` + accessor `MethodHandle` invocation).
- *(Historical trivia: Java 20 previewed Record Patterns inside enhanced `for (Point(var x, var y) : points)` loops, which was withdrawn before Java 21 JEP 440 pending future refinement — so use `instanceof` / `switch`!)*
