# Sealed Classes & Interfaces (JEP 409) Deep Dive

---

## 🟢 Layer 1 — Noob Track: Controlled Inheritance

Imagine designing a `PaymentMethod` hierarchy in a banking app:
- `CreditCard`, `BankTransfer`, `CryptoWallet`.
- Previously, if `PaymentMethod` was a normal `public interface`, ANY developer on another team could invent a 4th class `GiftCard` without updating your routing `if/else` or `switch` — failing silently at runtime!
- Previously, your only hack was making constructors package-private (impossible for interfaces!).

**Sealed classes/interfaces** give libraries & domain models explicit middle ground: *"Only `CreditCard`, `BankTransfer`, and `CryptoWallet` are allowed to implement `PaymentMethod` — period."*

---

## 🟡 Layer 2 — Practitioner Track: Subtype Modifiers & Algebraic Data Types

### The 3 Mandatory Modifiers on Every Permitted Subclass
Every direct subtype named in `permits` MUST declare one of:
1. **`final`** (or `record`, which is implicitly `final`): Closed leaf node.
2. **`sealed`:** Continues controlled narrowing (`permits SubA, SubB`).
3. **`non-sealed`:** Explicitly opens that branch back up (`class CustomWallet extends BankTransfer` style).

### Algebraic Data Types (ADTs) in Java 17+
Functional languages (Scala/Rust/Haskell) model domains with **Sum Types** ("one of X OR Y OR Z") and **Product Types** ("A AND B AND C"):
- **`sealed interface PaymentMethod permits CreditCard, BankTransfer`** -> **Sum Type**
- **`record CreditCard(String pan, String exp) implements PaymentMethod`** -> **Product Type**

Together, `javac` and your IDE know the **exact finite domain** of values at compile time!

---

## 🔴 Layer 3 — Senior / JVM Track: Bytecode & Security Boundaries

1. **`PermittedSubclasses` Attribute in `.class` Format:** When inspecting `PaymentMethod.class` with `javap -v`, `javac` writes a **`PermittedSubclasses`** attribute listing every authorized binary class name. At runtime, the JVM ClassLoader enforces this contract during class resolution — throwing **`IncompatibleClassChangeError`** even if someone tries bytecode manipulation or compiles a rogue subclass separately!
2. **Reflection Introspection:**
   - `PaymentMethod.class.isSealed()` -> `true`
   - `PaymentMethod.class.getPermittedSubclasses()` -> array of `ClassDesc`/`Class<?>` of permitted subtypes.
3. **Why JPMS Module Matters:** Inside a modular JAR (`module-info.java`), permitted subclasses can live in *different packages within the same module* (`com.app.model` & `com.app.internal`). On classpath (`unnamed module`), all subclasses must reside in the **exact same package**!
