# Java 17 — Strong Encapsulation, Helpful NPE, PRNG & Deserialization Filters

## ⚡ 2-Minute Revision
- **JEP 403 Strong Encapsulation of JDK Internals:** `sun.misc.*`, `sun.reflect.*`, and `java.base` internals can **no longer** be bypassed silently with `setAccessible(true)` (`--illegal-access=permit` removed!).
  - **`--add-exports module/pkg=target`:** Exposes `public` types at compile/run time.
  - **`--add-opens module/pkg=target`:** Permits deep runtime reflection on `private` members (required by legacy Lombok/Kryo/Spring/Mockito versions on Java 17!).
- **JEP 358 Helpful `NullPointerException` (Java 14+):** Tells you the exact sub-expression that was null: `Cannot invoke "Address.city()" because the return value of "User.address()" is null`!
- **JEP 356 Enhanced Pseudo-Random Number Generators:** Standardized `RandomGenerator` interface + `RandomGeneratorFactory` supporting modern splittable algorithms like `L64X128MixRandom` (`java.util.Random` only implemented `RandomGenerator` legacy LCG).
- **JEP 415 Context-Specific Deserialization Filters:** Global/per-stream **`ObjectInputFilter`** whitelists (`com.myapp.dto.*;!*`) defeat RCE serialization gadget chains cleanly!

## 📂 Contents
- [`notes/encapsulation-prng-and-security.md`](notes/encapsulation-prng-and-security.md)
- [`codes/lts/java17/security/HelpfulNpePrngAndFilterDemo.java`](codes/lts/java17/security/HelpfulNpePrngAndFilterDemo.java)
- [`interview/INTERVIEW_QUESTIONS.md`](interview/INTERVIEW_QUESTIONS.md)
