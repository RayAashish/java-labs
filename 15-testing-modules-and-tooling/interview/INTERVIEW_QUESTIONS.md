# 15. Testing, JPMS Modules, Maven & JVM Diagnostics — Interview Questions & Answers

---

## 🟢 Beginner Questions

### Q1. What is the AAA pattern in Unit Testing?
**Arrange** (prepare inputs & test doubles) -> **Act** (call the method under test) -> **Assert** (verify expected return value, state change, or thrown exception).

### Q2. Why is Java's built-in `assert` keyword disabled by default, and what replaces it?
`assert condition;` is ignored by the JVM unless explicitly enabled with `-ea` (`-enableassertions`) and should only verify internal invariants in dev, never public API input validation. Production unit tests always use **JUnit 5** (`assertEquals`, `assertThrows`, `assertAll`).

### Q3. What are the core Maven lifecycle commands?
`mvn clean` (deletes `target/`), `mvn compile`, `mvn test` (runs unit tests), `mvn package` (builds `.jar`/`.war`), `mvn verify` (runs integration tests), `mvn install` (installs to local `~/.m2` repo).

---

## 🟡 Intermediate Questions

### Q4. Differentiate Dummy, Fake, Stub, Spy, and Mock.
- **Dummy:** Empty placeholder arg never invoked.
- **Fake:** Real working lightweight alternative (`InMemoryUserRepository` backed by `HashMap` instead of MySQL).
- **Stub:** Returns pre-programmed canned responses (`when(repo.find(1)).thenReturn(...)`).
- **Spy:** Wraps a real/stub object and records call history/counts.
- **Mock:** Pre-programmed expectations verifying exact method interactions (`verify(emailService, times(1)).send(...)`).

### Q5. Maven Dependency Scopes: `compile` vs `provided` vs `runtime` vs `test`?
- `compile`: Needed everywhere (compile, test, packaged JAR).
- `provided`: Needed to compile & test, **excluded from final WAR/JAR** because container supplies it (e.g., `jakarta.servlet-api`, `lombok`).
- `runtime`: Not needed to compile code, **bundled at runtime** (e.g., `mysql-connector-j`, SLF4J implementation).
- `test`: Only available during `test-compile` & `test` (JUnit, Mockito, Testcontainers).

### Q6. `exports` vs `opens` in JPMS (`module-info.java`)?
- `exports pkg;`: Exposes `public` types at compile-time and runtime (blocks private reflection).
- `opens pkg;` (or `opens pkg to jackson.databind;`): Permits **deep runtime reflection** into private fields/constructors without opening compile-time dependencies!

### Q7. How do you inspect compiled `.class` bytecode?
Run `javap -c -p -v MyClass.class`: `-c` disassembles bytecode instructions (`invokevirtual`, `invokedynamic`), `-p` includes `private` members, `-v` prints Constant Pool, StackMapTable, and Exception Tables!

---

## 🔴 Senior / Production Troubleshooting Questions

### Q8. How do you diagnose a frozen or deadlocked Java microservice in production?
1. Find PID via `jps -l` or `ps aux | grep java`.
2. Capture 3 thread dumps 5 seconds apart: `jcmd <pid> Thread.print > dump1.txt` (or `jstack -l <pid>`).
3. Search for `"Found one Java-level deadlock"`, threads stuck in `BLOCKED` (waiting on `ObjectMonitor`) or `WAITING` on external HTTP/DB sockets without timeouts.

### Q9. How do you investigate a creeping `OutOfMemoryError` in Kubernetes?
1. Always deploy with `-XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/dumps/oom.hprof` and container-aware `-XX:MaxRAMPercentage=75.0`.
2. For live inspection without crashing: `jmap -histo:live <pid> | head -30` (fast class histogram) or `jcmd <pid> GC.heap_dump /tmp/live.hprof`.
3. Analyze the Dominator Tree & GC Roots in **Eclipse MAT** to find the exact static map / cache / `ThreadLocal` retaining memory.

### Q10. Why is `jcmd <pid> JFR.start` (Java Flight Recorder) preferred over traditional sampling/instrumentation profilers in production?
Classic profilers either distort JIT inlining (instrumentation) or suffer **Safepoint Bias** (only sampling threads paused at safepoints!). **JFR** hooks directly into JVM internals with **< 1% overhead**, capturing CPU hotspots, lock contention, GC pauses, socket I/O, and object allocations safely in live production!

### Q11. How do Maven and Gradle differ when resolving transitive version conflicts?
- **Maven:** Uses **nearest definition** in the dependency tree (first declared wins on ties). Inspect via `mvn dependency:tree -Dverbose`.
- **Gradle:** Uses **highest version wins** by default.

### Q12. Why does `@SpringBootTest` + `@Transactional` sometimes give false-positive persistence tests?
Rolling back the test transaction at method end prevents Hibernate `flush()` or triggers lazy loading while the Hibernate `Session` is still open — masking `LazyInitializationException` and constraint errors that only fire on actual `commit()` in production!
