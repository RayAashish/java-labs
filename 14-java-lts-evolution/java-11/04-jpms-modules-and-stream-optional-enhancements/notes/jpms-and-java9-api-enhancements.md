# JPMS Modules, Private Interface Methods & Stream/Optional Upgrades

---

## 🟢 Layer 1 — Noob Track: What Modules Fix vs Classpath

Before Java 9, the classpath was a giant flat bucket of JARs:
- Even `public` internal classes (`sun.misc.Unsafe`, internal parser helpers) were visible to any library!
- If two JARs required different versions of a dependency, or missed a transitive JAR at startup, the app crashed hours later at runtime with `NoClassDefFoundError`.
- The JVM had to ship the entire 200+ MB JDK (`rt.jar`) even for a tiny CLI tool.

**JPMS (Java Platform Module System)** puts a contract (`module-info.java`) on each module declaring *what packages it reveals* and *what modules it needs* — verified immediately at compile time and startup! *(See also Module 15 `JpmsAndServiceLoaderDemo.java` for live `ModuleLayer` introspection!)*

---

## 🟡 Layer 2 — Practitioner Track: `module-info.java` Directives & Java 9 APIs

### 1. Core `module-info.java` Cheat Table
```java
module com.acme.orders {
    requires java.sql;                  // Direct compile + runtime dependency
    requires transitive com.acme.model; // Re-exports com.acme.model types appearing in our public APIs
    exports com.acme.orders.api;        // Public types visible externally (internal packages remain sealed!)
    opens com.acme.orders.dto to com.fasterxml.jackson.databind; // Deep reflection ONLY (no compile coupling!)
    uses com.acme.spi.PaymentGateway;   // ServiceLoader consumer
    provides com.acme.spi.TaxRule with com.acme.orders.EuTaxRule; // ServiceLoader provider
}
```

### 2. Interface `private` Methods (Java 9)
Before Java 9, if two `default` methods shared helper logic, you had to make that helper `default` (polluting your public API contract!). Now:
```java
interface Logger {
    default void info(String msg) { log("INFO", msg); }
    default void error(String msg) { log("ERROR", msg); }
    private void log(String level, String msg) { System.out.println(level + ": " + msg); }
}
```

### 3. Java 9 Stream & Collectors Additions
- `takeWhile(p)`: stops as soon as predicate fails (short-circuit loop break).
- `dropWhile(p)`: skips leading prefix while true, emits everything afterward.
- `Stream.iterate(0, n -> n < 10, n -> n + 2)`: clean functional equivalent of `for (int n = 0; n < 10; n += 2)`.
- `Collectors.filtering(p, downstream)` & `Collectors.flatMapping(fn, downstream)`: filter/flatten **inside** `groupingBy` without erasing empty group keys!

---

## 🔴 Layer 3 — Senior / JVM Track: Strong Encapsulation, Unnamed Module & `jlink`

- **Migration Bridge:** Existing JARs on `--class-path` go into the **Unnamed Module** (reads everything for backward compatibility). A plain JAR moved onto `--module-path` without `module-info.java` becomes an **Automatic Module** (reads everything and exports everything).
- **Frameworks (Spring / Hibernate / Jackson):** Need **deep runtime reflection** onto private entity fields -> use **`opens pkg to ...`** rather than `exports` so external code cannot compile against internal classes while frameworks retain reflection access!
- **`jlink` Custom Runtimes:** Strip unused JDK modules (`java.desktop`, `java.sql`, `java.xml`) down to just `java.base + java.net.http`, cutting containerized runtime images dramatically and reducing attack surface.
