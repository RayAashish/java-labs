# Java 11 LTS (Sept 2018 — Covering Java 9, 10 & 11)

> **Enterprise Modernization Baseline:** Java 11 reshaped both how Java runs (JPMS modularity, Compact Strings, JFR built-in, no more JEE/CORBA baggage) and everyday developer ergonomics (`var`, `HttpClient`, `String`/`Files` APIs, `List.of`, `takeWhile`, single-file runner).

---

## ⚡ 2-Minute Rapid Revision Card

- **`var` (Java 10 & 11):** Compile-time **static** type inference ONLY (`javac` infers exact bytecode type, NOT dynamic typing!). Java 11 allows `(@NotNull var x) -> ...` in lambdas.
- **`HttpClient` (JEP 321):** Replaces verbose `HttpURLConnection` with HTTP/2 multiplexing + native `sendAsync()` (`CompletableFuture`) + WebSockets. **Reuse ONE singleton client** per base config!
- **`String` & `Files` + Compact Strings:** `isBlank()`, `strip()` (Unicode-aware vs ASCII `trim()`), `lines()`, `repeat()`, `Files.readString/writeString()`. Compact Strings encode ASCII as 1-byte `LATIN1` (~30-40% lower heap!).
- **Collection Factories & Stream/Optional Upgrades (Java 9):** `List.of`/`Map.of` (structurally immutable, forbid `null`), `takeWhile`/`dropWhile`, `Optional.ifPresentOrElse`/`or`/`stream`, `private` interface methods, JPMS `module-info.java`.
- **Runtime & JFR:** Single-file `java MyScript.java` (JEP 330), **JDK Flight Recorder** open-sourced (JEP 328, <1% overhead), JEE/JAXB removed (JEP 320), Epsilon/ZGC GC introduced.

---

## 📂 Feature Folders

| # | Feature Folder | Core Concepts Covered | Notes | Codes | Interview |
|---:|---|---|---|---|---|
| **01** | [**`var` Local Type Inference & Lambda `var`**](01-local-variable-var-and-lambda-var/) | JEP 286/323, valid vs illegal positions, diamond trap (`ArrayList<Object>`), bytecode equivalence | [Note](01-local-variable-var-and-lambda-var/notes/var-type-inference-deep-dive.md) | [Code](01-local-variable-var-and-lambda-var/codes/lts/java11/varinference/VarTypeInferenceAndLambdaDemo.java) | [Q&A](01-local-variable-var-and-lambda-var/interview/INTERVIEW_QUESTIONS.md) |
| **02** | [**Standard `HttpClient` (HTTP/2 & Async)**](02-standard-httpclient/) | JEP 321, sync `send()` vs `sendAsync()`, HTTP/2, connection leak vs singleton client, embedded local test server | [Note](02-standard-httpclient/notes/standard-httpclient-deep-dive.md) | [Code](02-standard-httpclient/codes/lts/java11/httpclient/StandardHttpClientLocalDemo.java) | [Q&A](02-standard-httpclient/interview/INTERVIEW_QUESTIONS.md) |
| **03** | [**`String`, `Files`, Compact Strings & Collection Factories**](03-string-files-and-collection-factories/) | `strip()` vs `trim()`, `isBlank`/`lines`/`repeat`, Compact Strings `LATIN1`, `Files.readString`, `List.of`/`Map.of` traps | [Note](03-string-files-and-collection-factories/notes/string-files-collections-deep-dive.md) | [Code](03-string-files-and-collection-factories/codes/lts/java11/stringsfiles/StringFilesAndCollectionsDemo.java) | [Q&A](03-string-files-and-collection-factories/interview/INTERVIEW_QUESTIONS.md) |
| **04** | [**JPMS Modules, Private Interface & Stream/Optional Upgrades**](04-jpms-modules-and-stream-optional-enhancements/) | `module-info.java` (`requires`/`exports`/`opens`), `private` interface methods, `takeWhile`/`dropWhile`/`ofNullable`, `Collectors.filtering` | [Note](04-jpms-modules-and-stream-optional-enhancements/notes/jpms-and-java9-api-enhancements.md) | [Code](04-jpms-modules-and-stream-optional-enhancements/codes/lts/java11/modular/StreamOptionalAndPrivateInterfaceDemo.java) | [Q&A](04-jpms-modules-and-stream-optional-enhancements/interview/INTERVIEW_QUESTIONS.md) |
| **05** | [**Single-File Launcher, JFR, EE Removal & Runtime APIs**](05-single-file-launch-jfr-and-ee-removal/) | JEP 330 `java Script.java`, JFR (<1% overhead), Epsilon/ZGC, JAXB removal, Nestmates JEP 181, `ProcessHandle` | [Note](05-single-file-launch-jfr-and-ee-removal/notes/jfr-single-file-and-runtime-changes.md) | [Code](05-single-file-launch-jfr-and-ee-removal/codes/lts/java11/runtime/RuntimeApisAndNestmatesDemo.java) | [Q&A](05-single-file-launch-jfr-and-ee-removal/interview/INTERVIEW_QUESTIONS.md) |
