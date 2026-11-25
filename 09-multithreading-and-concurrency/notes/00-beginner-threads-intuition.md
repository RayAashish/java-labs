# 00. Concurrency for Beginners — Threads, Race Conditions & Fixes

---

## 🟢 Layer 1 — Noob Intuition: Chefs in a Shared Kitchen

Think of a **Process** as a restaurant and **Threads** as chefs working inside that kitchen:
- Every chef has their own notepad (**Thread Stack** — local variables).
- All chefs share the **same fridge and whiteboard** (**Heap Memory** — shared objects).

### Why Bugs Happen: The Race Condition
Suppose two chefs read `"Eggs left: 10"` at the exact same instant, both mentally add `1`, and both write back `11`. One egg update was **lost**! That is a **Race Condition**.

`count++` looks like 1 line of Java, but to the CPU it is **3 separate steps**:
1. **Read** `count` from memory.
2. **Add** `1`.
3. **Write** the new value back.

### The 3 Simple Fixes (From Simplest to Fastest)
1. **`synchronized` (One chef at a time):** Lock the whiteboard so only 1 thread updates `count` at once.
2. **`AtomicInteger` (Hardware single-step swap):** Uses CPU Compare-And-Swap (`CAS`) without putting threads to sleep.
3. **Don't share mutable state at all:** Use immutable objects (`record`), local variables, or `ExecutorService` returning `Future` results!
