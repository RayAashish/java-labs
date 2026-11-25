# Interview Questions — Text Blocks & Java 12–17 Stream Gems

## 🟢 Beginner

### Q1. How does Java decide how much indentation to remove from a Text Block (`"""`)?
`javac` finds the minimum indentation across all non-blank content lines AND the closing `"""` line (incidental whitespace) and strips that prefix at compile time! Moving closing `"""` further left shifts the entire text block right by the extra spaces.

---

## 🟡 Intermediate

### Q2. What is the difference between `stream.toList()` (Java 16) and `Collectors.toUnmodifiableList()` (Java 10) vs `Collectors.toList()` (Java 8)?
| Method | Modifiable? | Allows `null` elements? |
|---|---|---|
| `Collectors.toList()` | ✅ Mutable (`ArrayList`) | ✅ Yes |
| `Collectors.toUnmodifiableList()` | ❌ Throws `UnsupportedOperationException` | ❌ Throws `NullPointerException` |
| `Stream.toList()` *(Java 16)* | ❌ Throws `UnsupportedOperationException` | ✅ **Yes!** |

### Q3. How does `Collectors.teeing()` work?
It feeds each incoming stream element simultaneously to two independent downstream collectors and merges both final results with a `BiFunction` — computing metrics like `(min, max)` or `(sum, count)` in **one single pass** without scanning the collection twice!

---

## 🔴 Senior

### Q4. When does `Stream.mapMulti()` outperform `Stream.flatMap()`?
When flattening elements that yield **0 or 1** (or imperatively unpacked nested collections) across millions of items! `flatMap` must allocate a new `Stream` wrapper object per input item (`Stream.of(...)` or `Stream.empty()`), whereas `mapMulti` invokes `downstream.accept(...)` directly with zero intermediate `Stream` allocations.
