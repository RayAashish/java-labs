package collections.hashmap_deep_dive.demo;

import java.util.*;

/**
 * Comparative Demonstration of Major Java Map Implementations:
 * 1. HashMap: Fast, unordered hashing.
 * 2. LinkedHashMap: Insertion-order preservation.
 * 3. LinkedHashMap (Access-Order): Production LRU Cache implementation.
 * 4. TreeMap: Red-Black tree with sorted keys and NavigableMap capabilities.
 * 5. IdentityHashMap: Reference equality (==) instead of logical equality (.equals()).
 */
public class MapImplementationsComparisonDemo {

    public static void main(String[] args) {
        System.out.println("======================================================================");
        System.out.println("          JAVA MAP IMPLEMENTATIONS: COMPARATIVE ARCHITECTURE          ");
        System.out.println("======================================================================");

        demoHashMapUnordered();
        demoLinkedHashMapInsertionOrder();
        demoLinkedHashMapLRUCache();
        demoTreeMapSortedOrder();
        demoIdentityHashMap();
    }

    // =========================================================================
    // 1. HashMap: Unordered
    // =========================================================================
    static void demoHashMapUnordered() {
        System.out.println("\n----------------------------------------------------------------------");
        System.out.println("1. java.util.HashMap (UNORDERED)");
        System.out.println("----------------------------------------------------------------------");

        Map<String, Integer> map = new HashMap<>();
        String[] keys = {"Zebra", "Apple", "Mango", "Banana", "Orange", "Kiwi"};
        for (int i = 0; i < keys.length; i++) {
            map.put(keys[i], i + 1);
        }

        System.out.println("Insertion sequence : " + Arrays.toString(keys));
        System.out.println("HashMap iteration  : " + map.keySet());
        System.out.println("Notice: Keys are ordered by internal bucket hash index, not insertion order!");
    }

    // =========================================================================
    // 2. LinkedHashMap: Insertion Order
    // =========================================================================
    static void demoLinkedHashMapInsertionOrder() {
        System.out.println("\n----------------------------------------------------------------------");
        System.out.println("2. java.util.LinkedHashMap (INSERTION-ORDER PRESERVED)");
        System.out.println("----------------------------------------------------------------------");

        Map<String, Integer> linkedMap = new LinkedHashMap<>();
        String[] keys = {"Zebra", "Apple", "Mango", "Banana", "Orange", "Kiwi"};
        for (int i = 0; i < keys.length; i++) {
            linkedMap.put(keys[i], i + 1);
        }

        System.out.println("Insertion sequence : " + Arrays.toString(keys));
        System.out.println("LinkedMap iteration: " + linkedMap.keySet());
        System.out.println("Notice: Iteration order exactly matches the order elements were inserted!");
    }

    // =========================================================================
    // 3. LinkedHashMap: LRU (Least Recently Used) Cache
    // =========================================================================
    static class SimpleLRUCache<K, V> extends LinkedHashMap<K, V> {
        private final int maxCapacity;

        public SimpleLRUCache(int maxCapacity) {
            // true flag enables ACCESS-ORDER instead of insertion-order
            super(maxCapacity, 0.75f, true);
            this.maxCapacity = maxCapacity;
        }

        @Override
        protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
            // Automatically evict eldest element when size exceeds capacity
            return size() > maxCapacity;
        }
    }

    static void demoLinkedHashMapLRUCache() {
        System.out.println("\n----------------------------------------------------------------------");
        System.out.println("3. LinkedHashMap (ACCESS-ORDER) AS AN LRU CACHE (MAX CAPACITY = 3)");
        System.out.println("----------------------------------------------------------------------");

        SimpleLRUCache<String, String> lru = new SimpleLRUCache<>(3);

        lru.put("Page1", "Home");
        lru.put("Page2", "Profile");
        lru.put("Page3", "Dashboard");
        System.out.println("Inserted Page1, Page2, Page3: " + lru.keySet());

        // Access Page1 -> Moves Page1 to the MRU (Most Recently Used / Tail) position!
        System.out.println("\n>>> Accessing Page1 (lru.get(\"Page1\")) <<<");
        lru.get("Page1");
        System.out.println("State after accessing Page1: " + lru.keySet());
        System.out.println("Page2 is now the Eldest (Least Recently Used)!");

        // Insert Page4 -> Exceeds maxCapacity (3), so Eldest (Page2) will be evicted!
        System.out.println("\n>>> Inserting Page4 (lru.put(\"Page4\", \"Settings\")) <<<");
        lru.put("Page4", "Settings");
        System.out.println("State after inserting Page4: " + lru.keySet());
        System.out.println("Notice: Page2 was automatically evicted! Page1 was retained because it was recently accessed.");
    }

    // =========================================================================
    // 4. TreeMap: Sorted Red-Black Tree
    // =========================================================================
    static void demoTreeMapSortedOrder() {
        System.out.println("\n----------------------------------------------------------------------");
        System.out.println("4. java.util.TreeMap (SORTED BY KEY / NAVIGABLE MAP)");
        System.out.println("----------------------------------------------------------------------");

        NavigableMap<Integer, String> treeMap = new TreeMap<>();
        treeMap.put(40, "Value 40");
        treeMap.put(10, "Value 10");
        treeMap.put(50, "Value 50");
        treeMap.put(20, "Value 20");
        treeMap.put(30, "Value 30");

        System.out.println("TreeMap keys (Sorted naturally) : " + treeMap.keySet());
        System.out.println("First Key (Minimum)             : " + treeMap.firstKey());
        System.out.println("Last Key (Maximum)              : " + treeMap.lastKey());
        System.out.println("Floor key for 25 (<= 25)        : " + treeMap.floorKey(25));
        System.out.println("Ceiling key for 25 (>= 25)      : " + treeMap.ceilingKey(25));
        System.out.println("SubMap [20 to 50 exclusive)     : " + treeMap.subMap(20, 50).keySet());
    }

    // =========================================================================
    // 5. IdentityHashMap: Reference Equality (==)
    // =========================================================================
    static void demoIdentityHashMap() {
        System.out.println("\n----------------------------------------------------------------------");
        System.out.println("5. java.util.IdentityHashMap (REFERENCE EQUALITY '==' COMPARISON)");
        System.out.println("----------------------------------------------------------------------");

        Map<String, String> identityMap = new IdentityHashMap<>();
        Map<String, String> standardMap = new HashMap<>();

        // Two distinct String heap objects with identical contents
        String s1 = new String("SecretToken");
        String s2 = new String("SecretToken");

        System.out.println("s1.equals(s2)                   : " + s1.equals(s2));
        System.out.println("s1 == s2                        : " + (s1 == s2));

        standardMap.put(s1, "Session_1");
        standardMap.put(s2, "Session_2");

        identityMap.put(s1, "Session_1");
        identityMap.put(s2, "Session_2");

        System.out.println("\nStandard HashMap size           : " + standardMap.size() + " (Key overwritten via equals())");
        System.out.println("IdentityHashMap size            : " + identityMap.size() + " (Both keys stored via == !)");
        System.out.println("Use Case: Graph traversal cycle detection and deep object cloning.");
    }
}
