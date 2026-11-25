# Records (`record` — JEP 395) Deep Dive

---

## 🟢 Layer 1 — Noob Track: Goodbye 50-Line POJO Boilerplate

In Java 8, creating a read-only `UserSummary` DTO required ~45 lines of repetitive constructor, getters, `equals`, `hashCode`, and `toString` — and forgetting to update `equals` when adding a 4th field caused subtle cache/Map bugs!

With Java 17 Records:
```java
public record UserSummary(String id, String email, boolean active) {}
```
One line gives you:
- `private final` components
- Canonical constructor `UserSummary(String, String, boolean)`
- Component accessors `id()`, `email()`, `active()`
- Value-based `equals`, `hashCode`, `toString`

---

## 🟡 Layer 2 — Practitioner Track: Compact Constructors, Wither & Traps

### 1. Compact Constructor (Validation + Normalization + Defensive Copy)
Notice there are **no parameter parentheses** — parameters are implicitly in scope and assigned to fields automatically *at the end* of the block:
```java
public record TenantKey(String tenantId, List<String> roles) {
    public TenantKey { // Compact constructor!
        Objects.requireNonNull(tenantId);
        tenantId = tenantId.strip().toLowerCase(); // Mutate parameter BEFORE implicit field assignment!
        roles = (roles == null) ? List.of() : List.copyOf(roles); // Deep immutability!
    }
}
```

### 2. Why `record` Is Shallowly Immutable by Default
If you pass a `new ArrayList<>(List.of("ADMIN"))` into `new TenantKey("acme", list)` without `List.copyOf(...)`, modifying the caller's `list` later **mutates the record's internal list and corrupts its `hashCode()`** while sitting inside a `HashMap` or `HashSet`!

### 3. When NOT to Use Records (`@Entity` Trap)
- **BAD:** `@Entity public record CustomerEntity(...) {}` — JPA providers rely on subclass runtime proxies (Hibernate ByteBuddy), zero-arg constructors, and setters for dirty checking. Records are `final` and fieldless-no-arg-less!
- **GOOD:** `@Query("SELECT new com.app.CustomerProjection(c.id, c.email) FROM Customer c")` returning a `record` read-only DTO projection!

---

## 🔴 Layer 3 — Senior / JVM Track: Bytecode & Serialization Safety

### 1. `ObjectMethods.bootstrap` (`invokedynamic`)
Run `javap -v UserSummary.class`: `javac` does **NOT** emit verbose hand-rolled `if (this == o) return true; ...` bytecode! Instead, `equals`, `hashCode`, and `toString` emit a single **`invokedynamic`** instruction bound to `java.lang.runtime.ObjectMethods.bootstrap`, generating optimized method handles lazily at runtime (and even simpler to optimize via JIT).

### 2. Why Record Serialization Cannot Bypass Validation!
Normal Java `Serializable` classes use hidden native allocation (`sun.reflect.ReflectionFactory`) that **skips constructors completely**, allowing malicious serialized byte payloads to inject invalid states (`amount = -9999`) or exploit gadget chains (`readObject()`).
When deserializing a `record`, `ObjectInputStream` ALWAYS invokes the **canonical/compact constructor** dynamically via `MethodHandles` — validating every invariant automatically! Plus `Class.isRecord()` + `Class.getRecordComponents()` expose clean runtime reflection metadata for Jackson/Spring/Bean Validation without reflection hacks on `private` fields!
