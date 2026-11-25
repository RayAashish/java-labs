# Java Constructors: Detailed Notes

A **constructor** is a special block of code, tied to a class, that runs when an object is created with `new`. Its job is to put the new object into a valid initial **state**.

```java
class Student {
    String name;
    int rollNumber;

    Student(String name, int rollNumber) {   // constructor
        this.name = name;
        this.rollNumber = rollNumber;
    }
}

Student s = new Student("Aashish", 101);     // constructor runs here
```

---

## Table of Contents

1. [What Makes a Constructor a Constructor](#1-what-makes-a-constructor-a-constructor)
2. [Default Constructor vs Parameterized Constructor](#2-default-constructor-vs-parameterized-constructor)
3. [Constructor Overloading](#3-constructor-overloading)
4. [Constructor Chaining with this(...)](#4-constructor-chaining-with-this)
5. [Copy Constructors](#5-copy-constructors)
6. [Order of Initialization](#6-order-of-initialization)
7. [Constructors and Inheritance (super)](#7-constructors-and-inheritance-super)
8. [Private Constructors and Other Special Uses](#8-private-constructors-and-other-special-uses)
9. [Common Pitfalls](#9-common-pitfalls)
10. [Interview Questions](#10-interview-questions)
11. [Interview-Style Output Questions](#11-interview-style-output-questions)
12. [Quick Cheat Sheet](#12-quick-cheat-sheet)

---

## 1. What Makes a Constructor a Constructor

A constructor is **not** a regular method:

| Feature | Constructor | Method |
|---------|-------------|--------|
| Name | Must be **exactly the class name** | Any valid identifier |
| Return type | **None at all** — not even `void` | Required (`void` if nothing is returned) |
| Called | Implicitly, via `new ClassName(...)` | Explicitly, via `object.method(...)` |
| Purpose | Initialize a new object's state | Perform behavior/logic on an existing object |
| Inherited? | **No** — a subclass does not inherit its parent's constructors | Yes (unless `private`/`static`) |
| Can be `abstract`, `final`, `static`, `synchronized`? | No | Yes (with restrictions) |
| Overloading | Supported | Supported |

```java
class Student {
    String name;

    Student(String name) {         // constructor: name matches class name, no return type
        this.name = name;
    }

    void Student(String name) {    // NOT a constructor — this is a regular method
        this.name = name;          // (has a return type "void", even though the name matches)
    }
}
```

Every constructor implicitly returns the newly created object once it finishes — that's handled by `new`, not by a `return` statement inside the constructor body. A bare `return;` (no value) is legal inside a constructor to exit early, but `return someValue;` is a compile error.

---

## 2. Default Constructor vs Parameterized Constructor

### 2.1 The compiler-supplied default constructor

If a class declares **no constructors at all**, the compiler automatically inserts a **default constructor**: a public, no-argument constructor with an empty body (it only implicitly calls `super()`).

```java
class Student {
    String name;
    int rollNumber;
    // no constructor written
}

Student s = new Student();     // compiler-provided default constructor runs
System.out.println(s.name);        // null
System.out.println(s.rollNumber);  // 0
```

This is equivalent to the compiler generating:

```java
class Student {
    String name;
    int rollNumber;

    Student() {          // compiler-generated
        super();          // implicit call to Object's constructor
    }
}
```

### 2.2 The moment you write ANY constructor, the default disappears

As soon as you define **any** constructor yourself, the compiler **stops** providing the no-arg default. If you still want a no-arg constructor, you must write it explicitly.

```java
class Student {
    String name;

    Student(String name) {          // you wrote a constructor
        this.name = name;
    }
}

Student s = new Student();          // COMPILE ERROR: no matching constructor Student()
```

Fix:

```java
class Student {
    String name;

    Student() { }                       // now written explicitly

    Student(String name) {
        this.name = name;
    }
}
```

### 2.3 Parameterized constructor

A **parameterized constructor** accepts arguments and uses them to initialize fields directly, avoiding the need to set fields one by one after creation.

```java
class Student {
    String name;
    int rollNumber;
    double cgpa;

    Student(String name, int rollNumber, double cgpa) {
        this.name = name;
        this.rollNumber = rollNumber;
        this.cgpa = cgpa;
    }
}

Student s1 = new Student("Aashish", 101, 8.76);   // one line, fully initialized
```

Compare with the field-by-field style, which allows an object to sit **partially initialized** in between:

```java
Student s1 = new Student();   // name=null, rollNumber=0, cgpa=0.0  (temporarily invalid state)
s1.name = "Aashish";
s1.rollNumber = 101;
s1.cgpa = 8.76;
```

### 2.4 "Default constructor" vs "no-arg constructor" — a naming nuance

Strictly, **"default constructor"** refers only to the one the **compiler** inserts when you write none. A no-argument constructor that **you** write yourself is technically a **"no-arg constructor"**, not a "default constructor" — though in casual conversation people often use the two terms interchangeably.

```java
class Student {
    Student() { System.out.println("no-arg constructor, written by me"); }
    // This is NOT "the default constructor" in the strict sense, even though it takes no arguments —
    // the moment you write it yourself, it's just an overload you provided.
}
```

---

## 3. Constructor Overloading

A class can have **multiple constructors**, as long as their **parameter lists differ** (in number, type, or order) — the same rule that governs method overloading.

```java
class Student {
    String name;
    int rollNumber;
    double cgpa;

    Student() {                                   // no-arg
        this("Unknown", 0, 0.0);
    }

    Student(String name) {                        // one arg
        this(name, 0, 0.0);
    }

    Student(String name, int rollNumber) {         // two args
        this(name, rollNumber, 0.0);
    }

    Student(String name, int rollNumber, double cgpa) {  // three args
        this.name = name;
        this.rollNumber = rollNumber;
        this.cgpa = cgpa;
    }
}

Student a = new Student();
Student b = new Student("Aashish");
Student c = new Student("Aashish", 101);
Student d = new Student("Aashish", 101, 8.76);
```

### 3.1 Overload resolution follows the same rules as methods

The compiler picks the **most specific applicable** constructor at compile time, based on the number and types of the arguments.

```java
class Box {
    Box(int x) { System.out.println("int version"); }
    Box(double x) { System.out.println("double version"); }
}

new Box(5);        // "int version"    (exact match)
new Box(5.0);      // "double version" (exact match)
new Box('a');      // "int version"    (char widens to int before double)
```

### 3.2 Ambiguous overloads don't compile

```java
class Box {
    Box(int a, long b) { }
    Box(long a, int b) { }
}

new Box(5, 5);     // COMPILE ERROR: ambiguous — both need one widening conversion (int -> long)
```

### 3.3 Why overload constructors at all

Overloaded constructors give callers **flexible, readable ways** to create an object depending on what information they have available, without forcing every caller to supply every field.

```java
// A caller who doesn't yet know the cgpa can still create a valid, meaningful object:
Student fresher = new Student("Rahul", 205);
```

---

## 4. Constructor Chaining with `this(...)`

**Constructor chaining** means one constructor calls **another constructor of the same class**, using `this(...)`, so that shared initialization logic lives in **one place**.

```java
class Student {
    String name;
    int rollNumber;
    double cgpa;

    Student() {
        this("Unknown", 0, 0.0);          // delegates to the 3-arg constructor
    }

    Student(String name, int rollNumber) {
        this(name, rollNumber, 0.0);      // delegates to the 3-arg constructor
    }

    Student(String name, int rollNumber, double cgpa) {
        this.name = name;
        this.rollNumber = rollNumber;
        this.cgpa = cgpa;
        System.out.println("Fully initializing: " + name);
    }
}

new Student();                    // prints: Fully initializing: Unknown
new Student("Aashish", 101);       // prints: Fully initializing: Aashish
```

### 4.1 Hard rules for `this(...)`

1. `this(...)` must be the **first statement** in the constructor body.
2. Only **one** `this(...)` call is allowed per constructor.
3. A constructor **cannot** call itself, directly or indirectly (no recursive constructor invocation) — the compiler detects and rejects this.
4. `this(...)` and `super(...)` **cannot both appear** in the same constructor (only one "first statement" slot exists, and both fight for it).

```java
class Student {
    Student() {
        System.out.println("before");
        this("X");                 // COMPILE ERROR: this() must be the first statement
    }
    Student(String name) { }
}
```

```java
class Student {
    Student() { this(); }           // COMPILE ERROR: recursive constructor invocation
}
```

### 4.2 Why chain constructors at all

- Avoids **duplicating** initialization logic across overloads.
- Creates a single "source of truth" constructor that actually sets every field; the others just supply defaults and delegate.
- Makes it easy to add validation once, in the constructor everyone eventually funnels into.

```java
class Student {
    String name;
    int rollNumber;

    Student(String name, int rollNumber) {
        if (rollNumber < 0) {
            throw new IllegalArgumentException("rollNumber cannot be negative");
        }
        this.name = name;
        this.rollNumber = rollNumber;
    }

    Student(String name) {
        this(name, 0);      // validation above runs no matter which constructor is used
    }
}
```

---

## 5. Copy Constructors

Unlike **C++**, Java has **no built-in copy constructor syntax** — there's no automatic `ClassName(const ClassName&)` generated for you. The **pattern**, however, is easy to write by hand: a constructor that takes **another object of the same class** and copies its fields into the new object.

```java
class Student {
    String name;
    int rollNumber;
    double cgpa;

    Student(String name, int rollNumber, double cgpa) {
        this.name = name;
        this.rollNumber = rollNumber;
        this.cgpa = cgpa;
    }

    // Copy constructor pattern
    Student(Student other) {
        this.name = other.name;
        this.rollNumber = other.rollNumber;
        this.cgpa = other.cgpa;
    }
}

Student original = new Student("Aashish", 101, 8.76);
Student copy = new Student(original);   // a genuinely separate object

copy.name = "Rahul";
System.out.println(original.name);      // "Aashish" — unaffected, unlike s2 = s1 aliasing
```

### 5.1 Copy constructor vs plain reference assignment

```java
Student a = new Student("Aashish", 101, 8.76);

Student aliasOfA = a;                 // NOT a copy — same object, two names
Student realCopy = new Student(a);    // an actual, independent copy
```

### 5.2 Shallow copy vs deep copy

The copy constructor shown above is a **shallow copy**: reference-type fields are copied as **references**, so the new object and the original still point to the **same** nested objects.

```java
class Address {
    String city;
    Address(String city) { this.city = city; }
}

class Student {
    String name;
    Address address;             // reference-type field

    Student(String name, Address address) {
        this.name = name;
        this.address = address;
    }

    Student(Student other) {     // shallow copy constructor
        this.name = other.name;
        this.address = other.address;    // SAME Address object is shared!
    }
}

Student original = new Student("Aashish", new Address("Bengaluru"));
Student copy = new Student(original);

copy.address.city = "Mumbai";
System.out.println(original.address.city);   // "Mumbai" — changed! Same Address object.
```

To make it a **deep copy**, the constructor must also copy the nested object(s):

```java
Student(Student other) {         // deep copy constructor
    this.name = other.name;
    this.address = new Address(other.address.city);   // a NEW Address object
}
```

| | Reference assignment (`b = a`) | Shallow copy constructor | Deep copy constructor |
|---|-------------------------------|---------------------------|-------------------------|
| New object created? | No | Yes | Yes |
| Primitive/`String` fields | Shared (aliased) | Copied independently | Copied independently |
| Reference-type fields | Shared (aliased) | Still **shared** (same nested object) | Copied into new nested objects too |

### 5.3 Alternatives to hand-written copy constructors

- Implementing `Cloneable` and overriding `clone()` — Java's built-in (but famously clunky and error-prone) cloning mechanism.
- A static **factory method**, e.g. `Student.copyOf(original)`, which internally does the same thing as a copy constructor but reads more clearly at the call site.
- Libraries/patterns like builders, or serialization-based deep copies for complex object graphs.

```java
static Student copyOf(Student other) {
    return new Student(other.name, other.rollNumber, other.cgpa);
}
```

---

## 6. Order of Initialization

When an object is created, Java runs initialization in a strict, well-defined order. Understanding this order explains a lot of "surprising" output in interview questions.

### 6.1 The full order (first time a class is used)

```text
1. Static variables + static initializer blocks   — run ONCE, in the order they appear,
                                                     the FIRST time the class is loaded/used
2. Instance variables + instance initializer blocks — run EVERY time an object is created,
                                                        in the order they appear
3. Constructor body                                — runs LAST, after all the above
```

And when inheritance is involved, the parent class's static/instance/constructor steps happen **before** the child's (see section 7).

### 6.2 Static blocks — run once, at class-loading time

```java
class Config {
    static int version;

    static {
        version = 1;
        System.out.println("Static block: class loaded");
    }
}

new Config();
new Config();
new Config();
// "Static block: class loaded" prints ONLY ONCE, no matter how many objects are created
```

### 6.3 Instance blocks — run every time, before the constructor body

```java
class Student {
    String name;

    { // instance initializer block
        System.out.println("Instance block running");
        name = "Default Name";
    }

    Student() {
        System.out.println("Constructor running");
    }

    Student(String name) {
        System.out.println("Parameterized constructor running");
        this.name = name;
    }
}

new Student();
// Instance block running
// Constructor running

new Student("Aashish");
// Instance block running
// Parameterized constructor running
```

**Key point:** the instance block runs before **every** constructor, regardless of which overload is called — it is effectively copy-pasted to the top of each constructor's body (right after any implicit/explicit `super()` call).

### 6.4 Full demonstration: static → instance → constructor

```java
class Demo {
    static int s;
    int i;

    static {
        s = 10;
        System.out.println("1. Static block");
    }

    {
        i = 20;
        System.out.println("2. Instance block");
    }

    Demo() {
        System.out.println("3. Constructor");
    }
}

public class Main {
    public static void main(String[] args) {
        System.out.println("--- creating first object ---");
        new Demo();
        System.out.println("--- creating second object ---");
        new Demo();
    }
}
```

**Output:**

```text
1. Static block
--- creating first object ---
2. Instance block
3. Constructor
--- creating second object ---
2. Instance block
3. Constructor
```

The static block runs once, the moment the class is first loaded (here, triggered by the first `new Demo()`), while the instance block + constructor pair repeats for every object.

### 6.5 Multiple static/instance blocks run in source order

If a class has more than one static block, or more than one instance block, they execute **top to bottom**, interleaved with field initializers in the order they textually appear.

```java
class Demo {
    static int a = initA();
    static { System.out.println("static block 1"); }
    static int b = initB();
    static { System.out.println("static block 2"); }

    static int initA() { System.out.println("init a"); return 1; }
    static int initB() { System.out.println("init b"); return 2; }
}

new Demo();
// init a
// static block 1
// init b
// static block 2
```

### 6.6 Field initializers vs constructor — which "wins"

A field's inline initializer runs **before** the constructor body, so the constructor can override it.

```java
class Student {
    String name = "Default";      // field initializer

    Student() {
        System.out.println(name); // "Default" — initializer already ran
        name = "Aashish";         // constructor overrides it
    }
}
```

---

## 7. Constructors and Inheritance (`super`)

### 7.1 A subclass always invokes a superclass constructor first

Every constructor's **first action** (even if you don't write it) is a call to a superclass constructor — either an explicit `super(...)` or, if you omit it, an **implicit, invisible `super()`** (the parent's no-arg constructor).

```java
class Animal {
    Animal() { System.out.println("Animal constructor"); }
}

class Dog extends Animal {
    Dog() {
        // implicit super(); happens here automatically
        System.out.println("Dog constructor");
    }
}

new Dog();
// Animal constructor
// Dog constructor
```

### 7.2 If the parent has no no-arg constructor, you must call `super(...)` explicitly

```java
class Animal {
    String name;
    Animal(String name) { this.name = name; }   // no no-arg constructor available
}

class Dog extends Animal {
    Dog() {
        // COMPILE ERROR: implicit super() looks for Animal(), which doesn't exist
    }
}
```

Fix:

```java
class Dog extends Animal {
    Dog() {
        super("Unnamed");     // must explicitly call a matching parent constructor
    }
}
```

### 7.3 `super(...)` rules, mirroring `this(...)`

- Must be the **first statement** in the constructor, if present.
- Only **one** `super(...)` call allowed.
- Cannot appear together with `this(...)` in the same constructor.
- If neither is written, the compiler inserts `super()` automatically.

### 7.4 Full initialization order with inheritance

```text
1. Parent's static block/variables   (once, class-loading time)
2. Child's static block/variables    (once, class-loading time)
3. Parent's instance block/variables
4. Parent's constructor body
5. Child's instance block/variables
6. Child's constructor body
```

```java
class Animal {
    static { System.out.println("Animal static block"); }
    { System.out.println("Animal instance block"); }
    Animal() { System.out.println("Animal constructor"); }
}

class Dog extends Animal {
    static { System.out.println("Dog static block"); }
    { System.out.println("Dog instance block"); }
    Dog() { System.out.println("Dog constructor"); }
}

new Dog();
```

**Output:**

```text
Animal static block
Dog static block
Animal instance block
Animal constructor
Dog instance block
Dog constructor
```

This confirms the parent is **fully constructed** (instance block + constructor) before the child's own instance block or constructor body runs.

---

## 8. Private Constructors and Other Special Uses

### 8.1 Private constructors — preventing instantiation

A `private` constructor can be called only from **within the same class**, which is useful for:

**Singleton pattern:**

```java
class Singleton {
    private static final Singleton INSTANCE = new Singleton();

    private Singleton() { }             // outside code cannot call new Singleton()

    static Singleton getInstance() {
        return INSTANCE;
    }
}

Singleton s = Singleton.getInstance();
Singleton bad = new Singleton();        // COMPILE ERROR: constructor has private access
```

**Utility classes with only static members:**

```java
class MathUtils {
    private MathUtils() {
        throw new AssertionError("No instances allowed");   // extra safety, even for reflection
    }

    static int square(int x) { return x * x; }
}
```

### 8.2 Constructors cannot be `abstract`, `static`, or `final`

- `abstract` — a constructor always has a body; abstract classes still have (usable) constructors that subclasses call via `super(...)`.
- `static` — a constructor is inherently tied to instance creation, so "static" makes no sense.
- `final` — constructors are never overridden (they aren't inherited at all), so `final` is meaningless here.

```java
abstract class Shape {
    Shape() { System.out.println("Shape constructor"); }   // legal and useful
    abstract double area();
}
```

### 8.3 Constructors in `enum` types are implicitly `private`

```java
enum Level {
    LOW(1), MEDIUM(2), HIGH(3);

    final int code;
    Level(int code) {          // implicitly private — cannot be public/protected
        this.code = code;
    }
}
```

---

## 9. Common Pitfalls

| # | Pitfall | Fix |
|---|---------|-----|
| 1 | Defining a parameterized constructor and assuming `new ClassName()` still works | Add an explicit no-arg constructor if you need one |
| 2 | Giving a constructor a return type | That silently makes it a regular method, not a constructor |
| 3 | Putting `this(...)` or `super(...)` anywhere but the first line | Compile error — move it to the first statement |
| 4 | Using both `this(...)` and `super(...)` in one constructor | Not allowed — pick one |
| 5 | Assuming Java auto-generates a copy constructor like C++ | Write your own copy constructor, `clone()`, or a factory method |
| 6 | Writing a copy constructor that shallow-copies mutable reference fields | Explicitly copy nested objects too, for a true deep copy |
| 7 | Expecting a static block to re-run for every `new` | Static blocks run once per class load, not once per object |
| 8 | Forgetting the instance block runs before **every** constructor overload | Don't rely on a specific constructor "skipping" it |
| 9 | Assuming a subclass inherits the parent's constructors | Constructors are never inherited; subclasses must define their own and call `super(...)` |
| 10 | Forgetting to call `super(...)` explicitly when the parent has no no-arg constructor | Add a matching explicit `super(...)` call |
| 11 | Doing heavy/risky work (I/O, throwing checked exceptions carelessly) inside a constructor | Prefer simple, fast, defensive initialization; consider a factory method for complex setup |
| 12 | Calling an overridable instance method from a constructor | The subclass override may run before the subclass's own fields are initialized — surprising bugs |

### 9.1 Pitfall #12 in detail: calling overridable methods from a constructor

```java
class Parent {
    Parent() {
        init();          // dangerous: calls an overridable method during construction
    }
    void init() { System.out.println("Parent init"); }
}

class Child extends Parent {
    int value = 5;
    @Override
    void init() {
        System.out.println("Child init, value = " + value);
    }
}

new Child();
// Parent init  ... is NOT what actually happens!
```

**Actual output:**

```text
Child init, value = 0
```

Because `Parent()`'s call to `init()` dispatches **polymorphically** to `Child`'s override, but this happens **before** `Child`'s own field initializer (`value = 5`) has run — so `value` is still its default, `0`. This is a classic, subtle bug; avoid calling overridable methods from constructors.

---

## 10. Interview Questions

**Q1. What is a constructor, and how does it differ from a method?**
A constructor initializes a new object and is invoked implicitly via `new`. It must share the class's exact name and has **no return type at all** (not even `void`). Unlike methods, constructors are not inherited, and cannot be `abstract`, `static`, or `final`.

---

**Q2. What is a default constructor?**
The no-argument constructor the **compiler** automatically generates only if the class defines **no constructors of its own**. The instant you write any constructor yourself, the compiler stops providing this default, and you must add a no-arg one explicitly if you still want one.

---

**Q3. What is constructor overloading?**
Defining multiple constructors in the same class with different parameter lists (different number, type, or order of parameters), so objects can be created in multiple ways depending on what data the caller has available.

---

**Q4. What is constructor chaining, and what keyword enables it within a class?**
One constructor calling another constructor of the **same class** using `this(...)`, so shared initialization logic lives in one place. `this(...)` must be the first statement, and only one is allowed per constructor.

---

**Q5. Does Java have a built-in copy constructor like C++?**
No. Java has no automatically generated copy constructor. The pattern is achieved manually — writing a constructor that takes another instance of the same class and copies its field values, with the developer deciding whether that copy is shallow (reference fields shared) or deep (nested objects also duplicated).

---

**Q6. What is the difference between a shallow copy and a deep copy?**
A shallow copy duplicates the top-level object but lets reference-type fields still point to the **same** nested objects as the original. A deep copy also creates new, independent copies of those nested objects, so changes to the copy's nested state don't affect the original.

---

**Q7. What is the order of initialization when an object is created?**
Static variables/blocks run once, the first time the class is loaded, in source order. Then, every time an object is created: instance variables/instance blocks run first (in source order), followed by the constructor body. With inheritance, the entire parent-class sequence (static once, then instance+constructor) completes before the child class's instance block and constructor run.

---

**Q8. Why does a static block run only once, while an instance block runs for every object?**
A static block is tied to the **class** itself and executes at class-loading time, which happens once per JVM run (per classloader). An instance block is tied to **object creation** and is effectively prepended to every constructor's body, so it runs once per `new`.

---

**Q9. Can a subclass inherit its parent's constructors?**
No. Constructors are never inherited. A subclass must define its own constructors, and every one of them implicitly or explicitly invokes a superclass constructor via `super(...)` as its first statement.

---

**Q10. Can `this(...)` and `super(...)` be used in the same constructor?**
No. Only one "first statement" call is allowed per constructor, and both `this(...)` and `super(...)` require that position. You must choose one.

---

**Q11. Why might a private constructor be useful?**
To **prevent instantiation** from outside the class — used for singletons (where a static factory method controls the single instance) and for pure utility/static-only classes that should never be instantiated at all.

---

**Q12. Why is calling an overridable method from a constructor considered risky?**
Because the call dispatches polymorphically to the subclass's override, but that override may run **before** the subclass's own field initializers have executed, leaving it operating on default/uninitialized field values — a subtle and hard-to-spot bug.

---

## 11. Interview-Style Output Questions

**Q1**
```java
class A {
    A() { System.out.println("A()"); }
    A(int x) { System.out.println("A(int)"); }
}
new A();
new A(5);
```
**Answer:**
```
A()
A(int)
```

---

**Q2**
```java
class Box {
    Box() {
        this(10);
        System.out.println("Box()");
    }
    Box(int x) {
        System.out.println("Box(int): " + x);
    }
}
new Box();
```
**Answer:**
```
Box(int): 10
Box()
```
(`this(10)` runs first, then control returns to finish the calling constructor's body.)

---

**Q3**
```java
class Demo {
    static { System.out.println("static"); }
    { System.out.println("instance"); }
    Demo() { System.out.println("constructor"); }
}
new Demo();
new Demo();
```
**Answer:**
```
static
instance
constructor
instance
constructor
```

---

**Q4**
```java
class Parent {
    Parent() { System.out.println("Parent"); }
}
class Child extends Parent {
    Child() { System.out.println("Child"); }
}
new Child();
```
**Answer:**
```
Parent
Child
```

---

**Q5**
```java
class Student {
    String name;
    Student(Student other) {
        this.name = other.name;
    }
    Student(String name) {
        this.name = name;
    }
}
Student a = new Student("Aashish");
Student b = new Student(a);
b.name = "Rahul";
System.out.println(a.name);
```
**Answer:** `Aashish`. `b` is a genuine copy (via the copy constructor), so changing `b.name` does not affect `a`.

---

**Q6**
```java
class Parent {
    Parent() { greet(); }
    void greet() { System.out.println("Parent greet"); }
}
class Child extends Parent {
    String msg = "Hello";
    @Override void greet() { System.out.println("Child greet: " + msg); }
}
new Child();
```
**Answer:** `Child greet: null`. `Parent()`'s call to `greet()` dispatches to `Child`'s override before `Child`'s own field initializer (`msg = "Hello"`) has run.

---

**Q7**
```java
class A {
    A(int x) { System.out.println("A(int)"); }
}
class B extends A {
    B() { System.out.println("B()"); }
}
```
**Answer:** Compile error. `B()`'s implicit `super()` looks for a no-arg constructor in `A`, which doesn't exist — `B()` must explicitly call `super(someInt)`.

---

## 12. Quick Cheat Sheet

```text
CONSTRUCTOR         Same name as the class, NO return type, runs on "new", never inherited

DEFAULT CTOR        Auto-generated ONLY if you define no constructor yourself; disappears
                    the moment you write any constructor of your own

OVERLOADING         Multiple constructors, different parameter lists — same rules as method overloading

this(...)           Calls ANOTHER constructor in the SAME class
                    - must be the first statement
                    - only one allowed
                    - cannot combine with super(...)

super(...)          Calls a constructor in the PARENT class
                    - must be the first statement
                    - inserted implicitly (as super()) if you write neither this(...) nor super(...)

COPY CONSTRUCTOR    Not built into Java (unlike C++) — write it yourself:
                    ClassName(ClassName other) { this.field = other.field; ... }
                    SHALLOW: reference fields still shared     DEEP: nested objects also duplicated

INIT ORDER          static vars/blocks (ONCE, class load, source order)
                       -> instance vars/blocks (EVERY object, source order)
                          -> constructor body (LAST)
                    With inheritance: parent's whole sequence completes before child's instance
                    block/constructor run.

PRIVATE CTOR        Blocks external "new" calls — used for singletons and static-only utility classes

AVOID               Calling an overridable instance method from a constructor
                    (subclass override may run before the subclass's own fields are initialized)
```

**Remember:**

1. Writing even one constructor removes the compiler's free no-arg default — add one back explicitly if needed.
2. `this(...)` and `super(...)` both fight for the "first statement" slot — never both, never anywhere else.
3. Java has no automatic copy constructor; you write it, and you choose shallow vs deep copying.
4. Initialization always goes static → instance → constructor, and for subclasses, parent fully finishes before the child begins its own instance block/constructor.
5. Constructors are never inherited — every subclass writes its own and must reach a parent constructor via `super(...)` (explicit or implicit).
6. Never call an overridable method from a constructor — the subclass's fields may not be ready yet.
