# Java Object Class Essentials: Detailed Notes

Every class in Java implicitly extends `java.lang.Object` (see the inheritance notes), which means every object automatically inherits a small set of fundamental methods. Three of them — `toString()`, `equals()`, and `hashCode()` — are overridden constantly in real code, and getting them right (or wrong) has consequences that ripple through collections, debugging output, and object comparison everywhere.

```java
class Student {
    String name;
    int rollNumber;

    Student(String name, int rollNumber) {
        this.name = name;
        this.rollNumber = rollNumber;
    }
}

Student s = new Student("Aashish", 101);
System.out.println(s);   // Student@1b6d3586  — the default Object.toString(), not very useful
```

---

## Table of Contents

1. [Why Override `Object`'s Methods?](#1-why-override-objects-methods)
2. [Overriding `toString()`](#2-overriding-tostring)
3. [Overriding `equals()`](#3-overriding-equals)
4. [`equals()` vs `==` for Objects](#4-equals-vs--for-objects)
5. [Overriding `hashCode()`](#5-overriding-hashcode)
6. [The `equals()`/`hashCode()` Contract](#6-the-equalshashcode-contract)
7. [Why the Contract Matters in Practice](#7-why-the-contract-matters-in-practice)
8. [A Correct, Complete Example](#8-a-correct-complete-example)
9. [Basics of `clone()`](#9-basics-of-clone)
10. [Shallow Clone vs Deep Clone](#10-shallow-clone-vs-deep-clone)
11. [Why `clone()` Is Often Avoided](#11-why-clone-is-often-avoided)
12. [Copy Constructors as the Preferred Alternative](#12-copy-constructors-as-the-preferred-alternative)
13. [Common Pitfalls](#13-common-pitfalls)
14. [Interview Questions](#14-interview-questions)
15. [Interview-Style Output Questions](#15-interview-style-output-questions)
16. [Quick Cheat Sheet](#16-quick-cheat-sheet)

---

## 1. Why Override `Object`'s Methods?

`Object` provides **default** implementations of `toString()`, `equals()`, and `hashCode()` that work for *any* object, but those defaults are almost never what you actually want for a custom class:

| Method | Default (`Object`) behavior |
|--------|-------------------------------|
| `toString()` | Returns `ClassName@hexHashCode` — not human-readable |
| `equals(Object o)` | Same as `==` — compares **references**, not content |
| `hashCode()` | Derived from the object's memory/identity (JVM-internal), unrelated to the object's field values |

```java
Student s1 = new Student("Aashish", 101);
Student s2 = new Student("Aashish", 101);

System.out.println(s1);              // Student@<hash>        — unreadable
System.out.println(s1.equals(s2));   // false                 — different objects, even with identical data
```

Without overriding these, two `Student` objects holding the exact same data are treated as completely unrelated — which is rarely the intent for a class meant to represent a value (a student, a point, a money amount, etc.).

---

## 2. Overriding `toString()`

`toString()` is called automatically whenever an object is used where a `String` is expected — `System.out.println(obj)`, string concatenation (`"Value: " + obj`), and more. Overriding it gives a meaningful, readable representation.

```java
class Student {
    String name;
    int rollNumber;

    Student(String name, int rollNumber) {
        this.name = name;
        this.rollNumber = rollNumber;
    }

    @Override
    public String toString() {
        return "Student{name='" + name + "', rollNumber=" + rollNumber + "}";
    }
}
```

```java
Student s = new Student("Aashish", 101);
System.out.println(s);                     // Student{name='Aashish', rollNumber=101}
System.out.println("Info: " + s);           // Info: Student{name='Aashish', rollNumber=101}
```

### 2.1 `toString()` is purely for readability — it has no effect on `equals()`

Overriding `toString()` changes only how the object **prints**; it has no bearing whatsoever on equality comparisons (`equals()`/`==`) or hashing (`hashCode()`) unless those are also separately overridden.

### 2.2 Signature rules

```java
public String toString()   // must be exactly this: public, no parameters, returns String
```

Since `toString()` is inherited from `Object` as `public`, the override cannot narrow its access (same rule as all overriding — see the polymorphism notes).

---

## 3. Overriding `equals()`

By default, `equals()` behaves identically to `==` (reference equality). Overriding it lets you define **logical/content equality** — "these two objects represent the same value," even if they are two distinct objects in memory.

```java
class Student {
    String name;
    int rollNumber;

    Student(String name, int rollNumber) {
        this.name = name;
        this.rollNumber = rollNumber;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;                        // same reference — trivially equal
        if (obj == null || getClass() != obj.getClass()) return false;   // null or different class — not equal
        Student other = (Student) obj;                         // safe cast, class already checked
        return rollNumber == other.rollNumber && name.equals(other.name);
    }
}
```

```java
Student s1 = new Student("Aashish", 101);
Student s2 = new Student("Aashish", 101);

System.out.println(s1 == s2);        // false — different objects
System.out.println(s1.equals(s2));   // true  — same content, thanks to the override
```

### 3.1 The standard `equals()` template

The pattern shown above is the conventional, defensive structure for a correct `equals()`:

```text
1. If obj is the SAME reference as this -> return true immediately (fast path).
2. If obj is null, or obj's class doesn't match this object's class -> return false.
3. Cast obj to the correct type (safe now, since the class was checked).
4. Compare the relevant fields for equality.
```

### 3.2 Signature rules

```java
public boolean equals(Object obj)   // must take an Object parameter, not the specific class type
```

```java
// COMMON MISTAKE: this does NOT override Object.equals() — it OVERLOADS it instead
public boolean equals(Student obj) {   // wrong parameter type
    return this.rollNumber == obj.rollNumber;
}
```

Because the parameter type is wrong, the compiler treats this as a brand-new overload rather than an override — `@Override` would catch this mistake immediately (see the polymorphism notes on why `@Override` matters).

### 3.3 Comparing fields safely

```java
// Comparing a reference-type field: use its own .equals(), never ==
return name.equals(other.name);

// Comparing primitive fields: use ==
return rollNumber == other.rollNumber;

// Null-safe field comparison (useful when a field might be null):
return java.util.Objects.equals(this.name, other.name);
```

`java.util.Objects.equals(a, b)` safely returns `true` if both are `null`, `false` if only one is `null`, and otherwise delegates to `a.equals(b)` — avoiding manual null checks scattered everywhere.

---

## 4. `equals()` vs `==` for Objects

This directly extends the reference-equality rules covered earlier (see the operators notes' section on `==` for primitives vs references): `==` **always** compares references for object types, with no exceptions, regardless of whether `equals()` has been overridden.

| | `==` | `.equals()` |
|---|------|--------------|
| For **primitives** | Compares actual values | N/A — primitives have no `.equals()` method |
| For **objects**, default behavior | Compares references (identity) | Same as `==` by default (inherited from `Object`), unless overridden |
| For **objects**, after overriding `equals()` | Still always compares references | Compares whatever logical/content equality the class defines |
| Can be customized? | No — `==` behavior is fixed by the language | Yes — this is the entire point of overriding it |

```java
String a = "hello";
String b = "hello";
String c = new String("hello");

System.out.println(a == b);         // true  — same pooled literal (see the encapsulation notes on the String Pool)
System.out.println(a == c);         // false — different objects
System.out.println(a.equals(c));    // true  — String overrides equals() to compare content
```

```java
Student s1 = new Student("Aashish", 101);
Student s2 = new Student("Aashish", 101);

System.out.println(s1 == s2);        // false — ALWAYS false for two separately created objects, no matter what
System.out.println(s1.equals(s2));   // true  — only because Student overrides equals()
```

### 4.1 The classic trap: forgetting to override `equals()`

```java
class Student {
    String name;
    Student(String name) { this.name = name; }
    // no equals() override
}

Student a = new Student("Aashish");
Student b = new Student("Aashish");
System.out.println(a.equals(b));   // false — Object's default equals() is just == underneath
```

This is exactly the reference-equality gotcha that first shows up with wrapper types and `String` early on (`Integer`/`new String(...)` comparisons) — it's the same underlying rule, just now applying to a class you wrote yourself. **If a class represents a value rather than a unique identity, override `equals()` (and `hashCode()`, section 5) or risk silent, confusing bugs** wherever that class is compared or placed in a collection.

### 4.2 When `==` is actually the right choice

Not every class should override `equals()`. Some objects are meant to represent **unique identity** rather than a comparable value — in those cases, the default reference-equality behavior (`==` and the inherited `equals()`) is exactly correct and shouldn't be changed.

```java
Thread t1 = new Thread();
Thread t2 = new Thread();
// t1.equals(t2) correctly stays false by default — two Thread objects are never "the same thread" by content
```

---

## 5. Overriding `hashCode()`

`hashCode()` returns an `int` meant to represent the object in a way suitable for **hash-based collections** (`HashMap`, `HashSet`, `Hashtable`). The default implementation (from `Object`) derives a value from the object's identity/memory location, which is why it must be overridden alongside `equals()`.

```java
class Student {
    String name;
    int rollNumber;

    Student(String name, int rollNumber) {
        this.name = name;
        this.rollNumber = rollNumber;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Student other = (Student) obj;
        return rollNumber == other.rollNumber && name.equals(other.name);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(name, rollNumber);   // standard, convenient way to combine fields
    }
}
```

### 5.1 `java.util.Objects.hash(...)` is the conventional way to implement this

```java
@Override
public int hashCode() {
    return java.util.Objects.hash(name, rollNumber);
}
```

Internally, this combines the hash codes of all the given fields in a standard, well-distributed way (similar to how it's manually done with a prime-number multiplier pattern, e.g. `31 * result + field.hashCode()`), without you needing to hand-roll that logic yourself.

### 5.2 Signature rules

```java
public int hashCode()   // must be exactly this: public, no parameters, returns int
```

---

## 6. The `equals()`/`hashCode()` Contract

Java defines a strict **contract** between these two methods that every overriding class must honor:

```text
1. If a.equals(b) is true, then a.hashCode() MUST equal b.hashCode().
2. If a.hashCode() == b.hashCode(), a.equals(b) is NOT required to be true
   (different objects CAN share a hash code — this is called a "collision," and it's allowed).
3. hashCode() must return the SAME value every time it's called on the SAME object,
   as long as no equals()-relevant field has changed.
```

The critical, most commonly broken rule is **#1: equal objects must have equal hash codes.** The reverse is not required — two unequal objects are allowed to share the same hash code (collisions are expected and handled internally by hash-based collections); what's forbidden is **two objects that `.equals()` says are the same, but that report different hash codes.**

```java
// CORRECT relationship:
a.equals(b) == true   =>   a.hashCode() == b.hashCode()     (MANDATORY)
a.hashCode() == b.hashCode()   =>   a.equals(b)  (NOT guaranteed, collisions are fine)
```

### 6.1 Why you must ALWAYS override both together, never just one

```java
class BrokenStudent {
    String name;
    BrokenStudent(String name) { this.name = name; }

    @Override
    public boolean equals(Object obj) {           // overridden...
        if (!(obj instanceof BrokenStudent)) return false;
        return name.equals(((BrokenStudent) obj).name);
    }
    // ...but hashCode() is NOT overridden — still uses Object's identity-based default!
}
```

```java
BrokenStudent a = new BrokenStudent("Aashish");
BrokenStudent b = new BrokenStudent("Aashish");

System.out.println(a.equals(b));        // true  — content is equal
System.out.println(a.hashCode() == b.hashCode());   // false (almost certainly) — identity-based hash, different objects
```

This single class **violates the contract**, and the consequences show up specifically — and dangerously silently — inside hash-based collections (see section 7).

---

## 7. Why the Contract Matters in Practice

Hash-based collections like `HashMap` and `HashSet` use `hashCode()` to decide **which bucket** to place an object in, and then use `equals()` **within that bucket** to confirm a match. If two equal objects report different hash codes, they can end up in **different buckets entirely**, and the collection will never find a match between them — even though `.equals()` would have said they're the same.

```java
Set<BrokenStudent> set = new HashSet<>();
set.add(new BrokenStudent("Aashish"));

System.out.println(set.contains(new BrokenStudent("Aashish")));
// FALSE — even though equals() would return true for these two objects!
// contains() first computes the hash code, looks in the WRONG bucket (different hash), and never even calls equals()
```

```java
Map<Student, String> map = new HashMap<>();
Student key1 = new Student("Aashish", 101);
map.put(key1, "First Year");

Student key2 = new Student("Aashish", 101);   // equals() says this equals key1... if hashCode() agrees
System.out.println(map.get(key2));
// Only returns "First Year" correctly if Student properly overrides BOTH equals() AND hashCode()
// consistently — otherwise this silently returns null, a classic, hard-to-diagnose bug
```

This is precisely why the rule is always stated as a pair: **override `equals()` and `hashCode()` together, or not at all.** Overriding only one is worse than overriding neither, because it creates a class that *looks* correct in simple tests but breaks specifically — and silently — the moment it's used as a `HashMap` key or placed in a `HashSet`.

---

## 8. A Correct, Complete Example

```java
import java.util.Objects;

class Student {
    private final String name;
    private final int rollNumber;

    public Student(String name, int rollNumber) {
        this.name = name;
        this.rollNumber = rollNumber;
    }

    @Override
    public String toString() {
        return "Student{name='" + name + "', rollNumber=" + rollNumber + "}";
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Student other = (Student) obj;
        return rollNumber == other.rollNumber && Objects.equals(name, other.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, rollNumber);
    }
}
```

```java
Student s1 = new Student("Aashish", 101);
Student s2 = new Student("Aashish", 101);

System.out.println(s1);                 // Student{name='Aashish', rollNumber=101}
System.out.println(s1 == s2);            // false
System.out.println(s1.equals(s2));       // true
System.out.println(s1.hashCode() == s2.hashCode());   // true — contract satisfied

java.util.Set<Student> set = new java.util.HashSet<>();
set.add(s1);
System.out.println(set.contains(s2));    // true — works correctly now
```

Most modern IDEs (and libraries like Lombok) can **auto-generate** correct `toString()`, `equals()`, and `hashCode()` implementations following exactly this pattern — but understanding what they generate and why is essential for debugging when something behaves unexpectedly.

---

## 9. Basics of `clone()`

`clone()` is a method on `Object` intended to create and return a **copy** of the calling object. Using it correctly requires several steps that are easy to get wrong.

```java
class Point implements Cloneable {       // MUST implement Cloneable, or clone() throws at runtime
    int x, y;
    Point(int x, int y) { this.x = x; this.y = y; }

    @Override
    public Point clone() {
        try {
            return (Point) super.clone();    // calls Object's native clone() implementation
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();        // can't actually happen once Cloneable is implemented
        }
    }
}
```

```java
Point p1 = new Point(3, 4);
Point p2 = p1.clone();

p2.x = 99;
System.out.println(p1.x);   // 3 — p1 and p2 are genuinely separate objects
```

### 9.1 Why `Cloneable` must be implemented

`Object.clone()` checks, at runtime, whether the object's class implements the `Cloneable` **marker interface** (an interface with no methods at all — it exists purely as a flag). If the class does **not** implement it, `clone()` throws `CloneNotSupportedException`.

```java
class NotCloneable {   // does NOT implement Cloneable
    @Override
    public NotCloneable clone() throws CloneNotSupportedException {
        return (NotCloneable) super.clone();   // throws CloneNotSupportedException at runtime, every time
    }
}
```

### 9.2 `super.clone()` performs a field-by-field (shallow) copy

`Object`'s native `clone()` implementation copies each field's value directly — for primitives, this copies the actual value; for reference-type fields, it copies only the **reference**, not the object it points to. This is exactly a **shallow copy** (see sections below, and the constructors notes on shallow vs deep copy constructors).

---

## 10. Shallow Clone vs Deep Clone

```java
class Address {
    String city;
    Address(String city) { this.city = city; }
}

class Person implements Cloneable {
    String name;
    Address address;      // a reference-type field

    Person(String name, Address address) {
        this.name = name;
        this.address = address;
    }

    @Override
    public Person clone() {
        try {
            return (Person) super.clone();   // SHALLOW — Address reference is copied, not the Address object itself
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }
}
```

```java
Person original = new Person("Aashish", new Address("Bengaluru"));
Person copy = original.clone();

copy.address.city = "Mumbai";
System.out.println(original.address.city);   // "Mumbai" — changed! Both share the SAME Address object
```

### 10.1 Achieving a deep clone requires manual extra work

```java
class Person implements Cloneable {
    String name;
    Address address;

    Person(String name, Address address) {
        this.name = name;
        this.address = address;
    }

    @Override
    public Person clone() {
        try {
            Person cloned = (Person) super.clone();
            cloned.address = new Address(this.address.city);   // manually clone the nested object too
            return cloned;
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }
}
```

```java
Person original = new Person("Aashish", new Address("Bengaluru"));
Person copy = original.clone();

copy.address.city = "Mumbai";
System.out.println(original.address.city);   // "Bengaluru" — unaffected now, truly independent
```

Every reference-type field in a class requires this kind of manual handling to achieve a true deep clone — and this manual work **compounds** for every level of nested objects, every subclass that adds new reference fields, and every collection field (which itself needs its own deep-copy logic).

---

## 11. Why `clone()` Is Often Avoided

`clone()` is widely considered one of the more awkward, failure-prone corners of the Java API, for several concrete reasons:

| Problem | Explanation |
|---------|-------------|
| **Requires `Cloneable`, a near-empty marker interface** | `Cloneable` declares no methods — it only flips an internal flag `Object.clone()` checks at runtime. Forgetting to implement it causes a runtime exception, not a compile-time error. |
| **Checked exception for no good reason** | `CloneNotSupportedException` must be caught or declared, even though implementing `Cloneable` makes it effectively impossible to actually occur in your own class — pure boilerplate. |
| **Shallow by default, silently** | `super.clone()` only copies references for non-primitive fields; forgetting to deep-copy a mutable field produces a bug that often isn't obvious until two "independent" objects start mysteriously affecting each other. |
| **Breaks down badly with inheritance** | Every subclass that adds new reference-type fields must remember to override `clone()` again and handle its own new fields — easy to forget, and the compiler won't catch it for you. |
| **No constructor runs** | `clone()` does not call any constructor at all — it conjures up a new object by copying raw field values. This can bypass validation logic, `final` field initialization expectations, and any invariants a constructor is supposed to enforce. |
| **Doesn't play well with `final` fields** | Since `clone()` doesn't go through a constructor, immutable classes with `final` fields are awkward or impossible to clone cleanly using this mechanism at all. |
| **The language/standard library itself shows mixed signals** | Many respected sources (including Java's own creators and widely cited style guides) recommend avoiding `Cloneable`/`clone()` in new code altogether, in favor of copy constructors or static factory methods. |

```java
// The exception-handling boilerplate alone signals something is off:
try {
    return (MyClass) super.clone();
} catch (CloneNotSupportedException e) {
    throw new AssertionError();   // "this can never happen" — a strong smell that the API is awkward
}
```

---

## 12. Copy Constructors as the Preferred Alternative

A **copy constructor** — a constructor that takes another instance of the same class and copies its fields — achieves everything `clone()` does, without any of its drawbacks (see the constructors notes for the original introduction of this pattern).

```java
class Person {
    String name;
    Address address;

    Person(String name, Address address) {
        this.name = name;
        this.address = address;
    }

    // Copy constructor — deep copy, explicit and readable
    Person(Person other) {
        this.name = other.name;
        this.address = new Address(other.address.city);   // deliberately, visibly deep-copied
    }
}
```

```java
Person original = new Person("Aashish", new Address("Bengaluru"));
Person copy = new Person(original);     // clear, ordinary constructor call — no casting, no checked exception

copy.address.city = "Mumbai";
System.out.println(original.address.city);   // "Bengaluru" — correctly independent
```

### 12.1 Why copy constructors win, point for point

| Problem with `clone()` | How a copy constructor solves it |
|--------------------------|--------------------------------------|
| Requires `Cloneable` marker interface | No special interface needed at all |
| `CloneNotSupportedException` boilerplate | No checked exception involved whatsoever |
| Shallow by default, easy to forget | Every field copy is written out explicitly and visibly — shallow vs deep is a deliberate choice you can see in the code |
| Breaks with inheritance silently | Each subclass simply writes its own copy constructor calling `super(other)`, following normal constructor-chaining rules |
| Bypasses the constructor and its validation | Runs through the actual constructor (or another constructor it delegates to), so invariants and validation logic apply normally |
| Awkward with `final` fields | Works perfectly — `final` fields are simply assigned once, in the copy constructor, exactly like any other constructor |
| Requires an explicit downcast from `Object` | No cast of any kind needed — the return type is just the class itself |

### 12.2 A static factory method is another common, equally good alternative

```java
class Person {
    String name;
    Address address;

    private Person(String name, Address address) {
        this.name = name;
        this.address = address;
    }

    static Person copyOf(Person other) {
        return new Person(other.name, new Address(other.address.city));
    }
}
```

```java
Person copy = Person.copyOf(original);
```

### 12.3 Practical takeaway

> For new code, prefer a **copy constructor** (or a static factory method that does the same thing) over implementing `Cloneable`/`clone()`. Reserve knowledge of `clone()` mainly for reading and understanding existing/legacy code, and for the rare case where you're working directly with an API that specifically requires `Cloneable` (like `java.util.ArrayList`, which does implement it internally).

---

## 13. Common Pitfalls

| # | Pitfall | Fix |
|---|---------|-----|
| 1 | Forgetting to override `equals()` for a class meant to represent a value | Without it, `.equals()` silently falls back to reference equality, just like `==` |
| 2 | Overriding `equals()` but forgetting `hashCode()` (or vice versa) | Always override both together — violating the contract breaks `HashMap`/`HashSet` silently |
| 3 | Writing `equals(Student obj)` instead of `equals(Object obj)` | Wrong parameter type creates an overload, not an override — use `@Override` to catch this immediately |
| 4 | Using `==` to compare field values inside a custom `equals()` for reference-type fields | Use `.equals()` (or `Objects.equals()` for null-safety) on reference-type fields, `==` only for primitives |
| 5 | Assuming `toString()` affects `equals()`/`hashCode()` | They are completely independent; overriding one does nothing to the others |
| 6 | Forgetting `Cloneable` before calling `clone()` | Throws `CloneNotSupportedException` at runtime, not a compile-time error |
| 7 | Assuming `super.clone()` performs a deep copy | It performs a shallow, field-by-field copy; reference-type fields still point to the same shared objects |
| 8 | Forgetting to re-override `clone()` in a subclass that adds new reference fields | The parent's shallow/deep logic won't automatically account for the subclass's new fields |
| 9 | Expecting `clone()` to run the class's constructor | It doesn't — fields are copied directly, bypassing any constructor validation or logic entirely |
| 10 | Reaching for `clone()`/`Cloneable` in new code by default | Prefer a copy constructor or a static factory method — simpler, safer, and more explicit |
| 11 | Assuming two objects with the same `hashCode()` must be `.equals()` | Not required — hash collisions between unequal objects are normal and expected |
| 12 | Using mutable fields as `HashMap` keys and then mutating them after insertion | If a key's `hashCode()`-relevant fields change after being placed in the map, it can become unfindable; prefer immutable keys |

---

## 14. Interview Questions

**Q1. Why would you override `toString()`, `equals()`, and `hashCode()`?**
The default `Object` implementations aren't useful for most custom classes: `toString()` prints an unreadable class-name-plus-hashcode string, and `equals()`/`hashCode()` are based purely on object identity rather than the object's actual field values. Overriding them provides readable output and content-based equality, which is usually what a class representing a value actually needs.

---

**Q2. What is the difference between `==` and `.equals()` for objects?**
`==` always compares references (identity) for object types and cannot be customized. `.equals()` does the same by default (since `Object.equals()` is implemented using `==`), but can be overridden to define logical/content equality instead — comparing what the objects represent, not where they live in memory.

---

**Q3. What is the contract between `equals()` and `hashCode()`?**
If two objects are `.equals()`, they must return the same `hashCode()`. The reverse is not required — two unequal objects may share a hash code (a collision), which is normal. `hashCode()` must also be consistent: calling it repeatedly on the same, unchanged object must always return the same value.

---

**Q4. What happens if you override `equals()` but not `hashCode()`?**
The class violates the equals/hashCode contract. Two objects that are logically equal (per your `equals()`) can end up with different hash codes (since the default `hashCode()` is identity-based), causing them to be placed in different buckets inside a `HashMap`/`HashSet`. This makes lookups silently fail even when `.equals()` would say the objects match.

---

**Q5. Why must `equals()` take an `Object` parameter rather than the specific class type?**
Because it's overriding `Object.equals(Object)`. If the parameter type is anything else, the compiler treats it as a new overload rather than an override of the inherited method, and the "overridden" version is simply never invoked by code (like collections) that calls `equals(Object)` polymorphically. `@Override` catches this mistake immediately.

---

**Q6. What must a class do before calling `clone()`?**
It must implement the `Cloneable` marker interface. `Object.clone()` checks at runtime whether the calling object's class implements `Cloneable`; if not, it throws `CloneNotSupportedException`.

---

**Q7. Does `Object.clone()` (via `super.clone()`) perform a shallow or deep copy?**
A shallow copy. Primitive fields are copied by value, but reference-type fields are copied as references only — the cloned object and the original end up sharing the same underlying nested objects, unless the overriding `clone()` method manually deep-copies each such field itself.

---

**Q8. Why is `clone()` often avoided in modern Java code?**
It requires implementing an essentially empty marker interface (`Cloneable`), forces handling a checked exception that can't realistically occur once that interface is implemented, defaults to a shallow copy that's easy to forget to deepen, doesn't run through the class's constructor (bypassing validation and awkward with `final` fields), and becomes fragile across inheritance hierarchies where subclasses must remember to extend the cloning logic themselves.

---

**Q9. Why are copy constructors preferred over `clone()`?**
A copy constructor is an ordinary constructor, so it requires no special interface, no checked exception, and no cast. It runs through normal constructor logic (so validation and `final` field assignment work naturally), and whether each field is shallow- or deep-copied is written out explicitly and visibly in the code, rather than depending on hidden default behavior.

---

**Q10. Can two unequal objects have the same `hashCode()`?**
Yes — this is called a hash collision, and it is both allowed and expected by the `equals()`/`hashCode()` contract. What is never allowed is the reverse: two objects that `.equals()` reports as equal must always share the same `hashCode()`.

---

## 15. Interview-Style Output Questions

**Q1**
```java
class Point {
    int x, y;
    Point(int x, int y) { this.x = x; this.y = y; }
}
Point p1 = new Point(1, 2);
Point p2 = new Point(1, 2);
System.out.println(p1 == p2);
System.out.println(p1.equals(p2));
```
**Answer:** `false`, `false`. Without an overridden `equals()`, it behaves exactly like `==` — pure reference comparison.

---

**Q2**
```java
class Point {
    int x, y;
    Point(int x, int y) { this.x = x; this.y = y; }
    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Point)) return false;
        Point p = (Point) obj;
        return x == p.x && y == p.y;
    }
    // hashCode() NOT overridden
}
java.util.Set<Point> set = new java.util.HashSet<>();
set.add(new Point(1, 2));
System.out.println(set.contains(new Point(1, 2)));
```
**Answer:** `false` (almost certainly). `equals()` is overridden, but `hashCode()` is not — the two `Point` objects likely land in different hash buckets, so `contains()` never even gets to call `equals()` on the matching element.

---

**Q3**
```java
class Box implements Cloneable {
    int value = 10;
    @Override
    public Box clone() {
        try { return (Box) super.clone(); }
        catch (CloneNotSupportedException e) { throw new AssertionError(); }
    }
}
Box a = new Box();
Box b = a.clone();
b.value = 99;
System.out.println(a.value);
```
**Answer:** `10`. Primitive fields are copied by value during a shallow clone, so `a` and `b` have independent `int` values.

---

**Q4**
```java
class Wrapper implements Cloneable {
    int[] data = {1, 2, 3};
    @Override
    public Wrapper clone() {
        try { return (Wrapper) super.clone(); }
        catch (CloneNotSupportedException e) { throw new AssertionError(); }
    }
}
Wrapper a = new Wrapper();
Wrapper b = a.clone();
b.data[0] = 999;
System.out.println(a.data[0]);
```
**Answer:** `999`. The array field is a reference type — a shallow clone copies the reference only, so `a.data` and `b.data` point to the exact same array.

---

**Q5**
```java
class Item {
    String name;
    Item(String name) { this.name = name; }
    @Override
    public boolean equals(Item obj) {   // wrong parameter type!
        return this.name.equals(obj.name);
    }
}
Item a = new Item("Pen");
Item b = new Item("Pen");
System.out.println(a.equals(b));
Object o = b;
System.out.println(a.equals(o));
```
**Answer:** `true`, then `false`. The first call resolves to the overloaded `equals(Item)` and works as written. The second call, with `o` declared as `Object`, resolves to `Object`'s default `equals(Object)` (since no real override of it exists) — pure reference comparison, which is `false`.

---

## 16. Quick Cheat Sheet

```text
toString()     Inherited default: ClassName@hexHashCode (unreadable)
               Override to return a meaningful String; called automatically by println/concatenation
               Independent of equals()/hashCode() entirely

equals()       Inherited default: same as == (reference equality)
               Override signature: public boolean equals(Object obj)   <- MUST be Object, not the class type
               Standard template: same-reference check -> null/class check -> cast -> compare fields
               Use .equals() (or Objects.equals()) for reference fields, == for primitives

== vs .equals()  ==        ALWAYS reference comparison for objects, never customizable
                 .equals() Reference comparison by DEFAULT, but can be overridden for content comparison

hashCode()     Inherited default: identity-based (unrelated to field values)
               Override signature: public int hashCode()
               Standard approach: return Objects.hash(field1, field2, ...);

THE CONTRACT   a.equals(b) == true   =>  a.hashCode() == b.hashCode()      (MANDATORY)
               a.hashCode() == b.hashCode()  =>  a.equals(b)                (NOT required — collisions OK)
               ALWAYS override equals() and hashCode() TOGETHER, never just one

clone()        Requires implementing Cloneable (marker interface, no methods)
               Must catch/declare CloneNotSupportedException
               super.clone() = SHALLOW copy: primitives copied by value, references copied AS references
               Deep clone needs manual, explicit copying of every reference-type field
               Does NOT run the class's constructor — bypasses validation entirely

PREFER INSTEAD  Copy constructor:   ClassName(ClassName other) { this.field = other.field; ... }
                Static factory:     static ClassName copyOf(ClassName other) { ... }
                No interface, no checked exception, no cast, runs through real constructor logic,
                shallow vs deep copying is explicit and visible in the code
```

**Remember:**

1. `toString()`, `equals()`, and `hashCode()` all have default implementations from `Object` that are rarely what a custom class actually wants.
2. `==` never changes meaning for objects — it's always reference comparison; `.equals()` is the only one of the two that can be customized.
3. Overriding `equals()` without `hashCode()` (or vice versa) is a classic, silent bug — always do both together, following the strict contract between them.
4. `clone()`/`Cloneable` is shallow by default, bypasses constructors, and is generally considered an awkward corner of the API.
5. Prefer a copy constructor (or a static factory method) over `clone()` for new code — it's simpler, safer, and makes shallow-vs-deep copying an explicit, visible choice.
