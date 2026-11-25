# Java Nested and Inner Classes: Detailed Notes

A **nested class** is any class defined **inside** another class. Java has **four** kinds, each with different rules about where they live, whether they need an enclosing instance, and what they can access:

```text
Static nested class    — static, no link to any enclosing instance
Inner class             — non-static, tied to a specific enclosing instance
Local class             — defined inside a method body
Anonymous class         — a local class with no name, declared and instantiated in one expression
```

```java
class Outer {
    static class StaticNested { }      // 1. static nested class
    class Inner { }                     // 2. (non-static) inner class

    void method() {
        class Local { }                 // 3. local class
        Runnable r = new Runnable() {   // 4. anonymous class
            public void run() { }
        };
    }
}
```

---

## Table of Contents

1. [Why Nested Classes Exist](#1-why-nested-classes-exist)
2. [Static Nested Classes](#2-static-nested-classes)
3. [Non-Static Inner Classes](#3-non-static-inner-classes)
4. [Inner Class Instantiation Syntax](#4-inner-class-instantiation-syntax)
5. [Accessing the Outer Instance Explicitly: `Outer.this`](#5-accessing-the-outer-instance-explicitly-outerthis)
6. [Static Nested Class vs Inner Class — Side by Side](#6-static-nested-class-vs-inner-class--side-by-side)
7. [Local Classes](#7-local-classes)
8. [Effectively Final Capture in Local Classes](#8-effectively-final-capture-in-local-classes)
9. [Anonymous Classes](#9-anonymous-classes)
10. [Anonymous Classes Extending a Class vs Implementing an Interface](#10-anonymous-classes-extending-a-class-vs-implementing-an-interface)
11. [How Anonymous Classes Connect to Functional Interfaces and Lambdas](#11-how-anonymous-classes-connect-to-functional-interfaces-and-lambdas)
12. [All Four, Side by Side](#12-all-four-side-by-side)
13. [Common Pitfalls](#13-common-pitfalls)
14. [Interview Questions](#14-interview-questions)
15. [Interview-Style Output Questions](#15-interview-style-output-questions)
16. [Quick Cheat Sheet](#16-quick-cheat-sheet)

---

## 1. Why Nested Classes Exist

Nesting a class inside another serves a few recurring purposes:

- **Logical grouping** — a helper class that is only ever meaningful in the context of its enclosing class (e.g., a `Node` class that only makes sense inside a `LinkedList`).
- **Tighter encapsulation** — a nested class can access the enclosing class's `private` members directly, something an unrelated top-level class never could.
- **Reduced namespace clutter** — avoids littering the package with many small, tightly-coupled classes that only one other class actually uses.
- **Concise, throwaway implementations** — local and anonymous classes let you implement an interface or extend a class right where it's needed, without a separate file or even a separate named class at all.

---

## 2. Static Nested Classes

A **static nested class** is declared `static` inside another class. It behaves like an ordinary top-level class that is simply namespaced inside its enclosing class — it does **not** hold any implicit reference to an enclosing instance.

```java
class Outer {
    static int sharedValue = 100;
    int instanceValue = 5;

    static class StaticNested {
        void show() {
            System.out.println(sharedValue);      // OK — can access the enclosing class's STATIC members
            // System.out.println(instanceValue);  // COMPILE ERROR — no enclosing instance to read from
        }
    }
}
```

### 2.1 Creating one requires no `Outer` instance at all

```java
Outer.StaticNested n = new Outer.StaticNested();   // just like any top-level class, namespaced under Outer
n.show();
```

### 2.2 Typical real-world use: a helper type tightly coupled to its outer class

```java
class LinkedList {
    static class Node {          // doesn't need any particular LinkedList's state
        int value;
        Node next;
        Node(int value) { this.value = value; }
    }

    Node head;

    void addFirst(int value) {
        Node newNode = new Node(value);
        newNode.next = head;
        head = newNode;
    }
}
```

```java
// Map.Entry is a well-known standard-library example of this exact pattern
for (Map.Entry<String, Integer> entry : someMap.entrySet()) {
    System.out.println(entry.getKey() + " = " + entry.getValue());
}
```

> This is the same "static nested class" covered when discussing the `static` keyword — see those notes for how it compares specifically to a **static block** (a block of code that runs once, versus a nested class, which is a type definition you instantiate on demand).

---

## 3. Non-Static Inner Classes

A **(non-static) inner class** is declared **without** `static` inside another class. Unlike a static nested class, every instance of an inner class is **tied to exactly one specific instance** of its enclosing class, and can freely access that enclosing instance's members — including its `private` fields and methods.

```java
class Outer {
    private int secret = 42;

    class Inner {                    // NOT static — a true inner class
        void reveal() {
            System.out.println(secret);   // OK — Inner has implicit access to its enclosing Outer instance's state
        }
    }
}
```

### 3.1 An inner class instance always carries a hidden reference to its enclosing instance

Conceptually, every `Inner` object silently stores a reference back to the specific `Outer` object that created it — this is what allows direct access to that outer instance's fields and methods, including `private` ones.

```java
class Outer {
    private String name = "Outer Instance";

    class Inner {
        void show() {
            System.out.println("Accessing: " + name);   // implicitly: Outer.this.name
        }
    }
}
```

### 3.2 Why access to private members works

Both the outer and inner class are compiled as part of the **same enclosing top-level type's access scope** — Java treats a class and its nested classes as mutually accessible to each other's `private` members, which is a deliberate language design choice supporting tight encapsulation between closely related classes.

```java
class BankAccount {
    private double balance = 1000;

    class StatementPrinter {           // inner class
        void print() {
            System.out.println("Balance: " + balance);   // direct access to Outer's private field
        }
    }
}
```

---

## 4. Inner Class Instantiation Syntax

Because every inner class instance needs a specific enclosing instance, creating one requires special syntax **unless** you're already inside a method of the enclosing class.

### 4.1 From outside the enclosing class

```java
Outer outer = new Outer();
Outer.Inner inner = outer.new Inner();   // the ENCLOSING INSTANCE creates the Inner object
```

### 4.2 From inside a method of the enclosing class

```java
class Outer {
    class Inner { }

    void createInner() {
        Inner i = new Inner();      // no special syntax needed — "this" is implicitly the enclosing instance
    }
}
```

### 4.3 Attempting to create one without an enclosing instance fails

```java
Outer.Inner i = new Outer.Inner();   // COMPILE ERROR — an Inner needs an Outer instance to be attached to
```

---

## 5. Accessing the Outer Instance Explicitly: `Outer.this`

If an inner class declares a field or variable that **shadows** a field in the outer class (same name), the plain name refers to the inner class's own version. `Outer.this.fieldName` is how you explicitly reach the enclosing instance's version instead — directly analogous to how `super.field` reaches a shadowed parent field in regular inheritance.

```java
class Outer {
    int value = 10;

    class Inner {
        int value = 20;

        void show() {
            System.out.println(value);         // 20 — Inner's own field
            System.out.println(this.value);     // 20 — same thing, explicit "this"
            System.out.println(Outer.this.value); // 10 — explicitly reach the enclosing Outer instance's field
        }
    }
}
```

```java
new Outer().new Inner().show();
// 20
// 20
// 10
```

---

## 6. Static Nested Class vs Inner Class — Side by Side

| | Static Nested Class | (Non-Static) Inner Class |
|---|------------------------|------------------------------|
| Declared with | `static class Name { }` | `class Name { }` (no `static`) |
| Needs an enclosing instance to create? | No | Yes |
| Instantiation (from outside) | `new Outer.Nested()` | `outer.new Inner()` |
| Can access enclosing instance fields/methods? | No — only the enclosing class's `static` members | Yes — directly, including `private` members |
| Holds a hidden reference to an enclosing instance? | No | Yes |
| Typical use | A helper type logically grouped with its outer class, not tied to any instance's state | A helper type whose behavior genuinely depends on, and manipulates, a specific outer instance's state |

```java
class Outer {
    static int staticField = 1;
    int instanceField = 2;

    static class StaticNested {
        void show() {
            System.out.println(staticField);       // OK
            // instanceField not accessible here
        }
    }

    class Inner {
        void show() {
            System.out.println(staticField);       // OK — static members always accessible
            System.out.println(instanceField);      // OK — inner classes see the enclosing INSTANCE's state too
        }
    }
}
```

---

## 7. Local Classes

A **local class** is a class defined **inside a method body** (or, less commonly, inside a constructor or any other block). It is only visible and usable **within that block** — it doesn't even exist as a named type anywhere else.

```java
class Outer {
    void process() {
        class Helper {                     // LOCAL class — only exists inside process()
            void assist() {
                System.out.println("Helping");
            }
        }

        Helper h = new Helper();
        h.assist();
    }
}
```

```java
Outer o = new Outer();
o.process();   // "Helping"
// Helper is completely invisible/unusable anywhere outside process()
```

### 7.1 Local classes can access the enclosing instance's members too

Since a local class is defined inside an instance method, it behaves much like an inner class with respect to the enclosing object — it can freely access the enclosing instance's fields and methods (including `private` ones).

```java
class Outer {
    private int value = 99;

    void process() {
        class Local {
            void show() {
                System.out.println(value);   // OK — accesses the enclosing Outer instance's private field
            }
        }
        new Local().show();
    }
}
```

### 7.2 Why use a local class at all

A local class is useful for a **one-off helper type** that's only ever needed inside a single method, and would otherwise clutter the enclosing class (or the package, as a separate top-level class) with a name nobody else needs to know about.

```java
void sortByCustomRule(List<String> names) {
    class LengthComparator implements java.util.Comparator<String> {
        @Override
        public int compare(String a, String b) {
            return Integer.compare(a.length(), b.length());
        }
    }
    names.sort(new LengthComparator());
}
```

> In practice, a local class like this is now most often written as a **lambda** instead (section 11) when the type being implemented is a functional interface — local classes remain useful for cases needing multiple methods, constructors, or additional state that a lambda can't express.

---

## 8. Effectively Final Capture in Local Classes

A local class can reference **local variables and parameters** from its enclosing method — but only if those variables are `final` or **effectively final** (assigned exactly once, and never reassigned afterward, even without the keyword).

```java
void process(int input) {
    int multiplier = 10;         // effectively final — never reassigned after this

    class Calculator {
        void compute() {
            System.out.println(input * multiplier);   // OK — both captured variables are effectively final
        }
    }
    new Calculator().compute();
}
```

```java
void processBroken(int input) {
    int counter = 0;

    class Logger {
        void log() {
            // System.out.println(counter);   // would be a COMPILE ERROR if counter is reassigned below
        }
    }
    counter = 5;   // reassignment — this makes "counter" NOT effectively final
}
```

### 8.1 Why this restriction exists

The local class's compiled form effectively **copies** the captured variable's value at the time the class instance is created, rather than holding a live reference to the method's actual local variable (which, being on the stack, may no longer exist by the time the local class instance is used — e.g., after the enclosing method has already returned). Restricting capture to effectively final variables guarantees that this captured copy can never silently go stale or disagree with a value the method itself might have changed.

---

## 9. Anonymous Classes

An **anonymous class** is a local class with **no name**, declared and instantiated **in a single expression**, typically to provide a one-off implementation of an interface or a subclass of an existing class right at the point where it's needed.

```java
interface Greetable {
    void greet();
}
```

```java
Greetable g = new Greetable() {        // anonymous class: implements Greetable, inline, with no separate name
    @Override
    public void greet() {
        System.out.println("Hello from an anonymous class");
    }
};

g.greet();   // "Hello from an anonymous class"
```

### 9.1 Anatomy of the syntax

```java
new InterfaceOrClassName(constructorArgsIfAny) {
    // method overrides / implementations go here
}
```

- It starts with `new`, followed by the type being implemented or extended.
- It's immediately followed by `{ ... }` containing the class body — method overrides, additional fields, even additional (non-constructor) methods.
- The whole expression evaluates to a reference of the named supertype, pointing at this new, nameless class's single instance.

### 9.2 Anonymous classes can also access effectively final local variables

Exactly like local classes (section 8), an anonymous class defined inside a method can capture effectively final local variables and parameters from the enclosing scope.

```java
void greetUser(String name) {
    Greetable g = new Greetable() {
        @Override
        public void greet() {
            System.out.println("Hello, " + name);   // "name" captured — must be effectively final
        }
    };
    g.greet();
}
```

### 9.3 Anonymous classes can also access and use enclosing instance members

```java
class Outer {
    private String label = "Outer's label";

    Runnable makeRunnable() {
        return new Runnable() {
            @Override
            public void run() {
                System.out.println(label);   // accesses the enclosing Outer instance's private field
            }
        };
    }
}
```

---

## 10. Anonymous Classes Extending a Class vs Implementing an Interface

An anonymous class can be based on **either** an interface **or** a concrete/abstract class — the syntax looks the same either way, but what's happening underneath differs slightly.

### 10.1 Implementing an interface

```java
Runnable r = new Runnable() {
    @Override
    public void run() {
        System.out.println("Running");
    }
};
```

### 10.2 Extending a class (concrete or abstract)

```java
class Animal {
    void makeSound() { System.out.println("Generic animal sound"); }
}
```

```java
Animal a = new Animal() {           // anonymous SUBCLASS of Animal, overriding makeSound()
    @Override
    void makeSound() {
        System.out.println("Anonymous animal sound");
    }
};
a.makeSound();   // "Anonymous animal sound"
```

### 10.3 Key restriction: you can only extend/implement ONE type

Unlike a regular named class, which can `implements` multiple interfaces, an anonymous class can only be based on **one** supertype — either one class (which it implicitly extends) or one interface (which it implicitly implements) — never both, and never more than one interface. If you need to combine multiple contracts, you need a real, named class.

---

## 11. How Anonymous Classes Connect to Functional Interfaces and Lambdas

As covered in the abstraction notes, a **functional interface** is one with exactly one abstract method — and that single-method shape is exactly what allows a much more concise alternative to an anonymous class: a **lambda expression**.

### 11.1 The same behavior, three ways

```java
interface Greetable {
    void greet(String name);
}
```

```java
// 1. A full, separately-named top-level (or nested) class
class FormalGreeter implements Greetable {
    @Override
    public void greet(String name) {
        System.out.println("Hello, " + name);
    }
}
Greetable g1 = new FormalGreeter();
```

```java
// 2. An anonymous class — inline, but still verbose
Greetable g2 = new Greetable() {
    @Override
    public void greet(String name) {
        System.out.println("Hello, " + name);
    }
};
```

```java
// 3. A lambda expression — only possible because Greetable is a FUNCTIONAL interface (exactly one abstract method)
Greetable g3 = (name) -> System.out.println("Hello, " + name);
```

All three produce an object implementing `Greetable`'s `greet(String)` method — the lambda is simply a far more concise notation for **exactly the same situation** an anonymous class handles: providing a one-off implementation of a single-method interface, without writing a separate named class.

### 11.2 Why lambdas can't replace every anonymous class

A lambda can **only** target a functional interface (exactly one abstract method). An anonymous class remains necessary when:

```text
The type being implemented has MORE than one abstract method (not a functional interface).
You need to extend a CLASS (abstract or concrete), not implement an interface.
You need additional fields, a constructor, or multiple helper methods beyond just the one being implemented.
You genuinely need access to "this" referring to the anonymous class instance itself
  (inside a lambda, "this" refers to the ENCLOSING instance, not the lambda — a subtle but real difference).
```

```java
abstract class Shape {
    abstract double area();
}

Shape s = new Shape() {       // MUST be an anonymous class — Shape is a class, not a functional interface,
    @Override                  // and lambdas cannot extend classes at all
    double area() { return 10; }
};
```

### 11.3 A practical takeaway

> Anonymous classes are the **general-purpose** mechanism for inline, one-off implementations of either a class or an interface. Lambdas are a **specialized, more concise** notation available **only** when the target is a functional interface. Understanding anonymous classes first is exactly what makes lambdas make sense later — a lambda is best understood as "an anonymous class, but only for the single-abstract-method case, with the ceremony stripped away."

---

## 12. All Four, Side by Side

| | Static Nested Class | Inner Class | Local Class | Anonymous Class |
|---|------------------------|---------------|----------------|---------------------|
| Has a name? | Yes | Yes | Yes | No |
| Declared where? | Inside a class, `static` | Inside a class, not `static` | Inside a method body | Inline, inside an expression |
| Needs an enclosing instance? | No | Yes | No (unless accessing instance members) | Depends on context (like a local class) |
| Can access enclosing instance's `private` members? | No (only static members) | Yes | Yes (if defined in an instance method) | Yes (if defined in an instance method) |
| Can capture local variables from its scope? | N/A (not inside a method) | N/A (not inside a method) | Yes, if effectively final | Yes, if effectively final |
| Can have its own constructor? | Yes | Yes | Yes | No (uses the supertype's constructor, if any) |
| Can implement/extend multiple types? | Yes (implements many interfaces, extends one class) | Yes | Yes | No — only ONE supertype (class or interface) |
| Typical use | Tightly-coupled helper type, not tied to instance state | Helper type that manipulates a specific outer instance's state | One-off helper type used only within a single method | Quick, inline, throwaway implementation of a class/interface |
| Modern lighter-weight alternative | — | — | Often a lambda (if target is a functional interface) | Often a lambda (if target is a functional interface) |

---

## 13. Common Pitfalls

| # | Pitfall | Fix |
|---|---------|-----|
| 1 | Trying to create an inner class instance without an enclosing instance (`new Outer.Inner()`) | Use `outer.new Inner()` from outside, or just `new Inner()` from inside an `Outer` instance method |
| 2 | Assuming a static nested class can access the enclosing class's instance fields | It can't — only the enclosing class's `static` members are reachable |
| 3 | Reassigning a local variable captured by a local or anonymous class | The variable must be effectively final — never reassigned after its first assignment, for the whole enclosing scope |
| 4 | Expecting an anonymous class to implement multiple interfaces | Not allowed — an anonymous class can only extend one class or implement one interface, never more than one supertype |
| 5 | Forgetting that `this` inside a lambda refers to the enclosing instance, not the lambda itself | This is a real behavioral difference from anonymous classes, where `this` refers to the anonymous class instance |
| 6 | Confusing a static nested class with a static block | A static block is code that runs once; a static nested class is a type definition you instantiate whenever you choose |
| 7 | Assuming every anonymous class implementing an interface could instead be a lambda | Only true if the interface is functional (exactly one abstract method); otherwise an anonymous class is required |
| 8 | Writing a local class for something that's really just a single-method implementation | Consider a lambda instead, if the target type is a functional interface — much less boilerplate |
| 9 | Forgetting field name shadowing can hide the outer class's field inside an inner class | Use `Outer.this.fieldName` to explicitly reach the enclosing instance's version |
| 10 | Assuming local/anonymous classes can only read captured variables, never use enclosing instance state | They can do both — captured local variables (effectively final) AND the enclosing instance's full state (including private members), if defined within an instance context |

---

## 14. Interview Questions

**Q1. What are the four kinds of nested classes in Java?**
Static nested classes, (non-static) inner classes, local classes (defined inside a method), and anonymous classes (unnamed, defined and instantiated in a single expression).

---

**Q2. What is the key difference between a static nested class and a non-static inner class?**
A static nested class has no connection to any enclosing instance and can only access the enclosing class's static members. A non-static inner class is tied to a specific enclosing instance (requiring one to be created, via `outer.new Inner()` from outside), and can freely access that instance's fields and methods, including private ones.

---

**Q3. How do you instantiate an inner class from outside its enclosing class?**
Using `outer.new Inner()`, where `outer` is an existing instance of the enclosing class — the inner class instance is explicitly tied to that specific outer instance.

---

**Q4. What is `Outer.this` used for?**
It explicitly refers to the specific enclosing class instance an inner class (or local/anonymous class inside an instance method) is associated with — most commonly used to access an outer field that's been shadowed by a same-named field in the inner class.

---

**Q5. What is a local class, and where can it be used?**
A class defined inside a method body (or another block), visible and usable only within that block. It behaves similarly to an inner class in terms of accessing the enclosing instance's state (if defined in an instance method), and can additionally capture effectively final local variables and parameters from its enclosing method.

---

**Q6. What does "effectively final" mean, and why does it matter for local/anonymous classes?**
A variable is effectively final if it is assigned exactly once and never reassigned afterward, even without the `final` keyword. Local and anonymous classes can only capture variables that are final or effectively final, because the captured value is effectively copied at creation time rather than linked live to the method's actual (stack-based) variable, which may no longer exist later.

---

**Q7. What is an anonymous class?**
A local class with no name, declared and instantiated in a single expression, typically used to provide a one-off implementation of an interface or a subclass of an existing class exactly where it's needed, without writing a separate named class file.

---

**Q8. Can an anonymous class implement multiple interfaces?**
No. An anonymous class can extend exactly one class or implement exactly one interface — never both, and never more than one interface. A named class is required if multiple interfaces need to be combined.

---

**Q9. How do anonymous classes relate to lambdas?**
A lambda expression is a much more concise alternative to an anonymous class, but only usable when the target type is a functional interface (exactly one abstract method). Anonymous classes remain necessary for extending a class, implementing a non-functional interface, or needing extra fields/methods/constructors beyond the single method being implemented.

---

**Q10. What is one subtle behavioral difference between `this` inside an anonymous class versus inside a lambda?**
Inside an anonymous class, `this` refers to the anonymous class's own instance. Inside a lambda, `this` refers to the **enclosing instance** — a lambda does not create its own distinct `this` context the way an anonymous (or any other) class does.

---

## 15. Interview-Style Output Questions

**Q1**
```java
class Outer {
    int value = 10;
    class Inner {
        int value = 20;
        void show() {
            System.out.println(value);
            System.out.println(Outer.this.value);
        }
    }
}
new Outer().new Inner().show();
```
**Answer:**
```
20
10
```

---

**Q2**
```java
class Outer {
    static class Nested {
        void show() { System.out.println("Nested"); }
    }
}
Outer.Nested n = new Outer.Nested();
n.show();
```
**Answer:** `Nested`. No `Outer` instance was needed since `Nested` is static.

---

**Q3**
```java
Outer.Inner i = new Outer.Inner();
```
(assuming `Inner` is a non-static inner class of `Outer`)
**Answer:** Compile error — a non-static inner class requires an enclosing `Outer` instance; must be `outer.new Inner()`.

---

**Q4**
```java
interface Greetable { void greet(); }

void test() {
    String name = "Aashish";
    Greetable g = new Greetable() {
        @Override
        public void greet() {
            System.out.println("Hi " + name);
        }
    };
    g.greet();
}
```
**Answer:** `Hi Aashish`. `name` is effectively final (never reassigned), so it can be captured by the anonymous class.

---

**Q5**
```java
abstract class Shape {
    abstract double area();
}
Shape s = new Shape() {
    @Override
    double area() { return 25.0; }
};
System.out.println(s.area());
```
**Answer:** `25.0`. An anonymous subclass of the abstract class `Shape`, overriding its single abstract method.

---

## 16. Quick Cheat Sheet

```text
STATIC NESTED CLASS   static class Name { }
                      - No enclosing instance needed:  new Outer.Nested()
                      - Only accesses Outer's STATIC members

INNER CLASS            class Name { }   (no static, inside another class)
                      - Needs an enclosing instance:   outer.new Inner()  (or just "new Inner()" from inside Outer)
                      - Accesses Outer's instance members too, including private

Outer.this.field       Explicitly reach the enclosing instance's (possibly shadowed) field from an inner/local/
                       anonymous class, same idea as super.field for shadowed inherited fields

LOCAL CLASS            Declared INSIDE a method body; usable only within that method
                      - Can access enclosing instance members (if in an instance method)
                      - Can capture effectively final local variables/parameters

ANONYMOUS CLASS        new Type() { ...overrides/body... }
                      - No name; declared + instantiated in ONE expression
                      - Can extend ONE class OR implement ONE interface — never both, never multiple
                      - Can capture effectively final locals, and access enclosing instance members

EFFECTIVELY FINAL      Assigned exactly once, never reassigned afterward — required for anything a
                       local/anonymous class (or lambda) captures from its enclosing method scope

ANONYMOUS CLASS         Functional interface (exactly ONE abstract method)?
  vs LAMBDA                 YES -> a lambda can replace the anonymous class, much less boilerplate
                            NO  -> anonymous class still required (multiple methods, or extending a class)
                       "this" inside a lambda = ENCLOSING instance; "this" inside an anonymous class = itself
```

**Remember:**

1. Static nested classes have no link to any enclosing instance; inner classes are tied to one specific instance and can access its private state.
2. Creating a non-static inner class from outside needs `outer.new Inner()`; from inside an instance method, just `new Inner()`.
3. Local and anonymous classes can capture local variables from their enclosing method, but only if those variables are final or effectively final.
4. An anonymous class can implement only one interface or extend only one class — never more than one supertype at once.
5. A lambda is best understood as a lighter-weight anonymous class, usable only when the target type is a functional interface (exactly one abstract method) — everything else about nested classes still requires the real thing.
