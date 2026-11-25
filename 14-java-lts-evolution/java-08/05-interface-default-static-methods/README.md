# Java 8 — Interface `default` & `static` Methods

## ⚡ 2-Minute Revision
- **Why Introduced:** To evolve existing JDK interfaces like `Collection` (`stream()`), `Iterable` (`forEach()`), and `List` (`sort()`, `replaceAll()`) **without breaking millions of existing implementing classes worldwide**!
- **3 Diamond Conflict Resolution Rules:**
  1. **Class wins over Interface** (concrete/abstract superclass implementation takes priority over any interface `default`).
  2. **More Specific Interface wins** (`ChildInterface extends ParentInterface` beats `ParentInterface`).
  3. **Explicit Disambiguation:** If two unrelated interfaces define conflicting `default` methods, `javac` forces you to override and optionally delegate via **`InterfaceName.super.method()`**.
- **`static` Interface Methods:** Belong strictly to the interface itself — **never inherited** by implementing classes or sub-interfaces (avoiding subtle method hiding issues). Note: **Java 9** later added **`private`** interface helper methods!

## 📂 Contents
- [`notes/default-and-static-interface-methods.md`](notes/default-and-static-interface-methods.md)
- [`codes/lts/java08/interfaces/DefaultAndStaticMethodsDemo.java`](codes/lts/java08/interfaces/DefaultAndStaticMethodsDemo.java)
- [`interview/INTERVIEW_QUESTIONS.md`](interview/INTERVIEW_QUESTIONS.md)
