package collections.beginner;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.SequencedCollection;
import java.util.SequencedMap;

/**
 * Demonstrates Java 21+ Sequenced Collections (JEP 431):
 * Uniform getFirst(), getLast(), reversed() across List, Deque, Set, and Map.
 */
public class SequencedCollectionsDemo {

    public static void main(String[] args) {
        SequencedCollection<String> steps = new ArrayList<>(List.of("Init", "Validate", "Persist"));
        System.out.println("First:    " + steps.getFirst());
        System.out.println("Last:     " + steps.getLast());
        System.out.println("Reversed: " + steps.reversed()); // Live reversed view, no copy!

        SequencedMap<String, Integer> stages = new LinkedHashMap<>();
        stages.putFirst("Stage-1", 10);
        stages.putLast("Stage-2", 20);
        stages.putLast("Stage-3", 30);
        System.out.println("SequencedMap firstEntry: " + stages.firstEntry());
        System.out.println("SequencedMap reversed:   " + stages.reversed());
    }
}
