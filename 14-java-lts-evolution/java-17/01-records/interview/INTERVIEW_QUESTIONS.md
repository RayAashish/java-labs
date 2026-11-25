# Interview Questions — Java 17 Records (`record`)

## 🟢 Beginner

### Q1. What methods and modifiers does `javac` automatically generate for a `record Point(int x, int y)`?
- Class is **`public final class Point extends java.lang.Record`**.
- Fields `private final int x;` and `private final int y;`.
- Canonical constructor `public Point(int x, int y)`.
- Accessor methods `x()` and `y()` (not `getX()`!).
- `equals(Object)`, `hashCode()`, and `toString()`.

### Q2. Can a `record` extend another class or implement interfaces?
It **cannot** extend any class because it implicitly extends `java.lang.Record` and is implicitly `final`. It **CAN** implement any number of interfaces cleanly (which makes `sealed interface` + `record` implementations ideal!).

---

## 🟡 Intermediate

### Q3. Explain the "Shallow Immutability Trap" with Records and how to solve it.
Records make field *references* `final`, NOT the objects pointed to! If a component is a `List<String>` or `Date`, callers holding the original reference can mutate its contents after creation. Fix it by performing `items = List.copyOf(items)` inside a **compact constructor**!

### Q4. Why should you avoid using a `record` as a JPA / Hibernate `@Entity`?
Hibernate/JPA relies on bytecode subclassing proxies (cannot subclass a `final` record), requires a mutable no-arg constructor and setters for dirty checking / lazy proxies, and defines equality often by database `@Id` rather than all fields (which could load lazy relationships unexpectedly inside `equals`/`hashCode`). Use Records for Spring Data **DTO projections** (`SELECT new ...`) and API responses!

---

## 🔴 Senior

### Q5. Why is deserializing a `record` much safer than deserializing a classic POJO?
Classic Java deserialization bypasses constructors completely using low-level allocation and injects fields reflectively — allowing forged streams to create illegal object states or run `readObject()` gadget exploits. For **Records**, `ObjectInputStream` resolves component values first and invokes the **canonical constructor**, enforcing compact-constructor validation automatically and forbidding custom `readObject`/`writeReplace` overrides.
