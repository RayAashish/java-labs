# 03. Structural Patterns (Adapter, Decorator, Proxy, Facade)

---

## 🟢 Layer 1 — Noob Track: Connecting & Wrapping Objects

- **Adapter:** A travel power-plug adapter — makes an incompatible interface fit what your code expects (`InputStreamReader` turns a byte `InputStream` into a char `Reader`).
- **Decorator:** Wrapping layers onto a gift — dynamically stacks extra behaviors around an object at runtime without exploding subclass counts (`BufferedInputStream(FileInputStream)`).
- **Proxy:** A security bouncer standing in front of a VIP — shares the **exact same interface** as the real object to control access, cache, log, or manage transactions (`Spring AOP`).
- **Facade:** A hotel concierge — gives you one simple button (`checkoutOrder()`) that hides 5 complex subsystems (Inventory, Payment, FraudCheck, Shipping, Email).

---

## 🟡 Layer 2 — Practitioner Track: Decorator vs Proxy vs Adapter

| Pattern | Implements Target Interface? | Changes Interface? | Primary Intent |
|---|---|---|---|
| **Adapter** | Implements target, wraps legacy/foreign | **YES** (translates A -> B) | Interoperability between mismatched APIs |
| **Decorator** | **Same** interface as wrapped object | NO | Dynamically stack/combine behaviors (e.g. encryption + compression + buffering) |
| **Proxy** | **Same** interface as real subject | NO | Control access, lazy-init, caching, transaction/security cross-cutting concerns |
| **Facade** | Defines a new simplified interface | **YES** (simplifies many -> 1) | Reduce coupling to complex multi-class subsystems |

---

## 🔴 Layer 3 — Senior Track: Decorator Explosion vs Functional Composition

- Classic GoF Decorator requires creating wrapper classes (`LoggingStream`, `EncryptedStream`, `BufferedStream`).
- In modern Java 8+, **linear single-method Decorators** can often be expressed cleanly with `Function.andThen()` or `UnaryOperator<T>` composition (`raw -> encrypt(compress(raw))`), while full multi-method stateful decorators (`BufferedReader`, `Collections.unmodifiableList`) still shine as classes.
