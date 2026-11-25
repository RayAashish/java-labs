# Interview Questions — Java 8 Date & Time API (`java.time`)

## 🟢 Beginner

### Q1. Why did `java.time` replace `java.util.Date` and `java.util.Calendar`?
1. `Date` & `Calendar` were **mutable** (violating defensive design for DTOs).
2. `Date` had confusing 1900 year offsets and 0-indexed months (`0 = January`).
3. `SimpleDateFormat` was **not thread-safe** — sharing one instance across threads corrupted parsed dates silently!
4. `java.time` is **immutable, thread-safe, ISO-8601 standard**, with clear separation of machine (`Instant`) vs human calendar (`LocalDate`/`ZonedDateTime`).

---

## 🟡 Intermediate

### Q2. Compare `Instant`, `LocalDateTime`, `OffsetDateTime`, and `ZonedDateTime`.
- **`Instant`:** Exact nanosecond count on UTC timeline — no timezone rules. Ideal for event timestamps & database storage.
- **`LocalDateTime`:** Calendar + clock (`2026-11-25 14:00`) with **zero timezone meaning** — ambiguous across regions.
- **`OffsetDateTime`:** Date + time + numeric UTC offset (`+05:30`) — ideal for REST JSON payloads & SQL `TIMESTAMPTZ`.
- **`ZonedDateTime`:** Date + time + full `ZoneId` region (`America/New_York`) including historical and future Daylight Saving Time transition rules.

### Q3. What is the difference between `Period` and `Duration`?
- **`Period`:** Date-based amount (years, months, days) matching calendar conventions (respects 23-hour/25-hour DST days and varying month lengths).
- **`Duration`:** Exact time-based amount stored in seconds & nanoseconds (`Duration.ofDays(1)` is always strictly `86,400` seconds).

---

## 🔴 Senior

### Q4. How do you avoid storing ambiguous local times for a future international meeting?
Never store only a `LocalDateTime` (you lose timezone) OR only an `Instant` (if the host country abolishes/changes DST between now and next year, your meeting's UTC timestamp will shift by 1 hour relative to local wall time!). Store **both** the intended wall-clock `LocalDateTime` AND the `ZoneId` (e.g., `"Europe/London"`), computing the `Instant` dynamically.

### Q5. How should you make business logic calling `.now()` deterministically unit-testable?
Inject **`java.time.Clock`** into services (`Instant.now(clock)`, `LocalDate.now(clock)`). Pass `Clock.systemUTC()` in production and `Clock.fixed(...)` or `Clock.offset(...)` in tests!
