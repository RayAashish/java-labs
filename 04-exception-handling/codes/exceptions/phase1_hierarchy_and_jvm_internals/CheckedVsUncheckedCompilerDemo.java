package exceptions.phase1_hierarchy_and_jvm_internals;

import java.io.IOException;
import java.sql.SQLException;

/**
 * Author: raysi
 * Package: com.raysi.exceptions.phase1_hierarchy_and_jvm_internals.codes
 *
 * Demonstrates:
 * 1. Static compile-time checking for Checked Exceptions (Exception & subclasses not RuntimeException).
 * 2. Unchecked Exception behavior (RuntimeException and Error subclasses).
 * 3. Type Erasure Sneaky Throws: Throwing checked exceptions without declaring them.
 * 4. Runtime type inspection and Throwable hierarchy proofs.
 */
public class CheckedVsUncheckedCompilerDemo {

    // Checked Exception: javac strictly enforces catch or declare
    public static class CustomCheckedException extends Exception {
        public CustomCheckedException(String message) {
            super(message);
        }
    }

    // Unchecked Exception: javac does NOT enforce catch or declare
    public static class CustomUncheckedException extends RuntimeException {
        public CustomUncheckedException(String message) {
            super(message);
        }
    }

    /**
     * Sneaky Throws idiom using Generics Type Erasure.
     * At compile time, T is unbounded/erased to Throwable, allowing any Throwable to be thrown
     * without javac requiring a 'throws' declaration in the calling code!
     */
    @SuppressWarnings("unchecked")
    public static <T extends Throwable> void sneakyThrow(Throwable t) throws T {
        throw (T) t;
    }

    public static void methodDeclaringChecked() throws CustomCheckedException, IOException {
        if (System.currentTimeMillis() % 2 == 0) {
            throw new CustomCheckedException("Checked exception occurred!");
        } else {
            throw new IOException("Simulated I/O checked exception!");
        }
    }

    public static void methodThrowingUnchecked() {
        // No throws clause required!
        throw new CustomUncheckedException("Unchecked runtime exception occurred without method signature declaration!");
    }

    public static void main(String[] args) {
        System.out.println("===============================================================");
        System.out.println("1. COMPILER PROOF: Checked vs Unchecked Exception Semantics");
        System.out.println("===============================================================");

        // Handling Unchecked Exception
        try {
            methodThrowingUnchecked();
        } catch (CustomUncheckedException e) {
            System.out.println("Caught Unchecked Exception: " + e.getMessage());
            System.out.println("Is instance of RuntimeException? " + (e instanceof RuntimeException));
            System.out.println("Is instance of Exception? " + (e instanceof Exception));
            System.out.println("Is instance of Throwable? " + (e instanceof Throwable));
        }

        // Handling Checked Exception
        try {
            methodDeclaringChecked();
        } catch (CustomCheckedException | IOException e) {
            System.out.println("\nCaught Checked Exception: " + e.getMessage());
            System.out.println("Is instance of RuntimeException? " + (e instanceof RuntimeException));
            System.out.println("Is instance of Exception? " + (e instanceof Exception));
        }

        System.out.println("\n===============================================================");
        System.out.println("2. SNEAKY THROWS: JVM Runtime vs javac Compile-Time Duality");
        System.out.println("===============================================================");

        try {
            System.out.println("Executing sneakyThrow with checked SQLException...");
            sneakyThrow(new SQLException("Simulated Database Connection Failure (Checked!)"));
        } catch (Throwable t) {
            System.out.println("Intercepted at runtime without compiler throws clause!");
            System.out.println("Actual runtime class: " + t.getClass().getName());
            System.out.println("Message: " + t.getMessage());
            System.out.println("Is SQLException? " + (t instanceof SQLException));
        }

        System.out.println("\n[SUCCESS] Phase 1 Compiler vs Runtime Demo Completed.");
    }
}
