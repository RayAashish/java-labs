# Java 8 — Date & Time API (JSR-310 / `java.time`)

## ⚡ 2-Minute Revision
- **Why Legacy Was Broken:** `java.util.Date` & `Calendar` were **mutable**, 0-indexed months (`Jan = 0`), years offset by 1900 (`1900 + 124`), and `SimpleDateFormat` was **not thread-safe** (corrupting dates silently under multi-threaded load!).
- **Core JSR-310 Principles:** **Immutable**, **thread-safe**, **ISO-8601**, **1-indexed months (`1..12` or `Month.JANUARY`)**, clean split between **Machine Time** and **Human Time**.
- **Which Type When:**
  - `Instant` — UTC epoch timestamp (logs, DB `TIMESTAMP`, API payloads)
  - `LocalDate` / `LocalTime` / `LocalDateTime` — Calendar/clock value WITHOUT timezone (birthdays, store opening hours)
  - `ZonedDateTime` — Exact human instant WITH DST rules (`America/New_York`, `Asia/Kolkata`)
  - `OffsetDateTime` — Fixed offset from UTC (`+05:30`), ideal for SQL `TIMESTAMP WITH TIME ZONE` & JSON
  - `Period` (`P1D`, calendar days — DST-aware) vs `Duration` (`PT24H`, exact elapsed seconds/nanos)
- **Testability:** Inject `java.time.Clock` (`Clock.fixed(...)`) instead of hardcoding `LocalDate.now()`!

## 📂 Contents
- [`notes/java-time-jsr310-deep-dive.md`](notes/java-time-jsr310-deep-dive.md)
- [`codes/lts/java08/datetime/DateAndTimeJsr310Demo.java`](codes/lts/java08/datetime/DateAndTimeJsr310Demo.java)
- [`interview/INTERVIEW_QUESTIONS.md`](interview/INTERVIEW_QUESTIONS.md)
