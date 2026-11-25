# Interview Questions — Flexible Constructors, Compact Headers & Java 25 Runtime

## 🟢 Beginner

### Q1. How did Java 25 (JEP 513) relax the `super(...)` / `this(...)` constructor rule?
Before Java 25, `super(...)` or `this(...)` ALWAYS had to be the very first statement of a constructor. JEP 513 lets you validate arguments, compute temporary values, and initialize **the subclass's own fields** in a prologue *before* calling `super(...)`.

---

## 🟡 Intermediate

### Q2. Can you read `super.field` or call `this.helperMethod()` before `super(...)` in Java 25?
**No!** Only local computation, throwing validation exceptions, and *writing* to uninitialized fields of the **current class** are permitted in the prologue. Reading `this` or superclass state before `super(...)` runs would observe an uninitialized superclass!

### Q3. What real-world bug did assigning `this.field = value;` before `super(...)` finally solve?
If a superclass constructor calls an overridable method during construction, Java historically entered the subclass override when subclass fields were still `null`/`0`! Assigning `this.field = value;` before `super(...)` guarantees subclass fields are populated *before* parent constructor logic executes.

---

## 🔴 Senior

### Q4. Explain **Project Lilliput / Compact Object Headers (`JEP 519`)** and its impact on cloud JVM workloads.
Every HotSpot Java object normally carries a 12-to-16 byte header (64-bit Mark Word + 32-bit compressed Klass Word). With **`-XX:+UseCompactObjectHeaders`** in Java 25, HotSpot merges them into **8 bytes**, saving ~10–20% heap in object-dense apps (Spring, caches, ASTs, trees), increasing CPU cache density, and reducing GC frequency!

### Q5. What changed for `synchronized` + Virtual Threads between Java 21 LTS and Java 25 LTS?
Java 21 pinned Virtual Threads to OS carriers whenever blocking inside `synchronized`, forcing migrations to `ReentrantLock`. **JEP 491** (included in Java 24 & Java 25 LTS) unpins virtual threads safely on intrinsic `synchronized` monitor acquisition/wait — removing the biggest migration friction from older libraries!
