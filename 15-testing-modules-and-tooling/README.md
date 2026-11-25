# 15. Testing, JPMS Modules, Build Tools & JVM Diagnostics

> **Why this module exists:** Knowing Java syntax isn't enough for production engineering — you need to write fast **unit tests with clean test doubles**, understand **Maven/Gradle build lifecycles**, navigate **JPMS `module-info.java`**, and troubleshoot live production issues with **`jcmd`, `jstack`, `jmap`, `javap`, and Java Flight Recorder (JFR)**.

---

## ⚡ 2-Minute Rapid Revision Card

- **Unit Testing AAA Pattern:** **A**rrange -> **A**ct -> **A**ssert. Test behavior, NOT private implementation details!
- **Test Doubles:** **Dummy** (filler), **Fake** (working in-memory impl like `InMemoryUserRepo`), **Stub** (canned returns), **Spy** (records calls), **Mock** (verifies interactions).
- **JPMS (Java 9 Modules):** `exports com.api;` exposes public types; `opens com.model;` permits deep reflection for Jackson/Hibernate/Spring; `requires java.sql;` declares dependencies.
- **Maven Standard Lifecycle:** `validate -> compile -> test -> package -> verify -> install -> deploy`. Scopes: `compile`, `provided`, `runtime`, `test`.
- **Production CLI Playbook:** `jcmd <pid> Thread.print` (deadlocks), `jcmd <pid> GC.heap_dump` (leaks), `jcmd <pid> JFR.start` (live profiling), `javap -c -p` (bytecode).

---

## 📂 Contents

| Note | Companion Runnable Labs |
|---|---|
| [`01-unit-testing-tdd-and-test-doubles.md`](notes/01-unit-testing-tdd-and-test-doubles.md) | [`UnitTestingAndTestDoublesDemo.java`](codes/tooling/testing/UnitTestingAndTestDoublesDemo.java) |
| [`02-jpms-java-module-system.md`](notes/02-jpms-java-module-system.md) | [`JpmsAndServiceLoaderDemo.java`](codes/tooling/modules/JpmsAndServiceLoaderDemo.java) |
| [`03-maven-gradle-build-lifecycle.md`](notes/03-maven-gradle-build-lifecycle.md) | Maven/Gradle lifecycle & conflict resolution reference |
| [`04-jvm-diagnostic-cli-playbook.md`](notes/04-jvm-diagnostic-cli-playbook.md) | [`RuntimeDiagnosticsDemo.java`](codes/tooling/diagnostics/RuntimeDiagnosticsDemo.java) |
| **Interview Bank** | 👉 [`interview/INTERVIEW_QUESTIONS.md`](interview/INTERVIEW_QUESTIONS.md) |

---

## 🚀 Compile & Run

```bash
javac -d out $(find 15-testing-modules-and-tooling/codes -name "*.java")
java -ea -cp out tooling.testing.UnitTestingAndTestDoublesDemo
java -cp out tooling.modules.JpmsAndServiceLoaderDemo
java -cp out tooling.diagnostics.RuntimeDiagnosticsDemo
```
