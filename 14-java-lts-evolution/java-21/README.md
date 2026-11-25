# Java 21 LTS (Sept 2023 — Covering Java 18–21)

> **Concurrency & Pattern Revolution:** Java 21 changed backend scalability forever with **Project Loom Virtual Threads**, unified collection endpoints with **Sequenced Collections**, and unlocked expressive Functional/ADT code with **Switch Pattern Matching + Nested Record Patterns**.

---

## ⚡ 2-Minute Rapid Revision Card

- **Virtual Threads (JEP 444):** JVM-scheduled M:N user-mode fibers backed by `ForkJoinPool` carrier threads (`Continuation` yield/resume on blocking I/O). Supports **hundreds of thousands** of concurrent blocking tasks cleanly! **Never pool virtual threads** (use `Semaphore` to cap downstream DB connections), watch **Java 21 `synchronized` pinning** (fixed in Java 24/25 JEP 491!), and don't bloat them with giant `ThreadLocal` buffers!
- **Sequenced Collections (JEP 431):** Unified `getFirst()`, `getLast()`, `addFirst()`, `addLast()`, and $O(1)$ live **`reversed()`** view across `List`, `Deque`, `LinkedHashSet`, `SortedSet`, and `SequencedMap` (`firstEntry()`, `lastEntry()`, `putFirst()`).
- **Pattern Matching for `switch` (JEP 441):** Supports type patterns, **`case null`**, **`when` guards**, dominance checking (specific before general!), sealed exhaustiveness, and `SwitchBootstraps.typeSwitch`.
- **Record Patterns (JEP 440):** Deeply deconstruct nested records (`case Order(Customer(var id, _), Amount(var amt)) -> ...`) inside both `instanceof` and `switch`.
- **Platform, Crypto & GC (JEP 400 / 408 / 439 / 452):** **UTF-8 default `Charset`** everywhere, **`SimpleFileServer` (`jwebserver`)**, **Generational ZGC**, and standard **KEM API** (`javax.crypto.KEM`) for post-quantum key encapsulation!

---

## 📂 Feature Folders

| # | Feature Folder | Core Concepts Covered | Notes | Codes | Interview |
|---:|---|---|---|---|---|
| **01** | [**Virtual Threads (Project Loom)**](01-virtual-threads-project-loom/) | JEP 444, `Thread.ofVirtual()`, `Continuation` unmount, Pinning vs `ReentrantLock`, `Semaphore` rate-limiting, `ThreadLocal` risk | [Note](01-virtual-threads-project-loom/notes/virtual-threads-loom-deep-dive.md) | [Code](01-virtual-threads-project-loom/codes/lts/java21/loom/VirtualThreadsDeepDiveDemo.java) | [Q&A](01-virtual-threads-project-loom/interview/INTERVIEW_QUESTIONS.md) |
| **02** | [**Sequenced Collections**](02-sequenced-collections/) | JEP 431 `SequencedCollection`, `SequencedSet`, `SequencedMap`, `reversed()` live view, `unmodifiableSequenced*` | [Note](02-sequenced-collections/notes/sequenced-collections-deep-dive.md) | [Code](02-sequenced-collections/codes/lts/java21/sequenced/SequencedCollectionsDeepDiveDemo.java) | [Q&A](02-sequenced-collections/interview/INTERVIEW_QUESTIONS.md) |
| **03** | [**Pattern Matching for `switch`**](03-pattern-matching-for-switch/) | JEP 441 `case null`, `when` guards, Dominance rule, exhaustiveness, `SwitchBootstraps`, `MatchException` | [Note](03-pattern-matching-for-switch/notes/switch-pattern-matching-deep-dive.md) | [Code](03-pattern-matching-for-switch/codes/lts/java21/switchpatterns/SwitchPatternMatchingDeepDiveDemo.java) | [Q&A](03-pattern-matching-for-switch/interview/INTERVIEW_QUESTIONS.md) |
| **04** | [**Record Patterns & Nested Deconstruction**](04-record-patterns-and-deconstruction/) | JEP 440 nested deconstruction, generic type inference, paired with sealed `switch` + guards | [Note](04-record-patterns-and-deconstruction/notes/record-patterns-deep-dive.md) | [Code](04-record-patterns-and-deconstruction/codes/lts/java21/recordpatterns/NestedRecordPatternsDemo.java) | [Q&A](04-record-patterns-and-deconstruction/interview/INTERVIEW_QUESTIONS.md) |
| **05** | [**Generational ZGC, KEM API, UTF-8 & Simple Web Server**](05-generational-zgc-kem-and-utf8-defaults/) | JEP 439 Gen-ZGC, JEP 452 `KEM` key encapsulation, JEP 400 UTF-8 default, JEP 408 `jwebserver` | [Note](05-generational-zgc-kem-and-utf8-defaults/notes/zgc-kem-utf8-and-jwebserver.md) | [Code](05-generational-zgc-kem-and-utf8-defaults/codes/lts/java21/runtime/KemUtf8AndCharsetDemo.java) | [Q&A](05-generational-zgc-kem-and-utf8-defaults/interview/INTERVIEW_QUESTIONS.md) |
