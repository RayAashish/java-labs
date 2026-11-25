# Java Abstraction: Detailed Notes

**Abstraction** is the pillar of OOP concerned with **exposing only essential details while hiding implementation complexity**. Instead of showing *how* something works, a class or interface declares *what* it can do, and lets the actual mechanics live somewhere else — in a subclass, or in a concrete implementation.

```java
abstract class Shape {
    abstract double area();    // WHAT every shape can do — no HOW here
}

class Circle extends Shape {
    double radius;
    @Override double area() { return Math.PI * radius * radius; }   // the HOW
}
```

> Memory aid: you press the accelerator pedal without knowing how the engine internally works — the pedal is the abstraction; the engine is the hidden implementation.

Java gives you two tools for abstraction: **abstract classes** and **interfaces**.

---

## Table of Contents

1. [Abstract Classes](#1-abstract-classes)
2. [The `abstract` Keyword on Methods](#2-the-abstract-keyword-on-methods)
3. [When a Class Cannot Be Instantiated](#3-when-a-class-cannot-be-instantiated)
4. [What Abstract Classes CAN Have](#4-what-abstract-classes-can-have)
5. [Rules for Abstract Classes](#5-rules-for-abstract-classes)
6. [Interfaces: Pure Abstraction](#6-interfaces-pure-abstraction)
7. [Multiple Interface Implementation](#7-multiple-interface-implementation)
8. [Default Methods (Java 8+)](#8-default-methods-java-8)
9. [Static Methods in Interfaces (Java 8+)](#9-static-methods-in-interfaces-java-8)
10. [Resolving Default Method Conflicts](#10-resolving-default-method-conflicts)
11. [Functional Interfaces](#11-functional-interfaces)
12. [A Preview of Lambdas](#12-a-preview-of-lambdas)
13. [Common Built-in Functional Interfaces](#13-common-built-in-functional-interfaces)
14. [Abstract Class vs Interface: Feature Comparison](#14-abstract-class-vs-interface-feature-comparison)
15. [Abstract Class vs Interface: When to Choose Which](#15-abstract-class-vs-interface-when-to-choose-which)
16. [Common Pitfalls](#16-common-pitfalls)
17. [Interview Questions](#17-interview-questions)
18. [Interview-Style Output Questions](#18-interview-style-output-questions)
19. [Quick Cheat Sheet](#19-quick-cheat-sheet)

---

## 1. Abstract Classes

An **abstract class** is a class declared with the `abstract` keyword. It is meant to be a **partial blueprint** — it can define some complete behavior itself, but it **cannot be instantiated directly**, and it typically leaves some behavior for subclasses to fill in.

```java
abstract class Animal {
    String name;

    Animal(String name) {           // abstract classes CAN have constructors
        this.name = name;
    }

    void sleep() {                  // a regular, fully implemented method
        System.out.println(name + " is sleeping");
    }

    abstract void makeSound();      // an abstract method — no body, must be implemented by subclasses
}
```

```java
class Dog extends Animal {
    Dog(String name) { super(name); }

    @Override
    void makeSound() {
        System.out.println(name + " says Woof");
    }
}
```

```java
Dog d = new Dog("Rex");
d.sleep();        // inherited, fully implemented in Animal
d.makeSound();     // Dog's own implementation of the abstract method
```

---

## 2. The `abstract` Keyword on Methods

An **abstract method** is a method declared **without a body** — just a signature ending in a semicolon. It specifies *what* a subclass must be able to do, without saying *how*.

```java
abstract class Shape {
    abstract double area();           // no { } body at all — just a signature
    abstract double perimeter();
}
```

```java
abstract double area() { return 0; }   // COMPILE ERROR: an abstract method cannot have a body
```

### 2.1 Any class containing an abstract method must itself be declared `abstract`

```java
class Shape {                    // forgot "abstract" on the class
    abstract double area();      // COMPILE ERROR: class Shape must be declared abstract
}
```

```java
abstract class Shape {           // correct
    abstract double area();
}
```

### 2.2 A concrete (non-abstract) subclass must implement every inherited abstract method

```java
abstract class Shape {
    abstract double area();
    abstract double perimeter();
}

class Circle extends Shape {
    double radius;
    @Override double area() { return Math.PI * radius * radius; }
    // COMPILE ERROR: Circle is not abstract and does not override abstract method perimeter()
}
```

```java
class Circle extends Shape {
    double radius;
    @Override double area() { return Math.PI * radius * radius; }
    @Override double perimeter() { return 2 * Math.PI * radius; }   // now Circle implements both
}
```

A subclass is allowed to leave some abstract methods unimplemented **only if it, too, is declared `abstract`**, passing the obligation further down the hierarchy.

```java
abstract class Shape {
    abstract double area();
    abstract double perimeter();
}

abstract class RoundShape extends Shape {
    @Override double perimeter() { return 0; }   // implements only one — still abstract overall
    // area() is still unimplemented, so RoundShape must stay abstract too
}

class Circle extends RoundShape {
    double radius;
    @Override double area() { return Math.PI * radius * radius; }   // finally implements the last one
}
```

---

## 3. When a Class Cannot Be Instantiated

An abstract class can **never** be instantiated directly with `new`, even if it has no abstract methods at all — the `abstract` keyword on the class itself is enough to block instantiation.

```java
abstract class Shape {
    void describe() { System.out.println("I am a shape"); }   // no abstract methods here at all
}

Shape s = new Shape();   // COMPILE ERROR: Shape is abstract; cannot be instantiated
```

```java
Shape s = new Circle();   // OK — you CAN have a reference of the abstract type, pointing at a concrete subclass
```

### 3.1 Why would you mark a class abstract with no abstract methods?

Sometimes a class is conceptually **incomplete** or shouldn't make sense as a standalone object, even though every method it currently defines happens to have a body. Marking it `abstract` communicates "this is meant to be extended, not used on its own" and enforces that intent at compile time.

### 3.2 Abstract classes still get constructors run via subclasses

Even though you can never write `new Shape()` directly, `Shape`'s constructor still runs — it executes as part of constructing any concrete subclass, exactly like any other superclass constructor (see the inheritance notes on `super(...)`).

```java
abstract class Shape {
    Shape() { System.out.println("Shape constructor"); }
}
class Circle extends Shape {
    Circle() { System.out.println("Circle constructor"); }
}

new Circle();
// Shape constructor
// Circle constructor
```

---

## 4. What Abstract Classes CAN Have

Abstract classes are much richer than interfaces (traditionally) — they behave like **regular classes** in almost every respect, with the one restriction that they can't be instantiated and may contain abstract methods.

```text
Abstract classes CAN have:
    Constructors
    Instance fields (any access level, final or not)
    Fully implemented (concrete) methods
    Abstract methods
    static methods and static fields
    static and instance initializer blocks
    Any access modifier on members (private, default, protected, public)
```

```java
abstract class PaymentProcessor {
    private static int processedCount = 0;    // static field
    protected String currency;                 // instance field

    PaymentProcessor(String currency) {         // constructor
        this.currency = currency;
    }

    void logTransaction(double amount) {        // concrete method
        processedCount++;
        System.out.println("Processed " + amount + " " + currency);
    }

    static int getProcessedCount() {            // static method
        return processedCount;
    }

    abstract void processPayment(double amount);   // abstract method — subclass must define this
}
```

```java
class CreditCardProcessor extends PaymentProcessor {
    CreditCardProcessor() { super("USD"); }

    @Override
    void processPayment(double amount) {
        logTransaction(amount);
        System.out.println("Charging credit card: " + amount);
    }
}
```

---

## 5. Rules for Abstract Classes

1. Declared with the `abstract` keyword on the class.
2. Cannot be instantiated directly (`new AbstractClass()` is a compile error).
3. **Can** have zero, some, or all abstract methods — having zero abstract methods is legal, as shown in section 3.1.
4. Any abstract method inherited must be implemented by the first **concrete** (non-abstract) subclass in the hierarchy.
5. **Can** have constructors — used via `super(...)` from subclasses, never called directly by outside code.
6. A class that `extends` an abstract class, but doesn't implement all its abstract methods, must **itself** be declared `abstract`.
7. An abstract method **cannot** be `private`, `static`, or `final` — all three would make "must be overridden" meaningless (private isn't inherited/visible, static isn't overridden at all, final can't be overridden).

```java
abstract class Shape {
    private abstract double area();   // COMPILE ERROR: abstract methods cannot be private
    static abstract double perimeter();  // COMPILE ERROR: abstract methods cannot be static
    final abstract double volume();      // COMPILE ERROR: abstract methods cannot be final
}
```

---

## 6. Interfaces: Pure Abstraction

An **interface** declares a **contract** — a set of capabilities a class promises to provide — without (traditionally) saying anything about how those capabilities are implemented, and without carrying any instance state.

```java
interface Drivable {
    void accelerate();
    void brake();
}
```

```java
class Car implements Drivable {
    @Override public void accelerate() { System.out.println("Car speeding up"); }
    @Override public void brake()      { System.out.println("Car slowing down"); }
}
```

### 6.1 Members of an interface are implicitly `public`

```java
interface Drivable {
    void accelerate();          // implicitly: public abstract void accelerate();
}
```

Interface methods without a body are always `public` and `abstract`, whether or not you write those keywords — you're free to write them explicitly, but it changes nothing.

### 6.2 Interface fields are implicitly `public static final` (constants)

```java
interface Constants {
    int MAX_SPEED = 300;        // implicitly: public static final int MAX_SPEED = 300;
}
```

```java
System.out.println(Constants.MAX_SPEED);   // 300
// Constants.MAX_SPEED = 400;                // COMPILE ERROR: final, cannot reassign
```

An interface **cannot** have regular instance fields — only `public static final` constants. This is a core part of why interfaces carry no object state.

### 6.3 A class implements an interface using `implements`

```java
interface Flyable {
    void fly();
}

class Bird implements Flyable {
    @Override
    public void fly() {                 // must be public — cannot narrow an interface method's access
        System.out.println("Bird flying");
    }
}
```

> A class implementing an interface **must** implement every abstract method as `public` — narrowing to `protected` or default access is not allowed, by the same "cannot narrow access on override" rule covered in the polymorphism notes.

---

## 7. Multiple Interface Implementation

A class can `implements` **as many interfaces as it wants**, separated by commas — unlike `extends`, which allows only one parent class.

```java
interface Flyable {
    void fly();
}
interface Swimmable {
    void swim();
}

class Duck implements Flyable, Swimmable {
    @Override public void fly()  { System.out.println("Duck flies"); }
    @Override public void swim() { System.out.println("Duck swims"); }
}
```

```java
Duck d = new Duck();
d.fly();
d.swim();
```

A class can also `extends` one class **and** `implements` any number of interfaces at the same time:

```java
class Vehicle { }

interface Flyable { void fly(); }
interface Chargeable { void charge(); }

class FlyingCar extends Vehicle implements Flyable, Chargeable {
    @Override public void fly()    { System.out.println("Flying"); }
    @Override public void charge() { System.out.println("Charging"); }
}
```

This is how Java delivers the useful parts of "multiple inheritance" (many capabilities combined into one class) without the state/constructor ambiguity that comes from extending multiple classes (see the inheritance notes' discussion of the Diamond Problem).

---

## 8. Default Methods (Java 8+)

A **default method** is a method inside an interface that **does** have a body, marked with the `default` keyword. It provides a **usable fallback implementation** that implementing classes can inherit as-is, or override if they need different behavior.

```java
interface Vehicle {
    void start();

    default void honk() {                    // default method — has a body
        System.out.println("Beep beep!");
    }
}
```

```java
class Car implements Vehicle {
    @Override
    public void start() { System.out.println("Car starting"); }
    // honk() is NOT overridden — Car just uses the interface's default implementation
}

Car c = new Car();
c.start();
c.honk();     // "Beep beep!" — inherited default method, no override needed
```

### 8.1 A class can still override a default method if it needs different behavior

```java
class Truck implements Vehicle {
    @Override
    public void start() { System.out.println("Truck starting"); }

    @Override
    public void honk() { System.out.println("HOOOONK!"); }   // overrides the default
}
```

### 8.2 Why default methods were introduced

Before Java 8, adding a **new** method to an existing interface would break **every** class that already implemented it (they'd suddenly be missing a required method). Default methods solve this: a library can add a new method to a widely-used interface, give it a sensible default body, and every existing implementing class keeps compiling and working — exactly how `java.util.List` gained methods like `forEach()` and `sort()` without breaking the entire ecosystem of existing `List` implementations.

---

## 9. Static Methods in Interfaces (Java 8+)

An interface can also define `static` methods — utility methods logically related to the interface, callable **only** through the interface name itself (never through an implementing object/instance).

```java
interface MathOperations {
    static int square(int x) {
        return x * x;
    }
}
```

```java
int result = MathOperations.square(5);   // called via the interface name
System.out.println(result);              // 25
```

```java
class Calculator implements MathOperations { }

Calculator c = new Calculator();
// c.square(5);                 // COMPILE ERROR: static interface methods are not inherited like instance methods
MathOperations.square(5);       // OK — must call through the interface itself
```

Static interface methods are commonly used for **factory methods** or **helper utilities** tightly related to the interface's purpose, keeping them grouped with the interface rather than scattered in a separate utility class — exactly how `List.of(...)` and `Comparator.comparing(...)` work in the standard library.

---

## 10. Resolving Default Method Conflicts

If a class implements **two interfaces** that each define a **default method with the same signature**, Java refuses to silently guess which one to use — the implementing class **must** override the method and explicitly resolve the conflict.

```java
interface A {
    default void greet() { System.out.println("A"); }
}
interface B {
    default void greet() { System.out.println("B"); }
}

class C implements A, B {
    // COMPILE ERROR if greet() is not overridden:
    // "class C inherits unrelated defaults for greet() from types A and B"
}
```

```java
class C implements A, B {
    @Override
    public void greet() {
        A.super.greet();     // explicitly choose A's version
        // or: B.super.greet();
        // or: write entirely new logic here
    }
}
```

The special syntax `InterfaceName.super.method()` is the **only** way to explicitly call a specific interface's default method from within an overriding class — it mirrors `super.method()` from class inheritance, but must name the interface because there's no single "parent" to default to.

> This compiler-enforced resolution is exactly why interfaces, even with default methods, avoid the ambiguity risk of the class Diamond Problem (see the inheritance notes) — Java never picks a winner on your behalf.

---

## 11. Functional Interfaces

A **functional interface** is an interface with **exactly one abstract method** (default methods and static methods don't count toward this limit). It represents a single, focused "unit of behavior" — which is exactly what makes it usable as a **lambda expression's target type**.

```java
@FunctionalInterface
interface Greetable {
    void greet(String name);        // exactly ONE abstract method
}
```

### 11.1 `@FunctionalInterface` is an optional, compiler-checked annotation

```java
@FunctionalInterface
interface Greetable {
    void greet(String name);
    default void sayBye() { System.out.println("Bye!"); }   // default methods don't count
}
```

```java
@FunctionalInterface
interface Broken {
    void methodOne();
    void methodTwo();      // COMPILE ERROR: a functional interface can have only ONE abstract method
}
```

Just like `@Override`, `@FunctionalInterface` isn't required to make an interface functional — it exists purely so the **compiler** can catch a violation (an accidental second abstract method) immediately, rather than leaving a confusing error at the first place someone tries to use it as a lambda.

### 11.2 Why "exactly one abstract method" matters

A lambda expression is, conceptually, a very compact way to supply the body of **a single method**. For the compiler to know which method a lambda is implementing, the target interface can only have one unimplemented method — any more, and there'd be no way to know which one the lambda's code is meant to fill in.

---

## 12. A Preview of Lambdas

A **lambda expression** is a concise, inline way to provide the implementation of a functional interface's single abstract method, **without** writing a full named class or anonymous class.

### 12.1 The old way: an anonymous class

```java
interface Greetable {
    void greet(String name);
}

Greetable g = new Greetable() {
    @Override
    public void greet(String name) {
        System.out.println("Hello, " + name);
    }
};

g.greet("Aashish");   // Hello, Aashish
```

### 12.2 The same thing, as a lambda

```java
Greetable g = (name) -> System.out.println("Hello, " + name);

g.greet("Aashish");   // Hello, Aashish
```

The lambda `(name) -> System.out.println("Hello, " + name)` is compiled by the compiler into an implementation of `Greetable`'s single abstract method `greet(String name)` — the parameter list before `->` matches the abstract method's parameters, and the code after `->` becomes that method's body.

### 12.3 Lambda syntax forms (just a preview — full depth comes later)

```java
// No parameters
Runnable r = () -> System.out.println("Running");

// One parameter — parentheses optional
Greetable g1 = name -> System.out.println("Hi " + name);
Greetable g2 = (name) -> System.out.println("Hi " + name);

// Multiple parameters — parentheses required
interface Adder { int add(int a, int b); }
Adder add = (a, b) -> a + b;

// Multi-statement body — needs braces and an explicit return (if the method returns a value)
Adder add2 = (a, b) -> {
    int sum = a + b;
    return sum;
};
```

### 12.4 Why this matters right now

Lambdas exist **specifically** because of functional interfaces — without the "exactly one abstract method" rule, the compiler would have no unambiguous target for a lambda's code to become. Understanding functional interfaces here is the bridge into everything lambdas, method references, and the Stream API build on in later study.

---

## 13. Common Built-in Functional Interfaces

Java's `java.util.function` package ships with several ready-made, general-purpose functional interfaces so you rarely need to declare your own for common shapes of behavior.

| Interface | Abstract Method | Purpose |
|-----------|------------------|---------|
| `Runnable` | `void run()` | A task with no input and no output |
| `Supplier<T>` | `T get()` | Produces a value, takes no input |
| `Consumer<T>` | `void accept(T t)` | Consumes a value, returns nothing |
| `Function<T, R>` | `R apply(T t)` | Transforms a `T` into an `R` |
| `Predicate<T>` | `boolean test(T t)` | Tests a condition, returns `true`/`false` |
| `Comparator<T>` | `int compare(T a, T b)` | Defines an ordering between two objects |

```java
Supplier<String> supplier = () -> "Generated value";
Consumer<String> printer = (s) -> System.out.println(s);
Function<Integer, Integer> square = (x) -> x * x;
Predicate<Integer> isEven = (x) -> x % 2 == 0;

System.out.println(supplier.get());       // Generated value
printer.accept("Hello");                   // Hello
System.out.println(square.apply(5));       // 25
System.out.println(isEven.test(4));        // true
```

These interfaces (and the Stream API that leans heavily on them) are covered in full depth later — this section exists just so functional interfaces feel concrete, not abstract-for-its-own-sake.

---

## 14. Abstract Class vs Interface: Feature Comparison

| Feature | Abstract Class | Interface |
|---------|------------------|------------|
| Keyword | `abstract class` | `interface` |
| Relationship keyword | `extends` (one only) | `implements` (multiple allowed) |
| Multiple inheritance | No — one abstract class max | Yes — many interfaces at once |
| Instance fields | Yes, any access level | No — only `public static final` constants |
| Constructors | Yes | No |
| Abstract methods | Optional (can have zero) | At least implicitly supported; any non-default/non-static method is abstract |
| Concrete (fully implemented) methods | Yes, any access level | Yes, via `default` (public only) or `static` (Java 8+) |
| Access modifiers on methods | Any (`private`, default, `protected`, `public`) | Implicitly `public` (abstract/default); `private` interface methods allowed since Java 9 for internal helper use only |
| Can hold shared/common state | Yes | No |
| Represents | An IS-A relationship, often with shared implementation | A CAN-DO / contract-style capability |

---

## 15. Abstract Class vs Interface: When to Choose Which

### 15.1 Choose an abstract class when...

- Related classes share **common state** (fields) that should live in one place.
- You want to provide a **partial implementation** that subclasses build on, with only some behavior left abstract.
- You need **constructors** to enforce initialization logic shared across subclasses.
- The relationship is a true **IS-A** hierarchy with substantial shared code, not just a shared capability.

```java
abstract class Employee {
    protected String name;
    protected double baseSalary;

    Employee(String name, double baseSalary) {
        this.name = name;
        this.baseSalary = baseSalary;
    }

    double getBaseSalary() { return baseSalary; }   // shared, concrete

    abstract double calculateBonus();                // varies per employee type
}
```

### 15.2 Choose an interface when...

- You want to describe a **capability** that unrelated classes might share (`Flyable`, `Comparable`, `Serializable`) regardless of where they sit in the class hierarchy.
- The class implementing it might **already** extend some other class (since Java allows only one `extends`, but unlimited `implements`).
- You want maximum **flexibility** — any class, anywhere in any hierarchy, can opt in to the contract.
- You're designing a **public API/contract** that many unrelated implementations might fulfill differently.

```java
interface Comparable<T> {
    int compareTo(T other);
}

class Student implements Comparable<Student> {
    int score;
    @Override
    public int compareTo(Student other) {
        return Integer.compare(this.score, other.score);
    }
}
```

### 15.3 They are often used together

A very common real-world design combines both: an interface defines the **public contract**, and an abstract class provides a **convenient partial implementation** of that interface for subclasses to extend (the "Abstract + Interface" or "Skeletal Implementation" pattern, e.g. `AbstractList` implementing `List`).

```java
interface Shape {
    double area();
    double perimeter();
}

abstract class AbstractShape implements Shape {
    @Override
    public String toString() {                 // shared, concrete helper for all shapes
        return "Area: " + area() + ", Perimeter: " + perimeter();
    }
}

class Circle extends AbstractShape {
    double radius;
    Circle(double radius) { this.radius = radius; }
    @Override public double area()      { return Math.PI * radius * radius; }
    @Override public double perimeter() { return 2 * Math.PI * radius; }
}
```

### 15.4 One-line decision rule

> If you're modeling **what something IS**, with meaningful shared state and implementation, reach for an **abstract class**. If you're modeling **what something CAN DO**, as a contract any unrelated class might fulfill, reach for an **interface**.

---

## 16. Common Pitfalls

| # | Pitfall | Fix |
|---|---------|-----|
| 1 | Trying to instantiate an abstract class directly | Not allowed, even with zero abstract methods — only concrete subclasses can be instantiated |
| 2 | Forgetting to mark a class `abstract` when it still has unimplemented abstract methods | The class itself must be declared `abstract` if even one inherited abstract method is left unimplemented |
| 3 | Declaring an abstract method as `private`, `static`, or `final` | None are allowed — all three contradict the idea of "must be overridden by a subclass" |
| 4 | Assuming interface fields can be reassigned | Interface fields are implicitly `public static final` — constants, not mutable state |
| 5 | Narrowing access when implementing an interface method | Interface methods are implicitly `public`; implementations must stay `public` |
| 6 | Assuming a default method is automatically "the implementation" with no way to change it | Implementing classes can freely override a default method when they need different behavior |
| 7 | Calling a `static` interface method through an instance | Static interface methods must be called through the interface name, not an object reference |
| 8 | Letting two interfaces' conflicting default methods go unresolved | The implementing class must override the method and explicitly pick (or recombine) a version via `InterfaceName.super.method()` |
| 9 | Adding a second abstract method to an interface meant to be functional | A functional interface permits exactly one abstract method; use `@FunctionalInterface` to catch this at compile time |
| 10 | Thinking `@FunctionalInterface` is required to use lambdas | It's optional — it only adds a compile-time check; any interface with exactly one abstract method can be used as a lambda target regardless |
| 11 | Reaching for an interface when classes need to share real state | If subclasses need common fields and concrete shared behavior, an abstract class is usually the better fit |
| 12 | Reaching for an abstract class when a class already extends something else | Since a class can only extend one class, prefer an interface when the capability needs to be added alongside existing inheritance |

---

## 17. Interview Questions

**Q1. What is abstraction, and what two language features does Java provide for it?**
Abstraction means exposing essential behavior while hiding implementation details. Java provides abstract classes (partial blueprints with some implementation and some left abstract) and interfaces (pure contracts describing capabilities) as its two main tools for abstraction.

---

**Q2. Can an abstract class have no abstract methods at all?**
Yes. Marking a class `abstract` alone is enough to prevent direct instantiation, even if every method in the class has a full implementation. This is often done to signal that the class is meant to be extended, not used standalone.

---

**Q3. What happens if a subclass of an abstract class doesn't implement all inherited abstract methods?**
The subclass must itself be declared `abstract`, passing the remaining implementation obligation further down the hierarchy. Only when a concrete (non-abstract) subclass is reached must every abstract method finally be implemented.

---

**Q4. Why can't an abstract method be `private`, `static`, or `final`?**
`private` methods aren't inherited/visible to subclasses, so there'd be nothing to override. `static` methods are resolved by reference type and aren't overridden at all, only hidden. `final` methods cannot be overridden by definition. All three directly contradict what an abstract method requires: a mandatory override by a subclass.

---

**Q5. Can a class implement multiple interfaces? Can it extend multiple classes?**
A class can implement any number of interfaces using `implements`, separated by commas. It can extend only one class using `extends` — Java does not support multiple class inheritance (see the inheritance notes for why, including the Diamond Problem).

---

**Q6. What are default methods, and why were they introduced in Java 8?**
Default methods are interface methods with a body, marked `default`. They were introduced so library maintainers could add new methods to widely-implemented interfaces (like `List`) without breaking every existing class that already implements that interface — the new method simply falls back to the default implementation unless a class chooses to override it.

---

**Q7. How are static methods in interfaces different from default methods?**
Static interface methods belong to the interface itself and must be called through the interface name (`InterfaceName.method()`), never through an implementing instance. Default methods, by contrast, are inherited by implementing classes and are called through an instance, and can be overridden by them.

---

**Q8. What happens if a class implements two interfaces that each declare a conflicting default method with the same signature?**
The class does not compile unless it explicitly overrides that method itself. Inside the override, it can call a specific interface's version using `InterfaceName.super.method()`, or supply entirely new logic. Java never silently picks a winner.

---

**Q9. What is a functional interface, and what rule defines it?**
An interface with exactly one abstract method (default and static methods don't count toward the limit). This single-method shape is exactly what allows a lambda expression to supply that method's body concisely, since the compiler can unambiguously tell which method the lambda implements.

---

**Q10. How do you decide between an abstract class and an interface for a given design?**
Choose an abstract class when related classes need shared state and partial shared implementation in a true IS-A hierarchy. Choose an interface when you're describing a capability/contract that unrelated classes (possibly already extending something else) might fulfill. The two are often combined: an interface defines the contract, and an abstract class provides a convenient partial implementation of it.

---

## 18. Interview-Style Output Questions

**Q1**
```java
abstract class Shape {
    void describe() { System.out.println("A shape"); }
}
Shape s = new Shape();
```
**Answer:** Compile error — `Shape` is abstract and cannot be instantiated, even though it has no abstract methods.

---

**Q2**
```java
abstract class Animal {
    abstract void sound();
}
abstract class Pet extends Animal {
    void greet() { System.out.println("Hello owner"); }
}
class Dog extends Pet {
    @Override void sound() { System.out.println("Woof"); }
}
new Dog().sound();
new Dog().greet();
```
**Answer:**
```
Woof
Hello owner
```
(`Pet` stays abstract because it never implements `sound()`; `Dog` finally implements it.)

---

**Q3**
```java
interface Vehicle {
    default void honk() { System.out.println("Beep"); }
}
class Car implements Vehicle { }
new Car().honk();
```
**Answer:** `Beep` — `Car` inherits the default method unchanged.

---

**Q4**
```java
interface MathOps {
    static int square(int x) { return x * x; }
}
class Calc implements MathOps { }
Calc c = new Calc();
System.out.println(c.square(4));
```
**Answer:** Compile error — static interface methods are not callable through an instance; must use `MathOps.square(4)`.

---

**Q5**
```java
interface A {
    default void hello() { System.out.println("A"); }
}
interface B {
    default void hello() { System.out.println("B"); }
}
class C implements A, B {
    @Override
    public void hello() {
        A.super.hello();
        B.super.hello();
    }
}
new C().hello();
```
**Answer:**
```
A
B
```
(Both defaults are explicitly invoked inside the overriding method.)

---

**Q6**
```java
@FunctionalInterface
interface Calculator {
    int operate(int a, int b);
    int operate(int a);
}
```
**Answer:** Compile error — a `@FunctionalInterface` is only allowed exactly one abstract method; here there are two (overloaded) abstract methods.

---

**Q7**
```java
interface Greetable {
    void greet(String name);
}
Greetable g = (name) -> System.out.println("Hi " + name);
g.greet("Aashish");
```
**Answer:** `Hi Aashish` — the lambda supplies the body of `greet(String)`, the interface's single abstract method.

---

## 19. Quick Cheat Sheet

```text
ABSTRACT CLASS            abstract class Name { ... }
                           - Cannot be instantiated (new Name() fails), even with zero abstract methods
                           - CAN have: constructors, instance fields, concrete methods, static members, any access level
                           - A class with ANY unimplemented abstract method must itself be abstract
                           - abstract methods: no body; cannot be private, static, or final
                           - extends ONE class only

INTERFACE                 interface Name { ... }
                           - Pure contract — no instance state, only public static final constants
                           - Methods without a body: implicitly public abstract
                           - default methods (Java 8+): have a body, inherited, OVERRIDABLE
                           - static methods (Java 8+): called via InterfaceName.method(), never via instance
                           - implements MANY interfaces at once (comma-separated)
                           - conflicting default methods -> class MUST override and resolve via
                             InterfaceName.super.method()

FUNCTIONAL INTERFACE      Exactly ONE abstract method (default/static methods don't count)
                           @FunctionalInterface -> optional, compiler-enforced check
                           Enables: lambda expressions as a concise inline implementation

LAMBDA (preview)           (params) -> expression
                           (params) -> { statements; return value; }
                           Compiles into an implementation of the functional interface's one method

BUILT-IN FUNCTIONAL TYPES  Runnable       () -> void
                           Supplier<T>    () -> T
                           Consumer<T>    (T) -> void
                           Function<T,R>  (T) -> R
                           Predicate<T>   (T) -> boolean
                           Comparator<T>  (T, T) -> int

CHOOSE ABSTRACT CLASS      Shared state + shared implementation, true IS-A hierarchy, need constructors
CHOOSE INTERFACE           Capability/contract across unrelated classes, need "multiple inheritance" of behavior
```

**Remember:**

1. `abstract` on a class blocks instantiation regardless of whether it has abstract methods.
2. Every abstract method left unimplemented forces the containing class to stay `abstract` too.
3. Interfaces carry no instance state — only constants — and their abstract methods are always `public`.
4. Default and static methods (Java 8+) let interfaces provide real code without breaking "no state" or forcing every implementer to override everything.
5. A functional interface's "exactly one abstract method" rule is precisely what makes lambdas possible.
6. Reach for an abstract class for shared state/implementation in an IS-A hierarchy; reach for an interface for a capability contract usable across unrelated classes — and feel free to combine both.
