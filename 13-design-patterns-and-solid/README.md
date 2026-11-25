# 13. SOLID Principles & Design Patterns in Modern Java

> **Granular Design:** **Every single SOLID principle** and **every major GoF Design Pattern** has its **own dedicated runnable `.java` file** documenting **What it is**, **Why it matters**, **❌ Bad Code**, and **✅ Good Code**.

---

## ⚡ 2-Minute Rapid Revision Card

- **SOLID:** **S**ingle Responsibility (1 actor/reason to change), **O**pen/Closed (extend via polymorphism, don't edit tested `if-else`), **L**iskov Substitution (preserve subtype invariants), **I**nterface Segregation (small focused roles over fat interfaces), **D**ependency Inversion (depend on abstractions via constructor injection).
- **Singleton Winner:** `enum Singleton { INSTANCE; }` (immune to reflection, serialization, and thread races). Lazy DCL **MUST** declare `private static volatile`.
- **JDK Patterns in the Wild:**
  - **Builder:** `StringBuilder`, `HttpRequest.newBuilder()`
  - **Factory Method:** `List.of()`, `EnumSet.of()`, `Executors.newVirtualThreadPerTaskExecutor()`
  - **Adapter:** `InputStreamReader(InputStream)`, `Arrays.asList(arr)`
  - **Decorator:** `new BufferedInputStream(new GZIPInputStream(new FileInputStream(...)))`
  - **Proxy:** `java.lang.reflect.Proxy`, Spring `@Transactional` / `@Cacheable`
  - **Composite:** Swing/UI trees, file trees
  - **Strategy:** `Comparator<T>` passed to `List.sort(...)`
  - **Template Method:** `AbstractList`, `HttpServlet.service()`
  - **Chain of Responsibility:** Servlet `FilterChain`, Spring `SecurityFilterChain`

---

## 📂 Complete Per-Concept Code Index

### 1. S.O.L.I.D. Principles (`codes/patterns/solid/` & [`01-solid-principles-modern-java.md`](notes/01-solid-principles-modern-java.md))
| Principle | Demo Class (Bad vs Good) | Core Lesson |
|---|---|---|
| **S** — Single Responsibility | [`SingleResponsibilityPrincipleDemo.java`](codes/patterns/solid/SingleResponsibilityPrincipleDemo.java) | Split math, persistence, and notifications |
| **O** — Open/Closed | [`OpenClosedPrincipleDemo.java`](codes/patterns/solid/OpenClosedPrincipleDemo.java) | Extend payments via registry/interface, not `if-else` |
| **L** — Liskov Substitution | [`LiskovSubstitutionPrincipleDemo.java`](codes/patterns/solid/LiskovSubstitutionPrincipleDemo.java) | Why `Square extends Rectangle` breaks invariants |
| **I** — Interface Segregation | [`InterfaceSegregationPrincipleDemo.java`](codes/patterns/solid/InterfaceSegregationPrincipleDemo.java) | Compose `Printable` / `Scannable` / `Faxable` roles |
| **D** — Dependency Inversion | [`DependencyInversionPrincipleDemo.java`](codes/patterns/solid/DependencyInversionPrincipleDemo.java) | Inject `OrderStore` abstraction (Prod Postgres vs Test Fake) |

### 2. Creational Patterns (`codes/patterns/creational/` & [`02-creational-patterns.md`](notes/02-creational-patterns.md))
| Pattern | Demo Class |
|---|---|
| **Singleton** | [`SingletonPatternDemo.java`](codes/patterns/creational/SingletonPatternDemo.java) *(DCL `volatile`, Bill Pugh Holder, Enum, Reflection Guard)* |
| **Factory Method** | [`FactoryMethodPatternDemo.java`](codes/patterns/creational/FactoryMethodPatternDemo.java) |
| **Abstract Factory** | [`AbstractFactoryPatternDemo.java`](codes/patterns/creational/AbstractFactoryPatternDemo.java) *(AWS vs GCP coherent families)* |
| **Builder** | [`BuilderPatternDemo.java`](codes/patterns/creational/BuilderPatternDemo.java) |
| **Prototype** | [`PrototypePatternDemo.java`](codes/patterns/creational/PrototypePatternDemo.java) *(Shallow bug vs Deep copy)* |

### 3. Structural Patterns (`codes/patterns/structural/` & [`03-structural-patterns.md`](notes/03-structural-patterns.md))
| Pattern | Demo Class |
|---|---|
| **Adapter** | [`AdapterPatternDemo.java`](codes/patterns/structural/AdapterPatternDemo.java) |
| **Decorator** | [`DecoratorPatternDemo.java`](codes/patterns/structural/DecoratorPatternDemo.java) |
| **Proxy** | [`ProxyPatternDemo.java`](codes/patterns/structural/ProxyPatternDemo.java) |
| **Facade** | [`FacadePatternDemo.java`](codes/patterns/structural/FacadePatternDemo.java) |
| **Composite** | [`CompositePatternDemo.java`](codes/patterns/structural/CompositePatternDemo.java) |

### 4. Behavioral Patterns (`codes/patterns/behavioral/` & [`04-behavioral-patterns.md`](notes/04-behavioral-patterns.md))
| Pattern | Demo Class |
|---|---|
| **Strategy** | [`StrategyPatternDemo.java`](codes/patterns/behavioral/StrategyPatternDemo.java) |
| **Observer** | [`ObserverPatternDemo.java`](codes/patterns/behavioral/ObserverPatternDemo.java) |
| **Template Method** | [`TemplateMethodPatternDemo.java`](codes/patterns/behavioral/TemplateMethodPatternDemo.java) |
| **Chain of Responsibility** | [`ChainOfResponsibilityPatternDemo.java`](codes/patterns/behavioral/ChainOfResponsibilityPatternDemo.java) |
| **Command** | [`CommandPatternDemo.java`](codes/patterns/behavioral/CommandPatternDemo.java) *(Undo stack)* |
| **State** | [`StatePatternDemo.java`](codes/patterns/behavioral/StatePatternDemo.java) *(Order lifecycle state machine)* |

### 5. Interview Bank
- 👉 **[`interview/INTERVIEW_QUESTIONS.md`](interview/INTERVIEW_QUESTIONS.md)**

---

## 🚀 Compile & Run Any Pattern

```bash
javac -d out $(find 13-design-patterns-and-solid/codes -name "*.java")

# Run any individual SOLID principle or GoF pattern:
java -cp out patterns.solid.SingleResponsibilityPrincipleDemo
java -cp out patterns.solid.OpenClosedPrincipleDemo
java -cp out patterns.solid.LiskovSubstitutionPrincipleDemo
java -cp out patterns.solid.InterfaceSegregationPrincipleDemo
java -cp out patterns.solid.DependencyInversionPrincipleDemo
java -cp out patterns.creational.SingletonPatternDemo
java -cp out patterns.structural.DecoratorPatternDemo
java -cp out patterns.behavioral.StatePatternDemo
```
