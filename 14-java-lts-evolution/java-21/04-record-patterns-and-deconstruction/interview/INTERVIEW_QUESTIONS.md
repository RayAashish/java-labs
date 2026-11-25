# Interview Questions — Java 21 Record Patterns (JEP 440)

## 🟢 Beginner

### Q1. What is a Record Pattern in Java 21?
Instead of testing `if (obj instanceof Point p)` and manually calling `p.x()` and `p.y()`, you unpack the components directly inside the pattern: `if (obj instanceof Point(int x, int y))` or inside a `switch` `case`.

---

## 🟡 Intermediate

### Q2. Can Record Patterns unpack arbitrarily deep nested records? Can you mix `var` and explicit types?
**Yes!** For example `case Line(Point(var x1, var y1), Point(int x2, int y2)) -> ...` unpacks all four coordinates in a single arm, mixing `var` inference and explicit types cleanly.

### Q3. What happens if `order` is non-null, `order.customer()` is `null`, and you run `switch (order)` matching `case Order(var id, Customer(var cid))`?
It **does not throw `NullPointerException`** on the check — `Customer(var cid)` simply evaluates as *non-matching* for `null`! If a fallback arm (`case Order(var id, _)`) exists, that executes; if no fallback covers the `null` customer remainder, Java throws **`MatchException`**.

---

## 🔴 Senior

### Q4. How does Record Pattern Matching handle generic records like `Box<String>`?
If the scrutinee expression statically already carries the parameterized type `Box<String> b`, you can write `b instanceof Box(var s)` and `javac` infers `s` as `String` directly! Conversely, writing `Object o; o instanceof Box<String>(var s)` where `o` might not be a `Box<String>` is disallowed to prevent unsafe erased type assertions.
