# 04. JVM Diagnostic & CLI Tooling Playbook

---

## 🟢 Layer 1 — Essential JDK CLI Tools Every Developer Uses

| Tool | What It Does | Quick Example |
|---|---|---|
| `javac` | Compiles `.java` source into `.class` bytecode | `javac -d out Main.java` |
| `java` | Launches JVM (or runs single `.java` file directly!) | `java -cp out pkg.Main` |
| `jshell` | Interactive REPL for testing Java snippets instantly | `jshell` |
| `jar` | Inspects or packages `.jar` archives | `jar tf app.jar` |
| `javap` | Disassembles `.class` bytecode instructions | `javap -c -p Main` |

---

## 🟡/🔴 Layer 2 & 3 — Production Incident Triage with `jcmd`, `jstack`, `jmap`, `jfr`

```text
Production Symptom ──► First Command to Run
───────────────────────────────────────────────────────────────────────
1. App frozen / high latency / deadlock?
   └─► jcmd <pid> Thread.print          (or jstack <pid> > threads.tdump)
       Look for: BLOCKED threads, "Found one Java-level deadlock"

2. Memory growing endlessly (OutOfMemoryError)?
   └─► jmap -histo:live <pid> | head -25        (quick live object histogram)
   └─► jcmd <pid> GC.heap_dump /tmp/heap.hprof  (open in Eclipse MAT / VisualVM)
   └─► Startup safety flag: -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/dumps

3. High CPU usage (100% core spike)?
   └─► jcmd <pid> JFR.start name=cpu duration=60s settings=profile filename=/tmp/cpu.jfr
       Open cpu.jfr in JDK Mission Control (JMC) -> hot methods, locks, allocations!

4. Inspect active JVM flags, GC & Heap sizes on live container:
   └─► jcmd <pid> VM.flags
   └─► jstat -gcutil <pid> 1000         (live Eden/Survivor/Old/Metaspace % every 1s)
```
