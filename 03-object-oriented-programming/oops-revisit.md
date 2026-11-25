# ⚡ Java OOP Revisit — The Senior Revision Engine

> **Purpose:** High-signal, zero-fluff revision hub for Java Object-Oriented Programming.  
> **Target Audience:** Engineers revisiting core OOP, JVM internals, design tradeoffs, and technical interview gotchas.  
> **Coverage:** Foundations, Memory Layout, Constructors, Encapsulation, Inheritance, Polymorphism, Abstraction, `static`/`final`, `Object` Contracts, Composition, Nested Classes, Enums & 25 Classic Interview Traps.

---

## 🧭 Rapid Revision Navigator

| # | Topic | Key Focus & Mental Models | Jump Link |
|---|---|---|:---:|
| **01** | **JVM Memory & Foundations** | Metaspace vs Heap vs Stack, 8-16B Object Headers, Pass-by-Value Proof | [Section 1](#1-jvm-memory-architecture--first-principles) |
| **02** | **Constructors & Lifecycle** | Constructor `<init>`, `this()` Chaining, Shallow vs Deep Copy Trap, Initialization Order | [Section 2](#2-constructors--object-initialization-lifecycle) |
| **03** | **Encapsulation & Boundaries** | 4 Access Modifiers Matrix, Invariant Defense, Immutable Class Recipe | [Section 3](#3-encapsulation--access-boundary-matrix) |
| **04** | **Inheritance Hierarchies** | `extends`, `super`, Why Single Inheritance (Diamond Problem), Subclass Memory | [Section 4](#4-inheritance--hierarchy-mechanics) |
| **05** | **Polymorphism & Dispatch** | 4-Tier Overload Ladder, Dynamic `vtable` Dispatch, Overriding Invariants | [Section 5](#5-polymorphism--method-resolution-engine) |
| **06** | **Abstraction & Interfaces** | Abstract Class vs Interface Decision Matrix, Java 8/9 Default/Private Methods | [Section 6](#6-abstraction--modern-interface-architecture) |
| **07** | **`static` & `final` Keywords** | Metaspace State, Method Hiding vs Overriding, Inlining, Reference Mutability | [Section 7](#7-static-and-final-mechanics) |
| **08** | **`java.lang.Object` Essentials** | `equals()` 5 Axioms, `hashCode()` Hash Collision Trap, Cloneable Flaws | [Section 8](#8-javalangobject-contracts--essentials) |
| **09** | **Object Relationships** | IS-A vs HAS-A, Favor Composition Over Inheritance, Aggregation vs Composition | [Section 9](#9-object-relationships-composition-vs-inheritance) |
| **10** | **Nested & Inner Classes** | Static Nested, Member Inner (`Outer.this`), Local, Anonymous, Effectively Final | [Section 10](#10-nested-inner-local--anonymous-classes) |
| **11** | **Enums as Rich Types** | `java.lang.Enum` Subclassing, Constant-Specific Bodies, Switch Expressions | [Section 11](#11-enums-as-first-class-types) |
| **12** | **Modern Domain Modeling** | Production domains: Gaming, Smart Home, Aerospace, Fintech | [Section 12](#12-modern-domain-architecture-spotlight) |
| **13** | **25 Interview Traps & Output** | High-frequency tricky output puzzles with 1-line JVM explanations | [Section 13](#13-the-25-essential-interview-traps--output-puzzles) |
| **14** | **5-Minute Pre-Interview Checklist**| High-density bullet recap before stepping into an interview | [Section 14](#14-the-5-minute-pre-interview-rapid-recap) |

---

## 1. JVM Memory Architecture & First Principles

### 1.1 The JVM Runtime Triangle
Every Java program operates across three core memory segments:

```
+---------------------------------------------------------------------------------+
|                                JVM RUNTIME MEMORY                               |
+-----------------------------------+---------------------------------------------+
| 1. METASPACE (Native Memory)      | 2. HEAP (Garbage-Collected)                |
| - Class metadata, method bytecode | - Object payload instances                  |
| - Method tables (vtable, itable)  | - Mark Word (8B) + Klass Pointer (4B/8B)    |
| - Static fields & String Pool     | - Instance field values                     |
+-----------------------------------+---------------------------------------------+
| 3. THREAD STACKS (Per-Thread)                                                   |
| - Stack frames pushed/popped per method call                                   |
| - Local primitive variables (int, double)                                       |
| - Object reference pointers (addresses pointing to Heap)                        |
+---------------------------------------------------------------------------------+
```

### 1.2 The Object Memory Layout (64-bit JVM)
An instantiated object on the Heap is **never** just its fields:
1. **Mark Word (8 bytes):** Hash code, GC age, lock flags (biased/lightweight/heavyweight monitor).
2. **Klass Pointer (4 bytes with Compressed Oops / 8 bytes without):** Pointer to class metadata in Metaspace.
3. **Instance Data:** The actual fields (`int` = 4B, `boolean` = 1B, reference = 4B/8B).
4. **Padding:** Padded to the nearest multiple of 8 bytes.
> **Key Takeaway:** An empty object `new Object()` takes **16 bytes** on a 64-bit JVM with compressed oops.

### 1.3 Reference Aliasing: The Pointer Truth
```java
BankAccount acc1 = new BankAccount(1000.0, "Ray");
BankAccount acc2 = acc1; // DOES NOT CREATE A NEW OBJECT
```
* **Stack:** Two distinct local reference variables `acc1` and `acc2`.
* **Heap:** Exactly **one** `BankAccount` instance. Both pointers store the identical memory address.
* `acc2.deposit(500)` directly modifies the single heap instance; reading `acc1.getBalance()` returns `1500.0`.

### 1.4 Critical Truth: Java is Strictly Pass-by-Value
Java NEVER passes objects by reference. **It passes reference addresses by value (copied bits).**

```java
void swap(Student a, Student b) {
    Student temp = a;
    a = b;
    b = temp; // Reassigning local pointer copies has ZERO effect on caller!
}

void modify(Student s) {
    s.setName("Bhanu"); // Mutating object fields through the copied pointer DOES affect caller!
}
```
* Reassigning the parameter reference (`s = new Student()`) only rebinds the local stack copy.
* Dereferencing and mutating (`s.setName(...)`) modifies the original heap object.

---

## 2. Constructors & Object Initialization Lifecycle

### 2.1 The Implicit Default Constructor Rule
* If a class defines **zero** constructors, `javac` automatically generates an implicit no-argument constructor:
  ```java
  public ClassName() { super(); }
  ```
* **The Trap:** The exact microsecond you declare **any** constructor (e.g. `User(String name)`), the compiler-generated default constructor **vanishes permanently**. Calling `new User()` will fail compilation unless you explicitly write a no-args constructor.

### 2.2 Constructor Chaining with `this(...)` and `super(...)`
* `this(...)` invokes another constructor in the same class (constructor delegation).
* `super(...)` invokes the parent class constructor.
* **Hard Rule:** Either `this(...)` or `super(...)` **must be the very first statement** in a constructor body.
* **Correlative Rule:** You can **never** have both `this(...)` and `super(...)` in the same constructor.

### 2.3 The Shallow vs. Deep Copy Constructor Hazard
When duplicating an object containing references to other mutable objects:
```java
// NAIVE SHALLOW COPY (DANGEROUS POINTER ALIASING BUG)
class Student {
    String name;
    Address address;

    Student(Student other) {
        this.name = other.name;
        this.address = other.address; // BUG: Copies memory address! Both students share one Address!
    }
}
// FIX: DEEP COPY CONSTRUCTOR
Student(Student other) {
    this.name = other.name;
    this.address = new Address(other.address); // Creates an independent nested heap instance!
}
```

### 2.4 The Definitive JVM Order of Initialization
When an object is instantiated for the first time:

```
[1] Superclass static variables & static blocks (class load, top-to-bottom)
      ↓
[2] Subclass static variables & static blocks (class load, top-to-bottom)
      ↓
[3] Superclass instance variables & instance initializers (top-to-bottom)
      ↓
[4] Superclass constructor body
      ↓
[5] Subclass instance variables & instance initializers (top-to-bottom)
      ↓
[6] Subclass constructor body
```
> [!WARNING]
> **Never call an overridable method inside a constructor.** The parent constructor runs *before* the child subclass fields are initialized. If an overridden method accesses child fields, it will read uninitialized default values (`null` or `0`), causing silent bugs or `NullPointerException`.

---

## 3. Encapsulation & Access Boundary Matrix

### 3.1 The 4 Access Modifiers Matrix

| Modifier | Class Scope | Package Scope | Subclass (Foreign Package) | World (Public) |
|---|:---:|:---:|:---:|:---:|
| `private` | **YES** | NO | NO | NO |
| *default* (package-private) | **YES** | **YES** | NO | NO |
| `protected` | **YES** | **YES** | **YES** *(via inheritance only)* | NO |
| `public` | **YES** | **YES** | **YES** | **YES** |

### 3.2 The `protected` Cross-Package Trap
In a foreign package, a subclass can access a `protected` member of its superclass **only through a reference of its own subclass type (or descendant)**:
```java
package pkgA;
public class Parent { protected int val = 10; }

package pkgB;
import pkgA.Parent;
public class Child extends Parent {
    void test() {
        System.out.println(this.val);       // OK: accessed via inheritance
        Child c = new Child();
        System.out.println(c.val);          // OK: accessed via Child reference
        Parent p = new Parent();
        // System.out.println(p.val);       // COMPILE ERROR: Cannot access protected member via Parent reference!
    }
}
```

### 3.3 The 5 Golden Rules of Immutable Classes
An immutable class guarantees its state cannot change once constructed (e.g. `java.lang.String`, `java.lang.Integer`, `LocalDate`):
1. **Declare the class `final`:** Prevents malicious subclassing from overriding methods to mutate state.
2. **Make all fields `private` and `final`:** Enforces encapsulation and ensures the JVM locks field values.
3. **Provide no setter methods:** Eliminates any mutator API.
4. **Defensive copies on constructor input:** If a constructor accepts a mutable parameter (like a `Date` or `List`), clone it: `this.items = new ArrayList<>(items);`.
5. **Defensive copies on getter output:** If a getter returns a mutable field, never return the internal reference: `return new ArrayList<>(this.items);` or `Collections.unmodifiableList(this.items)`.

---

## 4. Inheritance & Hierarchy Mechanics

### 4.1 IS-A vs. HAS-A
* **Inheritance (IS-A):** `class Dog extends Animal` — `Dog` is structurally and behaviorally an `Animal`.
* **Composition (HAS-A):** `class Car { private Engine engine; }` — `Car` holds a reference to `Engine`.

### 4.2 Why Java Forbids Multiple Class Inheritance
Java forbids `class C extends A, B` to eliminate the **Diamond Problem**:
```
       Class A (defines void display())
       /      \
  Class B      Class C (both override display())
       \      /
       Class D (extends B, C) -> Which display() should D inherit? AMBIGUOUS!
```
* Multiple inheritance of *classes* leads to ambiguous instance state and conflicting method bodies.
* Java solves this by allowing single class inheritance (`extends OneClass`) while permitting multiple interface implementation (`implements InterfaceA, InterfaceB`).

### 4.3 What is Inherited vs. What is Not
* **Constructors:** **NEVER inherited.** Subclasses must declare their own constructors and invoke `super(...)`.
* **`private` Members:** **Inherited in memory payload, but INACCESSIBLE by name.** The subclass's heap memory contains the parent's private fields, but subclass code cannot access them directly; it must use the parent's `public`/`protected` getters/setters.
* **`static` Members:** Inherited as class-level members, but **cannot be overridden** (only hidden).

---

## 5. Polymorphism & Method Resolution Engine

### 5.1 Static vs. Dynamic Polymorphism at a Glance

| Dimension | Compile-Time (Static) | Runtime (Dynamic) |
|---|---|---|
| **Mechanism** | Method Overloading | Method Overriding |
| **Binding Mechanism** | Early binding by compiler (`javac`) | Late binding by JVM (`vtable` virtual dispatch) |
| **Decision Factor** | Reference type & argument compile-time types | Actual object type on the Heap |
| **Execution Bytecode** | `invokestatic` / `invokevirtual` with static signature | `invokevirtual` resolved dynamically |

### 5.2 The 4-Tier Compiler Overload Resolution Ladder
When compiling an overloaded method call, `javac` evaluates candidates in 4 strict hierarchical phases:

```
Tier 1: Exact Match without any conversion
           ↓ (if no match)
Tier 2: Primitive Widening Conversion
        (byte → short → int → long → float → double, and char → int)
           ↓ (if no match)
Tier 3: Autoboxing / Unboxing Conversion
        (int ↔ Integer, double ↔ Double)
           ↓ (if no match)
Tier 4: Variable-Length Arguments (Varargs `...`)
```
> [!IMPORTANT]
> **Widening Primitive beats Autoboxing:**
> ```java
> void print(long x)    { System.out.println("long"); }
> void print(Integer x) { System.out.println("Integer"); }
> 
> print(10); // PRINTS: "long"! Primitive widening (int -> long) wins over autoboxing (int -> Integer)!
> ```

### 5.3 Overloading Ambiguity & `null` Resolution
* **Ambiguous Overloading:** If two methods match equally well across different arguments:
  ```java
  void show(int a, long b) {}
  void show(long a, int b) {}
  show(2, 3); // COMPILE ERROR: Ambiguous call! Both candidates match equally.
  ```
* **Passing `null`:** The compiler always selects the **most specific type** in the inheritance tree:
  ```java
  void test(Object o) { System.out.println("Object"); }
  void test(String s) { System.out.println("String"); }
  test(null); // PRINTS: "String" (String is more specific than Object)
  
  // BUT:
  void test(Integer i) {}
  test(null); // COMPILE ERROR: Ambiguous between String and Integer (unrelated siblings)!
  ```

### 5.4 The 4 Invariants of Method Overriding
When overriding a method in a subclass:
1. **Method Signature:** Must match the parent method name and parameter types identically.
2. **Return Type:** Must be **invariant** or **covariant** (the subclass method may return a subtype of the parent's return type).
3. **Access Modifiers:** May **widen** visibility (`protected` -> `public`), but can **never narrow** it (`public` -> `private` is forbidden).
4. **Checked Exceptions:** May declare **fewer or narrower** checked exceptions, but can **never throw broader or new** checked exceptions. (Unchecked `RuntimeException`s have no restrictions).

### 5.5 Critical Trap: Static Methods & Fields are NOT Polymorphic
* Static methods are bound at compile-time based on the **reference type**, not the heap object. This is **Method Hiding**, not Overriding.
* Fields are shadowed, never virtual:
```java
class Parent { String name = "Parent"; static void say() { System.out.println("P"); } }
class Child extends Parent { String name = "Child"; static void say() { System.out.println("C"); } }

Parent p = new Child();
System.out.println(p.name); // PRINTS: "Parent" (Fields are resolved by reference type!)
p.say();                    // PRINTS: "P" (Static methods are resolved by reference type!)
```

---

## 6. Abstraction & Modern Interface Architecture

### 6.1 Abstract Class vs. Interface Decision Matrix

| Dimension | Abstract Class | Interface (Modern Java 8/9/17+) |
|---|---|---|
| **Inheritance Model** | Single (`extends OneClass`) | Multiple (`implements InterfaceA, InterfaceB`) |
| **State & Instance Fields** | Full instance fields (`private int x;`), mutable state | Only `public static final` constants |
| **Constructors** | **YES** (invoked via `super(...)`) | **NO** (cannot hold constructor logic) |
| **Method Types** | Abstract, concrete, `final`, `static`, `private` | Abstract, `default` (Java 8), `static` (Java 8), `private` (Java 9) |
| **Architectural Intent** | "IS-A" identity: core shared blueprint & state | "CAN-DO" capability: behavioral contract across unrelated hierarchies |

### 6.2 Modern Interface Method Types
Since Java 8 and 9, interfaces are rich behavioral contracts:
1. **Abstract Methods:** Public contract without implementation: `void execute();`.
2. **Default Methods (`default`):** Provides default behavior without breaking existing implementers:
   ```java
   default void log(String msg) { System.out.println(msg); }
   ```
3. **Static Methods (`static`):** Utility methods bound to the interface namespace: `InterfaceName.utilMethod()`.
4. **Private Methods (`private`):** Helper methods encapsulated inside the interface to share logic between default methods (Java 9+).

### 6.3 Resolving Default Method Diamond Conflict
If class `C` implements interfaces `A` and `B` which both define `default void ping()`:
```java
class C implements A, B {
    @Override
    public void ping() {
        A.super.ping(); // Explicitly disambiguate which default method to invoke!
    }
}
```

---

## 7. `static` and `final` Mechanics

### 7.1 `static` Architecture
* **Memory:** Housed in Metaspace / Method Area, shared across all instances of the class.
* **No `this`:** A static context has no implicit `this` pointer because it is not tied to any heap instance.
* **Static Initialization Block:** Executes **once** when the class is loaded into the JVM by the ClassLoader:
  ```java
  static {
      // Runs once at class-load time
  }
  ```

### 7.2 The Three Uses of `final`
1. **`final` Variable:** The variable can be assigned **exactly once**.
   * Primitive: Value cannot change.
   * Reference: Pointer cannot be reassigned to point to another heap address. **The underlying object's fields can still mutate!**
2. **`final` Method:** Cannot be overridden by any subclass. Allows JIT compilers to perform method inlining.
3. **`final` Class:** Cannot be extended (`class Sub extends FinalClass` fails compilation). Protects immutability and security.

### 7.3 `static final` Constants
```java
public static final double GRAVITY = 9.80665;
```
* Evaluated at compile time.
* `javac` inlines compile-time primitive/string constants directly into referencing bytecode classes!

---

## 8. `java.lang.Object` Contracts & Essentials

### 8.1 The 5 Mathematical Axioms of `equals()`
When overriding `boolean equals(Object obj)`:
1. **Reflexive:** `x.equals(x)` must return `true`.
2. **Symmetric:** `x.equals(y) == y.equals(x)`.
3. **Transitive:** If `x.equals(y)` and `y.equals(z)`, then `x.equals(z)` must return `true`.
4. **Consistent:** Repeated calls return the same boolean if fields are unchanged.
5. **Non-nullity:** `x.equals(null)` must return `false`.

### 8.2 The Canonical `equals()` & `hashCode()` Implementation
```java
@Override
public boolean equals(Object o) {
    if (this == o) return true;                         // 1. Reference identity check
    if (o == null || getClass() != o.getClass()) return false; // 2. Null & strict type check
    Employee that = (Employee) o;                       // 3. Cast
    return id == that.id && Objects.equals(name, that.name); // 4. Compare significant fields
}

@Override
public int hashCode() {
    return Objects.hash(id, name); // ALWAYS hash the exact same fields used in equals()!
}
```

### 8.3 The Fatal HashMap / HashSet Corruption Trap
* **The Contract:** If `a.equals(b)` is `true`, then `a.hashCode() == b.hashCode()` **MUST be true**.
* **What happens if you override `equals()` but forget `hashCode()`?**
  Two logically identical objects will produce **different hash codes**, landing in different hash buckets. Searching for an existing key in a `HashMap` will look in the wrong bucket and return `null`!

### 8.4 Why `Cloneable` is Broken
The `java.lang.Cloneable` interface is widely considered an architectural mistake in Java:
* `clone()` does not invoke any constructor (violating normal object lifecycle invariants).
* Performs only a shallow field-by-field copy by default.
* Throws checked `CloneNotSupportedException` and requires unchecked type-casting.
* **Modern Best Practice:** Use a **Copy Constructor** (`new Course(otherCourse)`) or a **Static Factory Method** (`Course.copyOf(other)`).

---

## 9. Object Relationships: Composition vs. Inheritance

### 9.1 Relationship Taxonomy

```
+---------------------------------------------------------------------------------+
|                                OBJECT RELATIONSHIPS                             |
+-------------------+-----------------------------+-------------------------------+
| RELATIONSHIP      | LIFECYCLE OWNERSHIP         | ARCHITECTURAL EXAMPLE         |
+-------------------+-----------------------------+-------------------------------+
| 1. Composition    | Strong Ownership (Death together) | Spaceship HAS-A Thruster  |
| 2. Aggregation    | Weak Ownership (Independent life)| Playlist HAS-A Song       |
| 3. Association    | Peer Connection (Uses-A)    | Driver USES-A GPSNavigation   |
| 4. Inheritance    | Identity Coupling (IS-A)    | MainQuest IS-A Quest          |
+-------------------+-----------------------------+-------------------------------+
```

### 9.2 Why "Favor Composition Over Inheritance" (Gang of Four)
1. **The Fragile Base Class Problem:** Modifying a superclass's internal implementation can silently break subclasses that rely on inherited behavior.
2. **Encapsulation Breakdown:** Inheritance exposes protected internals of the parent to the child.
3. **Runtime Swapping:** Composed dependencies can be swapped at runtime via setters or dependency injection; inheritance hierarchies are frozen at compile time.
4. **Single Inheritance Slot:** Java allows extending only one class; composition keeps your inheritance slot free.

---

## 10. Nested, Inner, Local & Anonymous Classes

### 10.1 The 4 Nested Class Varieties

| Type | Outer Instance Required? | Outer Member Access | Instantiation Syntax | Typical Use Case |
|---|:---:|---|---|---|
| **Static Nested Class** | **NO** | Static members only | `new Outer.StaticNested()` | Builder pattern, co-located DTOs |
| **Member Inner Class** | **YES** | All (private, static, instance) | `outer.new Inner()` | Iterators, UI event delegates |
| **Local Class** | **YES** *(in instance method)*| All + Effectively Final locals | Inside declaring method only | Complex localized algorithm logic |
| **Anonymous Class** | **YES** *(in instance context)*| All + Effectively Final locals | `new Runnable() { ... }` | One-off callbacks, quick overrides |

### 10.2 Accessing the Outer Instance: `Outer.this`
Inside a member inner class, reference shadowed outer members using `OuterClass.this.fieldName`.

### 10.3 The "Effectively Final" Closure Capture Rule
Local and anonymous classes can access local variables from their enclosing method **only if they are never mutated after initialization (effectively final)**.
* **JVM Reason:** Local variables live on the Stack (destroyed when the method returns). The inner class object lives on the Heap (can outlive the method). The JVM copies the local variable's value into the inner class instance. If the variable could be modified, the Stack and Heap copies would become out of sync!

---

## 11. Enums as First-Class Types

### 11.1 Enums Under the Hood
In Java, an `enum` is **not an integer**:
* It is a class implicitly extending `java.lang.Enum<E>`.
* It is implicitly `final` and cannot be extended.
* Each declared enum constant is a `public static final` singleton instance of that enum class.
* Enum constructors are strictly `private`.

### 11.2 Constant-Specific Method Bodies (Strategy Pattern)
Enums can declare abstract methods that each constant implements differently:
```java
enum Operation {
    PLUS { double apply(double x, double y) { return x + y; } },
    MINUS { double apply(double x, double y) { return x - y; } };

    abstract double apply(double x, double y);
}
```

### 11.3 High-Performance Enum Collections
* **`EnumSet`:** Internally backed by a single bit-vector (`long`). Incredibly fast, zero-allocation bitwise operations.
* **`EnumMap`:** Internally backed by a simple array indexed by enum `ordinal()`. Much faster than a standard `HashMap`.

---

## 12. Modern Domain Architecture Spotlight
*(Real-world engineering implementations from the repository)*

### 🎮 Gaming Architecture
* **`InventoryItem` (Encapsulation):** Enforces finite backpack capacity (`bagSize = 6`). State mutation happens only through `addItem()` and `removeItem()` with capacity invariant validation.
* **`Quest` Hierarchy (Inheritance):** Base `Quest` defines `rewardXP` and `complete()`. `MainQuest` adds unskippable cutscene rules, `SideQuest` adds time limits, `DailyBounty` adds midnight reset logic.
* **`Avatar` (Dynamic Polymorphism):** `avatar.triggerAbility()` polymorphic dispatch calls `MageAvatar.castSpell()` or `WarriorAvatar.meleeStrike()` via `vtable`.

### 🏠 Smart Home Ecosystem
* **`SmartLock` (Encapsulation):** Private passcode hash; `unlock(String code)` locks out user for 48 hours after 3 failed attempts.
* **`SmartBulb` (Inheritance):** Extends `SmartAppliance`. Brightness level mutation enforces invariant: appliance power must be `ON` and brightness between 0-100.
* **`SmartHomeHub` (Aggregation):** Hub manages a dynamic list of `SmartAppliance` items. If the hub disconnects or is deleted, appliances continue running independently.

### 🚀 Sci-Fi & Aerospace Engineering
* **`TimeCapsule` (Encapsulation):** Sealed message locked until a specific target `LocalDate`. Inquiries before the date print remaining countdown days via `ChronoUnit.DAYS`.
* **`Spaceship` (Composition):** Composed of `Thruster` and `LifeSupport` instances. If the spaceship is destroyed, its propulsion and life support systems are destroyed with it.

---

## 13. The 25 Essential Interview Traps & Output Puzzles

#### Q01: Overload with Primitive Widening vs Autoboxing
```java
class Test {
    static void go(long x)    { System.out.print("L "); }
    static void go(Integer x) { System.out.print("I "); }
    public static void main(String[] args) { int a = 5; go(a); }
}
```
* **Output:** `L `
* **JVM Reason:** Primitive widening (`int` -> `long`) has higher precedence than Autoboxing (`int` -> `Integer`).

---

#### Q02: Overload with Ambiguous Autoboxing
```java
class Test {
    static void go(int a, long b) { System.out.print("1 "); }
    static void go(long a, int b) { System.out.print("2 "); }
    public static void main(String[] args) { go(5, 5); }
}
```
* **Output:** **Compile-time Error:** `reference to go is ambiguous`
* **JVM Reason:** Neither `(int, long)` nor `(long, int)` is more specific when passing two literal `int`s.

---

#### Q03: Passing `null` to Overloaded Methods
```java
class Test {
    static void show(Object o) { System.out.print("Object "); }
    static void show(String s) { System.out.print("String "); }
    public static void main(String[] args) { show(null); }
}
```
* **Output:** `String `
* **JVM Reason:** When arguments are `null`, the compiler picks the most specific reference type in the hierarchy.

---

#### Q04: Passing `null` to Sibling Types
```java
class Test {
    static void show(Integer i) { System.out.print("Integer "); }
    static void show(String s)  { System.out.print("String "); }
    public static void main(String[] args) { show(null); }
}
```
* **Output:** **Compile-time Error:** `reference to show is ambiguous`
* **JVM Reason:** `Integer` and `String` are sibling types with no subtype relationship; the compiler cannot pick one.

---

#### Q05: Static Method Hiding vs Dynamic Dispatch
```java
class Parent { static void ping() { System.out.print("P "); } }
class Child extends Parent { static void ping() { System.out.print("C "); } }
public class Main {
    public static void main(String[] args) {
        Parent p = new Child();
        p.ping();
    }
}
```
* **Output:** `P `
* **JVM Reason:** Static methods cannot be overridden; they are *hidden*. Static calls are resolved by the reference type (`Parent`) at compile-time.

---

#### Q06: Field Shadowing (Fields Are Not Virtual)
```java
class A { int x = 10; }
class B extends A { int x = 20; }
public class Main {
    public static void main(String[] args) {
        A a = new B();
        System.out.println(a.x);
    }
}
```
* **Output:** `10`
* **JVM Reason:** Field access is resolved at compile time using the reference type (`A`), never at runtime via `vtable`.

---

#### Q07: Calling Overridden Method in Constructor
```java
class Parent {
    Parent() { print(); }
    void print() { System.out.print("P "); }
}
class Child extends Parent {
    int val = 42;
    void print() { System.out.print(val + " "); }
}
public class Main {
    public static void main(String[] args) { new Child(); }
}
```
* **Output:** `0 `
* **JVM Reason:** When `Parent`'s constructor executes `print()`, virtual dispatch calls `Child.print()`. However, `Child`'s fields have not yet been initialized; `val` holds its default primitive value `0`!

---

#### Q08: Initialization Order with Static and Instance Blocks
```java
class Base {
    static { System.out.print("S1 "); }
    { System.out.print("I1 "); }
    Base() { System.out.print("C1 "); }
}
class Sub extends Base {
    static { System.out.print("S2 "); }
    { System.out.print("I2 "); }
    Sub() { System.out.print("C2 "); }
}
public class Main {
    public static void main(String[] args) { new Sub(); }
}
```
* **Output:** `S1 S2 I1 C1 I2 C2 `
* **JVM Reason:** All static initializers execute first in inheritance order (`S1 S2`), followed by parent instance init + constructor (`I1 C1`), then child instance init + constructor (`I2 C2`).

---

#### Q09: Static Block Execution on Second Instantiation
```java
// Running same Sub class as Q08:
new Sub();
new Sub();
```
* **Output:** `S1 S2 I1 C1 I2 C2 I1 C1 I2 C2 `
* **JVM Reason:** Static initializers run **only once** per class loading lifetime. Instance blocks and constructors run on every `new`.

---

#### Q10: Pass-by-Value Object Pointer Reassignment
```java
class Box { int val; Box(int v) { val = v; } }
public class Main {
    static void change(Box b) {
        b = new Box(99);
    }
    public static void main(String[] args) {
        Box box = new Box(10);
        change(box);
        System.out.println(box.val);
    }
}
```
* **Output:** `10`
* **JVM Reason:** `b` in `change()` is a local copy of the reference address. Reassigning `b` has zero impact on the caller's `box` reference.

---

#### Q11: Pass-by-Value Object Field Mutation
```java
class Box { int val; Box(int v) { val = v; } }
public class Main {
    static void mutate(Box b) {
        b.val = 99;
    }
    public static void main(String[] args) {
        Box box = new Box(10);
        mutate(box);
        System.out.println(box.val);
    }
}
```
* **Output:** `99`
* **JVM Reason:** The copied reference address points to the exact same heap instance; mutating fields affects the original object.

---

#### Q12: Varargs vs Fixed Arity Overload
```java
class Test {
    static void run(int a, int b) { System.out.print("Fixed "); }
    static void run(int... nums)  { System.out.print("Varargs "); }
    public static void main(String[] args) { run(1, 2); }
}
```
* **Output:** `Fixed `
* **JVM Reason:** Exact fixed arity matches always beat variable-length argument methods.

---

#### Q13: Breaking the `equals()` / `hashCode()` Contract
```java
class Key {
    int id;
    Key(int id) { this.id = id; }
    @Override public boolean equals(Object o) { return o instanceof Key && ((Key)o).id == this.id; }
    // Forgot to override hashCode()!
}
public class Main {
    public static void main(String[] args) {
        Map<Key, String> map = new HashMap<>();
        map.put(new Key(1), "Found");
        System.out.println(map.get(new Key(1)));
    }
}
```
* **Output:** `null`
* **JVM Reason:** The two `Key(1)` instances have different default `System.identityHashCode()` values. `map.get()` looks in the wrong bucket and misses the key.

---

#### Q14: String Immutability and Substring Modification
```java
public class Main {
    public static void main(String[] args) {
        String s1 = "Hello";
        s1.concat(" World");
        System.out.println(s1);
    }
}
```
* **Output:** `Hello`
* **JVM Reason:** `String` is immutable. `concat()` returns a brand new `String` object; `s1` remains unchanged because the return value was discarded.

---

#### Q15: String Literal Pool vs `new String()`
```java
public class Main {
    public static void main(String[] args) {
        String s1 = "Java";
        String s2 = "Java";
        String s3 = new String("Java");
        System.out.print((s1 == s2) + " ");
        System.out.print((s1 == s3) + " ");
        System.out.print(s1.equals(s3));
    }
}
```
* **Output:** `true false true`
* **JVM Reason:** `s1` and `s2` reference the same interned literal in the String Pool. `s3` creates an explicit distinct object on the Heap. `equals()` checks character equivalence.

---

#### Q16: Final Reference Variable Field Mutation
```java
class Person { String name = "Alex"; }
public class Main {
    public static void main(String[] args) {
        final Person p = new Person();
        p.name = "John";
        System.out.println(p.name);
    }
}
```
* **Output:** `John`
* **JVM Reason:** `final` locks the reference address (cannot do `p = new Person()`), but the object's internal state remains fully mutable.

---

#### Q17: Polymorphic Array Downcasting
```java
class Animal {}
class Dog extends Animal {}
class Cat extends Animal {}
public class Main {
    public static void main(String[] args) {
        Animal a = new Dog();
        Cat c = (Cat) a;
    }
}
```
* **Output:** `Exception in thread "main" java.lang.ClassCastException: Dog cannot be cast to Cat`
* **JVM Reason:** The heap instance is a `Dog`. Attempting to downcast an incompatible subtype throws `ClassCastException` at runtime.

---

#### Q18: Covariant Return Types in Action
```java
class Producer { Producer make() { return new Producer(); } }
class SubProducer extends Producer {
    @Override SubProducer make() { return new SubProducer(); }
}
public class Main {
    public static void main(String[] args) {
        SubProducer sp = new SubProducer();
        SubProducer result = sp.make(); // Compiles without casting!
        System.out.println("Success");
    }
}
```
* **Output:** `Success`
* **JVM Reason:** Java allows an overriding method to narrow its return type to a subtype (covariance).

---

#### Q19: Widening Access Modifier on Overriding
```java
class Parent { protected void act() {} }
class Child extends Parent { public void act() {} } // Legal widening!
```
* **Compilation:** Succeeds.
* **JVM Reason:** Subclasses can widen accessibility (`protected` -> `public`), but narrowing (`protected` -> `private`) is illegal.

---

#### Q20: Narrowing Checked Exceptions on Overriding
```java
class Parent { void read() throws IOException {} }
class Child extends Parent { void read() throws FileNotFoundException {} }
```
* **Compilation:** Succeeds.
* **JVM Reason:** `FileNotFoundException` is a subclass of `IOException`. Overriding methods can throw fewer or narrower checked exceptions.

---

#### Q21: Declaring Broader Checked Exception on Overriding
```java
class Parent { void read() throws IOException {} }
class Child extends Parent { void read() throws Exception {} }
```
* **Compilation:** **Compile-time Error:** `read() in Child cannot override read() in Parent; overridden method does not throw java.lang.Exception`
* **JVM Reason:** Subclasses cannot break callers expecting `IOException` by throwing broader checked exceptions like `Exception`.

---

#### Q22: Effectively Final Variable Mutation
```java
public class Main {
    public static void main(String[] args) {
        int factor = 2;
        Runnable r = () -> System.out.println(factor);
        factor = 3; // Mutated after lambda capture!
        r.run();
    }
}
```
* **Output:** **Compile-time Error:** `local variables referenced from a lambda expression must be final or effectively final`
* **JVM Reason:** Reassigning `factor` breaks its "effectively final" status, preventing closure capture.

---

#### Q23: Member Inner Class Instantiation Syntax
```java
class Outer {
    class Inner { void ping() { System.out.println("Inner"); } }
}
public class Main {
    public static void main(String[] args) {
        Outer.Inner in = new Outer().new Inner();
        in.ping();
    }
}
```
* **Output:** `Inner`
* **JVM Reason:** A non-static inner class instance requires an explicit enclosing instance: `outerInstance.new Inner()`.

---

#### Q24: Static Nested Class Instantiation Syntax
```java
class Outer {
    static class Nested { void ping() { System.out.println("Nested"); } }
}
public class Main {
    public static void main(String[] args) {
        Outer.Nested n = new Outer.Nested();
        n.ping();
    }
}
```
* **Output:** `Nested`
* **JVM Reason:** Static nested classes require no enclosing instance; instantiated like any top-level class.

---

#### Q25: Enum Identity vs Value Equality
```java
enum Status { ACTIVE, INACTIVE }
public class Main {
    public static void main(String[] args) {
        Status s1 = Status.ACTIVE;
        Status s2 = Status.ACTIVE;
        System.out.println(s1 == s2);
        System.out.println(s1.equals(s2));
    }
}
```
* **Output:** `true true`
* **JVM Reason:** Enum constants are JVM-level singletons. Both `==` (reference identity) and `equals()` evaluate to `true`.

---

## 14. The 5-Minute Pre-Interview Rapid Recap

* **Pass-by-Value:** Java copies reference bits. Modifying parameter pointers does not affect caller; modifying fields does.
* **Object Header:** 16 bytes for empty object on 64-bit JVM (8B Mark Word + 4B/8B Klass Pointer + padding).
* **Default Constructor:** Exists ONLY if zero constructors are declared. Writing any constructor removes it.
* **`this()` & `super()`:** Must be line 1 of constructor. Mutually exclusive in the same constructor.
* **Initialization Order:** Static (parent -> child) → Instance & Fields (parent) → Constructor (parent) → Instance & Fields (child) → Constructor (child).
* **Overload Ladder:** Exact Match → Primitive Widening → Autoboxing → Varargs. Primitive widening beats autoboxing!
* **Static Methods / Fields:** NOT polymorphic. Hidden, not overridden. Bound by reference type at compile-time.
* **Overriding Rules:** Identical signature, covariant return, widened access modifier, narrower checked exceptions.
* **`equals()` & `hashCode()`:** ALWAYS override both together using the same fields. Breaking this corrupts hash collections.
* **Immutability:** `final` class, `private final` fields, no setters, defensive copies on constructor in and getter out.
* **Composition vs Inheritance:** Favor composition. Solves Fragile Base Class problem and allows runtime dependency swapping.
* **Inner Class Closure:** Local/anonymous classes access enclosing local variables ONLY if they are effectively final.
* **Enums:** Rich classes extending `java.lang.Enum`. Constants are `public static final` singletons with private constructors.
