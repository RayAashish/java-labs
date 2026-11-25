# Java 11 — `String`, `Files`, Compact Strings & Collection Factories

## ⚡ 2-Minute Revision
- **`String` Additions (Java 11):** `isBlank()`, `strip()` / `stripLeading()` / `stripTrailing()` (Unicode-aware!), `lines()` (lazy `Stream<String>`), `repeat(n)`.
- **`strip()` vs Legacy `trim()`:** `trim()` only strips ASCII chars `<= U+0020`; it **fails** on Unicode whitespace like `U+2005` (`\u2005`) or non-breaking space `\u00A0` (`trim()` removes `<=\u0020`, `strip()` uses `Character.isWhitespace(int)` — wait: `Character.isWhitespace` excludes `\u00A0` non-breaking space too while catching Unicode spaces like `\u2000..\u200A`!).
- **Compact Strings (JEP 254, Java 9):** `String` changed backing field from `char[]` (2 bytes/char) to `byte[] value` + `byte coder` (`LATIN1 = 0` or `UTF16 = 1`), halving memory for English/JSON strings!
- **`Files.readString(path)` & `Files.writeString(path, str)`:** 1-line UTF-8 file I/O (replacing 10 lines of `BufferedReader`/`Files.readAllBytes`).
- **Collection Factories (Java 9/10):** `List.of(...)`, `Set.of(...)`, `Map.of(...)`, `Map.ofEntries(...)`, `List.copyOf(...)` — **structurally immutable** (`UnsupportedOperationException` on add/set) and **strictly reject `null` elements/keys/values**! Also `Predicate.not(String::isBlank)` & `Collection.toArray(String[]::new)`.

## 📂 Contents
- [`notes/string-files-collections-deep-dive.md`](notes/string-files-collections-deep-dive.md)
- [`codes/lts/java11/stringsfiles/StringFilesAndCollectionsDemo.java`](codes/lts/java11/stringsfiles/StringFilesAndCollectionsDemo.java)
- [`interview/INTERVIEW_QUESTIONS.md`](interview/INTERVIEW_QUESTIONS.md)
