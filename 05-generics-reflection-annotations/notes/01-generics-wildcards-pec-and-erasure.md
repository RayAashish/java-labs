# 01. Generics, PECS Wildcards & Type Erasure

---

## 🟢 Layer 1 — Noob Track: Type-Safe Containers

Before Java 5, collections held `Object`. You could accidentally put an `Integer` into a list of names and only discover the crash (`ClassCastException`) at runtime:
```java
// Pre-Java 5 unsafe code
List names = new ArrayList();
names.add("Aashish");
names.add(42); // Compiles silently!
String s = (String) names.get(1); // BOOM at runtime: ClassCastException!
```
**Generics (`<T>`)** move that failure from **runtime production** to **compile time** inside your IDE:
```java
List<String> names = new ArrayList<>();
names.add("Aashish");
// names.add(42); // Compile-time error! Safe!
```

---

## 🟡 Layer 2 — Practitioner Track: Bounds & PECS Wildcards

### 1. Bounded Type Parameters
Restrict `T` to types that support specific contracts:
```java
// Class bound MUST come first, followed by interface bounds
public static <T extends Number & Comparable<T>> T max(T a, T b) {
    return a.compareTo(b) >= 0 ? a : b;
}
```

### 2. Why `List<Integer>` Is NOT a Subtype of `List<Number>`
If `List<Integer>` were assignable to `List<Number>`, you could do:
```java
List<Integer> ints = new ArrayList<>();
List<Number> nums = ints; // Illegal! (Compile error)
nums.add(3.14);           // Would corrupt ints with a Double!
```

### 3. PECS: Producer `extends`, Consumer `super`
```text
┌─────────────────────────────────────────────────────────────────────────┐
│  PECS = Producer Extends, Consumer Super                                │
├─────────────────────────────────┬───────────────────────────────────────┤
│  List<? extends Number>         │  List<? super Integer>                │
│  • READS out Numbers safely     │  • WRITES Integers safely             │
│  • CANNOT add() (except null)   │  • Reads back only as Object          │
│  • Example: sumOf(src)          │  • Example: addIntegers(dest)         │
└─────────────────────────────────┴───────────────────────────────────────┘
```

```java
public static <T> void copy(List<? super T> dest, List<? extends T> src) {
    for (T item : src) { // src PRODUCES T
        dest.add(item);  // dest CONSUMES T
    }
}
```

---

## 🔴 Layer 3 — Senior / JVM Track: Type Erasure, Bridge Methods & Heap Pollution

1. **Type Erasure:** To maintain binary backward compatibility with Java 1.4 bytecode, `javac` erases generic type parameters after type-checking:
   - Unbounded `<T>` -> `java.lang.Object`
   - Bounded `<T extends Comparable<T>>` -> `java.lang.Comparable`
   - `ArrayList<String>` and `ArrayList<Integer>` have the **exact same runtime `Class`** (`list1.getClass() == list2.getClass()` is `true`!).
2. **Bridge Methods:** When `class Node implements Comparable<Node>` defines `public int compareTo(Node o)`, `javac` generates a synthetic **bridge method** `public int compareTo(Object o)` that casts to `Node` and delegates, keeping JVM virtual dispatch valid!
3. **Why `new T()` and `new T[10]` Are Illegal:** Because `T` is erased to `Object` at runtime, the JVM wouldn't know which constructor or array component type to allocate! Workaround: pass `Class<T> type` (`type.getDeclaredConstructor().newInstance()`) or `IntFunction<T[]>`.
4. **Heap Pollution & `@SafeVarargs`:** Combining varargs with non-reifiable generic types (`List<String>...`) causes `javac` to create an `Object[]` under the hood (`unchecked generic array creation`). Annotate `final` or `static` methods with `@SafeVarargs` ONLY when you don't expose or overwrite the varargs array!
