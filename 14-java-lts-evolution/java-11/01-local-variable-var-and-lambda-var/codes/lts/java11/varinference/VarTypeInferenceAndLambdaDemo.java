package lts.java11.varinference;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

/**
 * Java 11 LTS — Local-Variable Type Inference (JEP 286 + JEP 323).
 *
 * Covers:
 * 1. Java 10 local `var` for cutting noisy Map/Try-with-resources declarations.
 * 2. Java 11 JEP 323 `var` inside lambda parameters enabling @Annotations without explicit types.
 * 3. BAD diamond inference trap (`var list = new ArrayList<>()` -> ArrayList<Object>) vs GOOD.
 * 4. Non-denotable anonymous type capture with `var`.
 */
public class VarTypeInferenceAndLambdaDemo {

    @Target(ElementType.PARAMETER)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface NonNullParam {}

    public static void main(String[] args) {
        System.out.println("=== Java 11 `var` Type Inference & Lambda Params Demo ===");

        // -------------------------------------------------------------
        // 1. BAD vs GOOD Generic Inference with `var`
        // -------------------------------------------------------------
        // BAD: Inferred type becomes ArrayList<Object>, losing compile-time String safety!
        var unsafeList = new ArrayList<>();
        unsafeList.add("allowed");
        unsafeList.add(12345); // Compiles silently — bug waiting downstream!

        // GOOD: Always keep type witness on RHS when using `var` with generics
        var safeNames = new ArrayList<String>();
        safeNames.add("Alice");
        System.out.println("[1] safeNames=" + safeNames + " | unsafe mixed size=" + unsafeList.size());

        // Great readability win: nested Map iteration
        var scoresByTeam = Map.of("Core", List.of(95, 88), "Platform", List.of(91, 99));
        for (var entry : scoresByTeam.entrySet()) {
            System.out.println("    Team " + entry.getKey() + " -> " + entry.getValue());
        }

        // -------------------------------------------------------------
        // 2. Java 11 JEP 323: `var` in Lambda Parameters for Annotations
        // -------------------------------------------------------------
        // All parameters in the lambda must consistently use `var` if one does.
        BiFunction<String, String, String> auditJoin =
                (@NonNullParam var namespace, @NonNullParam var event) -> namespace + "::" + event.toUpperCase();
        System.out.println("\n[2] Annotated lambda with var: " + auditJoin.apply("AUTH", "login_success"));

        // -------------------------------------------------------------
        // 3. Non-Denotable Type Superpower
        // -------------------------------------------------------------
        var adHocMetric = new Object() {
            final String name = "latency_p99";
            final double ms = 12.4;

            String summary() {
                return name + "=" + ms + "ms";
            }
        };
        System.out.println("\n[3] Non-denotable local helper accessed cleanly: " + adHocMetric.summary());
    }
}
