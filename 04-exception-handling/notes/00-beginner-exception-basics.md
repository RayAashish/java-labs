# 00. Exception Handling for Beginners — First Principles

---

## 🟢 What Is an Exception?

When your program hits an unexpected problem (dividing by zero, reading a missing file, accessing `null`), Java stops normal execution and **throws an Exception object** describing *what* failed and *where* (`stack trace`).

Think of `try / catch / finally` like a safety net for circus acrobats:
- **`try { ... }`** — The risky code you want to attempt.
- **`catch (SpecificException e) { ... }`** — The safety net that catches the failure so your app recovers instead of crashing.
- **`finally { ... }`** — Cleanup that runs **whether the attempt succeeded or failed** (closing connections/files).

```java
try {
    int age = Integer.parseInt("not-a-number"); // throws NumberFormatException
} catch (NumberFormatException e) {
    System.out.println("Please enter digits only! Details: " + e.getMessage());
} finally {
    System.out.println("Validation finished.");
}
```

### Checked vs Unchecked (The 10-Second Rule)
- **Checked (`IOException`, `SQLException`):** External world problems (disk/network/DB). Compiler **forces** you to `try/catch` or declare `throws`.
- **Unchecked (`NullPointerException`, `IllegalArgumentException`):** Programming/input bugs. Fix the logic rather than blindly wrapping everything in `try/catch`.

### `throw` vs `throws`
- **`throw`** (inside method body): Actively launches one exception instance (`throw new IllegalArgumentException("Negative balance");`).
- **`throws`** (in method signature): Warns callers *"This method might throw `IOException`"* (`void readFile() throws IOException`).
