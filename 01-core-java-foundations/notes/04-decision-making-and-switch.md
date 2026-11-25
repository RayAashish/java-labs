# Java Decision Making (Selection) Statements: Comprehensive Notes

Decision-making statements evaluate boolean expressions to determine which branch or block of code should execute during runtime.

---

## 1. Overview of Decision-Making Constructs

```
                   Decision Making Statements
                               |
         +---------------------+---------------------+
         |                                           |
  `if` Family                                   `switch` Constructs
  - Simple `if`                                 - Classic `switch` (colon syntax)
  - `if-else`                                   - Enhanced `switch` expressions (-> arrow)
  - `if-else-if` ladder                         - Pattern Matching for `switch` (Java 17/21)
  - Nested `if-else`
```

---

## 2. The `if` Statement Family

### 1. Simple `if` Statement
Executes a block of code strictly when the specified condition evaluates to `true`.
```java
if (score >= 90) {
    System.out.println("Grade A");
}
```

### 2. `if-else` Statement
Provides two distinct execution pathways: one when the condition is `true` and an alternative when it is `false`.
```java
if (n % 2 == 0) {
    System.out.println("Even");
} else {
    System.out.println("Odd");
}
```

### 3. `if-else-if` Ladder
Tests multiple mutually exclusive conditions in sequential order. The first branch whose condition evaluates to `true` is executed, and subsequent checks are skipped.
```java
if (marks >= 90) {
    grade = "A+";
} else if (marks >= 80) {
    grade = "A";
} else if (marks >= 70) {
    grade = "B";
} else {
    grade = "F";
}
```

### 4. Nested `if-else`
An `if` or `else` block placed inside another `if` or `else` block for hierarchical conditional evaluation.
```java
if (user != null) {
    if (user.isActive()) {
        grantAccess();
    } else {
        showDeactivatedWarning();
    }
}
```

---

## 3. The `switch` Statement & Expressions

A `switch` evaluates a single selector expression against multiple constant branch labels.

### Supported Selector Types
- Primitive integral types: `byte`, `short`, `char`, `int` (and their boxed wrapper equivalents).
- `String` (Java 7+).
- `enum` types (Java 5+).
- *Not supported directly in classic switch*: `long`, `float`, `double`, `boolean`.

### Classic `switch` (Statement with `break` & Fall-through)
In classic switch statements, without an explicit `break`, execution falls through into subsequent case blocks:
```java
switch (dayNumber) {
    case 1:
        day = "Monday";
        break;
    case 2:
        day = "Tuesday";
        break;
    default:
        day = "Invalid Day";
        break;
}
```

### Modern `switch` Expression (Arrow Syntax `->`, Java 14+)
- Eliminates accidental fall-through.
- Allows returning a value directly from the switch construct.
- Uses `yield` when a block `{ ... }` is needed inside a case.
```java
String dayName = switch (dayNumber) {
    case 1 -> "Monday";
    case 2 -> "Tuesday";
    case 6, 7 -> "Weekend";
    default -> {
        System.out.println("Unknown day: " + dayNumber);
        yield "Invalid";
    }
};
```

---

## 4. Comparison: `if-else` vs `switch`

| Feature | `if-else` Ladder | `switch` Statement / Expression |
| :--- | :--- | :--- |
| **Expression Types** | Any boolean expression (relational, logical, complex method calls) | Constants: integers, chars, Strings, Enums, or pattern types |
| **Testing** | Tests ranges, inequalities (`<`, `>`, `<=`), and arbitrary boolean logic | Tests equality against discrete constant values |
| **Performance** | \(O(N)\) sequential checks (top-down evaluation) | Often optimized into a jump table (`tableswitch` / `lookupswitch`), approaching \(O(1)\) |
| **Readability** | Preferred for small branches or range evaluations | Preferred for multi-way equality checks on a single variable |

---

## 5. Common Pitfalls

1. **Dangling Else**: An `else` binds to the nearest preceding unmatched `if` unless explicit braces `{}` are used. Always use braces!
2. **Missing `break` in Classic Switch**: Causes accidental fall-through bugs where unintended subsequent case logic executes.
3. **`NullPointerException` on Switch**: In classic switch, passing a `null` `String` or wrapper causes a runtime `NullPointerException` before matching any case (unless using Java 21 `case null`).
