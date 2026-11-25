# Pattern Matching for `switch` (JEP 441) — Deep Dive

---

## 🟢 Layer 1 — Noob Track: From Ugly `if / else if` Cast Chains to Pattern `switch`

**Before Java 21 (`if/else` chain + boilerplate casts):**
```java
if (obj == null) return "null";
else if (obj instanceof Integer i) return "int " + i;
else if (obj instanceof String s && s.length() > 5) return "long string";
else if (obj instanceof String s) return "short string";
else return "other";
```

**Java 21 Pattern `switch`:**
```java
return switch (obj) {
    case null -> "null";
    case Integer i -> "int " + i;
    case String s when s.length() > 5 -> "long string";
    case String s -> "short string";
    default -> "other";
};
```

---

## 🟡 Layer 2 — Practitioner Track: Dominance, Exhaustiveness & Qualified Enums

### 1. Case Label Dominance Rules (Compile Errors!)
`javac` statically checks that earlier labels don't swallow later ones:
- **BAD:** Put `case String s -> ...` *above* `case String s when s.isBlank() -> ...` -> **COMPILE ERROR (`dominated`)** because unguarded `String s` matches every `String` first!
- **GOOD:** Always write **guarded `when` cases first**, unguarded subtype cases second, supertype / `default` last!
- Also note: **`default` does NOT match `null`!** Only an explicit `case null` matches `null` (`case null, default -> ...` catches both!).

### 2. Qualified `enum` Constants in Mixed Sealed Hierarchies
In Java 21, two different `enum`s implementing a `sealed interface Status permits HttpStatus, DbStatus` can appear together seamlessly inside the SAME `switch`:
```java
switch (status) {
    case HttpStatus.OK -> "200 OK";
    case HttpStatus.NOT_FOUND -> "404 Not Found";
    case DbStatus s -> "DB: " + s;
}
```

---

## 🔴 Layer 3 — Senior / JVM Track: `SwitchBootstraps.typeSwitch` & `MatchException`

1. **Bytecode (`invokedynamic` + Restartable Index):** For type switches, `javac` compiles the pattern `switch` into an **`invokedynamic`** call to `java.lang.runtime.SwitchBootstraps.typeSwitch(Lookup, String, MethodType, Object[])`, passing an array of pattern target types and an integer restart index `restartIdx`.
   - When a `when` guard evaluates to `false`, bytecode jumps cleanly back into the `tableswitch` passing `restartIdx = matchedCase + 1`, skipping already-tested cases!
2. **`java.lang.MatchException`:** Replacing arbitrary `IncompatibleClassChangeError` / `AssertionError`, Java 21 introduced `MatchException` thrown whenever exhaustiveness assumptions fail at runtime (for example, when an uninitialized/separately compiled subtype appears, or a `null` component leaks inside nested Record Pattern deconstruction!).
