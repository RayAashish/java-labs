# Interview Questions — Java 11 JFR, Single-File Launch & Runtime Changes

## 🟢 Beginner

### Q1. How does `java Hello.java` (JEP 330) work differently from `javac Hello.java && java Hello`?
`java Hello.java` compiles the single source file directly into **memory** (leaving zero `.class` clutter on disk) and immediately invokes its `main` method.

### Q2. Why did migrating older Java 8 Enterprise / Spring apps to Java 11 often break XML (`JAXB`) or `@PostConstruct`?
JEP 320 cleanly **removed Java EE and CORBA modules** (`java.xml.bind`, `java.xml.ws`, `javax.annotation`, `java.corba`) from the JDK because they belong to Jakarta EE / third-party Maven dependencies rather than the core Java SE runtime.

---

## 🟡 Intermediate

### Q3. What is JDK Flight Recorder (JFR) and why can it stay enabled in production?
JFR is a binary circular-buffer profiling engine built directly into HotSpot (open-sourced in Java 11 JEP 328). Rather than instrumenting bytecode slowly like external agents, HotSpot emits events straight from native VM/JIT/GC hooks at **<1% CPU overhead**.

### Q4. When would you ever use Epsilon GC (`-XX:+UseEpsilonGC`)?
Epsilon allocates memory without ever collecting garbage. It is strictly useful for:
1. Ultra-short-lived CLI/Lambda jobs that terminate well below `-Xmx`.
2. Latency/allocation microbenchmarks where GC noise must be eliminated completely.

---

## 🔴 Senior

### Q5. What bytecode change did Nestmates (JEP 181) introduce for Inner Classes?
Prior to Java 11, accessing a `private` outer field from an inner class required `javac` to inject package-private synthetic `access$000()` bridge methods, weakening encapsulation and breaking `MethodHandles.lookup()` reflection. JEP 181 added **`NestHost`** and **`NestMembers`** attributes inside `.class` files so the JVM verifies mutual private access natively inside a nest.
