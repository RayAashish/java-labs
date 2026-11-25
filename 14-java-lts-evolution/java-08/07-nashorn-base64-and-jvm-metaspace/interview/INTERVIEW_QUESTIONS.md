# Interview Questions — Base64, Metaspace, CHM V8 & Annotations

## 🟢 Beginner

### Q1. Why did Java 8 replace `PermGen` with `Metaspace`?
PermGen lived inside a contiguous fixed-size Java heap region that frequently threw `OutOfMemoryError: PermGen space` in classloader-heavy apps (Spring, Tomcat hot-redeploy). **Metaspace** moves class metadata into **native OS memory** so it auto-expands dynamically.

### Q2. When should you use `Base64.getUrlEncoder()` instead of `Base64.getEncoder()`?
Whenever the encoded token goes inside a **URL query string, path segment, or filename** (e.g., JWT payloads). Basic Base64 outputs `+`, `/`, and `=`, which break URLs unless percent-encoded; URL-safe Base64 substitutes `-` and `_` cleanly.

---

## 🟡 Intermediate

### Q3. Why does `HashMap` treeify buckets only at threshold 8 AND capacity >= 64?
- Under a good hash function (Poisson distribution $\lambda = 0.5$), the probability of 8 keys colliding into one bucket is less than 1 in 10 million — so normal maps remain fast compact linked nodes.
- Under hash collisions, if the table is still small (`< 64`), **doubling/resizing** spreads keys out faster than building trees! Once capacity >= 64, treeification guarantees worst-case $O(\log N)$ lookup instead of $O(N)$. (And it **untreeifies** back to a linked list if shrinkage drops bin count to `UNTREEIFY_THRESHOLD = 6`.)

### Q4. Why is `LongAdder` faster than `AtomicLong` under heavy write contention?
`AtomicLong` forces all threads to contend on a single CPU cache line via CAS, failing and spinning repeatedly. `LongAdder` uses **Lock/Cell striping** (`Cell[]`), spreading updates across cache-padded cells per thread and aggregating only when `.sum()` is called.

---

## 🔴 Senior

### Q5. How does `ConcurrentHashMap` in Java 8 differ structurally from Java 7, and what is the `computeIfAbsent` recursive pitfall?
Java 7 used 16 fixed `Segment` locks (`ReentrantLock`). Java 8 uses a flat CAS + per-bin `synchronized (firstNode)` locking + Red-Black trees (`TreeBin`), drastically reducing lock contention. However, because `computeIfAbsent` holds the **bin lock** while executing the mapping lambda, recursively updating another key that hashes to the same table or resizing inside the callback can throw `IllegalStateException: Recursive update` or deadlock!
