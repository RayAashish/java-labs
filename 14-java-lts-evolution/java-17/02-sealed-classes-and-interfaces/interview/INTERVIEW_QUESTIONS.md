# Interview Questions — Java 17 Sealed Classes & Interfaces

## 🟢 Beginner

### Q1. What are the 3 modifiers every direct subclass of a `sealed` class/interface must choose from?
1. **`final`** — prevents further inheritance (or `record`, which is implicitly `final`).
2. **`sealed`** — keeps subclassing restricted to a new `permits` list.
3. **`non-sealed`** — opens up that specific branch to normal unrestricted inheritance.

### Q2. Can `permits` ever be omitted on a `sealed` declaration?
Yes! When all permitted subclasses reside directly inside the **same `.java` compilation unit** (for example nested static classes/records inside a sealed interface), `javac` infers `permits` automatically.

---

## 🟡 Intermediate

### Q3. How do Sealed Classes + Records work together in Data-Oriented Programming?
They implement **Algebraic Data Types (ADTs)**:
- Sealed hierarchies act as **Sum Types** (one of N fixed alternatives).
- Records act as **Product Types** (immutable bundles of data fields).
Paired with Java 21 Pattern Matching in `switch`, the compiler verifies every case at compile time without needing a fragile `default` catch-all!

### Q4. Can a `non-sealed` subclass accidentally allow unauthorized direct implementations of the parent `sealed` interface?
No — direct implementations of the `sealed` interface are still checked strictly against its `permits` list. Only subclasses extending the `non-sealed` branch itself are unrestricted.

---

## 🔴 Senior

### Q5. How does the JVM prevent someone from compiling a malicious jar that tries to implement a `sealed interface`?
`javac` embeds a **`PermittedSubclasses`** attribute in the sealed `.class` file's constant pool metadata. At load time, the HotSpot ClassLoader rejects any loaded class claiming to extend/implement a sealed supertype unless it is listed in `PermittedSubclasses` AND resides in the same JPMS module (or same package in an unnamed module), throwing **`IncompatibleClassChangeError`**.
