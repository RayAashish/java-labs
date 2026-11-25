package lts.java08.lambdas;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntPredicate;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

/**
 * Java 8 LTS — Lambda Expressions, Functional Interfaces & Method References.
 *
 * Covers:
 * 1. Bad/Verbose Legacy Anonymous Class vs Good/Concise Lambda + Method Reference.
 * 2. Custom @FunctionalInterface with default & static methods (RetryableTask).
 * 3. Built-in java.util.function composition (.and, .negate, .andThen, .compose).
 * 4. All 4 Method Reference forms in action.
 * 5. Semantic difference: `this` binding in Anonymous Inner Class vs Lambda.
 * 6. Primitive functional interfaces (IntPredicate) avoiding autoboxing.
 */
public class LambdaAndFunctionalInterfacesDemo {

    private final String instanceTag = "OUTER_SERVICE";

    /**
     * Custom Functional Interface:
     * - Exactly ONE abstract method: execute().
     * - Default method withLogging(...) and static factory ofConstant(...) do NOT break SAM rule.
     */
    @FunctionalInterface
    public interface RetryableTask<T> {
        T execute() throws Exception;

        default RetryableTask<T> withLogging(String taskName) {
            return () -> {
                long start = System.nanoTime();
                try {
                    T result = this.execute();
                    double ms = (System.nanoTime() - start) / 1_000_000.0;
                    System.out.printf("  [LOG] %s succeeded in %.2f ms%n", taskName, ms);
                    return result;
                } catch (Exception ex) {
                    System.err.printf("  [LOG] %s failed: %s%n", taskName, ex.getMessage());
                    throw ex;
                }
            };
        }

        static <R> RetryableTask<R> ofConstant(R value) {
            return () -> value;
        }
    }

    public static void main(String[] args) throws Exception {
        System.out.println("=== Java 8: Lambda Expressions & Functional Interfaces ===");

        // -------------------------------------------------------------
        // 1. BAD/LEGACY (Java 7 Anonymous Inner Class) vs GOOD (Lambda & Method Ref)
        // -------------------------------------------------------------
        List<String> languages = new ArrayList<>(List.of("Java", "Go", "Kotlin", "Rust", "C"));

        // BAD (Legacy verbose boilerplate — creates LambdaAndFunctionalInterfacesDemo$1.class)
        languages.sort(new Comparator<String>() {
            @Override
            public int compare(String a, String b) {
                return Integer.compare(a.length(), b.length());
            }
        });

        // GOOD (Java 8 Comparator + Method Reference — concise, zero inner class file)
        languages.sort(Comparator.comparingInt(String::length).thenComparing(String::compareTo));
        System.out.println("[1] Sorted languages: " + languages);

        // -------------------------------------------------------------
        // 2. Custom @FunctionalInterface + Decorator via default method
        // -------------------------------------------------------------
        System.out.println("\n[2] Custom @FunctionalInterface (RetryableTask):");
        RetryableTask<String> remoteFetch = () -> "PAYLOAD_OK";
        String out = remoteFetch.withLogging("FetchUserConfig").execute();
        System.out.println("  Result: " + out + " | Constant: " + RetryableTask.ofConstant(42).execute());

        // -------------------------------------------------------------
        // 3. Built-in Function Composition (.and, .negate, .andThen, .compose)
        // -------------------------------------------------------------
        System.out.println("\n[3] Functional Composition:");
        Predicate<String> notBlank = s -> s != null && !s.isBlank();
        Predicate<String> longEnough = s -> s.length() >= 4;
        Predicate<String> validUsername = notBlank.and(longEnough);
        System.out.println("  validUsername('ali') = " + validUsername.test("ali"));
        System.out.println("  validUsername('alice') = " + validUsername.test("alice"));

        UnaryOperator<String> trim = String::strip;
        Function<String, String> upper = String::toUpperCase;
        // andThen: trim FIRST, then upper
        Function<String, String> sanitize = trim.andThen(upper);
        // compose: upper executes BEFORE adding prefix
        Function<String, String> prefix = s -> "USR_" + s;
        System.out.println("  sanitize('  alice ') = " + sanitize.apply("  alice "));
        System.out.println("  compose example = " + prefix.compose(sanitize).apply("  bob "));

        // -------------------------------------------------------------
        // 4. All 4 Method Reference Forms
        // -------------------------------------------------------------
        System.out.println("\n[4] All 4 Method Reference Forms:");
        Function<String, Integer> staticRef = Integer::parseInt;         // 1. Static
        Consumer<String> boundRef = System.out::println;                 // 2. Bound instance
        BiFunction<String, String, Boolean> unboundRef = String::equals; // 3. Unbound instance (arg1.equals(arg2))
        Supplier<List<String>> ctorRef = ArrayList::new;                 // 4. Constructor

        boundRef.accept("  Static parseInt('2025') = " + staticRef.apply("2025"));
        boundRef.accept("  Unbound equals('a','a') = " + unboundRef.apply("a", "a"));
        List<String> created = ctorRef.get();
        created.add("ConstructorRefItem");
        System.out.println("  Constructor ref created: " + created);

        // -------------------------------------------------------------
        // 5. Primitive Specialization (Zero Autoboxing Overhead)
        // -------------------------------------------------------------
        IntPredicate isEven = n -> (n & 1) == 0; // Works directly on primitive int, no Integer boxing!
        System.out.println("\n[5] IntPredicate isEven(1024): " + isEven.test(1024));

        // -------------------------------------------------------------
        // 6. `this` Scoping & Effectively Final
        // -------------------------------------------------------------
        new LambdaAndFunctionalInterfacesDemo().demonstrateThisBinding();
    }

    private void demonstrateThisBinding() {
        String localPrefix = "EF_"; // Effectively final (never reassigned)

        Runnable anon = new Runnable() {
            final String instanceTag = "ANON_INNER";

            @Override
            public void run() {
                // `this` refers to the anonymous Runnable instance!
                System.out.println("\n[6] Anonymous class this.instanceTag = " + this.instanceTag);
            }
        };

        Runnable lambda = () -> {
            // `this` refers to enclosing LambdaAndFunctionalInterfacesDemo!
            // Reassigning localPrefix anywhere in method would cause compile error.
            System.out.println("    Lambda this.instanceTag = " + this.instanceTag + " (" + localPrefix + "CAPTURED)");
        };

        anon.run();
        lambda.run();
    }
}
