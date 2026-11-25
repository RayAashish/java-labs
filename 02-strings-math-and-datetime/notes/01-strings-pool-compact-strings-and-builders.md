# 01. Strings, String Pool, Compact Strings & Builders

---

## 🟢 Layer 1 — Noob Track: Why `String` Never Changes

In Java, a `String` is **immutable** — once created in memory, its characters can never be edited.

```java
String name = "Java";
name.concat(" 25");          // creates a brand new String "Java 25" — and throws it away!
System.out.println(name);    // prints "Java"

name = name + " 25";         // rebinds 'name' to point to a NEW String object
```

### Why did Java's creators make `String` immutable?
1. **Memory Savings (String Pool):** Thousands of variables holding `"USD"` or `"OK"` can safely share a single `"USD"` object in memory.
2. **HashMap Key Safety:** `String` caches its `hashCode()` once and uses it in `HashMap`. If a `String` could mutate, keys in `HashMap` would vanish!
3. **Security:** File paths, database URLs, and class names passed as `String` cannot be silently mutated after validation checks.
4. **Thread Safety:** Immutable objects require zero locking across threads.

---

## 🟡 Layer 2 — Practitioner Track: `String`, `StringBuilder`, `StringBuffer` & Text Blocks

| Feature | `String` | `StringBuilder` | `StringBuffer` |
|---|---|---|---|
| **Mutability** | Immutable | Mutable | Mutable |
| **Thread Safety** | Safe (immutable) | **Not thread-safe** | Safe (`synchronized` methods) |
| **Performance in Loops** | Poor ($O(N^2)$ copies) | **Fastest** ($O(N)$ amortized) | Slower (lock overhead) |
| **Use When** | Fixed text, keys, constants | Building text in a single thread | Rare legacy multi-thread append |

### String Pool vs `new String(...)`
```java
String a = "Hello";             // Placed in String Constant Pool
String b = "Hello";             // Reuses exact same pooled instance (a == b is true)
String c = new String("Hello"); // Forces a brand-new Heap object outside pool (a == c is false)
String d = c.intern();          // Looks up/adds to Pool and returns pooled ref (a == d is true)
```

### Modern `String` APIs (Java 11–21)
- **Text Blocks (Java 15+):** Multi-line strings without `\n` or quote-escaping noise:
  ```java
  String json = """
          {
            "language": "Java",
            "version": 25
          }
          """;
  ```
- **Unicode-aware whitespace:** Prefer `str.isBlank()` and `str.strip()` over `trim()` (which only strips ASCII `<= '\u0020'`).
- **`formatted()` (Java 15+):** `"Order #%d: %s".formatted(101, "SHIPPED")`.

---

## 🔴 Layer 3 — Senior / JVM Internals Track

### 1. Java 9+ Compact Strings (`JEP 254`)
- **Java 8:** `String` stored `private final char[] value;` (2 bytes per char — even ASCII `"hello"` burned 10 bytes!).
- **Java 9+:** `String` stores `private final byte[] value;` + `private final byte coder;` (`LATIN1 = 0` for 1B/char, `UTF16 = 1` for 2B/char).
- Result: ~30–50% smaller Heap footprint and fewer GC cycles for ASCII-heavy backends (JSON, SQL, HTTP headers).

### 2. How `javac` Compiles `"a" + var + "b"` (Java 9 `StringConcatFactory`)
- **Inside loops:** `s += item;` still allocates a new `String` on every iteration ($O(N^2)$ GC pressure) -> ALWAYS use `StringBuilder` in loops!
- **Single-expression concatenation:** Since Java 9 (`JEP 280`), `javac` emits `invokedynamic` calling `java.lang.invoke.StringConcatFactory` instead of `new StringBuilder().append(...)`. At runtime, the JDK sizes the exact destination `byte[]` once with zero intermediate array resizing!

### 3. String Constant Pool Location & GC
- Moved from PermGen to **Main Heap** in Java 7, so unreferenced interned strings are **garbage-collected**!
- Avoid blindly calling `.intern()` on unbounded user input (can bloat the fixed-bucket `StringTable`, though `-XX:StringTableSize` is resizable in modern JVMs).
- Enable **`-XX:+UseStringDeduplication`** with G1GC to let GC point duplicate `String` objects at the same backing `byte[]`.
