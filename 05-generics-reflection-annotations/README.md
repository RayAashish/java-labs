# 05. Generics, Reflection, Dynamic Proxies & Annotations

> **Why this matters:** Generics catch type bugs at compile time (`List<String>` instead of raw `List`), while Reflection, Dynamic Proxies, and Annotations are literally how **Spring Boot, Hibernate, Jackson, and JUnit** work under the hood!

---

## ⚡ 2-Minute Rapid Revision Card

- **Generics Invariance:** `Dog` extends `Animal`, `Dog[]` IS-A `Animal[]` (covariant -> runtime `ArrayStoreException`), **but `List<Dog>` is NOT a `List<Animal>`** (invariant — caught at compile time!).
- **PECS Rule (Joshua Bloch):** **P**roducer **`extends`**, **C**onsumer **`super`**.
- **Type Erasure & Bridge Methods:** Generic `<T>` is checked by `javac` and **erased** in bytecode (`T` -> `Object`, or its upper bound). `javac` emits synthetic **bridge methods** to preserve virtual overriding.
- **Reflection & Dynamic Proxy:** `Class<?>` inspects fields/methods at runtime; `Proxy.newProxyInstance(...)` intercepts interface calls dynamically (exact mechanism behind `@Transactional`, Spring Data JPA repositories, and Mockito mocks).
- **Annotations:** `@Retention(RetentionPolicy.RUNTIME)` makes annotations readable via Reflection; `@Retention(SOURCE)` (e.g. `@Override`, Lombok) is stripped by `javac`.

---

## 📂 Contents

| Note | Companion Runnable Labs |
|---|---|
| [`01-generics-wildcards-pec-and-erasure.md`](notes/01-generics-wildcards-pec-and-erasure.md) | [`GenericsBasicsAndBoundsDemo.java`](codes/generics/GenericsBasicsAndBoundsDemo.java)<br>[`WildcardsAndPecsDemo.java`](codes/generics/WildcardsAndPecsDemo.java)<br>[`TypeErasureAndBridgeMethodsDemo.java`](codes/generics/TypeErasureAndBridgeMethodsDemo.java) |
| [`02-reflection-and-dynamic-proxies.md`](notes/02-reflection-and-dynamic-proxies.md) | [`ReflectionIntrospectionDemo.java`](codes/reflection/ReflectionIntrospectionDemo.java)<br>[`DynamicProxyAopDemo.java`](codes/reflection/DynamicProxyAopDemo.java) |
| [`03-annotations-and-metadata-processing.md`](notes/03-annotations-and-metadata-processing.md) | [`CustomAnnotationProcessorDemo.java`](codes/annotations/CustomAnnotationProcessorDemo.java) |
| **Interview Bank** | 👉 [`interview/INTERVIEW_QUESTIONS.md`](interview/INTERVIEW_QUESTIONS.md) |

---

## 🚀 Compile & Run

```bash
javac -d out $(find 05-generics-reflection-annotations/codes -name "*.java")
java -cp out generics.GenericsBasicsAndBoundsDemo
java -cp out generics.WildcardsAndPecsDemo
java -cp out generics.TypeErasureAndBridgeMethodsDemo
java -cp out reflection.ReflectionIntrospectionDemo
java -cp out reflection.DynamicProxyAopDemo
java -cp out annotations.CustomAnnotationProcessorDemo
```
