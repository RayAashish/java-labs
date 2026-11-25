package collections.beginner;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Beginner-friendly tour of the 4 workhorse Java Collections:
 * ArrayList, HashSet, HashMap, and ArrayDeque.
 */
public class BeginnerCollectionsDemo {

    public static void main(String[] args) {
        System.out.println("=== 1. List (Ordered, Allows Duplicates) ===");
        List<String> tasks = new ArrayList<>();
        tasks.add("Compile");
        tasks.add("Test");
        tasks.add("Compile");
        System.out.println("List: " + tasks + " | index[1]=" + tasks.get(1));

        System.out.println("\n=== 2. Set (Unique Elements Only) ===");
        Set<String> uniqueTags = new HashSet<>(tasks);
        System.out.println("Deduplicated HashSet: " + uniqueTags);

        System.out.println("\n=== 3. Map (Key -> Value Dictionary) ===");
        Map<String, Integer> scores = new HashMap<>();
        scores.put("Alice", 95);
        scores.put("Bob", 88);
        scores.merge("Alice", 5, Integer::sum); // Safe increment!
        System.out.println("Scores map: " + scores + " | Charlie=" + scores.getOrDefault("Charlie", 0));

        System.out.println("\n=== 4. Deque (Modern Stack & Queue via ArrayDeque) ===");
        Deque<String> queue = new ArrayDeque<>();
        queue.offer("Job-1");
        queue.offer("Job-2");
        System.out.println("FIFO poll: " + queue.poll() + " -> next: " + queue.peek());
    }
}
