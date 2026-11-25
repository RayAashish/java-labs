package functional.lambdas;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntPredicate;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

/**
 * COMPLETE CATALOG OF java.util.function INTERFACES
 *
 * Covers:
 * - Big 4: Predicate, Function, Consumer, Supplier
 * - Operators: UnaryOperator<T>, BinaryOperator<T>
 * - Bi-arity: BiFunction<T,U,R>, BiConsumer<K,V>
 * - Primitive specializations: IntPredicate (avoids Integer autoboxing!)
 * - Chaining: and/or/negate and andThen/compose
 */
public class FunctionalInterfacesCatalogDemo {

    public static void main(String[] args) {
        System.out.println("=== 1. Predicate & Logical Chaining (and / or / negate) ===");
        Predicate<String> nonBlank = s -> s != null && !s.isBlank();
        Predicate<String> minLen4 = s -> s.length() >= 4;
        Predicate<String> validUsername = nonBlank.and(minLen4);
        System.out.println("'ray' valid?     " + validUsername.test("ray"));
        System.out.println("'aashish' valid? " + validUsername.test("aashish"));
        System.out.println("negate('ray')?   " + validUsername.negate().test("ray"));

        System.out.println("\n=== 2. Function, andThen() vs compose() ===");
        Function<Integer, Integer> add2 = x -> x + 2;
        Function<Integer, Integer> times3 = x -> x * 3;
        System.out.println("add2.andThen(times3).apply(5) = " + add2.andThen(times3).apply(5)); // (5+2)*3 = 21
        System.out.println("add2.compose(times3).apply(5) = " + add2.compose(times3).apply(5)); // (5*3)+2 = 17

        System.out.println("\n=== 3. Consumer, Supplier & Operators ===");
        Consumer<String> logger = msg -> System.out.println("[LOG] " + msg);
        Supplier<String> idGen = () -> "REQ-" + UUID.randomUUID().toString().substring(0, 6);
        UnaryOperator<String> normalize = s -> s.trim().toUpperCase();
        BinaryOperator<Double> addMoney = Double::sum;

        logger.accept(normalize.apply("  " + idGen.get() + "  "));
        System.out.println("BinaryOperator sum: $" + addMoney.apply(19.50, 30.50));

        System.out.println("\n=== 4. BiFunction & Map.merge / forEach (BiConsumer) ===");
        BiFunction<String, Integer, String> repeatWord = (word, n) -> word.repeat(n);
        System.out.println("BiFunction: " + repeatWord.apply("Hi!", 3));

        Map<String, Integer> scores = new HashMap<>(Map.of("Alice", 10, "Bob", 20));
        scores.merge("Alice", 5, Integer::sum);
        scores.forEach((user, pts) -> System.out.println("   " + user + " -> " + pts));

        System.out.println("\n=== 5. Primitive Specialization (Zero Boxing!) ===");
        IntPredicate isEven = n -> (n & 1) == 0;
        System.out.println("isEven(42): " + isEven.test(42));
    }
}
