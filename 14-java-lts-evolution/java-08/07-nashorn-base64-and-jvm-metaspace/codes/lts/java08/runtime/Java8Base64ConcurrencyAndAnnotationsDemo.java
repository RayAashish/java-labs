package lts.java08.runtime;

import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;

/**
 * Java 8 LTS — Standard Base64 (Basic/URL/MIME), Repeatable Annotations, LongAdder & CHM.
 */
public class Java8Base64ConcurrencyAndAnnotationsDemo {

    @Retention(RetentionPolicy.RUNTIME)
    @interface Schedules {
        Schedule[] value();
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Repeatable(Schedules.class)
    @interface Schedule {
        String cron();
    }

    @Schedule(cron = "0 0 * * *")
    @Schedule(cron = "0 30 9 * MON-FRI")
    static class NightlyReportJob {}

    public static void main(String[] args) {
        System.out.println("=== Java 8 Base64, Concurrency & Repeatable Annotations Demo ===");

        // -------------------------------------------------------------
        // 1. Standard Base64 vs URL-Safe Base64 (JWT Style) vs MIME
        // -------------------------------------------------------------
        byte[] sampleBytes = "user=alice&role=admin?token+id/123".getBytes(StandardCharsets.UTF_8);

        String basic = Base64.getEncoder().encodeToString(sampleBytes);
        // Safe in URLs/filenames: replaces '+' -> '-', '/' -> '_', drops '=' padding
        String urlSafeNoPad = Base64.getUrlEncoder().withoutPadding().encodeToString(sampleBytes);
        String decoded = new String(Base64.getUrlDecoder().decode(urlSafeNoPad), StandardCharsets.UTF_8);

        System.out.println("[1] Basic Base64   : " + basic);
        System.out.println("    URL-Safe NoPad : " + urlSafeNoPad);
        System.out.println("    Round-trip OK  : " + decoded);

        // -------------------------------------------------------------
        // 2. @Repeatable Annotations
        // -------------------------------------------------------------
        Schedule[] schedules = NightlyReportJob.class.getAnnotationsByType(Schedule.class);
        System.out.println("\n[2] Repeatable @Schedule count = " + schedules.length);
        for (Schedule s : schedules) {
            System.out.println("    cron -> " + s.cron());
        }

        // -------------------------------------------------------------
        // 3. LongAdder & ConcurrentHashMap V8 Atomic Frequency Counting
        // -------------------------------------------------------------
        ConcurrentHashMap<String, LongAdder> endpointHits = new ConcurrentHashMap<>();
        for (String endpoint : new String[]{"/orders", "/health", "/orders", "/orders"}) {
            endpointHits.computeIfAbsent(endpoint, key -> new LongAdder()).increment();
        }
        System.out.println("\n[3] CHM + LongAdder endpoint hits -> " + endpointHits);
    }
}
