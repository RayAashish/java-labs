# Java Variables: Comprehensive Notes

A **variable** in Java is a reserved memory location to store values during program execution. It represents a named storage whose content can be modified during program runtime.

---

## 1. Anatomy of a Variable

```java
// Declaration
int age;

// Initialization
age = 24;

// Declaration + Initialization in a single statement
String fullName = "Aashish Kumar Ray";
```

- **Data Type**: Specifies the kind and size of data stored.
- **Variable Name (Identifier)**: The symbolic reference to the memory block.
- **Value**: The literal or expression result assigned to the variable.

---

## 2. Naming Rules & Conventions

### Legal Rules (Compiler-enforced)
1. Must begin with a letter (`a-z`, `A-Z`), currency symbol (`$`), or underscore (`_`).
2. Subsequent characters may include digits (`0-9`), letters, `$`, and `_`.
3. Cannot use reserved keywords (e.g., `class`, `int`, `return`, `static`).
4. Java is **case-sensitive** (`count`, `Count`, and `COUNT` are three distinct variables).
5. No spaces or other special characters (`@`, `#`, `%`, `!`, `-`) allowed.

### Best Practices & Conventions
- **Local variables & instance fields**: Lower camelCase (e.g., `userAge`, `totalAccountBalance`).
- **Constants (`static final`)**: UPPER_SNAKE_CASE (e.g., `MAX_RETRY_COUNT`, `DEFAULT_TIMEOUT_MS`).
- **Meaningful names**: Favor descriptive names (`itemsCount` over `n`, `isCompleted` over `flag`).

---

## 3. Types of Variables

Java variables fall into three categories based on scope, lifetime, and storage location:

```
                          Variables in Java
                                 |
         +-----------------------+-----------------------+
         |                       |                       |
   Local Variables       Instance Variables       Static (Class) Variables
   (Method / Block)      (Object State)           (Class-level State)
```

| Feature | Local Variables | Instance Variables | Static (Class) Variables |
| :--- | :--- | :--- | :--- |
| **Declaration Scope** | Inside a method, constructor, or block `{ ... }` | Inside a class, outside any method (without `static`) | Inside a class, outside any method (with `static`) |
| **Memory Allocation** | Call Stack (frame) | Heap (as part of the object) | Metaspace / Heap (Class metadata area) |
| **Lifetime** | Created when block is entered; destroyed upon block exit | Created when object is instantiated (`new`); destroyed by Garbage Collector | Created when class is loaded; persists until class is unloaded |
| **Default Values** | **NO DEFAULT VALUES** (must initialize before use; compiler error otherwise) | Initialized with default values (`0`, `0.0`, `false`, `null`) | Initialized with default values (`0`, `0.0`, `false`, `null`) |
| **Access Modifiers** | Cannot use access modifiers (`public`, `private`, etc.); only `final` is valid | Can use access modifiers (`private`, `protected`, `public`, default) | Can use access modifiers (`private`, `protected`, `public`, default) |

---

## 4. Variable Shadowing & The `this` Keyword

When a local variable shares the same name as an instance variable, the local variable **shadows** the instance variable inside that block:

```java
public class User {
    private String name; // Instance variable

    public User(String name) { // Parameter shadows instance variable
        this.name = name;      // 'this.name' refers to the instance variable
    }
}
```

---

## 5. Local Variable Type Inference (`var`)

Introduced in Java 10, the `var` reserved type name allows the compiler to infer the type from the initializer expression:

```java
var count = 10;                     // Inferred as int
var message = "Hello";              // Inferred as String
var list = new ArrayList<String>(); // Inferred as ArrayList<String>
```

> [!WARNING]
> `var` can **only** be used for local variables with initializers. It cannot be used for method parameters, return types, or class fields.
