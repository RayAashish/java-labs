package collections.hashmap_deep_dive.demo;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Demonstrates the hashCode() & equals() Contract, Contract Violations,
 * and the Mutable Key Hazard in HashMap.
 */
public class HashCodeEqualsContractDemo {

    public static void main(String[] args) {
        System.out.println("======================================================================");
        System.out.println("     HASHMAP CONTRACT VIOLATIONS & MUTABLE KEY HAZARD DEMONSTRATION   ");
        System.out.println("======================================================================");

        demoCorrectContract();
        demoMissingHashCode();
        demoMissingEquals();
        demoMutableKeyHazard();
    }

    // =========================================================================
    // Scenario 1: Correct Implementation
    // =========================================================================
    static class CorrectPerson {
        final int id;
        final String name;

        CorrectPerson(int id, String name) {
            this.id = id;
            this.name = name;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof CorrectPerson)) return false;
            CorrectPerson that = (CorrectPerson) o;
            return id == that.id && Objects.equals(name, that.name);
        }

        @Override
        public int hashCode() {
            return Objects.hash(id, name);
        }

        @Override
        public String toString() {
            return "Person(" + id + ", " + name + ")";
        }
    }

    static void demoCorrectContract() {
        System.out.println("\n----------------------------------------------------------------------");
        System.out.println("SCENARIO 1: BOTH equals() AND hashCode() PROPERLY OVERRIDDEN");
        System.out.println("----------------------------------------------------------------------");

        Map<CorrectPerson, String> map = new HashMap<>();
        CorrectPerson p1 = new CorrectPerson(101, "Alice");
        CorrectPerson p2 = new CorrectPerson(101, "Alice"); // Distinct object, logically equal

        System.out.println("p1.equals(p2)           : " + p1.equals(p2));
        System.out.println("p1.hashCode() == p2.hash: " + (p1.hashCode() == p2.hashCode()));

        map.put(p1, "Senior Software Engineer");
        System.out.println("map.put(p1, \"Senior Software Engineer\")");

        System.out.println("map.get(p2)             : " + map.get(p2));
        System.out.println("map.containsKey(p2)     : " + map.containsKey(p2));
        System.out.println("map.size()              : " + map.size());

        // Overwriting with p2
        map.put(p2, "Principal Engineer");
        System.out.println("map.put(p2, \"Principal Engineer\") -> Replaces value, no duplicate!");
        System.out.println("Updated map.get(p1)     : " + map.get(p1));
        System.out.println("Final map.size()        : " + map.size() + " (Expected: 1)");
        System.out.println("Outcome: [SUCCESS] HashMap functions correctly.");
    }

    // =========================================================================
    // Scenario 2: Overriding equals() without overriding hashCode()
    // =========================================================================
    static class BrokenEqualsOnlyPerson {
        final int id;
        final String name;

        BrokenEqualsOnlyPerson(int id, String name) {
            this.id = id;
            this.name = name;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof BrokenEqualsOnlyPerson)) return false;
            BrokenEqualsOnlyPerson that = (BrokenEqualsOnlyPerson) o;
            return id == that.id && Objects.equals(name, that.name);
        }
        // Missing hashCode()! Inherits Object.hashCode() based on memory address.
    }

    static void demoMissingHashCode() {
        System.out.println("\n----------------------------------------------------------------------");
        System.out.println("SCENARIO 2: OVERRIDING equals() BUT NOT hashCode() (CONTRACT VIOLATION)");
        System.out.println("----------------------------------------------------------------------");

        Map<BrokenEqualsOnlyPerson, String> map = new HashMap<>();
        BrokenEqualsOnlyPerson p1 = new BrokenEqualsOnlyPerson(202, "Bob");
        BrokenEqualsOnlyPerson p2 = new BrokenEqualsOnlyPerson(202, "Bob");

        System.out.println("p1.equals(p2)       : " + p1.equals(p2) + " (Logically Equal!)");
        System.out.println("p1.hashCode()       : " + p1.hashCode());
        System.out.println("p2.hashCode()       : " + p2.hashCode() + " (DIFFERENT HASHES!)");

        map.put(p1, "Staff Engineer");
        System.out.println("map.put(p1, \"Staff Engineer\")");

        System.out.println("map.get(p2)         : " + map.get(p2) + "  <-- BUG: FAILED LOOKUP (NULL)!");
        System.out.println("map.containsKey(p2) : " + map.containsKey(p2) + " <-- BUG: FALSE!");

        map.put(p2, "Architect");
        System.out.println("map.put(p2, \"Architect\")");
        System.out.println("map.size()          : " + map.size() + "  <-- BUG: DUPLICATE LOGICAL KEYS IN MAP!");
        System.out.println("Reason: Different hashCodes mapped p1 and p2 into different buckets.");
    }

    // =========================================================================
    // Scenario 3: Overriding hashCode() without overriding equals()
    // =========================================================================
    static class BrokenHashCodeOnlyPerson {
        final int id;
        final String name;

        BrokenHashCodeOnlyPerson(int id, String name) {
            this.id = id;
            this.name = name;
        }

        @Override
        public int hashCode() {
            return Objects.hash(id, name);
        }
        // Missing equals()! Inherits Object.equals() which checks reference identity (==).
    }

    static void demoMissingEquals() {
        System.out.println("\n----------------------------------------------------------------------");
        System.out.println("SCENARIO 3: OVERRIDING hashCode() BUT NOT equals() (CONTRACT VIOLATION)");
        System.out.println("----------------------------------------------------------------------");

        Map<BrokenHashCodeOnlyPerson, String> map = new HashMap<>();
        BrokenHashCodeOnlyPerson p1 = new BrokenHashCodeOnlyPerson(303, "Charlie");
        BrokenHashCodeOnlyPerson p2 = new BrokenHashCodeOnlyPerson(303, "Charlie");

        System.out.println("p1.hashCode()       : " + p1.hashCode());
        System.out.println("p2.hashCode()       : " + p2.hashCode() + " (Identical Hashes!)");
        System.out.println("p1.equals(p2)       : " + p1.equals(p2) + " (Checks this == obj -> false!)");

        map.put(p1, "Engineering Director");
        System.out.println("map.put(p1, \"Engineering Director\")");

        System.out.println("map.get(p2)         : " + map.get(p2) + "  <-- BUG: FAILED LOOKUP (NULL)!");
        System.out.println("Reason: Both land in the same bucket, but equals() failed identity check!");
    }

    // =========================================================================
    // Scenario 4: The Mutable Key Hazard
    // =========================================================================
    static class MutableAccountKey {
        String accountId;

        MutableAccountKey(String accountId) {
            this.accountId = accountId;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof MutableAccountKey)) return false;
            MutableAccountKey that = (MutableAccountKey) o;
            return Objects.equals(accountId, that.accountId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(accountId);
        }

        @Override
        public String toString() {
            return "AccountKey(" + accountId + ")";
        }
    }

    static void demoMutableKeyHazard() {
        System.out.println("\n----------------------------------------------------------------------");
        System.out.println("SCENARIO 4: THE DANGEROUS MUTABLE KEY HAZARD (SILENT MEMORY LEAK)");
        System.out.println("----------------------------------------------------------------------");

        Map<MutableAccountKey, String> map = new HashMap<>();
        MutableAccountKey key = new MutableAccountKey("ACC-1001");

        System.out.println("1. Initial Key: " + key + " | Initial Hash: " + key.hashCode());
        map.put(key, "Account Balance: $500,000");
        System.out.println("   map.put(key, ...)");
        System.out.println("   map.get(key) before mutation : " + map.get(key));

        // CRITICAL BUG: Mutating the key after storing it in HashMap!
        System.out.println("\n2. >>> MUTATING KEY STATE: key.accountId = \"ACC-9999\" <<<");
        key.accountId = "ACC-9999";
        System.out.println("   New HashCode after mutation   : " + key.hashCode());

        System.out.println("\n3. Attempting to retrieve using the mutated key:");
        System.out.println("   map.get(key)                  : " + map.get(key) + "  <-- LOOKUP RETURNS NULL!");
        System.out.println("   map.containsKey(key)          : " + map.containsKey(key) + " <-- RETURNS FALSE!");
        System.out.println("   map.size()                    : " + map.size() + "  <-- ENTRY IS STILL INSIDE MAP!");

        System.out.println("\n4. Attempting to remove the entry using map.remove(key):");
        String removed = map.remove(key);
        System.out.println("   map.remove(key)               : " + removed + " (FAILED TO REMOVE!)");
        System.out.println("   map.size() after removal      : " + map.size() + " (Still in map!)");

        System.out.println("\nDISASTER SUMMARY:");
        System.out.println("- The entry is placed in the table based on its OLD hash.");
        System.out.println("- When mutated, its NEW hash points to a completely different bucket.");
        System.out.println("- The entry becomes an UNREACHABLE ZOMBIE in the heap.");
        System.out.println("- Garbage Collection CANNOT reclaim it (referenced by table array).");
        System.out.println("- Result: Permanent, silent memory leak!");
    }
}
