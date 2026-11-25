# Java Data Types: Comprehensive Notes

In Java, every variable has a declared type, which determines the values it can hold, the memory it occupies, and the operations that can be performed on it. Java is a **strongly typed and statically typed** language.

---

## 1. Classification of Data Types

```
                            Java Data Types
                                   |
         +-------------------------+-------------------------+
         |                                                   |
   Primitive (8 types)                               Non-Primitive / Reference
   - byte    - float                                 - Arrays
   - short   - double                                - Classes & Objects
   - int     - char                                  - Interfaces
   - long    - boolean                               - Enums, Records, Strings
```

---

## 2. Primitive Data Types

Primitive types are predefined by the language and named by a reserved keyword. They do not share state with other values and reside directly on the thread stack.

| Data Type | Wrapper Class | Size | Default Value | Range | Example |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **`byte`** | `Byte` | 1 byte (8 bits) | `0` | `-128` to `127` (\(-2^7\) to \(2^7 - 1\)) | `byte b = 100;` |
| **`short`** | `Short` | 2 bytes (16 bits) | `0` | `-32,768` to `32,767` (\(-2^{15}\) to \(2^{15} - 1\)) | `short s = 10000;` |
| **`int`** | `Integer` | 4 bytes (32 bits) | `0` | `-2,147,483,648` to `2,147,483,647` (\(-2^{31}\) to \(2^{31} - 1\)) | `int count = 42;` |
| **`long`** | `Long` | 8 bytes (64 bits) | `0L` | \(-2^{63}\) to \(2^{63} - 1\) | `long dist = 100000000000L;` |
| **`float`** | `Float` | 4 bytes (32 bits) | `0.0f` | IEEE 754 single-precision (approx 6-7 decimal digits) | `float pi = 3.1415f;` |
| **`double`** | `Double` | 8 bytes (64 bits) | `0.0d` | IEEE 754 double-precision (approx 15-16 decimal digits) | `double price = 99.99;` |
| **`char`** | `Character` | 2 bytes (16 bits) | `'\u0000'` | Unicode characters `0` to `65,535` (`'\u0000'` to `'\uffff'`) | `char grade = 'A';` |
| **`boolean`** | `Boolean` | JVM-dependent (1 bit logically, stored as 1 byte in memory / 4 bytes in arrays) | `false` | `true` or `false` | `boolean active = true;` |

> [!NOTE]
> `char` in Java is unsigned and uses 16-bit Unicode encoding (UTF-16 code units), unlike C/C++ where `char` is an 8-bit ASCII value.

---

## 3. Literals and Suffixes

- **Integer literals**: Defaults to `int`. To specify `long`, append `L` or `l` (always prefer uppercase `L` to avoid confusion with `1`):
  ```java
  long bigNum = 5_000_000_000L; // Underscores can improve readability (Java 7+)
  ```
- **Floating-point literals**: Defaults to `double`. To specify `float`, append `F` or `f`. To explicitly denote `double`, append `D` or `d`:
  ```java
  float f = 3.14f;    // 'f' or 'F' is required
  double d = 3.14;    // default is double
  ```
- **Radix prefixes**:
  - Binary: `0b` or `0B` (e.g., `0b1010` = 10)
  - Octal: `0` (e.g., `012` = 10)
  - Hexadecimal: `0x` or `0X` (e.g., `0xA` = 10)

---

## 4. Type Casting (Conversion)

### Widening Casting (Implicit / Automatic)
Converting a smaller type to a larger type size. No data loss (except potential precision loss from `long` to `float`).
```
byte -> short -> int -> long -> float -> double
          char --^
```
```java
int i = 100;
long l = i;       // Automatic widening
double d = l;     // Automatic widening
```

### Narrowing Casting (Explicit / Manual)
Converting a larger type to a smaller type size. Requires explicit cast and can result in truncation or overflow.
```java
double d = 9.78;
int i = (int) d;  // i becomes 9 (decimal portion truncated)

int overflowInt = 130;
byte b = (byte) overflowInt; // b becomes -126 due to two's complement wraparound
```

---

## 5. Primitive vs Reference Types

| Aspect | Primitive Types | Reference Types |
| :--- | :--- | :--- |
| **Storage** | Stored on the Stack directly with value | Reference stored on Stack; object stored on Heap |
| **Default Value** | Has specific default (`0`, `false`, `\u0000`) for fields | Defaults to `null` for fields |
| **Methods** | No methods, cannot call `.toString()` | Can call methods defined on `java.lang.Object` |
| **Generics** | Cannot be used directly (e.g., `List<int>` is invalid) | Required in generic collections (e.g., `List<Integer>`) |
| **Memory footprint** | Extremely lightweight, no object header overhead | Object header overhead (12–16 bytes) + alignment |
