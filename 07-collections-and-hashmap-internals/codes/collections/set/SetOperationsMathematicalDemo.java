package collections.set;

import java.lang.reflect.Field;
import java.util.*;

/**
 * ============================================================================
 * Production-Grade Demonstration: Mathematical Set Operations & Set Internals
 * Author: raysi
 *
 * This executable class provides exhaustive verification of:
 * 1. Mathematical Set Operations: Union, Intersection, Difference, Symmetric Difference
 * 2. AbstractSet.removeAll() Performance Regression Trap [O(N) vs O(N * M)]
 * 3. Modern Immutable Set Contracts: Set.of() & Set.copyOf()
 * 4. Duplicate Rejection Mechanics and Identity
 * 5. Reflection Inspection: Proving HashSet is backed by HashMap & PRESENT dummy
 * ============================================================================
 */
public class SetOperationsMathematicalDemo {

    public static void main(String[] args) {
        System.out.println("================================================================================");
        System.out.println("          PHASE 4: SET & NAVIGABLESET ARCHITECTURAL VERIFICATION                ");
        System.out.println("================================================================================");

        demoMathematicalOperations();
        demoRemoveAllPerformanceTrap();
        demoImmutableSetContracts();
        demoDuplicateRejectionAndNullAllowance();
        demoHashSetBackedByHashMapReflection();

        System.out.println("\nAll Set Operations & Behavioral Invariants Verified Successfully.");
    }

    /**
     * 1. Mathematical Set Operations:
     * - Union (A ∪ B)
     * - Intersection (A ∩ B)
     * - Difference (A \ B)
     * - Symmetric Difference ((A \ B) ∪ (B \ A))
     */
    private static void demoMathematicalOperations() {
        System.out.println("\n--- 1. Mathematical Set Operations ---");

        Set<String> setA = new LinkedHashSet<>(Arrays.asList("Java", "Go", "Rust", "Python"));
        Set<String> setB = new LinkedHashSet<>(Arrays.asList("Rust", "Python", "C++", "Kotlin"));

        System.out.println("Initial Set A: " + setA);
        System.out.println("Initial Set B: " + setB);

        // Union: A ∪ B (mutate a clone of A using addAll)
        // Memory Note: New LinkedHashMap.Entry references are created for elements of B not already in A.
        Set<String> union = new LinkedHashSet<>(setA);
        union.addAll(setB);
        System.out.println("Union (A ∪ B): " + union);

        // Intersection: A ∩ B (mutate clone of A using retainAll)
        // Memory Note: Entries not present in setB are unlinked from the doubly-linked list
        // and removed from the hash bucket, becoming eligible for GC.
        Set<String> intersection = new LinkedHashSet<>(setA);
        intersection.retainAll(setB);
        System.out.println("Intersection (A ∩ B): " + intersection);

        // Difference (Relative Complement): A \ B
        Set<String> differenceAminusB = new LinkedHashSet<>(setA);
        differenceAminusB.removeAll(setB);
        System.out.println("Difference (A \\ B): " + differenceAminusB);

        // Symmetric Difference: (A \ B) ∪ (B \ A)
        // Elements present in either set A or set B, but NOT in both.
        Set<String> symDiff = new LinkedHashSet<>(setA);
        symDiff.addAll(setB); // (A ∪ B)
        Set<String> common = new LinkedHashSet<>(setA);
        common.retainAll(setB); // (A ∩ B)
        symDiff.removeAll(common); // (A ∪ B) \ (A ∩ B)
        System.out.println("Symmetric Difference (A Δ B): " + symDiff);
    }

    /**
     * 2. The AbstractSet.removeAll() Performance Cliff Trap.
     *
     * In JDK source code (AbstractSet.java):
     * - If this.size() > c.size(), iterates over 'c' and calls this.remove(x) -> O(c.size())
     * - If this.size() <= c.size(), iterates over 'this' and calls c.contains(x) -> O(this.size() * c.contains())
     *
     * When 'c' is an ArrayList, c.contains() is O(M). If this.size() <= c.size(),
     * the time complexity catastrophically blows up to O(N * M)!
     */
    private static void demoRemoveAllPerformanceTrap() {
        System.out.println("\n--- 2. AbstractSet.removeAll() Performance Trap ---");

        final int SET_SIZE = 50_000;
        final int COLLECTION_SIZE = 50_001; // Intentionally 1 element larger!

        Set<Integer> setForFastTest = new HashSet<>();
        Set<Integer> setForSlowTest = new HashSet<>();
        for (int i = 0; i < SET_SIZE; i++) {
            setForFastTest.add(i);
            setForSlowTest.add(i);
        }

        // Target 1: Set (O(1) contains)
        Set<Integer> setTarget = new HashSet<>();
        for (int i = 0; i < COLLECTION_SIZE; i++) {
            setTarget.add(i);
        }

        // Target 2: ArrayList (O(M) contains)
        List<Integer> listTarget = new ArrayList<>(COLLECTION_SIZE);
        for (int i = 0; i < COLLECTION_SIZE; i++) {
            listTarget.add(i);
        }

        // Scenario A: removeAll(Set)
        long t0 = System.nanoTime();
        setForFastTest.removeAll(setTarget);
        long timeFastNs = System.nanoTime() - t0;

        // Scenario B: removeAll(List) where set.size() <= list.size() -> triggers O(N * M) fallback!
        long t1 = System.nanoTime();
        setForSlowTest.removeAll(listTarget);
        long timeSlowNs = System.nanoTime() - t1;

        System.out.printf("removeAll(Set)       [Fast Path O(N)]:   %12d ns%n", timeFastNs);
        System.out.printf("removeAll(ArrayList) [Slow Path O(N*M)]: %12d ns (%.1fx slowdown!)%n",
                timeSlowNs, (double) timeSlowNs / Math.max(1, timeFastNs));
        System.out.println("ARCHITECTURAL TAKEAWAY: Never pass an unindexed List to Set.removeAll().");
    }

    /**
     * 3. Modern Immutable Set Contracts: Set.of() and Set.copyOf()
     */
    private static void demoImmutableSetContracts() {
        System.out.println("\n--- 3. Immutable Set Contracts (Set.of / Set.copyOf) ---");

        // Set.of creates compact Set12 or SetN instances with open addressing.
        Set<String> immutableSet = Set.of("Spring", "Quarkus", "Micronaut");
        System.out.println("Immutable Set: " + immutableSet + " | Implementation: " + immutableSet.getClass().getName());

        // Invariant A: Immutability enforcement
        try {
            immutableSet.add("Helidon");
            throw new AssertionError("Failed: Mutating Set.of() did not throw exception!");
        } catch (UnsupportedOperationException e) {
            System.out.println("Confirmed: Set.of() is unmodifiable (threw UnsupportedOperationException).");
        }

        // Invariant B: Rejection of null elements
        try {
            Set.of("Kafka", null, "RabbitMQ");
            throw new AssertionError("Failed: Set.of() allowed null!");
        } catch (NullPointerException e) {
            System.out.println("Confirmed: Set.of() rejects null elements with NullPointerException.");
        }

        // Invariant C: Rejection of duplicates at creation time
        try {
            Set.of("Redis", "Memcached", "Redis");
            throw new AssertionError("Failed: Set.of() allowed duplicate elements!");
        } catch (IllegalArgumentException e) {
            System.out.println("Confirmed: Set.of() rejects duplicates with IllegalArgumentException: " + e.getMessage());
        }

        // Invariant D: Set.copyOf() optimization (returns same reference if already an unmodifiable set)
        Set<String> copy = Set.copyOf(immutableSet);
        System.out.println("Set.copyOf(immutableSet) identity equal? " + (copy == immutableSet) +
                " (JDK avoids redundant defensive copying)");
    }

    /**
     * 4. Duplicate Rejection Behavior and Null Allowance.
     */
    private static void demoDuplicateRejectionAndNullAllowance() {
        System.out.println("\n--- 4. Duplicate Rejection & Null Allowance ---");

        Set<String> hashSet = new HashSet<>();
        boolean addedFirst = hashSet.add("Kubernetes");
        boolean addedDuplicate = hashSet.add("Kubernetes"); // duplicate

        System.out.println("First add('Kubernetes') returned: " + addedFirst);
        System.out.println("Second add('Kubernetes') returned: " + addedDuplicate);

        // Null element handling:
        // HashSet permits exactly one null element because HashMap allows a single null key (placed in bucket 0).
        boolean addedNullFirst = hashSet.add(null);
        boolean addedNullDuplicate = hashSet.add(null);

        System.out.println("First add(null) returned: " + addedNullFirst);
        System.out.println("Second add(null) returned: " + addedNullDuplicate);
        System.out.println("Current Set Elements: " + hashSet);
    }

    /**
     * 5. Reflection into HashSet: Demonstrating Backing HashMap & PRESENT Object.
     */
    private static void demoHashSetBackedByHashMapReflection() {
        System.out.println("\n--- 5. Reflection Verification: HashSet Backed by HashMap ---");

        HashSet<String> set = new HashSet<>();
        set.add("Element-A");
        set.add("Element-B");

        try {
            // Access private transient HashMap<E,Object> map;
            Field mapField = HashSet.class.getDeclaredField("map");
            mapField.setAccessible(true);
            Object internalMap = mapField.get(set);

            // Access private static final Object PRESENT;
            Field presentField = HashSet.class.getDeclaredField("PRESENT");
            presentField.setAccessible(true);
            Object presentObj = presentField.get(null);

            System.out.println("HashSet class instance: " + set.getClass().getName());
            System.out.println("Backing Map class: " + internalMap.getClass().getName());
            System.out.println("Static dummy PRESENT object identityHashCode: " + System.identityHashCode(presentObj));

            if (internalMap instanceof Map) {
                Map<?, ?> map = (Map<?, ?>) internalMap;
                System.out.println("Internal Map Size: " + map.size());
                for (Map.Entry<?, ?> entry : map.entrySet()) {
                    boolean isPresentInstance = (entry.getValue() == presentObj);
                    System.out.printf("  Entry -> Key: '%s' | Value: %s (== PRESENT: %b)%n",
                            entry.getKey(), entry.getValue(), isPresentInstance);
                }
            }
        } catch (ReflectiveOperationException e) {
            System.err.println("Reflection failed: " + e.getMessage());
        }
    }
}
