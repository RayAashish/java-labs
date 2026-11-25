# Java Encapsulation: Detailed Notes

**Encapsulation** is the pillar of OOP that **hides an object's internal state** and requires all interaction to happen through well-defined methods. The object's fields are kept `private`, and the only way the outside world can read or change that state is through methods the class itself exposes.

```java
class BankAccount {
    private double balance;              // hidden state

    public double getBalance() {         // controlled access
        return balance;
    }

    public void deposit(double amount) { // controlled modification
        if (amount > 0) balance += amount;
    }
}
```

Outside code can never do `account.balance = -500;` directly — it must go through `deposit()`, which can enforce the rule that balance never goes negative.

---

## Table of Contents

1. [Why Encapsulation Matters](#1-why-encapsulation-matters)
2. [Access Modifiers](#2-access-modifiers)
3. [`private`](#3-private)
4. [`default` (Package-Private)](#4-default-package-private)
5. [`protected`](#5-protected)
6. [`public`](#6-public)
7. [Access Modifier Comparison Table](#7-access-modifier-comparison-table)
8. [Getters and Setters](#8-getters-and-setters)
9. [Why Direct Public Fields Are Discouraged](#9-why-direct-public-fields-are-discouraged)
10. [Packages and Access Control](#10-packages-and-access-control)
11. [Immutable Classes](#11-immutable-classes)
12. [How String Achieves Immutability](#12-how-string-achieves-immutability)
13. [Writing Your Own Immutable Class](#13-writing-your-own-immutable-class)
14. [Common Pitfalls](#14-common-pitfalls)
15. [Interview Questions](#15-interview-questions)
16. [Interview-Style Output Questions](#16-interview-style-output-questions)
17. [Quick Cheat Sheet](#17-quick-cheat-sheet)

---

## 1. Why Encapsulation Matters

Without encapsulation, any code anywhere could directly reach into an object's fields and change them however it liked:

```java
class Account {
    double balance;   // no access modifier shown as public for illustration
}

Account a = new Account();
a.balance = -99999;   // nothing stops this — the object is now in an invalid state
```

Encapsulation fixes this by:

- **Protecting invariants** — rules the class wants to guarantee are always true (e.g., "balance never goes negative").
- **Hiding implementation details** — the internal representation can change later without breaking code that uses the class.
- **Controlling how and when state changes** — a setter can validate, log, trigger side effects, or simply refuse a bad value.
- **Reducing coupling** — other classes depend on a stable method contract, not on internal field names or types.

> Memory aid: a controller's dashboard hides the wiring; you interact with buttons and dials (methods), not the raw circuitry (fields).

---

## 2. Access Modifiers

Java has **four** levels of access control, used on classes, fields, methods, and constructors. They differ in **how visible** a member is from other classes.

```text
private   < default (package-private)   <   protected   <   public
```

From most restrictive to least restrictive:

| Modifier | Keyword written? |
|----------|-------------------|
| `private` | Yes — write `private` |
| default / package-private | **No keyword** — simply omit a modifier |
| `protected` | Yes — write `protected` |
| `public` | Yes — write `public` |

---

## 3. `private`

`private` members are accessible **only within the same class** — not even subclasses or other classes in the same package can see them.

```java
class Account {
    private double balance;    // only Account's own code can touch this directly

    private void logTransaction(String msg) {   // helper method, internal use only
        System.out.println(msg);
    }
}

class Main {
    public static void main(String[] args) {
        Account a = new Account();
        // a.balance = 100;        // COMPILE ERROR: balance has private access
        // a.logTransaction("hi"); // COMPILE ERROR: logTransaction has private access
    }
}
```

`private` is the natural default for **fields** (encapsulated state) and for **helper methods** that exist only to support the class's own logic.

> A top-level class itself cannot be declared `private` (only `public` or package-private) — `private` is for members: fields, methods, constructors, and nested classes.

---

## 4. `default` (Package-Private)

If you write **no** access modifier at all, Java applies **default** access — also called **package-private**. The member is visible to **any class in the same package**, but not to classes in other packages.

```java
package com.raysi.accounts;

class Account {          // package-private class
    double balance;      // package-private field (no modifier)

    void applyInterest() {   // package-private method
        balance *= 1.05;
    }
}
```

```java
package com.raysi.accounts;

class Bank {              // same package
    void test() {
        Account a = new Account();
        a.balance = 100;        // OK — same package
        a.applyInterest();      // OK — same package
    }
}
```

```java
package com.raysi.reports;

import com.raysi.accounts.Account;   // won't even compile if Account itself is package-private

class Report {
    void test() {
        // Account a = new Account();   // COMPILE ERROR if class is package-private
        // a.balance = 100;             // COMPILE ERROR: balance not visible outside its package
    }
}
```

Default access is useful for things an entire **module/package** should share internally, but that should stay invisible to code outside that package.

---

## 5. `protected`

`protected` members are visible:

- Within the **same class**
- Within the **same package** (just like default access)
- In **subclasses**, even if those subclasses live in a **different package**

```java
package com.raysi.animals;

public class Animal {
    protected String name;

    protected void makeSound() {
        System.out.println(name + " makes a sound");
    }
}
```

```java
package com.raysi.pets;

import com.raysi.animals.Animal;

public class Dog extends Animal {
    void bark() {
        name = "Rex";          // OK — protected, accessible via inheritance across packages
        makeSound();            // OK
    }
}
```

But a class in a different package that is **not** a subclass still cannot access it:

```java
package com.raysi.other;

import com.raysi.animals.Animal;

class Test {
    void test() {
        Animal a = new Animal();
        // a.name = "X";       // COMPILE ERROR: not a subclass, different package
    }
}
```

`protected` is the right choice for members meant to be used or overridden by subclasses, while still staying hidden from unrelated external code.

---

## 6. `public`

`public` members are accessible from **anywhere** — any class, in any package, with no restriction at all.

```java
package com.raysi.accounts;

public class Account {
    public String accountHolder;     // accessible from anywhere

    public double getBalance() {     // accessible from anywhere
        return 0;
    }
}
```

```java
package com.raysi.app;

import com.raysi.accounts.Account;

class Main {
    public static void main(String[] args) {
        Account a = new Account();
        a.accountHolder = "Aashish";   // OK, public
        a.getBalance();                // OK, public
    }
}
```

`public` is appropriate for the class's **API** — the methods meant to be the stable, documented entry points other code relies on. It is generally the wrong choice for raw fields (see section 9).

---

## 7. Access Modifier Comparison Table

| Accessible from... | `private` | default (package-private) | `protected` | `public` |
|---------------------|:---------:|:--------------------------:|:-----------:|:--------:|
| Same class | ✅ | ✅ | ✅ | ✅ |
| Same package, different class | ❌ | ✅ | ✅ | ✅ |
| Different package, subclass | ❌ | ❌ | ✅ | ✅ |
| Different package, non-subclass | ❌ | ❌ | ❌ | ✅ |

A simple way to remember the order, from most to least restrictive:

```text
private  →  default  →  protected  →  public
(class)     (package)    (package + subclass)   (everyone)
```

---

## 8. Getters and Setters

A **getter** returns the value of a private field. A **setter** updates it — usually with some validation along the way. Together they are the standard, controlled doorway into an object's encapsulated state.

```java
class Student {
    private String name;
    private int age;

    // Getter
    public String getName() {
        return name;
    }

    // Setter
    public void setName(String name) {
        this.name = name;
    }

    // Getter
    public int getAge() {
        return age;
    }

    // Setter with validation
    public void setAge(int age) {
        if (age < 0) {
            throw new IllegalArgumentException("Age cannot be negative");
        }
        this.age = age;
    }
}
```

```java
Student s = new Student();
s.setName("Aashish");
s.setAge(23);
System.out.println(s.getName());   // Aashish
System.out.println(s.getAge());    // 23

s.setAge(-5);   // throws IllegalArgumentException — invalid state is rejected
```

### 8.1 A getter/setter doesn't have to be a trivial pass-through

Because they are regular methods, getters and setters can do more than just read or write a field directly:

```java
class Temperature {
    private double celsius;

    public double getFahrenheit() {               // derived/computed getter
        return (celsius * 9 / 5) + 32;
    }

    public void setCelsius(double celsius) {
        if (celsius < -273.15) {                   // validation against absolute zero
            throw new IllegalArgumentException("Below absolute zero");
        }
        this.celsius = celsius;
    }
}
```

### 8.2 Read-only and write-only access

Encapsulation also lets a class expose only a **getter** (read-only from outside) or only a **setter** (write-only from outside), depending on what makes sense.

```java
class Circle {
    private final double radius;        // set once, never changed externally

    public Circle(double radius) {
        this.radius = radius;
    }

    public double getRadius() {         // getter only — no setRadius()
        return radius;
    }
}
```

### 8.3 Conventional naming

Java convention (used heavily by frameworks, IDEs, and libraries like Lombok, JavaBeans, and JSON serializers) expects:

```text
getXxx()   for a getter returning type T, field name xxx
setXxx(T)  for a setter accepting type T
isXxx()    for a getter of a boolean field (preferred over getXxx() for booleans)
```

```java
class Student {
    private boolean passed;

    public boolean isPassed() {         // preferred over getPassed()
        return passed;
    }

    public void setPassed(boolean passed) {
        this.passed = passed;
    }
}
```

---

## 9. Why Direct Public Fields Are Discouraged

```java
class Student {
    public String name;
    public int age;     // no validation possible, no encapsulation at all
}

Student s = new Student();
s.age = -500;           // perfectly legal, utterly meaningless — and nothing can stop it
```

Making fields `public` directly (instead of `private` with getters/setters) causes several real problems:

| Problem | Explanation |
|---------|-------------|
| **No validation** | Any value, however invalid, can be assigned with no check at all. |
| **No computed/derived logic** | You cannot later turn a field into a computed value without breaking every caller that read it as a plain field. |
| **Fragile API** | If you ever need to change the field's type, rename it, or add logic around it, every piece of code touching that field directly breaks. A method-based API can evolve internally without changing its public contract. |
| **No read-only enforcement** | A public field is always both readable and writable from outside; there is no way to expose "read but never write." |
| **Harder to debug/maintain** | With a setter, you can add a breakpoint or a log line in one place to catch every modification. With a public field, modifications can happen anywhere, with no single chokepoint. |
| **Breaks invariants across multiple fields** | If two fields must stay consistent with each other (e.g., `start` must be before `end`), only a method can check both together before allowing the change. |

```java
// A setter can protect an invariant across two fields; a public field cannot.
class DateRange {
    private LocalDate start;
    private LocalDate end;

    public void setEnd(LocalDate end) {
        if (end.isBefore(start)) {
            throw new IllegalArgumentException("end cannot be before start");
        }
        this.end = end;
    }
}
```

> Getters and setters look like more boilerplate at first, but they are the seam where validation, logging, derived values, and future changes all plug in — without ever breaking the classes that already depend on the object.

---

## 10. Packages and Access Control

A **package** is Java's way of grouping related classes together (and it also forms part of a class's fully qualified name, e.g. `com.raysi.accounts.Account`).

Packages interact directly with access control:

- **default (package-private)** access is defined **entirely** in terms of packages — "visible to anything in the same package."
- **`protected`** access extends visibility to subclasses **even across package boundaries**, which is the one case where being in a different package doesn't fully block access.
- **`public`** and **`private`** don't care about packages at all — `public` ignores package boundaries completely (visible everywhere), and `private` ignores them too, but in the opposite direction (visible to nothing outside the single class).

```text
com.raysi.accounts
    Account.java      (public class Account)
    AccountHelper.java (package-private class AccountHelper)

com.raysi.app
    Main.java          (imports Account, but CANNOT see AccountHelper)
```

```java
package com.raysi.accounts;

public class Account {
    // public API
}

class AccountHelper {     // package-private — an internal implementation detail
    static void validate(Account a) { /* ... */ }
}
```

```java
package com.raysi.app;

import com.raysi.accounts.Account;
// import com.raysi.accounts.AccountHelper;   // would not even compile — class isn't visible

class Main {
    public static void main(String[] args) {
        Account a = new Account();   // OK
        // AccountHelper is simply invisible from here
    }
}
```

### 10.1 Package-private as "module-internal" encapsulation

This is encapsulation **at the package level**, not just the class level: a package can expose a clean `public` API through a few classes, while keeping its internal helper classes, utility methods, and implementation details package-private — invisible to anyone outside that package, exactly the same idea as hiding a field behind a getter, just scaled up.

```text
Class-level encapsulation:    private field, public getter/setter
Package-level encapsulation:  package-private helper classes, public API classes
```

### 10.2 Importing does not bypass access control

Importing a class only lets you **refer to it by its simple name**; it never grants extra access. If a class or member isn't visible to you under the normal access rules, importing it changes nothing.

---

## 11. Immutable Classes

An **immutable class** is a class whose objects, once created, can **never have their state changed**. Every field is effectively "locked in" at construction time.

Encapsulation is the mechanism that makes immutability enforceable: if fields were public and mutable, nothing could stop external code from changing them after creation.

### 11.1 Rules for building an immutable class

1. Declare the class `final` (optional but common) so it cannot be subclassed into a mutable variant.
2. Make all fields `private` and `final`.
3. Provide **no setters** — only getters.
4. Initialize all fields fully inside the constructor.
5. If a field is a **mutable reference type** (like an array, `List`, or `Date`), never hand out the original reference — return a **defensive copy** instead.

```java
final class Point {
    private final int x;
    private final int y;

    public Point(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public int getX() { return x; }
    public int getY() { return y; }

    // No setX(), no setY() — the object can never change after construction
}
```

```java
Point p = new Point(3, 4);
// p.x = 10;        // COMPILE ERROR: x is private and final
// p.setX(10);       // COMPILE ERROR: no such method exists
```

### 11.2 Why immutability is valuable

| Benefit | Explanation |
|---------|-------------|
| **Thread-safe by default** | An object that can never change state can be freely shared across threads with no synchronization needed. |
| **Safe to share freely** | You can hand out the same instance to many callers without worrying that one caller's changes will affect another's. |
| **Predictable, easier to reason about** | A value, once created, behaves the same way forever — no "spooky action at a distance" from some other part of the code mutating it. |
| **Safe as keys in hash-based collections** | If an object's `hashCode()` could change after being inserted into a `HashMap`/`HashSet`, it could become unfindable — immutability guarantees this never happens. |

---

## 12. How `String` Achieves Immutability

`String` is Java's best-known immutable class, and it is built using exactly the rules in section 11.

```java
public final class String {           // final — cannot be subclassed
    private final char[] value;        // (conceptually; backed by a byte[] since Java 9)
    // ... no public setters exist anywhere on String
}
```

### 12.1 Every "modifying" operation actually creates a new object

Any method that looks like it changes a `String` — `concat()`, `toUpperCase()`, `replace()`, `substring()`, `trim()` — actually returns a **brand-new** `String` object, leaving the original untouched.

```java
String s1 = "hello";
String s2 = s1.concat(" world");

System.out.println(s1);   // "hello"        — unchanged
System.out.println(s2);   // "hello world"  — a new object

System.out.println(s1 == s2);   // false — genuinely different objects
```

```java
String a = "java";
a.toUpperCase();              // return value is discarded here!
System.out.println(a);        // "java" — a itself never changed

String b = a.toUpperCase();   // must capture the new object to use it
System.out.println(b);        // "JAVA"
```

### 12.2 The String Pool and why immutability makes it safe

Java keeps a special memory area called the **String Constant Pool** (inside the heap) to **reuse** identical string literals instead of creating duplicates.

```java
String x = "test";
String y = "test";
System.out.println(x == y);   // true — both refer to the SAME pooled object
```

This sharing is only safe **because** `String` is immutable: if `x` could be mutated, changing it through `x` would silently corrupt `y` too, since they're the same object. Immutability guarantees that sharing one object between many references is always safe.

```java
String c = new String("test");   // explicitly creates a separate object, bypassing the pool
System.out.println(x == c);       // false — different object
System.out.println(x.equals(c));  // true  — same content
```

### 12.3 Immutability also enables safe hashing and caching

`String` **caches its own hash code** the first time `hashCode()` is called, because the hash can never become stale — the content backing it can never change.

```java
// Conceptually, inside String:
private int hash;   // cached, computed once, reused forever
```

This is also why `String` is such a popular key type for `HashMap`: its hash code is guaranteed stable for the object's entire lifetime.

### 12.4 Summary: String's immutability checklist

| Rule from section 11 | How `String` applies it |
|------------------------|---------------------------|
| Class is `final` | `String` is declared `final` — cannot be subclassed |
| Fields are `private final` | The internal character/byte array is `private` and `final` |
| No setters | There is no `setChar()`, no `append()`-in-place — nothing mutates a `String` |
| Methods return new objects | `concat`, `substring`, `replace`, `toUpperCase`, etc. all return new `String`s |
| Defensive handling of internal data | The internal array is never exposed directly to callers |

---

## 13. Writing Your Own Immutable Class

Here is a complete, slightly more realistic example that also demonstrates **defensive copying** for a mutable field.

```java
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

final class Student {
    private final String name;
    private final int rollNumber;
    private final List<String> subjects;   // a mutable type (List) held internally

    public Student(String name, int rollNumber, List<String> subjects) {
        this.name = name;
        this.rollNumber = rollNumber;
        this.subjects = new ArrayList<>(subjects);   // defensive copy IN
    }

    public String getName() { return name; }
    public int getRollNumber() { return rollNumber; }

    public List<String> getSubjects() {
        return Collections.unmodifiableList(subjects);   // defensive copy/wrap OUT
    }
}
```

```java
List<String> subjects = new ArrayList<>();
subjects.add("Math");

Student s = new Student("Aashish", 101, subjects);

subjects.add("Physics");                 // modifying the ORIGINAL list after construction
System.out.println(s.getSubjects());     // still only [Math] — the copy inside Student is unaffected

s.getSubjects().add("Chemistry");        // throws UnsupportedOperationException — truly read-only
```

Without the two defensive-copy steps, a caller could mutate the `Student`'s internal subject list from outside in two different ways — either by changing the original list after passing it in, or by mutating whatever the getter returns — silently breaking immutability despite every field being `private final`.

---

## 14. Common Pitfalls

| # | Pitfall | Fix |
|---|---------|-----|
| 1 | Making fields `public` "just to make it compile faster" | Use `private` fields with getters/setters from the start |
| 2 | Assuming `protected` means "only subclasses" | It also means "same package," which is often broader than intended |
| 3 | Forgetting a top-level class can only be `public` or package-private | `private`/`protected` top-level classes don't compile — those modifiers are for members |
| 4 | Writing a setter with no validation at all | A setter that just assigns blindly provides no real benefit over a public field |
| 5 | Declaring fields `final` but still exposing the internal mutable object through a getter | Return a defensive copy or an unmodifiable wrapper for mutable fields |
| 6 | Calling a "modifying" `String` method and ignoring the return value | `str.toUpperCase();` alone does nothing — capture the result: `str = str.toUpperCase();` |
| 7 | Comparing `String`s with `==` assuming the pool always applies | `new String(...)` bypasses the pool — always use `.equals()` for content comparison |
| 8 | Thinking immutability means "cannot have any methods" | Immutable objects can have plenty of methods — they just never change the object's own state |
| 9 | Forgetting that `final` on a field only locks the **reference**, not deep mutability | `private final List<String> list` still lets callers mutate the list's contents unless you guard the getter |
| 10 | Using `getXxx()` naming for a `boolean` field | Convention prefers `isXxx()` for booleans (many libraries/frameworks expect this) |

---

## 15. Interview Questions

**Q1. What is encapsulation, and why is it important?**
Encapsulation is hiding an object's internal state and exposing controlled access through methods. It protects invariants, hides implementation details so they can change freely later, and lets the class validate or react to every state change in one place.

---

**Q2. What are the four access modifiers in Java, from most to least restrictive?**
`private` (class only) → default/package-private (same package) → `protected` (same package + subclasses in other packages) → `public` (everywhere).

---

**Q3. What is the difference between default and `protected` access?**
Default (package-private) access is visible only within the same package. `protected` includes everything default access includes, plus visibility to subclasses even in different packages.

---

**Q4. Why are public fields discouraged in favor of getters and setters?**
Public fields allow any external code to set any value with no validation, make it impossible to later add logic or enforce invariants without breaking callers, and offer no way to make a property read-only. Getters/setters create a stable seam where validation, logging, and future changes can be added without touching the class's public contract.

---

**Q5. Can a top-level class be `private`?**
No. A top-level class can only be `public` or default (package-private). `private` and `protected` are reserved for members — fields, methods, constructors, and nested classes.

---

**Q6. How do packages relate to access control?**
A package groups related classes and is the basis for default access (same-package visibility). `protected` extends visibility across package boundaries specifically for subclasses, while `public` and `private` ignore package boundaries entirely, in opposite directions.

---

**Q7. What makes a class immutable in Java?**
The class (often) is declared `final`, all fields are `private` and `final`, there are no setters, every field is fully set in the constructor, and any mutable field is defensively copied both coming in and going out (via the getter), so the internal state can never be altered after construction.

---

**Q8. How does `String` achieve immutability?**
`String` is `final` (cannot be subclassed), its internal character data is `private` and `final`, and it exposes no methods that modify it in place — every apparent "modification" (`concat`, `substring`, `toUpperCase`, etc.) returns a brand-new `String` object, leaving the original unchanged.

---

**Q9. Why is the String Pool safe only because `String` is immutable?**
The pool lets multiple references share the exact same `String` object for identical literals. If `String` were mutable, changing the string through one reference would silently affect every other reference sharing that pooled object. Immutability guarantees that shared objects can never be corrupted by one holder's changes.

---

**Q10. If a field is `private final List<String>`, is the class automatically immutable?**
Not necessarily. `final` only prevents the field from being reassigned to point at a different list — it does not stop the list's own contents from being changed. If the getter returns the actual internal list, callers can still call `.add()`/`.remove()` on it. True immutability requires returning a defensive copy or an unmodifiable wrapper from the getter (and ideally copying on the way in too).

---

## 16. Interview-Style Output Questions

**Q1**
```java
class Account {
    private double balance = 100;

    public double getBalance() { return balance; }
}

public class Main {
    public static void main(String[] args) {
        Account a = new Account();
        // a.balance = 500;
        System.out.println(a.getBalance());
    }
}
```
**Answer:** `100.0`. The commented-out line would not compile (`balance` is `private`); only the getter can be used.

---

**Q2**
```java
String s1 = "hi";
String s2 = "hi";
String s3 = new String("hi");

System.out.println(s1 == s2);
System.out.println(s1 == s3);
System.out.println(s1.equals(s3));
```
**Answer:** `true`, `false`, `true`. Literals share the pooled object; `new String(...)` creates a separate object with the same content.

---

**Q3**
```java
String s = "java";
s.concat(" rocks");
System.out.println(s);
```
**Answer:** `java`. The return value of `concat` was discarded, and `String` never mutates in place.

---

**Q4**
```java
final class Point {
    private final int x;
    Point(int x) { this.x = x; }
    int getX() { return x; }
}

Point p = new Point(5);
System.out.println(p.getX());
```
**Answer:** `5`. Compiles and runs fine — `final` fields just can't be reassigned after construction, which this code never attempts.

---

**Q5**
```java
import java.util.*;

class Team {
    private final List<String> members;
    Team(List<String> members) { this.members = members; }   // NOT copied
    List<String> getMembers() { return members; }             // NOT wrapped
}

List<String> list = new ArrayList<>(List.of("A", "B"));
Team t = new Team(list);
list.add("C");
System.out.println(t.getMembers());
```
**Answer:** `[A, B, C]`. Without a defensive copy in the constructor, the `Team`'s internal list is the very same object as the caller's `list`, so external mutation leaks straight through — this is NOT a truly immutable class despite the `final` field.

---

**Q6**
```java
package pkgA;
class Helper {              // package-private
    void show() { System.out.println("Helper"); }
}
```
```java
package pkgB;
import pkgA.Helper;         // attempt to use it from another package

public class Main {
    public static void main(String[] args) {
        Helper h = new Helper();
        h.show();
    }
}
```
**Answer:** Compile error. `Helper` is package-private (default access), so it is not visible outside `pkgA` at all — the import itself fails to resolve a usable type.

---

## 17. Quick Cheat Sheet

```text
ENCAPSULATION       Hide state (private fields) + expose controlled access (public methods)

ACCESS MODIFIERS    private            same class only
                    (none) / default   same package
                    protected          same package + subclasses (even other packages)
                    public             everywhere

GETTERS/SETTERS     getXxx() / setXxx(T)     isXxx() for booleans
                    Setters CAN validate, log, enforce invariants across fields
                    Getters CAN compute/derive values, or return defensive copies

PUBLIC FIELDS       Avoid for mutable/validated state:
                    no validation, no evolution, no read-only option, no single chokepoint

PACKAGES            Group related classes; basis for default access
                    public class in a package = its public API
                    package-private classes/members = internal implementation details

IMMUTABLE CLASS     final class (often)
                    private final fields
                    no setters
                    fully initialized in constructor
                    defensive copies for mutable fields, both in (constructor) and out (getter)

STRING              final class, private final backing data, no in-place mutation
                    "modifying" methods return NEW String objects
                    literals share objects via the String Pool — safe only because immutable
                    new String(...) bypasses the pool — use .equals(), not ==, for content
```

**Remember:**

1. Encapsulation means fields are `private`; access always goes through methods.
2. The four access levels, from tightest to loosest: `private` → default → `protected` → `public`.
3. Getters/setters aren't just boilerplate — they're the seam where validation and future change happen safely.
4. Packages define default access and let `protected` reach across package boundaries for subclasses.
5. An immutable class needs `final` fields, no setters, and defensive copies for any mutable field — `final` alone is not enough.
6. `String`'s immutability is what makes the String Pool, its cached hash code, and safe sharing across the whole JVM all possible.
