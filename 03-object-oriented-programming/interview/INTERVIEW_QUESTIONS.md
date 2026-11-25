# 03. Object-Oriented Programming (OOP) — Interview Questions & Answers

> Pairs directly with [`../oops-revisit.md`](../oops-revisit.md) (which also contains **25 rapid output puzzles** in Section 13!).

---

## 🟢 Beginner / Core OOP Questions

### Q1. What are the 4 Pillars of OOP?
1. **Encapsulation:** Bundling data + methods and hiding internal fields (`private`) behind validated accessors.
2. **Inheritance:** Reusing and extending behavior (`IS-A`) via `extends`.
3. **Polymorphism:** One interface / parent type behaving differently across implementations (compile-time overloading + runtime overriding).
4. **Abstraction:** Exposing *what* an object does while hiding *how* via `abstract class` or `interface`.

### Q2. Is Java 100% Object-Oriented?
No. Java has **8 primitive types** (`int`, `boolean`, `double`, etc.) that are not objects (`Object` subclasses) for performance reasons, plus `static` members that belong to classes rather than object instances.

### Q3. Is Java pass-by-value or pass-by-reference?
**Strictly pass-by-value.** For primitives, Java copies the value bits. For objects, Java **copies the reference address bits** into the parameter slot. Reassigning the parameter (`obj = new Foo()`) never changes the caller's variable; mutating fields through that copied pointer (`obj.setName("X")`) modifies the shared Heap object.

### Q4. What happens to the default constructor when you define a parameterized constructor?
The compiler stops generating the implicit no-arg constructor immediately. If you still want `new User()`, you must explicitly write `public User() {}`.

### Q5. Can constructors be `private`, `static`, `final`, or `abstract`?
- **`private`:** **YES!** Used in Singletons, Builder targets, and static utility classes (`Math`, `Collections`).
- **`static` / `final` / `abstract`:** **NO!** Constructors cannot be inherited/overridden, so `final`/`abstract` are meaningless, and `static` violates instance construction.

### Q6. Explain `this()` vs `super()`.
`this(...)` delegates to another overloaded constructor in the **same class**; `super(...)` invokes the **parent class** constructor. Either (not both!) must be the **very first statement** of a constructor.

---

## 🟡 Intermediate Questions

### Q7. Abstract Class vs Interface (Java 8/9/17+) — When to choose which?
- Choose **Abstract Class (`IS-A`)** when subclasses share **mutable instance state/fields**, **constructors**, or non-public protected template helpers.
- Choose **Interface (`CAN-DO`)** to define a behavioral capability across unrelated hierarchies, enable multiple inheritance of types, or expose functional/lambda contracts (`@FunctionalInterface`). Since Java 8/9, interfaces also support `default`, `static`, and `private` helper methods.

### Q8. Why doesn't Java support multiple inheritance of classes, and how did Java 8 handle default method conflicts?
Multiple class inheritance creates the **Diamond Problem** (ambiguous parent state and conflicting implementations). Java allows multiple interfaces instead; if two interfaces define the same `default void ping()` signature, the implementing class **must override `ping()`** and can explicitly disambiguate with `InterfaceA.super.ping()`.

### Q9. What are the 4 Rules of Method Overriding?
1. Identical method name and parameter types.
2. Same or **covariant** return type (subclass of parent return type).
3. Access modifier can **widen** (`protected -> public`), never narrow.
4. Checked exceptions can be **fewer or narrower**, never broader (`RuntimeException` has no restriction).

### Q10. Why can't `static` methods or fields be overridden?
Overriding uses runtime dynamic dispatch (`invokevirtual` via `vtable`) based on the Heap object type. `static` methods and fields bind at **compile time** using the **reference type** — declaring the same static method in a subclass is **Method Hiding**, not overriding!

### Q11. What are the 5 Rules to Create a Truly Immutable Class?
1. Make the class `final` (or all constructors `private` + static factory).
2. Make all fields `private final`.
3. Provide **no setters**.
4. Make **defensive copies** of mutable inputs in constructor (`this.tags = List.copyOf(tags);`).
5. Return **defensive copies** / unmodifiable views of mutable fields in getters.

### Q12. Composition vs Aggregation vs Inheritance — Why "Favor Composition"?
- **Composition (Strong HAS-A):** Child cannot outlive parent (`Spaceship` owns `Thruster`).
- **Aggregation (Weak HAS-A):** Independent lifecycles (`SmartHomeHub` holds `SmartBulb`s).
- **Why Composition beats Inheritance:** Avoids the **Fragile Base Class** problem, preserves encapsulation, keeps the single `extends` slot free, and permits swapping strategies at runtime.

### Q13. What is the `equals()` and `hashCode()` contract?
If `a.equals(b)` is `true`, `a.hashCode() == b.hashCode()` **MUST be `true`** (converse is not required — hash collisions are legal). Always override **both** using the exact same fields; otherwise `HashMap`/`HashSet` lookups miss keys.

### Q14. Why is `Cloneable` considered broken, and what replaces it?
`clone()` is a marker-interface hack that skips constructors, defaults to shallow copy, throws checked `CloneNotSupportedException`, and requires casting. Replace it with **Copy Constructors** (`new Order(other)`) or **Static Factory Methods** (`Order.copyOf(other)`).

---

## 🔴 Senior / JVM Internals Questions

### Q15. Explain the JVM Initialization Order when `new Child()` runs for the first time.
1. `Parent` static variables & static blocks (top-to-bottom)
2. `Child` static variables & static blocks (top-to-bottom)
3. `Parent` instance variables & instance init blocks -> `Parent` constructor
4. `Child` instance variables & instance init blocks -> `Child` constructor
*(Calling an overridable method inside `Parent()` dispatches to `Child.method()` BEFORE `Child`'s fields are initialized — printing `0`/`null`!)*

### Q16. Explain the 4-Tier Compile-Time Overload Resolution Ladder.
`javac` resolves overloaded calls in strict order:
1. Exact Match
2. Primitive Widening (`int -> long`)
3. Autoboxing (`int -> Integer`)
4. Varargs (`int...`)
*(Note: Primitive widening ALWAYS beats autoboxing!)*

### Q17. Why do local & anonymous classes require captured local variables to be `effectively final`?
Local variables live on the Thread Stack and disappear when the method returns, whereas the anonymous/local class instance lives on the Heap and may outlive the method. The JVM copies the variable's value into a synthetic field on the inner class; forbidding mutation prevents the two copies from diverging.

### Q18. How do Member Inner Classes cause subtle memory leaks?
Every non-static inner class (or non-static anonymous class) holds a hidden synthetic field `this$0` pointing to its enclosing outer instance. Passing a long-lived callback/listener from a short-lived UI/Controller keeps the entire outer object alive from GC! Use **`static` nested classes** by default unless you truly need `Outer.this`.

### Q19. Explain how `vtable` and `itable` dispatch work in HotSpot.
Each loaded `Klass` in Metaspace owns a **Virtual Method Table (`vtable`)** mapping virtual method slots to concrete implementations. `invokevirtual` performs an $O(1)$ slot lookup on the object's `Klass` pointer (often devirtualized/inlined by C2 JIT if Class Hierarchy Analysis sees a single implementation!). Interface dispatch (`invokeinterface`) scans an **`itable`** since classes implement multiple interfaces in varying orders.

### Q20. What makes Java `enum` ideal for Singletons and Strategy patterns?
An `enum` compiles to a `final` class extending `java.lang.Enum<E>` with `private` constructors and `public static final` singleton instances created safely during `<clinit>`. The JVM blocks Reflection instantiation (`Constructor.newInstance` throws `IllegalArgumentException`) and guarantees identity-preserving deserialization!

---

## 🧩 Top Output Puzzles (Quick Check)
*(See [`../oops-revisit.md`](../oops-revisit.md#13-the-25-essential-interview-traps--output-puzzles) for all 25 interactive puzzles with bytecode explanations!)*
1. `Parent p = new Child();` calling `static void ping()` -> Calls **`Parent.ping()`** (Method Hiding).
2. `void m(long x)` vs `void m(Integer x)` called with `m(5)` -> Calls **`long`** (Widening beats Boxing).
3. `void m(Object o)` vs `void m(String s)` called with `m(null)` -> Calls **`String`** (Most specific type).
4. `void m(String s)` vs `void m(Integer i)` called with `m(null)` -> **Compile Error: ambiguous**.
5. Calling overridden `print()` inside `Parent()` when `Child` sets `int val = 42` -> Prints **`0`** (before Child field init runs!).
