package lts.java08.datetime;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.Period;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;

/**
 * Java 8 LTS — Date & Time API (JSR-310) Mastery.
 *
 * Covers:
 * 1. Why legacy `Date(2025, 1, 1)` & mutable setters were catastrophic vs `LocalDate.of`.
 * 2. Instant (UTC machine time) <-> ZonedDateTime (regional DST-aware human time).
 * 3. CRITICAL DST difference between Period.ofDays(1) and Duration.ofDays(1).
 * 4. Immutable thread-safe DateTimeFormatter + TemporalAdjusters.
 * 5. Deterministic unit testing via injected java.time.Clock.
 */
public class DateAndTimeJsr310Demo {

    // Immutable & 100% thread-safe! (Unlike legacy SimpleDateFormat)
    private static final DateTimeFormatter ISO_DISPLAY = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm z");

    // Service designed for testability using injected Clock
    record SubscriptionService(Clock clock) {
        boolean isExpired(Instant expiry) {
            return Instant.now(clock).isAfter(expiry);
        }
    }

    public static void main(String[] args) {
        System.out.println("=== Java 8 JSR-310 Date & Time Mastery ===");

        // -------------------------------------------------------------
        // 1. Immutability (Operations ALWAYS return a NEW object)
        // -------------------------------------------------------------
        LocalDate launch = LocalDate.of(2026, Month.NOVEMBER, 25);
        LocalDate nextWeek = launch.plusWeeks(1);
        System.out.printf("[1] Original unchanged=%s | plusWeeks(1)=%s%n", launch, nextWeek);

        // -------------------------------------------------------------
        // 2. Period vs Duration Across a Daylight Saving Transition!
        // -------------------------------------------------------------
        // 2026 US Spring-Forward DST starts Sunday March 8 at 02:00 AM (jumps to 03:00 -> 23-hour day)
        ZoneId nyZone = ZoneId.of("America/New_York");
        ZonedDateTime beforeDst = ZonedDateTime.of(
                LocalDateTime.of(2026, 3, 8, 1, 0),
                nyZone
        );

        ZonedDateTime plusPeriod = beforeDst.plus(Period.ofDays(1));     // Keeps 01:00 wall-clock
        ZonedDateTime plusDuration = beforeDst.plus(Duration.ofDays(1)); // Adds exact 24h -> lands at 02:00!

        System.out.println("\n[2] Across 23-hour Spring-Forward DST boundary:");
        System.out.println("    Start            : " + ISO_DISPLAY.format(beforeDst));
        System.out.println("    + Period(1d)     : " + ISO_DISPLAY.format(plusPeriod) + " (preserves wall clock 01:00)");
        System.out.println("    + Duration(24h)  : " + ISO_DISPLAY.format(plusDuration) + " (exact 24h elapsed!)");

        // -------------------------------------------------------------
        // 3. Timezone Conversion & TemporalAdjusters
        // -------------------------------------------------------------
        Instant nowUtc = Instant.parse("2026-11-25T12:00:00Z");
        ZonedDateTime ist = nowUtc.atZone(ZoneId.of("Asia/Kolkata"));
        ZonedDateTime tokyo = nowUtc.atZone(ZoneId.of("Asia/Tokyo"));
        System.out.printf("%n[3] Same UTC instant across zones:%n    UTC=%s%n    IST=%s%n    JST=%s%n",
                nowUtc, ISO_DISPLAY.format(ist), ISO_DISPLAY.format(tokyo));

        LocalDate nextFriday = launch.with(TemporalAdjusters.next(DayOfWeek.FRIDAY));
        LocalDate lastDayOfMonth = launch.with(TemporalAdjusters.lastDayOfMonth());
        System.out.printf("    Next Friday=%s | Last day of month=%s%n", nextFriday, lastDayOfMonth);

        // -------------------------------------------------------------
        // 4. Deterministic Testing with Clock.fixed(...)
        // -------------------------------------------------------------
        Clock frozenClock = Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneId.of("UTC"));
        SubscriptionService service = new SubscriptionService(frozenClock);
        System.out.println("\n[4] Fixed clock test -> expired(2025-12-31)? "
                + service.isExpired(Instant.parse("2025-12-31T23:59:59Z")));
        System.out.println("    Fixed clock test -> expired(2026-06-01)? "
                + service.isExpired(Instant.parse("2026-06-01T00:00:00Z")));
    }
}
