# Java Enums as a Special Class Type: Detailed Notes

An **enum** (short for "enumeration") is a special kind of class that represents a **fixed, known set of constants**. Instead of using arbitrary `int`s or `String`s to represent a limited set of options (a classic source of bugs — nothing stops an invalid value), an enum gives you a **type-safe**, self-documenting set of named values.

```java
enum Day {
    MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY
}
```

```java
Day today = Day.MONDAY;
System.out.println(today);   // MONDAY
```

Under the hood, every enum **is actually a class** — specifically, one that implicitly extends `java.lang.Enum` — and each of its constants (`MONDAY`, `TUESDAY`, ...) is a `public static final` instance of that class. This is why enums can have fields, constructors, methods, and even implement interfaces, just like any other class.

---

## Table of Contents

1. [Why Enums Instead of Plain Constants](#1-why-enums-instead-of-plain-constants)
2. [Enum Basics](#2-enum-basics)
3. [Enums Are Classes Under the Hood](#3-enums-are-classes-under-the-hood)
4. [Built-in Enum Methods](#4-built-in-enum-methods)
5. [Enums with Fields and Constructors](#5-enums-with-fields-and-constructors)
6. [Enums with Methods](#6-enums-with-methods)
7. [Constant-Specific Method Bodies](#7-constant-specific-method-bodies)
8. [Enum Implementing an Interface](#8-enum-implementing-an-interface)
9. [Enum Constructors Are Always Private](#9-enum-constructors-are-always-private)
10. [`switch` on Enums](#10-switch-on-enums)
11. [Enums in Collections and Specialized Enum Collections](#11-enums-in-collections-and-specialized-enum-collections)
12. [Common Pitfalls](#12-common-pitfalls)
13. [Interview Questions](#13-interview-questions)
14. [Interview-Style Output Questions](#14-interview-style-output-questions)
15. [Quick Cheat Sheet](#15-quick-cheat-sheet)

---

## 1. Why Enums Instead of Plain Constants

Before enums (pre-Java 5), a fixed set of options was often represented with plain `int` or `String` constants — a pattern with real, recurring problems.

```java
class Day {
    static final int MONDAY = 0;
    static final int TUESDAY = 1;
    // ...
}

void schedule(int day) {
    if (day == Day.MONDAY) { /* ... */ }
}

schedule(99);         // compiles fine — no type safety at all, 99 isn't a valid "day"
schedule(Day.MONDAY);
```

### 1.1 Problems with the "int constant" pattern

| Problem | Explanation |
|---------|-------------|
| **No type safety** | Any `int` can be passed where a "day" is expected — nothing stops `schedule(99)` from compiling and running |
| **No namespace** | Constants from unrelated groups can be accidentally mixed up if their underlying values happen to overlap |
| **Poor readability** | Debugging output just shows a raw number (`0`), not a meaningful name |
| **No compile-time exhaustiveness checking** | Nothing ensures every valid value is actually handled somewhere |

### 1.2 Enums solve all of this

```java
enum Day { MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY }

void schedule(Day day) {
    if (day == Day.MONDAY) { /* ... */ }
}

// schedule(99);          // COMPILE ERROR — 99 is not a Day
schedule(Day.MONDAY);     // the ONLY valid way to call this, by construction
```

With an enum, the **type itself** enforces that only one of the defined constants can ever be used — there's no way to accidentally pass an invalid value, because invalid values simply don't exist for that type.

---

## 2. Enum Basics

```java
enum Season {
    SPRING, SUMMER, AUTUMN, WINTER
}
```

```java
Season s = Season.SUMMER;
System.out.println(s);                  // SUMMER        — toString() is overridden by default to show the constant's name
System.out.println(s.name());           // SUMMER        — the exact declared name
System.out.println(s.ordinal());        // 1             — zero-based POSITION in the declaration order
```

### 2.1 Comparing enum constants

Enum constants can be compared with `==` safely and correctly, because **each constant is a single, unique object** — there is exactly one `Season.SUMMER` in the entire JVM, ever.

```java
Season s1 = Season.SUMMER;
Season s2 = Season.SUMMER;
System.out.println(s1 == s2);          // true — both refer to the exact same singleton constant
System.out.println(s1.equals(s2));      // true — also works, and behaves identically to ==
```

> This is a notable exception to the usual rule-of-thumb about preferring `.equals()` over `==` for object types (see the Object-class-essentials notes) — for enums, `==` is both safe and idiomatic, since there's never more than one instance of any given constant.

### 2.2 Enums in a `switch` statement

Enums pair especially well with `switch` — covered in full in section 10, since it ties back to the switch statement fundamentals from earlier study.

### 2.3 Declaring an enum: top-level or nested

An enum can be declared as its own top-level type (in its own file, or alongside other classes), or nested inside another class — nested enum declarations are implicitly `static`, even without writing the keyword.

```java
class TrafficLight {
    enum Color { RED, YELLOW, GREEN }    // implicitly static, nested enum

    Color current = Color.RED;
}
```

```java
TrafficLight.Color c = TrafficLight.Color.GREEN;
```

---

## 3. Enums Are Classes Under the Hood

Every enum implicitly **extends `java.lang.Enum<EnumType>`**, which is why `enum` declarations **cannot** use `extends` themselves (the slot is already taken) — but they **can** `implements` interfaces (section 8), exactly like any other class.

```java
enum Season {
    SPRING, SUMMER, AUTUMN, WINTER
}
// conceptually similar to (not literally valid syntax, but illustrates the idea):
// final class Season extends Enum<Season> {
//     public static final Season SPRING = new Season();
//     public static final Season SUMMER = new Season();
//     ...
// }
```

Each named constant (`SPRING`, `SUMMER`, ...) is a `public static final` instance of the enum type itself — this is exactly why enum constants behave like singletons, and why `Season.SPRING` can be referenced anywhere `Season` is needed, just like a regular static field reference.

### 3.1 An enum class is implicitly `final`

Because every constant must be an instance of the enum type **exactly**, an enum class cannot be subclassed — attempting `extends SomeEnum` on another class is a compile error, mirroring how a `final` class behaves (see the static/final notes).

---

## 4. Built-in Enum Methods

Every enum automatically inherits several useful methods from `java.lang.Enum`.

| Method | Purpose |
|--------|---------|
| `name()` | Returns the constant's exact declared name as a `String` |
| `ordinal()` | Returns the constant's zero-based position in the declaration order |
| `toString()` | By default, returns the same as `name()` — but **can be overridden** (unlike `name()`, which cannot) |
| `valueOf(String)` | **Static** — converts a matching `String` back into the corresponding enum constant |
| `values()` | **Static** — returns an array of all the enum's constants, in declaration order |
| `compareTo(E other)` | Compares based on `ordinal()` position (implements `Comparable`) |

```java
enum Season { SPRING, SUMMER, AUTUMN, WINTER }
```

```java
for (Season s : Season.values()) {
    System.out.println(s.ordinal() + ": " + s.name());
}
// 0: SPRING
// 1: SUMMER
// 2: AUTUMN
// 3: WINTER
```

```java
Season s = Season.valueOf("SUMMER");
System.out.println(s);                   // SUMMER

// Season bad = Season.valueOf("summer"); // throws IllegalArgumentException — valueOf is case-sensitive
// Season bad2 = Season.valueOf("FALL");  // throws IllegalArgumentException — no such constant exists
```

### 4.1 `name()` vs `toString()` — an important distinction

`name()` **always** returns the exact constant name as declared, and **cannot** be overridden. `toString()` returns the same thing **by default**, but — being a regular method inherited from `Object` via `Enum` — **can** be overridden to provide custom, friendlier display text (section 6 shows this).

```java
enum Status {
    ACTIVE, INACTIVE;

    @Override
    public String toString() {
        return name().toLowerCase();    // custom display form
    }
}
```

```java
System.out.println(Status.ACTIVE);          // active    (toString() overridden)
System.out.println(Status.ACTIVE.name());   // ACTIVE    (name() always returns the exact declared name)
```

---

## 5. Enums with Fields and Constructors

Because enum constants are genuine objects of the enum class, the enum can declare **fields and a constructor**, and each constant can supply its **own** constructor arguments.

```java
enum Planet {
    MERCURY(3.303e+23, 2.4397e6),
    VENUS(4.869e+24, 6.0518e6),
    EARTH(5.976e+24, 6.37814e6);

    private final double mass;    // kilograms
    private final double radius;  // meters

    Planet(double mass, double radius) {    // constructor — called once per constant, automatically
        this.mass = mass;
        this.radius = radius;
    }

    double surfaceGravity() {
        final double G = 6.67300E-11;
        return G * mass / (radius * radius);
    }
}
```

```java
System.out.println(Planet.EARTH.surfaceGravity());   // computed from EARTH's own mass and radius
```

### 5.1 How constant declaration works with constructor arguments

```text
MERCURY(3.303e+23, 2.4397e6),    <- calls Planet(mass, radius) ONCE, creating the MERCURY constant
VENUS(4.869e+24, 6.0518e6),       <- calls Planet(mass, radius) ONCE, creating the VENUS constant
EARTH(5.976e+24, 6.37814e6);      <- calls Planet(mass, radius) ONCE, creating the EARTH constant — note the semicolon
```

Each constant invokes the constructor **exactly once**, with its own arguments, at class-loading time — this happens automatically; you never write `new Planet(...)` yourself (see section 9 on why enum constructors are always `private`).

### 5.2 The semicolon after the last constant is required once anything else follows

```java
enum Planet {
    MERCURY(3.303e+23, 2.4397e6),
    EARTH(5.976e+24, 6.37814e6);     // semicolon REQUIRED here, because fields/methods follow

    private final double mass;
    private final double radius;
    // ...
}
```

```java
enum Day {
    MONDAY, TUESDAY, WEDNESDAY       // no semicolon needed — nothing else follows the constant list
}
```

---

## 6. Enums with Methods

Enums can declare ordinary **instance methods**, usable by every constant, just like any class.

```java
enum Status {
    ACTIVE, INACTIVE, SUSPENDED;

    boolean isUsable() {
        return this == ACTIVE;
    }

    @Override
    public String toString() {
        return name().charAt(0) + name().substring(1).toLowerCase();   // "Active", "Inactive", "Suspended"
    }
}
```

```java
System.out.println(Status.ACTIVE.isUsable());     // true
System.out.println(Status.SUSPENDED.isUsable());   // false
System.out.println(Status.ACTIVE);                  // "Active" — using the overridden toString()
```

### 6.1 Enums can also have `static` methods

```java
enum Status {
    ACTIVE, INACTIVE, SUSPENDED;

    static Status defaultStatus() {
        return ACTIVE;
    }
}
```

```java
Status s = Status.defaultStatus();
```

---

## 7. Constant-Specific Method Bodies

A powerful, somewhat advanced enum feature: **individual constants** can override a method with their **own, unique implementation**, by giving that constant a class body (`{ ... }`) right where it's declared.

```java
enum Operation {
    ADD {
        @Override
        int apply(int a, int b) { return a + b; }
    },
    SUBTRACT {
        @Override
        int apply(int a, int b) { return a - b; }
    },
    MULTIPLY {
        @Override
        int apply(int a, int b) { return a * b; }
    };

    abstract int apply(int a, int b);     // each constant MUST provide its own implementation
}
```

```java
System.out.println(Operation.ADD.apply(3, 4));        // 7
System.out.println(Operation.SUBTRACT.apply(10, 3));   // 7
System.out.println(Operation.MULTIPLY.apply(2, 5));     // 10
```

### 7.1 How this works conceptually

Declaring the method `apply(int, int)` as `abstract` inside the enum forces **every single constant** to supply its own override — exactly like an abstract class forcing every concrete subclass to implement its abstract methods (see the abstraction notes). Each constant with a `{ ... }` body is, under the hood, effectively an **anonymous subclass** of the enum type, specialized for just that one constant.

### 7.2 Why this is useful

It avoids a long, error-prone `switch` statement or `if/else` chain scattered through unrelated code, keeping each constant's specific behavior **co-located** with its declaration — a clean way to achieve something close to polymorphism across a known, fixed set of variants.

```java
// The alternative, without constant-specific bodies, would look like this:
enum Operation { ADD, SUBTRACT, MULTIPLY }

int apply(Operation op, int a, int b) {
    switch (op) {
        case ADD: return a + b;
        case SUBTRACT: return a - b;
        case MULTIPLY: return a * b;
        default: throw new IllegalStateException();
    }
}
```

---

## 8. Enum Implementing an Interface

Since an enum already implicitly extends `Enum<E>`, it **cannot** `extends` anything else — but it **can** `implements` one or more interfaces, exactly like a regular class.

```java
interface Describable {
    String describe();
}

enum Season implements Describable {
    SPRING, SUMMER, AUTUMN, WINTER;

    @Override
    public String describe() {
        return "The season is " + name().toLowerCase();
    }
}
```

```java
Describable d = Season.SUMMER;     // can be referenced through the interface type
System.out.println(d.describe());  // "The season is summer"
```

### 8.1 Combining an interface with constant-specific bodies

This is a particularly powerful combination: a shared interface contract, with each constant free to implement it differently.

```java
interface Taxable {
    double taxRate();
}

enum ProductCategory implements Taxable {
    FOOD {
        @Override public double taxRate() { return 0.0; }
    },
    ELECTRONICS {
        @Override public double taxRate() { return 0.18; }
    },
    LUXURY {
        @Override public double taxRate() { return 0.28; }
    }
}
```

```java
for (ProductCategory category : ProductCategory.values()) {
    System.out.println(category + ": " + category.taxRate());
}
// FOOD: 0.0
// ELECTRONICS: 0.18
// LUXURY: 0.28
```

### 8.2 An enum can implement multiple interfaces

```java
interface Describable { String describe(); }
interface Taxable { double taxRate(); }

enum ProductCategory implements Describable, Taxable {
    FOOD {
        @Override public String describe() { return "Food item"; }
        @Override public double taxRate() { return 0.0; }
    }
    // ...
}
```

This is possible for exactly the same reason a regular class can implement multiple interfaces while extending only one class (see the inheritance and abstraction notes) — the "already extends `Enum`" restriction only ever blocks a second `extends`, never additional `implements`.

---

## 9. Enum Constructors Are Always Private

Every enum constructor is **implicitly `private`**, whether or not you write the keyword — and attempting to write `public` or `protected` on one is a compile error.

```java
enum Status {
    ACTIVE, INACTIVE;

    Status() { }          // implicitly private — writing "public Status()" would be a COMPILE ERROR
}
```

```java
// Status s = new Status();   // COMPILE ERROR, always — enum constructors cannot be called with "new"
```

### 9.1 Why this restriction exists

Enum constants are meant to be a **fixed, closed set** — exactly the constants declared in the enum body, and nothing more. If outside code could call `new Status()`, it could create **additional** `Status` instances beyond the declared ones, completely undermining the "known, finite set of values" guarantee that makes enums useful and type-safe in the first place.

---

## 10. `switch` on Enums

Enums work especially well with `switch` statements (and switch expressions). This directly builds on the switch statement fundamentals — the same `case`/`break`/`default` mechanics apply, with a few enum-specific rules layered on top.

```java
enum Day { MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY }

void printSchedule(Day day) {
    switch (day) {
        case MONDAY:
        case TUESDAY:
        case WEDNESDAY:
        case THURSDAY:
        case FRIDAY:
            System.out.println("Workday");
            break;
        case SATURDAY:
        case SUNDAY:
            System.out.println("Weekend");
            break;
        default:
            System.out.println("Unknown day");
    }
}
```

```java
printSchedule(Day.MONDAY);     // Workday
printSchedule(Day.SUNDAY);     // Weekend
```

### 10.1 Case labels use the bare constant name — NOT `Day.MONDAY`

This is a specific, easy-to-get-wrong rule: inside a `switch` on an enum, the `case` labels must be written as just the **unqualified constant name**, not prefixed with the enum type.

```java
switch (day) {
    case MONDAY:            // CORRECT
    // case Day.MONDAY:     // COMPILE ERROR — do not qualify with the enum type name in a case label
        break;
}
```

The compiler already knows the type being switched on is `Day` (from the `switch (day)` expression itself), so qualifying the case labels would be redundant — and Java simply disallows it rather than silently ignoring it.

### 10.2 Fall-through still applies, exactly as with any other switch

Just like regular `switch` statements, omitting a `break` lets execution **fall through** into the next case — this is the same mechanic used intentionally above to group `MONDAY` through `FRIDAY` under one shared `"Workday"` output.

### 10.3 The modern switch expression form works with enums too

Using the newer switch **expression** syntax (arrow form, no fall-through by default), the same logic becomes more concise and avoids the accidental-fall-through risk entirely:

```java
String result = switch (day) {
    case MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY -> "Workday";
    case SATURDAY, SUNDAY -> "Weekend";
};
```

### 10.4 `switch` on enums can be exhaustive without a `default`

If a switch **expression** covers **every** constant of the enum explicitly, the compiler can recognize the switch as exhaustive and does not require a `default` branch at all (unlike switching on a general type like `int` or `String`, where a `default` is otherwise needed to guarantee a value in every expression form).

```java
enum Status { ACTIVE, INACTIVE }

String label = switch (status) {
    case ACTIVE -> "Active";
    case INACTIVE -> "Inactive";
    // no default needed — every Status constant is explicitly covered
};
```

If a new constant is later added to the enum but this `switch` expression isn't updated, the compiler will immediately flag it as no longer exhaustive — a valuable safety net when enums evolve over time.

### 10.5 `NullPointerException` risk

If the enum reference being switched on is `null`, a traditional `switch` statement (and the switch expression form, by default) throws a `NullPointerException` at runtime — enums don't get special `null`-handling just because they're a known, finite set of values.

```java
Day d = null;
switch (d) {           // throws NullPointerException immediately
    case MONDAY: break;
}
```

---

## 11. Enums in Collections and Specialized Enum Collections

Because enum constants are a known, fixed, ordered set with a stable `ordinal()`, Java provides two specialized, highly efficient collection types built specifically around enums.

```java
import java.util.EnumSet;
import java.util.EnumMap;

enum Day { MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY }
```

```java
EnumSet<Day> weekend = EnumSet.of(Day.SATURDAY, Day.SUNDAY);
System.out.println(weekend.contains(Day.SATURDAY));   // true

EnumMap<Day, String> schedule = new EnumMap<>(Day.class);
schedule.put(Day.MONDAY, "Team meeting");
System.out.println(schedule.get(Day.MONDAY));           // "Team meeting"
```

`EnumSet` and `EnumMap` are internally implemented using the enum's `ordinal()` values (often as compact bit vectors), making them significantly faster and more memory-efficient than a general-purpose `HashSet`/`HashMap` of the same enum type — worth knowing exists, even if the full depth belongs with the Collections Framework material.

---

## 12. Common Pitfalls

| # | Pitfall | Fix |
|---|---------|-----|
| 1 | Using `extends` on an enum | Not allowed — an enum already implicitly extends `Enum<E>`; use `implements` for interfaces instead |
| 2 | Trying to create an enum constant with `new` | Not allowed, ever — enum constructors are implicitly `private`, and constants are created only by the enum declaration itself |
| 3 | Forgetting the semicolon after the last constant when fields/methods follow | Required whenever anything (fields, constructors, methods) comes after the constant list |
| 4 | Qualifying `case` labels with the enum type name (`case Day.MONDAY:`) | Use the bare constant name only (`case MONDAY:`) inside a switch on that enum |
| 5 | Assuming `valueOf(String)` is case-insensitive | It is case-sensitive, and throws `IllegalArgumentException` for any non-matching name |
| 6 | Relying on `ordinal()` for business logic (e.g., storing it in a database) | `ordinal()` depends on declaration order, which can silently change if the enum is edited later — use an explicit field instead for anything persisted or compared externally |
| 7 | Assuming `toString()` and `name()` always return the same thing | `toString()` can be overridden for custom display; `name()` cannot be overridden and always returns the exact declared constant name |
| 8 | Forgetting `default` on a traditional `switch` statement over an enum | Unlike a fully-covered switch *expression*, a switch *statement* still benefits from a `default` for safety unless every case is truly, deliberately covered |
| 9 | Switching on a `null` enum reference | Throws `NullPointerException` — enums get no special null-handling in a switch |
| 10 | Forgetting that every constant needs its own override when a method is declared `abstract` inside the enum | Constant-specific method bodies are mandatory for every single constant once the method is abstract — a partial implementation won't compile |
| 11 | Using `==` out of habit without realizing it's actually correct for enums | Unlike most object types, `==` is safe and idiomatic for enum constants, since each is a true singleton |
| 12 | Forgetting an enum is implicitly `final` and cannot be subclassed by an external class | By design — the same restriction a `final` class carries (see the static/final notes) |

---

## 13. Interview Questions

**Q1. What is an enum, and why use it instead of plain constants?**
An enum is a special class type representing a fixed, known set of named constants. Unlike plain `int` or `String` constants, enums provide compile-time type safety (invalid values simply cannot be constructed or passed), a proper namespace, and readable debugging output.

---

**Q2. Is an enum really a class in Java?**
Yes. Every enum implicitly extends `java.lang.Enum<E>`, and each declared constant is a `public static final` instance of the enum type. This is why enums can have fields, constructors, methods, and implement interfaces, just like ordinary classes.

---

**Q3. Can an enum extend another class? Can it implement an interface?**
It cannot extend another class, since it already implicitly extends `Enum<E>` — Java's single-inheritance rule for classes leaves no room for a second `extends`. It can implement any number of interfaces, exactly like a regular class.

---

**Q4. Why are enum constructors always private?**
To guarantee that the enum's constants form a fixed, closed set — exactly the ones declared in the enum body. If outside code could call `new EnumType()`, it could create additional instances beyond the declared constants, breaking that guarantee entirely.

---

**Q5. What is the difference between `name()` and `toString()` on an enum constant?**
`name()` always returns the exact name as declared in the source code and cannot be overridden. `toString()` returns the same value by default, but — being an ordinary inherited method — can be overridden to produce custom, friendlier display text.

---

**Q6. What is a constant-specific method body in an enum?**
A technique where individual constants provide their own override of a method (usually declared `abstract` in the enum body), each written inline as a class body right after that constant's declaration. This keeps each constant's unique behavior co-located with its definition, avoiding a separate `switch`/`if-else` chain elsewhere in the code.

---

**Q7. How does `switch` on an enum differ from `switch` on an `int` or `String`?**
The `case` labels use the bare constant name, without qualifying it with the enum's type name (`case MONDAY:`, not `case Day.MONDAY:`). Additionally, a switch expression that explicitly covers every constant of the enum can be considered exhaustive by the compiler without needing a `default` branch.

---

**Q8. Why shouldn't `ordinal()` be relied upon for things like persisting data to a database?**
`ordinal()` reflects the constant's position in the declaration order, which can silently shift if constants are reordered, inserted, or removed later in the source code. Any external reference relying on that position (like a stored database value) could then silently start referring to the wrong constant after such an edit.

---

**Q9. Is it safe to use `==` to compare enum constants?**
Yes — and it's actually the idiomatic way to do it. Since each enum constant is a single, unique object (a true singleton) for the entire JVM, `==` and `.equals()` behave identically for enum comparisons, unlike the usual caution needed with `==` on ordinary object types.

---

**Q10. What happens if you `switch` on a `null` enum reference?**
It throws a `NullPointerException` at runtime, exactly as it would for any other reference type used in a switch — enums receive no special handling for `null` despite representing a fixed, known set of non-null values.

---

## 14. Interview-Style Output Questions

**Q1**
```java
enum Season { SPRING, SUMMER, AUTUMN, WINTER }
System.out.println(Season.AUTUMN.ordinal());
System.out.println(Season.AUTUMN.name());
```
**Answer:**
```
2
AUTUMN
```

---

**Q2**
```java
enum Status {
    ACTIVE, INACTIVE;
    @Override
    public String toString() { return name().toLowerCase(); }
}
System.out.println(Status.ACTIVE);
System.out.println(Status.ACTIVE.name());
```
**Answer:**
```
active
ACTIVE
```
(`toString()` is overridden; `name()` always returns the exact declared name regardless.)

---

**Q3**
```java
enum Operation {
    ADD { int apply(int a, int b) { return a + b; } },
    SUBTRACT { int apply(int a, int b) { return a - b; } };
    abstract int apply(int a, int b);
}
System.out.println(Operation.ADD.apply(5, 3));
System.out.println(Operation.SUBTRACT.apply(5, 3));
```
**Answer:**
```
8
2
```

---

**Q4**
```java
enum Day { MONDAY, TUESDAY }
Day d = Day.valueOf("monday");
```
**Answer:** Throws `IllegalArgumentException` at runtime — `valueOf` is case-sensitive, and `"monday"` doesn't match the declared constant `MONDAY`.

---

**Q5**
```java
enum Day { MONDAY, TUESDAY, WEDNESDAY }

String result;
switch (Day.TUESDAY) {
    case MONDAY:
        result = "Start";
        break;
    case TUESDAY:
        result = "Middle";
    case WEDNESDAY:
        result = "End";
        break;
}
System.out.println(result);
```
**Answer:** `End`. `TUESDAY` matches, but has no `break`, so execution falls through into `WEDNESDAY`'s case, overwriting `result` before finally breaking.

---

**Q6**
```java
Day d1 = Day.MONDAY;
Day d2 = Day.MONDAY;
System.out.println(d1 == d2);
```
**Answer:** `true`. Each enum constant is a single, unique singleton instance — both variables reference the exact same object.

---

## 15. Quick Cheat Sheet

```text
ENUM BASICS            enum Name { CONST1, CONST2, ... }
                       Each constant is a PUBLIC STATIC FINAL singleton instance of the enum type

UNDER THE HOOD          Implicitly extends java.lang.Enum<E>  -> cannot "extends" anything else
                       Can "implements" any number of interfaces, like a regular class
                       Implicitly final -> cannot be subclassed

BUILT-IN METHODS        name()        exact declared name, NEVER overridable
                       ordinal()     zero-based declaration position
                       toString()    defaults to name(), CAN be overridden
                       values()      static, returns all constants as an array, in order
                       valueOf(s)    static, String -> constant (case-sensitive, throws if no match)

FIELDS/CONSTRUCTOR      enum Planet {
                           EARTH(5.97e24, 6.37e6);    <- calls the constructor once per constant
                           private final double mass, radius;
                           Planet(double m, double r) { mass = m; radius = r; }   <- ALWAYS implicitly private
                       }
                       Semicolon required after the last constant IF anything else follows

CONSTANT-SPECIFIC       CONST_NAME { @Override method() { ... } }  — each constant overrides an abstract
METHOD BODY             method declared in the enum; EVERY constant must supply one once it's abstract

IMPLEMENTS INTERFACE    enum Name implements SomeInterface { ... }
                       Combine with constant-specific bodies for per-constant interface behavior

SWITCH ON ENUM          case CONST_NAME:     <- bare name only, NEVER "case EnumType.CONST_NAME:"
                       Switch EXPRESSIONS covering every constant need no "default"
                       null reference -> NullPointerException, same as any other switch

ENUM COLLECTIONS        EnumSet<E>, EnumMap<E, V>  — specialized, efficient, ordinal-based collections
```

**Remember:**

1. Enums are real classes — every constant is a singleton instance, which is exactly why `==` is safe and idiomatic for comparing them.
2. Enum constructors are always implicitly private; constants can never be created with `new`, only by the enum's own declaration.
3. An enum can't extend a class (the slot is taken by `Enum<E>`), but can implement any number of interfaces, including with constant-specific method bodies.
4. `switch` case labels on enums use the bare constant name, and a fully-covered switch expression needs no `default`.
5. Don't rely on `ordinal()` for anything persisted externally — it silently shifts if the enum's declaration order ever changes.
