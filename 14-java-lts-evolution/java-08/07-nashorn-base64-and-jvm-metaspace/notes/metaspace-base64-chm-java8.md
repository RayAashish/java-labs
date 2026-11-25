# Metaspace, `Base64`, Concurrency V8 & Repeating Annotations

---

## 🟢 Layer 1 — Noob Track: Plain-English Overview

Alongside Streams & Lambdas, Java 8 fixed several everyday developer headaches:
1. **Standard `Base64` built in:** No more external libraries or private `sun.misc.BASE64Encoder` APIs just to encode JWT segments or tokens.
2. **Goodbye `PermGen` crashes:** Java 7 stores class metadata in fixed PermGen space (`-XX:PermSize`); Java 8 moves class metadata into dynamically growing native OS memory called **Metaspace**.
3. **Cleaner Annotations:** You can stack `@Schedule(cron="...")` twice directly instead of `@Schedules({@Schedule(...), @Schedule(...)})`.

---

## 🟡 Layer 2 — Practitioner Track: Key Improvements You Use Daily

### 1. `java.util.Base64` — The 3 Variants
| Encoder/Decoder | Characters Changed | Line Breaks? | Use Case |
|---|---|---|---|
| `Base64.getEncoder()` / `getDecoder()` | Uses `+` and `/` (pads `=`) | None | General payloads, Basic Auth header |
| `Base64.getUrlEncoder()` / `getUrlDecoder()` | Uses **`-` and `_`** (URL/filename safe!) | None | JWT tokens (use `.withoutPadding()`), OAuth query params |
| `Base64.getMimeEncoder()` / `getMimeDecoder()` | Basic alphabet | `\r\n` every **76 chars** | Email attachments (RFC 2045) |

### 2. `@Repeatable` Annotations (JEP 120)
Defining a container annotation `@Schedules` with `Schedule[] value()` and annotating `@Repeatable(Schedules.class)` on `@Schedule` lets developers repeat `@Schedule` cleanly while staying compatible with `clazz.getAnnotationsByType(Schedule.class)`.

### 3. Concurrency Additions: `LongAdder`, `StampedLock`, `ConcurrentHashMap.computeIfAbsent`
- **`LongAdder` vs `AtomicLong`:** Under 32+ threads furiously incrementing metrics counters, `AtomicLong` fails CAS repeatedly in a tight spin loop. `LongAdder` maintains a **striped `Cell[]` array** so threads update independent cells and `.sum()` totals them lazily!
- **`StampedLock`:** Adds **optimistic lock-free reads** (`tryOptimisticRead()` + `validate(stamp)`) outperforming `ReentrantReadWriteLock` on read-heavy workloads (but **NOT reentrant** — be careful!).

---

## 🔴 Layer 3 — Senior / JVM Track: Memory & HashMap/CHMInternals

### 1. Why `PermGen -> Metaspace` Still Needs `-XX:MaxMetaspaceSize`
In Docker/Kubernetes containers, unbounded Metaspace grows into native OS RAM until the kernel **OOM-kills (`SIGKILL` / exit 137)** the pod if dynamic proxy generation (`CGLIB`, ByteBuddy, Groovy scripts) leaks ClassLoaders! Always pair `-XX:MaxMetaspaceSize` with container memory headroom.

### 2. `HashMap` Treeification (Java 8)
If malicious hash collisions put 10,000 keys in 1 bucket, Java 7 `HashMap` degrades to **$O(N)$** linked-list lookup (HashDoS vulnerability). Java 8 splits/treeifies any bucket whose length hits **`TREEIFY_THRESHOLD = 8`** (ONLY once total table capacity reaches **`MIN_TREEIFY_CAPACITY = 64`** — otherwise it resizes first!), yielding **$O(\log N)$** worst-case lookup!

### 3. `ConcurrentHashMap` V8 Overhaul
Drops Java 7's 16 heavy `ReentrantLock` `Segment` objects -> uses a single flat **`Node[]` table** where empty bucket insertion is **lock-free CAS** and collision locking synchronizes on **only the head `Node` of that individual bucket**. Caution: never perform recursive mutations inside `chm.computeIfAbsent()` — in Java 8+ locking on the bucket head can deadlock or throw `IllegalStateException: Recursive update`!
