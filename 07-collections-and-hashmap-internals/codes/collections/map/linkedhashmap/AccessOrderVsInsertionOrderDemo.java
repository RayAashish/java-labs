package collections.map.linkedhashmap;

import java.util.*;

/**
 * ============================================================================
 * Java Collections Framework Mastery — LinkedHashMap Ordering Mechanics
 * ============================================================================
 *
 * Demonstrates:
 *  1. HashMap vs LinkedHashMap (Insertion-Order) vs LinkedHashMap (Access-Order).
 *  2. How get() and put() dynamically reshape the doubly-linked list in access-order mode.
 *  3. The infamous ConcurrentModificationException trap on read-only get() calls.
 *  4. Java 21 SequencedMap integration: firstEntry(), lastEntry(), reversed().
 *
 * @author raysi
 */
public class AccessOrderVsInsertionOrderDemo {

    public static void main(String[] args) {
        System.out.println("==================================================================");
        System.out.println("   LINKEDHASHMAP: INSERTION-ORDER VS ACCESS-ORDER INTERNALS       ");
        System.out.println("   Author: raysi                                                  ");
        System.out.println("==================================================================\n");

        demonstrateInsertionOrder();
        demonstrateAccessOrderDynamics();
        demonstrateHashMapArbitraryOrder();
        demonstrateGetTriggeringConcurrentModificationException();
        demonstrateJava21SequencedMapFeatures();
    }

    /**
     * Demonstrates standard LinkedHashMap (accessOrder = false).
     * Order strictly mirrors insertion order. Updating an existing key
     * does NOT change its position in the doubly-linked list.
     */
    private static void demonstrateInsertionOrder() {
        System.out.println("--- 1. LinkedHashMap in Insertion-Order Mode (Default) ---");
        Map<String, Integer> map = new LinkedHashMap<>();

        map.put("First", 1);
        map.put("Second", 2);
        map.put("Third", 3);
        map.put("Fourth", 4);

        System.out.println("Initial Insertion Order: " + map.keySet());

        // Accessing 'Second' via get
        map.get("Second");
        System.out.println("After get(\"Second\"):       " + map.keySet() + " (Unchanged!)");

        // Overwriting 'First' with a new value
        map.put("First", 999);
        System.out.println("After put(\"First\", 999):   " + map.keySet() + " (Unchanged: position preserved!)");
        System.out.println();
    }

    /**
     * Demonstrates LinkedHashMap in Access-Order Mode (accessOrder = true).
     * Every get() or put() that touches a key shifts that key to the tail (most-recently-used).
     */
    private static void demonstrateAccessOrderDynamics() {
        System.out.println("--- 2. LinkedHashMap in Access-Order Mode (LRU Mode) ---");
        // initialCapacity = 16, loadFactor = 0.75f, accessOrder = true
        Map<String, Integer> lruMap = new LinkedHashMap<>(16, 0.75f, true);

        lruMap.put("A", 1);
        lruMap.put("B", 2);
        lruMap.put("C", 3);
        lruMap.put("D", 4);

        System.out.println("Initial Keys:                   " + lruMap.keySet());
        System.out.println("Head (Eldest): A | Tail (Youngest): D");

        // Access 'B' -> Moves B to tail
        System.out.println("\n[Action] Accessing key 'B' via lruMap.get(\"B\")...");
        lruMap.get("B");
        System.out.println("Keys after accessing 'B':       " + lruMap.keySet() + " (B moved to tail!)");

        // Access 'A' -> Moves A to tail
        System.out.println("\n[Action] Accessing key 'A' via lruMap.get(\"A\")...");
        lruMap.get("A");
        System.out.println("Keys after accessing 'A':       " + lruMap.keySet() + " (A moved to tail!)");

        // Update 'C' -> Moves C to tail
        System.out.println("\n[Action] Updating key 'C' via lruMap.put(\"C\", 300)...");
        lruMap.put("C", 300);
        System.out.println("Keys after updating 'C':        " + lruMap.keySet() + " (C moved to tail!)");

        System.out.println("\nCurrent Eldest (Next to Evict): " + lruMap.keySet().iterator().next());
        System.out.println();
    }

    /**
     * Contrasts LinkedHashMap with HashMap, which has no ordering guarantees.
     */
    private static void demonstrateHashMapArbitraryOrder() {
        System.out.println("--- 3. Contrast: HashMap Has No Ordering Guarantees ---");
        Map<String, Integer> hashMap = new HashMap<>();

        // Insert elements
        for (int i = 1; i <= 8; i++) {
            hashMap.put("Key-" + i, i);
        }

        System.out.println("HashMap iteration order: " + hashMap.keySet());
        System.out.println("(Notice the keys do not follow insertion sequence; order depends solely on bucket hashes!)\n");
    }

    /**
     * Demonstrates the non-obvious CME trap:
     * In access-order mode, calling get() inside an iterator loop throws ConcurrentModificationException!
     */
    private static void demonstrateGetTriggeringConcurrentModificationException() {
        System.out.println("--- 4. The get() ConcurrentModificationException Trap ---");
        LinkedHashMap<String, Integer> accessMap = new LinkedHashMap<>(16, 0.75f, true);
        accessMap.put("Alpha", 1);
        accessMap.put("Beta", 2);
        accessMap.put("Gamma", 3);

        System.out.println("Iterating while calling get() on access-order map...");
        try {
            Iterator<String> it = accessMap.keySet().iterator();
            while (it.hasNext()) {
                String key = it.next();
                System.out.println("   Visiting: " + key);
                // READ OPERATION TRIGGERS CME!
                if ("Alpha".equals(key)) {
                    System.out.println("   [Action] Calling accessMap.get(\"Alpha\")...");
                    accessMap.get("Alpha"); // Rewires pointers and increments modCount!
                }
            }
        } catch (ConcurrentModificationException e) {
            System.out.println("   [CRASH] ConcurrentModificationException caught!");
            System.out.println("   Reason: In access-order mode, get() executes afterNodeAccess(),");
            System.out.println("   which modifies the doubly-linked list and increments modCount.");
            System.out.println("   Even though get() is a read operation, it is structurally mutating!\n");
        }
    }

    /**
     * Demonstrates JDK 21 SequencedMap methods on LinkedHashMap.
     */
    private static void demonstrateJava21SequencedMapFeatures() {
        System.out.println("--- 5. Java 21 SequencedMap Integration ---");
        LinkedHashMap<String, Integer> seqMap = new LinkedHashMap<>();
        seqMap.put("First", 100);
        seqMap.put("Middle", 200);
        seqMap.put("Last", 300);

        System.out.println("Original map: " + seqMap);
        System.out.println("firstEntry(): " + seqMap.firstEntry());
        System.out.println("lastEntry():  " + seqMap.lastEntry());

        // Reversed view (zero-allocation)
        SequencedMap<String, Integer> reversedView = seqMap.reversed();
        System.out.println("reversed() view: " + reversedView);

        // Mutating head and tail directly
        System.out.println("[Action] Calling seqMap.putFirst(\"Zero\", 0)...");
        seqMap.putFirst("Zero", 0);
        System.out.println("Map after putFirst: " + seqMap);

        System.out.println("[Action] Calling seqMap.pollFirstEntry()...");
        Map.Entry<String, Integer> polled = seqMap.pollFirstEntry();
        System.out.println("Polled entry: " + polled + " | Remaining map: " + seqMap);
        System.out.println();
    }
}
