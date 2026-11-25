# Java Polymorphism: Detailed Notes

**Polymorphism** means "many forms" — the same method call, or the same reference type, can behave differently depending on context. Java has two kinds:

```text
Compile-time (static) polymorphism   — method overloading     — resolved by the COMPILER
Runtime (dynamic) polymorphism       — method overriding       — resolved by the JVM at RUNTIME
```

```java
class Shape {
    double area() { return 0; }
}
class Circle extends Shape {
    double radius;
    @Override double area() { return Math.PI * radius * radius; }
}

Shape s = new Circle();   // reference type Shape, actual object Circle
s.area();                 // Circle's area() runs — decided at RUNTIME (dynamic polymorphism)
```

---

## Table of Contents

1. [Compile-Time (Static) Polymorphism](#1-compile-time-static-polymorphism)
2. [Overload Resolution Rules](#2-overload-resolution-rules)
3. [Ambiguous Overloads](#3-ambiguous-overloads)
4. [Runtime (Dynamic) Polymorphism](#4-runtime-dynamic-polymorphism)
5. [Dynamic Method Dispatch — How It Actually Works](#5-dynamic-method-dispatch--how-it-actually-works)
6. [The `@Override` Annotation](#6-the-override-annotation)
7. [Rules for Overriding](#7-rules-for-overriding)
8. [Return Type Covariance](#8-return-type-covariance)
9. [Access Modifier Widening](#9-access-modifier-widening)
10. [Exception Rules in Overriding](#10-exception-rules-in-overriding)
11. [Fields and Static Methods Are NOT Polymorphic](#11-fields-and-static-methods-are-not-polymorphic)
12. [Upcasting](#12-upcasting)
13. [Downcasting](#13-downcasting)
14. [`instanceof` Checks](#14-instanceof-checks)
15. [Pattern Matching for `instanceof`](#15-pattern-matching-for-instanceof)
16. [Common Pitfalls](#16-common-pitfalls)
17. [Interview Questions](#17-interview-questions)
18. [Interview-Style Output Questions](#18-interview-style-output-questions)
19. [Quick Cheat Sheet](#19-quick-cheat-sheet)

---

## 1. Compile-Time (Static) Polymorphism

Also called **method overloading**: multiple methods in the **same class** (or inherited into it) share the same name but differ in their **parameter list**. The compiler decides **which one to call** purely by looking at the arguments at the call site — before the program ever runs.

```java
class Calculator {
    int add(int a, int b) { return a + b; }
    double add(double a, double b) { return a + b; }
    int add(int a, int b, int c) { return a + b + c; }
    String add(String a, String b) { return a + b; }
}
```

```java
Calculator c = new Calculator();
c.add(2, 3);          // calls add(int, int)
c.add(2.5, 3.5);       // calls add(double, double)
c.add(1, 2, 3);        // calls add(int, int, int)
c.add("Hi", "There");  // calls add(String, String)
```

It's called "static" or "compile-time" polymorphism because the **compiler bakes the choice of method into the bytecode** based on the declared argument types — there is no decision left to make at runtime.

### 1.1 What makes two methods "overloads"

Two methods overload each other if they have the **same name** but a **different parameter list** — differing in:

- **Number** of parameters, or
- **Types** of parameters, or
- **Order** of parameter types

```java
void print(int a) { }
void print(int a, int b) { }         // different number — overload
void print(double a) { }              // different type — overload
void print(int a, String b) { }       // overload
void print(String a, int b) { }       // different order — overload
```

> Return type **alone** does not distinguish overloads — two methods with identical parameter lists but different return types do not compile.

```java
int print(int a) { return a; }
// double print(int a) { return a; }   // COMPILE ERROR: same signature, only return type differs
```

---

## 2. Overload Resolution Rules

When multiple overloads could plausibly match a call, the compiler follows a strict, phased search for the **most specific applicable** method:

```text
Phase 1: Match WITHOUT boxing/unboxing or varargs — exact type or simple widening only
Phase 2: Match allowing boxing/unboxing, but still no varargs
Phase 3: Match allowing varargs (as a last resort)
```

The compiler stops at the **first phase** that finds at least one applicable method.

### 2.1 Phase 1 — widening primitive conversions preferred

```java
class Box {
    void show(int x)    { System.out.println("int"); }
    void show(long x)    { System.out.println("long"); }
    void show(double x)  { System.out.println("double"); }
}

new Box().show(5);        // "int"     — exact match wins
new Box().show(5L);       // "long"    — exact match wins
new Box().show(5.0);      // "double"  — exact match wins
new Box().show((byte)5);  // "int"     — byte widens to int before it would ever reach long/double
```

Widening order for numeric types: `byte → short → int → long → float → double`, and `char → int`. The compiler always prefers the **smallest widening step** that makes a match.

### 2.2 Phase 2 — boxing/unboxing only considered if Phase 1 finds nothing

```java
class Box {
    void show(Integer x) { System.out.println("Integer"); }
    void show(long x)     { System.out.println("long"); }
}

new Box().show(5);   // "long" — widening (Phase 1: int -> long) is preferred over autoboxing (Phase 2: int -> Integer)
```

Even though `Integer` looks like a closer conceptual match for an `int`, **widening beats boxing** because Phase 1 is checked before Phase 2.

### 2.3 Phase 3 — varargs are the last resort

```java
class Box {
    void show(int a, int b)  { System.out.println("two ints"); }
    void show(int... nums)   { System.out.println("varargs"); }
}

new Box().show(1, 2);       // "two ints" — fixed-arity match found in Phase 1, varargs never considered
new Box().show(1, 2, 3);    // "varargs"  — only the varargs version can match three arguments
```

### 2.4 `null` arguments and overload resolution

```java
class Box {
    void show(String s) { System.out.println("String"); }
    void show(Object o)  { System.out.println("Object"); }
}

new Box().show(null);   // "String" — the MOST SPECIFIC applicable type wins when several match
```

When `null` matches multiple overloads, the compiler picks the **most specific** type among them. If two unrelated types are equally specific, the call becomes **ambiguous** (see section 3).

---

## 3. Ambiguous Overloads

If the compiler cannot determine a single **most specific** applicable method, it refuses to guess and reports a compile error.

```java
class Box {
    void show(int a, long b) { }
    void show(long a, int b) { }
}

new Box().show(5, 5);
// COMPILE ERROR: reference to show is ambiguous
// both methods require exactly one widening conversion (int->long), and neither is more specific
```

```java
class Box {
    void show(Integer a) { }
    void show(Long a) { }
}

new Box().show(null);
// COMPILE ERROR: reference to show is ambiguous
// null fits both Integer and Long, and neither is more specific than the other
```

The fix in both cases is to make the call unambiguous — either by casting the argument(s) explicitly, or by renaming/restructuring the overloads.

---

## 4. Runtime (Dynamic) Polymorphism

Also called **method overriding**: a subclass provides its own implementation of a method already defined in its superclass, using the **exact same signature**. Which implementation actually runs is decided at **runtime**, based on the object's **real type** — not the type of the reference used to call it.

```java
class Animal {
    void makeSound() { System.out.println("Some generic sound"); }
}
class Dog extends Animal {
    @Override void makeSound() { System.out.println("Woof"); }
}
class Cat extends Animal {
    @Override void makeSound() { System.out.println("Meow"); }
}
```

```java
Animal[] animals = { new Dog(), new Cat(), new Animal() };
for (Animal a : animals) {
    a.makeSound();    // the SAME line of code behaves differently per object — this IS polymorphism
}
// Woof
// Meow
// Some generic sound
```

This is "dynamic" or "runtime" polymorphism because the **JVM**, not the compiler, decides which method body to execute, and it makes that decision **fresh for every call**, based on the actual object sitting on the heap.

---

## 5. Dynamic Method Dispatch — How It Actually Works

The mechanism behind runtime polymorphism is called **dynamic method dispatch** (also "virtual method invocation" or "late binding").

### 5.1 Two different things: reference type vs object type

```java
Animal a = new Dog();
```

- **Reference type** (`Animal`): decided at **compile time**. It controls *which methods you're even allowed to call* on `a` — only `Animal`'s methods (and anything it inherits/overrides) are visible through this reference.
- **Object type** (`Dog`): the actual class of the object created on the heap. It decides *which version of an overridden method actually runs*.

```java
class Animal {
    void makeSound() { System.out.println("Generic sound"); }
}
class Dog extends Animal {
    @Override void makeSound() { System.out.println("Woof"); }
    void fetch() { System.out.println("Fetching"); }   // Dog-only method
}

Animal a = new Dog();
a.makeSound();   // "Woof" — object type (Dog) decides which override runs
// a.fetch();    // COMPILE ERROR — reference type (Animal) doesn't expose fetch()
```

### 5.2 The dispatch mechanism, conceptually

Each object effectively carries a reference to a **method table** (a "vtable," conceptually) built from its actual class. When you call an instance method through any reference, the JVM looks up the method in **that object's own table** — which always holds the **most-derived override** available for that signature — and calls it.

```text
Call:  a.makeSound()
       |
       v
JVM checks: what is the ACTUAL class of the object a points to?  -> Dog
       |
       v
JVM finds Dog's own makeSound() (since Dog overrides it) and calls THAT
```

If `Dog` did **not** override `makeSound()`, the lookup would walk up to the nearest ancestor that does define it (here, `Animal`), exactly the same way normal inheritance resolves any other inherited method.

### 5.3 Why this matters: writing code against abstractions

Dynamic dispatch is what allows code to be written against a **general type** while still getting **specific behavior**:

```java
void describe(Animal a) {
    a.makeSound();    // works correctly no matter what concrete subclass is passed in
}

describe(new Dog());   // Woof
describe(new Cat());   // Meow
```

The `describe` method never needs to know about `Dog` or `Cat` specifically — it just trusts that whatever `Animal` it receives will do the right thing when `makeSound()` is called. This is the practical payoff of polymorphism: flexible, extensible code.

---

## 6. The `@Override` Annotation

`@Override` is optional, but it is a **compiler-checked** safety net: it tells the compiler "this method is meant to override a superclass (or implemented interface) method," and the compiler then verifies that such a method genuinely exists with a matching signature.

```java
class Animal {
    void makeSound() { }
}
class Dog extends Animal {
    @Override
    void makeSond() {    // TYPO — "Sond" instead of "Sound"
    }
}
// COMPILE ERROR: method does not override a method from its superclass
```

Without `@Override`, that typo would **silently compile** as a brand-new, unrelated method that happens to live in `Dog` — `makeSound()` would still be inherited unchanged from `Animal`, and the bug would likely only surface much later as confusing runtime behavior. `@Override` turns this into an immediate, obvious compile-time error.

> Always use `@Override` on every overriding method as a matter of habit — it costs nothing and catches an entire category of silent bugs.

---

## 7. Rules for Overriding

For a subclass method to be a **valid override** of a superclass method, all of the following must hold:

1. **Same method name.**
2. **Same parameter list** (number, types, order) — this is exactly what distinguishes overriding from overloading.
3. **Same return type, or a covariant return type** (see section 8).
4. **Access modifier can stay the same or become less restrictive, never more restrictive** (see section 9).
5. **Cannot throw new or broader checked exceptions** than the overridden method declares (see section 10).
6. **Cannot override `final`, `static`, or `private` methods** — `final` locks the method, `static` methods are hidden rather than overridden, and `private` methods aren't inherited/visible at all.

```java
class Animal {
    protected Animal reproduce() throws Exception { return new Animal(); }
}
class Dog extends Animal {
    @Override
    public Dog reproduce() {                 // covariant return + widened access + no exception declared
        return new Dog();
    }
}
```

---

## 8. Return Type Covariance

Since Java 5, an overriding method is allowed to return a **subtype** of the return type declared in the overridden method — this is called a **covariant return type**.

```java
class Animal {
    Animal reproduce() { return new Animal(); }
}
class Dog extends Animal {
    @Override
    Dog reproduce() {           // Dog is a subtype of Animal — valid covariant override
        return new Dog();
    }
}
```

```java
Animal a = new Dog();
Animal baby = a.reproduce();   // dynamic dispatch calls Dog's reproduce(), which really does return a Dog
```

This lets callers working through the subtype get a **more specific, more useful** return type without needing a cast, while callers working through the supertype still see a compatible, assignable type.

```java
class Shape {
    Shape clone() { return new Shape(); }
}
class Circle extends Shape {
    @Override
    Circle clone() { return new Circle(); }   // OK — Circle IS-A Shape
}
class Square extends Shape {
    @Override
    // Rectangle clone() { }   // COMPILE ERROR if Rectangle is unrelated to Shape — must be a subtype
}
```

The return type can stay **exactly the same** too — covariance is a permitted *option*, not a requirement.

---

## 9. Access Modifier Widening

An override can **keep the same** access level as the parent method, or make it **less restrictive** — it can never make it **more** restrictive.

```text
private  <  default (package-private)  <  protected  <  public
```

```java
class Animal {
    protected void eat() { }
}
class Dog extends Animal {
    @Override
    public void eat() { }        // OK — widened from protected to public
}
```

```java
class Animal {
    public void eat() { }
}
class Dog extends Animal {
    @Override
    protected void eat() { }     // COMPILE ERROR — cannot narrow from public to protected
}
```

**Why this rule exists:** if narrowing were allowed, code holding an `Animal` reference could call `eat()` (since `Animal.eat()` is `public`), but the actual object might be a `Dog` whose `eat()` is now hidden — breaking the promise the superclass's public method made to all callers. Widening never breaks that promise; narrowing would.

---

## 10. Exception Rules in Overriding

An overriding method can declare:

```text
The SAME checked exceptions as the parent method, OR
NARROWER (subclass) checked exceptions, OR
FEWER checked exceptions, OR
NO checked exceptions at all
```

It can **never** declare **new** or **broader** checked exceptions than the method it overrides.

```java
class Animal {
    void makeSound() throws IOException { }
}

class Dog extends Animal {
    @Override
    void makeSound() throws FileNotFoundException { }   // OK — FileNotFoundException is a subtype of IOException (narrower)
}

class Cat extends Animal {
    @Override
    void makeSound() { }    // OK — declaring NO checked exception is always fine
}

class Bird extends Animal {
    @Override
    void makeSound() throws Exception { }   // COMPILE ERROR — Exception is broader than IOException
}
```

**Unchecked exceptions (`RuntimeException` and its subclasses) are not restricted at all** — an override can throw any unchecked exception freely, regardless of what the parent method declares, because callers are never forced to handle unchecked exceptions in the first place.

```java
class Animal {
    void makeSound() { }                     // declares nothing
}
class Dog extends Animal {
    @Override
    void makeSound() {
        throw new IllegalStateException();   // perfectly legal — unchecked, no restriction
    }
}
```

**Why this rule exists:** code calling `animal.makeSound()` through an `Animal` reference only wrote a `try/catch` (or `throws`) for `IOException`, as declared by `Animal`. If an override could throw a broader checked exception, that calling code could be blindsided by an exception type it never agreed to handle — the compiler forbids this entirely.

---

## 11. Fields and Static Methods Are NOT Polymorphic

Polymorphism (dynamic dispatch) applies **only to instance methods**. Fields and `static` methods are resolved at **compile time**, based on the reference's **declared type** — they do not participate in runtime polymorphism at all.

```java
class Animal {
    String type = "Animal";
    static void info() { System.out.println("Animal.info()"); }
}
class Dog extends Animal {
    String type = "Dog";                         // shadows, does not override
    static void info() { System.out.println("Dog.info()"); }   // hides, does not override
}

Animal a = new Dog();
System.out.println(a.type);   // "Animal" — field access resolved by reference TYPE
a.info();                     // "Animal.info()" — static method resolved by reference TYPE
```

This is a common source of confusion: calling `a.makeSound()` (an overridden instance method) uses the real object's type, but `a.type` (a field) and `a.info()` (a static method) both use the reference's declared type instead. Knowing this distinction is essential to predicting output correctly.

---

## 12. Upcasting

**Upcasting** is converting a reference from a **subclass type** to a **superclass type**. It is always **safe** and happens **implicitly** — no cast operator is required.

```java
class Animal { }
class Dog extends Animal { }

Dog d = new Dog();
Animal a = d;          // upcast — implicit, always safe
// equivalently: Animal a = new Dog();
```

Upcasting is safe because a `Dog` genuinely **IS-A** `Animal` — every capability an `Animal` reference needs is guaranteed to exist on a `Dog` object, since `Dog` inherits (and possibly overrides) everything `Animal` has.

### 12.1 What you lose (and don't lose) on upcast

Upcasting does **not** change the object itself — it only changes what the **reference** is allowed to see.

```java
class Animal {
    void eat() { System.out.println("Eating"); }
}
class Dog extends Animal {
    void eat() { System.out.println("Dog eating"); }   // overridden
    void fetch() { System.out.println("Fetching"); }   // Dog-only
}

Animal a = new Dog();    // upcast
a.eat();                 // "Dog eating" — dynamic dispatch still uses the REAL object's method
// a.fetch();            // COMPILE ERROR — Animal reference doesn't expose fetch(), even though the object has it
```

So: the **object** underneath is still fully a `Dog` (overridden methods still dispatch correctly), but the **compiler** only lets you call what the `Animal` type declares.

### 12.2 Why upcasting is useful

Upcasting is the basis for writing flexible, general-purpose code — accepting a supertype parameter lets a method work with any subtype:

```java
void feed(Animal a) {       // accepts ANY Animal subtype
    a.eat();
}

feed(new Dog());    // Dog is upcast to Animal automatically when passed in
feed(new Cat());
```

---

## 13. Downcasting

**Downcasting** is converting a reference from a **superclass type** back to a **subclass type**. Unlike upcasting, it is **not** automatically safe, so it requires an **explicit cast**, and it can fail at runtime.

```java
Animal a = new Dog();     // upcast, safe
Dog d = (Dog) a;          // downcast, explicit cast required
d.fetch();                // now fetch() is accessible again
```

### 13.1 Downcasting can throw `ClassCastException`

The cast only succeeds if the object being referenced is **actually** (or a subtype of) the target type. If not, the JVM throws `ClassCastException` at runtime.

```java
Animal a = new Cat();
Dog d = (Dog) a;      // compiles fine, but throws ClassCastException at RUNTIME
                       // because the real object is a Cat, not a Dog
```

```text
Exception in thread "main" java.lang.ClassCastException:
class Cat cannot be cast to class Dog
```

### 13.2 Why downcasting is sometimes necessary

Downcasting is needed when code holds a general reference but needs to use **subtype-specific** functionality that the general type doesn't expose.

```java
void process(Animal a) {
    a.eat();                       // works for any Animal
    if (a instanceof Dog) {        // check before downcasting — see section 14
        Dog d = (Dog) a;
        d.fetch();                 // Dog-specific behavior
    }
}
```

> Downcasting without first checking the object's real type (via `instanceof` or similar) is risky and is generally considered a code smell — it often signals that the design could use a better polymorphic solution (e.g., putting `fetch()`-like behavior behind an overridable method in `Animal` itself, even if most subclasses leave it a no-op).

---

## 14. `instanceof` Checks

The `instanceof` operator checks whether an object is an instance of a given class, subclass, or interface, returning a `boolean`. It is the standard safety check **before** downcasting.

```java
Animal a = new Dog();

System.out.println(a instanceof Dog);      // true
System.out.println(a instanceof Animal);   // true — a Dog IS-A Animal too
System.out.println(a instanceof Cat);      // false
System.out.println(null instanceof Dog);   // false — always false for null, no exception thrown
```

### 14.1 Safe downcasting pattern (pre-Java 16 style)

```java
void process(Animal a) {
    if (a instanceof Dog) {
        Dog d = (Dog) a;     // guaranteed safe — we already confirmed the real type
        d.fetch();
    }
}
```

### 14.2 `instanceof` rejects unrelated types at compile time

```java
String s = "hello";
// if (s instanceof Integer) { }   // COMPILE ERROR: String and Integer are unrelated, inconvertible types
```

The compiler only allows `instanceof` checks where the relationship between the object's declared type and the tested type is at least **possible** (one could be a subtype of the other, directly or through an interface) — a check that could never possibly be true is rejected outright.

---

## 15. Pattern Matching for `instanceof`

Since Java 16, `instanceof` supports **pattern matching**, which combines the type check and the cast into a single expression, eliminating the separate explicit cast.

```java
// Old style
if (a instanceof Dog) {
    Dog d = (Dog) a;
    d.fetch();
}

// Pattern matching style (Java 16+)
if (a instanceof Dog d) {
    d.fetch();     // "d" is already a Dog here — no separate cast needed
}
```

### 15.1 Scoping rules — the pattern variable is only in scope where the match is guaranteed

```java
if (a instanceof Dog d) {
    d.fetch();          // in scope: this branch only runs if a really is a Dog
} else {
    // d.fetch();        // COMPILE ERROR — d is NOT in scope here; the match failed in this branch
}
```

### 15.2 Flow-sensitive scoping extends into combined conditions

The compiler is smart enough to extend `d`'s scope wherever it can **prove** the match held, including after an early return or across `&&`.

```java
void process(Animal a) {
    if (!(a instanceof Dog d)) {
        return;                  // if we get past this line, a MUST be a Dog
    }
    d.fetch();                   // "d" is usable here — the compiler proved the negative case already returned
}
```

```java
if (a instanceof Dog d && d.isHungry()) {   // "d" is usable in the second half of the && because
    d.fetch();                               // the first half must already be true for evaluation to continue
}
```

### 15.3 Why pattern matching is an improvement

- Removes a redundant, error-prone manual cast.
- Narrows the `d.fetch()` -> ClassCastException risk entirely, since `d` only ever exists where the match already succeeded.
- Reduces boilerplate, especially when checking several types in sequence (common in `switch` pattern matching too, introduced in later Java versions building on this same mechanism).

```java
// Chaining several type checks cleanly
void describe(Object o) {
    if (o instanceof Dog d) {
        System.out.println("Dog: " + d.getName());
    } else if (o instanceof Cat c) {
        System.out.println("Cat: " + c.getName());
    } else {
        System.out.println("Unknown animal");
    }
}
```

---

## 16. Common Pitfalls

| # | Pitfall | Fix |
|---|---------|-----|
| 1 | Thinking return type alone can distinguish overloads | Only the parameter list distinguishes overloads; return type alone is not enough |
| 2 | Assuming autoboxing is preferred over widening in overload resolution | Widening (Phase 1) is always tried before boxing/unboxing (Phase 2), and varargs (Phase 3) is the last resort |
| 3 | Writing ambiguous overloads that both require one widening/boxing step | Make one overload more specific, or cast the argument explicitly at the call site |
| 4 | Omitting `@Override` and introducing a silent bug via typo | Always annotate overrides with `@Override` so the compiler verifies them |
| 5 | Narrowing access in an override | Overrides can only stay the same or widen access, never narrow it |
| 6 | Declaring a broader checked exception in an override | Overrides can only keep, narrow, or drop checked exceptions, never broaden them |
| 7 | Assuming field access is polymorphic like method calls | Fields are resolved by the reference's declared type at compile time (shadowing, not overriding) |
| 8 | Assuming `static` methods can be overridden | `static` methods are hidden, resolved by reference type at compile time, not polymorphic |
| 9 | Downcasting without an `instanceof` check first | Always verify the real type first, or be prepared to catch `ClassCastException` |
| 10 | Forgetting that upcasting doesn't restrict what methods actually run | Dynamic dispatch still uses the real object's overridden methods; only the *visible* method set shrinks |
| 11 | Using the pattern-matched variable outside the branch where the match is guaranteed | The compiler enforces flow-sensitive scoping — use it only where the match provably held |
| 12 | Thinking `null instanceof SomeType` throws an exception | It safely returns `false` for any type, never throws |

---

## 17. Interview Questions

**Q1. What are the two kinds of polymorphism in Java, and how do they differ?**
Compile-time (static) polymorphism is method overloading — the compiler decides which overload to call based on the argument types at the call site. Runtime (dynamic) polymorphism is method overriding — the JVM decides which overridden implementation to run based on the object's actual type, at runtime.

---

**Q2. How does the compiler resolve an overloaded method call when several overloads could match?**
It searches in three phases, stopping at the first phase with a match: first, exact types or widening conversions only (no boxing, no varargs); second, allowing autoboxing/unboxing; third, allowing varargs as a last resort. Within a phase, the most specific applicable method wins; if none is uniquely most specific, the call is ambiguous and fails to compile.

---

**Q3. What is dynamic method dispatch?**
The mechanism by which the JVM selects which overridden method implementation to execute, based on the object's actual runtime type rather than the compile-time reference type. Each object effectively carries its own method table reflecting its real class, and instance method calls are resolved through that table at the moment of the call.

---

**Q4. What is the difference between a reference's type and an object's type, and why does it matter?**
The reference type is fixed at compile time and determines which methods/fields are even visible/callable through that reference. The object's actual type is determined at creation and decides which overridden method implementation actually executes. Method calls use the object's type (dynamic dispatch); field and static-method access use the reference's type (resolved at compile time).

---

**Q5. What is a covariant return type, and why is it allowed?**
An overriding method is allowed to return a subtype of the return type declared by the method it overrides. It's allowed because any caller expecting the original return type can still safely treat the narrower returned type as compatible — the contract is actually strengthened, not broken.

---

**Q6. Can an overriding method narrow the access modifier of the method it overrides?**
No. An override can keep the same access level or widen it, but never narrow it. Narrowing would break the superclass's guarantee that any reference of the supertype can call that method, since the actual subclass object's version might then be less accessible than expected.

---

**Q7. What are the rules for exceptions in method overriding?**
An overriding method can declare the same checked exceptions, a narrower (subtype) set, fewer, or none at all — but never new or broader checked exceptions than the overridden method. Unchecked exceptions are unrestricted, since callers are never forced to handle them regardless of what's declared.

---

**Q8. Are fields and static methods polymorphic?**
No. Only instance methods participate in dynamic dispatch. Fields with the same name in a subclass are shadowed, and static methods with the same signature are hidden — both are resolved at compile time based on the reference's declared type, not the object's actual type.

---

**Q9. What is the difference between upcasting and downcasting?**
Upcasting converts a subclass reference to a superclass type; it's always safe and happens implicitly, since a subtype object guarantees everything the supertype requires. Downcasting converts a superclass reference back to a subclass type; it requires an explicit cast and can fail at runtime with a `ClassCastException` if the object isn't actually an instance of the target type.

---

**Q10. How does pattern matching for `instanceof` improve on the traditional check-then-cast pattern?**
It combines the type check and the cast into one expression (`if (obj instanceof Dog d)`), binding a new variable of the narrower type that is only in scope where the compiler can prove the match succeeded (including through flow-sensitive analysis across early returns and `&&`). This removes a redundant explicit cast and the risk of mismatching the check and the cast.

---

## 18. Interview-Style Output Questions

**Q1**
```java
class Box {
    void show(int x)   { System.out.println("int"); }
    void show(long x)  { System.out.println("long"); }
    void show(double x){ System.out.println("double"); }
}
new Box().show(5);
new Box().show(5L);
new Box().show('a');
```
**Answer:**
```
int
long
int
```
(`char` widens to `int` before it would reach `long` or `double`.)

---

**Q2**
```java
class Animal {
    String type = "Animal";
    void speak() { System.out.println("Animal speaks"); }
}
class Dog extends Animal {
    String type = "Dog";
    @Override void speak() { System.out.println("Dog speaks"); }
}
Animal a = new Dog();
System.out.println(a.type);
a.speak();
```
**Answer:**
```
Animal
Dog speaks
```
(Field access uses the reference type; method calls use dynamic dispatch on the object type.)

---

**Q3**
```java
class Shape {
    Shape clone() { return new Shape(); }
}
class Circle extends Shape {
    @Override
    Circle clone() { return new Circle(); }
}
Shape s = new Circle();
Shape result = s.clone();
System.out.println(result.getClass().getSimpleName());
```
**Answer:** `Circle` — covariant return type, and dynamic dispatch runs `Circle`'s `clone()`.

---

**Q4**
```java
class Animal { }
class Dog extends Animal { }
class Cat extends Animal { }

Animal a = new Dog();
Cat c = (Cat) a;
```
**Answer:** Compiles, but throws `ClassCastException` at runtime — `a` actually references a `Dog`, which is not a `Cat`.

---

**Q5**
```java
Object o = "hello";
if (o instanceof String s) {
    System.out.println(s.length());
}
```
**Answer:** `5`. Pattern matching binds `s` as a `String` inside the `if` block, letting `.length()` be called directly.

---

**Q6**
```java
class Animal {
    static void info() { System.out.println("Animal"); }
}
class Dog extends Animal {
    static void info() { System.out.println("Dog"); }
}
Animal a = new Dog();
a.info();
```
**Answer:** `Animal` — `static` methods are hidden, not overridden, and resolved by the reference's declared type (`Animal`).

---

**Q7**
```java
class Box {
    void show(Integer x) { System.out.println("Integer"); }
    void show(long x)     { System.out.println("long"); }
}
new Box().show(10);
```
**Answer:** `long`. Widening (`int` → `long`, Phase 1) is attempted before autoboxing (`int` → `Integer`, Phase 2), so the widening overload wins.

---

## 19. Quick Cheat Sheet

```text
COMPILE-TIME (STATIC)        Method overloading — same name, different params, decided by COMPILER
RUNTIME (DYNAMIC)            Method overriding — same signature in subclass, decided by JVM at runtime

OVERLOAD RESOLUTION          Phase 1: exact match / widening only
                              Phase 2: + autoboxing/unboxing
                              Phase 3: + varargs (last resort)
                              Ambiguous if no single most-specific match -> compile error

DYNAMIC DISPATCH              reference type   -> what you're ALLOWED to call (compile time)
                              object's real type -> WHICH override actually runs (runtime)

@Override                     Optional but recommended: compiler verifies a real override exists

OVERRIDE RULES                Same name + same params
                              Same OR covariant return type
                              Access: same or WIDER only (never narrower)
                              Checked exceptions: same, narrower, fewer, or none (never broader)
                              Unchecked exceptions: unrestricted
                              Cannot override: final, static (hidden), private (invisible)

NOT POLYMORPHIC                Fields       -> resolved by reference type (shadowing)
                                static methods -> resolved by reference type (hiding)

UPCASTING                     Subclass -> Superclass reference: implicit, always safe
DOWNCASTING                   Superclass -> Subclass reference: explicit cast, can throw ClassCastException

instanceof                    obj instanceof Type         -> boolean, false for null, never throws
PATTERN MATCHING (16+)        obj instanceof Type var     -> binds "var" only where match is guaranteed
```

**Remember:**

1. Overloading is resolved by the compiler using argument types; overriding is resolved by the JVM using the object's real type.
2. Widening beats boxing beats varargs, in that strict order, during overload resolution.
3. An override can widen access and narrow/drop checked exceptions, but never the reverse.
4. Only instance methods are truly polymorphic — fields and static methods are resolved by reference type.
5. Upcasting is implicit and safe; downcasting needs an explicit cast and can fail at runtime — guard it with `instanceof`.
6. Pattern-matching `instanceof` folds the check and cast into one, with the compiler enforcing where the new variable is actually valid.
