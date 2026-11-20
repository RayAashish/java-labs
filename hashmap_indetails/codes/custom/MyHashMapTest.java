package custom;

import java.util.Collection;
import java.util.Objects;
import java.util.Set;

/**
 * Comprehensive verification test suite for MyHashMap.
 * Tests basic CRUD, null keys/values, collision chaining, dynamic resizing,
 * high-bit partition rehashing, and collection views.
 */
public class MyHashMapTest {

    // Helper class to intentionally simulate hash collisions
    static class BadHashKey {
        final String name;
        final int fixedHash;

        BadHashKey(String name, int fixedHash) {
            this.name = name;
            this.fixedHash = fixedHash;
        }

        @Override
        public int hashCode() {
            return fixedHash; // Force collision
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof BadHashKey)) return false;
            BadHashKey that = (BadHashKey) o;
            return Objects.equals(name, that.name);
        }

        @Override
        public String toString() {
            return "Key(" + name + ", hash=" + fixedHash + ")";
        }
    }

    public static void main(String[] args) {
        System.out.println("=================================================");
        System.out.println("         RUNNING MyHashMap TEST SUITE            ");
        System.out.println("=================================================");

        testBasicPutAndGet();
        testNullKeyAndValues();
        testUpdateExistingKey();
        testCollisionChaining();
        testDynamicResizing();
        testNodeRemovalAcrossChain();
        testContainsAndClear();
        testCollectionViews();

        System.out.println("\n>>> ALL MyHashMap TESTS PASSED SUCCESSFULLY! <<<\n");
    }

    static void testBasicPutAndGet() {
        System.out.print("[TEST] Basic Put and Get ... ");
        MyHashMap<String, Integer> map = new MyHashMap<>();
        assertEqual(0, map.size());
        assertTrue(map.isEmpty());

        map.put("Apple", 10);
        map.put("Banana", 20);
        map.put("Cherry", 30);

        assertEqual(3, map.size());
        assertFalse(map.isEmpty());
        assertEqual(10, map.get("Apple"));
        assertEqual(20, map.get("Banana"));
        assertEqual(30, map.get("Cherry"));
        assertNull(map.get("NonExistent"));
        System.out.println("PASSED");
    }

    static void testNullKeyAndValues() {
        System.out.print("[TEST] Null Key and Null Values ... ");
        MyHashMap<String, String> map = new MyHashMap<>();

        // Null key mapping
        map.put(null, "NullValue");
        assertEqual(1, map.size());
        assertEqual("NullValue", map.get(null));
        assertTrue(map.containsKey(null));

        // Key mapped to null value
        map.put("EmptyKey", null);
        assertEqual(2, map.size());
        assertNull(map.get("EmptyKey"));
        assertTrue(map.containsKey("EmptyKey")); // Distinguish absent vs null value

        // Overwrite null key
        String old = map.put(null, "UpdatedNullValue");
        assertEqual("NullValue", old);
        assertEqual("UpdatedNullValue", map.get(null));
        System.out.println("PASSED");
    }

    static void testUpdateExistingKey() {
        System.out.print("[TEST] Overwriting Existing Keys ... ");
        MyHashMap<Integer, String> map = new MyHashMap<>();
        assertNull(map.put(1, "First"));
        assertEqual("First", map.get(1));
        assertEqual(1, map.size());

        String previous = map.put(1, "Second");
        assertEqual("First", previous);
        assertEqual("Second", map.get(1));
        assertEqual(1, map.size()); // Size must NOT change
        System.out.println("PASSED");
    }

    static void testCollisionChaining() {
        System.out.print("[TEST] Severe Hash Collision Chaining ... ");
        MyHashMap<BadHashKey, String> map = new MyHashMap<>();

        // All 5 keys will return hashCode = 42, forcing all into the exact same bucket!
        BadHashKey k1 = new BadHashKey("Alpha", 42);
        BadHashKey k2 = new BadHashKey("Beta", 42);
        BadHashKey k3 = new BadHashKey("Gamma", 42);
        BadHashKey k4 = new BadHashKey("Delta", 42);
        BadHashKey k5 = new BadHashKey("Epsilon", 42);

        map.put(k1, "Val1");
        map.put(k2, "Val2");
        map.put(k3, "Val3");
        map.put(k4, "Val4");
        map.put(k5, "Val5");

        assertEqual(5, map.size());
        assertEqual("Val1", map.get(k1));
        assertEqual("Val2", map.get(k2));
        assertEqual("Val3", map.get(k3));
        assertEqual("Val4", map.get(k4));
        assertEqual("Val5", map.get(k5));
        System.out.println("PASSED");
    }

    static void testDynamicResizing() {
        System.out.print("[TEST] Dynamic Resizing & High-Bit Rehashing ... ");
        // Initial capacity = 16, default load factor = 0.75, threshold = 12
        MyHashMap<Integer, String> map = new MyHashMap<>(16);

        for (int i = 1; i <= 12; i++) {
            map.put(i, "Value" + i);
        }
        assertEqual(12, map.size());
        assertEqual(16, map.capacity());

        // Inserting 13th element must trigger resize from 16 -> 32
        map.put(13, "Value13");
        assertEqual(13, map.size());
        assertEqual(32, map.capacity());
        assertEqual(24, map.threshold());

        // Verify all 13 elements remain intact after rehashing
        for (int i = 1; i <= 13; i++) {
            assertEqual("Value" + i, map.get(i));
        }

        // Insert up to 50 elements to trigger additional resizes
        for (int i = 14; i <= 50; i++) {
            map.put(i, "Value" + i);
        }
        assertEqual(50, map.size());
        assertTrue(map.capacity() >= 64);

        for (int i = 1; i <= 50; i++) {
            assertEqual("Value" + i, map.get(i));
        }
        System.out.println("PASSED");
    }

    static void testNodeRemovalAcrossChain() {
        System.out.print("[TEST] Node Removal Across Collision Chains ... ");
        MyHashMap<BadHashKey, String> map = new MyHashMap<>();

        BadHashKey k1 = new BadHashKey("Head", 99);
        BadHashKey k2 = new BadHashKey("Middle", 99);
        BadHashKey k3 = new BadHashKey("Tail", 99);

        map.put(k1, "V1");
        map.put(k2, "V2");
        map.put(k3, "V3");

        // 1. Remove middle node
        assertEqual("V2", map.remove(k2));
        assertEqual(2, map.size());
        assertNull(map.get(k2));
        assertEqual("V1", map.get(k1));
        assertEqual("V3", map.get(k3));

        // 2. Remove head node
        assertEqual("V1", map.remove(k1));
        assertEqual(1, map.size());
        assertNull(map.get(k1));
        assertEqual("V3", map.get(k3));

        // 3. Remove tail node
        assertEqual("V3", map.remove(k3));
        assertEqual(0, map.size());
        assertTrue(map.isEmpty());
        assertNull(map.get(k3));

        // 4. Remove non-existent key returns null
        assertNull(map.remove(k1));
        System.out.println("PASSED");
    }

    static void testContainsAndClear() {
        System.out.print("[TEST] ContainsKey, ContainsValue, Clear ... ");
        MyHashMap<String, Integer> map = new MyHashMap<>();
        map.put("X", 100);
        map.put("Y", 200);

        assertTrue(map.containsKey("X"));
        assertFalse(map.containsKey("Z"));
        assertTrue(map.containsValue(100));
        assertFalse(map.containsValue(999));

        map.clear();
        assertEqual(0, map.size());
        assertTrue(map.isEmpty());
        assertNull(map.get("X"));
        System.out.println("PASSED");
    }

    static void testCollectionViews() {
        System.out.print("[TEST] KeySet, Values, EntrySet Views ... ");
        MyHashMap<String, Integer> map = new MyHashMap<>();
        map.put("One", 1);
        map.put("Two", 2);
        map.put("Three", 3);

        Set<String> keys = map.keySet();
        assertEqual(3, keys.size());
        assertTrue(keys.contains("One"));
        assertTrue(keys.contains("Two"));
        assertTrue(keys.contains("Three"));

        Collection<Integer> vals = map.values();
        assertEqual(3, vals.size());
        assertTrue(vals.contains(1));
        assertTrue(vals.contains(2));
        assertTrue(vals.contains(3));

        Set<MyMap.Entry<String, Integer>> entries = map.entrySet();
        assertEqual(3, entries.size());
        System.out.println("PASSED");
    }

    // =========================================================================
    // Test Assertion Helpers
    // =========================================================================

    static void assertTrue(boolean condition) {
        if (!condition) {
            throw new AssertionError("Expected true but was false");
        }
    }

    static void assertFalse(boolean condition) {
        if (condition) {
            throw new AssertionError("Expected false but was true");
        }
    }

    static void assertEqual(Object expected, Object actual) {
        if (!Objects.equals(expected, actual)) {
            throw new AssertionError("Expected [" + expected + "] but found [" + actual + "]");
        }
    }

    static void assertNull(Object object) {
        if (object != null) {
            throw new AssertionError("Expected null but found [" + object + "]");
        }
    }
}
