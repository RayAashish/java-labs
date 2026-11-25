# Single-File Launcher, JFR, EE Removal & Runtime Changes (Java 11)

---

## 🟢 Layer 1 — Noob Track: Plain-English Highlights

1. **Zero-Friction Scripting:** Before Java 11, running a 10-line experiment required `javac Test.java && java Test` leaving a messy `Test.class` behind. From Java 11, **`java Test.java`** compiles straight into RAM and runs immediately!
2. **Built-in Production Profiler (JFR):** `jcmd <pid> JFR.start duration=60s filename=trace.jfr` captures CPU allocation spikes, GC pauses, thread locks, and socket I/O safely in production with **under 1% overhead**.
3. **Deleted Legacy Tech in Java 11:** Applets, Java Web Start, `javax.xml.bind` (JAXB), SOAP JAX-WS, and CORBA were stripped out — keeping the JDK lean and cloud-native.

---

## 🟡 Layer 2 — Practitioner Track: Java 8 -> Java 11 Migration Checklist

| Removed in Java 11 | Production Fix |
|---|---|
| `javax.xml.bind.*` (JAXB XML parsing) | Add explicit Maven coordinates (`jakarta.xml.bind:jakarta.xml.bind-api` + runtime) |
| `javax.annotation.PostConstruct` / `@Resource` | Add `jakarta.annotation:jakarta.annotation-api` |
| `sun.misc.BASE64Encoder` | Switch to `java.util.Base64` (Java 8) |
| Legacy GC Flags (`-XX:+UseConcMarkSweepGC`) | Migrate to **G1GC** (default since Java 9) or **ZGC** (Java 15+/21+) |

### Epsilon GC (`-XX:+UseEpsilonGC`) & `ProcessHandle`
- **Epsilon** allocates objects in TLABs and **never performs GC** (shuts down with `OutOfMemoryError` if heap fills). Ideal for latency-noise-free JMH allocation benchmarks or short-lived batch CLI jobs that never fill Xmx!
- **`ProcessHandle.current()`:** Gives clean OS PID, user, CPU duration, and child processes without fragile shell parsing.

---

## 🔴 Layer 3 — Senior / JVM Track: Nestmates (JEP 181)

In Java 8, a `private` field accessed between an `Outer` class and its `Inner` class forced `javac` to quietly generate synthetic package-private bridge methods named **`access$000()`**!
Even worse: calling `innerField.setAccessible(true)` via Reflection or `MethodHandles.Lookup` failed or needed awkward hacks because JVM bytecode considered `Outer` and `Outer$Inner` two completely unrelated classes!

**Java 11 Nestmates (`JEP 181`)** adds **`NestHost`** and **`NestMembers`** attributes directly in the `.class` binary format:
- HotSpot grants `Outer` and its nested classes native JVM private access rights to each other (`Outer.class.getNestMembers()`).
- Eliminates synthetic `access$000` bytecode bloat and cleans up `MethodHandles` reflection.
