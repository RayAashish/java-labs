package collections.map.hashmap;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * ============================================================================
 * Java Collections Framework Mastery — Mutable Key Memory Leak Demo
 * ============================================================================
 *
 * Demonstrates one of the most critical and silent bugs in Java backend systems:
 * Using a mutable object as a HashMap key and mutating its state after insertion.
 *
 * This demo proves:
 *  1. {@link Map#get(Object)} returns null (entry becomes invisible).
 *  2. {@link Map#containsKey(Object)} returns false.
 *  3. {@link Map#remove(Object)} fails to remove the entry.
 *  4. {@link Map#size()} remains unchanged (the zombie entry stays in memory).
 *  5. The entry continues to consume JVM heap memory, causing a silent leak.
 *  6. The correct mitigation: Immutable key design.
 *
 * @author raysi
 */
public class MutableKeyMemoryLeakDemo {

    public static void main(String[] args) {
        System.out.println("==================================================================");
        System.out.println("   MUTABLE KEY SILENT HEAP MEMORY LEAK DEMONSTRATION              ");
        System.out.println("   Author: raysi                                                  ");
        System.out.println("==================================================================\n");

        demonstrateMutableKeyLeak();
        demonstrateImmutableKeySafety();
    }

    /**
     * Demonstrates the severe pathology of mutating a key in-place.
     */
    private static void demonstrateMutableKeyLeak() {
        System.out.println("--- 1. Mutating a Key In-Place Inside HashMap ---");
        Map<MutableAccountKey, String> sessionStore = new HashMap<>();

        // Create a mutable key
        MutableAccountKey key = new MutableAccountKey("USER-1001", "FINANCE");
        System.out.println("Created Key: " + key + " (Initial HashCode: " + key.hashCode() + ")");

        // Insert into HashMap
        sessionStore.put(key, "ActiveSessionToken_XYZ123");
        System.out.println("Inserted into HashMap. Size: " + sessionStore.size());
        System.out.println("Lookup right after insertion: " + sessionStore.get(key));

        // NOW MUTATE THE KEY'S STATE
        System.out.println("\n[MUTATION ACTION] Mutating key.setDepartment(\"ENGINEERING\")...");
        key.setDepartment("ENGINEERING");
        System.out.println("Key after mutation: " + key + " (New HashCode: " + key.hashCode() + ")");

        // ATTEMPT LOOKUP WITH THE SAME OBJECT REFERENCE
        String retrieved = sessionStore.get(key);
        System.out.println("\n[LOOKUP FAILURE]");
        System.out.println("sessionStore.get(key): " + retrieved + "  <-- RETURNED NULL!");
        System.out.println("sessionStore.containsKey(key): " + sessionStore.containsKey(key) + " <-- RETURNS FALSE!");

        // ATTEMPT TO REMOVE THE ENTRY
        String removed = sessionStore.remove(key);
        System.out.println("\n[DELETION FAILURE]");
        System.out.println("sessionStore.remove(key): " + removed + "  <-- FAILED TO REMOVE!");
        System.out.println("sessionStore.size(): " + sessionStore.size() + "  <-- STILL 1! ZOMBIE ENTRY IS TRAPPED!");

        // PROVE THE ENTRY IS STILL IN MEMORY VIA ITERATION
        System.out.println("\n[PROOF OF HEAP LEAK]");
        System.out.println("Scanning entire entrySet() directly:");
        for (Map.Entry<MutableAccountKey, String> entry : sessionStore.entrySet()) {
            System.out.println("   Found Zombie Entry -> Key: " + entry.getKey()
                    + ", Stored Hash: " + entry.getKey().hashCode()
                    + ", Value: " + entry.getValue());
        }
        System.out.println("Verdict: The entry exists in the JVM heap, consumes memory, but is");
        System.out.println("completely unreachable by any key lookup! In long-running microservices,");
        System.out.println("this leads to OutOfMemoryError (OOM) as millions of zombie entries accumulate.\n");
    }

    /**
     * Demonstrates how an immutable key completely eliminates this risk.
     */
    private static void demonstrateImmutableKeySafety() {
        System.out.println("--- 2. Immutable Key Architectural Defense ---");
        Map<ImmutableAccountKey, String> safeSessionStore = new HashMap<>();

        ImmutableAccountKey safeKey = new ImmutableAccountKey("USER-2002", "PLATFORM");
        safeSessionStore.put(safeKey, "SecureToken_ABC987");

        System.out.println("Inserted Immutable Key: " + safeKey + " (HashCode: " + safeKey.hashCode() + ")");
        System.out.println("Lookup: " + safeSessionStore.get(safeKey));

        // Create another equivalent key instance
        ImmutableAccountKey duplicateInstance = new ImmutableAccountKey("USER-2002", "PLATFORM");
        System.out.println("Lookup using separate instance with equal fields: " + safeSessionStore.get(duplicateInstance));

        // Clean removal
        safeSessionStore.remove(duplicateInstance);
        System.out.println("Removed successfully. Size: " + safeSessionStore.size());
        System.out.println();
    }

    /**
     * Anti-pattern: Mutable class with equals and hashCode based on mutable fields.
     */
    public static class MutableAccountKey {
        private String accountId;
        private String department;

        public MutableAccountKey(String accountId, String department) {
            this.accountId = accountId;
            this.department = department;
        }

        public void setDepartment(String department) {
            this.department = department;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            MutableAccountKey that = (MutableAccountKey) o;
            return Objects.equals(accountId, that.accountId) &&
                   Objects.equals(department, that.department);
        }

        @Override
        public int hashCode() {
            return Objects.hash(accountId, department);
        }

        @Override
        public String toString() {
            return "MutableAccountKey{id='" + accountId + "', dept='" + department + "'}";
        }
    }

    /**
     * Best Practice: Immutable Key with final fields and no mutators.
     */
    public static final class ImmutableAccountKey {
        private final String accountId;
        private final String department;
        private final int hash; // Precomputed/cached hash code

        public ImmutableAccountKey(String accountId, String department) {
            this.accountId = Objects.requireNonNull(accountId, "accountId cannot be null");
            this.department = Objects.requireNonNull(department, "department cannot be null");
            this.hash = Objects.hash(accountId, department);
        }

        public String getAccountId() {
            return accountId;
        }

        public String getDepartment() {
            return department;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ImmutableAccountKey that = (ImmutableAccountKey) o;
            return Objects.equals(accountId, that.accountId) &&
                   Objects.equals(department, that.department);
        }

        @Override
        public int hashCode() {
            return hash;
        }

        @Override
        public String toString() {
            return "ImmutableAccountKey{id='" + accountId + "', dept='" + department + "'}";
        }
    }
}
