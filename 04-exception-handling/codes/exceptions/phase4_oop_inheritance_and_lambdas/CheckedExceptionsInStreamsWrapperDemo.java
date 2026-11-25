package exceptions.phase4_oop_inheritance_and_lambdas;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

/**
 * Author: raysi
 * Package: com.raysi.exceptions.phase4_oop_inheritance_and_lambdas.codes
 *
 * Demonstrates:
 * 1. Why standard Java 8 Streams break when lambdas throw Checked Exceptions.
 * 2. Higher-order functional wrapper 'uncheck' converting to RuntimeException.
 * 3. Generic Sneaky Throws wrapper allowing checked exceptions to pass without declaration.
 * 4. Enterprise Result/Either monad pattern for resilient stream processing without early abortion.
 */
public class CheckedExceptionsInStreamsWrapperDemo {

    @FunctionalInterface
    public interface ThrowingFunction<T, R, E extends Throwable> {
        R apply(T t) throws E;
    }

    // Approach 1: Convert Checked Exception into a RuntimeException
    public static <T, R> Function<T, R> uncheck(ThrowingFunction<T, R, ?> function) {
        return input -> {
            try {
                return function.apply(input);
            } catch (Throwable t) {
                throw new RuntimeException("Unchecked stream failure: " + t.getMessage(), t);
            }
        };
    }

    // Approach 2: Sneaky Throws using generic type erasure
    @SuppressWarnings("unchecked")
    private static <R, E extends Throwable> R sneakyThrow(Throwable t) throws E {
        throw (E) t;
    }

    public static <T, R> Function<T, R> sneaky(ThrowingFunction<T, R, ?> function) {
        return input -> {
            try {
                return function.apply(input);
            } catch (Throwable t) {
                return CheckedExceptionsInStreamsWrapperDemo.<R, RuntimeException>sneakyThrow(t);
            }
        };
    }

    // Approach 3: Result Monad Pattern (Success / Failure Container)
    public static final class Result<T> {
        private final T value;
        private final Throwable error;

        private Result(T value, Throwable error) {
            this.value = value;
            this.error = error;
        }

        public static <T> Result<T> success(T value) {
            return new Result<>(Objects.requireNonNull(value), null);
        }

        public static <T> Result<T> failure(Throwable error) {
            return new Result<>(null, Objects.requireNonNull(error));
        }

        public boolean isSuccess() {
            return error == null;
        }

        public T getValue() {
            return value;
        }

        public Throwable getError() {
            return error;
        }

        @Override
        public String toString() {
            return isSuccess() ? "Success(" + value + ")" : "Failure(" + error.getMessage() + ")";
        }
    }

    public static <T, R> Function<T, Result<R>> lift(ThrowingFunction<T, R, ?> function) {
        return input -> {
            try {
                return Result.success(function.apply(input));
            } catch (Throwable t) {
                return Result.failure(t);
            }
        };
    }

    // Target method throwing Checked Exception
    public static String fetchRemoteRecord(String recordId) throws IOException {
        if ("corrupt-002".equals(recordId)) {
            throw new IOException("Remote cluster 503: Corrupted record payload for " + recordId);
        }
        return "PARSED_RECORD_PAYLOAD_[" + recordId.toUpperCase() + "]";
    }

    public static void main(String[] args) {
        List<String> records = List.of("rec-001", "corrupt-002", "rec-003");

        System.out.println("===============================================================");
        System.out.println("1. HIGHER-ORDER 'uncheck' WRAPPER (Aborts Stream on Error)");
        System.out.println("===============================================================");
        try {
            List<String> processed = records.stream()
                    .map(uncheck(CheckedExceptionsInStreamsWrapperDemo::fetchRemoteRecord))
                    .toList();
            System.out.println("Processed: " + processed);
        } catch (RuntimeException e) {
            System.out.println("Stream aborted as expected: " + e.getMessage());
            System.out.println("Underlying Root Cause: " + e.getCause().getClass().getName());
        }

        System.out.println("\n===============================================================");
        System.out.println("2. ENTERPRISE 'lift' RESULT MONAD (Resilient Fault Tolerance)");
        System.out.println("===============================================================");
        List<Result<String>> results = records.stream()
                .map(lift(CheckedExceptionsInStreamsWrapperDemo::fetchRemoteRecord))
                .toList();

        System.out.println("All Pipeline Results: " + results);

        System.out.println("\n--- Successfully Processed Items ---");
        results.stream()
                .filter(Result::isSuccess)
                .map(Result::getValue)
                .forEach(val -> System.out.println("  [+] " + val));

        System.out.println("\n--- Failed Items (Sent to Dead Letter Queue) ---");
        results.stream()
                .filter(r -> !r.isSuccess())
                .forEach(r -> System.out.println("  [-] DLQ Event: " + r.getError().getMessage()));

        System.out.println("\n[SUCCESS] Checked Exceptions in Java 8 Streams demonstration completed.");
    }
}
