# Java 17 — Text Blocks (JEP 378), `.formatted()`, `teeing` & `Stream.toList()`

## ⚡ 2-Minute Revision
- **Text Blocks (JEP 378):** Multi-line string literals framed by **`"""`** (followed by mandatory newline!) eliminating SQL/JSON/HTML escape hell (`\"`, `\n +`).
- **Incidental Whitespace Rule:** The closing `"""` delimiter's column position controls how much leading indentation `javac` strips automatically at **compile time**!
- **Two Special Text-Block Escapes:**
  - `\` at end of line: **suppresses the newline** (joins lines cleanly without runtime concat).
  - `\s` at end of line: translates to an explicit space character, **protecting preceding trailing spaces** from automatic trimming!
- **`.formatted(args...)` (Java 15):** Fluent instance equivalent of `String.format(template, args...)` right on a Text Block!
- **Handy Java 12–16 Stream Gems Bundled in Java 17 Baseline:**
  - **`Collectors.teeing(c1, c2, merger)` (Java 12):** Compute 2 independent aggregations in a **single Stream pass**!
  - **`Stream.toList()` (Java 16):** Shorter unmodifiable list terminal op (NOTE difference from `Collectors.toUnmodifiableList()` on `null`!).
  - **`Stream.mapMulti()` (Java 16):** Zero-stream-allocation 1-to-Many flatMapping!

## 📂 Contents
- [`notes/text-blocks-and-stream-additions.md`](notes/text-blocks-and-stream-additions.md)
- [`codes/lts/java17/text/TextBlocksAndStreamAdditionsDemo.java`](codes/lts/java17/text/TextBlocksAndStreamAdditionsDemo.java)
- [`interview/INTERVIEW_QUESTIONS.md`](interview/INTERVIEW_QUESTIONS.md)
