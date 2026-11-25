# Interview Questions — Interface `default` & `static` Methods

## 🟢 Beginner

### Q1. Why did Java 8 introduce `default` methods in interfaces?
To add new methods (like `Collection.stream()` and `Iterable.forEach()`) to existing interfaces while maintaining **100% source & binary compatibility** with existing implementing classes.

### Q2. Can an interface `static` method be called on an implementing class instance or reference?
**No.** Unlike class static methods, `static` methods in interfaces are **not inherited**. You must always invoke them through the interface type name: `Comparator.naturalOrder()`, `List.of(...)`.

---

## 🟡 Intermediate

### Q3. Explain the 3 Diamond Problem resolution rules for interface `default` methods.
1. **Class wins:** A method in a superclass (concrete or abstract) always beats any interface `default` method.
2. **Sub-interface wins:** Between `Child extends Parent`, the more specific interface `Child`'s `default` method wins.
3. **Explicit conflict resolution:** If two unrelated interfaces clash, the implementing class won't compile unless it overrides the method (and may delegate via `InterfaceA.super.foo()`).

### Q4. Now that interfaces have `default`, `static`, and `private` methods (Java 9), why do `abstract class`es still exist?
Because interfaces **still cannot hold mutable instance state (instance fields)** or **constructors**, nor can they declare `protected`/package-private members cleanly like an abstract base class.

---

## 🔴 Senior

### Q5. Can you define a `default equals(Object)` or `default toString()` inside an interface?
**No — compile error!** Under Rule 1 ("Class always wins"), every class extends `java.lang.Object`, which already declares concrete `equals`, `hashCode`, and `toString` implementations — so an interface `default` implementation of them could never execute polymorphically anyway.
