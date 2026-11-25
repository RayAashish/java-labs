# Java 25 — Flexible Constructor Bodies, Compact Object Headers & Runtime (JEP 513/512/519/491)

## ⚡ 2-Minute Revision
- **JEP 513 Flexible Constructor Bodies (Finalized Java 25!):** You can now execute **validation, argument computation, and subclass field initialization BEFORE `super(...)` or `this(...)`**! Solves the classic bug where a parent constructor calls an overridden method and observes `null`/`0` uninitialized subclass fields! (Restriction: cannot read/pass `this` before `super(...)` — only assign subclass fields / local work.)
- **JEP 512 Compact Source Files & Instance `main` (Finalized Java 25!):** Beginners can start with just `void main() { IO.println("Hello!"); }` cleanly — no `public class` or `public static void main(String[] args)` boilerplate (`java.lang.IO` added alongside `java.lang.*`!).
- **JEP 519 Compact Object Headers (Product in Java 25 `-XX:+UseCompactObjectHeaders`):** Project Lilliput collapses HotSpot's 64-bit Mark Word + compressed Klass Word from **12–16 bytes down to 8 bytes** — cutting real-world microservice heap by **10–20%**!
- **JEP 491 Synchronize Virtual Threads Without Pinning (Java 24/25):** Blocking inside `synchronized` **no longer pins carrier OS threads**!
- **JEP 515 Ahead-of-Time Method Profiling (Project Leyden):** Persists JIT profile telemetry across runs so C2 JIT warms up rapidly on startup!

## 📂 Contents
- [`notes/java25-language-and-jvm-upgrades.md`](notes/java25-language-and-jvm-upgrades.md)
- [`codes/lts/java25/ergonomics/FlexibleConstructorBodiesDemo.java`](codes/lts/java25/ergonomics/FlexibleConstructorBodiesDemo.java)
- [`interview/INTERVIEW_QUESTIONS.md`](interview/INTERVIEW_QUESTIONS.md)
