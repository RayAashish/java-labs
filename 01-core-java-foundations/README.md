# 01. Core Java Foundations — Variables, Data Types, Operators, Control Flow & Arrays

> **Learning Method:** **3-Layer Dual-Track Spiral** — start at **🟢 Noob Track** for plain-English intuition, level up through **🟡 Practitioner Track** for edge cases, and lock in **🔴 Senior Track** for JVM Stack/Heap memory and bytecode behavior.

---

## ⚡ 2-Minute Rapid Revision Card

- **8 Primitives:** `byte`(1B), `short`(2B), `int`(4B), `long`(8B), `float`(4B), `double`(8B), `char`(2B unsigned UTF-16), `boolean`(JVM-dependent, 1B in arrays).
- **Local vs Field Defaults:** Fields (`instance` & `static`) auto-initialize (`0`, `false`, `null`); **local variables have NO default** (must initialize before read).
- **Integer Wrapper Cache:** `Integer.valueOf()` caches **`-128` to `127`** (`Byte`, `Short`, `Long` also cache `-128..127`; `Character` caches `0..127`). Never use `==` on wrappers — always use `.equals()`.
- **Numeric Promotion:** In arithmetic (`a + b`), operands smaller than `int` (`byte`, `short`, `char`) promote to `int`. Compound operators (`+=`, `++`) insert an **implicit narrowing cast**.
- **Arrays:** Objects on the Heap! Every array has a 16-byte header (`Mark Word 8B + Klass 4B + Length 4B`). `clone()` / `Arrays.copyOf()` creates a **shallow copy** (2D arrays share inner row references!).
- **Modern `switch` (Java 14+):** Use arrow syntax `case X -> ...` (no fall-through!) and `yield` for multi-line value blocks. `javac` compiles dense cases to $O(1)$ `tableswitch` and sparse cases to $O(\log N)$ `lookupswitch`.

---

## 🧭 Progressive Learning Path

### 🟢 Layer 1 — Noob Track ("First Principles & Intuition")
1. Read [00-noob-to-senior-roadmap.md](notes/00-noob-to-senior-roadmap.md) for the "Boxes vs Remote Controls" mental model of Primitives vs References.
2. Read [01-variables-and-scope.md](notes/01-variables-and-scope.md) and run `Variables.java`.
3. Read [02-datatypes-casting-and-wrappers.md](notes/02-datatypes-casting-and-wrappers.md) and run `DataTypes.java`.
4. Read [04-decision-making-and-switch.md](notes/04-decision-making-and-switch.md), [05-looping-constructs.md](notes/05-looping-constructs.md), [06-jump-statements.md](notes/06-jump-statements.md).

### 🟡 Layer 2 — Practitioner Track ("Edge Cases & Clean Idioms")
1. Master operator traps, short-circuiting (`&&` vs `&`), shift masks, and overflow (`Math.addExact`) in [03-operators-and-bitwise-mechanics.md](notes/03-operators-and-bitwise-mechanics.md).
2. Master 1D/2D/3D arrays, varargs, and shallow-vs-deep array copying in [07-arrays-memory-and-varargs.md](notes/07-arrays-memory-and-varargs.md).
3. Run `WrapperCachingAndEqualityDemo.java` and `ModernSwitchExpressionDemo.java`.

### 🔴 Layer 3 — Senior / JVM Track ("Under the Hood")
- How `javac` emits `tableswitch` vs `lookupswitch`.
- Why `0.1 + 0.2 != 0.3` in IEEE 754 floating-point and how `BigDecimal` solves it.
- Why `Integer.valueOf(127) == Integer.valueOf(127)` is `true` while `128 == 128` is `false` (JLS §5.1.7).
- Array object headers, CPU cache-line spatial locality for `int[]` vs `Integer[]`, and why Java has covariant arrays (`String[]` IS-A `Object[]` -> `ArrayStoreException`).

---

## 📂 Module Contents

| Folder | Contents |
|---|---|
| [`notes/`](notes/) | 8 progressive guides from variables & loops to JVM memory, IEEE-754 & bitwise math |
| [`codes/`](codes/) | Runnable demos for variables, datatypes, operators, loops, switches, arrays, and wrapper caching |
| [`interview/`](interview/INTERVIEW_QUESTIONS.md) | 30+ interview questions, tricky output puzzles, and senior explanations |

---

## 🚀 Compile & Run

```bash
# Compile everything in this module
javac -d out $(find 01-core-java-foundations/codes -name "*.java")

# Run key demos
java -cp out foundations.wrappers.WrapperCachingAndEqualityDemo
java -cp out foundations.control.decision.ModernSwitchExpressionDemo
java -cp out foundations.arrays.DeepAndShallowCopyGotcha
java -cp out foundations.operators.BitwiseOperators
```
