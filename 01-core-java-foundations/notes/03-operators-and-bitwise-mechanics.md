# Java Operators: Detailed Notes

An **operator** is a symbol that performs an operation on one or more **operands** and produces a result.

```java
int result = a + b;   // '+' is the operator, 'a' and 'b' are operands
```

---

## Table of Contents

1. [Classification of Operators](#1-classification-of-operators)
2. [Arithmetic Operators](#2-arithmetic-operators)
3. [Unary Operators](#3-unary-operators)
4. [Assignment Operators](#4-assignment-operators)
5. [Relational (Comparison) Operators](#5-relational-comparison-operators)
6. [Logical Operators](#6-logical-operators)
7. [Bitwise Operators](#7-bitwise-operators)
8. [Shift Operators](#8-shift-operators)
9. [Ternary Operator](#9-ternary-operator)
10. [instanceof Operator](#10-instanceof-operator)
11. [Type Promotion and Casting](#11-type-promotion-and-casting)
12. [Operator Precedence and Associativity](#12-operator-precedence-and-associativity)
13. [Overflow and Safe Arithmetic](#13-overflow-and-safe-arithmetic)
14. [Common Pitfalls](#14-common-pitfalls)
15. [Interview-Style Output Questions](#15-interview-style-output-questions)
16. [Quick Cheat Sheet](#16-quick-cheat-sheet)

---

## 1. Classification of Operators

### By number of operands

| Type    | Operands | Examples                    |
|---------|----------|-----------------------------|
| Unary   | 1        | `++`, `--`, `!`, `~`, `-x`  |
| Binary  | 2        | `+`, `-`, `*`, `==`, `&&`   |
| Ternary | 3        | `condition ? a : b`         |

### By function

| Category        | Operators                                                  |
|-----------------|------------------------------------------------------------|
| Arithmetic      | `+  -  *  /  %`                                            |
| Unary           | `+  -  ++  --  !  ~`                                       |
| Assignment      | `=  +=  -=  *=  /=  %=  &=  \|=  ^=  <<=  >>=  >>>=`      |
| Relational      | `==  !=  >  <  >=  <=`                                     |
| Logical         | `&&  \|\|  !`                                              |
| Bitwise         | `&  \|  ^  ~`                                              |
| Shift           | `<<  >>  >>>`                                              |
| Ternary         | `? :`                                                      |
| Type comparison | `instanceof`                                               |
| Miscellaneous   | `.`  `[]`  `()`  `(type)` cast  `new`  `->` lambda  `::` method reference |

---

## 2. Arithmetic Operators

| Operator | Meaning        | Example  | Result |
|----------|----------------|----------|--------|
| `+`      | Addition / String concatenation | `5 + 3` | `8` |
| `-`      | Subtraction    | `5 - 3`  | `2`    |
| `*`      | Multiplication | `5 * 3`  | `15`   |
| `/`      | Division       | `7 / 2`  | `3` (integer division) |
| `%`      | Remainder (modulus) | `7 % 3` | `1` |

### 2.1 Integer division truncates toward zero

```java
System.out.println(7 / 2);     // 3
System.out.println(-7 / 2);    // -3   (truncates toward zero, NOT floor)
System.out.println(7 / 2.0);   // 3.5  (one operand is double, so result is double)
System.out.println(1 / 2);     // 0
```

### 2.2 Division by zero

| Expression   | Result                       |
|--------------|------------------------------|
| `5 / 0`      | `ArithmeticException` (int)  |
| `5 % 0`      | `ArithmeticException` (int)  |
| `5.0 / 0`    | `Infinity`                   |
| `-5.0 / 0`   | `-Infinity`                  |
| `0.0 / 0`    | `NaN`                        |
| `5.0 % 0`    | `NaN`                        |

### 2.3 Remainder operator `%`

- Works on integers **and** floating-point numbers.
- The **sign of the result follows the dividend** (left operand).
- Identity: `a == (a / b) * b + (a % b)`

```java
System.out.println(7 % 3);      //  1
System.out.println(-7 % 3);     // -1
System.out.println(7 % -3);     //  1
System.out.println(-7 % -3);    // -1
System.out.println(5.5 % 2);    //  1.5

// True mathematical modulus (always non-negative for positive divisor)
System.out.println(Math.floorMod(-7, 3));   // 2
System.out.println(Math.floorDiv(-7, 2));   // -4
```

### 2.3.1 Common uses of `%`

```java
n % 2 == 0          // even check
n % 10              // last digit
n / 10              // remove last digit
index % length      // circular / wrap-around indexing
```

### 2.4 The `+` operator and Strings

If **either** operand is a `String`, `+` performs concatenation. Evaluation is **left to right**.

```java
System.out.println(1 + 2 + "3");        // "33"   (1+2=3, then "3" + "3")
System.out.println("1" + 2 + 3);        // "123"
System.out.println("a" + null);         // "anull"
System.out.println('a' + 'b');          // 195    (char + char = int: 97 + 98)
System.out.println("" + 'a' + 'b');     // "ab"
System.out.println('a' + 1);            // 98     (int)
System.out.println((char) ('a' + 1));   // 'b'
```

> **Tip:** Avoid `s += x` inside loops for large strings. Each `+=` creates a new `String`. Use `StringBuilder`.

---

## 3. Unary Operators

| Operator | Name               | Description                               |
|----------|--------------------|-------------------------------------------|
| `+x`     | Unary plus         | Numeric promotion (rarely used)           |
| `-x`     | Unary minus        | Negates the value                         |
| `++x`    | Pre-increment      | Increment, **then** use value             |
| `x++`    | Post-increment     | Use value, **then** increment             |
| `--x`    | Pre-decrement      | Decrement, **then** use value             |
| `x--`    | Post-decrement     | Use value, **then** decrement             |
| `!x`     | Logical NOT        | Inverts a `boolean`                       |
| `~x`     | Bitwise complement | Flips every bit (integral types only)     |

### 3.1 Pre vs Post increment

```java
int i = 5;
int a = i++;   // a = 5, i = 6   (use, then increment)
int b = ++i;   // i = 7, b = 7   (increment, then use)
```

### 3.2 Tricky evaluation

```java
int i = 5;
int a = i++ + ++i;
// i++  -> value 5, i becomes 6
// ++i  -> i becomes 7, value 7
// a = 5 + 7 = 12, i = 7

int j = 5;
j = j++;
System.out.println(j);   // 5   (j++ returns old value 5, which is then assigned back to j)

int x = 10;
x = x++ + x--;
// x++ -> 10 (x=11); x-- -> 11 (x=10); sum = 21; x = 21
System.out.println(x);   // 21
```

### 3.3 Increment works on other types too

```java
double d = 1.5;  d++;     // 2.5
char c = 'a';    c++;     // 'b'
Integer w = 10;  w++;     // 11 (auto-unboxing then boxing)
```

`++` / `--` include an **implicit cast**, so they work on `byte` and `short` without errors:

```java
byte b = 127;
b++;                      // -128 (overflow wraps around)
```

### 3.4 `~` (bitwise complement)

```java
~n == -(n + 1)
~5   // -6
~0   // -1
~-1  // 0
```

### 3.5 `!` (logical NOT)

Works **only** on `boolean`. Unlike C/C++, Java has no truthy or falsy values.

```java
if (1) { }        // compile error: int cannot be converted to boolean
```

---

## 4. Assignment Operators

### 4.1 Simple assignment `=`

`=` is **right-associative** and the assignment expression itself has a value.

```java
int a, b, c;
a = b = c = 10;      // c=10, then b=10, then a=10
```

### 4.2 Compound assignment

| Operator | Equivalent           |
|----------|----------------------|
| `a += b` | `a = (T)(a + b)`     |
| `a -= b` | `a = (T)(a - b)`     |
| `a *= b` | `a = (T)(a * b)`     |
| `a /= b` | `a = (T)(a / b)`     |
| `a %= b` | `a = (T)(a % b)`     |
| `a &= b` | `a = (T)(a & b)`     |
| `a \|= b`| `a = (T)(a \| b)`    |
| `a ^= b` | `a = (T)(a ^ b)`     |
| `a <<= b`| `a = (T)(a << b)`    |
| `a >>= b`| `a = (T)(a >> b)`    |
| `a >>>= b`| `a = (T)(a >>> b)`  |

where `T` is the type of the left-hand variable `a`.

**Key point:** compound assignment performs an **implicit narrowing cast**.

```java
byte b = 10;
b = b + 5;       // COMPILE ERROR: int cannot be converted to byte
b += 5;          // OK: b = (byte)(b + 5)

int i = 10;
i *= 2.5;        // i = (int)(10 * 2.5) = 25
i += 3.7;        // i = (int)(25 + 3.7) = 28   (fraction silently lost)

short s = 1;
s += 1;          // OK
s = s + 1;       // ERROR
```

### 4.3 Left operand is evaluated first

```java
int a = 5;
a += a++ * 2;
// a = a + (a++ * 2)  -> left 'a' is saved as 5 first
// a++ returns 5 (a becomes 6); 5 * 2 = 10; 5 + 10 = 15
System.out.println(a);   // 15   (the ++ side effect is overwritten)
```

### 4.4 Assignment inside a condition

```java
boolean flag = false;
if (flag = true) { }      // compiles! assigns true, then tests it. Classic bug: meant ==
```

---

## 5. Relational (Comparison) Operators

| Operator | Meaning                  |
|----------|--------------------------|
| `==`     | Equal to                 |
| `!=`     | Not equal to             |
| `>`      | Greater than             |
| `<`      | Less than                |
| `>=`     | Greater than or equal to |
| `<=`     | Less than or equal to    |

- Result is always a `boolean`.
- `>`, `<`, `>=`, `<=` work only on **numeric** types (including `char`), not on `boolean` or objects.
- `==` and `!=` work on all types.

### 5.1 `==` on primitives vs references

| Operand type | `==` compares            |
|--------------|--------------------------|
| Primitive    | Actual **values**        |
| Reference    | **Memory addresses** (identity) |

```java
String a = "hi";
String b = "hi";
String c = new String("hi");

a == b;               // true   (both point to the same interned literal)
a == c;               // false  (c is a new object on the heap)
a.equals(c);          // true   (content comparison)
c.intern() == a;      // true
```

### 5.2 Integer cache trap

`Integer.valueOf()` caches objects for values **-128 to 127**.

```java
Integer x = 127, y = 127;
System.out.println(x == y);        // true   (cached)

Integer p = 128, q = 128;
System.out.println(p == q);        // false  (different objects)
System.out.println(p.equals(q));   // true
```

Rule: always compare wrapper objects with `.equals()`.

### 5.3 Floating-point comparison

```java
System.out.println(0.1 + 0.2);            // 0.30000000000000004
System.out.println(0.1 + 0.2 == 0.3);     // false

// Compare with a tolerance
Math.abs((0.1 + 0.2) - 0.3) < 1e-9;       // true
// For money, use BigDecimal, never double
```

Special cases:

```java
Double.NaN == Double.NaN      // false  (NaN is not equal to anything, even itself)
Double.NaN != Double.NaN      // true
Double.isNaN(x)               // correct way to check for NaN
0.0 == -0.0                   // true
Double.valueOf(0.0).equals(-0.0)  // false
```

### 5.4 `==` with mixed types

```java
'a' == 97        // true   (char is promoted to int)
10 == 10.0       // true   (int promoted to double)
```

---

## 6. Logical Operators

| Operator | Name        | Short-circuit? | Works on              |
|----------|-------------|----------------|-----------------------|
| `&&`     | Logical AND | Yes            | `boolean` only        |
| `\|\|`   | Logical OR  | Yes            | `boolean` only        |
| `!`      | Logical NOT | n/a            | `boolean` only        |
| `&`      | AND         | **No**         | `boolean` and integers|
| `\|`     | OR          | **No**         | `boolean` and integers|
| `^`      | XOR         | **No**         | `boolean` and integers|

### 6.1 Truth table

| A     | B     | A && B | A \|\| B | A ^ B | !A    |
|-------|-------|--------|----------|-------|-------|
| true  | true  | true   | true     | false | false |
| true  | false | false  | true     | true  | false |
| false | true  | false  | true     | true  | true  |
| false | false | false  | false    | false | true  |

### 6.2 Short-circuit evaluation

- `a && b`: if `a` is `false`, `b` is **not evaluated**.
- `a || b`: if `a` is `true`, `b` is **not evaluated**.

```java
String s = null;

if (s != null && s.length() > 0) { ... }   // safe: length() is never called when s is null
if (s != null & s.length() > 0) { ... }    // NullPointerException: both sides always evaluated
```

```java
int x = 0;
if (x != 0 && 10 / x > 1) { }    // safe
if (x != 0 &  10 / x > 1) { }    // ArithmeticException
```

Side effects are skipped too:

```java
int a = 5, b = 5;
if (a > 10 && ++b > 0) { }
System.out.println(b);    // 5   (++b never executed)

if (a > 10 & ++b > 0) { }
System.out.println(b);    // 6   (++b executed)
```

### 6.3 Use `&&`/`||` for conditions, `&`/`|`/`^` for bits or when you need both sides evaluated

`^` on booleans is true when the operands **differ**.

---

## 7. Bitwise Operators

Operate on the **binary representation** of integral types (`byte`, `short`, `char`, `int`, `long`). Operands smaller than `int` are promoted to `int` first.

| Operator | Name | Rule                                   |
|----------|------|----------------------------------------|
| `&`      | AND  | 1 only if **both** bits are 1          |
| `\|`     | OR   | 1 if **at least one** bit is 1         |
| `^`      | XOR  | 1 if bits are **different**            |
| `~`      | NOT  | Flips each bit                         |

```
  5 = 0101
  3 = 0011
-------------
5 & 3 = 0001 = 1
5 | 3 = 0111 = 7
5 ^ 3 = 0110 = 6
   ~5 = ...11111010 = -6
```

### 7.1 Two's complement (how negatives are stored)

- The leftmost bit is the **sign bit**.
- `-n = ~n + 1`
- `-1` is all ones (`0xFFFFFFFF` for int).
- Ranges: `byte` -128..127, `short` -32768..32767, `int` -2^31..2^31-1, `long` -2^63..2^63-1.

### 7.2 Handy bit tricks

```java
// Check even/odd
(n & 1) == 0            // even

// Power of two (n > 0)
(n & (n - 1)) == 0

// Get lowest set bit
n & -n

// Clear lowest set bit
n & (n - 1)

// Count set bits
Integer.bitCount(n);

// Check k-th bit (0-indexed from right)
((n >> k) & 1) == 1

// Set k-th bit
n |= (1 << k);

// Clear k-th bit
n &= ~(1 << k);

// Toggle k-th bit
n ^= (1 << k);

// Swap two ints without a temp variable
a ^= b;
b ^= a;
a ^= b;

// Find the single non-duplicated number in an array (others appear twice)
int single = 0;
for (int v : arr) single ^= v;      // x ^ x = 0, x ^ 0 = x
```

XOR properties: `a ^ a = 0`, `a ^ 0 = a`, commutative, associative.

### 7.3 Bitmask example (subsets)

```java
int n = 3;
for (int mask = 0; mask < (1 << n); mask++) {     // 2^n subsets
    for (int i = 0; i < n; i++) {
        if ((mask & (1 << i)) != 0) {
            // element i is in this subset
        }
    }
}
```

---

## 8. Shift Operators

| Operator | Name                    | Fills vacated bits with |
|----------|-------------------------|--------------------------|
| `<<`     | Left shift              | `0`                      |
| `>>`     | Signed right shift      | **sign bit** (keeps sign)|
| `>>>`    | Unsigned right shift    | `0`                      |

```java
System.out.println(1 << 3);          // 8
System.out.println(5 << 2);          // 20     (5 * 2^2)
System.out.println(20 >> 2);         // 5      (20 / 2^2)
System.out.println(-16 >> 2);        // -4     (sign preserved)
System.out.println(-16 >>> 28);      // 15     (0xFFFFFFF0 >>> 28 = 0xF)
System.out.println(-1 >>> 1);        // 2147483647  (Integer.MAX_VALUE)
System.out.println(1 << 31);         // -2147483648 (sets the sign bit)
```

### 8.1 Rules

- `x << n` is equivalent to `x * 2^n` (as long as no overflow occurs).
- `x >> n` is equivalent to `floor(x / 2^n)`. This **differs from `/` for negatives**:

```java
-7 / 2     // -3   (truncates toward zero)
-7 >> 1    // -4   (floors)
```

- **Shift distance is masked**: for `int` only the low 5 bits (`n & 31`) are used; for `long` the low 6 bits (`n & 63`).

```java
System.out.println(1 << 32);      // 1   (32 & 31 = 0, so no shift)
System.out.println(1 << 33);      // 2   (33 & 31 = 1)
System.out.println(1L << 32);     // 4294967296
```

- `>>>` on `byte`/`short` gotcha (value is promoted to `int` first, then cast back):

```java
byte b = -1;             // 0xFF as byte, 0xFFFFFFFF as int
b >>>= 1;                // (byte)(0xFFFFFFFF >>> 1) = (byte)0x7FFFFFFF = -1
System.out.println(b);   // -1   (NOT 127)
```

- There is **no** `<<<` operator (it would be identical to `<<`).

---

## 9. Ternary Operator

**Syntax:** `condition ? valueIfTrue : valueIfFalse`

The only operator in Java with three operands. It is an **expression** (yields a value), unlike `if-else`, which is a statement.

```java
int max = (a > b) ? a : b;
String type = (n % 2 == 0) ? "even" : "odd";
```

### 9.1 Nesting (right-associative)

```java
String grade = marks >= 90 ? "A"
             : marks >= 75 ? "B"
             : marks >= 60 ? "C"
             : "F";
```

Keep nesting shallow. Prefer `if/else` when it hurts readability.

### 9.2 Type promotion gotchas

```java
System.out.println(true ? 1 : 2.0);     // 1.0   (int promoted to double)
System.out.println(true ? 'a' : 0);     // a     (0 is a constant that fits in char, result is char)

Object o = true ? Integer.valueOf(1) : Double.valueOf(2);
System.out.println(o);                   // 1.0   (unboxed and promoted to double)
```

### 9.3 NullPointerException via unboxing

```java
Integer i = null;
int r = flag ? i : 0;     // if flag is true -> NPE (i is unboxed to int)
```

---

## 10. instanceof Operator

Checks whether an object is an instance of a class, subclass, or interface.

```java
Object obj = "hello";
obj instanceof String;      // true
obj instanceof Object;      // true
obj instanceof Integer;     // false
null instanceof String;     // false   (always false for null)
```

### 10.1 Pattern matching (Java 16+)

Removes the need for an explicit cast.

```java
// Old style
if (obj instanceof String) {
    String s = (String) obj;
    System.out.println(s.length());
}

// New style
if (obj instanceof String s && s.length() > 3) {
    System.out.println(s.toUpperCase());
}
```

- The pattern variable `s` is in scope only where the match is guaranteed.
- The compiler rejects `instanceof` between unrelated class types (e.g., `Integer` vs `String`).

---

## 11. Type Promotion and Casting

### 11.1 Binary numeric promotion (for arithmetic operators)

Applied in this order:

1. If either operand is `double` -> both become `double`.
2. Else if either is `float` -> both become `float`.
3. Else if either is `long` -> both become `long`.
4. Otherwise -> **both become `int`** (this includes `byte`, `short`, `char`).

```java
byte a = 10, b = 20;
byte c = a + b;        // ERROR: a + b is int
byte d = (byte)(a + b); // OK
int e = a + b;         // OK

char ch = 'a';
ch = ch + 1;           // ERROR
ch += 1;               // OK -> 'b'
ch++;                  // OK -> 'c'
```

**Exception: compile-time constants.**

```java
byte b1 = 10 + 20;         // OK, constant expression 30 fits in byte
final byte x = 1;
byte y = x + 1;            // OK, x is a constant variable
```

### 11.2 Widening (implicit) vs narrowing (explicit)

```
byte -> short -> int -> long -> float -> double
          char -> int
```

- **Widening** (left to right): automatic, no data loss (except `long`->`float` precision).
- **Narrowing** (right to left): needs an explicit cast and may lose data.

```java
System.out.println((int) 3.99);        // 3     (truncates)
System.out.println((int) -3.99);       // -3
System.out.println((int) 1e20);        // 2147483647  (saturates at Integer.MAX_VALUE)
System.out.println((int) Double.NaN);  // 0
System.out.println((byte) 200);        // -56   (200 = 0xC8, top bit set)
System.out.println((byte) 128);        // -128
System.out.println((char) 65);         // 'A'
System.out.println((int) 'A');         // 65
```

### 11.3 Overflow happens *before* widening

```java
long big = 1000 * 1000 * 1000 * 10;    // int arithmetic overflows first!
System.out.println(big);               // 1410065408  (wrong)

long ok = 1000L * 1000 * 1000 * 10;    // 10000000000
```

### 11.4 Integer division before assigning to double

```java
double avg = 5 / 2;        // 2.0   (integer division happens first)
double avg2 = 5 / 2.0;     // 2.5
double avg3 = (double) 5 / 2;   // 2.5  (cast binds tighter than /)
```

---

## 12. Operator Precedence and Associativity

From **highest** to **lowest** precedence:

| Level | Operators                                   | Associativity  |
|-------|---------------------------------------------|----------------|
| 1     | `()`  `[]`  `.`  `expr++`  `expr--`         | Left to right  |
| 2     | `++expr`  `--expr`  `+expr`  `-expr`  `~`  `!`  `(type)` cast  `new` | **Right to left** |
| 3     | `*`  `/`  `%`                               | Left to right  |
| 4     | `+`  `-`                                    | Left to right  |
| 5     | `<<`  `>>`  `>>>`                           | Left to right  |
| 6     | `<`  `<=`  `>`  `>=`  `instanceof`          | Left to right  |
| 7     | `==`  `!=`                                  | Left to right  |
| 8     | `&`                                         | Left to right  |
| 9     | `^`                                         | Left to right  |
| 10    | `\|`                                        | Left to right  |
| 11    | `&&`                                        | Left to right  |
| 12    | `\|\|`                                      | Left to right  |
| 13    | `? :`                                       | **Right to left** |
| 14    | `=`  `+=`  `-=`  `*=`  `/=`  `%=`  `&=`  `^=`  `\|=`  `<<=`  `>>=`  `>>>=`  `->` | **Right to left** |

### 12.1 Mnemonic

Postfix -> Unary -> Multiplicative -> Additive -> Shift -> Relational -> Equality -> `&` -> `^` -> `|` -> `&&` -> `||` -> Ternary -> Assignment.

### 12.2 Examples

```java
int r = 10 + 5 * 2;            // 20 (not 30)
int s = 2 + 3 << 1;            // (2+3) << 1 = 10   (+ binds tighter than <<)
boolean t = 5 > 3 == true;     // (5 > 3) == true -> true
int u = 5 & 3 | 2 ^ 1;         // (5&3) | (2^1) = 1 | 3 = 3   (& > ^ > |)
```

### 12.3 Precedence trap with bitwise operators

`==` binds tighter than `&`, so parentheses are essential:

```java
if (x & 1 == 0) { }      // parsed as x & (1 == 0) -> int & boolean -> COMPILE ERROR
if ((x & 1) == 0) { }    // correct
```

### 12.4 Evaluation order vs precedence

- Precedence decides **grouping**; Java always evaluates operands **left to right**.

```java
int a = 1;
int b = a + a++ + ++a;
// a       -> 1
// a++     -> 1 (a becomes 2)
// ++a     -> 3 (a becomes 3)
// b = 1 + 1 + 3 = 5
```

- When in doubt, **use parentheses**. They cost nothing and remove ambiguity.

---

## 13. Overflow and Safe Arithmetic

Java integer arithmetic **wraps around silently** (no exception).

```java
int max = Integer.MAX_VALUE;               // 2147483647
System.out.println(max + 1);               // -2147483648 (Integer.MIN_VALUE)
System.out.println(Math.abs(Integer.MIN_VALUE));   // -2147483648 (still negative!)
System.out.println(-Integer.MIN_VALUE);    // -2147483648
```

### 13.1 Exact-arithmetic methods (throw `ArithmeticException` on overflow)

```java
Math.addExact(a, b);
Math.subtractExact(a, b);
Math.multiplyExact(a, b);
Math.incrementExact(a);
Math.negateExact(a);
Math.toIntExact(longValue);
```

### 13.2 Overflow-safe midpoint (classic binary search bug)

```java
int mid = (low + high) / 2;             // can overflow when low + high > Integer.MAX_VALUE
int mid = low + (high - low) / 2;       // safe
int mid = (low + high) >>> 1;           // also safe (unsigned shift)
```

### 13.3 Big numbers

Use `long` for larger ranges, and `BigInteger` / `BigDecimal` for arbitrary precision.

---

## 14. Common Pitfalls

| # | Pitfall | Fix |
|---|---------|-----|
| 1 | `==` on `String` or wrapper objects | Use `.equals()` |
| 2 | Integer division loses fractions (`5/2 = 2`) | Cast or use a `double` operand |
| 3 | `double` equality (`0.1 + 0.2 == 0.3` is false) | Compare with epsilon, or use `BigDecimal` |
| 4 | Using `&` instead of `&&` (no short-circuit, possible NPE) | Use `&&` / `||` for conditions |
| 5 | `if (x = 5)` / `if (flag = true)` | Use `==` |
| 6 | `i = i++` leaves `i` unchanged | Just write `i++` |
| 7 | `x & 1 == 0` precedence error | `(x & 1) == 0` |
| 8 | `byte b = b + 1` fails | Use `b += 1` or a cast |
| 9 | Overflow before widening to `long` | Use `1000L * ...` |
| 10 | `(low + high) / 2` overflow | `low + (high - low) / 2` |
| 11 | `-7 % 3 == -1` surprises people | Use `Math.floorMod` for non-negative results |
| 12 | Unboxing `null` in ternary | Check for `null` first |
| 13 | `Math.abs(Integer.MIN_VALUE)` is negative | Handle that edge case |
| 14 | `'a' + 'b'` is `195`, not `"ab"` | Add a `String` first: `"" + 'a' + 'b'` |
| 15 | `>>` on negatives differs from `/` | Know that `>>` floors, `/` truncates |

---

## 15. Interview-Style Output Questions

Try to predict each output before reading the answer.

**Q1**
```java
System.out.println(10 + 20 + "Java" + 10 + 20);
```
**Answer:** `30Java1020`. Numbers are added until the first `String` appears, and after that everything is concatenated.

---

**Q2**
```java
int x = 10;
System.out.println(x++ + ++x);
```
**Answer:** `22`. `x++` gives 10 (x=11), `++x` gives 12, so 10 + 12.

---

**Q3**
```java
System.out.println('a' + 'b' + "c");
```
**Answer:** `195c`. `'a' + 'b'` = 195 (int), then + `"c"`.

---

**Q4**
```java
System.out.println(1 / 2 + 1 / 2.0);
```
**Answer:** `0.5`. That is `0 + 0.5`.

---

**Q5**
```java
int a = 5;
a += a++ * 2;
System.out.println(a);
```
**Answer:** `15`. Expands to `5 + (5 * 2)`.

---

**Q6**
```java
Integer a = 128, b = 128;
System.out.println(a == b);
System.out.println(a.equals(b));
```
**Answer:** `false`, then `true`. 128 is outside the Integer cache range.

---

**Q7**
```java
System.out.println(-8 >> 1);
System.out.println(-8 >>> 29);
System.out.println(1 << 33);
```
**Answer:** `-4`, `7`, `2`.
(`-8 = 0xFFFFFFF8`; `>>> 29` leaves the top 3 bits `111` = 7; shift distance `33 & 31 = 1`.)

---

**Q8**
```java
System.out.println(5 & 3 | 2 ^ 1);
```
**Answer:** `3`. Order is `&` then `^` then `|`: `1 | 3 = 3`.

---

**Q9**
```java
byte b = (byte) 200;
System.out.println(b);
```
**Answer:** `-56`.

---

**Q10**
```java
int i = 0;
if (i != 0 && 10 / i > 1) System.out.println("A");
else System.out.println("B");
```
**Answer:** `B`. Short-circuit avoids the division by zero.

---

**Q11**
```java
System.out.println(true ? 1 : 2.0);
```
**Answer:** `1.0`. Binary numeric promotion applies in the ternary.

---

**Q12**
```java
System.out.println(Integer.MAX_VALUE + 1);
```
**Answer:** `-2147483648`. Silent overflow.

---

**Q13**
```java
int i = 5;
i = i++;
System.out.println(i);
```
**Answer:** `5`.

---

**Q14**
```java
System.out.println(0.1 + 0.2 == 0.3);
System.out.println(Double.NaN == Double.NaN);
```
**Answer:** `false`, `false`.

---

## 16. Quick Cheat Sheet

```text
ARITHMETIC     +  -  *  /  %                 int / int -> int, % sign follows dividend
UNARY          ++  --  +  -  !  ~            pre: change then use, post: use then change
ASSIGNMENT     =  op=                        op= includes an implicit cast
RELATIONAL     ==  !=  <  >  <=  >=          == on objects compares references
LOGICAL        &&  ||  !                     short-circuit
BITWISE        &  |  ^  ~                    work on bits (and on booleans without short-circuit)
SHIFT          <<  >>  >>>                   >> keeps sign, >>> fills with 0, distance masked (31/63)
TERNARY        cond ? a : b                  right-associative, numeric promotion applies
TYPE CHECK     instanceof                    null -> false, supports patterns in Java 16+
```

**Remember:**

1. Operands smaller than `int` are promoted to `int` in arithmetic.
2. Compound assignment hides a cast; `a = a + b` does not.
3. `&&` / `||` short-circuit; `&` / `|` do not.
4. Use `.equals()` for objects and an epsilon or `BigDecimal` for decimals.
5. Parenthesize bitwise expressions and anything you are unsure about.
6. Integer overflow is silent, so use `long`, `Math.*Exact`, or `BigInteger` when it matters.
