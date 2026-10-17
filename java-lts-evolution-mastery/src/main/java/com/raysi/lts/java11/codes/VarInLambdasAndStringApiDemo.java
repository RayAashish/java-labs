package com.raysi.lts.java11.codes;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Author: raysi
 * Package: com.raysi.lts.java11.codes
 *
 * Demonstrates:
 * 1. Local-Variable Syntax for Lambda Parameters (JEP 323) with Type Annotations.
 * 2. Modern String APIs (isBlank, lines, strip, repeat).
 * 3. strip() vs legacy trim() Unicode whitespace mechanics.
 * 4. Predicate.not() and toArray(IntFunction) enhancements.
 */
public class VarInLambdasAndStringApiDemo {

    @Target(ElementType.PARAMETER)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface NotNullParam {}

    public static void main(String[] args) {
        System.out.println("=== Java 11: Var In Lambdas & String API Enhancements ===");

        // -------------------------------------------------------------
        // 1. Var in Lambda Parameters with Annotations (JEP 323)
        // -------------------------------------------------------------
        System.out.println("\n--- 1. Var in Lambda Parameters with Annotations ---");
        // Prior to Java 11, applying an annotation to a lambda parameter required typing out the full type.
        // In Java 11, 'var' allows keeping type inference while attaching annotations.
        BiFunction<String, String, String> concatFormatter = 
                (@NotNullParam var prefix, @NotNullParam var content) -> prefix + "::" + content.toUpperCase();

        String formatted = concatFormatter.apply("AUDIT", "transaction_payload_01");
        System.out.println(">> Formatted with annotated var: " + formatted);

        // -------------------------------------------------------------
        // 2. Modern String APIs (isBlank, lines, strip, repeat)
        // -------------------------------------------------------------
        System.out.println("\n--- 2. Modern String API Methods ---");
        String multilineText = "  \n  Line 1: System Config  \n\n  Line 2: Active Nodes  \n  Line 3: Shutdown Hook  \n";

        // lines() returns a Stream<String> split by line terminators (\n, \r, \r\n) lazily
        List<String> cleanedLines = multilineText.lines()
                .filter(Predicate.not(String::isBlank)) // Predicate.not introduced in Java 11
                .map(String::strip) // Unicode-aware whitespace stripping
                .collect(Collectors.toList());

        System.out.println(">> Processed lines using lines() & strip():");
        cleanedLines.forEach(l -> System.out.println("   -> " + l));

        // repeat(n)
        String divider = "=".repeat(40);
        System.out.println(divider);

        // -------------------------------------------------------------
        // 3. strip() vs trim() Mechanics
        // -------------------------------------------------------------
        System.out.println("\n--- 3. strip() vs legacy trim() Unicode Mechanics ---");
        // Character \u2005 is a Unicode Mid-Quad space (U+2005)
        String unicodeSpaceString = "\u2005\u2005Modern Java Enterprise\u2005\u2005";

        // trim() only strips ASCII whitespace (<= U+0020 ' ')
        // strip() is Unicode-aware (checks Character.isWhitespace())
        System.out.println("Raw length: " + unicodeSpaceString.length());
        System.out.println("trim() length (Fails on Unicode space): " + unicodeSpaceString.trim().length());
        System.out.println("strip() length (Properly cleans Unicode space): " + unicodeSpaceString.strip().length());

        // -------------------------------------------------------------
        // 4. Collection.toArray(IntFunction)
        // -------------------------------------------------------------
        System.out.println("\n--- 4. Collection.toArray(IntFunction) ---");
        List<String> frameworks = List.of("Spring Boot", "Micronaut", "Quarkus");
        // Old way: frameworks.toArray(new String[0]);
        // Java 11 idiomatic way:
        String[] frameworkArray = frameworks.toArray(String[]::new);
        System.out.println(">> Converted array type: " + frameworkArray.getClass().getSimpleName() + ", size: " + frameworkArray.length);
    }
}
