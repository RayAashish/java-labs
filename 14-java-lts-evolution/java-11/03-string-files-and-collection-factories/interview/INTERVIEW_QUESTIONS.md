# Interview Questions — `String`, `Files` & Collection Factories (Java 9–11)

## 🟢 Beginner

### Q1. What is the difference between `String.isEmpty()` and `String.isBlank()` (Java 11)?
- `isEmpty()` checks `length() == 0` (`"   ".isEmpty()` is `false`).
- `isBlank()` checks whether the string is empty OR contains only whitespace (`" \n\t ".isBlank()` is `true`).

### Q2. Why did `List.of()` replace `Collections.unmodifiableList(Arrays.asList(...))`?
It is concise, uses a compact internal layout (`ImmutableCollections$ListN`) instead of wrapping an `ArrayList` inside an unmodifiable view, rejects nulls at creation time, and avoids extra wrapper allocation.

---

## 🟡 Intermediate

### Q3. Explain the difference between `Arrays.asList("a", "b")` and `List.of("a", "b")`.
- `Arrays.asList` allows `set(index, val)` mutation (which passes through to the backing array!) and permits `null` elements.
- `List.of` is **deeply immutable structurally** (`set`, `add`, `remove` all throw `UnsupportedOperationException`), forbids `null` elements (`NullPointerException`), and skips copying if passed into `List.copyOf(...)`.

### Q4. How does `String.strip()` differ from `String.trim()`?
`trim()` uses legacy logic removing characters `<= '\u0020'` (ASCII control & space), failing on Unicode whitespaces such as `\u2005` or `\u3000`. `strip()` uses `Character.isWhitespace(int codePoint)` to handle Unicode spaces cleanly.

---

## 🔴 Senior

### Q5. How do Compact Strings (Java 9 JEP 254) reduce heap consumption and what happens if a string mixes ASCII and Emoji?
`String` stores characters in `byte[] value` plus a 1-byte `coder` flag (`LATIN1` or `UTF16`). Pure Latin-1 text takes 1 byte/character instead of 2. The moment ANY character requires UTF-16 (`"hi 🚀"`), the entire `String` stores `coder = UTF16` (2 bytes per `char` for every character in that string).
