package functional.optional;

import java.util.List;
import java.util.Optional;

/**
 * COMPLETE Optional<T> API & ANTI-PATTERN GUARD
 *
 * Covers every essential method:
 * - Creation: of(), ofNullable(), empty()
 * - Inspection: isPresent(), isEmpty(), ifPresent(), ifPresentOrElse()
 * - Pipelines: map(), flatMap(), filter(), or(), stream()
 * - Extraction: orElse() [EAGER TRAP!] vs orElseGet() [LAZY] vs orElseThrow()
 */
public class OptionalCompleteApiAndTrapsDemo {

    record Address(String city, Optional<String> postalCode) {}
    record Customer(String name, Optional<Address> address) {}

    public static void main(String[] args) {
        System.out.println("=== 1. of() vs ofNullable() ===");
        Optional<String> safeEmpty = Optional.ofNullable(null);
        System.out.println("ofNullable(null).isEmpty(): " + safeEmpty.isEmpty());
        try {
            Optional.of(null);
        } catch (NullPointerException ex) {
            System.out.println("Optional.of(null) fails fast with NullPointerException!");
        }

        System.out.println("\n=== 2. map(), flatMap(), filter(), or(), ifPresentOrElse() ===");
        Customer ray = new Customer("Ray", Optional.of(new Address("Bengaluru", Optional.of("560001"))));
        Customer guest = new Customer("Guest", Optional.empty());

        System.out.println("Ray zip:   " + extractUpperCity(ray));
        System.out.println("Guest zip: " + extractUpperCity(guest));

        Optional<String> primary = Optional.empty();
        Optional<String> resolved = primary
                .or(() -> Optional.of("BACKUP_CACHE_HIT"))
                .filter(s -> s.startsWith("BACKUP"));
        resolved.ifPresentOrElse(
                val -> System.out.println("Resolved via .or(): " + val),
                () -> System.out.println("Nothing found")
        );

        System.out.println("\n=== 3. Optional.stream() in Collections ===");
        List<Optional<String>> maybeTags = List.of(Optional.of("Java"), Optional.empty(), Optional.of("Loom"));
        List<String> presentTags = maybeTags.stream().flatMap(Optional::stream).toList();
        System.out.println("Flattened present tags: " + presentTags);

        System.out.println("\n=== 4. BAD orElse() Eager Trap vs GOOD orElseGet() Lazy ===");
        Optional<String> cached = Optional.of("HIT");
        String v1 = cached.orElse(expensiveDbFallback("orElse [BAD]"));     // STILL runs DB fallback!
        String v2 = cached.orElseGet(() -> expensiveDbFallback("orElseGet")); // Skipped cleanly!
        System.out.println("Values: " + v1 + " / " + v2);
    }

    private static String extractUpperCity(Customer customer) {
        return Optional.ofNullable(customer)
                .flatMap(Customer::address)
                .map(Address::city)
                .map(String::toUpperCase)
                .orElse("UNKNOWN_CITY");
    }

    private static String expensiveDbFallback(String caller) {
        System.out.println("   [SIDE EFFECT] Executed expensiveDbFallback from " + caller);
        return "FALLBACK";
    }
}
