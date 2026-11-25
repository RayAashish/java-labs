package functional.lambdas;

import java.util.Comparator;
import java.util.List;

/**
 * LAMBDAS, TARGET TYPING, EFFECTIVELY FINAL & INVOKEDYNAMIC
 *
 * WHAT IT IS:
 *   Anonymous function syntax `(args) -> body` implementing any @FunctionalInterface.
 *
 * WHY IT MATTERS:
 *   Eliminates verbose 6-line anonymous inner classes AND runs faster on the JVM because
 *   javac emits `invokedynamic` + LambdaMetafactory instead of generating Outer$1.class files!
 */
public class LambdasAndInvokedynamicDemo {

    private String instanceTag = "OuterClassTag";

    public static void main(String[] args) {
        new LambdasAndInvokedynamicDemo().runDemo();
    }

    private void runDemo() {
        System.out.println("=== 1. Anonymous Inner Class (Verbose) vs Lambda ===");
        Comparator<String> oldStyle = new Comparator<String>() {
            @Override
            public int compare(String a, String b) {
                return Integer.compare(a.length(), b.length());
            }
        };
        Comparator<String> lambdaStyle = (a, b) -> Integer.compare(a.length(), b.length());
        System.out.println("Old vs Lambda agree? " + (oldStyle.compare("hi", "java") == lambdaStyle.compare("hi", "java")));

        System.out.println("\n=== 2. Meaning of 'this' Inside Lambda vs Anonymous Class ===");
        Runnable anon = new Runnable() {
            @Override
            public void run() {
                // Inside anonymous class, `this` refers to Anonymous Class instance!
                System.out.println("Anonymous class `this`: " + this.getClass().getName());
            }
        };
        Runnable lambda = () -> {
            // Inside lambda, `this` refers to the ENCLOSING outer object!
            System.out.println("Lambda `this` field:    " + this.instanceTag + " (" + this.getClass().getSimpleName() + ")");
        };
        anon.run();
        lambda.run();

        System.out.println("\n=== 3. Effectively Final Local Variable Capture ===");
        int taxPercent = 18; // Effectively final: assigned once and never modified
        List.of(100.0, 200.0).forEach(price ->
                System.out.printf("Price $%.2f + %d%% tax = $%.2f%n", price, taxPercent, price * (1 + taxPercent / 100.0)));
        // Uncommenting `taxPercent = 20;` anywhere in runDemo() causes a COMPILE ERROR!
    }
}
