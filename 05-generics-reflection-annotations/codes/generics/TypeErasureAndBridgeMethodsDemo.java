package generics;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * TYPE ERASURE, SYNTHETIC BRIDGE METHODS & @SafeVarargs
 *
 * WHAT IT IS:
 *   javac checks generic types at compile time and ERASES `<T>` in `.class` bytecode
 *   for backward compatibility (replacing `<T>` with `Object` or its upper bound).
 *
 * WHY IT MATTERS:
 *   Explains why `new T()`, `List<int>`, `instanceof List<String>`, and overloading
 *   `foo(List<String>)` + `foo(List<Integer>)` are illegal — and reveals how `javac`
 *   generates hidden **Bridge Methods** to preserve polymorphism!
 */
public class TypeErasureAndBridgeMethodsDemo {

    static class VersionNode implements Comparable<VersionNode> {
        private final int version;
        VersionNode(int version) { this.version = version; }

        @Override
        public int compareTo(VersionNode other) {
            return Integer.compare(this.version, other.version);
        }
    }

    // Heap pollution risk: varargs of non-reifiable generic type creates Object[] under the hood!
    // @SafeVarargs promises callers we neither store alien types into `lists` nor expose the backing array.
    @SafeVarargs
    public static <T> List<T> mergeAll(List<T>... lists) {
        List<T> merged = new ArrayList<>();
        for (List<T> list : lists) {
            merged.addAll(list);
        }
        return merged;
    }

    public static void main(String[] args) {
        System.out.println("=== 1. Type Erasure Proof ===");
        List<String> strings = new ArrayList<>();
        List<Integer> integers = new ArrayList<>();
        System.out.println("List<String>.getClass() == List<Integer>.getClass()? "
                + (strings.getClass() == integers.getClass()));

        System.out.println("\n=== 2. Inspecting Compiler-Generated Bridge Method ===");
        for (Method m : VersionNode.class.getDeclaredMethods()) {
            if (m.getName().equals("compareTo")) {
                System.out.printf("Method: %-12s | paramTypes=%-22s | isBridge=%s | isSynthetic=%s%n",
                        m.getName(),
                        Arrays.toString(m.getParameterTypes()),
                        m.isBridge(),
                        m.isSynthetic());
            }
        }

        System.out.println("\n=== 3. @SafeVarargs Safe Merge ===");
        System.out.println(mergeAll(List.of("Java", "25"), List.of("Generics")));
    }
}
