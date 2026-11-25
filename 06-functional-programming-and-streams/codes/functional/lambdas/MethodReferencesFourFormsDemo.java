package functional.lambdas;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Supplier;

/**
 * ALL 4 FORMS OF METHOD REFERENCES (::)
 *
 * 1. Static method:            ClassName::staticMethod
 * 2. Bound instance method:    instanceRef::instanceMethod
 * 3. Unbound instance method:  ClassName::instanceMethod (1st lambda arg becomes `this`)
 * 4. Constructor reference:    ClassName::new (or Type[]::new)
 */
public class MethodReferencesFourFormsDemo {

    record Product(String name) {}

    public static void main(String[] args) {
        // 1. Static Method Reference
        Function<String, Integer> parseInt = Integer::parseInt;
        System.out.println("1. Static ref Integer::parseInt -> " + parseInt.apply("2026"));

        // 2. Bound Instance Method Reference (bound to specific prefix object)
        String prefix = "[TAG] ";
        Function<String, String> prepender = prefix::concat;
        System.out.println("2. Bound ref prefix::concat -> " + prepender.apply("Java25"));

        // 3. Unbound Instance Method Reference (first argument becomes target receiver!)
        Function<String, String> upper = String::toUpperCase;
        System.out.println("3. Unbound ref String::toUpperCase -> " + upper.apply("streams"));

        // 4. Constructor References (no-arg, 1-arg, and array constructor!)
        Supplier<List<String>> listFactory = ArrayList::new;
        Function<String, Product> productFactory = Product::new;
        IntFunction<String[]> arrayFactory = String[]::new;

        List<String> items = listFactory.get();
        items.add(productFactory.apply("Mechanical Keyboard").name());
        String[] array = items.toArray(arrayFactory);
        System.out.println("4. Constructor refs created array length=" + array.length + ", item=" + array[0]);
    }
}
