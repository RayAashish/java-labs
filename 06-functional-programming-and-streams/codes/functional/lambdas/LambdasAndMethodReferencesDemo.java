package functional.lambdas;

import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Demonstrates Functional Interfaces, Method References (4 forms),
 * Predicate/Function composition, and effectively final capture.
 */
public class LambdasAndMethodReferencesDemo {

    @FunctionalInterface
    interface PriceRule {
        double apply(double price);

        default PriceRule andThenRule(PriceRule next) {
            return price -> next.apply(this.apply(price));
        }
    }

    public static void main(String[] args) {
        System.out.println("=== 1. Custom @FunctionalInterface Composition ===");
        PriceRule tenPercentOff = p -> p * 0.90;
        PriceRule addTax = p -> p * 1.18;
        PriceRule finalBilling = tenPercentOff.andThenRule(addTax);
        System.out.printf("$100.00 after 10%% discount + 18%% tax = $%.2f%n", finalBilling.apply(100.0));

        System.out.println("\n=== 2. Built-in Predicates & Functions ===");
        Predicate<String> nonEmpty = s -> !s.isBlank();
        Predicate<String> startsWithJ = s -> s.startsWith("J");
        Predicate<String> validJavaTag = nonEmpty.and(startsWithJ);

        List<String> tags = List.of("Java", "Spring", "JVM", "", "Go");
        tags.stream().filter(validJavaTag).forEach(System.out::println);

        Function<String, String> trim = String::trim;           // Unbound instance method ref
        Function<String, Integer> len = String::length;         // Unbound instance method ref
        System.out.println("Trimmed length of '  Loom  ': " + trim.andThen(len).apply("  Loom  "));

        System.out.println("\n=== 3. Effectively Final Capture ===");
        int bonusMultiplier = 2; // Effectively final (never reassigned)
        Function<Integer, Integer> scale = score -> score * bonusMultiplier;
        System.out.println("Scaled score: " + scale.apply(45));
    }
}
