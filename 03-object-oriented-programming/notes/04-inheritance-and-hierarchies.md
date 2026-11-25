# Java Inheritance: Detailed Notes

**Inheritance** is the pillar of OOP that lets one class **acquire the fields and methods** of another class, so common behavior can be written once and reused — modeled on real-world **is-a** relationships like "a Dog **is an** Animal."

```java
class Animal {
    void eat() { System.out.println("This animal eats food"); }
}

class Dog extends Animal {
    void bark() { System.out.println("Woof!"); }
}

Dog d = new Dog();
d.eat();    // inherited from Animal
d.bark();   // defined in Dog
```

---

## Table of Contents

1. [The `extends` Keyword](#1-the-extends-keyword)
2. [Single Inheritance in Java](#2-single-inheritance-in-java)
3. [What Gets Inherited (and What Doesn't)](#3-what-gets-inherited-and-what-doesnt)
4. [The `super` Keyword](#4-the-super-keyword)
5. [`super(...)` — Calling the Parent Constructor](#5-super---calling-the-parent-constructor)
6. [`super.method()` — Calling a Parent Method](#6-supermethod---calling-a-parent-method)
7. [`super.field` — Accessing a Shadowed Field](#7-superfield---accessing-a-shadowed-field)
8. [Method Overriding Basics](#8-method-overriding-basics)
9. [Rules for Valid Overriding](#9-rules-for-valid-overriding)
10. [Overriding vs Overloading](#10-overriding-vs-overloading)
11. [The `Object` Class: Root of All Classes](#11-the-object-class-root-of-all-classes)
12. [Why Java Doesn't Support Multiple Class Inheritance](#12-why-java-doesnt-support-multiple-class-inheritance)
13. [How Interfaces Fill the Gap](#13-how-interfaces-fill-the-gap)
14. [Common Pitfalls](#14-common-pitfalls)
15. [Interview Questions](#15-interview-questions)
16. [Interview-Style Output Questions](#16-interview-style-output-questions)
17. [Quick Cheat Sheet](#17-quick-cheat-sheet)

---

## 1. The `extends` Keyword

A class uses `extends` to inherit from another class. The class being inherited from is the **superclass** (parent/base class); the class doing the inheriting is the **subclass** (child/derived class).

```java
class Vehicle {
    int speed;
    void move() { System.out.println("Vehicle is moving"); }
}

class Car extends Vehicle {   // Car IS-A Vehicle
    void honk() { System.out.println("Beep beep!"); }
}
```

```java
Car c = new Car();
c.speed = 100;    // inherited field
c.move();         // inherited method
c.honk();         // Car's own method
```

### 1.1 Terminology

| Term | Meaning |
|------|---------|
| Superclass / parent / base class | The class being extended (`Vehicle`) |
| Subclass / child / derived class | The class using `extends` (`Car`) |
| `extends` | The keyword that establishes the relationship |
| IS-A relationship | The conceptual rule: a `Car` IS-A `Vehicle` |

### 1.2 A class that doesn't explicitly extend anything still inherits from `Object`

```java
class Animal {   // implicitly: class Animal extends Object
}
```

Every class in Java, whether it says so or not, ultimately extends `Object` (see section 11).

---

## 2. Single Inheritance in Java

Java allows a class to `extends` **only one** other class. This is called **single inheritance** at the class level.

```java
class A { }
class B { }

class C extends A, B { }   // COMPILE ERROR: Java does not allow extending two classes
```

```java
class A { }
class B extends A { }       // OK — B extends exactly one class
class C extends B { }       // OK — multi-level inheritance is fine; this is NOT multiple inheritance
```

**Multi-level inheritance** (a chain: `A → B → C`) is completely different from **multiple inheritance** (one class trying to extend two classes directly) — Java allows the former and forbids the latter.

```text
Multi-level (ALLOWED):        Multiple (NOT ALLOWED):
    A                              A       B
    |                               \     /
    B                                \   /
    |                                 C
    C                          (C cannot extend both A and B)
```

### 2.1 Why single inheritance for classes?

Java's designers deliberately chose single inheritance for classes to avoid the ambiguity problems that come with multiple inheritance (explained fully in section 12) — most famously the **Diamond Problem**.

---

## 3. What Gets Inherited (and What Doesn't)

A subclass inherits:

```text
public and protected members of the superclass
default (package-private) members, IF the subclass is in the same package
```

A subclass does **NOT** inherit:

```text
private members of the superclass
constructors of the superclass
static members are not "inherited" in the polymorphic sense — they're
  inherited as accessible, but belong to the class, not overridden per-instance
```

```java
class Animal {
    private String secret = "hidden";   // NOT inherited (not accessible in Dog)
    protected String name = "Animal";   // inherited

    Animal() { System.out.println("Animal constructor"); }   // NOT inherited
}

class Dog extends Animal {
    void test() {
        System.out.println(name);        // OK — inherited, protected
        // System.out.println(secret);   // COMPILE ERROR — private, not accessible
    }
}
```

Constructors are never inherited; every subclass must define its own constructor(s), and each one reaches a superclass constructor via `super(...)` (explicit or implicit) — see section 5.

---

## 4. The `super` Keyword

`super` refers to the **immediate parent class** from within a subclass. It has three main uses:

```text
super(...)        — call a parent constructor
super.method()    — call a parent's (possibly overridden) method
super.field       — access a parent's field, usually when shadowed by the subclass
```

---

## 5. `super(...)` — Calling the Parent Constructor

Every constructor's **first action**, whether written or not, is a call to a superclass constructor.

```java
class Animal {
    String name;
    Animal(String name) {
        this.name = name;
        System.out.println("Animal constructor: " + name);
    }
}

class Dog extends Animal {
    Dog(String name) {
        super(name);                      // explicitly calls Animal(String)
        System.out.println("Dog constructor");
    }
}

new Dog("Rex");
// Animal constructor: Rex
// Dog constructor
```

### 5.1 If you don't write `super(...)`, Java inserts an implicit `super()`

```java
class Animal {
    Animal() { System.out.println("Animal()"); }   // no-arg constructor exists
}

class Dog extends Animal {
    Dog() {
        // implicit super(); happens here automatically
        System.out.println("Dog()");
    }
}
```

### 5.2 If the parent has no no-arg constructor, `super(...)` becomes mandatory

```java
class Animal {
    String name;
    Animal(String name) { this.name = name; }   // only a parameterized constructor exists
}

class Dog extends Animal {
    Dog() {
        // COMPILE ERROR: implicit super() looks for Animal(), which doesn't exist
    }
}
```

```java
class Dog extends Animal {
    Dog() {
        super("Unnamed");    // must explicitly supply matching arguments
    }
}
```

### 5.3 `super(...)` rules

- Must be the **first statement** in the constructor, if written at all.
- Only **one** `super(...)` call is allowed.
- Cannot be combined with `this(...)` in the same constructor — only one "first statement" slot exists.

---

## 6. `super.method()` — Calling a Parent Method

When a subclass **overrides** a method, it can still call the parent's original version through `super.method()`.

```java
class Animal {
    void makeSound() {
        System.out.println("Animal makes a sound");
    }
}

class Dog extends Animal {
    @Override
    void makeSound() {
        super.makeSound();                // calls Animal's version first
        System.out.println("Dog barks");
    }
}

new Dog().makeSound();
// Animal makes a sound
// Dog barks
```

This is especially useful when the subclass wants to **extend** the parent's behavior rather than fully **replace** it.

```java
class Employee {
    double calculateSalary() { return 30000; }
}

class Manager extends Employee {
    @Override
    double calculateSalary() {
        return super.calculateSalary() + 10000;   // base salary + bonus
    }
}
```

---

## 7. `super.field` — Accessing a Shadowed Field

If a subclass declares a field with the **same name** as a field in the parent, the subclass's field **shadows** (hides) the parent's — both still exist, but the plain name now refers to the subclass's version. `super.field` reaches the parent's copy explicitly.

```java
class Animal {
    String type = "Animal";
}

class Dog extends Animal {
    String type = "Dog";    // shadows Animal's "type"

    void printTypes() {
        System.out.println(type);         // "Dog"       — this class's field
        System.out.println(super.type);   // "Animal"    — parent's field
    }
}
```

> Field shadowing is resolved at **compile time** based on the reference's declared type, unlike method overriding, which is resolved at **runtime** based on the actual object type. This is a key difference covered further when studying polymorphism.

---

## 8. Method Overriding Basics

**Overriding** happens when a subclass provides its **own implementation** of a method that is already defined in its superclass, using the **same signature** (name + parameter types).

```java
class Animal {
    void makeSound() {
        System.out.println("Some generic animal sound");
    }
}

class Cat extends Animal {
    @Override
    void makeSound() {
        System.out.println("Meow");
    }
}

Animal a = new Cat();
a.makeSound();    // "Meow" — the overridden version runs, even though the reference type is Animal
```

This last line is the heart of **runtime polymorphism**: the method that actually executes depends on the object's **real type** (`Cat`), not the type of the variable holding it (`Animal`). The deeper mechanics and implications of this are covered fully when studying polymorphism — this section only covers the basic mechanics of *writing* an override correctly.

### 8.1 The `@Override` annotation

`@Override` is optional but strongly recommended. It tells the compiler "I intend this to override a parent method," and the compiler then **verifies** that an actual matching method exists in a superclass.

```java
class Animal {
    void makeSound() { }
}

class Dog extends Animal {
    @Override
    void makeSond() {    // typo! Doesn't actually override anything
    }
}
// COMPILE ERROR: method does not override a method from its superclass
```

Without `@Override`, that typo would silently compile as a brand-new, unrelated method — a classic hard-to-spot bug. `@Override` turns it into an immediate compiler error.

---

## 9. Rules for Valid Overriding

For a subclass method to be a valid override of a parent method, it must satisfy all of these:

1. **Same method name.**
2. **Same parameter list** (number, types, and order) — this is what distinguishes overriding from overloading.
3. **Same return type, or a covariant return type** (a subtype of the original return type — allowed since Java 5).
4. **Access modifier cannot be more restrictive** — it can stay the same or become **less** restrictive, never more.
5. **Cannot throw new or broader checked exceptions** than the parent method (can throw the same, narrower, fewer, or none — see the constructors/exceptions notes for the full rule).
6. **Cannot override a `final`, `static`, or `private` method** (see section 9.1–9.3).

```java
class Animal {
    protected Animal reproduce() { return new Animal(); }
}

class Dog extends Animal {
    @Override
    public Dog reproduce() {     // covariant return type (Dog is-a Animal) + widened access (public > protected)
        return new Dog();
    }
}
```

```java
class Animal {
    public void eat() { }
}

class Dog extends Animal {
    @Override
    protected void eat() { }     // COMPILE ERROR: cannot reduce visibility from public to protected
}
```

### 9.1 `final` methods cannot be overridden

```java
class Animal {
    final void breathe() { System.out.println("Breathing"); }
}

class Dog extends Animal {
    // void breathe() { }   // COMPILE ERROR: cannot override final method
}
```

`final` on a method is a deliberate way to **lock in** behavior that subclasses must not be allowed to change.

### 9.2 `static` methods are not overridden — they are hidden

A `static` method in a subclass with the same signature as one in the parent does **not** override it; it **hides** it. Which version runs is decided at **compile time**, based on the reference type, not the object's real type.

```java
class Animal {
    static void info() { System.out.println("Animal.info()"); }
}

class Dog extends Animal {
    static void info() { System.out.println("Dog.info()"); }   // hides, does not override
}

Animal a = new Dog();
a.info();          // "Animal.info()" — decided by reference TYPE (Animal), not the real object
Dog.info();        // "Dog.info()"
```

### 9.3 `private` methods cannot be overridden

A `private` method isn't even visible to the subclass, so there's nothing to override. A subclass method with the same name and signature is simply a **new, unrelated method**.

---

## 10. Overriding vs Overloading

These two are frequently confused — they are entirely different mechanisms.

| | Overriding | Overloading |
|---|------------|--------------|
| Relationship | Between superclass and subclass | Within the same class (or, same class via inheritance) |
| Parameter list | Must be **identical** | Must **differ** (number, type, or order) |
| Return type | Same or covariant | Can be anything |
| Resolved | At **runtime** (dynamic/late binding) | At **compile time** (static/early binding) |
| Purpose | Runtime polymorphism — different behavior per object type | Multiple ways to call conceptually similar operations |
| `@Override` applies? | Yes | No |

```java
class Calculator {
    int add(int a, int b) { return a + b; }           // overload 1
    double add(double a, double b) { return a + b; }  // overload 2 — same class, different params
}

class Animal {
    void makeSound() { System.out.println("..."); }
}
class Dog extends Animal {
    @Override
    void makeSound() { System.out.println("Woof"); }   // override — same signature, subclass
}
```

---

## 11. The `Object` Class: Root of All Classes

Every class in Java, directly or indirectly, extends `java.lang.Object` — it is the single root of the entire class hierarchy.

```java
class Animal {        // implicitly: class Animal extends Object
}
```

```text
                 Object
                    |
                 Animal
                    |
                  Dog
```

Because of this, **every** object in Java — arrays included — automatically has access to the methods `Object` defines, whether or not the class explicitly overrides them.

### 11.1 Key methods defined in `Object`

| Method | Purpose |
|--------|---------|
| `toString()` | Returns a string representation of the object. Default: `ClassName@hashCodeInHex` |
| `equals(Object obj)` | Compares objects for equality. Default: reference equality (`==`) |
| `hashCode()` | Returns an integer hash code, used by hash-based collections |
| `getClass()` | Returns the runtime `Class` object representing the object's actual type |
| `clone()` | Creates and returns a copy of the object (requires implementing `Cloneable`) |
| `finalize()` | Called by the garbage collector before reclaiming the object (deprecated, rarely used today) |
| `wait()`, `notify()`, `notifyAll()` | Used for thread synchronization and inter-thread communication |

```java
class Student {
    String name;
    Student(String name) { this.name = name; }
}

Student s = new Student("Aashish");
System.out.println(s.toString());     // Student@<hashcode>  (default Object behavior)
System.out.println(s.getClass().getName());   // Student
```

### 11.2 Overriding `Object`'s methods is common and expected

```java
class Student {
    String name;
    Student(String name) { this.name = name; }

    @Override
    public String toString() {
        return "Student(name=" + name + ")";
    }
}

Student s = new Student("Aashish");
System.out.println(s);        // Student(name=Aashish)   — toString() is called automatically by println
```

Overriding `equals()` and `hashCode()` together (never just one) is one of the most common real-world uses of overriding, and is essential for using custom objects correctly as keys in `HashMap`/`HashSet` or as elements compared for equality.

### 11.3 Reference variables of type `Object`

Because every class is-a `Object`, a variable of type `Object` can hold a reference to **any** object at all.

```java
Object o1 = "a string";
Object o2 = 42;              // autoboxed to Integer
Object o3 = new Student("X");
```

This is the foundation that makes things like heterogeneous collections (pre-generics) and reflection possible, though generics are the modern, type-safe preferred approach for most everyday code.

---

## 12. Why Java Doesn't Support Multiple Class Inheritance

"Multiple inheritance" means a class directly inheriting from **more than one** class. Java deliberately forbids this for classes (section 2), primarily to avoid the **Diamond Problem**.

### 12.1 The Diamond Problem

```text
        A
       / \
      B   C
       \ /
        D
```

Suppose classes `B` and `C` both inherit from `A` and both **override** the same method differently, and then `D` tries to inherit from both `B` and `C`:

```java
class A {
    void greet() { System.out.println("A"); }
}
class B extends A {
    @Override void greet() { System.out.println("B"); }
}
class C extends A {
    @Override void greet() { System.out.println("C"); }
}
// class D extends B, C { }   // if this were legal...
// D d = new D();
// d.greet();                  // ... which version runs? B's? C's? Ambiguous!
```

If `D` called `greet()`, the compiler would have no unambiguous way to decide whether to use `B`'s version or `C`'s version. This ambiguity — inherited **state** and **behavior** conflicts from two parallel parent branches — is exactly what the Diamond Problem describes.

### 12.2 Other reasons Java avoids multiple class inheritance

- **Field conflicts:** if both parents declare a field with the same name, which one does the child "own"?
- **Constructor ambiguity:** which parent constructor(s) would run, and in what order?
- **Increased complexity:** languages that do support it (like C++) require extra rules (e.g., virtual inheritance) specifically to manage these conflicts — Java's designers chose simplicity instead.

Java's answer: **a class can extend only one class**, keeping the inheritance hierarchy a clean tree rather than a tangled graph.

---

## 13. How Interfaces Fill the Gap

Java still wants the benefits of "inheriting multiple behaviors" — it just avoids the *state*-conflict problems of multiple **class** inheritance. The solution: a class can `implements` **multiple interfaces**, because interfaces (traditionally) declare **no state** and no conflicting constructors.

```java
interface Flyable {
    void fly();
}

interface Swimmable {
    void swim();
}

class Duck implements Flyable, Swimmable {   // multiple interfaces — perfectly legal
    @Override
    public void fly() { System.out.println("Duck flies"); }

    @Override
    public void swim() { System.out.println("Duck swims"); }
}
```

```java
Duck d = new Duck();
d.fly();    // Duck flies
d.swim();   // Duck swims
```

### 13.1 Why interfaces avoid the Diamond Problem

- Interfaces traditionally have **no instance fields** — so there's no field-conflict version of the Diamond Problem.
- Even with Java 8+ **default methods** (methods with a body inside an interface), if a class implements two interfaces with a **conflicting default method**, Java does **not** guess — it forces the implementing class to **explicitly resolve** the conflict by overriding the method itself.

```java
interface A {
    default void greet() { System.out.println("A"); }
}
interface B {
    default void greet() { System.out.println("B"); }
}

class C implements A, B {
    // COMPILE ERROR if greet() is not overridden here — Java refuses to guess
    @Override
    public void greet() {
        A.super.greet();     // explicitly choose A's version (or B's, or write new logic)
    }
}
```

This is the key difference from the class Diamond Problem: Java **forces an explicit decision** at compile time rather than silently picking one parent or failing unpredictably at runtime.

### 13.2 Summary: classes vs interfaces for "multiple inheritance"

| | Classes (`extends`) | Interfaces (`implements`) |
|---|----------------------|------------------------------|
| How many can be combined? | Only **one** | **Multiple**, freely |
| Carries state (instance fields)? | Yes | No (traditionally) |
| Diamond Problem risk? | High — Java forbids multiple inheritance to avoid it entirely | Low — default-method conflicts must be explicitly resolved by the programmer |
| Purpose | Share implementation and state ("IS-A" with shared behavior) | Share a **contract**/capability ("CAN-DO", e.g. `Flyable`, `Comparable`) |

> In short: Java gives up multiple inheritance of **state and implementation** (classes) to stay simple and unambiguous, but still allows multiple inheritance of **type and behavior contracts** (interfaces), with explicit, compiler-enforced conflict resolution when default methods collide.

---

## 14. Common Pitfalls

| # | Pitfall | Fix |
|---|---------|-----|
| 1 | Trying to `extends` two classes | Not allowed — Java supports single inheritance for classes only |
| 2 | Confusing multi-level inheritance with multiple inheritance | A chain (`A → B → C`) is fine; one class extending two parents directly is not |
| 3 | Assuming a subclass inherits the parent's constructors | Constructors are never inherited; every subclass writes its own and calls `super(...)` |
| 4 | Forgetting `super(...)` must be the first statement | Compile error if placed anywhere else |
| 5 | Omitting `@Override` and introducing a typo | The "override" silently becomes a new, unrelated method — always use `@Override` |
| 6 | Trying to override a `final` method | Not allowed — `final` methods are locked against overriding |
| 7 | Thinking a `static` method can be overridden | `static` methods are hidden, not overridden, and are resolved by reference type at compile time |
| 8 | Narrowing the access modifier in an override | An override can only keep the same or widen access, never narrow it |
| 9 | Widening the checked exceptions in an override | An override can only keep the same, narrow, or drop checked exceptions, never add broader ones |
| 10 | Overriding `equals()` without also overriding `hashCode()` | Always override them together, or hash-based collections (`HashMap`/`HashSet`) can misbehave |
| 11 | Assuming interfaces can never conflict | Default methods from two interfaces with the same signature force the implementing class to resolve the conflict explicitly |
| 12 | Confusing field shadowing with method overriding | Shadowed fields resolve by the reference's declared type at compile time; overridden methods resolve by the real object type at runtime |

---

## 15. Interview Questions

**Q1. What is inheritance, and what keyword enables it in Java?**
Inheritance lets a class acquire the fields and methods of another class, modeling an IS-A relationship. It is enabled with the `extends` keyword (for classes) or `implements` (for interfaces).

---

**Q2. Does Java support multiple inheritance for classes?**
No. A class can `extends` only one other class (single inheritance). Java avoids multiple class inheritance to prevent ambiguity problems like the Diamond Problem — conflicting fields, methods, and constructors inherited from two parallel parent branches.

---

**Q3. What is the difference between multi-level inheritance and multiple inheritance?**
Multi-level inheritance is a chain — `A → B → C`, where each class extends exactly one parent — and is fully supported in Java. Multiple inheritance is one class trying to extend two or more parents directly (`class D extends B, C`), which Java does not allow for classes.

---

**Q4. What are the three uses of the `super` keyword?**
`super(...)` to call a parent constructor, `super.method()` to call a parent's (possibly overridden) method, and `super.field` to access a parent's field when it has been shadowed by a field of the same name in the subclass.

---

**Q5. What happens if a constructor doesn't explicitly call `super(...)`?**
The compiler automatically inserts an implicit `super()` call to the parent's no-argument constructor, as the very first statement. If the parent has no no-arg constructor, this implicit call fails to compile, and the subclass must provide an explicit `super(...)` call with matching arguments.

---

**Q6. What is method overriding, and what rules must be followed?**
Overriding is providing a new implementation of an inherited method in a subclass, using the exact same signature. Rules: same name and parameters, same or covariant return type, access modifier can stay the same or widen (never narrow), and checked exceptions can stay the same, narrow, or be dropped (never widen). `final`, `static`, and `private` methods cannot be overridden.

---

**Q7. What is the `Object` class, and why does it matter?**
`Object` is the implicit root of every class hierarchy in Java — every class extends it directly or indirectly, even if `extends` is never written. It provides universally available methods like `toString()`, `equals()`, `hashCode()`, and `getClass()`, which classes are free to override.

---

**Q8. Why doesn't Java support multiple inheritance of classes, but does support it for interfaces?**
Multiple class inheritance risks the Diamond Problem: ambiguous, conflicting state and behavior inherited from two parent branches with a common ancestor. Interfaces traditionally carry no state, so there's no field-conflict version of this problem; and even with Java 8+ default methods, conflicting method signatures from two interfaces force the implementing class to explicitly resolve the conflict, rather than Java silently guessing.

---

**Q9. What's the difference between overriding and method hiding (with `static` methods)?**
Overriding applies to instance methods and is resolved at runtime based on the object's actual type (dynamic binding) — this is what enables polymorphism. Method hiding applies when a subclass defines a `static` method with the same signature as the parent's; it is resolved at compile time based on the reference's declared type, and does not participate in polymorphism.

---

**Q10. Why is it recommended to always use `@Override` when overriding a method?**
Without `@Override`, a typo in the method name or a mismatched parameter list silently creates a brand-new, unrelated method rather than failing to compile — a subtle, hard-to-detect bug. `@Override` makes the compiler verify that a matching method actually exists in a superclass, turning that mistake into an immediate compile error.

---

## 16. Interview-Style Output Questions

**Q1**
```java
class Animal {
    Animal() { System.out.println("Animal()"); }
}
class Dog extends Animal {
    Dog() { System.out.println("Dog()"); }
}
new Dog();
```
**Answer:**
```
Animal()
Dog()
```

---

**Q2**
```java
class Animal {
    void sound() { System.out.println("Some sound"); }
}
class Cat extends Animal {
    @Override
    void sound() {
        super.sound();
        System.out.println("Meow");
    }
}
new Cat().sound();
```
**Answer:**
```
Some sound
Meow
```

---

**Q3**
```java
class Parent {
    static void show() { System.out.println("Parent.show()"); }
}
class Child extends Parent {
    static void show() { System.out.println("Child.show()"); }
}
Parent p = new Child();
p.show();
```
**Answer:** `Parent.show()` — `static` methods are hidden, not overridden; resolved by the reference's declared type (`Parent`), not the actual object type.

---

**Q4**
```java
class Animal {
    String type = "Animal";
}
class Dog extends Animal {
    String type = "Dog";
}
Animal a = new Dog();
System.out.println(a.type);
```
**Answer:** `Animal` — field access (unlike method calls) is resolved by the reference's declared type at compile time, not the object's actual type.

---

**Q5**
```java
class Animal {
    final void breathe() { System.out.println("Breathing"); }
}
class Dog extends Animal {
    void breathe() { System.out.println("Dog breathing"); }
}
```
**Answer:** Compile error — `breathe()` is `final` in `Animal` and cannot be overridden in `Dog`.

---

**Q6**
```java
class Shape {
    double area() { return 0; }
}
class Circle extends Shape {
    @Override
    protected double area() { return 3.14; }
}
```
**Answer:** Compiles successfully. `Shape.area()` has default (package-private) access, and `Circle.area()` declares `protected`, which is **wider**, not narrower — widening access in an override is always allowed.

---

**Q7**
```java
interface A {
    default void greet() { System.out.println("A"); }
}
interface B {
    default void greet() { System.out.println("B"); }
}
class C implements A, B {
}
```
**Answer:** Compile error — `C` inherits conflicting default implementations of `greet()` from `A` and `B`, and must override `greet()` itself to resolve the ambiguity.

---

## 17. Quick Cheat Sheet

```text
EXTENDS             class Sub extends Super { }   — single class inheritance only (one parent max)
IS-A                Sub IS-A Super (conceptual relationship inheritance models)

MULTI-LEVEL         A -> B -> C          ALLOWED (a chain of single-parent links)
MULTIPLE            class D extends B, C   NOT ALLOWED for classes

SUPER               super(...)      first statement only, calls a parent constructor
                    super.method()  calls the parent's version of an overridden method
                    super.field     accesses a parent's field when shadowed by the subclass

NOT INHERITED       private members, constructors (subclass must define its own)

OVERRIDING          Same name + same params + same/covariant return type
                    Access: same or WIDER only (never narrower)
                    Checked exceptions: same, narrower, or none (never broader)
                    Cannot override: final, static (hidden instead), private (invisible, unrelated)
                    @Override lets the compiler verify it's a real override

OVERRIDE vs HIDE    instance method override -> resolved at RUNTIME (actual object type)
                    static method "override"  -> just HIDING, resolved at COMPILE TIME (reference type)
                    field "override"          -> just SHADOWING, resolved at COMPILE TIME (reference type)

OBJECT CLASS        Root of every class, implicit or explicit
                    Provides: toString(), equals(), hashCode(), getClass(), clone(), wait()/notify()
                    Override equals() and hashCode() TOGETHER, never just one

WHY NO MULTIPLE     Diamond Problem: ambiguous state/behavior from two parent branches
CLASS INHERITANCE   Field conflicts, constructor ambiguity, added language complexity

INTERFACES FILL IT  class X implements A, B, C { }   — many interfaces allowed
                    No state (traditionally) -> no field-conflict Diamond Problem
                    Conflicting default methods -> compiler FORCES explicit resolution
```

**Remember:**

1. `extends` is for single class inheritance; `implements` is for (potentially multiple) interfaces.
2. Multi-level chains are fine; a class extending two classes directly is not.
3. `super(...)` must be the constructor's first statement, explicit or implicit.
4. An override can only widen access and narrow/drop exceptions, never the reverse.
5. `static` methods are hidden (compile-time, by reference type), not overridden (runtime, by actual type) — polymorphism only applies to the latter.
6. Every class implicitly extends `Object`, inheriting `toString()`, `equals()`, `hashCode()`, and more.
7. Java forbids multiple class inheritance to dodge the Diamond Problem, but allows multiple interface implementation, forcing explicit resolution if default methods collide.
