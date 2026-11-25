# Java `static` and `final` Keywords: Detailed Notes

`static` and `final` are two independent keywords that frequently show up together but mean very different things:

```text
static   — belongs to the CLASS itself, shared across all instances (one copy, not per-object)
final    — cannot be changed/overridden/extended once set (locks something down)
```

```java
class Counter {
    static int count = 0;        // one shared copy, belongs to the class
    final int id;                 // locked once assigned, cannot change afterward

    Counter() {
        count++;
        id = count;                // assigned once, in the constructor
    }
}
```

---

## Table of Contents

1. [The `static` Keyword: Class-Level vs Instance-Level](#1-the-static-keyword-class-level-vs-instance-level)
2. [Static Variables](#2-static-variables)
3. [Static Methods](#3-static-methods)
4. [Static Blocks](#4-static-blocks)
5. [Static Nested Classes](#5-static-nested-classes)
6. [Static Nested Classes vs Static Blocks](#6-static-nested-classes-vs-static-blocks)
7. [The `final` Keyword: Three Uses](#7-the-final-keyword-three-uses)
8. [`final` Variables (Constants)](#8-final-variables-constants)
9. [`final` Methods](#9-final-methods)
10. [`final` Classes](#10-final-classes)
11. [`static final` Combined](#11-static-final-combined)
12. [Common Pitfall: Static Methods Can't Be Overridden, Only Hidden](#12-common-pitfall-static-methods-cant-be-overridden-only-hidden)
13. [Common Pitfalls](#13-common-pitfalls)
14. [Interview Questions](#14-interview-questions)
15. [Interview-Style Output Questions](#15-interview-style-output-questions)
16. [Quick Cheat Sheet](#16-quick-cheat-sheet)

---

## 1. The `static` Keyword: Class-Level vs Instance-Level

Every class member (field, method, nested class, or block) is, by default, an **instance member** — it belongs to each individual object, and a separate copy/behavior exists per object. Adding `static` makes it a **class member** instead — it belongs to the **class itself**, shared by every instance, and it exists even if **no objects at all** have been created yet.

```java
class Student {
    String name;            // instance field — each Student object gets its OWN copy
    static String school;   // static field — ONE copy, shared by ALL Student objects
}
```

```java
Student s1 = new Student();
Student s2 = new Student();

s1.name = "Aashish";
s2.name = "Dipesh";
// s1.name and s2.name are independent — two separate copies

Student.school = "GIET";
System.out.println(s1.school);   // "GIET" — same shared copy
System.out.println(s2.school);   // "GIET" — same shared copy, because there's only ONE
```

```text
INSTANCE member    ->  one copy PER OBJECT    -> accessed as  object.member
STATIC member      ->  one copy PER CLASS     -> accessed as  ClassName.member (preferred)
                                                  or object.member (works, but discouraged — see 2.3)
```

---

## 2. Static Variables

A **static variable** (also called a **class variable**) is shared across every instance of the class. Changing it through any one object (or through the class name) changes the single shared value everyone sees.

```java
class Counter {
    static int totalObjects = 0;   // shared counter

    Counter() {
        totalObjects++;             // every construction increments the ONE shared copy
    }
}
```

```java
new Counter();
new Counter();
new Counter();
System.out.println(Counter.totalObjects);   // 3
```

### 2.1 When to use a static variable

- A value that is genuinely a property of the **class as a whole**, not of any individual object — a running count, a shared configuration value, a cache.
- Constants shared by all instances (almost always combined with `final` — see section 11).

### 2.2 Static variables exist before any object does

```java
class Config {
    static String environment = "production";
}

System.out.println(Config.environment);   // "production" — accessible with ZERO Config objects ever created
```

### 2.3 Accessing a static variable through an instance works, but is discouraged

```java
Counter c = new Counter();
System.out.println(c.totalObjects);   // works, compiles — but misleadingly suggests it's instance-specific
System.out.println(Counter.totalObjects);   // preferred — makes the "shared, class-level" nature obvious
```

Most IDEs and linters will warn against `instance.staticMember` access precisely because it reads like instance state when it isn't.

---

## 3. Static Methods

A **static method** belongs to the class and can be called **without creating any object**. Because it has no connection to any particular instance, it cannot use `this`, and it cannot directly access instance (non-static) fields or methods.

```java
class MathUtils {
    static int square(int x) {
        return x * x;
    }
}

int result = MathUtils.square(5);   // called via the class name, no object needed
System.out.println(result);          // 25
```

### 3.1 A static method cannot access instance members directly

```java
class Demo {
    int instanceField = 10;

    static void show() {
        // System.out.println(instanceField);   // COMPILE ERROR: cannot reference non-static field from a static context
    }
}
```

This makes sense: a static method could be called with **zero objects in existence**, so there would be no particular `instanceField` to read.

### 3.2 An instance method CAN freely access static members

```java
class Demo {
    static int sharedValue = 100;

    void show() {
        System.out.println(sharedValue);   // OK — an instance method always has access to static (class-level) state too
    }
}
```

### 3.3 Why use a static method

- **Utility/helper logic** that doesn't depend on any object's state at all (`Math.sqrt()`, `Collections.sort()`).
- **Factory methods** that construct and return instances (`List.of(...)`, a custom `Student.createDefault()`).
- The class's `main()` method — the JVM calls `main` with no `Student`-like object to work with yet, so it **must** be `static`.

```java
public class Main {
    public static void main(String[] args) {   // called by the JVM with no object created first
        System.out.println("Hello World");
    }
}
```

---

## 4. Static Blocks

A **static initializer block** is a block of code marked `static { ... }` that runs **exactly once**, automatically, the **first time the class is loaded** by the JVM — before any object of that class is created, and even if no object is ever created at all.

```java
class Config {
    static String environment;

    static {
        environment = loadFromSomewhere();
        System.out.println("Static block ran");
    }

    static String loadFromSomewhere() { return "production"; }
}
```

```java
new Config();
new Config();
new Config();
// "Static block ran" prints only ONCE in total, no matter how many objects are created
```

### 4.1 Multiple static blocks run in source order, top to bottom

```java
class Demo {
    static int a = 1;
    static { System.out.println("block 1, a=" + a); }
    static int b = 2;
    static { System.out.println("block 2, b=" + b); }
}

new Demo();
// block 1, a=1
// block 2, b=2
```

### 4.2 Why use a static block

- Performing **non-trivial initialization** of static fields that can't be done in a single expression — loading a config file, computing a derived value, catching a checked exception during setup.
- Guaranteeing the initialization logic runs **exactly once**, no matter how many instances are created.

```java
class LookupTable {
    static final Map<Integer, String> TABLE = new HashMap<>();

    static {
        TABLE.put(1, "One");
        TABLE.put(2, "Two");
        TABLE.put(3, "Three");
    }
}
```

### 4.3 Static blocks run before instance blocks and constructors

This is part of Java's full initialization order: **static** (once, at class load) → **instance** (every object) → **constructor** (every object) — covered in depth in the constructors notes, but worth restating here since static blocks are the piece unique to this topic.

```java
class Demo {
    static { System.out.println("1. static block"); }
    { System.out.println("2. instance block"); }
    Demo() { System.out.println("3. constructor"); }
}

new Demo();
new Demo();
// 1. static block        (only once, first time)
// 2. instance block
// 3. constructor
// 2. instance block       (repeats for the second object)
// 3. constructor
```

---

## 5. Static Nested Classes

A **static nested class** is a class declared `static` **inside** another class. Unlike a regular (non-static) **inner class**, it does **not** hold an implicit reference to an instance of the enclosing class — it behaves like a normal top-level class that just happens to be namespaced inside another class.

```java
class Outer {
    static int outerStaticField = 100;
    int outerInstanceField = 5;

    static class Nested {
        void show() {
            System.out.println(outerStaticField);     // OK — can access the enclosing class's static members
            // System.out.println(outerInstanceField); // COMPILE ERROR — no implicit Outer instance to read from
        }
    }
}
```

### 5.1 Creating a static nested class does not require an Outer instance

```java
Outer.Nested n = new Outer.Nested();   // no "Outer" object needed at all
n.show();
```

Compare this to a **non-static (inner) class**, which *does* require an enclosing instance:

```java
class Outer {
    class Inner {    // NOT static — a true inner class
    }
}

Outer o = new Outer();
Outer.Inner i = o.new Inner();   // requires an Outer instance to construct
```

### 5.2 Why use a static nested class

- Grouping a **helper class** tightly with the class it supports, when that helper doesn't need access to the enclosing instance's state (e.g., `Map.Entry`, a `Node` class inside a `LinkedList` implementation).
- Keeping related classes organized under one namespace without polluting the top-level package with many small classes.

```java
class LinkedList {
    static class Node {      // doesn't need any particular LinkedList instance
        int value;
        Node next;
        Node(int value) { this.value = value; }
    }

    Node head;
}
```

---

## 6. Static Nested Classes vs Static Blocks

These two are easy to conflate because both use the word `static` inside another class, but they solve **completely different problems**.

| | Static Block | Static Nested Class |
|---|---------------|------------------------|
| What it is | A block of **code** (`static { ... }`) | A **class definition** nested inside another class |
| Purpose | Run initialization logic **once**, at class-loading time | Define a **reusable type**, grouped under the enclosing class's namespace |
| Runs automatically? | Yes — automatically, once, when the class is loaded | No — it's a class, you instantiate it (or use it) like any other class, whenever you choose |
| Produces a value/object? | No — it just executes statements | Yes — you create instances of it with `new Outer.Nested()` |
| Relationship to enclosing class | Executes in the context of setting up the enclosing class's static state | Can access the enclosing class's static members, but has no automatic link to any particular instance |

```java
class Example {
    static int value;

    static {                       // STATIC BLOCK — runs once, sets up `value`
        value = 42;
    }

    static class Helper {          // STATIC NESTED CLASS — a type definition, instantiated on demand
        void printValue() {
            System.out.println(value);
        }
    }
}
```

```java
// The static block already ran by the time this line executes (class is loaded on first use)
Example.Helper h = new Example.Helper();   // explicitly creating an instance of the nested class
h.printValue();   // 42
```

In short: a **static block runs code**; a **static nested class defines a type** you can later create instances of. One happens automatically and only once; the other happens whenever and however many times you choose to instantiate it.

---

## 7. The `final` Keyword: Three Uses

`final` means **"cannot be changed further"**, but what exactly it locks down depends on what it's applied to:

```text
final variable    — the variable's value/reference cannot be reassigned after initialization
final method      — the method cannot be overridden by any subclass
final class       — the class cannot be extended (subclassed) at all
```

---

## 8. `final` Variables (Constants)

A `final` variable can be assigned **exactly once** — either at declaration, or later, but only one single time (and it must definitely happen before the variable is used).

```java
final int MAX_USERS = 100;
// MAX_USERS = 200;   // COMPILE ERROR: cannot assign a value to final variable MAX_USERS
```

### 8.1 A `final` local variable can be assigned later, but only once

```java
final int x;         // declared, not yet assigned
if (someCondition) {
    x = 10;
} else {
    x = 20;
}
// x is definitely assigned exactly once along every possible path — this compiles
System.out.println(x);
```

```java
final int y;
y = 5;
y = 10;   // COMPILE ERROR: variable y might already have been assigned
```

### 8.2 `final` on a reference type locks the reference, NOT the object's contents

This is one of the most important and most misunderstood rules around `final`.

```java
final int[] arr = {1, 2, 3};
arr[0] = 99;              // OK — the ARRAY's contents can change
// arr = new int[]{4,5,6}; // COMPILE ERROR — the REFERENCE itself cannot be reassigned
```

```java
final StringBuilder sb = new StringBuilder("Hello");
sb.append(" World");       // OK — mutating the object through the reference is fine
// sb = new StringBuilder(); // COMPILE ERROR — cannot point "sb" at a different object
```

So `final` guarantees the **variable always points to the same object** (or always holds the same primitive value) — it says nothing about whether that object's own internal state can be mutated. (See the encapsulation notes for how true immutability requires more than just `final` fields.)

### 8.3 Why use `final` on a variable

- Declaring **constants** that should never change (almost always combined with `static` — section 11).
- Documenting intent: a variable that is assigned once and never meant to be reassigned, catching accidental reassignment at compile time.
- Required for variables captured by lambdas or anonymous classes — they must be `final` or **effectively final** (never reassigned after their one and only assignment, even without the keyword).

```java
int base = 10;   // effectively final — never reassigned after this
Runnable r = () -> System.out.println(base);   // can be captured because it's effectively final
```

### 8.4 `final` method parameters

```java
void process(final int value) {
    // value = value + 1;   // COMPILE ERROR: cannot reassign a final parameter
    System.out.println(value);
}
```

`final` parameters are less common day-to-day, but prevent accidental reassignment of the parameter variable inside the method body — useful for clarity in longer methods.

---

## 9. `final` Methods

A `final` method **cannot be overridden** by any subclass. Any subclass attempting to redefine it with the same signature fails to compile.

```java
class Animal {
    final void breathe() {
        System.out.println("Breathing");
    }
}

class Dog extends Animal {
    // void breathe() { }   // COMPILE ERROR: cannot override the final method breathe() in Animal
}
```

### 9.1 Why mark a method `final`

- **Locking in critical behavior** that must stay exactly as written, for correctness or security reasons — subclasses should never be able to subtly change it.
- **Performance** — historically, the JVM could more aggressively inline a `final` method since it knows no override could ever exist, though modern JIT compilers often achieve similar optimizations even without `final` via other analysis.
- Methods called from a constructor are a classic candidate for `final`, since calling an **overridable** method from a constructor is risky (see the constructors notes) — marking it `final` removes that risk entirely by guaranteeing no subclass override can run partway through construction.

```java
class Vehicle {
    Vehicle() {
        init();       // calling an overridable method from a constructor is normally risky...
    }
    final void init() {   // ...but marking it final removes the risk: no subclass can override it
        System.out.println("Vehicle initialized");
    }
}
```

### 9.2 A `final` method can still be inherited and called normally

`final` only blocks **overriding** — the method is still fully inherited and usable by subclasses exactly as written.

```java
Dog d = new Dog();
d.breathe();   // "Breathing" — inherited and callable, just never overridable
```

---

## 10. `final` Classes

A `final` class **cannot be extended** by any other class — `extends FinalClass` is a compile error, full stop.

```java
final class ImmutablePoint {
    final int x, y;
    ImmutablePoint(int x, int y) { this.x = x; this.y = y; }
}

// class Point3D extends ImmutablePoint { }   // COMPILE ERROR: cannot inherit from final class ImmutablePoint
```

### 10.1 Why mark a class `final`

- **Enforcing immutability** — if a class can be subclassed, a subclass could add mutable state or override methods in ways that break the immutability guarantee; sealing the class off with `final` closes that door entirely (this is exactly why `String` is `final` — see the encapsulation notes).
- **Security** — preventing a malicious or careless subclass from overriding methods to change trusted behavior.
- **API stability** — signaling clearly that this class's behavior is complete and not meant to be extended or specialized further.

```java
public final class String {   // the real java.lang.String declaration (conceptually)
    // cannot be subclassed into some "EvilString" that breaks assumptions everywhere
}
```

### 10.2 A `final` class can still have `final` methods — it's redundant, but legal

```java
final class Utility {
    final void helper() { }   // the "final" on the method is redundant here, since the whole class can't be subclassed anyway
}
```

### 10.3 A `final` class can have non-final fields and methods just fine

`final` on the class only blocks **inheritance of the class itself**; it says nothing about whether the class's own fields can be reassigned or its own instances mutated.

```java
final class Counter {
    int count = 0;         // perfectly mutable field — "final" here is about inheritance, not mutability
    void increment() { count++; }
}
```

---

## 11. `static final` Combined

`static` and `final` are frequently combined to declare a **true constant**: one shared value, for the entire class, that never changes.

```java
class PhysicsConstants {
    static final double GRAVITY = 9.8;
    static final String UNIT = "m/s^2";
}
```

```java
System.out.println(PhysicsConstants.GRAVITY);   // 9.8
// PhysicsConstants.GRAVITY = 10.0;               // COMPILE ERROR: final, cannot reassign
```

### 11.1 Naming convention

Constants declared `static final` are conventionally named in **ALL_CAPS with underscores**, by long-standing Java convention (not a compiler rule, but followed almost universally):

```java
static final int MAX_CONNECTIONS = 100;
static final String DEFAULT_NAME = "Guest";
```

### 11.2 Interface fields are implicitly `public static final`

As covered in the abstraction notes, **every** field declared in an interface is automatically `public static final`, whether or not you write those keywords — this is exactly the `static final` pattern applied at the language level.

```java
interface Config {
    int MAX_USERS = 100;    // implicitly: public static final int MAX_USERS = 100;
}
```

---

## 12. Common Pitfall: Static Methods Can't Be Overridden, Only Hidden

This is one of the single most commonly misunderstood rules in Java, and it directly connects `static` back to the polymorphism and inheritance notes.

A `static` method in a subclass with the same signature as one in the superclass does **not** override it — it **hides** it. The key practical difference: **which version runs is decided at compile time, based on the reference's declared type** — never at runtime, and never based on the object's actual type.

```java
class Animal {
    static void info() { System.out.println("Animal.info()"); }
}

class Dog extends Animal {
    static void info() { System.out.println("Dog.info()"); }   // HIDES Animal's version, does not override it
}
```

```java
Animal a = new Dog();     // reference type Animal, actual object Dog
a.info();                 // "Animal.info()"  <-- decided by the REFERENCE TYPE, not the real object!

Dog.info();                // "Dog.info()"
Animal.info();             // "Animal.info()"
```

Compare this directly against a real (instance-method) override, where the exact same setup produces the **opposite** result:

```java
class Animal {
    void makeSound() { System.out.println("Animal sound"); }
}
class Dog extends Animal {
    @Override void makeSound() { System.out.println("Woof"); }   // a TRUE override — instance method
}

Animal a = new Dog();
a.makeSound();   // "Woof" — decided by the OBJECT's real type (Dog), via dynamic dispatch
```

### 12.1 Why this happens

- Polymorphism (dynamic dispatch) exists specifically for **instance** methods, resolved per-object at runtime via each object's method table (see the polymorphism notes).
- A `static` method, by definition, has **no connection to any particular object** — it belongs to the class. So there is no "object's real type" to dispatch to; the compiler simply looks at the **declared type of the reference** used to make the call and resolves it then and there.

### 12.2 `@Override` will catch this mistake for you

If you accidentally write `@Override` above a `static` method intending to override a parent's instance method (or vice versa), the compiler flags it immediately, since `@Override` verifies a real, valid override actually exists.

```java
class Animal {
    static void info() { }
}
class Dog extends Animal {
    @Override
    static void info() { }   // COMPILE ERROR: static methods cannot be annotated with @Override — nothing is being "overridden"
}
```

### 12.3 Practical takeaway

```text
Call a static method through a variable?   -> resolved by the VARIABLE'S DECLARED TYPE (compile time)
Call an instance method through a variable? -> resolved by the OBJECT'S ACTUAL TYPE (runtime, dynamic dispatch)
```

Because of this, **always call static methods through the class name**, not through an instance reference — it both avoids the misleading syntax and makes this hiding-vs-overriding distinction a non-issue in your own code (see section 2.3 and 3, which make the same recommendation).

---

## 13. Common Pitfalls

| # | Pitfall | Fix |
|---|---------|-----|
| 1 | Calling a static member through an instance reference (`obj.staticMember`) | Works, but misleading — always prefer `ClassName.staticMember` |
| 2 | Trying to access an instance field/method from inside a static method | Not allowed — a static method has no `this` and no connection to any particular object |
| 3 | Assuming a static block runs once per object | It runs exactly once, when the class is first loaded — not per instance |
| 4 | Confusing a static nested class with a static block | A static block runs code once; a static nested class defines a type you instantiate on demand |
| 5 | Assuming `final` on a reference variable makes the object itself immutable | `final` only locks the reference/value; the object's own internal state can still change unless the class itself is designed to be immutable |
| 6 | Reassigning a `final` local variable along some code path | Every path must assign it exactly once — reassigning (even conditionally, if a path already assigned it) fails to compile |
| 7 | Trying to override a `final` method | Not allowed — `final` methods are locked against overriding, by design |
| 8 | Trying to extend a `final` class | Not allowed — `final` classes cannot be subclassed at all |
| 9 | Assuming a `static` method in a subclass overrides the parent's `static` method | It hides it instead; resolved by reference type at compile time, not by the object's real type |
| 10 | Annotating a hiding `static` method with `@Override` | Compile error — `@Override` is only valid for true (instance-method) overrides |
| 11 | Thinking `static` members don't exist until an object is created | Static members exist (and static blocks run) from the moment the class is loaded, even with zero objects ever instantiated |
| 12 | Forgetting the ALL_CAPS naming convention for `static final` constants | Not a compiler rule, but near-universal convention — follow it for readability |

---

## 14. Interview Questions

**Q1. What is the difference between a static and an instance variable?**
An instance variable has a separate copy per object — each object maintains its own value. A static variable has exactly one copy, shared by the class and all its instances, and it exists even before any object is created.

---

**Q2. Why can't a static method access instance fields or call `this`?**
A static method belongs to the class, not to any particular object, and could be called with zero objects ever created. Since there's no guaranteed object to read instance state from, the compiler forbids static methods from referencing instance members directly.

---

**Q3. When does a static block run, and how many times?**
A static block runs exactly once, automatically, the first time the JVM loads the class — before any object is created, regardless of how many (or how few) objects are ultimately instantiated.

---

**Q4. What is a static nested class, and how is it different from a non-static inner class?**
A static nested class is a class declared `static` inside another class; it behaves like a normal top-level class namespaced inside the enclosing class, and does not hold an implicit reference to any enclosing instance. A non-static inner class, by contrast, is tied to a specific enclosing instance and requires one to be constructed (`outer.new Inner()`).

---

**Q5. What does `final` mean when applied to a variable, a method, and a class, respectively?**
On a variable, it means the variable can be assigned only once (locking the reference/value, not necessarily the referenced object's internal state). On a method, it means the method cannot be overridden by any subclass. On a class, it means the class cannot be extended/subclassed at all.

---

**Q6. Does `final` on a reference-type variable make the object it points to immutable?**
No. `final` only prevents the variable from being reassigned to point at a different object. The object's own fields can still be freely modified through that same reference, unless the object's class was specifically designed to be immutable (private final fields, no setters, defensive copying).

---

**Q7. Why might a class be declared `final`?**
Commonly to enforce immutability (preventing a subclass from adding mutable state or overriding methods in ways that break the immutability contract), for security (preventing trusted behavior from being overridden), or simply to signal that the class's design is complete and not meant to be extended.

---

**Q8. Can a `final` method still be called by subclasses?**
Yes. `final` only blocks overriding — the method is still fully inherited and callable by subclasses exactly as the superclass wrote it.

---

**Q9. Why are `static` and `final` so often combined, and what naming convention goes with it?**
`static final` together define a true constant: one shared value for the entire class that can never change. By long-standing convention, such constants are named in ALL_CAPS with underscores (e.g., `MAX_USERS`), though this is a style convention, not a compiler-enforced rule.

---

**Q10. Why can a `static` method not be overridden, only hidden — and how do you tell the difference in practice?**
A `static` method has no connection to a particular object, so there's no "real object type" for dynamic dispatch to use. A same-signature `static` method in a subclass is resolved at compile time, based on the reference's declared type — this is hiding, not overriding. In practice: calling it through a variable typed as the parent class always runs the parent's version, even if the object is really an instance of the subclass — the exact opposite of what happens with a true (instance-method) override.

---

## 15. Interview-Style Output Questions

**Q1**
```java
class Counter {
    static int count = 0;
    Counter() { count++; }
}
new Counter();
new Counter();
new Counter();
System.out.println(Counter.count);
```
**Answer:** `3`. One shared static field, incremented by every constructor call.

---

**Q2**
```java
class Demo {
    static { System.out.println("static block"); }
    Demo() { System.out.println("constructor"); }
}
System.out.println("before");
new Demo();
new Demo();
```
**Answer:**
```
before
static block
constructor
constructor
```
(The static block runs once, triggered by the first use of `Demo`, before any constructor call.)

---

**Q3**
```java
final int[] arr = {1, 2, 3};
arr[1] = 99;
System.out.println(arr[1]);
```
**Answer:** `99`. `final` locks the reference `arr`, not the array's contents.

---

**Q4**
```java
final int x;
x = 5;
System.out.println(x);
x = 10;
```
**Answer:** Compile error on the last line — `x` is `final` and was already assigned once; it cannot be reassigned.

---

**Q5**
```java
class Animal {
    static void speak() { System.out.println("Animal"); }
}
class Dog extends Animal {
    static void speak() { System.out.println("Dog"); }
}
Animal a = new Dog();
a.speak();
```
**Answer:** `Animal`. `static` methods are hidden, not overridden — resolved by the reference's declared type (`Animal`), not the object's real type.

---

**Q6**
```java
class Animal {
    void speak() { System.out.println("Animal"); }
}
class Dog extends Animal {
    @Override void speak() { System.out.println("Dog"); }
}
Animal a = new Dog();
a.speak();
```
**Answer:** `Dog`. A true (instance-method) override — resolved by the object's actual type at runtime via dynamic dispatch. (Contrast directly with Q5.)

---

**Q7**
```java
final class Base { }
class Derived extends Base { }
```
**Answer:** Compile error — `Base` is `final` and cannot be extended.

---

**Q8**
```java
class Outer {
    static int value = 42;
    static class Nested {
        void show() { System.out.println(value); }
    }
}
Outer.Nested n = new Outer.Nested();
n.show();
```
**Answer:** `42`. A static nested class can access the enclosing class's static members, and does not require an `Outer` instance to be constructed.

---

## 16. Quick Cheat Sheet

```text
STATIC VARIABLE      ONE copy shared by the whole class (not per object); exists before any object
STATIC METHOD        Belongs to the class; no "this"; cannot touch instance fields/methods directly;
                      callable with zero objects ever created
STATIC BLOCK          static { ... }  — runs CODE exactly once, at class-loading time
STATIC NESTED CLASS   static class Name { ... }  — defines a TYPE, no implicit link to an enclosing instance,
                      instantiated with new Outer.Nested()

FINAL VARIABLE        Assigned exactly once; locks the REFERENCE/VALUE, not the referenced object's own state
FINAL METHOD          Cannot be overridden by any subclass (still inherited and callable, just not overridable)
FINAL CLASS           Cannot be extended/subclassed at all

STATIC FINAL          A true constant: one shared, unchangeable value for the whole class
                      Convention: ALL_CAPS_WITH_UNDERSCORES
                      Interface fields are implicitly public static final

STATIC METHOD         Resolved by REFERENCE'S DECLARED TYPE, at COMPILE TIME  -> HIDING, not overriding
  vs                   @Override is INVALID on a static "override" attempt
INSTANCE METHOD       Resolved by OBJECT'S ACTUAL TYPE, at RUNTIME            -> true OVERRIDING (polymorphism)
OVERRIDE
```

**Remember:**

1. `static` means "belongs to the class, one shared copy"; it exists and runs independent of any particular object.
2. A static block runs once, at class load; a static nested class is a type you instantiate whenever you want, however many times.
3. `final` on a variable locks the reference, not the referenced object's mutability — `final int[] arr` still lets you change `arr`'s elements.
4. `final` methods can't be overridden; `final` classes can't be extended — both are permanent, compiler-enforced locks.
5. The single most important static-related pitfall: static methods are **hidden**, resolved by reference type at compile time — they are never truly overridden, and polymorphism (dynamic dispatch) never applies to them.
6. Always call static members through the class name, not an instance, to avoid confusion and keep the static-vs-instance distinction crystal clear in your own code.
