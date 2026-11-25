# Local-Variable Type Inference (`var` — JEP 286 & JEP 323) Deep Dive

---

## 🟢 Layer 1 — Noob Track: Static Inference, NOT Dynamic Typing!

In JavaScript, `var x = 10; x = "hello";` changes type dynamically at runtime.
In Java, **`var` is 100% statically typed at compile time**:
- `var name = "Alice";` tells `javac` *"look at the right side (`String`) and fill in `String name = "Alice";` automatically."*
- Trying `name = 42;` on the next line is a **compile error**!

---

## 🟡 Layer 2 — Practitioner Track: Valid Sites, Traps & Readability

### Where `var` IS Allowed vs NOT Allowed

| Allowed (`✅`) | Forbidden (`❌ Compile Error`) |
|---|---|
| Local variables with initializer: `var x = 10;` | Local without initializer: `var x;` or `var x = null;` |
| Enhanced `for` / classic `for`: `for (var item : list)` | Class / instance fields: `private var count = 0;` |
| `try`-with-resources: `try (var in = ...)` | Method parameters or return types |
| Java 11 lambda params (all or none): `(@NotNull var x, var y) -> ...` | Mixing `var` and inferred/explicit params: `(var x, String y) -> ...` ❌ |
| Non-denotable types (anonymous subclasses / intersections!) | Array initializers without `new`: `var arr = {1, 2, 3};` ❌ |

### Two Classic `var` Traps
1. **Diamond Operator Trap:**
   - **BAD:** `var list = new ArrayList<>();` -> infers **`ArrayList<Object>`**!
   - **GOOD:** `var list = new ArrayList<String>();`
2. **Readability Deception (Unclear Return Types):**
   - **BAD:** `var data = service.process();` (Reader has no clue what `data` is!)
   - **GOOD:** `var entries = Map.<String, Integer>of("a", 1);` or `var stream = Files.lines(path);` where the RHS makes the type self-evident!
3. **Byte Overflow Surprise:**
   - `byte b = 10;` narrows constant `10` to `byte`. With `var b = 10;`, `b` is inferred as **`int`** (32-bit), NOT `byte`!

---

## 🔴 Layer 3 — Senior / JVM Track

- `var` is a **reserved type name** (`context-sensitive`), NOT a hard keyword — existing legacy code naming a variable/method `var` (`int var = 5;`) still compiles cleanly!
- **Bytecode:** Comparing `javap -c` of `String s = "hi";` vs `var s = "hi";` reveals **zero difference** in JVM instructions or LocalVariableTable attributes — zero runtime penalty!
- **Secret Superpower — Non-Denotable Types:** Previously impossible to store directly in a named variable:
  `var duck = new Object() { void quack() { System.out.println("Quack"); } }; duck.quack();` works cleanly because `var` captures the compiler's internal anonymous subtype directly!
