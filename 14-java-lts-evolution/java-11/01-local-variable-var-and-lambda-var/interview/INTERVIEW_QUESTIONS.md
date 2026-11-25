# Interview Questions — `var` Local Type Inference (Java 10 & 11)

## 🟢 Beginner

### Q1. Does `var` make Java dynamically typed like Python or JavaScript?
**No.** Java remains strictly statically typed. `javac` infers the exact compile-time type from the initializer expression and writes the exact concrete type into bytecode.

### Q2. Can `var` be used for class fields, method parameters, or `var x = null;`?
**No.** `var` is strictly restricted to initialized local variables, loop variables, `try`-with-resources variables, and (from Java 11) lambda parameters. Fields and signatures intentionally keep explicit types to preserve clean API contracts.

---

## 🟡 Intermediate

### Q3. Why did Java 11 specifically add `var` to lambda parameters when lambdas already had implicit `(a, b) -> ...`?
Because you **cannot place annotations** (`@NonNull`, `@Valid`, `@Deprecated`) on implicitly typed parameters (`(@NonNull a, b) -> ...` fails to compile!). Before Java 11, you had to spell out huge generic types (`(@NonNull Map<String, List<Order>> a) -> ...`). Java 11 lets you write `(@NonNull var a, var b) -> ...`.

### Q4. What trap happens with `var items = new ArrayList<>();`?
Because `<>` relies on target type inference from the LHS and `var` eliminates the explicit LHS type, `javac` infers **`ArrayList<Object>`**, allowing accidental mixed types unless you write `new ArrayList<Order>()` or `List.of(...)`!

---

## 🔴 Senior

### Q5. Can `int var = 10;` break existing Java 8 codebases on Java 11?
No — `var` is a **reserved type name**, not a reserved keyword. You can still name variables, packages, and methods `var`, though you can no longer name a *class* `var` (since Java classes should be PascalCase anyway).
