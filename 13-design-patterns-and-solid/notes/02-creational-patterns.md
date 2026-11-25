# 02. Creational Patterns (Singleton, Factory, Builder, Prototype)

---

## 🟢 Layer 1 — Noob Track: Controlling Object Creation

Normally you write `new User(...)`. Creational patterns step in when:
- You want **only ONE shared instance** in the whole app (**Singleton**).
- You have 10 optional constructor arguments (`new Pizza(true, false, true, null, "thin")`) -> unreadable! Use **Builder**.
- You want to decide *which* concrete class to instantiate based on config/input (**Factory**).

---

## 🟡 Layer 2 — Practitioner Track: Key Patterns

### 1. Builder Pattern
Replaces telescoping constructors (`User(a)`, `User(a,b)`, `User(a,b,c)`) and mutable JavaBeans setters (which allow half-constructed invalid objects!) with fluent, validated, **immutable** construction.

### 2. Factory Method vs Abstract Factory
- **Factory Method:** One method creates one product family (`NotificationFactory.create("SMS")`).
- **Abstract Factory:** Creates **families of related objects** that must match (`CloudStorageFactory` -> `AwsS3Client` + `AwsKmsEncryptor` vs `GcpStorageClient` + `GcpKmsEncryptor`).

---

## 🔴 Layer 3 — Senior Track: The 4 Ways to Break Singleton & Why `enum` Wins

In interviews, Double-Checked Locking (`DCL`) is tested for Java Memory Model mastery:
```java
public final class ConfigCache {
    // volatile PREVENTS instruction reordering of:
    // 1. allocate memory -> 2. assign reference -> 3. run constructor!
    private static volatile ConfigCache instance;

    private ConfigCache() {}

    public static ConfigCache getInstance() {
        if (instance == null) {
            synchronized (ConfigCache.class) {
                if (instance == null) {
                    instance = new ConfigCache();
                }
            }
        }
        return instance;
    }
}
```
**How classic Singletons still break:**
1. **Reflection:** `constructor.setAccessible(true); constructor.newInstance();` -> fix by guarding constructor if `instance != null`.
2. **Deserialization:** `ObjectInputStream.readObject()` creates a new instance -> fix by adding `private Object readResolve() { return instance; }`.
3. **Cloning:** `clone()` creates a copy -> don't implement `Cloneable` (or throw `CloneNotSupportedException`).

**Joshua Bloch's Gold Standard (Effective Java):**
```java
public enum Registry {
    INSTANCE; // JVM guarantees single instance across threads, serialization, and Reflection!
}
```
*(In Spring Boot apps, Spring manages singletons at the ApplicationContext container level rather than static globals so dependencies remain mockable.)*
