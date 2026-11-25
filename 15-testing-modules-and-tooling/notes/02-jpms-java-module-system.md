# 02. Java Platform Module System (JPMS — Java 9+)

---

## 🟢 Layer 1 — Noob Track: Packages vs Modules

Before Java 9, any `public class` in any JAR on the classpath was accessible to everyone — even JDK internal classes like `sun.misc.Unsafe`!
**JPMS (`module-info.java`)** groups related packages into a **Module** with explicit boundaries:
- Which packages other modules are allowed to use (**`exports`**)
- Which external modules this module needs (**`requires`**)

---

## 🟡 Layer 2 — Practitioner Track: `module-info.java` Syntax

```java
module com.myapp.billing {
    requires java.sql;                  // Direct dependency
    requires transitive java.logging;   // Downstream users automatically read java.logging too

    exports com.myapp.billing.api;      // Public types visible at compile & runtime
    opens com.myapp.billing.model to com.fasterxml.jackson.databind; // Deep reflection access!
}
```

### `exports` vs `opens`
- **`exports`:** Allows normal compile-time & runtime access to `public` types only (no private-field reflection!).
- **`opens`:** Allows **deep runtime reflection** (reading `private` fields, calling private constructors) — mandatory when Hibernate/Jackson/Spring inspect entity classes in modular apps!

---

## 🔴 Layer 3 — Senior Track: Strong Encapsulation & `jlink`

1. **Strong Encapsulation (`JEP 403`, Java 17+):** JDK internal packages (`java.base/java.lang`, `java.util`) are closed by default to unnamed modules. Legacy libraries reflecting into `HashMap.table` or `ByteBuffer` fail unless you pass `--add-opens java.base/java.util=ALL-UNNAMED`.
2. **Classpath vs Modulepath:** Passing JARs with `-cp` puts them in the **Unnamed Module** (backwards-compatible mode). Passing `--module-path` enforces strict module graph checks at startup (fail-fast missing dependency detection instead of `NoClassDefFoundError` hours later!).
3. **`jlink` Custom Runtime Images:** Strips unused JDK modules (like `java.desktop`, `java.xml`) and packages a tiny ~35–50 MB Docker runtime containing ONLY `java.base` + `java.sql` + your app!
