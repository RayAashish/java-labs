# 13. SOLID Principles & Design Patterns — Interview Questions & Answers

---

## 🟢 Beginner Questions

### Q1. What does SOLID stand for?
- **S:** Single Responsibility Principle (1 reason/actor to change)
- **O:** Open/Closed Principle (open for extension, closed for modification)
- **L:** Liskov Substitution Principle (subtypes honor parent behavior)
- **I:** Interface Segregation Principle (small focused interfaces over fat ones)
- **D:** Dependency Inversion Principle (depend on interfaces/abstractions, not concrete classes)

### Q2. What are the 3 GoF categories of Design Patterns?
1. **Creational:** How objects are created (`Singleton`, `Factory Method`, `Abstract Factory`, `Builder`, `Prototype`).
2. **Structural:** How classes/objects compose (`Adapter`, `Decorator`, `Proxy`, `Facade`, `Composite`).
3. **Behavioral:** How objects communicate & delegate responsibility (`Strategy`, `Observer`, `Template Method`, `Chain of Responsibility`, `Command`, `Iterator`).

### Q3. When should you use the Builder pattern over Constructors?
When a class has **4+ parameters** (especially multiple optional fields of the same type like `String`, `boolean`, `int`), telescoping constructors become unreadable and error-prone. Builder enables fluent, readable, immutable object creation with validation inside `.build()`.

---

## 🟡 Intermediate Questions

### Q4. Give real examples of Design Patterns inside the standard JDK.
- **Singleton:** `Runtime.getRuntime()`
- **Builder:** `StringBuilder`, `HttpRequest.newBuilder()`, `Locale.Builder`
- **Factory Method:** `List.of()`, `EnumSet.of()`, `Calendar.getInstance()`, `Executors.newVirtualThreadPerTaskExecutor()`
- **Adapter:** `InputStreamReader(InputStream)`, `Arrays.asList(array)`
- **Decorator:** `BufferedInputStream(FileInputStream)`, `Collections.unmodifiableList(list)`
- **Proxy:** `java.lang.reflect.Proxy`
- **Strategy:** `Comparator<T>` passed to `List.sort()` / `Collections.sort()`
- **Template Method:** `AbstractList`, `AbstractMap`, `HttpServlet`
- **Iterator:** `java.util.Iterator<E>`

### Q5. Factory Method vs Abstract Factory?
- **Factory Method:** Creates **one** product via a single factory method (`NotificationFactory.create("SMS")`).
- **Abstract Factory:** Creates **families of related/compatible products** (`UIFactory` -> `createButton()` + `createCheckbox()` for macOS vs Windows; `CloudFactory` -> S3 + KMS vs GCS + CloudKMS).

### Q6. Decorator vs Proxy vs Adapter?
- **Adapter:** Translates an **incompatible** interface into what the client expects.
- **Decorator:** Implements the **same** interface to dynamically stack extra responsibilities/behaviors (`Encryption(Compression(Stream))`).
- **Proxy:** Implements the **same** interface to control access, cache, lazy-initialize, or weave cross-cutting concerns (`@Transactional`, `@PreAuthorize`).

### Q7. Strategy vs Template Method?
- **Strategy** uses **composition (`HAS-A`)**: swap algorithms cleanly at runtime (often a simple lambda in modern Java).
- **Template Method** uses **inheritance (`IS-A`)**: base class fixes algorithm steps and subclasses override abstract hook steps.

---

## 🔴 Senior / Architecture Questions

### Q8. Write a thread-safe Singleton using Double-Checked Locking, and explain WHY `volatile` is mandatory!
```java
private static volatile Cache instance;
public static Cache getInstance() {
    if (instance == null) {
        synchronized (Cache.class) {
            if (instance == null) instance = new Cache();
        }
    }
    return instance;
}
```
**Why `volatile` is mandatory:** `instance = new Cache()` is NOT atomic; the CPU/JIT can reorder instructions into: (1) allocate memory, (2) **assign `instance` reference BEFORE constructor finishes**, (3) run constructor! Without `volatile`, Thread B reads `instance != null` on the outer check and uses a **partially constructed object**! `volatile` inserts memory barriers preventing that reordering.

### Q9. What are all 3 ways to break a classic Singleton, and why does `enum` solve all of them?
Classic Singletons break via:
1. **Reflection** (`constructor.setAccessible(true); constructor.newInstance()`)
2. **Deserialization** (`ObjectInputStream.readObject()` allocates a new instance unless `readResolve()` is defined)
3. **Cloning** (`super.clone()` unless overridden to throw)
A single-element **`enum Singleton { INSTANCE; }`** is guaranteed by the JVM to block reflective constructor calls, survive serialization identically, and initialize thread-safely during `<clinit>`.

### Q10. Why do Spring applications rarely write static `Singleton.getInstance()`?
Static globals introduce hidden coupling and make unit testing difficult because tests cannot easily swap dependencies. Instead, **Spring Inversion of Control (IoC)** creates singleton-scoped beans managed by the container and injects them via constructors (**Dependency Inversion Principle**).

### Q11. Explain the Liskov Substitution Principle (LSP) with a classic violation.
If `Square extends Rectangle`, calling `rect.setWidth(5); rect.setHeight(10); assert rect.area() == 50;` fails when `rect` is a `Square` (whose `setHeight` overwrote width to `10`, returning `100`!). `Square` is mathematically a `Rectangle`, **but behaviorally violates `Rectangle`'s mutable setter contract**. Fix: make shapes immutable `Shape` (`area()`) without mutable width/height setters!

### Q12. How does Chain of Responsibility scale in Spring Security / Servlet Filters?
Each filter wraps/invokes `chain.doFilter(request, response)` recursively (or iterates a filter list): auth token parsing -> CORS -> rate limiting -> logging -> controller, allowing clean insertion, ordering (`@Order`), and early-return short-circuiting (`401 Unauthorized`) with zero coupling between filters!
