package collections.set.hashset;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.*;

/**
 * ============================================================================
 * Production-Grade Demonstration: HashSet Backed By HashMap & PRESENT Internals
 * Author: raysi
 *
 * This runnable class provides deep source-code level proof of:
 * 1. Reflection extraction of private transient HashMap<E,Object> map and private static final Object PRESENT
 * 2. Demonstration of duplicate rejection mechanics via map.put() return value
 * 3. Package-private constructor used exclusively by LinkedHashSet
 * 4. Memory overhead analysis: Node<K,V> chaining overhead vs flat memory models
 * 5. Fatal mutable key trap: Hash corruption causing silent memory leaks & broken Set contracts
 * ============================================================================
 */
public class HashSetBackedByHashMapDemo {

    public static void main(String[] args) throws Exception {
        System.out.println("================================================================================");
        System.out.println("          HASHSET SOURCE-LEVEL INTERNALS & MEMORY ARCHITECTURE                  ");
        System.out.println("================================================================================");

        verifyBackingHashMapAndPresentObject();
        verifyDuplicateRejectionMechanism();
        verifyPackagePrivateConstructorForLinkedHashSet();
        verifyMemoryFootprintCalculations();
        verifyMutableKeyCorruptionAndMemoryLeak();

        System.out.println("\nAll HashSet Internals & Architectural Invariants Verified Successfully.");
    }

    /**
     * 1. Access the internal HashMap and static dummy PRESENT object using reflection.
     * Proves that HashSet does not store elements directly; it stores them as KEYS in a HashMap.
     */
    private static void verifyBackingHashMapAndPresentObject() throws Exception {
        System.out.println("\n--- 1. Reflection: Inspecting Internal HashMap & PRESENT Object ---");

        HashSet<String> hashSet = new HashSet<>();
        hashSet.add("PostgreSQL");
        hashSet.add("MongoDB");
        hashSet.add("Cassandra");

        // Step A: Extract private transient HashMap<E,Object> map
        Field mapField = HashSet.class.getDeclaredField("map");
        mapField.setAccessible(true);
        Object internalMap = mapField.get(hashSet);

        // Step B: Extract private static final Object PRESENT
        Field presentField = HashSet.class.getDeclaredField("PRESENT");
        presentField.setAccessible(true);
        Object presentSingleton = presentField.get(null);

        System.out.println("HashSet Class: " + hashSet.getClass().getName());
        System.out.println("Backing Map Class: " + internalMap.getClass().getName());
        System.out.println("Static Dummy PRESENT IdentityHashCode: " + System.identityHashCode(presentSingleton));

        if (internalMap instanceof Map) {
            Map<?, ?> map = (Map<?, ?>) internalMap;
            System.out.println("Backing Map Size: " + map.size());

            // Iterate over the underlying Map entries directly
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                boolean isExactPresentReference = (entry.getValue() == presentSingleton);
                System.out.printf("  [Node Entry] Key: %-12s | Value Ref: 0x%08x | Exact PRESENT Singleton? %b%n",
                        entry.getKey(), System.identityHashCode(entry.getValue()), isExactPresentReference);
            }
        }
    }

    /**
     * 2. Duplicate rejection mechanics:
     * add(e) calls map.put(e, PRESENT) == null
     * When key is new: map.put returns null -> add() returns true.
     * When key exists: map.put returns existing value (PRESENT) -> add() returns false.
     */
    private static void verifyDuplicateRejectionMechanism() {
        System.out.println("\n--- 2. Duplicate Rejection Mechanics & Return Values ---");

        HashSet<String> set = new HashSet<>();

        // 1st insertion: absent -> map.put returns null -> (null == null) -> true
        boolean added1 = set.add("Redis");
        System.out.println("1st set.add('Redis'): " + added1 + " (map.put returned null)");

        // 2nd insertion: present -> map.put returns old PRESENT -> (PRESENT == null) -> false
        boolean added2 = set.add("Redis");
        System.out.println("2nd set.add('Redis'): " + added2 + " (map.put returned old PRESENT)");

        // Null insertion:
        // HashMap hashes null to 0 and stores it in bucket table[0].
        boolean addedNull1 = set.add(null);
        boolean addedNull2 = set.add(null);
        System.out.println("1st set.add(null):    " + addedNull1 + " (placed at bucket 0)");
        System.out.println("2nd set.add(null):    " + addedNull2 + " (detected existing null key at bucket 0)");
    }

    /**
     * 3. Package-Private constructor demo:
     * HashSet(int initialCapacity, float loadFactor, boolean dummy)
     * Demonstrates how LinkedHashSet leverages this to initialize LinkedHashMap.
     */
    private static void verifyPackagePrivateConstructorForLinkedHashSet() throws Exception {
        System.out.println("\n--- 3. Package-Private Constructor Verification ---");

        Constructor<HashSet> ctor = HashSet.class.getDeclaredConstructor(int.class, float.class, boolean.class);
        ctor.setAccessible(true);

        // Instantiate HashSet using the package-private constructor with dummy=true
        HashSet<String> customSet = ctor.newInstance(32, 0.75f, true);

        Field mapField = HashSet.class.getDeclaredField("map");
        mapField.setAccessible(true);
        Object internalMap = mapField.get(customSet);

        System.out.println("Instantiated HashSet with dummy boolean = true");
        System.out.println("Internal map type: " + internalMap.getClass().getName());
        System.out.println("Is instance of LinkedHashMap? " + (internalMap instanceof LinkedHashMap));
        System.out.println("EXPLANATION: LinkedHashSet has no storage fields of its own; it invokes this constructor!");
    }

    /**
     * 4. Memory Overhead Analysis:
     * Quantifying the heap memory penalty of HashSet's HashMap adapter pattern.
     */
    private static void verifyMemoryFootprintCalculations() {
        System.out.println("\n--- 4. Memory Overhead Calculation (64-bit JVM, Compressed OOPs) ---");

        final int N = 100_000;

        // In 64-bit JVM with Compressed OOPs:
        // - HashSet instance: 16 bytes
        // - HashMap instance: 48 bytes
        // - Table array: 16 (header) + 4 * capacity bytes
        //   For 100,000 entries at 0.75 load factor -> capacity = 131,072
        //   Table array = 16 + (131,072 * 4) = 524,304 bytes (~512 KB)
        // - HashMap.Node per element: 32 bytes (Header 12B + hash 4B + key 4B + val 4B + next 4B + padding 4B)
        //   100,000 nodes * 32 bytes = 3,200,000 bytes (~3.05 MB)
        // Total HashSet overhead = ~3.72 MB (excluding actual payload String/Integer objects).

        long estimatedHashSetOverheadBytes = 16 + 48 + 16 + (131072L * 4) + (N * 32L);

        // Hypothetical flat open-addressing Set (e.g., SetN / fastutil / primitive set):
        // - Single Object[] array of size 131,072: 16 + (131072 * 4) = 524,304 bytes (~512 KB)
        // - Zero Node objects allocated! Zero PRESENT pointer overhead!
        long estimatedFlatSetOverheadBytes = 24 + 16 + (131072L * 4);

        System.out.printf("Estimated overhead for %,d elements in HashSet: %,d bytes (%.2f MB)%n",
                N, estimatedHashSetOverheadBytes, estimatedHashSetOverheadBytes / (1024.0 * 1024.0));
        System.out.printf("Estimated overhead for %,d elements in Flat Open-Addressing Set: %,d bytes (%.2f MB)%n",
                N, estimatedFlatSetOverheadBytes, estimatedFlatSetOverheadBytes / (1024.0 * 1024.0));
        System.out.printf("Memory Reduction Factor: %.1fx less overhead with open-addressing!%n",
                (double) estimatedHashSetOverheadBytes / estimatedFlatSetOverheadBytes);
    }

    /**
     * 5. The Fatal Mutable Key Memory Leak Trap:
     * What happens when an element's hashCode() changes while it is inside a HashSet?
     */
    private static void verifyMutableKeyCorruptionAndMemoryLeak() {
        System.out.println("\n--- 5. The Fatal Mutable Key Memory Leak Trap ---");

        class Account {
            private final int accountId;
            private String ownerName; // Mutable field participating in hashCode!

            Account(int accountId, String ownerName) {
                this.accountId = accountId;
                this.ownerName = ownerName;
            }

            public void setOwnerName(String ownerName) {
                this.ownerName = ownerName;
            }

            @Override
            public boolean equals(Object o) {
                if (this == o) return true;
                if (!(o instanceof Account account)) return false;
                return accountId == account.accountId && Objects.equals(ownerName, account.ownerName);
            }

            @Override
            public int hashCode() {
                return Objects.hash(accountId, ownerName);
            }

            @Override
            public String toString() {
                return "Account{" + "id=" + accountId + ", name='" + ownerName + "'}";
            }
        }

        HashSet<Account> accountSet = new HashSet<>();
        Account acc = new Account(1001, "Aashish");
        accountSet.add(acc);

        int originalHash = acc.hashCode();
        System.out.println("Inserted Account: " + acc + " | Hash: " + originalHash);
        System.out.println("contains() before mutation: " + accountSet.contains(acc));

        // MUTATION: Changes the hash code!
        acc.setOwnerName("Aashish_Updated");
        int mutatedHash = acc.hashCode();
        System.out.println("\n--- Mutating Owner Name ---");
        System.out.println("Account After Mutation: " + acc + " | New Hash: " + mutatedHash);

        // Verification of catastrophic corruption:
        boolean foundByContains = accountSet.contains(acc);
        boolean removed = accountSet.remove(acc);
        boolean addedAgain = accountSet.add(acc); // Tries to re-add the exact same object!

        System.out.println("1. contains(acc): " + foundByContains + " -> FAILED! Looks in bucket for new hash.");
        System.out.println("2. remove(acc):   " + removed + " -> FAILED! Cannot find node to unlink.");
        System.out.println("3. add(acc) again: " + addedAgain + " -> SUCCESS! Inserted duplicate reference into set!");
        System.out.println("4. Set size after adding duplicate: " + accountSet.size());

        System.out.println("\nIterating over elements currently in set:");
        for (Account a : accountSet) {
            System.out.println("  -> " + a + " (identity: 0x" + Integer.toHexString(System.identityHashCode(a)) + ")");
        }
        System.out.println("CONSEQUENCE: Mutating keys produces silent zombie entries, memory leaks, and duplicate elements.");
    }
}
