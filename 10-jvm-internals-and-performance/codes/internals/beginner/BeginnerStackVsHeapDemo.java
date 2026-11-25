package internals.beginner;

/**
 * Beginner demonstration of Stack frames, Heap allocations,
 * Runtime memory stats, and JVM system properties.
 */
public class BeginnerStackVsHeapDemo {

    public static void main(String[] args) {
        Runtime runtime = Runtime.getRuntime();
        System.out.println("=== JVM Environment ===");
        System.out.println("Java Version:      " + System.getProperty("java.version"));
        System.out.println("Available Cores:   " + runtime.availableProcessors());
        System.out.printf("Max Heap (-Xmx):   %.1f MB%n", runtime.maxMemory() / (1024.0 * 1024.0));
        System.out.printf("Total Heap Now:    %.1f MB%n", runtime.totalMemory() / (1024.0 * 1024.0));

        // Primitive lives directly inside main's Stack frame
        int stackValue = 42;
        // Object payload lives on the Heap; 'heapText' pointer lives on Stack
        String heapText = new String("Allocated on Heap");
        System.out.println("Stack primitive=" + stackValue + " | Heap string='" + heapText + "'");
    }
}
