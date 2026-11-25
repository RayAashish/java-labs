package collections.foundations;

import java.math.BigDecimal;
import java.util.*;

/**
 * ComparableVsComparatorDemo — Deep Dive into Comparison Contracts, Edge Cases & Performance Traps.
 *
 * Demonstrates:
 * 1. Natural Ordering via Comparable<T> (Domain-level total ordering).
 * 2. External Strategy Ordering via Comparator<T> (Decoupled sorting strategies).
 * 3. Modern Comparator Combinator Chaining: thenComparing(), reversed(), nullsFirst(), nullsLast().
 * 4. The Critical Integer Subtraction Underflow/Overflow Trap (a - b bug).
 * 5. Violating Consistency with equals(): Demonstrating how TreeSet drops valid elements while HashSet preserves them.
 * 6. Non-obvious behavior: ClassCastException when inserting non-comparable objects into TreeSet.
 *
 * Author: raysi
 */
public class ComparableVsComparatorDemo {

    /**
     * Domain model: OrderItem.
     * Implements Comparable<OrderItem> to establish Natural Ordering by skuId.
     */
    public static class OrderItem implements Comparable<OrderItem> {
        private final String skuId;
        private final String productName;
        private final int quantity;
        private final long unitPriceCents;

        public OrderItem(String skuId, String productName, int quantity, long unitPriceCents) {
            this.skuId = Objects.requireNonNull(skuId, "skuId cannot be null");
            this.productName = productName;
            this.quantity = quantity;
            this.unitPriceCents = unitPriceCents;
        }

        public String getSkuId() { return skuId; }
        public String getProductName() { return productName; }
        public int getQuantity() { return quantity; }
        public long getUnitPriceCents() { return unitPriceCents; }

        /**
         * NATURAL ORDER CONTRACT:
         * Primary comparison by skuId (alphabetical ascending).
         */
        @Override
        public int compareTo(OrderItem other) {
            Objects.requireNonNull(other, "Cannot compare to null OrderItem");
            return this.skuId.compareTo(other.skuId);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof OrderItem that)) return false;
            return quantity == that.quantity &&
                   unitPriceCents == that.unitPriceCents &&
                   Objects.equals(skuId, that.skuId) &&
                   Objects.equals(productName, that.productName);
        }

        @Override
        public int hashCode() {
            return Objects.hash(skuId, productName, quantity, unitPriceCents);
        }

        @Override
        public String toString() {
            return String.format("{sku='%s', name='%s', qty=%d, price=$%.2f}",
                    skuId, productName, quantity, unitPriceCents / 100.0);
        }
    }

    /**
     * Defective entity modeling a contract violation:
     * compareTo is INCONSISTENT with equals().
     * Two accounts with different accountNumbers and owners are declared "equal" by compareTo
     * simply because they have identical credit scores!
     */
    public static class DefectiveAccount implements Comparable<DefectiveAccount> {
        private final String accountNumber;
        private final String owner;
        private final int creditScore;

        public DefectiveAccount(String accountNumber, String owner, int creditScore) {
            this.accountNumber = accountNumber;
            this.owner = owner;
            this.creditScore = creditScore;
        }

        @Override
        public int compareTo(DefectiveAccount o) {
            // BUGGY CONTRACT: Evaluates solely creditScore!
            // When creditScores match, returns 0, claiming the accounts are identical!
            return Integer.compare(this.creditScore, o.creditScore);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof DefectiveAccount that)) return false;
            return creditScore == that.creditScore &&
                   Objects.equals(accountNumber, that.accountNumber) &&
                   Objects.equals(owner, that.owner);
        }

        @Override
        public int hashCode() {
            return Objects.hash(accountNumber, owner, creditScore);
        }

        @Override
        public String toString() {
            return String.format("[%s: %s (Score: %d)]", accountNumber, owner, creditScore);
        }
    }

    /**
     * Non-comparable class to demonstrate ClassCastException in sorted collections.
     */
    public static class RawPayload {
        private final int id;
        public RawPayload(int id) { this.id = id; }
        @Override public String toString() { return "RawPayload(" + id + ")"; }
    }

    public static void main(String[] args) {
        System.out.println("======================================================================");
        System.out.println(" 1. NATURAL ORDERING VIA COMPARABLE<T>");
        System.out.println("======================================================================");
        verifyNaturalOrdering();

        System.out.println("\n======================================================================");
        System.out.println(" 2. CUSTOM ORDERING & MULTI-TIER COMPARATOR CHAINING");
        System.out.println("======================================================================");
        verifyComparatorChaining();

        System.out.println("\n======================================================================");
        System.out.println(" 3. THE INFAMOUS INTEGER SUBTRACTION OVERFLOW TRAP");
        System.out.println("======================================================================");
        verifyIntegerSubtractionTrap();

        System.out.println("\n======================================================================");
        System.out.println(" 4. BROKEN TREESET: VIOLATING 'CONSISTENCY WITH EQUALS'");
        System.out.println("======================================================================");
        verifyInconsistencyWithEquals();

        System.out.println("\n======================================================================");
        System.out.println(" 5. NULL-SAFETY STRATEGIES: nullsFirst() AND nullsLast()");
        System.out.println("======================================================================");
        verifyNullHandlingComparators();

        System.out.println("\n======================================================================");
        System.out.println(" 6. NON-COMPARABLE ELEMENT TRAP: CLASSCASTEXCEPTION IN TREESET");
        System.out.println("======================================================================");
        verifyClassCastExceptionInTreeSet();
    }

    private static void verifyNaturalOrdering() {
        List<OrderItem> items = new ArrayList<>();
        items.add(new OrderItem("SKU-900", "Mechanical Keyboard", 1, 15000));
        items.add(new OrderItem("SKU-100", "USB-C Monitor Cable", 2, 2000));
        items.add(new OrderItem("SKU-500", "Ergonomic Mouse", 1, 8500));

        System.out.println("Before sort (insertion order):");
        items.forEach(i -> System.out.println("  " + i));

        // Uses Comparable.compareTo() under the hood
        Collections.sort(items);

        System.out.println("\nAfter natural sort (skuId ascending):");
        items.forEach(i -> System.out.println("  " + i));
    }

    private static void verifyComparatorChaining() {
        List<OrderItem> items = new ArrayList<>(Arrays.asList(
            new OrderItem("SKU-1", "Laptop Stand", 2, 4500),
            new OrderItem("SKU-2", "Webcam HD", 5, 4500),
            new OrderItem("SKU-3", "USB Hub", 1, 2500),
            new OrderItem("SKU-4", "Noise Canceling Headset", 5, 12000),
            new OrderItem("SKU-5", "Webcam 4K", 2, 4500)
        ));

        /*
         * HIGH-PERFORMANCE COMPARATOR CHAINING:
         * Strategy:
         * 1. Quantity DESCENDING (using comparingInt to avoid boxing primitives into Integer)
         * 2. UnitPrice ASCENDING (using comparingLong to avoid boxing longs)
         * 3. ProductName ASCENDING (alphabetical fallback)
         */
        Comparator<OrderItem> compositeComparator = Comparator
            .comparingInt(OrderItem::getQuantity).reversed()
            .thenComparingLong(OrderItem::getUnitPriceCents)
            .thenComparing(OrderItem::getProductName);

        items.sort(compositeComparator);

        System.out.println("Sorted via composite comparator (Qty DESC -> Price ASC -> Name ASC):");
        items.forEach(i -> System.out.println("  " + i));
    }

    private static void verifyIntegerSubtractionTrap() {
        // High-stakes scenario: Comparing timestamps, metrics, or integer IDs
        int valueA = Integer.MIN_VALUE; // -2147483648
        int valueB = 1;

        // Buggy subtraction comparator
        Comparator<Integer> buggy = (a, b) -> a - b;

        int buggyResult = buggy.compare(valueA, valueB);
        System.out.println("Evaluating Integer.MIN_VALUE (-2147483648) vs 1:");
        System.out.println("  Buggy Subtraction (a - b) returned: " + buggyResult);
        System.out.println("  Did buggy comparator claim MIN_VALUE > 1? " + (buggyResult > 0) + " [FATAL BUG!]");

        // Safe integer comparison
        Comparator<Integer> safe = Integer::compare;
        int safeResult = safe.compare(valueA, valueB);
        System.out.println("  Integer.compare(a, b) returned: " + safeResult);
        System.out.println("  Correctly identified MIN_VALUE < 1? " + (safeResult < 0) + " [CORRECT]");
    }

    private static void verifyInconsistencyWithEquals() {
        // Two distinct bank accounts for two different owners, but both with credit score 720
        DefectiveAccount acc1 = new DefectiveAccount("ACCT-1001", "Alice Smith", 720);
        DefectiveAccount acc2 = new DefectiveAccount("ACCT-1002", "Bob Jones", 720);

        System.out.println("Account 1: " + acc1);
        System.out.println("Account 2: " + acc2);
        System.out.println("Are the accounts equal via equals()? " + acc1.equals(acc2));
        System.out.println("What does compareTo() return? " + acc1.compareTo(acc2));

        // 1. Store in HashSet (Governed by hashCode() and equals())
        Set<DefectiveAccount> hashSet = new HashSet<>();
        hashSet.add(acc1);
        hashSet.add(acc2);
        System.out.println("\nHashSet contents (size=" + hashSet.size() + "):");
        hashSet.forEach(a -> System.out.println("  " + a));

        // 2. Store in TreeSet (Governed solely by compareTo() == 0)
        Set<DefectiveAccount> treeSet = new TreeSet<>();
        boolean added1 = treeSet.add(acc1);
        boolean added2 = treeSet.add(acc2); // SILENTLY REJECTED!
        System.out.println("\nTreeSet add(acc1): " + added1);
        System.out.println("TreeSet add(acc2): " + added2 + " <-- SILENT DATA LOSS!");
        System.out.println("TreeSet contents (size=" + treeSet.size() + "):");
        treeSet.forEach(a -> System.out.println("  " + a));
        System.out.println("CONCLUSION: TreeSet dropped Bob Jones because compareTo() returned 0,");
        System.out.println("violating the Set contract that requires uniqueness based on equivalence!");
    }

    private static void verifyNullHandlingComparators() {
        List<String> names = new ArrayList<>(Arrays.asList("Charlie", null, "Alice", null, "Bob"));

        System.out.println("List with nulls: " + names);

        // Sorting with nullsFirst
        names.sort(Comparator.nullsFirst(Comparator.naturalOrder()));
        System.out.println("Sorted with nullsFirst: " + names);

        // Sorting with nullsLast and reverseOrder
        names.sort(Comparator.nullsLast(Comparator.reverseOrder()));
        System.out.println("Sorted with nullsLast(reverseOrder): " + names);
    }

    private static void verifyClassCastExceptionInTreeSet() {
        System.out.println("Attempting to insert non-comparable objects into TreeSet without Comparator...");
        Set<RawPayload> tree = new TreeSet<>();
        try {
            tree.add(new RawPayload(1));
            tree.add(new RawPayload(2));
            System.err.println("FAILED: Expected ClassCastException was not thrown!");
        } catch (ClassCastException e) {
            System.out.println("PASSED: Caught expected ClassCastException: " + e.getMessage());
        }

        // Fix: Provide an explicit Comparator
        Set<RawPayload> fixedTree = new TreeSet<>(Comparator.comparingInt(p -> p.id));
        fixedTree.add(new RawPayload(10));
        fixedTree.add(new RawPayload(5));
        System.out.println("Fixed TreeSet with custom Comparator: " + fixedTree);
    }
}
