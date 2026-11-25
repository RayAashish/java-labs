# Date & Time API (`java.time` / JSR-310) — Deep Dive

---

## 🟢 Layer 1 — Noob Track: Choosing the Right Class in 10 Seconds

| Need | Use | Example |
|---|---|---|
| Exact UTC timestamp across servers | `Instant` | `2026-11-25T17:50:00Z` |
| Calendar date only (birthday, holiday) | `LocalDate` | `2026-11-25` |
| Wall clock time only (alarm at 07:30) | `LocalTime` | `07:30:00` |
| Date + Time without timezone | `LocalDateTime` | `2026-11-25T07:30` |
| Meeting in a specific city (handles DST) | `ZonedDateTime` | `2026-11-25T07:30-05:00[America/New_York]` |
| JSON/REST ISO offset timestamp | `OffsetDateTime` | `2026-11-25T13:00+05:30` |

---

## 🟡 Layer 2 — Practitioner Track: `Period` vs `Duration`, DST & Formatting

### 1. `Period` (Human Calendar) vs `Duration` (Machine Elapsed Time) — The DST Trap!
- `Duration.ofDays(1)` is **always strictly 24 hours (86,400 seconds)**.
- `Period.ofDays(1)` means **1 calendar day at the same local wall-clock time tomorrow**.
- **On a Spring-Forward DST transition day (23-hour day):**
  - `2026-03-08T01:00[America/New_York].plus(Period.ofDays(1))` -> `2026-03-09T01:00` (same clock time, 23 real hours elapsed!).
  - `.plus(Duration.ofDays(1))` -> `2026-03-09T02:00` (24 real hours elapsed!).

### 2. Thread-Safe `DateTimeFormatter` vs Legacy `SimpleDateFormat` Bug
- `SimpleDateFormat` stores intermediate parsed calendar fields in mutable instance state — sharing a `static final SimpleDateFormat` across Tomcat threads produces garbled dates (`2014` mixed with `2026`) without even throwing an exception!
- `DateTimeFormatter` is **100% immutable and thread-safe** — declare it `public static final` freely.

### 3. Unit Testing Time with `java.time.Clock`
Never sprinkle `LocalDate.now()` directly inside business logic (`isExpired()`). Inject a `Clock` bean (`Clock.systemUTC()` in production, `Clock.fixed(instant, zone)` in JUnit tests) so tests are deterministic forever.

---

## 🔴 Layer 3 — Senior / JVM Track: DST Gaps, Overlaps & Storage Rules

1. **Spring-Forward Gap (Non-existent Local Time):** At `02:00 -> 03:00` jump in New York, constructing `ZonedDateTime.of(2026, 3, 8, 2, 30, 0, 0, nyZone)` silently pushes forward by 1 hour to `03:30 EDT`!
2. **Fall-Back Overlap (Ambiguous Local Time):** At `02:00 -> 01:00` repeat, `01:30` happens **twice** (`-04:00 EDT` then `-05:00 EST`). Use `.withEarlierOffsetAtOverlap()` or `.withLaterOffsetAtOverlap()` explicitly.
3. **Database / Microservice Golden Rule:** Store `Instant` (UTC) for exact events (`created_at`), and store `(LocalDateTime + ZoneId)` separately when scheduling *future* events in cities whose governments may change DST laws later!
