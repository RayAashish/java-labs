package lts.java08.optional;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Java 8 LTS — Idiomatic Optional<T> Usage vs Classic Anti-Patterns.
 *
 * Covers:
 * 1. Bad nested null checks vs Good monadic flatMap/map/filter pipeline.
 * 2. Eager `orElse(method())` bug vs Lazy `orElseGet(() -> method())`.
 * 3. `of()` fail-fast NPE vs `ofNullable()`.
 * 4. Java 9+ `or()`, `ifPresentOrElse()`, and `Optional::stream`.
 */
public class OptionalCompleteGuideDemo {

    record Address(String city, String zipCode) {
        Optional<String> zipOpt() { return Optional.ofNullable(zipCode); }
    }

    record UserProfile(String username, Address address) {
        Optional<Address> addressOpt() { return Optional.ofNullable(address); }
    }

    public static void main(String[] args) {
        System.out.println("=== Java 8 Optional Complete Guide Demo ===");

        UserProfile fullUser = new UserProfile("alice", new Address("Bengaluru", "560001"));
        UserProfile missingZip = new UserProfile("bob", new Address("Delhi", "  "));
        UserProfile noAddress = new UserProfile("guest", null);

        // -------------------------------------------------------------
        // 1. BAD (Imperative Null Pyramid) vs GOOD (Monadic Pipeline)
        // -------------------------------------------------------------
        System.out.println("[1] alice zip   = " + resolveZipModern(fullUser));
        System.out.println("    bob zip     = " + resolveZipModern(missingZip));
        System.out.println("    guest zip   = " + resolveZipModern(noAddress));
        System.out.println("    null user   = " + resolveZipModern(null));

        // -------------------------------------------------------------
        // 2. Eager orElse() Bug vs Lazy orElseGet()
        // -------------------------------------------------------------
        System.out.println("\n[2] orElse (EAGER) vs orElseGet (LAZY) proof:");
        AtomicInteger dbHits = new AtomicInteger(0);
        Optional<String> cacheHit = Optional.of("CACHED_TOKEN_XYZ");

        // BAD: computeFallback() runs EVEN THOUGH cacheHit is present!
        String a = cacheHit.orElse(computeFallback("orElse (BAD)", dbHits));
        // GOOD: Supplier lambda NEVER executes when value is present!
        String b = cacheHit.orElseGet(() -> computeFallback("orElseGet (GOOD)", dbHits));

        System.out.printf("    Results -> a=%s, b=%s | Wasteful fallback DB hits = %d%n", a, b, dbHits.get());

        // -------------------------------------------------------------
        // 3. Java 9+ Bridges: Optional::stream & or()
        // -------------------------------------------------------------
        List<Optional<String>> maybeEmails = List.of(
                Optional.of("alice@corp.io"),
                Optional.empty(),
                Optional.of("bob@corp.io")
        );
        List<String> validEmails = maybeEmails.stream()
                .flatMap(Optional::stream)
                .toList();
        System.out.println("\n[3] Extracted via Optional::stream -> " + validEmails);

        String resolved = Optional.<String>empty()
                .or(() -> Optional.of("SECONDARY_CONFIG_SOURCE"))
                .orElseThrow();
        System.out.println("    Fallback Optional.or() -> " + resolved);
    }

    // Compare how readable this is vs 4 nested `if (x != null)` checks!
    private static String resolveZipModern(UserProfile user) {
        return Optional.ofNullable(user)
                .flatMap(UserProfile::addressOpt)
                .flatMap(Address::zipOpt)
                .map(String::strip)
                .filter(zip -> !zip.isEmpty())
                .orElse("000000");
    }

    private static String computeFallback(String caller, AtomicInteger counter) {
        counter.incrementAndGet();
        System.out.println("    [!] Fallback executed inside: " + caller);
        return "DB_FALLBACK";
    }
}
