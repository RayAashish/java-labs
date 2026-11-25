package datetime;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Demonstrates the immutable, thread-safe java.time (JSR-310) API:
 * LocalDate, LocalDateTime, Instant, ZonedDateTime, Period, Duration, and Clock.
 */
public class JavaTimeApiMasteryDemo {

    private static final DateTimeFormatter DISPLAY_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z");

    public static void main(String[] args) {
        System.out.println("=== 1. LocalDate & Period (Calendar / Human Time) ===");
        LocalDate launchDate = LocalDate.of(2026, 1, 15);
        LocalDate renewalDate = launchDate.plusMonths(6).plusDays(10);
        Period term = Period.between(launchDate, renewalDate);
        System.out.println("Launch: " + launchDate + " -> Renewal: " + renewalDate);
        System.out.printf("Period: %d months, %d days%n", term.getMonths(), term.getDays());

        System.out.println("\n=== 2. Instant, ZonedDateTime & Duration (Machine UTC vs Wall Clock) ===");
        Instant nowUtc = Instant.now();
        ZonedDateTime tokyoTime = nowUtc.atZone(ZoneId.of("Asia/Tokyo"));
        ZonedDateTime newYorkTime = nowUtc.atZone(ZoneId.of("America/New_York"));
        System.out.println("UTC Instant: " + nowUtc);
        System.out.println("Tokyo:       " + tokyoTime.format(DISPLAY_FMT));
        System.out.println("New York:    " + newYorkTime.format(DISPLAY_FMT));

        LocalDateTime start = LocalDateTime.of(2026, 11, 25, 9, 0);
        LocalDateTime end = LocalDateTime.of(2026, 11, 25, 11, 45);
        Duration meetingLength = Duration.between(start, end);
        System.out.println("Meeting duration in minutes: " + meetingLength.toMinutes());

        System.out.println("\n=== 3. Deterministic Testing via Clock Injection ===");
        Clock fixedClock = Clock.fixed(Instant.parse("2026-11-25T12:00:00Z"), ZoneId.of("UTC"));
        System.out.println("Token expires at: " + Instant.now(fixedClock).plus(Duration.ofHours(1)));
    }
}
