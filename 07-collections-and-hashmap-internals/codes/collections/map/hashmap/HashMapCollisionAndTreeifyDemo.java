package collections.map.hashmap;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Objects;
import sun.misc.Unsafe;

/**
 * ============================================================================
 * Java Collections Framework Mastery — HashMap Collision & Treeification Internals
 * ============================================================================
 *
 * This production-grade demonstration uses deep memory inspection to reveal:
 *  1. How hash collisions force singly-linked list chaining (Node<K,V>).
 *  2. Why having >= 8 colliding elements in a bucket does NOT treeify if
 *     table capacity < 64 (MIN_TREEIFY_CAPACITY); it resizes instead!
 *  3. Exact transition from Node<K,V> to TreeNode<K,V> once capacity >= 64.
 *  4. Untreeification behavior when entries are removed.
 *  5. Why Comparable<K> is mandatory for optimal O(log N) tree performance.
 *
 * @author raysi
 */
public class HashMapCollisionAndTreeifyDemo {

    public static void main(String[] args) {
        System.out.println("==================================================================");
        System.out.println("   HASHMAP COLLISION, RESIZE & TREEIFICATION DEEP DIVE            ");
        System.out.println("   Author: raysi                                                  ");
        System.out.println("==================================================================\n");

        demonstrateCapacityExpansionBeforeTreeify();
        demonstrateTreeificationAtCapacity64();
        demonstrateUntreeificationOnRemoval();
    }

    /**
     * Demonstrates that when initial capacity is 16, adding 8+ colliding keys
     * causes RESIZING (to 32, then 64) rather than treeification, because
     * MIN_TREEIFY_CAPACITY (64) is not yet met.
     */
    private static void demonstrateCapacityExpansionBeforeTreeify() {
        System.out.println("--- 1. Collisions with Default Capacity (MIN_TREEIFY_CAPACITY check) ---");
        // Default capacity = 16, load factor = 0.75
        HashMap<CollidingKey, String> map = new HashMap<>();

        System.out.println("Inserting 10 colliding keys (all hash to bucket with hashCode = 100)...");
        for (int i = 1; i <= 10; i++) {
            map.put(new CollidingKey(i, 100), "Value-" + i);
            Object[] table = getInternalTable(map);
            int capacity = (table == null) ? 0 : table.length;
            int bucketIndex = (table == null) ? 0 : (capacity - 1) & hash(new CollidingKey(i, 100));
            Object headNode = (table == null || bucketIndex >= capacity) ? null : table[bucketIndex];
            String nodeType = (headNode == null) ? "null" : headNode.getClass().getSimpleName();

            System.out.printf("   Inserted Key #%02d | Map Size: %02d | Table Capacity: %02d | Bucket Index: %02d | Bucket Head: %s%n",
                    i, map.size(), capacity, bucketIndex, nodeType);
        }

        System.out.println("Notice: Despite having 8+ elements in the collision chain,");
        System.out.println("HashMap resized from 16 -> 32 -> 64 instead of treeifying, because MIN_TREEIFY_CAPACITY = 64!\n");
    }

    /**
     * Demonstrates that when table capacity is >= 64, adding the 9th element
     * (binCount >= TREEIFY_THRESHOLD - 1) triggers treeifyBin(), converting
     * Node into TreeNode (Red-Black Tree).
     */
    private static void demonstrateTreeificationAtCapacity64() {
        System.out.println("--- 2. Treeification at Capacity >= 64 (TREEIFY_THRESHOLD = 8) ---");
        // Pre-allocate capacity 64 to bypass resize trigger
        HashMap<CollidingKey, String> map = new HashMap<>(64);

        System.out.println("Inserting colliding keys into pre-sized map (initial capacity = 64)...");
        for (int i = 1; i <= 12; i++) {
            CollidingKey key = new CollidingKey(i, 77);
            map.put(key, "Payload-" + i);

            Object[] table = getInternalTable(map);
            int capacity = table.length;
            int bucketIndex = (capacity - 1) & hash(key);
            Object headNode = table[bucketIndex];
            String nodeType = headNode.getClass().getSimpleName();

            System.out.printf("   Key #%02d | Size: %02d | Capacity: %02d | Bucket [%02d] Structure: %s%n",
                    i, map.size(), capacity, bucketIndex, nodeType);

            if (nodeType.equals("TreeNode") && i == 9) {
                System.out.println("   >>> [TREEIFICATION TRIGGERED] Chain length reached 8 -> Converted to TreeNode (Red-Black Tree)!");
            }
        }
        System.out.println();
    }

    /**
     * Demonstrates untreeification: when entries are removed from a treeified bucket,
     * the Red-Black tree is disassembled back into a singly-linked Node chain.
     */
    private static void demonstrateUntreeificationOnRemoval() {
        System.out.println("--- 3. Tree Disassembly (UNTREEIFY_THRESHOLD = 6) ---");
        HashMap<CollidingKey, String> map = new HashMap<>(64);

        // Populate with 10 colliding keys to force treeification
        for (int i = 1; i <= 10; i++) {
            map.put(new CollidingKey(i, 42), "Item-" + i);
        }

        Object[] table = getInternalTable(map);
        int bucketIndex = (table.length - 1) & hash(new CollidingKey(1, 42));
        System.out.println("State before removal: Bucket head is " + table[bucketIndex].getClass().getSimpleName());

        // Remove elements one by one
        System.out.println("Removing elements down from 10 to 5...");
        for (int i = 10; i >= 1; i--) {
            map.remove(new CollidingKey(i, 42));
            table = getInternalTable(map);
            Object headNode = table[bucketIndex];
            String nodeType = (headNode == null) ? "EMPTY (null)" : headNode.getClass().getSimpleName();
            System.out.printf("   Removed Key #%02d | Remaining Size: %02d | Bucket Head: %s%n",
                    i, map.size(), nodeType);
        }
        System.out.println();
    }

    /**
     * JDK perturbation hash algorithm replicated for exact bucket calculation.
     */
    private static int hash(Object key) {
        int h;
        return (key == null) ? 0 : (h = key.hashCode()) ^ (h >>> 16);
    }

    /**
     * Custom key designed to test collision dynamics.
     * All instances with the same 'targetHash' produce identical hash codes.
     * Implements Comparable to enable Red-Black tree binary branching.
     */
    public static class CollidingKey implements Comparable<CollidingKey> {
        private final int id;
        private final int targetHash;

        public CollidingKey(int id, int targetHash) {
            this.id = id;
            this.targetHash = targetHash;
        }

        @Override
        public int hashCode() {
            // Intentionally force identical hash codes to induce collisions
            return targetHash;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            CollidingKey that = (CollidingKey) o;
            return id == that.id && targetHash == that.targetHash;
        }

        @Override
        public int compareTo(CollidingKey o) {
            // Comparable ordering required for Red-Black Tree navigation in HashMap
            return Integer.compare(this.id, o.id);
        }

        @Override
        public String toString() {
            return "Key(" + id + ", hash=" + targetHash + ")";
        }
    }

    /**
     * High-reliability table extractor using Unsafe and standard reflection fallback.
     * Works across JDK 8, 11, 17, and 21+ without requiring JVM module command flags.
     */
    @SuppressWarnings("unchecked")
    private static Object[] getInternalTable(HashMap<?, ?> map) {
        try {
            // 1. Try Unsafe direct field offset (bypasses module access barriers in JDK 17/21)
            Field theUnsafeField = Unsafe.class.getDeclaredField("theUnsafe");
            theUnsafeField.setAccessible(true);
            Unsafe unsafe = (Unsafe) theUnsafeField.get(null);
            Field tableField = HashMap.class.getDeclaredField("table");
            long offset = unsafe.objectFieldOffset(tableField);
            return (Object[]) unsafe.getObject(map, offset);
        } catch (Throwable t) {
            try {
                // 2. Fallback to standard reflection (if --add-opens is supplied)
                Field tableField = HashMap.class.getDeclaredField("table");
                tableField.setAccessible(true);
                return (Object[]) tableField.get(map);
            } catch (Exception ex) {
                throw new RuntimeException("Failed to access HashMap.table via reflection/Unsafe", ex);
            }
        }
    }
}
