# Flexible Constructors, Compact Object Headers & Java 25 Runtime Upgrades

---

## 🟢 Layer 1 — Noob Track: Two Pain Points Removed in Java 25!

### 1. Cleaner Onboarding (`void main()` + `IO.println` — JEP 512)
In a beginner/script file `Hello.java`, Java 25 lets you write:
```java
void main() {
    String name = IO.readln("Your name: ");
    IO.println("Welcome to Java 25, " + name + "!");
}
```
Notice `IO` is in **`java.lang`**, auto-imported without `System.out.println` or `Scanner`! Meanwhile regular `public class` code continues working identically.

### 2. Fail-Fast Constructor Validation Before `super(...)` (JEP 513)
Previously, `super(...)` HAD to be line #1 of a constructor! Calling `validatePositive(age)` BEFORE wasting resources initializing the parent class was illegal unless crammed awkwardly into `super(validateAndReturn(age))`!
Java 25 splits constructor bodies into:
- **Prologue** *(before `super(...)`)*: validate inputs, compute derived parameters, initialize **your own class's fields**.
- **`super(...)` / `this(...)` call**
- **Epilogue** *(after `super(...)`)*: normal post-parent setup where `this` is fully reachable!

---

## 🟡 Layer 2 — Practitioner Track: JEP 513 Rules & Fixing Overridable Superclass Bug

### What IS Allowed vs Forbidden in Prologue (Before `super(...)`)?
- ✅ Validate arguments (`if (limit <= 0) throw new IllegalArgumentException(...)`) — **fail fast before running expensive parent constructor**!
- ✅ Share computation assigned to locals and pass into `super(x, x)` without static helper hacks!
- ✅ **Write to fields declared in the SAME subclass (`this.region = region;`)!**
  - **HUGE FIX:** Previously, if `Parent()` called `overridableMethod()`, the subclass saw `this.region == null` because subclass field initializers ran AFTER `super()`! Setting `this.region = region;` in the prologue fixes that subtle bug completely!
- ❌ **Forbidden before `super(...)`:** Reading fields inherited from `super`, calling instance methods on `this`, or capturing/returning `this` (because the superclass half of the object isn't constructed yet!).

---

## 🔴 Layer 3 — Senior / JVM Architect Track: Lilliput (JEP 519), JEP 491 & Leyden

### 1. Project Lilliput — Compact Object Headers (`JEP 519`)
In HotSpot 64-bit JVMs (Java 8–21):
- Every Java object carried a **12-byte header** (8-byte Mark Word + 4-byte Compressed Klass Pointer) — padded to 16 bytes for `Long`/`Optional`! An `Integer` / tiny `Node` (`next`, `val`) spent ~40–50% of its footprint on metadata!
- **JEP 519 (`-XX:+UseCompactObjectHeaders`, product feature in Java 25):** Packs Mark Word + compressed class pointer + hash + GC age bits + lock state into **one 64-bit word (8 bytes)** -> massive heap reduction (10–22% RAM savings) + denser CPU cache lines + fewer GC cycles!

### 2. JEP 491 Unpinning `synchronized`
Removes the #1 Virtual Thread adoption blocker from Java 21! HotSpot now inflates/transfers monitor ownership directly to virtual threads rather than OS carrier threads, unmounting cleanly when entering contended monitors or calling `Object.wait()`!

### 3. Project Leyden AOT Class Loading & Method Profiling (`JEP 483` & `JEP 515`)
Pre-links loaded/linked classes & method execution profiles at build time into an AOT cache (`-XX:AOTCache=...`), slicing Spring Boot / microservice cold-start & C2 JIT warmup times dramatically without GraalVM native-image closed-world reflection limitations!
