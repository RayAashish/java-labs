# Exception Rules in Method Overriding and the Liskov Substitution Principle
> **Author:** raysi  
> **Package:** `com.raysi.exceptions.phase4_oop_inheritance_and_lambdas`  
> **Module:** Polymorphic Exception Contracts, Liskov Substitution, and Interface Conflicts

---

# 1. Architectural Concept & Compiler vs. JVM Behavior

In Object-Oriented Programming, the **Liskov Substitution Principle (LSP)** dictates that objects of a superclass should be replaceable with objects of its subclasses without altering the correctness of the program.

Java enforces this principle at compile-time through strict **Method Overriding Exception Rules**:

```text
                            POLYMORPHIC CALL SITE
            Parent p = new Child(); // Upcasted reference
            try {
                p.execute(); // Caller ONLY catches IOException!
            } catch (IOException e) {
                // Handled
            }

                        METHOD OVERRIDING PERMISSIONS
  ┌────────────────────────────────────────────────────────────────────────┐
  │ Parent Method Signature: void execute() throws IOException             │
  └───────────────────────────────────┬────────────────────────────────────┘
                                      │
          ┌───────────────────────────┼───────────────────────────┐
          │ ALLOWED                   │ ALLOWED                   │ FORBIDDEN ❌
          ▼                           ▼                           ▼
  ┌───────────────┐           ┌───────────────┐           ┌───────────────┐
  │ Child Method: │           │ Child Method: │           │ Child Method: │
  │ throws        │           │ (throws       │           │ throws        │
  │ FileNotFound- │           │  nothing)     │           │ Exception     │
  │ Exception     │           │               │           │ (Broader!)    │
  └───────────────┘           └───────────────┘           └───────────────┘
  (Narrower subclass          (Substitutable:             (Violates LSP!   
   is substitutable)           caller catches              Caller cannot   
                               safe subset)                catch raw Ex)   
```

### The Overriding Rule Matrix
| Parent Method Declares | Child Method CAN Declare | Child Method CANNOT Declare |
|---|---|---|
| **Checked Exception (`IOException`)** | Same exception (`IOException`), narrower subclass (`FileNotFoundException`), or **NO exception at all** | Broader checked exception (`Exception`, `Throwable`), or new unrelated checked exception (`SQLException`) |
| **No Checked Exception** | **NO checked exception**, or any unchecked exception (`RuntimeException` / `Error`) | Any checked exception (`IOException`, `Exception`) |
| **Unchecked Exception (`NullPointerException`)** | Any `RuntimeException`, any subclass, or no exception at all (Zero restrictions) | Any checked exception (`IOException`) |

### Compile-Time (`javac`) vs. Runtime JVM Behavior
- **Compile-Time (`javac`):** The compiler inspects the polymorphic call site `p.execute()`. Because `p` is statically typed as `Parent`, the compiler only checks that the caller catches or declares `IOException`. If the child runtime class were permitted to throw `SQLException` or `Exception`, the caller's catch block would fail to intercept it, creating an uncaught checked exception at runtime. Thus, `javac` rejects broader checked exceptions with compile error: `overridden method does not throw <ExceptionType>`.
- **Runtime (JVM Execution Engine):** The JVM bytecode instruction `invokevirtual` looks up the vtable (virtual method table) of the concrete runtime instance. HotSpot does not enforce exception overriding rules at runtime; all enforcement is purely performed statically by `javac`.

---

# 2. Under the Hood: Bytecode & JDK Source Code Analysis

### The `Exceptions` Attribute in Class Files
When a method declares a `throws` clause, `javac` generates an `Exceptions` attribute inside the method's `method_info` structure in the class file:
```text
Exceptions_attribute {
    u2 attribute_name_index;   // points to "Exceptions" in Constant Pool
    u4 attribute_length;
    u2 number_of_exceptions;
    u2 exception_index_table[number_of_exceptions]; // Constant Pool class references
}
```
During compilation of a subclass, `javac` reads the `Exceptions` attribute of the parent method in the parent's `.class` file and cross-references each exception declared in the child class:
- For each child exception $E_{child}$, there must exist at least one parent exception $E_{parent}$ such that $E_{child}$ is an instance of $E_{parent}$ ($E_{child} \subseteq E_{parent}$), unless $E_{child}$ is an unchecked exception (`RuntimeException` or `Error`).

### Covariant Returns vs. Contravariant Exceptions
- **Return Types are Covariant:** A child method can return a narrower (more specific) type than the parent (`CharSequence` -> `String`).
- **Declared Exceptions are Covariant / Subset-Restricted:** A child method can declare a narrower (more specific) exception type or a subset of parent exceptions. It can never widen the exception specification.

---

# 3. Tricky Edge Cases & "What Prints?" Interview Puzzles

### Puzzle 1: Multiple Interface Inheritance Exception Collision
```java
public class InterfaceCollisionPuzzle {
    interface ReaderA {
        void read() throws java.io.IOException;
    }
    interface ReaderB {
        void read() throws java.sql.SQLException;
    }

    // What can the implementing class declare?
    static class DataReader implements ReaderA, ReaderB {
        @Override
        public void read() { // MUST declare NO checked exception (intersection)!
            System.out.println("Reading data safely...");
        }
    }
}
```
**What happens?**
- **Rule:** When a class implements two interfaces declaring conflicting checked exceptions for the same method signature, the implementing method can only declare the **intersection** of the two `throws` clauses.
- Since `IOException` and `SQLException` share no common checked subclass (only `Exception`, which is broader), the intersection is **empty**. The implementation can declare **NO checked exceptions at all**!

### Puzzle 2: Parent Method vs. Child Constructor Exception Inversion
```java
public class ConstructorExceptionPuzzle {
    static class SuperClass {
        public SuperClass() throws java.io.IOException {
            System.out.println("Super constructed");
        }
    }

    static class SubClass extends SuperClass {
        // Child constructor CANNOT declare narrower exception than super constructor!
        public SubClass() throws Exception { // Must be EQUAL OR BROADER!
            super();
            System.out.println("Sub constructed");
        }
    }
}
```
**Why does SubClass constructor declare a BROADER exception?**
- **Mind-Bending Rule:** Unlike methods, a child constructor **MUST declare at least the same or a BROADER exception** than the superclass constructor!
- **Reason:** The very first line of a child constructor is implicitly or explicitly `super()`. Because the parent constructor can throw `IOException`, the child constructor cannot catch it (constructors cannot wrap `super()` inside a `try-catch`). Therefore, the child constructor must declare `throws IOException` or broader (`Exception`).

### Puzzle 3: Overriding Method Adding Unchecked Exceptions
```java
public class UncheckedOverridePuzzle {
    static class Parent {
        void run() { System.out.println("Parent"); } // No throws clause
    }
    static class Child extends Parent {
        @Override
        void run() throws NullPointerException, IllegalArgumentException {
            System.out.println("Child");
        }
    }
}
```
**Does this compile?**
- **Output:** Compiles with zero errors.
- **Reason:** `NullPointerException` and `IllegalArgumentException` are subclasses of `RuntimeException` (Unchecked). Overriding methods have absolute freedom to declare any unchecked exceptions without restriction.

---

# 4. Production Best Practices (❌ Anti-Pattern vs. ✅ Clean Architecture)

### 1. Broadening Interface Contracts
```java
// ❌ ANTI-PATTERN: Catch-all throws on interface leaking implementation details
public interface PaymentGateway {
    void processPayment(BigDecimal amount) throws Exception; // Forces all callers to catch raw Exception
}

// ✅ CLEAN ARCHITECTURE: Strongly typed domain exception hierarchy
public interface PaymentGateway {
    void processPayment(BigDecimal amount) throws PaymentProcessingException;
}

public class StripePaymentGateway implements PaymentGateway {
    @Override
    public void processPayment(BigDecimal amount) throws CardDeclinedException { // Specific subtype!
        // Concrete Stripe API logic
    }
}
```

### 2. Leaking Infrastructure Checked Exceptions Across Clean Architecture Boundaries
```java
// ❌ ANTI-PATTERN: Leaking SQL / IO details across clean architecture domain boundaries
public class UserRepositoryImpl implements UserRepository {
    @Override
    public User findById(String id) throws SQLException { // Leaks persistence technology to domain layer!
        return queryDb(id);
    }
}

// ✅ CLEAN ARCHITECTURE: Translate to domain exception at boundary
public class UserRepositoryImpl implements UserRepository {
    @Override
    public User findById(String id) {
        try {
            return queryDb(id);
        } catch (SQLException e) {
            throw new EntityNotFoundException("User not found for ID: " + id, e);
        }
    }
}
```

---

# 5. Runnable Java Verification Code (`author: raysi`)

```java
package com.raysi.exceptions.phase4_oop_inheritance_and_lambdas.codes;

import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * Author: raysi
 * Package: com.raysi.exceptions.phase4_oop_inheritance_and_lambdas.codes
 * Proves method overriding exception rules and Liskov Substitution compliance.
 */
public class OverridingExceptionRulesCovariantDemo {

    // Parent Class
    static class StorageService {
        public void saveFile(String path) throws IOException {
            System.out.println("StorageService: Saving file -> " + path);
            if (path.isEmpty()) throw new IOException("Invalid empty path");
        }

        public void ping() {
            System.out.println("StorageService: Ping alive.");
        }
    }

    // Compliant Child A: Throws narrower checked exception (FileNotFoundException subclass of IOException)
    static class LocalDiskStorageService extends StorageService {
        @Override
        public void saveFile(String path) throws FileNotFoundException {
            System.out.println("LocalDiskStorageService: Checking file exists -> " + path);
            if (path.equals("missing.txt")) {
                throw new FileNotFoundException("Local file missing: " + path);
            }
        }
    }

    // Compliant Child B: Throws NO checked exception at all
    static class InMemoryStorageService extends StorageService {
        @Override
        public void saveFile(String path) {
            System.out.println("InMemoryStorageService: Storing purely in RAM buffer (No I/O can fail)");
        }

        @Override
        public void ping() throws NullPointerException, IllegalStateException {
            // Can freely declare Unchecked Exceptions even if parent declared none!
            System.out.println("InMemoryStorageService: Ping with runtime safety checks.");
        }
    }

    public static void main(String[] args) {
        System.out.println("=== 1. Polymorphic Invocation with Narrower Checked Exception ===");
        StorageService localService = new LocalDiskStorageService();
        try {
            localService.saveFile("missing.txt");
        } catch (IOException e) { // Catches via parent contract!
            System.out.println("Caught via parent contract IOException: " + e.getClass().getSimpleName() + " - " + e.getMessage());
        }

        System.out.println("\n=== 2. Polymorphic Invocation with Method Declaring No Exceptions ===");
        StorageService memService = new InMemoryStorageService();
        try {
            memService.saveFile("data.bin");
            memService.ping();
        } catch (IOException e) {
            System.out.println("Will not be thrown: " + e.getMessage());
        }

        System.out.println("\n[SUCCESS] Method Overriding Exception Rules Verified.");
    }
}
```

---

# 6. Top 7 High-Signal Interview Questions & Crisp Answers

### Q1: Why can an overridden method NOT throw a broader checked exception than the parent method?
**Answer:** Because it violates the **Liskov Substitution Principle (LSP)** and breaks static type safety. A caller interacting with the parent reference `Parent p = new Child();` writes catch blocks based on `Parent`'s signature. If `Child` could throw a broader checked exception, the caller's catch blocks would fail to intercept it, crashing the application with an unhandled exception.

### Q2: Can an overridden method throw fewer checked exceptions than the parent method?
**Answer:** Yes. An overridden method can declare a subset of the parent's exceptions, or even declare **NO exceptions at all**. This is fully substitutable because callers catching the parent's exceptions will remain safe.

### Q3: What happens when an overriding method declares new Unchecked Exceptions?
**Answer:** There are no restrictions. A child method can declare any number of unchecked exceptions (`RuntimeException` or `Error`) even if the parent method declares none, because unchecked exceptions are not enforced by `javac`'s catch-or-declare rule.

### Q4: If two interfaces declare a method with identical signatures but different checked exceptions, what must the implementing class do?
**Answer:** The implementing class must declare the **intersection** of the exceptions declared by both interfaces. If the exception types share no common checked subtype, the implementing method must declare **no checked exceptions at all**.

### Q5: How do constructor exception rules differ from method overriding rules?
**Answer:** While child methods can only declare equal or narrower exceptions, a child **constructor must declare equal or BROADER exceptions** than the superclass constructor. Because `super(...)` must execute first in a child constructor and cannot be surrounded by a `try-catch`, any checked exception thrown by the super constructor must be declared in the child constructor's `throws` clause.

### Q6: Can a child method declare a broader checked exception if it is also declared as a `RuntimeException`?
**Answer:** No. An exception class is either checked or unchecked based on its place in the class hierarchy (`RuntimeException` and `Error` are unchecked; all other `Throwable` subclasses are checked). Declaring a broader checked exception is always a compile-time error.

### Q7: If a parent method throws `FileNotFoundException`, can the child method throw `IOException`?
**Answer:** No. `IOException` is the superclass of `FileNotFoundException`. That would be widening the exception contract, which is strictly prohibited by `javac` and causes compile error: `overridden method does not throw java.io.IOException`.
