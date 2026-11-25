package functional.streams;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * STREAM TERMINAL OPERATIONS & ALL 3 REDUCE OVERLOADS
 *
 * Covers:
 * - Matching: anyMatch, allMatch (vacuous truth on empty!), noneMatch
 * - Finding:  findFirst vs findAny
 * - Extremes: min, max, count, toArray(T[]::new)
 * - reduce(): 1-arg (Optional), 2-arg (identity), 3-arg (identity + accumulator + combiner)
 */
public class StreamTerminalAndReduceDemo {

    record Product(String name, double price) {}

    public static void main(String[] args) {
        List<Product> cart = List.of(
                new Product("SSD", 80.0),
                new Product("RAM", 120.0),
                new Product("GPU", 500.0)
        );

        System.out.println("=== 1. Matching & Empty-Stream Vacuous Truth Trap ===");
        System.out.println("anyMatch (>400): " + cart.stream().anyMatch(p -> p.price() > 400));
        System.out.println("allMatch (>50):  " + cart.stream().allMatch(p -> p.price() > 50));
        System.out.println("noneMatch (<0):  " + cart.stream().noneMatch(p -> p.price() < 0));
        // Classic interview trap: allMatch on an EMPTY stream returns TRUE!
        System.out.println("Empty List allMatch(>1000): " + List.<Product>of().stream().allMatch(p -> p.price() > 1000));

        System.out.println("\n=== 2. min(), max(), toArray(Typed) ===");
        Optional<Product> priciest = cart.stream().max(Comparator.comparingDouble(Product::price));
        System.out.println("Most expensive: " + priciest.map(Product::name).orElse("None"));
        String[] namesArr = cart.stream().map(Product::name).toArray(String[]::new);
        System.out.println("Typed array length: " + namesArr.length);

        System.out.println("\n=== 3. All 3 reduce() Overloads ===");
        List<Integer> nums = List.of(2, 3, 4);

        // Overload 1: returns Optional<T> (handles empty streams safely)
        Optional<Integer> sumOpt = nums.stream().reduce(Integer::sum);
        System.out.println("1-arg reduce sum: " + sumOpt.orElse(0));

        // Overload 2: T identity + accumulator (identity for multiply is 1, NOT 0!)
        int product = nums.stream().reduce(1, (a, b) -> a * b);
        System.out.println("2-arg reduce product: " + product);

        // Overload 3: U identity + BiFunction<U,T,U> accumulator + BinaryOperator<U> combiner
        // Accumulates Product -> Double directly and merges parallel chunks cleanly!
        double cartTotal = cart.parallelStream().reduce(
                0.0,
                (runningSum, item) -> runningSum + item.price(),
                Double::sum
        );
        System.out.printf("3-arg parallel reduce cart total: $%.2f%n", cartTotal);
    }
}
