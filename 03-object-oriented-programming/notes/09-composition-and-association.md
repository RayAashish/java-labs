# Java Composition and Association: Detailed Notes

So far, the pillars of OOP (inheritance especially) modeled relationships through **IS-A** — a `Dog` IS-A `Animal`. But not every relationship between classes is "is-a." Many are **HAS-A** — a `Car` HAS-A `Engine`, a `Library` HAS-A collection of `Book`s. Modeling these correctly, and knowing when to reach for a HAS-A relationship instead of inheritance, is what this topic covers.

```java
class Engine {
    void start() { System.out.println("Engine starting"); }
}

class Car {
    private Engine engine = new Engine();   // Car HAS-A Engine — composition, not inheritance

    void start() {
        engine.start();
        System.out.println("Car is moving");
    }
}
```

---

## Table of Contents

1. [IS-A vs HAS-A](#1-is-a-vs-has-a)
2. [Association: The General HAS-A Relationship](#2-association-the-general-has-a-relationship)
3. [Composition: Strong Ownership](#3-composition-strong-ownership)
4. [Aggregation: Weak Ownership](#4-aggregation-weak-ownership)
5. [Composition vs Aggregation — Side by Side](#5-composition-vs-aggregation--side-by-side)
6. [Composition vs Inheritance](#6-composition-vs-inheritance)
7. [Why "Favor Composition Over Inheritance"](#7-why-favor-composition-over-inheritance)
8. [When Inheritance Is Still the Right Choice](#8-when-inheritance-is-still-the-right-choice)
9. [Recognizing Which Relationship to Model](#9-recognizing-which-relationship-to-model)
10. [UML-Style Notation (For Reference)](#10-uml-style-notation-for-reference)
11. [Common Pitfalls](#11-common-pitfalls)
12. [Interview Questions](#12-interview-questions)
13. [Interview-Style Output Questions](#13-interview-style-output-questions)
14. [Quick Cheat Sheet](#14-quick-cheat-sheet)

---

## 1. IS-A vs HAS-A

| | IS-A | HAS-A |
|---|------|--------|
| Modeled with | Inheritance (`extends`) or interface implementation (`implements`) | A field referencing another object (composition/aggregation) |
| Question it answers | "What kind of thing is this?" | "What does this thing contain or use?" |
| Example | `Dog extends Animal` — a Dog IS-A kind of Animal | `Car` has an `Engine` field — a Car HAS-A Engine |
| Relationship strength | Tight — the subclass inherits the parent's whole public contract and often its implementation | Flexible — ranges from "owns and controls the lifetime of" to "merely uses, temporarily" |

```java
// IS-A — inheritance
class Animal { }
class Dog extends Animal { }    // Dog IS-A Animal

// HAS-A — composition/association
class Engine { }
class Car {
    private Engine engine;      // Car HAS-A Engine
}
```

### 1.1 The litmus test: can you honestly say "X IS-A Y"?

A simple way to decide which relationship fits: try saying the sentence out loud.

```text
"A Dog IS-A Animal."          -> makes sense           -> inheritance (IS-A)
"A Car IS-A Engine."          -> does NOT make sense    -> composition (HAS-A) instead
"A Car HAS-A Engine."         -> makes sense             -> composition (HAS-A)
```

If the IS-A sentence sounds wrong or forced, that's usually a sign the relationship should be modeled as HAS-A instead — a mistake covered in depth in section 7.

---

## 2. Association: The General HAS-A Relationship

**Association** is the broadest term — it simply means one class **uses or is connected to** another class, with no specific implication about ownership or lifetime. Composition and aggregation (sections 3 and 4) are both specific, stronger **kinds** of association.

```java
class Teacher {
    String name;
}

class Student {
    String name;

    void attendClass(Teacher t) {       // Student is ASSOCIATED with Teacher, just for this method call
        System.out.println(name + " is attending " + t.name + "'s class");
    }
}
```

Here, `Student` and `Teacher` are related — a `Student` *uses* a `Teacher` — but neither one owns the other, and neither one's lifetime depends on the other. A `Teacher` can exist without any particular `Student`, and vice versa. This loose, general "uses-a" or "knows-about" relationship is association in its plainest form.

### 2.1 Association can be one-directional or bidirectional

```java
class Student {
    List<Course> courses;      // Student knows about its Courses
}

class Course {
    // Course doesn't necessarily need to know about its Students — one-directional
}
```

```java
class Student {
    List<Course> courses;      // Student knows about Courses
}
class Course {
    List<Student> students;    // Course also knows about its Students — bidirectional
}
```

---

## 3. Composition: Strong Ownership

**Composition** is a HAS-A relationship with **strong ownership**: the contained object's **lifetime is entirely controlled by** the containing object. The "part" cannot meaningfully exist without its "whole," and when the whole is destroyed, the part is destroyed with it.

```java
class Heart {
    void beat() { System.out.println("Heart beating"); }
}

class Human {
    private final Heart heart = new Heart();   // the Heart is created INSIDE Human, owned entirely by it

    void live() {
        heart.beat();
    }
}
```

A `Human`'s `Heart` doesn't exist independently of that `Human` — it's created when the `Human` is created, and it's never shared with or handed off to another `Human`. If the `Human` object is garbage collected, its `Heart` has no other references keeping it alive either.

### 3.1 Hallmarks of composition

```text
The "part" object is typically created INSIDE the "whole" (often in the constructor).
The "part" has no meaningful existence or identity outside its "whole."
The "part" is usually NOT shared between multiple "whole" instances.
Destroying the "whole" effectively destroys the "part" (no external references survive it).
Often implemented with a private field, with no setter exposing it to be replaced externally.
```

```java
class Room {
    void clean() { System.out.println("Room cleaned"); }
}

class House {
    private final List<Room> rooms = new ArrayList<>();   // Rooms created and owned entirely by this House

    House() {
        rooms.add(new Room());
        rooms.add(new Room());
    }
}
```

A `Room`, in this design, is meaningless outside the specific `House` that built it — it was never handed in from outside, and nothing else holds a reference to it.

---

## 4. Aggregation: Weak Ownership

**Aggregation** is also a HAS-A relationship, but with **weak ownership**: the contained object can exist **independently** of the containing object, both before the relationship forms and after it ends.

```java
class Author {
    String name;
    Author(String name) { this.name = name; }
}

class Book {
    private Author author;          // Book HAS-A Author, but doesn't OWN it

    Book(Author author) {            // the Author is created OUTSIDE, then handed in
        this.author = author;
    }
}
```

```java
Author a = new Author("Premchand");
Book b1 = new Book(a);
Book b2 = new Book(a);        // the SAME Author is shared by two different Books — this is aggregation

// if b1 and b2 are both discarded, "a" (the Author) can still exist independently elsewhere
```

### 4.1 Hallmarks of aggregation

```text
The "part" object is typically created OUTSIDE the "whole" and passed in (constructor, setter, or method).
The "part" has its own independent lifetime and identity.
The "part" CAN be shared between multiple "whole" instances simultaneously.
Destroying the "whole" does NOT destroy the "part" — other references can keep it alive.
Often implemented with a field set via a constructor parameter or a public setter.
```

```java
class Department {
    String name;
}

class Employee {
    private Department department;   // Employee is assigned TO a Department, doesn't own it

    void setDepartment(Department d) {
        this.department = d;
    }
}
```

An `Employee` being deleted from the system doesn't delete the `Department` — the `Department` continues to exist, with or without that particular employee, and can have other employees associated with it.

---

## 5. Composition vs Aggregation — Side by Side

| | Composition (strong) | Aggregation (weak) |
|---|------------------------|----------------------|
| Ownership | Whole **owns** the part exclusively | Whole merely **uses/references** the part |
| Part's lifetime | Tied to the whole's lifetime | Independent of the whole's lifetime |
| Can the part be shared? | No, typically exclusive to one whole | Yes, can be shared across multiple wholes |
| Where is the part usually created? | Inside the whole (often in its constructor) | Outside the whole, then passed in |
| Example | `Human` and `Heart`, `House` and `Room`, `Car` and `Engine` | `Book` and `Author`, `Employee` and `Department`, `Student` and `Teacher` |
| "Part" survives "whole" being destroyed? | No | Yes |

```java
// COMPOSITION — Engine is created inside, owned exclusively by this Car
class Car {
    private final Engine engine = new Engine();
}

// AGGREGATION — Author exists independently, can belong to many Books
class Book {
    private Author author;
    Book(Author author) { this.author = author; }
}
```

### 5.1 A useful mental shortcut

```text
Composition:  "The part is BUILT as part of the whole, and dies with it."
Aggregation:  "The part is BORROWED/ASSIGNED to the whole, and outlives it."
```

### 5.2 Both are still "HAS-A" — the difference is only ownership strength

It's easy to think of composition and aggregation as two completely separate relationship types, but they're really the **same kind of relationship (HAS-A)**, sitting at different points on a spectrum of ownership strength, with plain association being the loosest, most general form of that same spectrum.

```text
Association (general "uses-a")  --->  Aggregation (weak "has-a, shared")  --->  Composition (strong "has-a, owned")
      loosest relationship                                                          tightest relationship
```

---

## 6. Composition vs Inheritance

Both inheritance and composition let one class **reuse** functionality defined in another class — but they do it in fundamentally different ways, with different tradeoffs.

```java
// INHERITANCE — reuse via "is-a"
class Engine {
    void start() { System.out.println("Engine starting"); }
}
class Car extends Engine {        // Car IS-A Engine?? This doesn't make conceptual sense
    void drive() {
        start();                   // inherited directly
        System.out.println("Driving");
    }
}
```

```java
// COMPOSITION — reuse via "has-a"
class Engine {
    void start() { System.out.println("Engine starting"); }
}
class Car {
    private Engine engine = new Engine();   // Car HAS-A Engine — conceptually correct
    void drive() {
        engine.start();                      // delegated, not inherited
        System.out.println("Driving");
    }
}
```

Both versions let `Car` reuse `Engine`'s `start()` logic — but only the composition version models the relationship **correctly** (a Car isn't a kind of Engine; it merely contains/uses one).

### 6.1 Key structural differences

| | Inheritance | Composition |
|---|--------------|---------------|
| Relationship modeled | IS-A | HAS-A |
| Reuse mechanism | Implicit — inherited members are automatically available | Explicit — must deliberately delegate/call through the contained object |
| Coupling | Tight — subclass is deeply tied to the superclass's implementation details | Loose — the containing class only depends on the contained object's public interface |
| Flexibility to change at runtime | No — a class's superclass is fixed at compile time | Yes — the contained object/reference can potentially be swapped out (e.g., via a setter or constructor parameter) |
| Exposes unwanted parent behavior? | Yes, risk of inheriting methods that don't make sense for the subclass | No — the containing class exposes only what it deliberately chooses to expose |
| Number of "parents"/components | Single inheritance only (one superclass, see the inheritance notes) | Can compose with many other objects simultaneously, no restriction |

---

## 7. Why "Favor Composition Over Inheritance"

This is one of the most repeated pieces of advice in object-oriented design, and it comes from real, recurring problems that inheritance introduces when it's used just to **reuse code**, rather than to model a genuine IS-A relationship.

### 7.1 Problem: inheritance can expose behavior that doesn't belong

```java
class ArrayListBasedStack extends ArrayList<Integer> {
    void push(int val) { add(val); }
    int pop() { return remove(size() - 1); }
}
```

```java
ArrayListBasedStack stack = new ArrayListBasedStack();
stack.push(1);
stack.push(2);
stack.add(0, 99);     // inherited from ArrayList — breaks the stack's "only push/pop at the top" invariant!
```

Because `ArrayListBasedStack` **extends** `ArrayList`, it inherits **every** `ArrayList` method, including ones that violate what a stack is supposed to guarantee (insert-anywhere access). A `Stack` IS-NOT-really an `ArrayList` conceptually — it merely needs to **use** one internally.

```java
class Stack {
    private final List<Integer> items = new ArrayList<>();   // composition — Stack HAS-A List

    void push(int val) { items.add(val); }
    int pop() { return items.remove(items.size() - 1); }
    // NO add(index, value) exposed — the stack's invariant is protected
}
```

### 7.2 Problem: tight coupling to superclass implementation details

A subclass's behavior can be silently broken if the superclass's **internal implementation** changes — even if the superclass's public contract stays the same — because overriding methods can interact with the parent's internals in subtle, undocumented ways (this is the well-known "fragile base class" problem).

```java
class Base {
    void doWork() {
        step1();
        step2();
    }
    void step1() { System.out.println("step1"); }
    void step2() { System.out.println("step2"); }
}
class Derived extends Base {
    @Override
    void step1() {
        System.out.println("overridden step1");
        step2();     // if Base.doWork() changes to also call step2() again later, Derived's override now double-calls it
    }
}
```

Composition avoids this entirely: a containing class only calls the **public methods** of the object it holds, through a stable, well-defined interface — it has no access to (and no accidental dependency on) the contained object's internals.

### 7.3 Problem: inflexible at runtime, single-parent limitation

```java
class Car extends Engine { }   // locked in at compile time — this Car can NEVER have a different kind of Engine
```

```java
class Car {
    private Engine engine;
    Car(Engine engine) { this.engine = engine; }   // can be swapped for a DIFFERENT Engine implementation entirely
}

Car c1 = new Car(new PetrolEngine());
Car c2 = new Car(new ElectricEngine());
```

Composition allows a class to be configured with **different** behaviors at construction time (or even swapped later, via a setter), something inheritance's compile-time, single-parent nature simply cannot offer (see the inheritance notes on why Java restricts classes to a single `extends`).

### 7.4 Summary: what "favor composition" actually means

> It does **not** mean "never use inheritance." It means: **before reaching for `extends` purely to reuse another class's code, first ask whether the relationship is genuinely IS-A.** If it's really HAS-A (or simply "uses"), composition gives you the same code reuse with better encapsulation, looser coupling, and more runtime flexibility — without exposing unwanted behavior or locking in a rigid, compile-time-fixed relationship.

---

## 8. When Inheritance Is Still the Right Choice

Composition isn't a universal replacement — inheritance remains the correct tool when the relationship is **genuinely** IS-A, and especially when you want to leverage **polymorphism**.

```java
abstract class Shape {
    abstract double area();
}
class Circle extends Shape {
    double radius;
    @Override double area() { return Math.PI * radius * radius; }
}
class Square extends Shape {
    double side;
    @Override double area() { return side * side; }
}
```

```java
List<Shape> shapes = List.of(new Circle(), new Square());
for (Shape s : shapes) {
    System.out.println(s.area());    // dynamic dispatch — THIS is what inheritance uniquely enables
}
```

Composition **cannot** replicate this: there is no equivalent way to put unrelated "part" objects in a single list and have a shared method call dispatch to each one's specific implementation, unless those objects already share a common supertype — which is exactly what inheritance (or interface implementation) provides.

### 8.1 Good signals that inheritance is the right call

```text
The relationship genuinely reads as "X IS-A Y" without feeling forced.
You need POLYMORPHISM — treating many different subtypes uniformly through a shared reference type.
The subclass should inherit essentially ALL of the parent's public contract, with no unwanted leftovers.
The hierarchy is stable and unlikely to need restructuring as requirements evolve.
```

### 8.2 Good signals that composition is the right call

```text
The relationship reads more like "X HAS-A Y" or "X USES Y" than "X IS-A Y."
You only need to reuse PART of another class's behavior, not its entire public contract.
You want the flexibility to swap the contained object/behavior at construction time or later.
You want to avoid exposing methods that don't make sense for the containing class.
```

---

## 9. Recognizing Which Relationship to Model

A practical way to work through a design decision, combining everything above into one flow:

```text
1. Can you honestly say "X IS-A Y"?
     YES -> consider inheritance, especially if you need polymorphism
     NO  -> it's a HAS-A / USES-A relationship -> go to step 2

2. Does X fully OWN Y's lifetime — is Y created inside X, never shared, and
   meaningless once X is gone?
     YES -> COMPOSITION (strong ownership)
     NO  -> go to step 3

3. Does Y exist independently, potentially before/after/alongside X, and
   could it be shared across multiple X instances?
     YES -> AGGREGATION (weak ownership)

4. Is the relationship just a temporary "uses" with no ownership implication
   at all (e.g., a parameter passed into one method call)?
     YES -> plain ASSOCIATION
```

```java
class Engine { }
class Car {
    private final Engine engine = new Engine();   // Step 1: NO (Car is-not-an Engine). Step 2: YES -> COMPOSITION
}

class Author { }
class Book {
    private Author author;                          // Step 1: NO. Step 2: NO (Author outlives/is shared). Step 3: YES -> AGGREGATION
    Book(Author author) { this.author = author; }
}

class Teacher { }
class Student {
    void attendClass(Teacher t) { }                 // Step 1: NO. Step 2: NO. Step 3: NO (not even stored). Step 4: YES -> ASSOCIATION
}

class Animal { }
class Dog extends Animal { }                        // Step 1: YES -> INHERITANCE
```

---

## 10. UML-Style Notation (For Reference)

These relationships are commonly drawn in UML class diagrams with specific arrow/line styles — useful to recognize when reading design documents or diagrams, even without drawing them yourself.

```text
Inheritance (IS-A)     : A ──────▷ B     (hollow triangle arrowhead, pointing to the parent/interface)
Composition (strong)   : A ──♦──── B     (filled diamond at the "whole" end)
Aggregation (weak)     : A ──◇──── B     (hollow diamond at the "whole" end)
Association (general)  : A ──────── B    (plain line, sometimes with an arrowhead showing direction)
```

The diamond (filled or hollow) always sits on the side of the **"whole"/owner**, and points toward the **"part"/owned (or referenced) object**.

---

## 11. Common Pitfalls

| # | Pitfall | Fix |
|---|---------|-----|
| 1 | Using inheritance purely to reuse code, without a genuine IS-A relationship | Run the "X IS-A Y" sentence test; if it sounds forced, use composition instead |
| 2 | Extending a collection class (like `ArrayList`) to add custom behavior | Prefer composition — hold a `List` as a private field and expose only the methods that make sense |
| 3 | Confusing composition and aggregation as interchangeable terms | They differ specifically in ownership strength and lifetime — not just "a class has a field" |
| 4 | Assuming all HAS-A relationships are composition | Only strong-ownership, lifetime-bound relationships are composition; shared or independently-created objects are aggregation |
| 5 | Exposing a setter for a composed ("owned") object, letting outside code swap it for something else | True composition usually keeps the contained object `private` and `final`, created internally, with no way to replace it externally |
| 6 | Thinking composition can fully replace inheritance in every case | Composition can't give you polymorphism across unrelated types; genuine IS-A hierarchies with shared behavior still need inheritance |
| 7 | Forgetting that a subclass inherits ALL public/protected members, including ones that break invariants | This is exactly the risk "favor composition" warns about — audit what you're really inheriting, not just what you want to reuse |
| 8 | Treating "favor composition over inheritance" as "never use inheritance" | It's guidance for when reuse is the only motivation, not an absolute rule against all inheritance |
| 9 | Modeling a loose "uses for one method call" relationship as composition or aggregation | If there's no stored reference or ownership implication at all, it's simply association, not the stronger forms |
| 10 | Assuming the "part" object must always be a different class entirely | A composed/aggregated field is just a regular field holding a reference to another object — nothing more exotic than that |

---

## 12. Interview Questions

**Q1. What is the difference between an IS-A and a HAS-A relationship?**
IS-A describes what kind of thing something is, modeled through inheritance or interface implementation (a Dog IS-A Animal). HAS-A describes what something contains or uses, modeled by holding a reference to another object as a field (a Car HAS-A Engine).

---

**Q2. What is association, and how does it relate to composition and aggregation?**
Association is the general term for any relationship where one class uses or is connected to another, with no specific implication about ownership. Composition and aggregation are both specific, stronger forms of association that add a notion of ownership — composition being strong ownership, aggregation being weak ownership.

---

**Q3. What distinguishes composition from aggregation?**
In composition, the "whole" owns the "part" exclusively — the part is typically created inside the whole, cannot be shared, and its lifetime is entirely tied to the whole's. In aggregation, the "part" exists independently — it's typically created outside and passed in, can be shared across multiple "wholes," and continues to exist after any particular "whole" is destroyed.

---

**Q4. Give an example each of composition and aggregation.**
Composition: a `House` and its `Room`s — rooms are built as part of the house and have no existence outside it. Aggregation: a `Book` and its `Author` — the author exists independently of any one book and could be the author of many books.

---

**Q5. What does "favor composition over inheritance" mean?**
It's guidance that, when the only reason to use inheritance is to reuse another class's code (rather than a genuine IS-A relationship), composition is usually the better choice — it avoids exposing unwanted inherited behavior, reduces tight coupling to the other class's implementation details, and allows more runtime flexibility, since the contained object can potentially be swapped.

---

**Q6. Give a concrete example of a problem caused by using inheritance purely for code reuse.**
Extending `ArrayList` to build a `Stack` inherits every `ArrayList` method, including ones like inserting at an arbitrary index — which breaks the stack's core invariant that elements should only be pushed/popped at the top. Composition (holding a `List` as a private field) avoids this by exposing only the methods that make sense for a stack.

---

**Q7. Does "favor composition over inheritance" mean inheritance should never be used?**
No. Inheritance remains the right tool when the relationship is genuinely IS-A, and especially when polymorphism is needed — treating many different subtypes uniformly through a shared supertype reference, which composition alone cannot replicate for unrelated classes.

---

**Q8. How would you decide, in a real design, whether to use composition or aggregation for a given relationship?**
Ask whether the "part" object is created inside the "whole" and has no meaningful existence without it (composition), or whether it's created independently, can be shared, and continues to exist after the "whole" is gone (aggregation). The deciding factor is ownership and lifetime, not just "does this class have a field referencing another object."

---

**Q9. Why is tight coupling a concern with inheritance that composition avoids?**
A subclass can depend on undocumented details of how a superclass's methods interact internally (e.g., one method calling another), so changes to the superclass's implementation — even without changing its public contract — can silently break subclasses. This is often called the "fragile base class" problem. Composition avoids it because the containing class only interacts with the contained object's stable, public interface.

---

**Q10. How is composition typically implemented in terms of field declaration?**
Usually as a `private` (often also `final`) field, initialized inside the containing class's own constructor, with no setter exposed to replace it from outside — reinforcing that the contained object is exclusively owned and its lifetime is fully controlled by the containing object.

---

## 13. Interview-Style Output Questions

**Q1**
```java
class Engine {
    void start() { System.out.println("Engine starting"); }
}
class Car {
    private final Engine engine = new Engine();
    void drive() {
        engine.start();
        System.out.println("Car driving");
    }
}
new Car().drive();
```
**Answer:**
```
Engine starting
Car driving
```
(Straightforward composition — `Car` delegates to its owned `Engine`.)

---

**Q2**
```java
class Author {
    String name;
    Author(String name) { this.name = name; }
}
class Book {
    Author author;
    Book(Author author) { this.author = author; }
}
Author a = new Author("Premchand");
Book b1 = new Book(a);
Book b2 = new Book(a);
System.out.println(b1.author == b2.author);
```
**Answer:** `true`. Both `Book`s reference the exact same `Author` object — this sharing is a hallmark of aggregation, which would not typically happen with true composition.

---

**Q3**
```java
class ArrayListBasedStack extends java.util.ArrayList<Integer> {
    void push(int val) { add(val); }
    int pop() { return remove(size() - 1); }
}
ArrayListBasedStack s = new ArrayListBasedStack();
s.push(1);
s.push(2);
s.add(0, 99);
System.out.println(s);
```
**Answer:** `[99, 1, 2]`. The inherited `add(index, value)` method from `ArrayList` compiles and runs fine, silently violating the "stack" abstraction's intended invariant — exactly the risk inheritance-for-reuse introduces.

---

**Q4**
```java
class Heart {
    Heart() { System.out.println("Heart created"); }
}
class Human {
    private final Heart heart = new Heart();
    Human() { System.out.println("Human created"); }
}
new Human();
```
**Answer:**
```
Heart created
Human created
```
(Field initializers run before the constructor body — the owned `Heart` is fully constructed as part of building the `Human`, consistent with composition's "part is built as part of the whole.")

---

## 14. Quick Cheat Sheet

```text
IS-A                 Modeled with inheritance/interfaces; "Dog IS-A Animal"
HAS-A                Modeled with a field referencing another object; "Car HAS-A Engine"
LITMUS TEST           Say "X IS-A Y" out loud — if it sounds forced, it's HAS-A instead

ASSOCIATION          General "uses-a" / "knows-about" relationship; no ownership implied
AGGREGATION          HAS-A + WEAK ownership: part created outside, shareable, outlives the whole
COMPOSITION          HAS-A + STRONG ownership: part created inside, exclusive, dies with the whole

SPECTRUM              Association (loosest)  ->  Aggregation (weak has-a)  ->  Composition (strong has-a)

COMPOSITION SIGNS      private (often final) field, initialized in the constructor, no external setter
AGGREGATION SIGNS       field set via constructor param or setter, object created OUTSIDE, can be shared

INHERITANCE vs          Inheritance: implicit reuse, tight coupling, fixed at compile time, single parent
COMPOSITION             Composition: explicit delegation, loose coupling, swappable, many components allowed

FAVOR COMPOSITION       Guidance for REUSE-ONLY inheritance, not a ban on inheritance entirely
                        Risk avoided: exposing unwanted inherited behavior (e.g., Stack extends ArrayList)
                        Risk avoided: fragile base class (subclass broken by parent's internal changes)
                        Gain: runtime flexibility (swap the contained object/behavior)

KEEP INHERITANCE WHEN   Relationship is genuinely IS-A
                        You need POLYMORPHISM across the hierarchy
                        Subclass should inherit essentially the WHOLE public contract, no leftovers

UML NOTATION            A ──▷ B     inheritance (hollow triangle, points to parent)
                        A ──♦── B   composition (filled diamond, at the "whole")
                        A ──◇── B   aggregation (hollow diamond, at the "whole")
                        A ──── B    plain association
```

**Remember:**

1. IS-A is inheritance; HAS-A is association, and composition/aggregation are its two ownership-flavored specializations.
2. Composition means strong, exclusive ownership — the part is built inside and dies with the whole; aggregation means weak, shareable ownership — the part lives independently.
3. "Favor composition over inheritance" targets reuse-only inheritance, not every use of `extends` — genuine IS-A relationships needing polymorphism still belong to inheritance.
4. The classic cautionary example is extending a collection class purely for reuse — it silently inherits behavior that breaks the new class's intended invariants.
5. A quick design flow: ask "IS-A?" first: polymorphism needed and a true is-a → inheritance. Otherwise ask "does it own the part's lifetime exclusively?" → composition if yes, aggregation if the part is shared or independent, plain association if there's no real ownership at all.
