# Java 8 — `Optional<T>` Class

## ⚡ 2-Minute Revision
- **Why:** Replace silent `NullPointerException` landmines and deeply nested `if (a != null && a.getB() != null)` checks with an explicit type-safe container signaling *"might be absent"*.
- **Factory Rule:** Use `Optional.ofNullable(x)` when `x` may be null; `Optional.of(x)` throws `NullPointerException` **immediately** if `x == null`.
- **Golden Trap:** `opt.orElse(expensiveCall())` executes `expensiveCall()` **EAGERLY even when present**! Always use `opt.orElseGet(() -> expensiveCall())` for lazy fallback!
- **Never Do:** Never call `.get()` blindly without checking, never store `Optional` in entity fields / DTOs / method arguments / Collections, and note `Optional` does **NOT** implement `Serializable`.

## 📂 Contents
- [`notes/optional-complete-guide.md`](notes/optional-complete-guide.md)
- [`codes/lts/java08/optional/OptionalCompleteGuideDemo.java`](codes/lts/java08/optional/OptionalCompleteGuideDemo.java)
- [`interview/INTERVIEW_QUESTIONS.md`](interview/INTERVIEW_QUESTIONS.md)
