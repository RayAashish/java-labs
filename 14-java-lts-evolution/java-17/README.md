# Java 17 LTS (Sept 2021 — Covering Java 12–17)

> **Data-Oriented Programming & Enterprise Security:** Java 17 replaced decades of verbose DTO boilerplate with Records, brought compile-time closed hierarchies with Sealed Classes, cleaned up `switch`/`instanceof`/multi-line strings, and hardened JVM internals.

---

## ⚡ 2-Minute Rapid Revision Card

- **`record` (JEP 395):** Shallowly immutable data carrier generating canonical constructor, component accessors (`name()`), and `invokedynamic`-backed `equals`/`hashCode`/`toString`. Implicitly **`final`**, extends `java.lang.Record` — defensive copy mutable `List`/`Date` in compact constructors! Not suitable as mutable JPA `@Entity`.
- **`sealed` Classes/Interfaces (JEP 409):** Restrict inheritance via `permits`. Subtypes MUST declare **`final`**, **`sealed`**, or **`non-sealed`**. Enables compile-time exhaustiveness for Algebraic Data Types (Sum + Product types).
- **`switch` Expressions (JEP 361) & `instanceof` Patterns (JEP 394):** `switch` returns values cleanly with `->` (zero fall-through bugs!) and `yield`. `if (obj instanceof String s && !s.isBlank())` eliminates casts (`||` fails flow scoping!).
- **Text Blocks (JEP 378) + Collector/Stream Upgrades:** `"""` strips incidental indentation, supports `\` (join line) & `\s` (trailing space), `.formatted(...)`. Java 12 added `Collectors.teeing()`; Java 16 added `Stream.toList()` & `mapMulti()`.
- **JVM & Security Hardening:** JEP 403 seals `sun.misc.*`/`jdk.internal.*` (`--add-opens` vs `--add-exports`), JEP 358 **Helpful NPE** identifies exact null operand, JEP 356 **`RandomGeneratorFactory`**, JEP 415 **Deserialization Filters**.

---

## 📂 Feature Folders

| # | Feature Folder | Core Concepts Covered | Notes | Codes | Interview |
|---:|---|---|---|---|---|
| **01** | [**Records (`record`)**](01-records/) | Canonical & compact constructors, defensive copying, `ObjectMethods` bytecode, safe deserialization, JPA pitfall | [Note](01-records/notes/records-deep-dive.md) | [Code](01-records/codes/lts/java17/records/RecordsDeepDiveDemo.java) | [Q&A](01-records/interview/INTERVIEW_QUESTIONS.md) |
| **02** | [**Sealed Classes & Interfaces**](02-sealed-classes-and-interfaces/) | `sealed`/`permits`/`final`/`non-sealed`, Sum + Product ADTs, `PermittedSubclasses` bytecode | [Note](02-sealed-classes-and-interfaces/notes/sealed-classes-deep-dive.md) | [Code](02-sealed-classes-and-interfaces/codes/lts/java17/sealedtypes/SealedClassesHierarchyDemo.java) | [Q&A](02-sealed-classes-and-interfaces/interview/INTERVIEW_QUESTIONS.md) |
| **03** | [**Pattern Matching `instanceof` & `switch` Expressions**](03-pattern-matching-instanceof-and-switch-expressions/) | Flow scoping (`&&` vs `||`), `switch` `->` expressions, `yield`, fall-through elimination | [Note](03-pattern-matching-instanceof-and-switch-expressions/notes/instanceof-pattern-and-switch-expr.md) | [Code](03-pattern-matching-instanceof-and-switch-expressions/codes/lts/java17/flow/SwitchExprAndInstanceofPatternDemo.java) | [Q&A](03-pattern-matching-instanceof-and-switch-expressions/interview/INTERVIEW_QUESTIONS.md) |
| **04** | [**Text Blocks, `formatted`, `teeing` & `mapMulti`**](04-text-blocks-and-string-formatting/) | Incidental indentation, `\` / `\s` escapes, `.formatted()`, `Collectors.teeing()`, `Stream.toList()`, `mapMulti` | [Note](04-text-blocks-and-string-formatting/notes/text-blocks-and-stream-additions.md) | [Code](04-text-blocks-and-string-formatting/codes/lts/java17/text/TextBlocksAndStreamAdditionsDemo.java) | [Q&A](04-text-blocks-and-string-formatting/interview/INTERVIEW_QUESTIONS.md) |
| **05** | [**Strong Encapsulation, Helpful NPE, PRNG & Deserialization Filters**](05-strong-encapsulation-prng-and-deserialization-filters/) | JEP 403 `--add-opens`, JEP 358 Helpful NPE, JEP 356 `RandomGeneratorFactory`, JEP 415 `ObjectInputFilter` | [Note](05-strong-encapsulation-prng-and-deserialization-filters/notes/encapsulation-prng-and-security.md) | [Code](05-strong-encapsulation-prng-and-deserialization-filters/codes/lts/java17/security/HelpfulNpePrngAndFilterDemo.java) | [Q&A](05-strong-encapsulation-prng-and-deserialization-filters/interview/INTERVIEW_QUESTIONS.md) |
