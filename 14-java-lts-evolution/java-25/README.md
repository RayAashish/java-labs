# Java 25 LTS (Sept 2025 — Covering Java 22–25)

> **Modern Enterprise Baseline:** Java 25 matures Project Panama (FFM API), Project Amber (Unnamed `_`, Flexible Constructors, Instance `main`), Project Loom (`ScopedValue`, `synchronized` Unpinning, `StructuredTaskScope`), Project Leyden (AOT Cache/Profiling), and Project Lilliput (Compact Object Headers).

---

## ⚡ 2-Minute Rapid Revision Card

- **Foreign Function & Memory API (JEP 454, Java 22+):** `Arena` + `MemorySegment` + `MemoryLayout` + `Linker` — deterministic off-heap memory & native C calls replacing unsafe `sun.misc.Unsafe` and slow JNI boilerplate!
- **Unnamed Variables & Patterns `_` (JEP 456, Java 22+):** Cleanly mark ignored exceptions (`catch (Exception _)`), unused lambda args (`(_, v) -> ...`), loop items (`for (Item _ : items)`), and record components (`Point(var x, _)`).
- **Stream Gatherers (JEP 485, Java 24/25):** First standard intermediate Stream extension hook! Built-in `Gatherers.windowFixed(n)`, `windowSliding(n)`, `scan`, `fold`, `mapConcurrent`, plus custom `Gatherer.ofSequential(...)`.
- **`ScopedValue` (JEP 506, Finalized Java 25) & `StructuredTaskScope` (JEP 505, Preview Java 25):** Immutable thread-scoped values replacing leaky `ThreadLocal`, paired with `StructuredTaskScope.open(Joiner...)` for structured cancellation without orphan threads!
- **Java 25 Language & HotSpot Upgrades:**
  - **JEP 513 Flexible Constructor Bodies:** Validate args & init subclass fields **before `super(...)`**!
  - **JEP 512 Compact Source Files & `IO.println`:** Clean `void main()` scripting ergonomics!
  - **JEP 519 Compact Object Headers:** **12–16B -> 8B** (`-XX:+UseCompactObjectHeaders`) — ~10–20% lower microservice RAM!
  - **JEP 491 Unpinned `synchronized` on Virtual Threads:** Monitor locking no longer pins OS carriers!

---

## 📂 Feature Folders

| # | Feature Folder | Core Concepts Covered | Notes | Codes | Interview |
|---:|---|---|---|---|---|
| **01** | [**Foreign Function & Memory (FFM) API**](01-foreign-function-and-memory-api/) | `Arena` (`ofConfined`/`ofAuto`), `MemorySegment`, `StructLayout`, native C `Linker` `strlen` | [Note](01-foreign-function-and-memory-api/notes/ffm-api-deep-dive.md) | [Code](01-foreign-function-and-memory-api/codes/lts/java25/ffm/FfmMemoryAndDowncallDemo.java) | [Q&A](01-foreign-function-and-memory-api/interview/INTERVIEW_QUESTIONS.md) |
| **02** | [**Unnamed Variables & Patterns (`_`)**](02-unnamed-variables-and-patterns/) | JEP 456 `_` in `catch`, lambdas, loops, try-with-resources, record patterns | [Note](02-unnamed-variables-and-patterns/notes/unnamed-variables-deep-dive.md) | [Code](02-unnamed-variables-and-patterns/codes/lts/java25/unnamed/UnnamedVariablesAndPatternsDemo.java) | [Q&A](02-unnamed-variables-and-patterns/interview/INTERVIEW_QUESTIONS.md) |
| **03** | [**Stream Gatherers (`Stream.gather`)**](03-stream-gatherers/) | JEP 485 `windowFixed`, `windowSliding`, `scan`, `fold`, `mapConcurrent`, custom `Gatherer` | [Note](03-stream-gatherers/notes/stream-gatherers-deep-dive.md) | [Code](03-stream-gatherers/codes/lts/java25/gatherers/StreamGatherersMasteryDemo.java) | [Q&A](03-stream-gatherers/interview/INTERVIEW_QUESTIONS.md) |
| **04** | [**`ScopedValue` & Structured Concurrency**](04-scoped-values-and-structured-concurrency/) | JEP 506 finalized `ScopedValue` vs `ThreadLocal`; JEP 505 `StructuredTaskScope.open(Joiner)` | [Note](04-scoped-values-and-structured-concurrency/notes/scoped-values-and-structured-concurrency.md) | [2 Labs](04-scoped-values-and-structured-concurrency/codes/lts/java25/concurrency/) | [Q&A](04-scoped-values-and-structured-concurrency/interview/INTERVIEW_QUESTIONS.md) |
| **05** | [**Flexible Constructors, Compact Headers & Runtime**](05-flexible-constructors-compact-headers-and-runtime/) | JEP 513 pre-`super()` statements, JEP 512 instance `main`, JEP 519 Lilliput 8B headers, JEP 491 unpinning, JEP 515 AOT profiling | [Note](05-flexible-constructors-compact-headers-and-runtime/notes/java25-language-and-jvm-upgrades.md) | [Code](05-flexible-constructors-compact-headers-and-runtime/codes/lts/java25/ergonomics/FlexibleConstructorBodiesDemo.java) | [Q&A](05-flexible-constructors-compact-headers-and-runtime/interview/INTERVIEW_QUESTIONS.md) |
