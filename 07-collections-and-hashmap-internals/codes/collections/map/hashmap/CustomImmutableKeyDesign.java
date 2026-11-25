package collections.map.hashmap;

import java.util.*;

/**
 * ============================================================================
 * Java Collections Framework Mastery — Production-Grade Immutable Key Design
 * ============================================================================
 *
 * Implements a bulletproof, high-performance immutable key for hash-based
 * collections, adhering strictly to Joshua Bloch's Effective Java principles:
 *  1. Class declared 'final' to prevent malicious/buggy subclassing.
 *  2. All fields declared 'private final'.
 *  3. Deep defensive copying during constructor initialization for mutable objects.
 *  4. Deep defensive copying (or unmodifiable views) during field accessor calls.
 *  5. Cached hash code calculation with thread-safe benign race idiom.
 *  6. Contrast with Java 14+ Records (shallow vs deep immutability).
 *
 * @author raysi
 */
public class CustomImmutableKeyDesign {

    public static void main(String[] args) {
        System.out.println("==================================================================");
        System.out.println("   PRODUCTION-GRADE IMMUTABLE KEY ARCHITECTURE                    ");
        System.out.println("   Author: raysi                                                  ");
        System.out.println("==================================================================\n");

        demonstrateDefensiveCopyingOnConstructor();
        demonstrateDefensiveCopyingOnGetter();
        demonstrateCachedHashCodePerformance();
        demonstrateRecordImmutabilityCaveat();
    }

    /**
     * Demonstrates that mutating external data structures passed into the key's
     * constructor does NOT affect the key's internal state or hash code.
     */
    private static void demonstrateDefensiveCopyingOnConstructor() {
        System.out.println("--- 1. Defensive Copying on Construction ---");

        List<String> roles = new ArrayList<>(Arrays.asList("ADMIN", "ENGINEER"));
        Date validUntil = new Date();

        OrderRoutingKey key = new OrderRoutingKey("TENANT-001", "EU-CENTRAL", roles, validUntil);
        Map<OrderRoutingKey, String> router = new HashMap<>();
        router.put(key, "ActiveRoute_Alpha");

        int initialHash = key.hashCode();
        System.out.println("Initial Key Hash: " + initialHash);

        // Mutating external objects passed to constructor
        System.out.println("[Action] Modifying external 'roles' list and 'validUntil' date...");
        roles.add("SUPERUSER");
        validUntil.setTime(0L); // Shift epoch to 1970

        // Verify key remains completely unaffected
        System.out.println("Key Hash after external mutation: " + key.hashCode());
        System.out.println("Did HashCode change? " + (initialHash != key.hashCode() ? "YES (BUG!)" : "NO (SAFE)"));
        System.out.println("Map lookup succeeds: " + router.get(key));
        System.out.println();
    }

    /**
     * Demonstrates that calling a getter and mutating the returned object
     * cannot breach encapsulation or corrupt the key.
     */
    private static void demonstrateDefensiveCopyingOnGetter() {
        System.out.println("--- 2. Defensive Protection on Field Accessors ---");

        OrderRoutingKey key = new OrderRoutingKey("TENANT-002", "US-WEST",
                List.of("OPERATOR"), new Date());

        Map<OrderRoutingKey, String> router = new HashMap<>();
        router.put(key, "ActiveRoute_Beta");

        // Attempt to mutate through getter
        try {
            System.out.println("[Action] Attempting key.getPermissions().add(\"HACKER\")...");
            key.getPermissions().add("HACKER");
        } catch (UnsupportedOperationException e) {
            System.out.println("[Expected Exception] Accessor returned unmodifiable collection view: "
                    + e.getClass().getSimpleName());
        }

        // Accessing mutable Date via getter
        Date dateFromGetter = key.getValidUntil();
        dateFromGetter.setTime(9999999999L);
        System.out.println("Mutated Date returned by getter. Key's internal timestamp: "
                + key.getValidUntil().getTime());
        System.out.println("Key remains completely isolated! Lookup result: " + router.get(key));
        System.out.println();
    }

    /**
     * Demonstrates high-throughput cached hash code.
     * Computes once, caches in a 32-bit integer, and reuses for all subsequent calls.
     */
    private static void demonstrateCachedHashCodePerformance() {
        System.out.println("--- 3. Cached HashCode Mechanics ---");

        OrderRoutingKey key = new OrderRoutingKey("TENANT-003", "AP-SOUTH",
                List.of("READ", "WRITE"), new Date());

        // First invocation calculates and stores
        int h1 = key.hashCode();
        // Second invocation retrieves from cached field in 1 clock cycle
        int h2 = key.hashCode();

        System.out.println("h1 (calculated): " + h1 + ", h2 (cached): " + h2);
        System.out.println("HashCodes identical: " + (h1 == h2));
        System.out.println();
    }

    /**
     * Highlights Java 14+ Records: Records provide shallow immutability,
     * but mutable components (like Date or ArrayList) can still corrupt HashMap keys!
     */
    private static void demonstrateRecordImmutabilityCaveat() {
        System.out.println("--- 4. Java 14+ Record Immutability Caveat ---");

        Date mutableDate = new Date();
        RecordKey recordKey = new RecordKey("TENANT-REC", mutableDate);

        Map<RecordKey, String> map = new HashMap<>();
        map.put(recordKey, "RecordValue_OK");

        System.out.println("Initial Record Lookup: " + map.get(recordKey));

        // Mutating date field inside record
        System.out.println("[Action] Mutating date inside RecordKey externally...");
        mutableDate.setTime(0L);

        System.out.println("Record Lookup after date mutation: " + map.get(recordKey)
                + " <-- RETURNED NULL! (Record key corrupted!)");
        System.out.println("Lesson: Records guarantee final fields, but NOT deep immutability.");
        System.out.println("Always use immutable primitives, Strings, or defensive copies inside record compact constructors.\n");
    }

    /**
     * Production-grade Immutable Key Class.
     */
    public static final class OrderRoutingKey implements Comparable<OrderRoutingKey> {
        private final String tenantId;
        private final String region;
        private final List<String> permissions;
        private final Date validUntil;

        // Cached hash code (0 indicates uncomputed)
        // Thread-safe benign race: multiple threads computing identical hash
        private int hash;

        public OrderRoutingKey(String tenantId, String region,
                               List<String> permissions, Date validUntil) {
            this.tenantId = Objects.requireNonNull(tenantId, "tenantId cannot be null");
            this.region = Objects.requireNonNull(region, "region cannot be null");

            // 1. Defensive copy of Collection (creates an unmodifiable snapshot)
            this.permissions = permissions == null
                    ? List.of()
                    : List.copyOf(permissions);

            // 2. Defensive copy of mutable Date
            this.validUntil = validUntil == null
                    ? null
                    : new Date(validUntil.getTime());
        }

        public String getTenantId() {
            return tenantId;
        }

        public String getRegion() {
            return region;
        }

        public List<String> getPermissions() {
            // Already an unmodifiable List via List.copyOf()
            return permissions;
        }

        public Date getValidUntil() {
            // Defensive copy on read
            return validUntil == null ? null : new Date(validUntil.getTime());
        }

        @Override
        public boolean equals(Object o) {
            // 1. Identity check (1 clock cycle)
            if (this == o) return true;

            // 2. Exact class check (avoids instanceof symmetry violations across subclasses)
            if (o == null || getClass() != o.getClass()) return false;

            OrderRoutingKey that = (OrderRoutingKey) o;

            // 3. Compare primitive and object fields
            return Objects.equals(tenantId, that.tenantId) &&
                   Objects.equals(region, that.region) &&
                   Objects.equals(permissions, that.permissions) &&
                   Objects.equals(validUntil, that.validUntil);
        }

        @Override
        public int hashCode() {
            int h = hash;
            if (h == 0) {
                // Effective Java 31 multiplier algorithm
                int result = 1;
                result = 31 * result + tenantId.hashCode();
                result = 31 * result + region.hashCode();
                result = 31 * result + permissions.hashCode();
                result = 31 * result + (validUntil == null ? 0 : validUntil.hashCode());

                // Guard against degenerate hash code 0
                if (result == 0) {
                    result = 1;
                }
                hash = result;
                h = result;
            }
            return h;
        }

        @Override
        public int compareTo(OrderRoutingKey o) {
            int c = this.tenantId.compareTo(o.tenantId);
            if (c != 0) return c;
            return this.region.compareTo(o.region);
        }

        @Override
        public String toString() {
            return "OrderRoutingKey{" +
                    "tenant='" + tenantId + '\'' +
                    ", region='" + region + '\'' +
                    ", perms=" + permissions +
                    ", validUntil=" + validUntil +
                    '}';
        }
    }

    /**
     * Java 14+ Record representation demonstrating shallow vs deep immutability.
     */
    public record RecordKey(String tenant, Date timestamp) {
        // Compact constructor can enforce deep immutability if defensive copy is added:
        // public RecordKey {
        //     timestamp = new Date(timestamp.getTime());
        // }
    }
}
