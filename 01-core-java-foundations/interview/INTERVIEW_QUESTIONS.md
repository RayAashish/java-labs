# 01. Core Java Foundations — Interview Questions & Answers

> Structured across **Beginner**, **Intermediate**, **Senior / JVM**, and **Tricky Output Puzzles**.

---

## 🟢 Beginner / Fresher Questions

### Q1. Why is Java platform-independent ("Write Once, Run Anywhere")?
`javac` compiles `.java` source into architecture-neutral **bytecode (`.class`)**, not native CPU instructions. Each OS (Linux, macOS, Windows) runs its own **JVM** implementation that translates bytecode into local machine code.

### Q2. What are the 8 primitive types and their sizes?
`byte` (1B), `short` (2B), `int` (4B), `long` (8B), `float` (4B), `double` (8B), `char` (2B unsigned UTF-16), and `boolean` (logically 1 bit; JVM stores 1 byte in `boolean[]` and 4 bytes as an `int` slot on the Operand Stack).

### Q3. What is the difference between local, instance, and static variables?
- **Local:** Declared inside methods/blocks; lives on the **Thread Stack**; has **no default value** (must initialize before reading).
- **Instance:** Declared in class body without `static`; lives inside the object on the **Heap**; auto-defaults (`0`, `false`, `null`).
- **Static:** Declared with `static`; stored once per class in **Metaspace/Class object**; shared by all instances.

### Q4. Why doesn't `int x; System.out.println(x);` compile, while an uninitialized `int` field works?
Class fields are zeroed by the JVM during object/class allocation. Local stack frame slots are NOT automatically cleared for speed, so `javac` enforces **definite assignment** to prevent reading garbage stack bytes.

### Q5. What is the difference between `=` and `==`, and `==` vs `.equals()`?
`=` assigns a value. `==` compares raw values for primitives and **memory addresses** for reference variables. `.equals()` compares logical content (assuming the class overrides `Object.equals()`, like `String` or `Integer`).

### Q6. Explain `++i` (pre-increment) vs `i++` (post-increment).
`++i` increments `i` first and evaluates to the **new** value. `i++` evaluates to the **old** value and increments `i` afterward.

### Q7. How does Java handle `var` (Java 10+)? Does it make Java dynamically typed?
No. `var` is strictly **compile-time local type inference**. `javac` infers the exact static type from the right-hand initializer and emits identical bytecode. It cannot be used for fields, method parameters, or uninitialized variables.

---

## 🟡 Intermediate Questions

### Q8. Explain the `Integer` cache (`-128` to `127`) trap.
`Integer.valueOf(int)` (used by autoboxing) caches instances in `IntegerCache` for values **`-128` through `127`** (configurable upward via `-XX:AutoBoxCacheMax`). Thus `Integer a = 127, b = 127; a == b` is `true`, whereas `128 == 128` allocates two distinct Heap objects and evaluates to `false`! Always compare wrapper objects with `.equals()`.

### Q9. Why does `byte b = 10; b = b + 5;` fail to compile, while `b += 5;` compiles?
In binary arithmetic (`b + 5`), operands smaller than `int` are promoted to `int`, so `b + 5` has type `int`, which cannot fit into `byte` without a cast. Compound assignment (`b += 5`) automatically inserts an implicit narrowing cast: `b = (byte)(b + 5)`.

### Q10. Explain short-circuit (`&&`, `||`) vs bitwise/logical (`&`, `|`) operators on booleans.
`&&` stops evaluating if the left operand is `false`; `||` stops if left is `true` — essential for null-guards like `if (s != null && !s.isEmpty())`. Single `&` and `|` always evaluate **both** sides.

### Q11. What is the difference between `>>` (signed) and `>>>` (unsigned) right shift?
`>>` preserves the sign bit (fills left vacated bits with `1` for negatives, `0` for positives). `>>>` always shifts in **`0`** from the left regardless of sign (used in `HashMap` hash perturbation `h ^ (h >>> 16)` and overflow-safe binary search midpoint `(low + high) >>> 1`).

### Q12. Why is `Arrays.copyOf` / `.clone()` dangerous on 2D arrays?
A 2D array `int[][]` is an array of references to inner `int[]` arrays. `clone()` and `Arrays.copyOf()` perform a **shallow copy** — copying the outer row pointers, so mutating `copy[0][0]` mutates `original[0][0]`! Deep copy requires cloning each inner row individually.

### Q13. What happens when `low + high` overflows in `(low + high) / 2`?
Integer arithmetic wraps around silently (`Integer.MAX_VALUE + 1 == Integer.MIN_VALUE`), producing a negative midpoint index and throwing `ArrayIndexOutOfBoundsException`. Fix with `low + (high - low) / 2` or `(low + high) >>> 1`.

### Q14. What are the rules for Varargs (`String... args`)?
Varargs compiles to an array (`String[]`), must be the **last** parameter in the method signature, and at most **one** varargs parameter is allowed per method. Fixed-arity overloads always beat varargs in method resolution.

### Q15. How did Java 14+ `switch` expressions improve over classic `switch`?
Arrow syntax (`case 1 -> ...`) eliminates accidental fall-through (no `break` needed), can return a value directly (with `yield` inside multi-line blocks), and enables compile-time exhaustiveness checking on enums and sealed types.

---

## 🔴 Senior / JVM Architecture Questions

### Q16. How much memory does an `int[10]` occupy vs an `Integer[10]` on a 64-bit JVM with Compressed Oops?
- `int[10]`: 16-byte array header (8B Mark + 4B Klass + 4B Length) + $10 \times 4\text{B} = 56\text{ bytes}$ contiguous.
- `Integer[10]`: $16 + 10 \times 4\text{B} = 56\text{B}$ (rounded to 56B) for the reference array **PLUS** up to $10 \times 16\text{B} = 160\text{B}$ for the individual `Integer` objects on the Heap (if outside `-128..127`) -> ~4x more memory and pointer-chasing cache misses!

### Q17. Why are Java arrays covariant (`String[]` IS-A `Object[]`), and what bug does that create?
Pre-Java 5 needed `Arrays.sort(Object[])` to accept `String[]`. The flaw is that `Object[] arr = new String[2]; arr[0] = 42;` compiles cleanly and crashes at runtime with **`ArrayStoreException`**! Generics fixed this by being **invariant** (`List<String>` is NOT a `List<Object>`).

### Q18. How does `javac` optimize `switch` into bytecode?
For dense integer cases (`1, 2, 3, 4`), `javac` emits **`tableswitch`** — an $O(1)$ jump offset table. For sparse cases (`1, 500, 10000`), it emits **`lookupswitch`** — sorted keys searched via $O(\log N)$ binary search. String switches switch on `hashCode()` first and guard collisions with `.equals()`.

### Q19. Why does `Math.abs(Integer.MIN_VALUE)` return a negative number?
Two's complement asymmetric range: `Integer.MIN_VALUE` is $-2^{31}$ (`-2147483648`), while `Integer.MAX_VALUE` is $2^{31} - 1$ (`2147483647`). Negating `-2147483648` overflows back to `-2147483648`! Use `Math.absExact()` (Java 15+) if overflow must throw.

### Q20. What is the difference between `System.out.println` and `Math.addExact` regarding overflow?
Standard Java integer operators wrap silently in modular $2^{32}$/$2^{64}$ arithmetic. `Math.addExact`, `subtractExact`, and `multiplyExact` check hardware overflow flags and throw `ArithmeticException` on overflow.

---

## 🧩 Tricky Output Puzzles

1. `int i = 5; i = i++; System.out.println(i);` -> **Prints `5`** (`i++` evaluates old `5`, increments `i` to `6`, then `=` overwrites `i` back to `5`).
2. `System.out.println('a' + 'b');` -> **Prints `195`** (`char` promotes to `int`: `97 + 98`).
3. `System.out.println(1 + 2 + "3" + 4 + 5);` -> **Prints `"3345"`** (left-to-right: `3 + "3"` becomes `"33"`, then concatenates `"4"` and `"5"`).
4. `System.out.println(-7 % 3);` -> **Prints `-1`** (remainder keeps dividend's sign; use `Math.floorMod(-7, 3)` for `2`).
5. `System.out.println(1 << 32);` -> **Prints `1`** (for `int`, shift distance is masked by `& 31`, so `32 & 31 == 0`).
6. `System.out.println(Double.NaN == Double.NaN);` -> **Prints `false`** (always use `Double.isNaN(x)`).
7. `byte a = 10, b = 20; final byte x = 10, y = 20; byte c = x + y;` -> **Compiles!** Compile-time constant expressions within `byte` range fit implicitly (`a + b` would fail).
