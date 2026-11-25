package exceptions.phase1_hierarchy_and_jvm_internals;

/**
 * Author: raysi
 * Package: com.raysi.exceptions.phase1_hierarchy_and_jvm_internals.codes
 *
 * Demonstrates:
 * 1. JVM Call Stack Frame Unwinding across multiple method frames.
 * 2. How unhandled exception in child frames immediately terminates child execution.
 * 3. Inspection of StackTraceElement attributes (class, method, file, line number).
 */
public class StackUnwindingAndCallStackDemo {

    public static void main(String[] args) {
        System.out.println("===============================================================");
        System.out.println("JVM STACK UNWINDING & FRAME INSPECTION DEMO");
        System.out.println("===============================================================");
        System.out.println("[Frame 0: main()] Starting execution.");

        try {
            layerA();
        } catch (IllegalStateException e) {
            System.out.println("\n[Frame 0: main()] Intercepted unwound exception!");
            System.out.println("Exception Class: " + e.getClass().getName());
            System.out.println("Exception Message: " + e.getMessage());

            System.out.println("\n--- Traversing StackTraceElements Recorded by HotSpot ---");
            StackTraceElement[] stack = e.getStackTrace();
            for (int i = 0; i < stack.length; i++) {
                StackTraceElement frame = stack[i];
                System.out.printf("  Frame [%02d] -> %s.%s() [%s:%d]%n",
                        i,
                        frame.getClassName(),
                        frame.getMethodName(),
                        frame.getFileName(),
                        frame.getLineNumber());
            }

            System.out.println("\nObservation: Notice how layerC, layerB, and layerA were sequentially unwound!");
        }

        System.out.println("\n[Frame 0: main()] Program execution resumed normally after handling.");
    }

    private static void layerA() {
        System.out.println("  [Frame 1: layerA()] Invoking layerB()...");
        layerB();
        System.out.println("  [Frame 1: layerA()] This line will NEVER print due to stack unwinding!");
    }

    private static void layerB() {
        System.out.println("    [Frame 2: layerB()] Invoking layerC()...");
        layerC();
        System.out.println("    [Frame 2: layerB()] This line will NEVER print due to stack unwinding!");
    }

    private static void layerC() {
        System.out.println("      [Frame 3: layerC()] Root execution failure occurring now!");
        throw new IllegalStateException("Simulated critical state corruption in layerC!");
    }
}
