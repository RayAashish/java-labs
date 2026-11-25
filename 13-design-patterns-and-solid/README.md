# 13. SOLID Principles & Design Patterns in Modern Java

> **Philosophy:** Design Patterns are not academic ceremony — they are reusable names for decoupling change, protecting invariants, and making code testable. Every pattern here maps directly to classes inside the **JDK (`java.io`, `Runtime`, `Comparator`, `Proxy`)** and **Spring Boot**.

---

## ⚡ 2-Minute Rapid Revision Card

- **SOLID:** **S**ingle Responsibility (1 reason to change), **O**pen/Closed (extend via polymorphism, don't modify core switch-ladders), **L**iskov Substitution (subtypes preserve parent contracts), **I**nterface Segregation (small focused roles over fat interfaces), **D**ependency Inversion (depend on abstractions, inject implementations).
- **Singleton Winner:** `enum Singleton { INSTANCE; }` (reflection-safe, serialization-safe, thread-safe). If lazy-loaded with `double-checked locking`, the field **MUST be `volatile`** to prevent partial-object publication via instruction reordering!
- **JDK Patterns in the Wild:**
  - **Builder:** `StringBuilder`, `HttpRequest.newBuilder()`, `Stream.builder()`
  - **Factory Method:** `List.of()`, `EnumSet.of()`, `Executors.newVirtualThreadPerTaskExecutor()`
  - **Adapter:** `InputStreamReader(InputStream)` (bytes -> chars), `Arrays.asList(arr)`
  - **Decorator:** `new BufferedInputStream(new FileInputStream(...))`
  - **Proxy:** `java.lang.reflect.Proxy`, Spring `@Transactional` / `@Cacheable`
  - **Strategy:** `Comparator<T>` passed to `List.sort(...)`
  - **Template Method:** `AbstractList`, `HttpServlet.service()` -> `doGet()`/`doPost()`
  - **Chain of Responsibility:** Servlet `FilterChain`, Spring Security `SecurityFilterChain`

---

## 📂 Contents

| File | Focus |
|---|---|
| [`notes/01-solid-principles-modern-java.md`](notes/01-solid-principles-modern-java.md) | S.O.L.I.D. explained with realistic backend refactoring examples |
| [`notes/02-creational-patterns.md`](notes/02-creational-patterns.md) | Singleton (`enum` & `volatile` DCL), Factory, Abstract Factory, Builder, Prototype |
| [`notes/03-structural-patterns.md`](notes/03-structural-patterns.md) | Adapter, Decorator, Proxy, Facade |
| [`notes/04-behavioral-patterns.md`](notes/04-behavioral-patterns.md) | Strategy, Observer, Template Method, Chain of Responsibility, Command |
| [`codes/`](codes/) | 4 self-contained runnable demos for SOLID, Creational, Structural, and Behavioral |
| [`interview/INTERVIEW_QUESTIONS.md`](interview/INTERVIEW_QUESTIONS.md) | 30+ SOLID & Design Patterns interview Q&As |

---

## 🚀 Compile & Run

```bash
javac -d out $(find 13-design-patterns-and-solid/codes -name "*.java")
java -cp out patterns.solid.SolidPrinciplesDemo
java -cp out patterns.creational.CreationalPatternsDemo
java -cp out patterns.structural.StructuralPatternsDemo
java -cp out patterns.behavioral.BehavioralPatternsDemo
```
