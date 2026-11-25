# 00. Core Java Foundations — From Zero to JVM Intuition

> **How to use this guide:** Read **Layer 1** if you are new to Java, **Layer 2** for real-world coding rules, and **Layer 3** to understand what the JVM actually executes in hardware & bytecode.

---

## 🟢 Layer 1 — The Noob Intuition ("Boxes vs. Remote Controls")

Imagine you are moving into an apartment:
1. **Primitive Types (`int`, `double`, `boolean`, `char`, etc.)** are **small sealed boxes** sitting right on your desk (**Thread Stack**). The value (`42`, `true`, `'A'`) lives **directly inside the box**. Copying the box creates a completely independent second copy.
2. **Reference Types (`String`, `int[]`, `Student`, etc.)** are **TV Remote Controls**. The remote control sits on your desk (**Stack**), while the actual TV (**Heap Object**) sits across the room. Copying the remote gives you a second remote pointing to the **exact same TV**!

```text
THREAD STACK (Fast, Private)              HEAP (Shared, Garbage-Collected)
┌──────────────────────────┐             ┌─────────────────────────────────┐
│ int score = 95;          │             │                                 │
│ [ 95 ]  <-- raw value    │             │                                 │
│                          │             │                                 │
│ int[] nums = 0x4A20; ────┼────────────►│ [ Header | len=3 | 10 | 20 | 30 ]
└──────────────────────────┘             └─────────────────────────────────┘
```

### 3 Beginner Traps to Dodge on Day 1
1. **Forgetting to initialize local variables:** Java refuses to compile `int x; System.out.println(x);` because local stack slots are NOT zeroed out for you.
2. **Integer division truncation:** `5 / 2` evaluates to `2`, NOT `2.5`! Make one side floating-point (`5 / 2.0`).
3. **Comparing Strings or Wrappers with `==`:** `==` checks whether two remotes point to the same TV, NOT whether the two TVs display the same text. Always use `.equals()`!

---

## 🟡 Layer 2 — The Practitioner Rules

### 1. The Narrowing vs. Widening Ladder
- **Widening (Safe, Implicit):** `byte -> short -> int -> long -> float -> double` (and `char -> int`).
- **Narrowing (Lossy, Explicit cast required):** `(byte) 130` silently wraps around in two's complement to `-126`!

### 2. Modern `var` (Java 10+)
- `var` is **compile-time local type inference**, NOT dynamic typing like JavaScript or Python.
- `var count = 10;` is strictly typed as `int` in bytecode — you cannot later write `count = "hello"`.
- Only valid for **local variables with immediate initializers** (never fields, parameters, or return types).

### 3. Overflow-Safe Arithmetic
Standard Java `+`, `-`, `*` wrap around silently (`Integer.MAX_VALUE + 1 == Integer.MIN_VALUE`).
In financial or size-critical code, use `Math.addExact(a, b)` / `Math.multiplyExact(a, b)` to throw `ArithmeticException` instead of silently corrupting numbers.

---

## 🔴 Layer 3 — The Senior / JVM Architect View

### 1. Stack Frame Anatomy
Every method invocation pushes a **Stack Frame** onto the thread's JVM stack containing:
- **Local Variable Array:** Slot 0 is `this` (for instance methods), followed by parameters and local variables (`long` and `double` occupy 2 consecutive 32-bit slots!).
- **Operand Stack:** Scratchpad where bytecode instructions (`iload`, `iadd`, `istore`) push and pop values.

### 2. Why `int[]` Crushes `Integer[]` in Performance
- `int[1_000_000]` allocates **one contiguous 4 MB block** (`16B header + 4M * 4B`). CPU prefetcher loads sequential cache lines cleanly.
- `Integer[1_000_000]` allocates **1M pointer slots + 1M separate 16-byte Heap objects** (~20 MB+ total), triggering pointer-chasing L1/L2 cache misses and heavy GC scanning!

### 3. Switch Bytecode Optimization
`javac` compiles dense `switch` statements (`case 1, 2, 3, 4`) into **`tableswitch`** ($O(1)$ jump offset table) and sparse switches (`case 10, 500, 9999`) into **`lookupswitch`** ($O(\log N)$ binary search over sorted keys).
