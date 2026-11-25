# Interview Questions — Unnamed Variables & Patterns (`_`)

## 🟢 Beginner

### Q1. Where can `_` be used since Java 22 / Java 25?
1. Ignored `catch (Exception _)` exceptions.
2. Unused lambda parameters (`(_, val) -> ...`).
3. Unused local side-effect assignments (`var _ = queue.poll()`).
4. `for (Item _ : items)` counting loops.
5. `try (var _ = acquireLock())` auto-close scopes.
6. Record pattern components (`case Point(int x, _) -> x`).

---

## 🟡 Intermediate

### Q2. Can two unnamed variables `_` coexist in the same method or lambda? Can you print `_`?
- **Multiple `_` in same scope:** ✅ **Yes!** Because `_` produces no identifier binding in scope (`( _, _ ) -> 42` or `try (var _ = a(); var _ = b())` compiles cleanly).
- **Printing `_`:** ❌ **Compile error!** You cannot read or reference `_` in any expression — that guarantees maintainers can't accidentally use an unused placeholder!

### Q3. Why can't you declare `void handle(int _)` on a method parameter?
Public/package method parameter names belong to an API's documented signature (`javadoc` and tooling). Only implementation-local declarations (`catch`, lambdas, local vars, patterns) permit `_`.

---

## 🔴 Senior

### Q4. What is the subtle difference between `case Point(int x, _)` and `case Point(int x, Object _)` vs `case Point(int x, String _)`?
If the second component's declared type in `Point` is `Object`, **`_`** (or `Object _`) matches *any* value including `null`, whereas **`String _`** still checks runtime subtype (`instanceof String`, rejecting `null` and `Integer`) even though the value isn't bound to a name!
