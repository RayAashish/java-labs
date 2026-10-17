package com.raysi.lts.java08.codes;

import java.util.Objects;
import java.util.Optional;

/**
 * Author: raysi
 * Package: com.raysi.lts.java08.codes
 *
 * Demonstrates:
 * 1. Custom Single Abstract Method (SAM) interface with @FunctionalInterface.
 * 2. Default & Static Interface methods (Java 8 API evolution without breaking implementations).
 * 3. Idiomatic Optional<T> usage (map, flatMap, filter, orElseGet) vs dangerous get() anti-pattern.
 */
public class CustomFunctionalInterfaceAndOptionalDemo {

    /**
     * Custom Functional Interface representing a retryable business command.
     */
    @FunctionalInterface
    public interface RetryableTask<T> {

        // The Single Abstract Method (SAM)
        T execute() throws Exception;

        // Default Method: Enables functional composition without breaking implementors
        default RetryableTask<T> withLogging(String taskName) {
            return () -> {
                System.out.println("[TASK START] Initiating: " + taskName);
                long start = System.nanoTime();
                try {
                    T result = this.execute();
                    System.out.println("[TASK SUCCESS] Completed in: " + (System.nanoTime() - start) / 1_000_000.0 + " ms");
                    return result;
                } catch (Exception ex) {
                    System.err.println("[TASK FAILED] Error: " + ex.getMessage());
                    throw ex;
                }
            };
        }

        // Static Utility Method inside Interface
        static <R> RetryableTask<R> ofConstant(R value) {
            return () -> value;
        }
    }

    // Domain models for Optional demonstration
    public static class UserProfile {
        private final String username;
        private final Address address; // May be null

        public UserProfile(String username, Address address) {
            this.username = username;
            this.address = address;
        }

        public String getUsername() { return username; }
        public Optional<Address> getAddress() { return Optional.ofNullable(address); }
    }

    public static class Address {
        private final String city;
        private final String zipCode;

        public Address(String city, String zipCode) {
            this.city = city;
            this.zipCode = zipCode;
        }

        public String getCity() { return city; }
        public Optional<String> getZipCode() { return Optional.ofNullable(zipCode); }
    }

    public static void main(String[] args) throws Exception {
        System.out.println("=== Java 8 Functional Interface & Optional Demo ===");

        // 1. Invoking Custom Functional Interface via Lambda
        RetryableTask<String> task = () -> {
            Thread.sleep(50);
            return "SUCCESS_DATA_PAYLOAD";
        };

        RetryableTask<String> decoratedTask = task.withLogging("FetchRemoteConfig");
        String output = decoratedTask.execute();
        System.out.println(">> Task Result: " + output);

        // 2. Idiomatic Monadic Optional Pipeline
        System.out.println("\n--- Optional Monadic Traversal ---");
        UserProfile userWithAddress = new UserProfile("raysi", new Address("San Francisco", "94105"));
        UserProfile userWithoutAddress = new UserProfile("guest", null);

        printUserZipCode(userWithAddress);
        printUserZipCode(userWithoutAddress);

        // 3. Optional Anti-Pattern Warning: orElse() vs orElseGet()
        System.out.println("\n--- orElse (Eager) vs orElseGet (Lazy) ---");
        Optional<String> cachedValue = Optional.of("Pre-computed Cache");

        // ANTI-PATTERN: expensiveFallback() executes EVEN THOUGH cachedValue is present!
        String v1 = cachedValue.orElse(expensiveFallback());
        System.out.println("Result using orElse: " + v1);

        // IDIOMATIC: Supplier executes ONLY IF Optional is empty!
        String v2 = cachedValue.orElseGet(() -> expensiveFallback());
        System.out.println("Result using orElseGet: " + v2);
    }

    private static void printUserZipCode(UserProfile user) {
        String zip = Optional.ofNullable(user)
                .flatMap(UserProfile::getAddress)
                .flatMap(Address::getZipCode)
                .filter(code -> !code.isBlank())
                .orElse("DEFAULT_ZIP_00000");

        System.out.println("User: " + user.getUsername() + " -> Resolved Zip: " + zip);
    }

    private static String expensiveFallback() {
        System.out.println("  [!] WARNING: expensiveFallback() was called eagerly!");
        return "FALLBACK_FROM_DB";
    }
}
