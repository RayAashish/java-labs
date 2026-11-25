package lts.java21.sequenced;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.SequencedCollection;
import java.util.SequencedMap;
import java.util.SequencedSet;
import java.util.TreeSet;

/**
 * Java 21 LTS — Sequenced Collections, SequencedSet & SequencedMap (JEP 431).
 */
public class SequencedCollectionsDeepDiveDemo {

    public static void main(String[] args) {
        System.out.println("=== Java 21 Sequenced Collections (JEP 431) Demo ===");

        // -------------------------------------------------------------
        // 1. Unified First/Last/Reversed across List & LinkedHashSet
        // -------------------------------------------------------------
        SequencedCollection<String> steps = new ArrayList<>(List.of("init", "validate"));
        steps.addFirst("boot");
        steps.addLast("commit");

        System.out.printf("[1] List=%s | first=%s | last=%s%n", steps, steps.getFirst(), steps.getLast());

        // reversed() is a LIVE O(1) view — mutating rev mutates `steps` directly!
        SequencedCollection<String> revView = steps.reversed();
        revView.addFirst("shutdown"); // Adds to END of original `steps`!
        System.out.println("    After revView.addFirst('shutdown'): original steps = " + steps);

        // -------------------------------------------------------------
        // 2. SequencedSet Repositioning Semantics (LinkedHashSet) vs SortedSet Trap (TreeSet)
        // -------------------------------------------------------------
        SequencedSet<String> nodes = new LinkedHashSet<>(List.of("node-A", "node-B", "node-C"));
        nodes.addFirst("node-C"); // Moves node-C cleanly to front!
        System.out.println("\n[2] LinkedHashSet after addFirst('node-C'): " + nodes);

        SequencedSet<String> sorted = new TreeSet<>(List.of("B", "A", "C"));
        try {
            sorted.addFirst("Z"); // Sorted collections CANNOT force arbitrary element placement!
        } catch (UnsupportedOperationException expected) {
            System.out.println("    TreeSet.addFirst throws UnsupportedOperationException as designed! Sorted first=" + sorted.getFirst());
        }

        // -------------------------------------------------------------
        // 3. SequencedMap (LinkedHashMap Promotion & Polling)
        // -------------------------------------------------------------
        SequencedMap<String, Integer> scores = new LinkedHashMap<>();
        scores.putLast("alice", 10);
        scores.putLast("bob", 20);
        scores.putFirst("priority_vip", 99);

        System.out.printf("%n[3] SequencedMap=%s%n    firstEntry=%s | lastEntry=%s%n    reversed=%s%n",
                scores, scores.firstEntry(), scores.lastEntry(), scores.reversed());

        // Safe unmodifiable view preserving SequencedMap contract
        SequencedMap<String, Integer> readOnly = Collections.unmodifiableSequencedMap(scores);
        System.out.println("    Unmodifiable view first key: " + readOnly.sequencedKeySet().getFirst());

        // Empty check throws NoSuchElementException (unified!)
        try {
            List.of().getFirst();
        } catch (NoSuchElementException expected) {
            System.out.println("[4] Empty getFirst() cleanly throws NoSuchElementException");
        }
    }
}
