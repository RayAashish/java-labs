# 10. JVM Internals, Memory Architecture, GC & Performance Tuning

> **Learning Method:** Start at **🟢 Noob Track** (`00_jvm_explained_for_beginners.md` — what actually happens when you run `java Main`), then climb into **🟡/🔴 Senior Architect Track** (ClassLoaders, Metaspace, Object Headers, JIT Escape Analysis, G1 vs ZGC Colored Pointers, MESI Cache Coherence, Bytecode & JFR profiling).

---

## ⚡ 2-Minute Rapid Revision Card

- **JDK vs JRE vs JVM:** `JVM` executes bytecode; `JRE` = `JVM` + standard class libraries; `JDK` = `JRE` + compiler (`javac`), disassembler (`javap`), profiler (`jfr`, `jcmd`).
- **ClassLoader Delegation Model:** Application -> Platform -> Bootstrap (delegates **UP** first to prevent `java.lang.String` spoofing; loads **DOWN** if parent can't find it). Tomcat/OSGi invert this for plugin isolation.
- **64-bit Object Header (Compressed OOPs):** `Mark Word` (8B: hash, GC age, lock flags) + `Klass Word` (4B) + fields + 8-byte alignment padding -> empty `new Object()` = **16 bytes** (`-XX:+UseCompactObjectHeaders` in Java 24/25 shrinks headers to 8 bytes!).
- **JIT Tiered Compilation:** Interpreter (Tier 0) -> C1 fast compiler (Tier 1–3) -> C2 aggressive optimizing compiler (Tier 4: **Inlining**, **Escape Analysis -> Scalar Replacement & Lock Elision**, Loop Unrolling).
- **Garbage Collectors:** **G1 GC** (default balanced region collector) vs **Generational ZGC** (`-XX:+UseZGC -XX:+ZGenerational` — colored pointers + load barriers for sub-millisecond pauses on multi-TB heaps).
- **Diagnostics:** `jcmd <pid> VM.flags`, `jstack <pid>` (thread dumps/deadlocks), `jmap -histo:live <pid>`, `jcmd <pid> JFR.start` (Java Flight Recorder).

---

## 📂 Contents

| Resource | Link |
|---|---|
| **🟢 Beginner Bridge** | [`notes/00_jvm_explained_for_beginners.md`](notes/00_jvm_explained_for_beginners.md) + `internals.beginner.BeginnerStackVsHeapDemo` |
| **🔴 Architect Notes (01–08)** | [`notes/`](notes/) (`01_jdk_jre_jvm...` through `08_architect_troubleshooting...`) |
| **📊 Visual Diagrams (6)** | [`diagrams/`](diagrams/) (JVM Master, Heap Map, ClassLoader, JIT, GC/ZGC, JMM) |
| **💻 Runnable Labs** | [`codes/internals/`](codes/internals/) |
| **🎯 Interview Questions** | [`interview/INTERVIEW_QUESTIONS.md`](interview/INTERVIEW_QUESTIONS.md) |

---

## 🚀 Compile & Run

```bash
javac -d out $(find 10-jvm-internals-and-performance/codes -name "*.java")

java -cp out internals.beginner.BeginnerStackVsHeapDemo
java -cp out internals.memory.ObjectHeaderAndMemoryLayoutDemo
java -cp out internals.jit.EscapeAnalysisAndOptimizationDemo
java -cp out internals.concurrency.FalseSharingAndCacheLinesDemo
java -cp out internals.gc.TLABAllocationPathDemo
java -cp out internals.classloader.CustomClassLoaderDemo
```
