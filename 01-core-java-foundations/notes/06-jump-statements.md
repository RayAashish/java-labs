# Java Jump Statements (`break`, `continue`, `return`)

Jump statements unconditionally transfer control flow inside loops, `switch` blocks, or methods.

---

## 1. The `break` Statement

`break` immediately terminates the **nearest enclosing loop** (`for`, `while`, `do-while`) or classic `switch` statement.

```java
for (int i = 1; i <= 10; i++) {
    if (i == 5) {
        break; // exits the loop completely when i reaches 5
    }
    System.out.print(i + " "); // prints: 1 2 3 4
}
```

### Labeled `break` (Escaping Nested Loops)
Java intentionally has no `goto` keyword (though `goto` is reserved). To exit multiple nested loops cleanly without introducing messy boolean flags, attach a label to the outer loop:

```java
searchGrid:
for (int r = 0; r < matrix.length; r++) {
    for (int c = 0; c < matrix[r].length; c++) {
        if (matrix[r][c] == target) {
            System.out.println("Found at (" + r + ", " + c + ")");
            break searchGrid; // breaks BOTH inner and outer loops
        }
    }
}
```

> [!TIP]
> In production code, extracting nested loops into a small helper method and using an early `return` is usually even cleaner than labeled `break`.

---

## 2. The `continue` Statement

`continue` skips the remainder of the **current iteration** and advances to the next loop cycle:
- In a `for` loop, control jumps to the **update expression** (`i++`), then checks the condition.
- In a `while` or `do-while` loop, control jumps directly to the **boolean condition check** (be sure to update loop counters *before* `continue` to avoid infinite loops!).

```java
for (int i = 1; i <= 6; i++) {
    if (i % 2 == 0) {
        continue; // skip even numbers
    }
    System.out.print(i + " "); // prints: 1 3 5
}
```

---

## 3. The `return` Statement

`return` immediately exits the **current method** and transfers control (and optionally a result value) back to the caller.

```java
public static String classifyAge(int age) {
    if (age < 0) {
        return "Invalid"; // Guard clause / early return keeps nesting shallow
    }
    return age >= 18 ? "Adult" : "Minor";
}
```

> [!WARNING]
> **The `finally` Override Trap:** If a `try` block executes `return 10;` and its `finally` block executes `return 20;`, the `finally` return **silently overrides** the original return value (and even swallows thrown exceptions!). Never place `return` inside `finally`.

---

## 4. Quick Comparison Matrix

| Statement | Valid Inside | Effect | Supports Labels? |
|---|---|---|---|
| `break` | Loops & `switch` | Terminates entire enclosing loop/switch | Yes (`break label;`) |
| `continue` | Loops only | Skips rest of current iteration only | Yes (`continue label;`) |
| `return` | Any method/constructor | Exits current method (with optional value) | No |
