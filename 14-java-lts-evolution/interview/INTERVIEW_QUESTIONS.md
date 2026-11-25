# 14. Java LTS Evolution (Java 8 -> 11 -> 17 -> 21 -> 25) — Master Interview Hub

> **How to use:** Every individual feature folder across `java-08/` through `java-25/` contains its own dedicated `interview/INTERVIEW_QUESTIONS.md` covering deep feature nuances. Use **this file** for cross-version migration, trade-off comparisons, and full-stack architectural interviews!

---

## 📚 Per-Feature Interview Question Banks

- **Java 8:** [Lambdas](java-08/01-lambda-expressions-and-functional-interfaces/interview/INTERVIEW_QUESTIONS.md) | [Stream API](java-08/02-stream-api-exhaustive/interview/INTERVIEW_QUESTIONS.md) | [Optional](java-08/03-optional-class/interview/INTERVIEW_QUESTIONS.md) | [Date/Time](java-08/04-date-and-time-api-jsr310/interview/INTERVIEW_QUESTIONS.md) | [Interfaces](java-08/05-interface-default-static-methods/interview/INTERVIEW_QUESTIONS.md) | [CompletableFuture](java-08/06-completablefuture-async-pipelines/interview/INTERVIEW_QUESTIONS.md) | [Metaspace & CHM](java-08/07-nashorn-base64-and-jvm-metaspace/interview/INTERVIEW_QUESTIONS.md)
- **Java 11:** [`var` Inference](java-11/01-local-variable-var-and-lambda-var/interview/INTERVIEW_QUESTIONS.md) | [HttpClient](java-11/02-standard-httpclient/interview/INTERVIEW_QUESTIONS.md) | [Strings/Files/Factories](java-11/03-string-files-and-collection-factories/interview/INTERVIEW_QUESTIONS.md) | [JPMS/Java9](java-11/04-jpms-modules-and-stream-optional-enhancements/interview/INTERVIEW_QUESTIONS.md) | [JFR/Single-File](java-11/05-single-file-launch-jfr-and-ee-removal/interview/INTERVIEW_QUESTIONS.md)
- **Java 17:** [Records](java-17/01-records/interview/INTERVIEW_QUESTIONS.md) | [Sealed Classes](java-17/02-sealed-classes-and-interfaces/interview/INTERVIEW_QUESTIONS.md) | [`switch`/`instanceof`](java-17/03-pattern-matching-instanceof-and-switch-expressions/interview/INTERVIEW_QUESTIONS.md) | [Text Blocks & `teeing`](java-17/04-text-blocks-and-string-formatting/interview/INTERVIEW_QUESTIONS.md) | [Encapsulation & Security](java-17/05-strong-encapsulation-prng-and-deserialization-filters/interview/INTERVIEW_QUESTIONS.md)
- **Java 21:** [Virtual Threads](java-21/01-virtual-threads-project-loom/interview/INTERVIEW_QUESTIONS.md) | [Sequenced Collections](java-21/02-sequenced-collections/interview/INTERVIEW_QUESTIONS.md) | [`switch` Patterns](java-21/03-pattern-matching-for-switch/interview/INTERVIEW_QUESTIONS.md) | [Record Patterns](java-21/04-record-patterns-and-deconstruction/interview/INTERVIEW_QUESTIONS.md) | [Gen-ZGC & KEM](java-21/05-generational-zgc-kem-and-utf8-defaults/interview/INTERVIEW_QUESTIONS.md)
- **Java 25:** [FFM API](java-25/01-foreign-function-and-memory-api/interview/INTERVIEW_QUESTIONS.md) | [Unnamed `_`](java-25/02-unnamed-variables-and-patterns/interview/INTERVIEW_QUESTIONS.md) | [Stream Gatherers](java-25/03-stream-gatherers/interview/INTERVIEW_QUESTIONS.md) | [`ScopedValue` & Structured Concurrency](java-25/04-scoped-values-and-structured-concurrency/interview/INTERVIEW_QUESTIONS.md) | [Flexible Ctors & Lilliput](java-25/05-flexible-constructors-compact-headers-and-runtime/interview/INTERVIEW_QUESTIONS.md)

---

## 🎯 Top Cross-Version Migration & Architecture Questions

### Q1. Walk me through upgrading a legacy Java 8 backend microservice straight to Java 21 or Java 25 LTS. What breaks first, and what wins immediately?
**What can break:**
1. Removed Java EE modules (`javax.xml.bind` JAXB, `javax.annotation`) dropped in Java 11 -> add Jakarta Maven deps cleanly.
2. Strong Encapsulation (`JEP 403`, Java 17): old Lombok/Kryo/Spring proxies reflecting into `java.base` fail with `InaccessibleObjectException` -> upgrade libraries first, add targeted `--add-opens` only if unavoidable.
3. G1GC is default instead of ParallelGC; deprecated `-XX:+UseConcMarkSweepGC` fails startup cleanly -> switch to G1GC or Generational ZGC!
**Immediate wins without rewriting business logic:**
1. ~30–40% String heap savings automatically via **Compact Strings** (Java 9+).
2. Additional ~10–20% heap savings on Java 25 via **`-XX:+UseCompactObjectHeaders`** (JEP 519).
3. Virtual Threads (`Executors.newVirtualThreadPerTaskExecutor()` / Spring `spring.threads.virtual.enabled=true`) + JEP 491 unpinned `synchronized` monitors in Java 25!

### Q2. Compare `ThreadLocal` (Java 1.2) vs `ScopedValue` (Java 25) vs Reactive Context (WebFlux).
`ThreadLocal` is mutable, manually cleared (`remove()`), and blows up memory with 500k Virtual Threads. Reactive Context ties your entire codebase to `Mono`/`Flux` operators and breaks stack traces. **`ScopedValue`** keeps normal synchronous Java stack traces, binds immutably for a lexical block, auto-clears cleanly, and shares in $O(1)$ across structured child Virtual Threads!

### Q3. Compare Custom Reductions: Java 8 `Collector` vs Java 25 `Gatherer`.
`Collector<T, A, R>` customizes **terminal** operations (turning a stream into a `List`, `Map`, or single summary). **`Gatherer<T, A, R>`** customizes **intermediate** operations (`Stream.gather(...)`) enabling custom batching (`windowFixed`), sliding windows, running scans, distinct-by-key, and short-circuiting lazy transformations *inside* the pipeline!

### Q4. Compare Data-Oriented Programming Evolution: Java 8 POJO -> Java 17 (`record` + `sealed`) -> Java 21 (`switch` + Record Patterns) -> Java 25 (`_`).
- **Java 8:** Verbose immutable classes + `instanceof` casts / Visitor pattern boilerplate.
- **Java 17:** Concise Sum + Product ADTs via `sealed interface` + `record`s.
- **Java 21:** Exhaustive `switch` with `when` guards and deep nested record deconstruction (`case Order(Customer(var id, var tier), _) -> ...`).
- **Java 25:** Unnamed pattern `_` eliminates dummy ignored variables cleanly across nested patterns, `catch`, lambdas, and `try`-with-resources!
