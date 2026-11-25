package collections.list.copyonwritearraylist;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Production-grade verification of CopyOnWriteArrayList:
 * 1. Safe concurrent iteration during active structural mutation (Zero CME)
 * 2. Snapshot isolation semantics (iterator sees frozen point-in-time view)
 * 3. UnsupportedOperationException on iterator mutation methods
 * 4. Comparative CME verification with standard ArrayList
 *
 * @author raysi
 */
public class ConcurrentListReadWriteDemo {

    public static void main(String[] args) throws InterruptedException {
        System.out.println("===============================================================");
        System.out.println("   COPYONWRITEARRAYLIST: SNAPSHOT ISOLATION & CONCURRENCY      ");
        System.out.println("   Author: raysi                                              ");
        System.out.println("===============================================================\n");

        demonstrateSnapshotIsolation();
        demonstrateIteratorRemoveThrowsUOE();
        demonstrateConcurrentReadWriteStress();
        demonstrateArrayListCMEComparison();
    }

    /**
     * Demonstrates that an Iterator sees the exact snapshot captured at creation time,
     * remaining completely oblivious to concurrent additions.
     */
    private static void demonstrateSnapshotIsolation() {
        System.out.println("--- 1. Snapshot Isolation Semantics ---");
        CopyOnWriteArrayList<String> cowList = new CopyOnWriteArrayList<>(List.of("A", "B", "C"));

        // Create iterator over snapshot ["A", "B", "C"]
        Iterator<String> snapshotIterator = cowList.iterator();

        // Mutate list structurally after iterator creation
        cowList.add("D");
        cowList.add("E");
        cowList.remove("A");

        System.out.println("Live CopyOnWriteArrayList after mutations: " + cowList);

        // Traverse the previously created iterator
        System.out.print("Iterator traversal output (captured snapshot): ");
        List<String> observedFromIterator = new ArrayList<>();
        while (snapshotIterator.hasNext()) {
            observedFromIterator.add(snapshotIterator.next());
        }
        System.out.println(observedFromIterator);

        System.out.println("Notice: Iterator traversed ['A', 'B', 'C']. It did NOT see 'D' or 'E',");
        System.out.println("and 'A' was still present in the snapshot. Zero CME thrown!\n");
    }

    /**
     * Demonstrates that iterator mutating operations are strictly unsupported.
     */
    private static void demonstrateIteratorRemoveThrowsUOE() {
        System.out.println("--- 2. Iterator Mutability Check (UnsupportedOperationException) ---");
        CopyOnWriteArrayList<String> list = new CopyOnWriteArrayList<>(List.of("Alpha", "Beta"));
        Iterator<String> it = list.iterator();

        it.next(); // Advances past "Alpha"
        try {
            it.remove();
            System.err.println("ERROR: Expected UnsupportedOperationException was not thrown!");
        } catch (UnsupportedOperationException e) {
            System.out.println("Caught expected UnsupportedOperationException on iterator.remove()!");
            System.out.println("Reason: COWIterator operates on an immutable snapshot array; mutating it is forbidden.\n");
        }
    }

    /**
     * Stresses CopyOnWriteArrayList with concurrent writer threads and reader threads.
     */
    private static void demonstrateConcurrentReadWriteStress() throws InterruptedException {
        System.out.println("--- 3. Concurrent Read-Write Stress Test (Zero Locks on Reads) ---");
        CopyOnWriteArrayList<Integer> cowList = new CopyOnWriteArrayList<>();
        for (int i = 0; i < 100; i++) cowList.add(i);

        ExecutorService executor = Executors.newFixedThreadPool(4);
        AtomicBoolean running = new AtomicBoolean(true);

        // Reader task: continuously iterates without locks
        Callable<Long> readerTask = () -> {
            long reads = 0;
            while (running.get()) {
                for (int val : cowList) {
                    reads++;
                }
            }
            return reads;
        };

        // Writer task: appends elements
        Runnable writerTask = () -> {
            for (int i = 100; i < 200; i++) {
                cowList.add(i);
                try {
                    Thread.sleep(1);
                } catch (InterruptedException ignored) {}
            }
        };

        Future<Long> reader1 = executor.submit(readerTask);
        Future<Long> reader2 = executor.submit(readerTask);
        Future<Long> reader3 = executor.submit(readerTask);
        Future<?> writer = executor.submit(writerTask);

        // Wait for writer to complete
        try {
            writer.get(3, TimeUnit.SECONDS);
        } catch (Exception e) {
            e.printStackTrace();
        }
        running.set(false);

        try {
            long totalReads = reader1.get() + reader2.get() + reader3.get();
            System.out.printf("Stress test completed successfully! Total lock-free reads executed: %,d\n", totalReads);
            System.out.println("Final CopyOnWriteArrayList size: " + cowList.size());
            System.out.println("No ConcurrentModificationExceptions occurred during heavy concurrent iteration.\n");
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            executor.shutdownNow();
        }
    }

    /**
     * Demonstrates that standard ArrayList fails fast with CME under concurrent mutation.
     */
    private static void demonstrateArrayListCMEComparison() {
        System.out.println("--- 4. ArrayList CME Comparison Verification ---");
        ArrayList<String> standardList = new ArrayList<>(List.of("One", "Two", "Three"));

        Iterator<String> it = standardList.iterator();
        standardList.add("Four"); // Increments parent modCount

        try {
            it.next(); // Checks: if (modCount != expectedModCount) throw CME
            System.err.println("ERROR: ArrayList unexpectedly failed to throw CME!");
        } catch (ConcurrentModificationException e) {
            System.out.println("ArrayList threw ConcurrentModificationException as expected when mutated during iteration.");
            System.out.println("This highlights the exact problem CopyOnWriteArrayList was engineered to solve.\n");
        }
    }
}
