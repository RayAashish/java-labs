# 14. Java LTS Evolution Mastery (Java 8 -> 11 -> 17 -> 21 -> 25)

> **Why this matters:** Most job interviews test what changed between legacy Java 8 and modern Java 17/21/25 LTS — and *why* Records, Sealed Classes, Pattern Matching, Virtual Threads, and FFM replaced old boilerplate!

---

## ⚡ 2-Minute Rapid Revision Card

- **Java 8 (2014):** Lambdas, Streams, `Optional`, `java.time`, Interface `default`/`static` methods.
- **Java 11 (2018):** `HttpClient` (HTTP/2 + async), `var` in lambda parameters, `String.isBlank/strip/lines`, single-file `java Hello.java` runner, Applets/JavaFX removed from JDK.
- **Java 17 (2021):** `record` (immutable data carrier), `sealed` classes (`permits`), `instanceof` pattern matching, Text Blocks (`"""`), `switch` expressions (`->`/`yield`), Strong encapsulation of JDK internals (`JEP 403`).
- **Java 21 (2023):** **Virtual Threads** (Project Loom — millions of concurrent I/O tasks), **Sequenced Collections** (`getFirst`/`getLast`/`reversed`), Record Patterns + Exhaustive Pattern Matching in `switch`.
- **Java 25 (2025):** **Foreign Function & Memory API** (`Arena`/`MemorySegment` replacing `Unsafe`/JNI), **Unnamed Variables `_`**, **Stream Gatherers** (`Gatherers.windowFixed`), **Scoped Values** (`ScopedValue` replacing `ThreadLocal`), Flexible Constructor Bodies, Compact Object Headers, and JEP 491 monitor unpinning!

---

## 📂 Contents

| File | Focus |
|---|---|
| [`notes/00-java-versions-for-beginners.md`](notes/00-java-versions-for-beginners.md) | Plain-English tour of why Java releases LTS versions every 2 years |
| [`notes/01-Java8-Lambdas-Streams-And-Time-API.md`](notes/01-Java8-Lambdas-Streams-And-Time-API.md) | Java 8 functional revolution |
| [`notes/02-Java11-Modularity-HttpClient-And-String-Enhancements.md`](notes/02-Java11-Modularity-HttpClient-And-String-Enhancements.md) | Java 11 enterprise baseline |
| [`notes/03-Java17-Records-SealedClasses-And-PatternMatching.md`](notes/03-Java17-Records-SealedClasses-And-PatternMatching.md) | Java 17 Data-Oriented Programming |
| [`notes/04-Java21-VirtualThreads-SequencedCollections-And-SwitchPatterns.md`](notes/04-Java21-VirtualThreads-SequencedCollections-And-SwitchPatterns.md) | Java 21 Concurrency Revolution |
| [`notes/05-Java25-Finalized-Features-And-Runtime-Improvements.md`](notes/05-Java25-Finalized-Features-And-Runtime-Improvements.md) | Java 25 Modern Enterprise Baseline |
| [`codes/lts/`](codes/lts/) | 9 runnable demos across Java 8, 11, 17, 21, and 25 |
| [`interview/INTERVIEW_QUESTIONS.md`](interview/INTERVIEW_QUESTIONS.md) | 30+ LTS Evolution interview Q&As |

---

## 🚀 Compile & Run

```bash
javac -d out $(find 14-java-lts-evolution/codes -name "*.java")

java -cp out lts.java08.StreamApiIntermediateVsTerminalDemo
java -cp out lts.java11.VarInLambdasAndStringApiDemo
java -cp out lts.java17.SealedClassDomainHierarchyDemo
java -cp out lts.java21.SequencedCollectionsAndSwitchPatternsDemo
java -cp out lts.java25.ModernJdk25ErgonomicsAndFeaturesDemo
```
