# Java 11 — Single-File Launcher, JFR, EE/CORBA Removal & Runtime APIs

## ⚡ 2-Minute Revision
- **JEP 330 Single-File Source Execution:** Run `java Hello.java` directly in memory without manually invoking `javac` first (supports Unix `#!/usr/bin/env java --source 11` shebangs!). *(Java 22 JEP 458 later extended this to **multi-file** source launches!)*
- **JDK Flight Recorder (JEP 328):** Oracle's commercial **<1% overhead** continuous event profiler became free & open-source in OpenJDK 11!
- **JEP 320 Java EE & CORBA Removal:** Dropped `java.xml.bind` (JAXB), `javax.activation`, `java.xml.ws` (JAX-WS), `java.corba` — migrating apps from Java 8 -> 11 must add Maven dependencies like `jakarta.xml.bind-api`!
- **New Experimental GCs (Java 11):** **Epsilon GC (`-XX:+UseEpsilonGC`)** no-op allocator for microbenchmarks/ultra-short serverless jobs; **ZGC (`-XX:+UseZGC`)** low-latency sub-ms pause collector introduced experimentally (production-ready in Java 15, Generational in Java 21/23).
- **Nest-Based Access Control (JEP 181):** Top-level classes and nested inner classes form a **Nest**, allowing direct private member access via `MethodHandles`/reflection without compiler-generated synthetic bridge `access$000` methods!
- **`ProcessHandle` (Java 9):** Cross-platform PID, parent/child process inspection, and `onExit()` termination futures without parsing `ps`/`tasklist` output.

## 📂 Contents
- [`notes/jfr-single-file-and-runtime-changes.md`](notes/jfr-single-file-and-runtime-changes.md)
- [`codes/lts/java11/runtime/RuntimeApisAndNestmatesDemo.java`](codes/lts/java11/runtime/RuntimeApisAndNestmatesDemo.java)
- [`interview/INTERVIEW_QUESTIONS.md`](interview/INTERVIEW_QUESTIONS.md)
