# Production Anti-Patterns and REST API Error Design (RFC 7807)
> **Author:** raysi  
> **Package:** `com.raysi.exceptions.phase5_production_custom_exceptions`  
> **Module:** Enterprise Error Design, Security Hardening, and Production Anti-Patterns

---

# 1. Architectural Concept & Compiler vs. JVM Behavior

In production backend engineering, an unhandled exception that propagates directly to an HTTP client presents severe availability, stability, and security risks. Leaking raw stack traces or SQL snippets reveals internal database schema, package structures, and library versions to attackers.

Modern production architectures decouple internal forensic logging from external client error representations via **RFC 7807 (Problem Details for HTTP APIs)** and centralized exception interceptors.

```text
                  GLOBAL REST ERROR HANDLING ARCHITECTURE
                                    HTTP GET /api/v1/orders/42
                                                │
                                                ▼
  ┌────────────────────────────────────────────────────────────────────────┐
  │ DispatcherServlet -> OrderController.getOrder("42")                   │
  └─────────────────────────────────────┬──────────────────────────────────┘
                                        │ invokes
                                        ▼
  ┌────────────────────────────────────────────────────────────────────────┐
  │ OrderService: throws new OrderNotFoundException("Order 42 not found")  │
  └─────────────────────────────────────┬──────────────────────────────────┘
                                        │ Exception propagates up dispatcher
                                        ▼
  ┌────────────────────────────────────────────────────────────────────────┐
  │ @RestControllerAdvice (Central Error Boundary)                         │
  │                                                                        │
  │ 1. Intercepts OrderNotFoundException.class                             │
  │ 2. Emits structured forensic log:                                      │
  │    log.error("[TRACE_ID: 8fa9c] Order not found for id 42", ex)        │
  │ 3. Assembles Sanitized RFC 7807 JSON Payload:                          │
  │    {                                                                   │
  │      "type": "https://api.raysi.com/errors/not-found",                 │
  │      "title": "Resource Not Found",                                    │
  │      "status": 404,                                                    │
  │      "detail": "Order '42' does not exist in our system",              │
  │      "instance": "/api/v1/orders/42",                                  │
  │      "timestamp": "2026-10-18T02:46:17Z",                              │
  │      "traceId": "8fa9c7b2e1a4"                                         │
  │    }                                                                   │
  └─────────────────────────────────────┬──────────────────────────────────┘
                                        │ HTTP 404 Not Found (Sanitized JSON)
                                        ▼
                                   Client App
```

### Compile-Time (`javac`) vs. Runtime JVM Behavior
- The compiler enforces syntax and type safety, but cannot detect logical anti-patterns such as empty catch blocks, log-and-throw duplications, or clearing thread interrupt flags.
- At runtime, Spring MVC's `HandlerExceptionResolverComposite` intercepts any unhandled exception emerging from the controller execution chain, passes it to the registered `@RestControllerAdvice` bean, and serializes the resulting RFC 7807 DTO to the client response stream.

---

# 2. Under the Hood: Bytecode & JDK Source Code Analysis

### The `InterruptedException` Flag Clearing Trap
When a thread is waiting, sleeping, or blocked and another thread calls `thread.interrupt()`, the JVM throws an `InterruptedException`.
Crucially, **the HotSpot JVM clears the thread's interrupt status flag before throwing the exception**:
```java
// OpenJDK Thread.java
public static boolean interrupted() {
    return currentThread().isInterrupted(true); // 'true' resets the status to false!
}
```
If a developer writes:
```java
try {
    Thread.sleep(1000);
} catch (InterruptedException e) {
    // ❌ ANTI-PATTERN: Doing nothing or just logging
    log.warn("Interrupted");
}
```
The thread's interrupted status is now **FALSE**. Any parent task runner, thread pool executor, or cooperative shutdown hook inspecting `Thread.currentThread().isInterrupted()` will never realize that a shutdown was requested, resulting in **zombie threads and hanging JVM shutdowns**!

**The Golden Fix:**
```java
try {
    Thread.sleep(1000);
} catch (InterruptedException e) {
    Thread.currentThread().interrupt(); // Restore interrupted status!
    log.warn("Thread interrupted; status restored for upstream cooperative shutdown");
}
```

---

# 3. Tricky Edge Cases & "What Prints?" Interview Puzzles

### Puzzle 1: The "Log and Throw" Multiplier Anti-Pattern
```java
public void level3() {
    try {
        dao.query();
    } catch (Exception e) {
        log.error("Error in level 3", e); // Log 1
        throw e;
    }
}
public void level2() {
    try {
        level3();
    } catch (Exception e) {
        log.error("Error in level 2", e); // Log 2
        throw e;
    }
}
```
**What happens in production?**
- For a single database outage, the identical 100-line stack trace is logged at level 3, level 2, level 1, and the global controller advice (4 times!).
- A single failure in a system handling 1,000 requests/sec generates 4,000 massive stack traces per second, exhausting disk I/O, clogging Elasticsearch / Datadog pipelines, and costing thousands in logging fees.
- **Rule:** **Either log the exception OR re-throw it. NEVER do both!**

### Puzzle 2: Catching `Exception` Swallowing `InterruptedException`
```java
public class ThreadHungPuzzle {
    public static void main(String[] args) throws InterruptedException {
        Thread worker = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    Thread.sleep(5000);
                } catch (Exception e) { // Catches InterruptedException and does not restore flag!
                    System.out.println("Swallowed interrupt!");
                }
            }
            System.out.println("Worker terminated gracefully.");
        });
        worker.start();
        Thread.sleep(500);
        worker.interrupt(); // Main requests cancellation
        Thread.sleep(1000);
        System.out.println("Worker state: " + worker.getState() + " (Still running!)");
    }
}
```
**What happens?**
- The worker thread will **NEVER terminate**! Because `catch (Exception e)` swallowed `InterruptedException` without restoring the flag, `Thread.currentThread().isInterrupted()` returns `false`, looping indefinitely.

### Puzzle 3: Modifying Response Headers After Stream Flush
If a controller writes partial bytes to `HttpServletResponse.getOutputStream()` and then throws an exception:
- The HTTP headers and status code (`200 OK`) have **already been committed** to the network socket!
- The global `@RestControllerAdvice` cannot change the status to `500 Internal Server Error`. The client receives a truncated body with a 200 OK status.

---

# 4. Production Best Practices (❌ Anti-Pattern vs. ✅ Clean Architecture)

### 7 Classic Production Anti-Patterns and Clean Fixes

| # | ❌ Anti-Pattern | ✅ Clean Architecture |
|---|---|---|
| **1** | Swallowing exceptions (`catch (Exception e) {}`) | Log with context or handle with fallback. |
| **2** | "Log and Throw" (Logging at every stack layer) | Log at the boundary where the exception is finally resolved. |
| **3** | Catching `Throwable` or `Error` | Catch specific application exceptions; let JVM errors propagate. |
| **4** | Losing root cause (`new CustomException(e.getMessage())`) | Chain cause: `new CustomException(msg, e)`. |
| **5** | Clearing interrupt flag on `InterruptedException` | Always invoke `Thread.currentThread().interrupt()`. |
| **6** | Returning `null` on error instead of throwing or `Optional` | Return `Optional.empty()` for missing data or throw domain exception. |
| **7** | Using exceptions for predictable business validation flow | Use deterministic validation (`if-else` or validator libraries). |

---

# 5. Runnable Java Verification Code (`author: raysi`)

```java
package com.raysi.exceptions.phase5_production_custom_exceptions.codes;

import java.net.URI;
import java.time.Instant;
import java.util.Map;

/**
 * Author: raysi
 * Package: com.raysi.exceptions.phase5_production_custom_exceptions.codes
 * Proves RFC 7807 Problem Detail response generation and InterruptedException restoration.
 */
public class StacklessFastControlFlowExceptionDemo {

    // RFC 7807 Standard Error DTO
    public record ProblemDetail(
            URI type,
            String title,
            int status,
            String detail,
            URI instance,
            Instant timestamp,
            Map<String, Object> extensions
    ) {
        public static ProblemDetail of(int status, String title, String detail, String instancePath) {
            return new ProblemDetail(
                    URI.create("https://api.raysi.com/errors/" + title.toLowerCase().replace(' ', '-')),
                    title,
                    status,
                    detail,
                    URI.create(instancePath),
                    Instant.now(),
                    Map.of("traceId", "TRC-" + Long.toHexString(System.currentTimeMillis()))
            );
        }
    }

    // High-performance Stackless Exception for predictable business branching
    public static class EntityNotFoundSignalException extends RuntimeException {
        public EntityNotFoundSignalException(String message) {
            super(message);
        }

        @Override
        public synchronized Throwable fillInStackTrace() {
            // Overridden to eliminate native stack walking overhead
            return this;
        }
    }

    public static void main(String[] args) {
        System.out.println("===============================================================");
        System.out.println("1. RFC 7807 PROBLEM DETAILS ERROR SERIALIZATION");
        System.out.println("===============================================================");

        ProblemDetail problem = ProblemDetail.of(
                404,
                "Order Not Found",
                "Order with ID 'ORD-9902' does not exist in the fulfillment warehouse",
                "/api/v1/orders/ORD-9902"
        );

        System.out.println("RFC 7807 Serialized Error Payload:");
        System.out.println("  Type:      " + problem.type());
        System.out.println("  Title:     " + problem.title());
        System.out.println("  Status:    " + problem.status());
        System.out.println("  Detail:    " + problem.detail());
        System.out.println("  Instance:  " + problem.instance());
        System.out.println("  Timestamp: " + problem.timestamp());
        System.out.println("  Trace ID:  " + problem.extensions().get("traceId"));

        System.out.println("\n===============================================================");
        System.out.println("2. PROVING INTERRUPTED STATUS RESTORATION");
        System.out.println("===============================================================");

        // Simulate cooperative cancellation
        Thread worker = new Thread(() -> {
            try {
                System.out.println("  [Worker] Going to sleep...");
                Thread.sleep(10_000);
            } catch (InterruptedException e) {
                System.out.println("  [Worker] Caught InterruptedException! Status was reset to: " + Thread.currentThread().isInterrupted());
                // Crucial step: restore flag
                Thread.currentThread().interrupt();
                System.out.println("  [Worker] Restored interrupt flag! Status is now: " + Thread.currentThread().isInterrupted());
            }
        });

        worker.start();
        try { Thread.sleep(200); } catch (InterruptedException ignored) {}
        worker.interrupt();
        try { worker.join(); } catch (InterruptedException ignored) {}

        System.out.println("\n[SUCCESS] Production Error Architecture and Interruption verified.");
    }
}
```

---

# 6. Top 7 High-Signal Interview Questions & Crisp Answers

### Q1: What is the single biggest design flaw with the "Log and Throw" pattern?
**Answer:** Log pollution and cascade inflation. When an exception is logged at every layer of the call stack (DAO, Service, Controller), the same failure is recorded multiple times in server logs. In high-concurrency production systems, this fills up disk buffers, degrades I/O throughput, and increases the cost of log ingestion tools.

### Q2: Why is catching `InterruptedException` without restoring the flag dangerous?
**Answer:** The JVM clears the thread's interrupt flag when it throws `InterruptedException`. If the catch block does not call `Thread.currentThread().interrupt()` or propagate the exception, the thread's interrupted state is lost. Higher-level thread pool executors or loops checking `!Thread.currentThread().isInterrupted()` will fail to shut down, causing zombie threads and hanging container graceful terminations.

### Q3: What is RFC 7807 / RFC 9457 ("Problem Details for HTTP APIs")?
**Answer:** It is an IETF standard defining a uniform machine-readable JSON structure for HTTP API error responses. It includes standard fields: `type` (URI identifier), `title` (summary), `status` (HTTP code), `detail` (specific explanation), and `instance` (request URI), eliminating arbitrary and inconsistent custom JSON error formats across microservices.

### Q4: How does Spring Boot 3 support RFC 7807 natively?
**Answer:** Spring Boot 3 / Spring Framework 6 includes the built-in `org.springframework.http.ProblemDetail` class and the `@RestControllerAdvice` base class `ResponseEntityExceptionHandler`. Setting `spring.mvc.problemdetails.enabled=true` automatically renders all standard Spring MVC exceptions into RFC 7807 JSON responses.

### Q5: Why should database constraint exceptions never be exposed directly to API callers?
**Answer:** Revealing raw database error messages exposes internal table names, foreign key relationships, database vendor versions, and sensitive column names. This provides attackers with structural intelligence for SQL injection attacks and schema mapping.

### Q6: When is it acceptable to override `fillInStackTrace()` in a custom exception?
**Answer:** When an exception is used exclusively for internal signaling or high-frequency control flow where the Java stack trace is never logged or inspected by developers. Disabling stack trace generation avoids native JNI frame traversal, achieving near-zero allocation and CPU overhead.

### Q7: What is the difference between `Thread.interrupted()` and `Thread.currentThread().isInterrupted()`?
**Answer:** 
- `Thread.interrupted()` is a **static** method that checks the current thread's interrupt status and **clears** it (resets it to `false`).
- `Thread.currentThread().isInterrupted()` is an **instance** method that checks the thread's interrupt status **without modifying** the flag.
