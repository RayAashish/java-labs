# 00. Choosing the Right Java Collection (Noob to Practitioner Guide)

---

## 🟢 Layer 1 — The 30-Second Collection Decision Flowchart

Ask yourself **one question**: *"What shape is my data?"*

1. **Ordered list of items (duplicates allowed):** -> **`ArrayList`** (default 99% choice)
2. **Unique items only (no duplicates):**
   - Don't care about order -> **`HashSet`** ($O(1)$ lookup)
   - Keep insertion order -> **`LinkedHashSet`**
   - Always sorted -> **`TreeSet`** ($O(\log N)$)
3. **Key -> Value lookup dictionary:**
   - Fast lookup, any order -> **`HashMap`** ($O(1)$)
   - Keep insertion or recent-access order -> **`LinkedHashMap`**
   - Always sorted by key -> **`TreeMap`** ($O(\log N)$)
   - Shared across multiple threads -> **`ConcurrentHashMap`**
4. **Queue (FIFO) or Stack (LIFO):**
   - Standard FIFO queue / LIFO stack -> **`ArrayDeque`**
   - Always pop smallest/largest element first -> **`PriorityQueue`**

---

## 🟡 Layer 2 — Quick Big-O & Null Rules Cheat Sheet

| Class | Lookup / Get | Insert / Remove | Null Allowed? | Thread-Safe? |
|---|---|---|---|---|
| `ArrayList` | $O(1)$ by index | $O(1)$ amortized end / $O(N)$ mid | Yes | No |
| `LinkedList` | $O(N)$ by index | $O(1)$ ends | Yes | No |
| `ArrayDeque` | $O(1)$ ends | $O(1)$ ends | **NO (NPE)** | No |
| `PriorityQueue` | $O(1)$ peek | $O(\log N)$ offer/poll | **NO (NPE)** | No |
| `HashSet` / `HashMap` | $O(1)$ avg | $O(1)$ avg | 1 null key | No |
| `TreeSet` / `TreeMap` | $O(\log N)$ | $O(\log N)$ | **NO null key** | No |
| `ConcurrentHashMap` | $O(1)$ avg | $O(1)$ avg | **NO nulls** | **YES** |
