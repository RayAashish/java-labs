# 14. Java LTS Evolution Mastery (Java 8 -> 11 -> 17 -> 21 -> 25)

> **Deep Per-LTS, Per-Feature Architecture:** Every Long-Term Support release has its own dedicated folder (`java-08/` through `java-25/`), and **every major feature inside each LTS has its own self-contained folder** with 3-Layer Noob-to-Senior `notes/`, runnable Bad-vs-Good `codes/`, and dedicated `interview/INTERVIEW_QUESTIONS.md`!

---

## ⚡ 2-Minute Master LTS Revision Card

- **[Java 8 (2014)](java-08/):** **7 Feature Modules** — Lambdas (`invokedynamic`), Exhaustive Streams (Creation / Intermediate / Terminal / Collectors / Primitives / Parallel), `Optional`, `java.time` (JSR-310), Interface `default`/`static`, `CompletableFuture`, Metaspace + `Base64` + CHM V8.
- **[Java 11 (2018)](java-11/):** **5 Feature Modules** — Local `var` & Lambda `var`, Standard HTTP/2 `HttpClient`, `String`/`Files` & `List.of` + Compact Strings, JPMS & Java 9 Stream/Optional upgrades, Single-file `java Foo.java` + JFR + EE Removal.
- **[Java 17 (2021)](java-17/):** **5 Feature Modules** — `record` (immutable carriers & safe serialization), `sealed` Hierarchies & ADTs, `instanceof` Pattern Matching & `switch` Expressions, Text Blocks (`"""`) & `Collectors.teeing`/`mapMulti`, Strong Encapsulation + Helpful NPE + PRNG + `ObjectInputFilter`.
- **[Java 21 (2023)](java-21/):** **5 Feature Modules** — Project Loom Virtual Threads, Sequenced Collections (`getFirst`/`reversed`), Pattern Matching for `switch` (`when` / `case null` / dominance), Nested Record Patterns, Generational ZGC + Post-Quantum `KEM` + UTF-8 Default.
- **[Java 25 (2025)](java-25/):** **5 Feature Modules** — Foreign Function & Memory API (`Arena`/`Linker`), Unnamed Variables & Patterns `_`, Stream Gatherers (`Gatherers.*` + custom `Gatherer`), Finalized `ScopedValue` + `StructuredTaskScope.open(Joiner)`, Flexible Constructor Bodies (`pre-super()`) + Compact Object Headers (Lilliput 8B) + JEP 491 unpinning!

---

## 🧭 Navigation Index Across All 5 LTS Releases (27 Feature Folders)

| LTS Version | Overview | Dedicated Feature Folders | Cross-Check Guide |
|---|---|---|---|
| **Entry Guide** | [Java Versions for Beginners](notes/00-java-versions-for-beginners.md) | Why 6-month cadence + 2-year LTS works | [Master Interview Bank](interview/INTERVIEW_QUESTIONS.md) |
| **Java 8 LTS** | [Java 8 Overview](java-08/README.md) | [`01 Lambdas`](java-08/01-lambda-expressions-and-functional-interfaces/) • [`02 Stream API (Exhaustive)`](java-08/02-stream-api-exhaustive/) • [`03 Optional`](java-08/03-optional-class/) • [`04 Date/Time`](java-08/04-date-and-time-api-jsr310/) • [`05 Interface Defaults`](java-08/05-interface-default-static-methods/) • [`06 CompletableFuture`](java-08/06-completablefuture-async-pipelines/) • [`07 Base64 & Metaspace`](java-08/07-nashorn-base64-and-jvm-metaspace/) | [Java 8 Docs](java-08/) |
| **Java 11 LTS** | [Java 11 Overview](java-11/README.md) | [`01 var Inference`](java-11/01-local-variable-var-and-lambda-var/) • [`02 HttpClient`](java-11/02-standard-httpclient/) • [`03 String/Files/Collections`](java-11/03-string-files-and-collection-factories/) • [`04 JPMS & Streams9`](java-11/04-jpms-modules-and-stream-optional-enhancements/) • [`05 Single-File/JFR/EE`](java-11/05-single-file-launch-jfr-and-ee-removal/) | [Java 11 Docs](java-11/) |
| **Java 17 LTS** | [Java 17 Overview](java-17/README.md) | [`01 Records`](java-17/01-records/) • [`02 Sealed Classes`](java-17/02-sealed-classes-and-interfaces/) • [`03 Switch Expr & instanceof`](java-17/03-pattern-matching-instanceof-and-switch-expressions/) • [`04 Text Blocks & teeing`](java-17/04-text-blocks-and-string-formatting/) • [`05 Encapsulation & Security`](java-17/05-strong-encapsulation-prng-and-deserialization-filters/) | [Java 17 Docs](java-17/) |
| **Java 21 LTS** | [Java 21 Overview](java-21/README.md) | [`01 Virtual Threads`](java-21/01-virtual-threads-project-loom/) • [`02 Sequenced Collections`](java-21/02-sequenced-collections/) • [`03 Switch Patterns`](java-21/03-pattern-matching-for-switch/) • [`04 Record Patterns`](java-21/04-record-patterns-and-deconstruction/) • [`05 Gen-ZGC, KEM & UTF-8`](java-21/05-generational-zgc-kem-and-utf8-defaults/) | [Java 21 Docs](java-21/) |
| **Java 25 LTS** | [Java 25 Overview](java-25/README.md) | [`01 FFM API`](java-25/01-foreign-function-and-memory-api/) • [`02 Unnamed _`](java-25/02-unnamed-variables-and-patterns/) • [`03 Stream Gatherers`](java-25/03-stream-gatherers/) • [`04 ScopedValue & StructuredTaskScope`](java-25/04-scoped-values-and-structured-concurrency/) • [`05 Flexible Ctors & Compact Headers`](java-25/05-flexible-constructors-compact-headers-and-runtime/) | [Java 25 Docs](java-25/) |

---

## 🚀 Quick Compile & Run (Any Feature or Entire LTS Suite)

```bash
# Compile ALL 27 feature labs cleanly on JDK 25
javac --enable-preview --release 25 -d out $(find 14-java-lts-evolution -name "*.java")

# Sample runs from each LTS generation:
java -cp out lts.java08.streams.StreamCollectorsAndPrimitivesDemo
java -cp out lts.java11.httpclient.StandardHttpClientLocalDemo
java -cp out lts.java17.sealedtypes.SealedClassesHierarchyDemo
java -cp out lts.java21.recordpatterns.NestedRecordPatternsDemo
java -cp out lts.java25.gatherers.StreamGatherersMasteryDemo
java --enable-preview -cp out lts.java25.concurrency.StructuredTaskScopeJoinerDemo
```
