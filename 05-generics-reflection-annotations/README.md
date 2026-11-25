# 05. Generics, Reflection, Dynamic Proxies & Annotations

> **Why this matters:** Generics catch type bugs at compile time (`List<String>` instead of raw `List`), while Reflection, Dynamic Proxies, and Annotations are literally how **Spring Boot, Hibernate, Jackson, and JUnit** work under the hood!

---

## ⚡ 2-Minute Rapid Revision Card

- **Generics Invariance:** `Dog` extends `Animal`, `Dog[]` IS-A `Animal[]` (covariant -> causes runtime `ArrayStoreException`), **but `List<Dog>` is NOT a `List<Animal>`** (invariant — caught at compile time!).
- **PECS Rule (Joshua Bloch):** **P**roducer **`extends`**, **C**onsumer **`super`**.
  - Read items from a `List<? extends Number>` (Producer).
  - Write `Integer` items into a `List<? super Integer>` (Consumer).
- **Type Erasure:** Generic types (`<T>`) are checked by `javac` and **erased** in bytecode (`T` becomes `Object`, or its upper bound `Number`). That's why `new T()`, `new T[]`, `instanceof List<String>`, and `List<int>` are illegal.
- **Reflection & Dynamic Proxy:** `Class<?>` inspects fields/methods at runtime; `Proxy.newProxyInstance(...)` intercepts interface calls dynamically (exact mechanism behind `@Transactional`, Spring Data JPA repositories, and Mockito mocks).
- **Annotations:** `@Retention(RetentionPolicy.RUNTIME)` makes annotations readable via Reflection; `@Retention(SOURCE)` (e.g. `@Override`, Lombok) is stripped by `javac`.

---

## 📂 Contents

| File | Focus |
|---|---|
| [`notes/01-generics-wildcards-pec-and-erasure.md`](notes/01-generics-wildcards-pec-and-erasure.md) | Generic classes/methods, PECS wildcards, Type Erasure, Bridge Methods, `@SafeVarargs` |
| [`notes/02-reflection-and-dynamic-proxies.md`](notes/02-reflection-and-dynamic-proxies.md) | `Class<?>` introspection, `setAccessible`, `MethodHandles`, `java.lang.reflect.Proxy` |
| [`notes/03-annotations-and-metadata-processing.md`](notes/03-annotations-and-metadata-processing.md) | Built-ins, Meta-annotations (`@Retention`, `@Target`), Custom runtime processors |
| [`codes/`](codes/) | 3 runnable labs: Generics/PECS, Spring-like Dynamic Proxy, Custom `@Timed` Annotation |
| [`interview/INTERVIEW_QUESTIONS.md`](interview/INTERVIEW_QUESTIONS.md) | 30+ interview Q&As on PECS, Erasure, Bridge Methods, Dynamic Proxies & Annotations |

---

## 🚀 Compile & Run

```bash
javac -d out $(find 05-generics-reflection-annotations/codes -name "*.java")
java -cp out generics.GenericsAndPecsDemo
java -cp out reflection.ReflectionAndDynamicProxyDemo
java -cp out annotations.CustomAnnotationProcessorDemo
```
