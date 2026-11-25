# 06. Parallel Streams — Internals, Performance & Production Traps

---

## 🟢 Layer 1 — Noob Track: One Method Call, Many Traps

Adding `.parallelStream()` or `.parallel()` splits stream work across multiple CPU cores using `ForkJoinPool.commonPool()`. **However, parallel streams are NOT a free "make code faster" button** — on small lists or wrong data structures, they are often **slower and thread-unsafe**!

---

## 🟡 Layer 2 — The 4 Rules for Safe Parallel Streams

1. **NEVER mutate shared state (Race Condition!):**
   ```java
   // ❌ BAD: ArrayList is NOT thread-safe -> data loss / ArrayIndexOutOfBoundsException!
   List<Integer> out = new ArrayList<>();
   IntStream.range(0, 10_000).parallel().forEach(out::add);

   // ✅ GOOD: Thread-safe terminal reduction managed by Stream framework!
   List<Integer> safe = IntStream.range(0, 10_000).parallel().boxed().toList();
   ```
2. **NEVER execute blocking I/O inside `parallelStream()`:**
   All parallel streams across the **entire JVM** share one global **`ForkJoinPool.commonPool()`** (`CPU cores - 1` threads). Blocking on HTTP/DB calls inside `parallelStream()` starves every unrelated `parallelStream()` and `CompletableFuture` in your microservice! *(Use Virtual Threads or a dedicated `ExecutorService` for I/O!)*
3. **Ensure the source splits cleanly (`Spliterator`):**
   - **Splits fast ($O(1)$):** `ArrayList`, arrays, `IntStream.range`, `ConcurrentHashMap`.
   - **Splits terribly ($O(N)$):** `LinkedList`, `Stream.iterate`, `BufferedReader.lines()`.
4. **Apply the NQ Rule (Data size $N \times$ Compute cost per item $Q$):**
   Only consider parallelizing when $N \times Q \ge 10{,}000+$ CPU cycles (e.g. heavy cryptography, numerical math, image pixel transforms — NOT summing 500 ints!).

---

## 🔴 Layer 3 — Senior Track: Custom `ForkJoinPool` Workaround & Ordering Costs

- **Avoiding `commonPool` Poisoning:** If you MUST parallelize a pure-CPU task with isolated thread boundaries, submit the parallel stream task inside a custom `ForkJoinPool`:
  ```java
  try (ForkJoinPool customPool = new ForkJoinPool(4)) {
      long result = customPool.submit(() ->
          bigList.parallelStream().mapToLong(HeavyMath::compute).sum()
      ).get();
  }
  ```
- **Encounter Order Overhead (`limit` / `findFirst` / `forEachOrdered`):** Calling `.unordered()` before `.distinct()` or `.limit()` on parallel streams relaxes cross-thread sequence synchronization and dramatically boosts multi-core speedup.
