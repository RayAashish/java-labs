# Java OOP Foundations: Detailed Notes

**Object-Oriented Programming (OOP)** is a programming paradigm that organizes software around **objects** — bundles of **data** (fields/state) and **behavior** (methods) — instead of organizing it around functions acting on separate, often global, data.

```java
class BankAccount {
    private String name;
    private double balance;

    BankAccount(String name, double balance) {
        this.name = name;
        this.balance = balance;
    }

    void deposit(double amount) {
        balance += amount;
        System.out.println(name + "'s new balance: " + balance);
    }
}
```

---

## Table of Contents

1. [Why OOP Exists](#1-why-oop-exists)
2. [Procedural vs Object-Oriented Thinking](#2-procedural-vs-object-oriented-thinking)
3. [The Four Pillars of OOP](#3-the-four-pillars-of-oop)
4. [Classes and Objects](#4-classes-and-objects)
5. [Memory Model: Stack vs Heap](#5-memory-model-stack-vs-heap)
6. [Fields, Methods and Local Variables](#6-fields-methods-and-local-variables)
7. [The `this` Keyword](#7-the-this-keyword)
8. [Java Is Pass-by-Value](#8-java-is-pass-by-value)
9. [Static vs Instance Members](#9-static-vs-instance-members)
10. [Primitives and Wrapper Classes](#10-primitives-and-wrapper-classes)
11. [Common Pitfalls](#11-common-pitfalls)
12. [Interview Questions](#12-interview-questions)
13. [Interview-Style Output Questions](#13-interview-style-output-questions)
14. [Quick Cheat Sheet](#14-quick-cheat-sheet)

---

## 1. Why OOP Exists

Before OOP became dominant, most software was written **procedurally**: a list of functions operating on shared, often global, data. This worked for small programs but became hard to manage as systems grew, because:

- Data and the functions that used it were **disconnected** — a change in one place could silently break another.
- There was no natural way to model real-world entities such as a `Car`, `BankAccount`, or `Employee` as a single unit.
- Code reuse often meant **copy-pasting** rather than extending existing code.

OOP addresses these problems by:

- Binding data and behavior together into **objects**.
- Giving objects relationships such as **inheritance** and **composition**.
- Allowing software to model entities and hierarchies the way we naturally think about them.

Java is a **mostly pure** object-oriented language — almost everything lives inside a class. The **eight primitive types** are the notable exception to "everything is an object."

---

## 2. Procedural vs Object-Oriented Thinking

| Aspect | Procedural Programming | Object-Oriented Programming |
|--------|------------------------|------------------------------|
| Primary unit | Function / procedure | Object (class instance) |
| Data handling | Data and functions are separate | Data and functions are bundled together |
| Focus | "What steps need to happen?" | "What entities exist, and how do they behave?" |
| Reusability | Through function calls | Through inheritance, composition, interfaces |
| Data security | Data is often global/exposed | Data can be hidden via encapsulation |
| Example language | C | Java, C++, Python (multi-paradigm) |

### 2.1 The same problem, two paradigms

**Procedural style** — data and logic are separate:

```java
// Data
String name = "Aashish";
double balance = 5000.0;

// Function operating on external data
static void deposit(double currentBalance, double amount) {
    System.out.println("New balance: " + (currentBalance + amount));
}
```

**Object-oriented style** — data and logic are bundled together:

```java
class BankAccount {
    private String name;
    private double balance;

    BankAccount(String name, double balance) {
        this.name = name;
        this.balance = balance;
    }

    void deposit(double amount) {
        balance += amount;
        System.out.println(name + "'s new balance: " + balance);
    }
}
```

The OOP version is **self-contained**: code outside `BankAccount` cannot directly modify the `private` `balance` field.

---

## 3. The Four Pillars of OOP

| Pillar | One-line definition | Java mechanism |
|--------|----------------------|-----------------|
| **Encapsulation** | Hiding internal state and requiring interaction through well-defined methods | `private` fields + public getters/setters |
| **Inheritance** | Acquiring properties and behavior from another class | `extends` keyword |
| **Polymorphism** | One interface, many forms — the same method call behaves differently depending on the object | Method overloading (compile-time) & overriding (runtime) |
| **Abstraction** | Exposing only essential details while hiding implementation complexity | `abstract` classes and interfaces |

### 3.1 Memory aid: E-I-P-A

Think of a car:

- **E — Encapsulation:** The dashboard hides the complex wiring and internal state.
- **I — Inheritance:** A sports car **is a** car.
- **P — Polymorphism:** The same action (pressing the pedal) behaves differently depending on the object (a bicycle vs a car).
- **A — Abstraction:** You press the accelerator without needing to know how the engine internally works.

`E-I-P-A` → Encapsulation, Inheritance, Polymorphism, Abstraction.

### 3.2 A quick taste of each pillar in code

```java
// Encapsulation
class Account {
    private double balance;                 // hidden state
    public double getBalance() { return balance; }   // controlled access
}

// Inheritance
class Vehicle { void move() { System.out.println("moving"); } }
class Car extends Vehicle { }               // Car IS-A Vehicle

// Polymorphism (overriding — runtime)
class Shape { double area() { return 0; } }
class Circle extends Shape {
    double radius;
    @Override double area() { return Math.PI * radius * radius; }
}

// Polymorphism (overloading — compile-time)
class Calculator {
    int add(int a, int b) { return a + b; }
    double add(double a, double b) { return a + b; }
}

// Abstraction
interface Payable { void pay(double amount); }   // essential detail only: "you can pay"
```

Each pillar is covered in its own dedicated depth elsewhere; this file focuses on the **foundational** concepts — classes, objects, memory, and `this` — that the other three pillars are built on top of.

---

## 4. Classes and Objects

### 4.1 Class — a blueprint

A **class** is a blueprint or template. It defines:

- What **fields/state** its objects will have.
- What **methods/behavior** its objects will have.

A class itself is a **type definition** — it does not hold separate instance data for each object.

```java
class Student {
    // Fields (instance variables)
    String name;
    int rollNumber;
    double cgpa;

    // Method (behavior)
    void displayInfo() {
        System.out.println(name + " (" + rollNumber + ") — CGPA: " + cgpa);
    }
}
```

### 4.2 Object — a concrete instance

An **object** is a concrete instance of a class, typically created with the `new` keyword.

```java
public class Main {
    public static void main(String[] args) {
        Student s1 = new Student();
        s1.name = "Aashish";
        s1.rollNumber = 101;
        s1.cgpa = 8.76;

        Student s2 = new Student();
        s2.name = "Dipesh";
        s2.rollNumber = 102;
        s2.cgpa = 8.40;

        s1.displayInfo();   // Aashish (101) — CGPA: 8.76
        s2.displayInfo();   // Dipesh (102) — CGPA: 8.40
    }
}
```

**Key idea:** `s1` and `s2` are two **independent** objects of the same class. They share the same blueprint but hold different data.

```text
Student class
    │
    ├── s1 → name: Aashish, rollNumber: 101, cgpa: 8.76
    │
    └── s2 → name: Dipesh, rollNumber: 102, cgpa: 8.40
```

### 4.3 A class can exist without any objects

Defining a class only creates a type/blueprint — it does not require an instance.

```java
class Student {
    String name;
}
// No Student object exists yet.

Student s1 = new Student();   // now one exists
```

Utility classes containing only `static` members (like `Math`) are often **never instantiated** at all.

---

## 5. Memory Model: Stack vs Heap

A simplified mental model:

- A **reference variable** (`s1`, `s2`) lives on the **stack** when it is a local variable.
- The **object** and its instance fields are stored on the **heap**.
- The reference variable holds a **reference** to the object, not the object itself.

```text
Stack                 Heap
┌────────┐            ┌─────────────────────┐
│  s1  ──┼───────────▶│ name: "Aashish"     │
└────────┘            │ rollNumber: 101     │
                       │ cgpa: 8.76          │
                       └─────────────────────┘
```

> This is a simplified conceptual model — actual JVM memory behavior (escape analysis, generational GC, etc.) is more nuanced, but this model is enough for everyday reasoning.

### 5.1 Object references — aliasing

```java
Student s3 = s1;
```

No new `Student` object is created. Both `s1` and `s3` refer to the **same** object.

```text
Stack                 Heap
┌────────┐            ┌─────────────────────┐
│  s1  ──┼────────┐    │ Student object      │
└────────┘        ├───▶│ name: "Aashish"     │
┌────────┐        │    │ rollNumber: 101     │
│  s3  ──┼────────┘    │ cgpa: 8.76          │
└────────┘            └─────────────────────┘
```

Therefore:

```java
s3.name = "Rahul";
System.out.println(s1.name);   // "Rahul" — visible through s1 too, same underlying object
```

### 5.2 Where reference variables live

| Reference variable is a... | Stored in |
|------------------------------|-----------|
| Local variable in a method | Stack |
| Instance field of another object | Heap (as part of that object) |
| Static field | Method area / heap (implementation detail, conceptually "class-level") |

---

## 6. Fields, Methods and Local Variables

### 6.1 Fields (instance / member variables)

Fields represent an object's **state**.

```java
class Student {
    String name;       // state
    int rollNumber;    // state
    double cgpa;       // state
}
```

### 6.2 Methods

Methods represent an object's **behavior** and typically operate on its fields.

```java
void displayInfo() {
    System.out.println(name + " (" + rollNumber + ")");
}
```

### 6.3 Local variables

Local variables exist inside methods, constructors, or blocks, and are separate from instance fields.

```java
void calculate() {
    int result = 10 + 20;   // local variable — exists only within this method's scope
}
```

### 6.4 Instance variable vs local variable

| Feature | Instance Variable | Local Variable |
|---------|--------------------|-----------------|
| Declared | Inside class, outside methods | Inside method/constructor/block |
| Belongs to | Object instance | Method/block execution |
| Default value | Yes (`0`, `null`, `false`, ...) | No |
| Must initialize before use? | No | Yes |
| Lifetime | Generally tied to object lifetime | Until scope/block exits |

```java
class Student {
    // Instance variable — gets a default value automatically
    String name;

    void display() {
        // Local variable — must be initialized before use
        int age = 20;
        System.out.println(name);   // fine, defaults to null if never set
        System.out.println(age);
    }
}
```

---

## 7. The `this` Keyword

`this` is a reference to the **current object instance**.

### 7.1 The problem `this` solves: name shadowing

When a constructor (or method) parameter has the **same name** as an instance field, the parameter **shadows** the field within that scope.

```java
class Student {
    String name;
    int rollNumber;

    Student(String name, int rollNumber) {
        this.name = name;              // this.name = instance field
        this.rollNumber = rollNumber;  // name/rollNumber (right side) = parameters
    }
}
```

Without `this`:

```java
Student(String name, int rollNumber) {
    name = name;             // both sides refer to the PARAMETER
    // the instance field never gets set — stays at its default (null)
}
```

### 7.2 Other uses of `this`

```java
// 1. Refer to the current object's field
this.name = name;

// 2. Invoke another constructor of the same class ("constructor chaining")
class Student {
    String name;
    int rollNumber;

    Student() {
        this("Unknown", 0);          // calls the constructor below
    }

    Student(String name, int rollNumber) {
        this.name = name;
        this.rollNumber = rollNumber;
    }
}

// 3. Pass the current object as an argument
someMethod(this);

// 4. Return the current object (method chaining / builder pattern)
class Builder {
    Builder setName(String name) {
        this.name = name;
        return this;
    }
}
```

`this(...)` must be the **first statement** in a constructor, and a constructor cannot call itself directly or indirectly (that would be a compile error: recursive constructor invocation).

---

## 8. Java Is Pass-by-Value

Java is **strictly pass-by-value** — always. When you pass an object to a method, the **value of the reference** is copied, not the object itself.

```java
class Student { String name; }

static void changeName(Student s) {
    s.name = "Rahul";     // modifies the object THROUGH the copied reference
}

Student student = new Student();
student.name = "Aashish";
changeName(student);
System.out.println(student.name);   // "Rahul" — the object's state changed
```

But **reassigning the parameter** does not change the caller's reference:

```java
static void changeReference(Student s) {
    s = new Student();     // s now points to a brand-new object, locally
    s.name = "Rahul";
}

changeReference(student);
System.out.println(student.name);   // still "Rahul" from before, unaffected by the new object
```

### 8.1 The key idea, visually

```text
Caller
   │
   │ reference value
   ▼
┌──────────────┐
│   Student    │
│ name: Aashish│
└──────────────┘
   ▲
   │
   │ copied reference value
   │
Method
```

**Summary:** Java always uses pass-by-value. For objects, the *value being passed* happens to be a copy of the reference. A method can therefore **mutate** the object's state through that copied reference, but it can never make the **caller's** variable point to a different object just by reassigning its own local parameter.

This is the same rule that governs arrays (see the arrays notes): the method can modify elements but cannot make the caller's array variable point elsewhere.

---

## 9. Static vs Instance Members

### 9.1 Why an instance method needs an object

Instance methods operate on instance state (`this.name`, `this.age`), and that state belongs to a specific object. So calling one requires an object to operate on:

```java
Student s = new Student();
s.displayInfo();          // needs a specific Student's data
```

### 9.2 Why a static method does not

A `static` method belongs to the **class**, not to any particular object, so it can be called without creating an instance:

```java
Math.sqrt(9);              // no Math object was created
```

```text
Instance method                 Static method
      │                              │
      ▼                              ▼
Object required                Class-level
      │                              │
      ▼                              ▼
Instance state available       No particular object required
```

### 9.3 Quick comparison

| | Instance member | Static member |
|---|------------------|----------------|
| Belongs to | Each object | The class itself |
| Access | `object.member` | `ClassName.member` (or `object.member`, discouraged) |
| Can use `this`? | Yes | No |
| Memory | One copy per object | One copy shared across all objects |
| Typical use | An entity's own state/behavior | Utility logic, shared counters, constants |

```java
class Counter {
    static int totalObjects = 0;   // shared across ALL Counter objects
    int id;                        // unique per object

    Counter() {
        totalObjects++;
        id = totalObjects;
    }
}

Counter c1 = new Counter();
Counter c2 = new Counter();
System.out.println(Counter.totalObjects);   // 2
System.out.println(c1.id + " " + c2.id);    // 1 2
```

---

## 10. Primitives and Wrapper Classes

Java is called a **mostly pure** object-oriented language because almost everything is modeled as classes and objects — you cannot declare a regular method or variable directly outside a class. The main exception is the **eight primitive types**, which are **not** objects:

```text
byte  short  int  long  float  double  char  boolean
```

Java provides a **wrapper class** for each primitive, for situations where an object is required (collections, generics, nullability):

| Primitive | Wrapper |
|-----------|---------|
| `byte`    | `Byte` |
| `short`   | `Short` |
| `int`     | `Integer` |
| `long`    | `Long` |
| `float`   | `Float` |
| `double`  | `Double` |
| `char`    | `Character` |
| `boolean` | `Boolean` |

```java
int primitive = 5;
Integer wrapped = 5;          // autoboxing
int back = wrapped;           // auto-unboxing

List<Integer> nums = new ArrayList<>();   // generics require an object type, not "int"
nums.add(10);                             // autoboxed to Integer
```

---

## 11. Common Pitfalls

| # | Pitfall | Fix |
|---|---------|-----|
| 1 | Forgetting `this` when a parameter shadows a field | Always use `this.field = field` in constructors/setters when names match |
| 2 | Assuming `s2 = s1` creates a copy of the object | It only copies the **reference** — both point to the same object |
| 3 | Expecting a method to change the caller's reference by reassigning the parameter | Only element/field mutation through the reference is visible to the caller |
| 4 | Treating primitives as objects (`5.equals(5)`) | Only wrapper types have methods; primitives don't |
| 5 | Thinking Java has pass-by-reference | It is always pass-by-value; for objects, the value happens to be a reference |
| 6 | Calling an instance method without an object (conceptually) | Instance methods always need `this`/an object; only `static` methods don't |
| 7 | Confusing a class with an object | A class is a blueprint; an object is a runtime instance created via `new` |
| 8 | Forgetting fields get default values, but local variables don't | Uninitialized local variables cause a compile error if used |
| 9 | Calling `this(...)` after other statements in a constructor | `this(...)` must be the **first** statement |
| 10 | Making everything `public` (skipping encapsulation) | Keep fields `private`, expose behavior through methods |

---

## 12. Interview Questions

**Q1. What is the difference between a class and an object?**
A class is a logical blueprint/template that defines fields and methods. An object is a runtime instance of a class, created using `new`, that contains actual values for those fields. You can create many objects from a single class:

```java
Student s1 = new Student();
Student s2 = new Student();
Student s3 = new Student();
```

All three are independent instances of the `Student` class.

---

**Q2. Why is Java called a mostly "pure" object-oriented language, and what's the exception?**
Almost everything in Java is modeled using classes and objects; you cannot declare a regular method or variable outside a class. The exception is the eight primitive types (`byte`, `short`, `int`, `long`, `float`, `double`, `char`, `boolean`), which are not objects. Wrapper classes (`Integer`, `Double`, ...) exist for when an object form is needed.

---

**Q3. What are the four pillars of OOP?**
**Encapsulation** (hiding internal state behind controlled methods), **Inheritance** (acquiring properties/behavior from another class), **Polymorphism** (one interface, multiple behaviors), and **Abstraction** (exposing essential details, hiding implementation complexity). Mnemonic: E-I-P-A.

---

**Q4. What is the difference between procedural programming and OOP?**
Procedural programming structures a program as a sequence of functions operating on (often separate/global) data. OOP bundles data and the functions that operate on it into objects. OOP generally improves modularity, reusability, maintainability, data encapsulation, and domain modeling.

---

**Q5. Where are objects stored in memory — stack or heap?**
The object itself is stored on the **heap**. A reference variable pointing to it is stored on the **stack** if it's a local variable, or as part of another object on the heap if it's an instance field. (This is a simplified conceptual model; actual JVM memory management is more nuanced.)

---

**Q6. What happens when you assign one object reference to another?**
No new object is created — the second variable simply refers to the **same** underlying object as the first.

```java
Student s2 = s1;
s2.name = "Rahul";
System.out.println(s1.name);   // "Rahul" — same object
```

---

**Q7. What is the `this` keyword, and why is it needed?**
`this` is a reference to the current object instance. Its most common use is resolving ambiguity between an instance field and a parameter with the same name (`this.name = name;`). Other uses: `this(...)` for constructor chaining, passing `this` as an argument, and `return this;` for method chaining.

---

**Q8. Can a class exist without any objects being created from it?**
Yes. Defining a class creates a type/blueprint; it doesn't require an instance. Utility classes with only `static` members are often never instantiated at all.

---

**Q9. What is the difference between an instance variable and a local variable?**
An instance variable is declared inside a class (outside any method), belongs to the object, gets a default value automatically, and generally lives as long as the object does. A local variable is declared inside a method/constructor/block, belongs to that execution, has no default value (must be initialized before use), and exists only until its scope ends.

---

**Q10. Why can't Java have instance methods without an object, but can have static methods?**
Instance methods operate on instance state (`this.field`), which belongs to a specific object — so they need an object to run against. A `static` method belongs to the class itself, not to any particular object, so it can be invoked without creating an instance (e.g., `Math.sqrt(9)`).

---

**Q11. What is meant by "objects model real-world entities"?**
OOP encourages software models to represent domain concepts directly — a `Student` class with `name`, `rollNumber`, `cgpa` fields and a `displayInfo()` method represents an actual student from the problem domain, which makes the code structure easier to relate to the real world.

---

**Q12. Is Java pass-by-value or pass-by-reference?**
Java is strictly **pass-by-value**. When passing an object, the *value of the reference* is copied. This lets a method mutate the object's state through that copied reference, but reassigning the parameter inside the method never changes what the caller's own variable points to.

---

## 13. Interview-Style Output Questions

Try to predict each output before reading the answer.

**Q1**
```java
class Student { String name; }

Student s1 = new Student();
s1.name = "Aashish";
Student s2 = s1;
s2.name = "Rahul";
System.out.println(s1.name);
```
**Answer:** `Rahul`. `s2 = s1` copies the reference, not the object; both point to the same `Student`.

---

**Q2**
```java
class Student {
    String name;
    Student(String name) {
        name = name;      // no "this"
    }
}

Student s = new Student("Aashish");
System.out.println(s.name);
```
**Answer:** `null`. Both sides of `name = name` refer to the parameter; the instance field is never assigned and keeps its default value.

---

**Q3**
```java
static void changeReference(Student s) {
    s = new Student();
    s.name = "New";
}

Student student = new Student();
student.name = "Original";
changeReference(student);
System.out.println(student.name);
```
**Answer:** `Original`. Reassigning the parameter `s` inside the method has no effect on the caller's `student` variable.

---

**Q4**
```java
static void changeField(Student s) {
    s.name = "Changed";
}

Student student = new Student();
student.name = "Original";
changeField(student);
System.out.println(student.name);
```
**Answer:** `Changed`. This time the method mutates the object **through** the copied reference, which is visible to the caller.

---

**Q5**
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
**Answer:** `3`. `static` fields are shared across all instances, not reset per object.

---

**Q6**
```java
class Box {
    int value;
    Box(int value) {
        this.value = value;
    }
    Box() {
        this(10);
    }
}

Box b = new Box();
System.out.println(b.value);
```
**Answer:** `10`. The no-arg constructor delegates to the parameterized one via `this(10)`.

---

**Q7**
```java
class Student { int rollNumber; }

Student s = new Student();
System.out.println(s.rollNumber);
```
**Answer:** `0`. Instance fields get default values automatically (unlike local variables).

---

## 14. Quick Cheat Sheet

```text
OOP            Organizes code around objects that bundle STATE + BEHAVIOR
FOUR PILLARS   Encapsulation - Inheritance - Polymorphism - Abstraction   (E-I-P-A)

CLASS          Blueprint / template — defines fields + methods, holds no per-object data itself
OBJECT         new ClassName(...)  — a runtime instance with its own copy of instance fields

FIELD          Instance variable — object's state, gets a default value automatically
METHOD         Object's behavior, usually operates on its own fields
LOCAL VAR      Inside a method/block; no default value, must be initialized before use

MEMORY         Object            -> heap
               Local reference   -> stack
               s2 = s1           -> copies the REFERENCE only (aliasing), not the object

this           Reference to the current object
               this.field = param;    resolve name shadowing
               this(args);            constructor chaining (must be first statement)
               someMethod(this);      pass current object
               return this;           method chaining / builder pattern

STATIC         Belongs to the CLASS — one copy shared by all objects, no "this", callable without an instance
INSTANCE       Belongs to each OBJECT — needs "this"/an object to operate on

PASS-BY-VALUE  Java always copies the value being passed.
               For objects: the copied value is a REFERENCE.
               -> method CAN mutate the object's state through it
               -> method CANNOT redirect the caller's variable to a new object

PRIMITIVES     byte short int long float double char boolean  — NOT objects
WRAPPERS       Byte Short Integer Long Float Double Character Boolean — object form via autoboxing
```

**Remember:**

1. A class is a blueprint; an object is an instance — many objects can share one class.
2. Assigning one reference to another (`s2 = s1`) aliases the same object; it never copies it.
3. `this` exists mainly to resolve field/parameter name collisions, plus constructor/method chaining.
4. Java is always pass-by-value — for objects, the value passed is a copy of the reference.
5. Instance members need an object (`this`); static members belong to the class and don't.
6. Primitives are the one exception to "everything is an object" in Java; wrapper classes bridge the gap.
