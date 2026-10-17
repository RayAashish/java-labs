# 02: Java 11 LTS — Modularity, Standard HttpClient & Enterprise Stability
**Author:** `raysi`  
**Package:** `com.raysi.lts.java11`  
**Target LTS:** Java 11 (Released September 2018 — The First Modern LTS on the 6-Month Cadence)

---

# 1. Architectural Impact & Why This LTS Version Matters

### Pain Points in Previous Versions (Java 8 Baseline)
Java 11 marked the transition from the legacy multi-year release cycle to the modern 6-month rapid cadence with 3-year LTS milestones. Upgrading from Java 8 to Java 11 addressed long-standing architectural deficiencies:

1. **Monolithic JDK Bloat & Monolithic Runtime:**
   Java 8 shipped with an unprunable runtime (`rt.jar` exceeded 60MB), including legacy CORBA, applets, and enterprise XML modules that backend microservices never needed. Java 11 stabilized **Project Jigsaw** (modular runtime), allowing minimal container images via `jlink`.
2. **Reliance on Third-Party HTTP Clients:**
   Java 8's native `HttpURLConnection` was synchronous, blocking, hard to use, and only supported HTTP/1.1. Backend systems had to pull in heavy third-party dependencies like Apache HttpClient or Square OkHttp just to make standard REST calls.
3. **Verbose Type Ceremony in Local Variables:**
   Even simple local declarations required redundant type declarations: `Map<String, List<Transaction>> map = new HashMap<String, List<Transaction>>()`.
4. **Removal of Legacy Java EE and CORBA Modules (JEP 320):**
   Java 11 removed deprecated enterprise modules (`javax.xml.bind`, `javax.ws.rs`, `javax.activation`), forcing enterprise architectures to explicitly declare modern standalone dependencies.
5. **Open Sourcing Enterprise JVM Tooling:**
   Java 11 open-sourced **Java Flight Recorder (JFR)** and **Java Mission Control (JMC)** into OpenJDK (JEP 328), providing production zero-overhead diagnostic profiling.

---

### Summary of Landmark Features Introduced in Java 11 LTS

| JEP Number | Landmark Feature | Category | Primary Architectural Value |
| :--- | :--- | :--- | :--- |
| **JEP 321** | **Standard HTTP Client (`java.net.http`)** | Networking | Native non-blocking HTTP/2, WebSocket, and Reactive Streams client. |
| **JEP 323** | **Local-Variable Syntax for Lambda Parameters** | Language Syntax | Enables `var` in lambda formal parameters to attach type annotations. |
| **JEP 330** | **Launch Single-File Source-Code Programs** | Developer Tooling | Run script-like Java source directly without an explicit `javac` compile step (`java Script.java`). |
| **JEP 328** | **Java Flight Recorder (JFR)** | Observability | Low-overhead (<1%) production diagnostic telemetry built into the HotSpot VM. |
| **JEP 318** | **Epsilon: A No-Op Garbage Collector** | Runtime / GC | Zero-allocation GC for performance benchmarking and ephemeral serverless jobs. |
| **JEP 333** | **ZGC: Experimental Low-Latency GC** | Runtime / GC | Sub-millisecond pause time garbage collector for multi-terabyte heaps. |
| Core API | **String & Files Convenience APIs** | Core Libraries | `isBlank()`, `lines()`, `strip()`, `repeat()`, `Files.readString()`, `Files.writeString()`. |

---

# 2. Deep-Dive Feature Breakdown (With Source Code & Internal Mechanics)

## Feature 1: Standard HttpClient (JEP 321)

### What Happens Under the Hood
The modern `java.net.http.HttpClient` replaced the antiquated `HttpURLConnection`.
- Native **HTTP/2 multiplexing**: Multiple concurrent requests share a single TCP connection.
- Built on **Reactive Streams (Flow API)**: Non-blocking request body generation and response consumption.
- Asynchronous dispatch: `sendAsync()` returns a `CompletableFuture<HttpResponse<T>>`, executing on an internal `Executor`.

#### The Old Java 8 Way (Verbose, Blocking `HttpURLConnection`):
```java
// Pre-Java 11: Blocking, awkward stream handling, no HTTP/2 support
URL url = new URL("https://api.example.com/data");
HttpURLConnection conn = (HttpURLConnection) url.openConnection();
conn.setRequestMethod("GET");
conn.setConnectTimeout(5000);

try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
    StringBuilder response = new StringBuilder();
    String line;
    while ((line = reader.readLine()) != null) {
        response.append(line);
    }
}
```

#### The New Idiomatic Java 11 Way:
```java
package com.raysi.lts.java11.codes;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

public class ModernHttpService {
    private final HttpClient client = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_2)
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    public CompletableFuture<String> fetchPayloadAsync(String url) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(3))
                .header("Accept", "application/json")
                .GET()
                .build();

        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(HttpResponse::body);
    }
}
```

---

## Feature 2: Local-Variable Syntax for Lambda Parameters (JEP 323)

### What Happens Under the Hood
Java 10 introduced `var` for local variables. However, lambda parameters could not use `var`. In Java 11, `var` was permitted in lambda parameters so that **type annotations** could be applied without spelling out complex types:

```java
// Pre-Java 11: Must declare full type to apply an annotation
(@Nonnull String s1, @Nonnull String s2) -> s1.concat(s2)

// Java 11 Idiomatic: Type inference preserved while applying annotations!
(@Nonnull var s1, @Nonnull var s2) -> s1.concat(s2)
```

---

## Feature 3: Modern String API Enhancements & Unicode Whitespace

### What Happens Under the Hood
Java 11 added high-performance string manipulation methods:
1. `isBlank()`: Checks if string is empty or contains only whitespace codepoints (more robust than `isEmpty()`).
2. `lines()`: Returns a `Stream<String>` split lazily by `\n`, `\r`, or `\r\n`.
3. `strip()`: Strips leading and trailing whitespace using Unicode standards (`Character.isWhitespace()`).
   - Contrast with legacy `trim()`, which only stripped characters $\le \text{ASCII } 32$ (`U+0020`).

#### Comparison:
```java
// Pre-Java 11:
if (str != null && str.trim().length() == 0) { ... } // Allocates intermediate string!

// Java 11 Idiomatic:
if (str.isBlank()) { ... } // Fast, zero-allocation scan
```

---

## Feature 4: Single-File Source-Code Execution (JEP 330) & Files API

### What Happens Under the Hood
Java 11 allows running standalone scripts directly without compiling:
```bash
java MaintenanceScript.java
```
Under the hood, `javac` compiles the file entirely in memory, loads the resulting classes into a private ClassLoader, and executes `main` without writing `.class` files to the filesystem.

Additionally, `java.nio.file.Files` gained one-liner string helpers:
```java
// Read file directly to string in UTF-8
String config = Files.readString(Path.of("/etc/app/config.json"));

// Write string directly to file
Files.writeString(Path.of("/tmp/output.log"), "Audit Log Content");
```

---

# 3. Compiler & JVM Under-the-Hood Behavior

## 3.1. How `var` Operates in Bytecode
A common misconception is that `var` makes Java dynamically typed.
**It does not:**
- `var` is **strictly a compile-time static type inference shorthand**.
- At compile time, `javac` inspects the initializer expression on the right-hand side, determines the exact static type, and emits the identical bytecode descriptor.
- Inspecting the `.class` file via `javap` reveals the exact fully qualified type in the local variable table (`LocalVariableTable`). There is **zero runtime or bytecode difference** between `var list = new ArrayList<String>()` and `ArrayList<String> list = new ArrayList<String>()`.

## 3.2. Compact Strings (Java 9/11 HotSpot Architecture)
In Java 8, `java.lang.String` stored characters in a `char[]` array (2 bytes per character, UTF-16).
In modern OpenJDK (Java 11+):
- Strings store characters in an internal `byte[]` array accompanied by an 8-bit `coder` flag:
  - `LATIN1` (1 byte per char if all characters fit in ISO-8859-1 / ASCII).
  - `UTF16` (2 bytes per char if multi-byte characters are present).
- **Architectural Impact:** In enterprise backend heaps where typical strings are English ASCII (JSON keys, URLs, IDs), string memory footprint is halved, reducing GC pressure by up to 30%.

---

# 4. Production Anti-Patterns & Pitfalls

### Anti-Pattern 1: Instantiating a New `HttpClient` Per Request
- **The Mistake:** Creating an `HttpClient` inside a method or service call:
  ```java
  public String callService() {
      HttpClient client = HttpClient.newHttpClient(); // OUTAGE HAZARD!
      return client.send(req, BodyHandlers.ofString()).body();
  }
  ```
- **Why It Fails:** Each `HttpClient` instance manages its own selector thread, connection pool, SSL engine, and thread executors. Creating one per request leads to socket descriptor leakage, thread explosion, and high GC overhead.
- **The Fix:** Treat `HttpClient` as an application-scoped singleton. Share one client across all services.

---

### Anti-Pattern 2: Missing JAXB Dependencies After Upgrading from Java 8
- **The Mistake:** Upgrading a Java 8 project directly to Java 11 without adding external dependencies for XML processing.
- **The Consequence:** Application crashes at runtime with:
  ```text
  java.lang.NoClassDefFoundError: javax/xml/bind/JAXBException
  ```
- **The Fix:** Because Java 11 removed Java EE modules (JEP 320), explicitly include standalone JAXB libraries in `pom.xml` or `build.gradle`:
  ```xml
  <dependency>
      <groupId>org.glassfish.jaxb</groupId>
      <artifactId>jaxb-runtime</artifactId>
      <version>2.3.8</version>
  </dependency>
  ```

---

### Anti-Pattern 3: Type Obfuscation with `var`
- **The Mistake:** Using `var` where the initializer does not make the type self-evident:
  ```java
  var result = processOrder(orderId); // Unreadable: what type is result?
  ```
- **The Fix:** Restrict `var` to scenarios where the type is clearly declared in the constructor or factory method (`var client = new HttpClient()`), or in short-lived iteration variables (`for (var entry : map.entrySet())`).

---

# 5. Top 5 Interview Questions & Authoritative Answers

### Q1: Is `var` in Java 11 dynamic typing? How does it differ from JavaScript's `var` or `let`?
> **Authoritative Answer:**  
> No, Java remains 100% statically typed. In JavaScript, `var` variables are dynamically typed at runtime and can hold values of varying types across their lifecycle. In Java, `var` is merely a reserved type name for compile-time local variable type inference. `javac` infers the static type from the initializer expression at compile time and writes the explicit type descriptor into the bytecode. Once inferred, the type is immutable: assigning an incompatible type to that variable causes a compile error.

---

### Q2: What is the architectural difference between `String.trim()` and `String.strip()` introduced in Java 11?
> **Authoritative Answer:**  
> `String.trim()` defines whitespace as any character whose codepoint is less than or equal to `U+0020` (the ASCII space). It completely ignores Unicode whitespace characters like non-breaking spaces (`\u00A0`), em space (`\u2003`), or thin space (`\u2009`).  
> `String.strip()` uses `Character.isWhitespace(int codepoint)`, which adheres to the full Unicode standard. Furthermore, `strip()` is optimized to return a sub-slice without unnecessary string allocations when no whitespace is present.

---

### Q3: Why is `HttpClient.sendAsync()` preferred over `HttpClient.send()` in high-concurrency enterprise services?
> **Authoritative Answer:**  
> `HttpClient.send()` blocks the calling carrier thread until the remote server finishes transmitting headers and the complete response body. Under high concurrency, blocking threads exhausts servlet worker pools (e.g., Tomcat's default 200 threads).  
> In contrast, `sendAsync()` utilizes Java's non-blocking I/O (NIO) selectors and returns a `CompletableFuture`. The operating system's kernel signals the JVM via `epoll`/`kqueue` when data buffers arrive, freeing worker threads to process other requests and dramatically improving service throughput.

---

### Q4: Why did JEP 320 remove Java EE and CORBA modules from Java 11, and what was the migration strategy?
> **Authoritative Answer:**  
> Java 6-8 bundled enterprise Java EE technologies (`java.xml.ws`, `java.xml.bind`, `java.corba`) directly into the standard SE distribution. This created runtime bloat, prevented the independent evolution of those libraries, and tied the JDK to obsolete specifications.  
> Removing them in Java 11 decoupled the JDK from application-level enterprise frameworks. The migration strategy required architects to explicitly import third-party standalone Maven artifacts (such as Jakarta EE / Glassfish JAXB) into their build configurations.

---

### Q5: How does Java 11's single-file source-code launcher (JEP 330) work internally without producing `.class` files?
> **Authoritative Answer:**  
> When executing `java Application.java`, the launcher detects the `.java` extension, invokes the in-memory compiler via the `javax.tools` compiler API, and compiles the source code into byte arrays stored in heap memory. It then loads these compiled bytes using an isolated custom ClassLoader without writing `.class` files to disk. Finally, it uses reflection to locate and invoke `public static void main(String[])`.
