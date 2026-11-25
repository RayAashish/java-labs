# 00. Java LTS Versions Explained for Beginners

---

## 🟢 What Does "LTS" Mean?

Java releases a new version every **6 months** (March and September), but companies run their production servers on **Long-Term Support (LTS)** versions that receive security & stability patches for many years:

```text
Java 8 (2014) ──► Java 11 (2018) ──► Java 17 (2021) ──► Java 21 (2023) ──► Java 25 (2025)
  Lambdas &         HttpClient &       Records, Text      Virtual Threads    FFM API, ScopedValue,
  Streams           var in lambdas     Blocks, Sealed     & Switch Patterns  Gatherers, Compact Headers
```

### Before vs After Example (Why Modern Java Rocks!)
**Old Java 8 DTO (35+ lines):** private fields, constructor, getters, `equals`, `hashCode`, `toString`.  
**Modern Java 17+ Record (1 line):**
```java
public record UserDto(long id, String name) {}
```
One line automatically gives you an immutable class, constructor, `id()`, `name()`, `equals()`, `hashCode()`, and `toString()`!
