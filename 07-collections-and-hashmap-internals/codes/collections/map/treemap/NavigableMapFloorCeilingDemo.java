package collections.map.treemap;

import java.util.*;

/**
 * Production-Grade Demonstration of TreeMap and NavigableMap Internals.
 * 
 * Explores:
 *  1. Boundary Lookups: floorKey, ceilingKey, lowerKey, higherKey
 *  2. Range Views: subMap (bounded), headMap, tailMap (inclusive/exclusive flags)
 *  3. Extremity Queries & Polling: firstKey, lastKey, pollFirstEntry, pollLastEntry
 *  4. Non-allocating Reverse Iteration: descendingMap() view
 *  5. Real-World Scenario: Financial Limit Order Book & Nearest-Neighbor Price Matcher
 *  6. Architectural Traps: ClassCastException on non-Comparable keys & equals() vs compareTo()
 * 
 * @author raysi
 */
public class NavigableMapFloorCeilingDemo {

    // Domain entity that deliberately does NOT implement Comparable
    static final class UncomparableKey {
        final int id;
        UncomparableKey(int id) { this.id = id; }
        @Override
        public String toString() { return "Key(" + id + ")"; }
    }

    // Domain entity modeling an Order Book price point
    static final class OrderSummary {
        final long quantity;
        final String orderType;

        OrderSummary(long quantity, String orderType) {
            this.quantity = quantity;
            this.orderType = orderType;
        }

        @Override
        public String toString() {
            return String.format("[%s, Qty: %d]", orderType, quantity);
        }
    }

    public static void main(String[] args) {
        System.out.println("========================================================================");
        System.out.println(" 1. NAVIGABLEMAP BOUNDARY LOOKUPS (FLOOR, CEILING, LOWER, HIGHER)");
        System.out.println("========================================================================");

        // Populate a TreeMap representing discrete available timestamps (in seconds)
        NavigableMap<Integer, String> timeEvents = new TreeMap<>();
        timeEvents.put(100, "User Login");
        timeEvents.put(150, "Added to Cart");
        timeEvents.put(200, "Checkout Initiated");
        timeEvents.put(250, "Payment Completed");
        timeEvents.put(300, "Session Expired");

        System.out.println("Existing Event Timestamps: " + timeEvents.keySet());

        int queryTimestamp = 200; // Exact match exists
        System.out.println("\n--- Querying timestamp with exact match present (Key = " + queryTimestamp + ") ---");
        // floorKey(k)   : greatest key <= k
        // lowerKey(k)   : greatest key <  k
        // ceilingKey(k) : least key    >= k
        // higherKey(k)  : least key    >  k
        System.out.println("floorKey(200)   [<= 200] : " + timeEvents.floorKey(queryTimestamp));   // 200
        System.out.println("lowerKey(200)   [<  200] : " + timeEvents.lowerKey(queryTimestamp));   // 150
        System.out.println("ceilingKey(200) [>= 200] : " + timeEvents.ceilingKey(queryTimestamp)); // 200
        System.out.println("higherKey(200)  [>  200] : " + timeEvents.higherKey(queryTimestamp));  // 250

        int missingTimestamp = 175; // No exact match exists
        System.out.println("\n--- Querying timestamp with NO exact match (Key = " + missingTimestamp + ") ---");
        System.out.println("floorKey(175)   [<= 175] : " + timeEvents.floorKey(missingTimestamp));   // 150
        System.out.println("lowerKey(175)   [<  175] : " + timeEvents.lowerKey(missingTimestamp));   // 150
        System.out.println("ceilingKey(175) [>= 175] : " + timeEvents.ceilingKey(missingTimestamp)); // 200
        System.out.println("higherKey(175)  [>  175] : " + timeEvents.higherKey(missingTimestamp));  // 200

        System.out.println("\n--- Boundary Edge Conditions (Out of Bounds) ---");
        System.out.println("floorKey(50)    [< min]  : " + timeEvents.floorKey(50));               // null
        System.out.println("ceilingKey(400) [> max]  : " + timeEvents.ceilingKey(400));              // null

        System.out.println("\n========================================================================");
        System.out.println(" 2. RANGE VIEWS: SUBMAP, HEADMAP, TAILMAP (INCLUSIVE/EXCLUSIVE)");
        System.out.println("========================================================================");

        // Memory note: Range views do NOT allocate new tree structures!
        // They return an AscendingSubMap wrapper that references the original TreeMap.root.
        // Range checks are enforced lazily on traversal and eagerly on mutation.
        NavigableMap<Integer, String> fullMap = new TreeMap<>();
        for (int i = 10; i <= 100; i += 10) {
            fullMap.put(i, "Value-" + i);
        }
        System.out.println("Full Map Keys: " + fullMap.keySet());

        // SubMap with inclusive/exclusive control
        // Range: [30, 70) -> 30 inclusive, 70 exclusive
        NavigableMap<Integer, String> halfOpenSubMap = fullMap.subMap(30, true, 70, false);
        System.out.println("subMap(30, true, 70, false) [30 <= k < 70] : " + halfOpenSubMap.keySet());

        // Range: (30, 70] -> 30 exclusive, 70 inclusive
        NavigableMap<Integer, String> invertedSubMap = fullMap.subMap(30, false, 70, true);
        System.out.println("subMap(30, false, 70, true) [30 < k <= 70] : " + invertedSubMap.keySet());

        // headMap: all elements strictly before or up to boundary
        System.out.println("headMap(40, true)           [k <= 40]       : " + fullMap.headMap(40, true).keySet());
        System.out.println("headMap(40, false)          [k <  40]       : " + fullMap.headMap(40, false).keySet());

        // tailMap: all elements strictly from or after boundary
        System.out.println("tailMap(80, true)           [k >= 80]       : " + fullMap.tailMap(80, true).keySet());
        System.out.println("tailMap(80, false)          [k >  80]       : " + fullMap.tailMap(80, false).keySet());

        // Demonstration of bidirectional mutation between View and Master Tree
        halfOpenSubMap.put(35, "Value-35"); // Inside [30, 70)
        System.out.println("\nInserted 35 into subMap. Verified in master fullMap: " + fullMap.containsKey(35));

        try {
            System.out.println("Attempting to insert key 85 into subMap bounded to [30, 70)...");
            halfOpenSubMap.put(85, "Illegal-85");
        } catch (IllegalArgumentException e) {
            System.out.println("-> CAUGHT EXPECTED EXCEPTION: " + e.getMessage() + " (Key is out of range)");
        }

        System.out.println("\n========================================================================");
        System.out.println(" 3. EXTREMITIES & MUTATIVE POLLING (FIRST, LAST, POLL FIRST, POLL LAST)");
        System.out.println("========================================================================");

        NavigableMap<Integer, String> taskQueue = new TreeMap<>();
        taskQueue.put(3, "Low Priority Task");
        taskQueue.put(1, "Critical Sev-1 Bug");
        taskQueue.put(2, "Medium Feature Request");

        System.out.println("Current Task Priorities: " + taskQueue.keySet());
        System.out.println("firstKey() [Minimum] : " + taskQueue.firstKey());
        System.out.println("lastKey()  [Maximum] : " + taskQueue.lastKey());

        // pollFirstEntry() removes and returns the Entry with minimum key (leftmost node)
        // pollLastEntry() removes and returns the Entry with maximum key (rightmost node)
        Map.Entry<Integer, String> highestPri = taskQueue.pollFirstEntry();
        System.out.println("pollFirstEntry() removed: " + highestPri);
        System.out.println("Remaining task priorities: " + taskQueue.keySet());

        Map.Entry<Integer, String> lowestPri = taskQueue.pollLastEntry();
        System.out.println("pollLastEntry() removed: " + lowestPri);
        System.out.println("Remaining task priorities: " + taskQueue.keySet());

        System.out.println("\n========================================================================");
        System.out.println(" 4. ZERO-COPY REVERSE NAVIGATION: descendingMap() VIEW");
        System.out.println("========================================================================");

        NavigableMap<Integer, String> ascendingScores = new TreeMap<>();
        ascendingScores.put(100, "Bronze");
        ascendingScores.put(500, "Silver");
        ascendingScores.put(1000, "Gold");

        // descendingMap() returns a DescendingSubMap view backed by the same Red-Black tree
        // Iterations traverse right-to-left rather than left-to-right. No copying is done!
        NavigableMap<Integer, String> leaderBoard = ascendingScores.descendingMap();
        System.out.println("Ascending  View: " + ascendingScores.keySet());
        System.out.println("Descending View: " + leaderBoard.keySet());

        // Navigable operations invert naturally on descending map:
        // higherKey on descendingMap corresponds to lowerKey on ascendingMap!
        System.out.println("descendingMap.higherKey(500) (Next lower score): " + leaderBoard.higherKey(500)); // 100

        System.out.println("\n========================================================================");
        System.out.println(" 5. INTERVIEW SCENARIO: REAL-TIME LIMIT ORDER BOOK MATCHING ENGINE");
        System.out.println("========================================================================");

        // Financial Limit Order Book:
        // Bids: Sorted in descending order (highest buy price prioritized)
        // Asks: Sorted in ascending order (lowest sell price prioritized)
        NavigableMap<Double, OrderSummary> askBook = new TreeMap<>();
        askBook.put(100.25, new OrderSummary(500, "ASK"));
        askBook.put(100.50, new OrderSummary(1200, "ASK"));
        askBook.put(101.00, new OrderSummary(3000, "ASK"));

        System.out.println("Current Ask Book (Sell Orders): " + askBook);

        double marketBuyBudget = 100.75;
        System.out.println("Market Buyer willing to pay up to: $" + marketBuyBudget);

        // Find all executable sell orders: price <= $100.75
        NavigableMap<Double, OrderSummary> executableAsks = askBook.headMap(marketBuyBudget, true);
        System.out.println("Executable Orders matching criteria: " + executableAsks);

        // Find the absolute best execution price (lowest ask)
        Double bestAskPrice = askBook.firstKey();
        System.out.println("Best execution price on exchange: $" + bestAskPrice);

        System.out.println("\n========================================================================");
        System.out.println(" 6. CRITICAL TRAPS: ClassCastException & compareTo() Inconsistency");
        System.out.println("========================================================================");

        // Trap 1: Non-Comparable keys without explicit Comparator
        System.out.println("--- Trap 1: Inserting non-Comparable key without explicit Comparator ---");
        Map<UncomparableKey, String> badMap = new TreeMap<>();
        try {
            badMap.put(new UncomparableKey(1), "First");
            System.out.println("First element inserted successfully.");
            // In JDK 7+, inserting a second element triggers compareTo() cast and explodes!
            badMap.put(new UncomparableKey(2), "Second");
        } catch (ClassCastException e) {
            System.out.println("-> CAUGHT ClassCastException: " + e.getMessage());
            System.out.println("   Explanation: UncomparableKey does not implement Comparable<UncomparableKey>!");
        }

        // Fix: Provide explicit Comparator
        Map<UncomparableKey, String> fixedMap = new TreeMap<>(Comparator.comparingInt(k -> k.id));
        fixedMap.put(new UncomparableKey(1), "First");
        fixedMap.put(new UncomparableKey(2), "Second");
        System.out.println("Fixed Map with explicit Comparator succeeds: " + fixedMap.keySet());

        // Trap 2: Null Key Rejection
        System.out.println("\n--- Trap 2: Inserting null Key in TreeMap ---");
        try {
            NavigableMap<String, String> nullTestMap = new TreeMap<>();
            nullTestMap.put(null, "NullValue");
        } catch (NullPointerException e) {
            System.out.println("-> CAUGHT NullPointerException: TreeMap strictly disallows null keys!");
        }

        // Null Values ARE allowed:
        NavigableMap<String, String> allowedNullValueMap = new TreeMap<>();
        allowedNullValueMap.put("KeyWithNullVal", null);
        System.out.println("Null value allowed: containsKey='KeyWithNullVal', value=" + allowedNullValueMap.get("KeyWithNullVal"));
        System.out.println("========================================================================");
    }
}
