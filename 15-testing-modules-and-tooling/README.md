# 15. Testing, JPMS Modules, Build Tools & JVM Diagnostics

> **Why this module exists:** Knowing Java syntax isn't enough for production engineering — you need to write fast **unit tests with clean test doubles**, understand **Maven/Gradle build lifecycles**, navigate **JPMS `module-info.java`**, and troubleshoot live production issues with **`jcmd`, `jstack`, `jmap`, `javap`, and Java Flight Recorder (JFR)**.

---

## ⚡ 2-Minute Rapid Revision Card

- **Unit Testing AAA Pattern:** **A**rrange (setup inputs & fakes) -> **A**ct (invoke method under test) -> **A**ssert (verify outcome). Test behavior, NOT private implementation details!
- **Test Doubles Taxonomy:** **Dummy** (filler arg), **Fake** (working in-memory impl like `InMemoryUserRepo`), **Stub** (returns canned answers), **Spy** (records calls made to real/wrapped object), **Mock** (verifies interaction expectations).
- **JPMS (Java 9 Modules):** `module-info.java` controls **Compile/Runtime visibility** (`exports com.api;` exposes public types; `opens com.model;` allows deep reflection for Jackson/Hibernate/Spring; `requires java.sql;` declares dependencies).
- **Maven Standard Lifecycle:** `validate -> compile -> test -> package -> verify -> install -> deploy`. Scopes: `compile` (default), `provided` (Servlet API at runtime by container), `runtime` (JDBC driver), `test` (JUnit/Mockito).
- **Production CLI Playbook:**
  - `jcmd <pid> Thread.print` or `jstack <pid>` -> Detect deadlocks & blocked threads.
  - `jcmd <pid> GC.heap_dump app.hprof` or `jmap -histo:live <pid>` -> Catch memory leaks.
  - `jcmd <pid> JFR.start duration=60s filename=profile.jfr` -> Ultra-low-overhead (<1%) CPU & allocation profiling!
  - `javap -c -p MyClass` -> Disassemble `.class` bytecode (`invokevirtual`, `invokedynamic`, exception table).

---

## 📂 Contents

| File | Focus |
|---|---|
| [`notes/01-unit-testing-tdd-and-test-doubles.md`](notes/01-unit-testing-tdd-and-test-doubles.md) | AAA pattern, Test Pyramid, Stub vs Fake vs Mock vs Spy, JUnit 5 & Mockito cheatsheet |
| [`notes/02-jpms-java-module-system.md`](notes/02-jpms-java-module-system.md) | `module-info.java`, `exports` vs `opens`, Strong Encapsulation, `jlink` custom JREs |
| [`notes/03-maven-gradle-build-lifecycle.md`](notes/03-maven-gradle-build-lifecycle.md) | `pom.xml` coordinates, lifecycles, dependency scopes, shading/fat JARs |
| [`notes/04-jvm-diagnostic-cli-playbook.md`](notes/04-jvm-diagnostic-cli-playbook.md) | `javac`, `javap`, `jshell`, `jps`, `jcmd`, `jstack`, `jmap`, `jstat`, `jfr` |
| [`codes/`](codes/) | Pure-JDK Unit Testing + Fake/Stub/Spy demo & `assert` / JVM diagnostics demo |
| [`interview/INTERVIEW_QUESTIONS.md`](interview/INTERVIEW_QUESTIONS.md) | 25+ Testing, JPMS, Maven & Diagnostic interview Q&As |

---

## 🚀 Compile & Run

```bash
javac -d out $(find 15-testing-modules-and-tooling/codes -name "*.java")
java -ea -cp out tooling.testing.UnitTestingAndTestDoublesDemo
java -cp out tooling.diagnostics.RuntimeDiagnosticsDemo
```
