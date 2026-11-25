package exceptions.phase2_control_flow_and_traps;

/**
 * Author: raysi
 * Package: com.raysi.exceptions.phase2_control_flow_and_traps.codes
 *
 * Demonstrates:
 * 1. Primitive Return vs Finally reassignment.
 * 2. Reference Object Return vs Finally state mutation.
 * 3. The Dangerous Finally Return Trap: Overriding and swallowing an ArithmeticException.
 * 4. Finally block execution ordering when catch also throws.
 */
public class TryCatchFinallyReturnOverrideTrap {

    static class MutableContainer {
        int payload;

        public MutableContainer(int payload) {
            this.payload = payload;
        }
    }

    public static void main(String[] args) {
        System.out.println("===============================================================");
        System.out.println("CASE 1: Primitive Return vs Finally Modification");
        System.out.println("===============================================================");
        int primitiveResult = testPrimitiveReturn();
        System.out.println("Returned value: " + primitiveResult + " (Expected 10, NOT 99)");

        System.out.println("\n===============================================================");
        System.out.println("CASE 2: Reference Object Return vs Finally Modification");
        System.out.println("===============================================================");
        MutableContainer container = testReferenceReturn();
        System.out.println("Returned container.payload: " + container.payload + " (Expected 999)");

        System.out.println("\n===============================================================");
        System.out.println("CASE 3: The Fatal 'Finally Return' Trap (Swallowing Exception)");
        System.out.println("===============================================================");
        int swallowedResult = testFinallySwallowException();
        System.out.println("Returned value: " + swallowedResult + " (Notice: Exception completely vanished!)");

        System.out.println("\n===============================================================");
        System.out.println("CASE 4: Catch Block Throw Overridden by Finally Throw");
        System.out.println("===============================================================");
        try {
            testDualThrow();
        } catch (Exception e) {
            System.out.println("Caught Exception: " + e.getMessage());
            System.out.println("(Notice: Original NullPointerException was wiped out by Finally IllegalStateException!)");
        }
    }

    public static int testPrimitiveReturn() {
        int x = 10;
        try {
            return x; // Evaluates x (10) and stores in hidden return register
        } finally {
            x = 99;   // Modifies local variable, but return register retains 10!
            System.out.println("  [finally] Reassigned local variable x = 99");
        }
    }

    public static MutableContainer testReferenceReturn() {
        MutableContainer box = new MutableContainer(100);
        try {
            return box; // Stores heap reference in return register
        } finally {
            box.payload = 999; // Mutates data on heap at that reference!
            System.out.println("  [finally] Mutated box.payload = 999 on heap");
        }
    }

    public static int testFinallySwallowException() {
        try {
            System.out.println("  [try] Dividing 100 by 0 (ArithmeticException)...");
            return 100 / 0;
        } finally {
            System.out.println("  [finally] Executing return 500; wiping out active exception!");
            return 500; // ANTI-PATTERN: Overrides active exception and returns normally!
        }
    }

    public static void testDualThrow() {
        try {
            throw new NullPointerException("PRIMARY EXCEPTION from try block");
        } catch (NullPointerException e) {
            throw new IllegalArgumentException("SECONDARY EXCEPTION from catch block");
        } finally {
            // Overwrites both previous exceptions
            throw new IllegalStateException("TERTIARY EXCEPTION from finally block (Masks all prior exceptions!)");
        }
    }
}
