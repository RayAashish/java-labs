# Generational ZGC, KEM API, UTF-8 Default & `jwebserver` (Java 18–21)

---

## 🟢 Layer 1 — Noob Track: Plain-English Highlights

1. **No More Encoding Roulette (JEP 400):** On Java 17 and earlier, `new FileWriter("out.txt")` picked `Cp1252` on US Windows, `UTF-8` on Linux, and sometimes ASCII inside minimal Docker containers — silently turning `₹` or `café` into `???`! From Java 18/21 onward, `Charset.defaultCharset()` defaults to **`UTF-8` everywhere**.
2. **Instant Local File Server (`jwebserver`):** Need to serve a folder of HTML/JS files for local testing? Run `jwebserver -p 8000` straight from your terminal (no Python, Node, or Apache needed!).

---

## 🟡 Layer 2 — Practitioner Track: Generational ZGC & `javax.crypto.KEM`

### 1. Why Generational ZGC Matters vs Classic G1GC vs Single-Gen ZGC
- **G1GC (default):**Balances throughput & latency (typical pauses 10–100 ms under allocation bursts).
- **Classic ZGC (Java 15):** Scans ALL live heap objects every cycle -> wastes CPU when 90% of objects die young (Weak Generational Hypothesis), forcing massive heap headroom (`2x–3x` live set) to avoid allocation stalls.
- **Generational ZGC (Java 21 JEP 439, default ZGC mode in Java 24+):** Separates Young objects (collected frequently & concurrently with minimal barrier cost) from Old objects — achieving **<1 ms GC pauses**, lower CPU cost, and lower memory headroom simultaneously!

### 2. How `javax.crypto.KEM` (JEP 452) Works in 3 Steps
Instead of encrypting data with Alice's asymmetric public key directly (slow/size-limited):
1. **Bob (Sender)** calls `KEM.getInstance("DHKEM").newEncapsulator(alicePublicKey).encapsulate()` -> gets **`(SecretKey aesSessionKey, byte[] ciphertextCapsule)`**.
2. Bob sends `ciphertextCapsule` over the network and encrypts data with `aesSessionKey` (`AES-GCM`).
3. **Alice (Receiver)** calls `KEM.getInstance("DHKEM").newDecapsulator(alicePrivateKey).decapsulate(ciphertextCapsule)` -> recovers the **exact same `SecretKey aesSessionKey`**!
*(Ready seamlessly for NIST Post-Quantum ML-KEM algorithms finalized in JDK 24+!)*

---

## 🔴 Layer 3 — Senior / JVM Track

- **Gen-ZGC Load Barriers:** Uses **Colored Pointers** (`Marked0`, `Marked1`, `Remapped`, `Finalizable` metadata bits + store/load barriers) on 64-bit pointers (not compressed OOPs) so object relocation runs concurrently with mutator Java application threads without Stop-The-World heap compaction pauses!
- **JEP 400 nuance (`System.out` vs `FileReader`):** `Charset.defaultCharset()` is strictly UTF-8, while `Console.charset()` (and interactive terminal encoding) inspects native OS stdout/console codepages (`native.encoding`) so terminal characters display cleanly too.
