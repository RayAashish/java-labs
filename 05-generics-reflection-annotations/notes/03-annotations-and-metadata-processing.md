# 03. Annotations & Runtime Metadata Processing

---

## 🟢 Layer 1 — Noob Track: Sticky Notes for the Compiler & Frameworks

An **annotation** (`@Override`, `@Deprecated`, `@SuppressWarnings`) attaches structured metadata to classes, methods, fields, or parameters without altering business logic directly.

```java
@Override // Tells javac: "Verify I actually matched a parent method signature!"
public String toString() {
    return "User";
}
```

---

## 🟡 Layer 2 — Practitioner Track: Meta-Annotations & Custom Annotations

To define your own annotation, use `@interface` plus Java's **meta-annotations**:

| Meta-Annotation | Purpose | Common Values |
|---|---|---|
| `@Retention` | How long the annotation survives | `SOURCE` (stripped by `javac`), `CLASS` (in `.class` bytecode, default), `RUNTIME` (readable via Reflection) |
| `@Target` | Where the annotation can be placed | `TYPE`, `METHOD`, `FIELD`, `PARAMETER`, `CONSTRUCTOR`, `RECORD_COMPONENT` |
| `@Documented` | Includes annotation in generated Javadoc | Marker annotation |
| `@Inherited` | Subclasses inherit class-level annotation (interfaces ignore it) | Marker annotation |
| `@Repeatable` | Allows applying the same annotation multiple times | Container annotation class |

```java
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Timed {
    String label() default "operation";
}
```

---

## 🔴 Layer 3 — Senior Track: Runtime Reflection vs Compile-Time Annotation Processing (APT)

1. **Runtime Scanning (Spring / Jackson / Hibernate):** Reads `@Retention(RUNTIME)` annotations via `Method.isAnnotationPresent(...)`. Flexible, but pays startup classpath-scanning & reflection overhead.
2. **Compile-Time Annotation Processing (`AbstractProcessor` / Lombok / MapStruct / Micronaut):** Reads `@Retention(SOURCE/CLASS)` annotations during `javac` compilation, generating real `.java` source files or bytecode at build time -> **zero runtime reflection cost** and instant startup (critical for GraalVM Native Image!).
