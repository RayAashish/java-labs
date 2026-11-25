# 04. Behavioral Patterns (Strategy, Observer, Template Method, Chain of Responsibility, Command)

---

## 🟢 Layer 1 — Noob Track: How Objects Collaborate

- **Strategy:** Swap algorithms cleanly at runtime (e.g. shipping calculator: `StandardShipping` vs `ExpressShipping` vs `DroneShipping`).
- **Observer:** Publish-Subscribe notification (`YouTubeChannel` notifies all subscribed `Subscriber` objects automatically when a new video uploads).
- **Template Method:** Define a fixed skeleton algorithm in a parent class (`parse -> transform -> export`) and let subclasses override just the varying step (`CsvExporter` vs `JsonExporter`).
- **Chain of Responsibility:** Pass a request down a pipeline of handlers (`AuthFilter -> RateLimitFilter -> ValidationFilter -> Controller`), where each handler inspects, enriches, or short-circuits!
- **Command:** Encapsulate an action as an object (`TextInsertCommand` with `execute()` and `undo()`), enabling undo stacks, retries, and job queues.

---

## 🟡 Layer 2 — Practitioner Track: Strategy vs Template Method

| Dimension | Strategy (Composition) | Template Method (Inheritance) |
|---|---|---|
| **Mechanism** | `HAS-A` interface injected via constructor/method | `IS-A` abstract class with `final` skeleton + abstract hooks |
| **Runtime Swapping** | **YES** — swap strategy object anytime | **NO** — locked at instantiation |
| **Java 8+ Form** | Often just a lambda / `@FunctionalInterface` (`list.sort(Comparator)`)! | Abstract class (`AbstractList`) |

---

## 🔴 Layer 3 — Senior Track: Real-World Production Caveats

1. **Observer Memory Leaks (Lapsed Listener Problem):** If a long-lived EventBus holds strong references to short-lived observers without `unsubscribe()`, observers are never garbage-collected! Use explicit deregistration, `WeakReference`, or scoped async brokers (Kafka/Spring `ApplicationEventPublisher`).
2. **Chain of Responsibility in Web Frameworks:** Tomcat `FilterChain`, Spring Security `FilterChainProxy`, and Netty `ChannelPipeline` are exact Chain of Responsibility implementations. Always guard against forgetting `chain.doFilter(req, res)`!
3. **Strategy + Spring DI:** Inject `Map<String, PaymentStrategy>` in Spring Boot; Spring auto-populates the map by bean name, turning a 200-line `switch` into an $O(1)$ map dispatch!
