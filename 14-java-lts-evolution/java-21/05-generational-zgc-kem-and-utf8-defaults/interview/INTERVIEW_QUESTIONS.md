# Interview Questions — Gen-ZGC, KEM API, UTF-8 Default & Simple Web Server

## 🟢 Beginner

### Q1. What bug did JEP 400 ("UTF-8 by Default") permanently eliminate?
Previously, `FileReader`, `FileWriter`, and `String.getBytes()` without an explicit `Charset` fell back to the OS locale (`Windows-1252` on Windows vs `UTF-8` on macOS/Linux), silently corrupting non-ASCII text (`₹`, accents, emojis) between developer laptops and production servers. Since Java 18/21, `Charset.defaultCharset()` is **UTF-8 across all platforms**.

---

## 🟡 Intermediate

### Q2. Why is **Generational ZGC** (JEP 439) significantly better than the earlier non-generational ZGC?
Most allocated Java objects die almost immediately (Weak Generational Hypothesis). Non-generational ZGC scanned the entire heap every cycle, wasting CPU and risking allocation stalls if traffic spiked. Generational ZGC collects young objects frequently with minimal cost while keeping GC pauses **sub-millisecond (<1 ms)**.

### Q3. Why did Java add `javax.crypto.KEM` (JEP 452) when `KeyAgreement` (Diffie-Hellman) already existed?
Post-quantum cryptographic standards selected by NIST (such as **ML-KEM / Kyber**) do **not** fit the Diffie-Hellman shared-secret math model — they work natively as **Key Encapsulation Mechanisms (`encapsulate` / `decapsulate`)** producing `(capsule, sharedSecretKey)`. JEP 452 standardizes that API natively across JDK crypto providers.
