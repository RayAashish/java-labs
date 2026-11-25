# Custom Exceptions, Exception Chaining, and Root-Cause Preservation
> **Author:** raysi  
> **Package:** `com.raysi.exceptions.phase5_production_custom_exceptions`  
> **Module:** Domain Exception Modeling, Causality Chains, and Forensic Traceability

---

# 1. Architectural Concept & Compiler vs. JVM Behavior

In modern enterprise and microservice architectures, technical exceptions from low-level infrastructure (such as `java.sql.SQLException`, `java.net.SocketTimeoutException`, or AWS SDK errors) must not leak into business services or out to API clients.

Instead, architectures apply the **Exception Translation Pattern**: low-level exceptions are intercepted and re-packaged into semantic **Domain Exceptions** (e.g., `OrderNotFoundException`, `PaymentAuthorizationException`).

```text
               EXCEPTION TRANSLATION & ROOT CAUSE CHAINING
   ┌────────────────────────────────────────────────────────────────────────┐
   │ PERSISTENCE LAYER                                                      │
   │ catch (SQLException sqlEx) // "Connection refused: port 5432"          │
   └───────────────────────────────────┬────────────────────────────────────┘
                                       │ throws new RepositoryAccessException(..., sqlEx)
                                       ▼
   ┌────────────────────────────────────────────────────────────────────────┐
   │ SERVICE / DOMAIN LAYER                                                 │
   │ catch (RepositoryAccessException repoEx)                               │
   └───────────────────────────────────┬────────────────────────────────────┘
                                       │ throws new OrderProcessingException(..., repoEx)
                                       ▼
   ┌────────────────────────────────────────────────────────────────────────┐
   │ REST CONTROLLER / GLOBAL ADVICE                                        │
   │ catch (OrderProcessingException orderEx)                               │
   │                                                                        │
   │ CAUSALITY GRAPH:                                                       │
   │ OrderProcessingException                                               │
   │   └── Cause: RepositoryAccessException                                 │
   │                └── Root Cause: SQLException ("Connection refused")     │
   └────────────────────────────────────────────────────────────────────────┘
```

### The Architectural Choice: Checked vs. Unchecked Custom Exceptions
In modern backend engineering (Spring Boot, Quarkus, Micronaut), **custom domain exceptions almost universally extend `java.lang.RuntimeException`**:
1. **Separation of Concerns:** Business logic is not polluted with boilerplate `throws` declarations on every service interface.
2. **Spring Transaction Management:** In Spring `@Transactional`, transactions roll back automatically **ONLY for Unchecked Exceptions** (`RuntimeException` and `Error`). If a method throws a custom *Checked* exception, Spring commits the transaction by default unless explicitly configured via `@Transactional(rollbackFor = Exception.class)`.
3. **Clean Global Exception Handling:** Declarative controllers (`@RestControllerAdvice`) catch domain runtime exceptions seamlessly at the web boundary.

---

# 2. Under the Hood: Bytecode & JDK Source Code Analysis

### Anatomy of `Throwable.cause` and `initCause()`
Looking at OpenJDK's `java.lang.Throwable`:
```java
public class Throwable implements Serializable {
    private Throwable cause = this; // Default self-reference indicates uninitialized cause

    public synchronized Throwable initCause(Throwable cause) {
        if (this.cause != this)
            throw new IllegalStateException("Can't overwrite cause with " +
                                            Objects.toString(cause, "a null"), this);
        if (cause == this)
            throw new IllegalArgumentException("Self-causation not permitted", this);
        this.cause = cause;
        return this;
    }

    public synchronized Throwable getCause() {
        return (cause == this ? null : cause);
    }
}
```

### The Four Standard Constructors
Every production custom exception should implement the canonical 4-constructor pattern from `Throwable`:
```java
public class BaseDomainException extends RuntimeException {
    public BaseDomainException() { super(); }
    public BaseDomainException(String message) { super(message); }
    public BaseDomainException(String message, Throwable cause) { super(message, cause); }
    public BaseDomainException(Throwable cause) { super(cause); }
}
```

---

# 3. Tricky Edge Cases & "What Prints?" Interview Puzzles

### Puzzle 1: Overwriting Cause via `initCause()`
```java
public class InitCausePuzzle {
    public static void main(String[] args) {
        try {
            Exception e = new Exception("Custom", new IOException("Cause 1"));
            e.initCause(new SQLException("Cause 2")); // ❌ IllegalStateException!
        } catch (Exception ex) {
            System.out.println("Caught: " + ex.getClass().getSimpleName());
        }
    }
}
```
**What happens?**
- **Output:** `Caught: IllegalStateException`
- **Reason:** The constructor `new Exception(msg, cause)` already initialized `this.cause`. Calling `initCause()` when `this.cause != this` violates the immutability contract of the causality chain and throws `IllegalStateException: Can't overwrite cause`.

### Puzzle 2: Circular Causation Loop Detection
```java
public class CircularCausePuzzle {
    public static void main(String[] args) {
        try {
            Exception a = new Exception("Ex A");
            Exception b = new Exception("Ex B");
            a.initCause(b);
            b.initCause(a); // ❌ Throws IllegalArgumentException!
        } catch (Exception ex) {
            System.out.println("Loop detected: " + ex.getClass().getSimpleName());
        }
    }
}
```
**What happens?**
- **Output:** `Loop detected: IllegalArgumentException`
- **Reason:** `Throwable.initCause()` iterates up the chain to ensure the target does not reference an ancestor. If a cycle is detected, it throws `IllegalArgumentException: Self-causation not permitted`.

### Puzzle 3: Extracting the True Root Cause
```java
public class RootCauseResolver {
    public static Throwable getRootCause(Throwable throwable) {
        Objects.requireNonNull(throwable);
        Throwable root = throwable;
        while (root.getCause() != null && root.getCause() != root) {
            root = root.getCause();
        }
        return root;
    }
}
```
- In deep distributed microservices, exceptions can be wrapped 4 or 5 layers deep (`NestedServletException` -> `TransactionSystemException` -> `RollbackException` -> `PersistenceException` -> `PSQLException`). Traversing to the root cause is essential for identifying database constraint names and error codes.

---

# 4. Production Best Practices (❌ Anti-Pattern vs. ✅ Clean Architecture)

### 1. The Message-Only Re-Throw (Stack Trace Obliteration)
```java
// ❌ ANTI-PATTERN: Original stack trace and SQL state code are permanently destroyed!
try {
    userDao.save(user);
} catch (SQLException e) {
    throw new UserRegistrationException(e.getMessage()); // Lost e!
}

// ✅ CLEAN ARCHITECTURE: Full exception object passed to constructor
try {
    userDao.save(user);
} catch (SQLException e) {
    throw new UserRegistrationException("Failed to persist user profile for " + user.getEmail(), e);
}
```

### 2. Generic Custom Exception with Raw Strings
```java
// ❌ ANTI-PATTERN: Monolithic exception with unparseable freeform string
throw new AppException("ERROR_CODE_404: User not found with ID 42");

// ✅ CLEAN ARCHITECTURE: Strongly typed Domain Exception with machine-readable metadata
public class EntityNotFoundException extends BaseDomainException {
    private final String entityType;
    private final Object entityId;

    public EntityNotFoundException(String entityType, Object entityId) {
        super(String.format("%s with ID '%s' was not found", entityType, entityId),
              ErrorCode.ENTITY_NOT_FOUND);
        this.entityType = entityType;
        this.entityId = entityId;
    }
}
```

---

# 5. Runnable Java Verification Code (`author: raysi`)

```java
package com.raysi.exceptions.phase5_production_custom_exceptions.codes;

import java.sql.SQLException;
import java.time.Instant;

/**
 * Author: raysi
 * Package: com.raysi.exceptions.phase5_production_custom_exceptions.codes
 * Proves custom exception design, error code taxonomies, and multi-level root-cause chaining.
 */
public class DomainExceptionChainingAndErrorCodeDemo {

    public enum ErrorCode {
        ORDER_PAYMENT_FAILED("PAY-4001", "Payment processing failed via downstream gateway"),
        DATABASE_UNAVAILABLE("INF-5001", "Database storage cluster unreachable");

        private final String code;
        private final String defaultMessage;

        ErrorCode(String code, String defaultMessage) {
            this.code = code;
            this.defaultMessage = defaultMessage;
        }

        public String getCode() { return code; }
        public String getDefaultMessage() { return defaultMessage; }
    }

    public static class BaseDomainException extends RuntimeException {
        private final ErrorCode errorCode;
        private final Instant timestamp;

        public BaseDomainException(ErrorCode errorCode, String message, Throwable cause) {
            super(message, cause);
            this.errorCode = errorCode;
            this.timestamp = Instant.now();
        }

        public ErrorCode getErrorCode() { return errorCode; }
        public Instant getTimestamp() { return timestamp; }
    }

    public static class PaymentProcessingException extends BaseDomainException {
        private final String transactionId;

        public PaymentProcessingException(String transactionId, String message, Throwable cause) {
            super(ErrorCode.ORDER_PAYMENT_FAILED, message, cause);
            this.transactionId = transactionId;
        }

        public String getTransactionId() { return transactionId; }
    }

    public static void main(String[] args) {
        System.out.println("=== Simulating Multi-Tier Enterprise Exception Translation ===");

        try {
            executeOrderCheckout("TXN-987654");
        } catch (PaymentProcessingException e) {
            System.out.println("\n[Top-Level API Handler Intercepted]");
            System.out.println("  Error Code: " + e.getErrorCode().getCode());
            System.out.println("  Transaction: " + e.getTransactionId());
            System.out.println("  Message: " + e.getMessage());
            System.out.println("  Timestamp: " + e.getTimestamp());

            System.out.println("\n--- Traversing Causality Graph to Root Cause ---");
            Throwable current = e;
            int level = 0;
            while (current != null) {
                System.out.printf("  Level %d: [%s] -> %s%n",
                        level++,
                        current.getClass().getSimpleName(),
                        current.getMessage());
                current = current.getCause();
            }
        }
    }

    private static void executeOrderCheckout(String txnId) {
        try {
            invokeDatabaseCluster();
        } catch (SQLException dbEx) {
            // Preserving original SQLException as the cause
            throw new PaymentProcessingException(txnId, "Checkout aborted due to persistence layer failure", dbEx);
        }
    }

    private static void invokeDatabaseCluster() throws SQLException {
        throw new SQLException("SQLState[08001]: Unable to establish connection to PostgreSQL primary at 10.0.1.20:5432");
    }
}
```

---

# 6. Top 7 High-Signal Interview Questions & Crisp Answers

### Q1: Why do modern frameworks like Spring favor Unchecked Exceptions over Checked Exceptions?
**Answer:**
1. Checked exceptions pollute domain models and service interfaces with infrastructure-specific implementation details.
2. Spring's declarative transaction manager (`@Transactional`) automatically rolls back transactions **only** on unchecked exceptions (`RuntimeException` and `Error`) by default.
3. Functional programming constructs (Streams, Lambdas, `CompletableFuture`) cannot cleanly accommodate methods throwing checked exceptions without unwieldy wrappers.

### Q2: What is the risk of calling `new CustomException(e.getMessage())` instead of `new CustomException(msg, e)`?
**Answer:** The original exception object `e` and its full stack trace (source line numbers, method frames, and native trace) are permanently discarded. The log will only display where the `CustomException` was created, making post-mortem debugging in production impossible.

### Q3: What happens if you call `initCause()` on an exception that was created with `new Exception(cause)`?
**Answer:** It throws a `java.lang.IllegalStateException: Can't overwrite cause`. The cause field can only be initialized once.

### Q4: How does Spring Boot's `@ExceptionHandler` or `@RestControllerAdvice` utilize the exception hierarchy?
**Answer:** Spring inspects the thrown exception and searches the registered `@ExceptionHandler` methods for the closest matching type in the class hierarchy. More specific handlers (e.g., `EntityNotFoundException`) take precedence over generic handlers (e.g., `Exception.class`).

### Q5: How do you prevent circular references when serializing exception causality chains to JSON?
**Answer:** By implementing a custom serializer or using DTOs (e.g., RFC 7807 Problem Details). Never serialize raw `Throwable` objects directly to Jackson JSON, as cyclic references between cause and suppressed exceptions can trigger a `StackOverflowError` or leak internal server architecture.

### Q6: Can a custom exception class override `fillInStackTrace()` to improve throughput?
**Answer:** Yes. If a custom exception is used strictly for signaling predictable business states where the call stack is irrelevant, overriding `fillInStackTrace()` to return `this` eliminates native JNI frame traversal overhead, making exception instantiation virtually as fast as standard POJO instantiation.

### Q7: What is the RFC 7807 specification for REST API error responses?
**Answer:** RFC 7807 ("Problem Details for HTTP APIs") defines a standardized JSON schema for HTTP API errors: `type` (URI reference), `title` (human-readable summary), `status` (HTTP status code), `detail` (human-readable explanation of this specific occurrence), and `instance` (URI reference to the specific failure instance).
