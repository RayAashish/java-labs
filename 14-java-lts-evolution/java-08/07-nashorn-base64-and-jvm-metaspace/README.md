# Java 8 — `Base64`, JVM Metaspace, `HashMap`/`ConcurrentHashMap` V8 & Annotations

## ⚡ 2-Minute Revision
- **JVM Memory (`PermGen -> Metaspace`):** Replaced fixed-heap Permanent Generation (notorious for `OutOfMemoryError: PermGen space` during Spring/redeploy) with **native OS memory Metaspace**! Still set `-XX:MaxMetaspaceSize=512m` in containers to cap classloader leaks.
- **`java.util.Base64`:** Official standard encoder/decoder (**Basic**, **URL/Filename-safe**, **MIME**), eliminating internal `sun.misc.BASE64Encoder` hacks.
- **Data Structures V8 Overhaul:**
  - `HashMap`: Bucket linked lists turn into balanced **Red-Black Trees** when bucket count >= **8** (and capacity >= **64**) -> worst-case $O(N)$ DoS becomes $O(\log N)$!
  - `ConcurrentHashMap`: Drops Java 7's fixed 16 `Segment` locks -> uses **CAS on empty bins + `synchronized` on ONLY the bin's head node**!
  - `LongAdder` / `StampedLock`: Striped counter cells eliminate high-contention `AtomicLong` CAS spin bottlenecks.
- **`@Repeatable` Annotations:** Apply `@Schedule` / `@Role` multiple times cleanly on one declaration instead of manually wrapping inside container annotations.
- **Nashorn JS Engine:** Replaced Rhino in Java 8 -> later **deprecated in Java 11** and **removed in Java 15** (succeeded externally by GraalJS).

## 📂 Contents
- [`notes/metaspace-base64-chm-java8.md`](notes/metaspace-base64-chm-java8.md)
- [`codes/lts/java08/runtime/Java8Base64ConcurrencyAndAnnotationsDemo.java`](codes/lts/java08/runtime/Java8Base64ConcurrencyAndAnnotationsDemo.java)
- [`interview/INTERVIEW_QUESTIONS.md`](interview/INTERVIEW_QUESTIONS.md)
