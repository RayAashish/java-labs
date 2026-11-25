# Interview Questions — Strong Encapsulation, Helpful NPE & Deserialization Security

## 🟢 Beginner

### Q1. What is a "Helpful `NullPointerException`" (JEP 358)?
Starting in Java 14/17, when `a.b().c()` throws `NullPointerException`, the JVM reverse-analyzes the failing bytecode instruction and explicitly prints *which expression was `null`* (for instance: `Cannot invoke "B.c()" because the return value of "A.b()" is null`).

---

## 🟡 Intermediate

### Q2. Why did legacy Java 8 apps fail with `InaccessibleObjectException` on Java 17, and what is the fix?
Java 17 (JEP 403) strongly encapsulated all internal JDK packages (`sun.*`, `java.lang.String.value`, etc.), removing the Java 9–16 escape hatch `--illegal-access=permit`. Fix it by:
1. Upgrading libraries (Jackson, Lombok, Mockito, Hibernate) to Java 17-compatible versions first!
2. Only as a targeted escape hatch for unmaintained libraries, add **`--add-opens module/pkg=ALL-UNNAMED`** on startup.

### Q3. Distinguish `--add-exports` vs `--add-opens`.
- `--add-exports`: allows access to **`public`** types at compile & run time.
- `--add-opens`: additionally grants **`setAccessible(true)` deep reflection** into `private`/package-private internals at runtime.

---

## 🔴 Senior

### Q4. Why must an `ObjectInputFilter` end with `;!*`?
Without `;!*`, unspecified classes remain `UNDECIDED` (meaning permitted by default — a **blocklist** bypass!). Ending with **`!*` (reject-all fallback)** turns the filter into a **strict allowlist**, ensuring unknown deserialization gadget classes are rejected with `InvalidClassException` before instantiation.
