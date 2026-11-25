package collections.set.treeset;

import java.util.*;

/**
 * ============================================================================
 * Production-Grade Demonstration: TreeSet, Red-Black Tree & NavigableSet Views
 * Author: raysi
 *
 * This runnable class provides deep source-level verification of:
 * 1. NavigableSet boundary queries: lower, floor, ceiling, higher
 * 2. Queue-like polling: pollFirst() and pollLast()
 * 3. Live subSet, headSet, and tailSet range views and boundary validation
 * 4. Bidirectional navigation via descendingSet() and descendingIterator()
 * 5. ClassCastException when inserting non-Comparable elements without Comparator
 * 6. The compareTo() vs equals() inconsistency trap (silent duplicate drops)
 * 7. Case-insensitive string TreeSet using custom Comparator
 * ============================================================================
 */
public class TreeSetRangeViewsDemo {

    public static void main(String[] args) {
        System.out.println("================================================================================");
        System.out.println("          TREESET: RED-BLACK TREE INTERNALS & NAVIGABLESET RANGE VIEWS          ");
        System.out.println("================================================================================");

        verifyNavigableBoundaryQueries();
        verifyPollingOperations();
        verifyLiveSubSetViewsAndBoundaryValidation();
        verifyDescendingSetView();
        verifyNonComparableAndNullRejection();
        verifyCompareToEqualsInconsistencyTrap();
        verifyCustomComparatorCaseInsensitive();

        System.out.println("\nAll TreeSet Internals & Behavioral Invariants Verified Successfully.");
    }

    /**
     * 1. NavigableSet boundary queries: lower, floor, ceiling, higher.
     * Demonstrates logarithmic binary search tree nearest-neighbor queries.
     */
    private static void verifyNavigableBoundaryQueries() {
        System.out.println("\n--- 1. Boundary Queries: lower, floor, ceiling, higher ---");

        NavigableSet<Integer> tree = new TreeSet<>(Arrays.asList(10, 20, 30, 40, 50));
        System.out.println("Base TreeSet: " + tree);

        // Queries around 30 (exact key present in tree)
        System.out.println("lower(30)   [strictly < 30]  : " + tree.lower(30));   // 20
        System.out.println("floor(30)   [closest <= 30]  : " + tree.floor(30));   // 30
        System.out.println("ceiling(30) [closest >= 30]  : " + tree.ceiling(30)); // 30
        System.out.println("higher(30)  [strictly > 30]  : " + tree.higher(30));  // 40

        // Queries around 25 (key absent from tree)
        System.out.println("lower(25)   [strictly < 25]  : " + tree.lower(25));   // 20
        System.out.println("floor(25)   [closest <= 25]  : " + tree.floor(25));   // 20
        System.out.println("ceiling(25) [closest >= 25]  : " + tree.ceiling(25)); // 30
        System.out.println("higher(25)  [strictly > 25]  : " + tree.higher(25));  // 30

        // Edge boundaries
        System.out.println("lower(10)   [below min]      : " + tree.lower(10));   // null
        System.out.println("higher(50)  [above max]      : " + tree.higher(50));  // null
    }

    /**
     * 2. Polling Operations: pollFirst() and pollLast().
     * Retrieves and deletes minimum and maximum elements in O(log N) time.
     */
    private static void verifyPollingOperations() {
        System.out.println("\n--- 2. Polling Operations: pollFirst() & pollLast() ---");

        NavigableSet<Integer> scores = new TreeSet<>(Arrays.asList(88, 42, 99, 15, 73));
        System.out.println("TreeSet Before Polling: " + scores);

        Integer lowest = scores.pollFirst();
        Integer highest = scores.pollLast();

        System.out.println("pollFirst() unlinked minimum: " + lowest);
        System.out.println("pollLast()  unlinked maximum: " + highest);
        System.out.println("TreeSet After Polling:        " + scores);
        System.out.println("Remaining first(): " + scores.first() + " | Remaining last(): " + scores.last());
    }

    /**
     * 3. SubSet / HeadSet / TailSet Live Views & Boundary Enforcement.
     * Proves that range views do NOT duplicate data and strictly enforce bounds.
     */
    private static void verifyLiveSubSetViewsAndBoundaryValidation() {
        System.out.println("\n--- 3. Live Range Views & Boundary Validation ---");

        NavigableSet<Integer> tree = new TreeSet<>(Arrays.asList(10, 20, 30, 40, 50, 60, 70));
        System.out.println("Original Base TreeSet: " + tree);

        // Create bounded views with inclusive/exclusive toggles
        NavigableSet<Integer> sub = tree.subSet(20, true, 50, true); // [20, 50]
        NavigableSet<Integer> head = tree.headSet(40, false);        // (< 40)
        NavigableSet<Integer> tail = tree.tailSet(40, true);         // (>= 40)

        System.out.println("subSet [20, 50] (both inclusive): " + sub);
        System.out.println("headSet (< 40)  (exclusive):       " + head);
        System.out.println("tailSet (>= 40) (inclusive):       " + tail);

        // View is LIVE: Mutating the subSet view reflects immediately in the backing tree
        sub.add(25);
        System.out.println("\nAdded 25 through subSet view.");
        System.out.println("Base TreeSet reflects addition: " + tree);

        // View is LIVE: Mutating the base tree within the range reflects in the view
        tree.add(35);
        System.out.println("Added 35 directly to base TreeSet.");
        System.out.println("subSet view reflects addition:  " + sub);

        // View Boundary Validation: Adding out-of-range elements throws IllegalArgumentException
        try {
            sub.add(15); // Below lower bound (20)
            throw new AssertionError("Failed: Allowed out-of-bounds addition (15 < 20)!");
        } catch (IllegalArgumentException iae) {
            System.out.println("Confirmed: subSet rejected out-of-range key 15: " + iae.getMessage());
        }

        try {
            sub.add(55); // Above upper bound (50)
            throw new AssertionError("Failed: Allowed out-of-bounds addition (55 > 50)!");
        } catch (IllegalArgumentException iae) {
            System.out.println("Confirmed: subSet rejected out-of-range key 55: " + iae.getMessage());
        }
    }

    /**
     * 4. DescendingSet View: Bidirectional In-Order Traversal.
     */
    private static void verifyDescendingSetView() {
        System.out.println("\n--- 4. DescendingSet View ---");

        NavigableSet<String> letters = new TreeSet<>(Arrays.asList("Alpha", "Beta", "Gamma", "Delta"));
        System.out.println("Ascending TreeSet:       " + letters);

        NavigableSet<String> descending = letters.descendingSet();
        System.out.println("descendingSet() View:    " + descending);

        // Descending iterator
        System.out.print("descendingIterator():    ");
        Iterator<String> descIter = letters.descendingIterator();
        while (descIter.hasNext()) {
            System.out.print(descIter.next() + " ");
        }
        System.out.println();

        // Mutating descending view
        descending.add("Omega");
        System.out.println("Added 'Omega' via descendingSet. Original TreeSet: " + letters);
    }

    /**
     * 5. Null Rejection & Non-Comparable ClassCastException.
     */
    private static void verifyNonComparableAndNullRejection() {
        System.out.println("\n--- 5. Null Rejection & ClassCastException ---");

        TreeSet<String> set = new TreeSet<>();

        // Null insertion:
        // Unlike HashSet, TreeSet does not permit null elements because comparing against null fails.
        try {
            set.add(null);
            throw new AssertionError("Failed: TreeSet allowed null addition!");
        } catch (NullPointerException npe) {
            System.out.println("Confirmed: TreeSet throws NullPointerException on null insertion.");
        }

        // Non-Comparable insertion without Comparator
        class SensorData {
            final int reading;
            SensorData(int reading) { this.reading = reading; }
        }

        TreeSet<SensorData> sensorTree = new TreeSet<>();
        try {
            sensorTree.add(new SensorData(100));
            throw new AssertionError("Failed: Non-comparable element allowed without Comparator!");
        } catch (ClassCastException cce) {
            System.out.println("Confirmed: Inserting non-Comparable element throws ClassCastException: " +
                    cce.getMessage());
        }
    }

    /**
     * 6. Fatal compareTo() vs equals() Inconsistency Trap:
     * TreeSet relies exclusively on compare() == 0, completely ignoring equals()!
     */
    private static void verifyCompareToEqualsInconsistencyTrap() {
        System.out.println("\n--- 6. The compareTo() vs equals() Inconsistency Trap ---");

        // Defective class where compareTo compares ONLY on studentId,
        // but equals compares studentId AND studentName!
        class Student implements Comparable<Student> {
            final int studentId;
            final String studentName;

            Student(int studentId, String studentName) {
                this.studentId = studentId;
                this.studentName = studentName;
            }

            @Override
            public int compareTo(Student o) {
                // Flawed compareTo: compares ONLY ID!
                return Integer.compare(this.studentId, o.studentId);
            }

            @Override
            public boolean equals(Object o) {
                if (this == o) return true;
                if (!(o instanceof Student student)) return false;
                return studentId == student.studentId && Objects.equals(studentName, student.studentName);
            }

            @Override
            public int hashCode() {
                return Objects.hash(studentId, studentName);
            }

            @Override
            public String toString() {
                return "Student{" + "id=" + studentId + ", name='" + studentName + '\'' + '}';
            }
        }

        Student s1 = new Student(101, "Alice");
        Student s2 = new Student(101, "Bob"); // Same ID, DIFFERENT NAME!

        System.out.println("Student 1: " + s1);
        System.out.println("Student 2: " + s2);
        System.out.println("s1.equals(s2):     " + s1.equals(s2) + " (Objects are NOT equal!)");
        System.out.println("s1.compareTo(s2):  " + s1.compareTo(s2) + " (compareTo treats as EQUAL!)");

        // 1. HashSet uses equals() -> stores BOTH students!
        Set<Student> hashSet = new HashSet<>();
        hashSet.add(s1);
        hashSet.add(s2);
        System.out.println("HashSet size: " + hashSet.size() + " -> Correctly preserved both distinct students.");

        // 2. TreeSet uses compareTo() -> DROPS Student 2 SILENTLY!
        Set<Student> treeSet = new TreeSet<>();
        boolean added1 = treeSet.add(s1);
        boolean added2 = treeSet.add(s2);

        System.out.println("TreeSet added s1: " + added1);
        System.out.println("TreeSet added s2: " + added2 + " (SILENTLY DISCARDED AS DUPLICATE!)");
        System.out.println("TreeSet size:     " + treeSet.size() + " (Data Loss: Student Bob was dropped!)");
        System.out.println("ARCHITECTURAL RULE: compareTo() MUST be consistent with equals()!");
    }

    /**
     * 7. Custom Comparator: Case-Insensitive TreeSet.
     */
    private static void verifyCustomComparatorCaseInsensitive() {
        System.out.println("\n--- 7. Custom Comparator: Case-Insensitive TreeSet ---");

        TreeSet<String> caseInsensitiveTree = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        caseInsensitiveTree.add("Oracle");
        caseInsensitiveTree.add("google");
        caseInsensitiveTree.add("MICROSOFT");

        System.out.println("TreeSet with CASE_INSENSITIVE_ORDER: " + caseInsensitiveTree);

        // Inserting "oracle" (lowercase) collides with "Oracle"
        boolean added = caseInsensitiveTree.add("oracle");
        System.out.println("Attempt to add 'oracle' (lowercase): " + added + " (rejected as duplicate!)");
        System.out.println("contains('GOOGLE'): " + caseInsensitiveTree.contains("GOOGLE") + " (matched case-insensitively)");
    }
}
