# Java 17 — Sealed Classes & Interfaces (JEP 409)

## ⚡ 2-Minute Revision
- **Problem Solved:** Before Java 17, inheritance was all-or-nothing: `public` (anyone anywhere could subclass an open hierarchy forever, ruining `switch` exhaustiveness) OR `final` (nobody could extend it!).
- **Syntax:** `public sealed interface Payment permits Card, Bank, Crypto {}`
- **3 Mandatory Subtype Rules:** Every permitted direct subclass MUST declare exactly ONE modifier:
  1. **`final`** — stops hierarchy here (`record` is implicitly `final`!).
  2. **`sealed`** — keeps delegating closed control to another restricted `permits` list.
  3. **`non-sealed`** — explicitly re-opens that branch to unrestricted extension.
- **Same Module / Package Constraint:** Permitted subclasses must live in the **same module** (or **same package** on unnamed classpath). If subtypes live in the same `.java` source file, `permits` can even be omitted!
- **ADT Superpower:** Sealed Interfaces (Sum Types: OR) + Records (Product Types: AND) unlock clean Data-Oriented Programming + compile-time exhaustive `switch`!

## 📂 Contents
- [`notes/sealed-classes-deep-dive.md`](notes/sealed-classes-deep-dive.md)
- [`codes/lts/java17/sealedtypes/SealedClassesHierarchyDemo.java`](codes/lts/java17/sealedtypes/SealedClassesHierarchyDemo.java)
- [`interview/INTERVIEW_QUESTIONS.md`](interview/INTERVIEW_QUESTIONS.md)
