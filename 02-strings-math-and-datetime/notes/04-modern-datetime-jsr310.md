# 04. Modern Date & Time API (`java.time` / JSR-310)

---

## 🟢 Layer 1 — Noob Track: Picking the Right Class

Forget `java.util.Date` and `java.util.Calendar` (months are 0-indexed `0..11`, mutable, and confusing!). Since Java 8, use `java.time`:

| Need | Use Class | Example |
|---|---|---|
| Just a calendar date (birthday, invoice due date) | `LocalDate` | `2026-11-25` |
| Just a wall-clock time (alarm at 09:30) | `LocalTime` | `09:30:00` |
| Date + time without timezone (local booking slot) | `LocalDateTime` | `2026-11-25T09:30` |
| Machine timestamp in UTC (DB `created_at`, logs) | `Instant` | `2026-11-25T17:00:00Z` |
| Human date+time in a real timezone (flights, meetings) | `ZonedDateTime` | `2026-11-25T22:30+05:30[Asia/Kolkata]` |

---

## 🟡 Layer 2 — Practitioner Track: `Duration` vs `Period` & Formatting

- **`Duration`:** Machine/time-based gap in **seconds & nanoseconds** (for `Instant`, `LocalTime`, `LocalDateTime`).
- **`Period`:** Calendar/human gap in **years, months, days** (for `LocalDate` — accounts for leap years and varying month lengths!).
- **`DateTimeFormatter`:** Immutable and **thread-safe** (unlike legacy `SimpleDateFormat` which corrupts dates silently under concurrent threads!).

```java
DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd-MMM-yyyy HH:mm");
String formatted = LocalDateTime.now().format(fmt);
```

---

## 🔴 Layer 3 — Senior Track: DST Pitfalls, Clock Injection & DB Persistence

1. **Testability (`java.time.Clock`):** Never hardcode `Instant.now()` inside business services if tests must verify expiry or SLA logic. Inject a `Clock` bean (`Clock.systemUTC()` in prod, `Clock.fixed(...)` in unit tests).
2. **Daylight Saving Time (DST) & `ZonedDateTime` vs `OffsetDateTime`:**
   - `ZonedDateTime` uses IANA ZoneRules (`America/New_York`) and handles DST spring-forward gaps & fall-back overlaps automatically.
   - `OffsetDateTime` stores a fixed numeric offset (`+05:30`), matching SQL `TIMESTAMP WITH TIME ZONE` / ISO-8601 wire payloads cleanly.
3. **Rule of Thumb:** Persist **`Instant`** (UTC epoch) in databases and APIs; convert to `ZonedDateTime` only in the presentation layer for the user's locale.
