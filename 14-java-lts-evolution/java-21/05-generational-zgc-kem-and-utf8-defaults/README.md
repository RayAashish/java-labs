# Java 21 — Generational ZGC, KEM API, UTF-8 Default & Simple Web Server

## ⚡ 2-Minute Revision
- **Generational ZGC (JEP 439):** Upgraded ZGC from single-generation to **Young + Old generations** (`-XX:+UseZGC -XX:+ZGenerational` in Java 21 -> became the **only** ZGC mode in **Java 24 JEP 490**!). Collects young garbage ultra-cheaply without stalling allocation, maintaining **sub-millisecond pauses** even at multi-TB heaps!
- **Key Encapsulation Mechanism API (`javax.crypto.KEM` — JEP 452):** Standardized crypto primitive needed by NIST **Post-Quantum Cryptography (ML-KEM / Kyber)** and modern TLS 1.3 hybrid key agreement!
- **UTF-8 by Default (JEP 400 — Java 18+):** `new FileReader(...)`, `new PrintWriter(...)`, and `Charset.defaultCharset()` now **ALWAYS use `UTF-8`** across Windows, macOS, and Linux — ending decades of *"works on my Mac/Linux, mangles emojis/accented names on Windows `Cp1252`"* bugs! (Override via `-Dfile.encoding=COMPAT` only for legacy migrations.)
- **Simple Web Server (`jwebserver` / `SimpleFileServer` — JEP 408, Java 18+):** Built-in zero-config static HTTP server for local frontend/docs testing!

## 📂 Contents
- [`notes/zgc-kem-utf8-and-jwebserver.md`](notes/zgc-kem-utf8-and-jwebserver.md)
- [`codes/lts/java21/runtime/KemUtf8AndCharsetDemo.java`](codes/lts/java21/runtime/KemUtf8AndCharsetDemo.java)
- [`interview/INTERVIEW_QUESTIONS.md`](interview/INTERVIEW_QUESTIONS.md)
