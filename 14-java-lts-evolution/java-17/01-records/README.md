# Java 17 — Records (`record` — JEP 395)

## ⚡ 2-Minute Revision
- **What:** Concise syntax `public record Point(int x, int y) {}` modeling pure immutable **tuples / DTOs / API contracts / composite Map keys**.
- **Generated Automatically:** `private final` fields, canonical constructor, accessor methods matching component names (`x()` — NOT `getX()`!), `equals()`, `hashCode()`, `toString()`.
- **Strict Constraints:** Implicitly `final` (no subclassing), extends `java.lang.Record` (cannot extend another class, though CAN implement interfaces!), cannot declare extra instance fields.
- **Shallow Immutability Trap:** A `List<String>` component can still be mutated externally UNLESS defensively copied inside a **Compact Constructor** (`items = List.copyOf(items);`).
- **Anti-Pattern:** Do **NOT** use `record` for **JPA/Hibernate `@Entity` classes** (ORM proxies require non-final classes, no-arg constructors, and dirty-tracked mutable setters; use Records only for Spring Data `@Query` **read projections**!).

## 📂 Contents
- [`notes/records-deep-dive.md`](notes/records-deep-dive.md)
- [`codes/lts/java17/records/RecordsDeepDiveDemo.java`](codes/lts/java17/records/RecordsDeepDiveDemo.java)
- [`interview/INTERVIEW_QUESTIONS.md`](interview/INTERVIEW_QUESTIONS.md)
