package exceptions.phase5_production_custom_exceptions;

import java.net.URI;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Author: raysi
 * Package: com.raysi.exceptions.phase5_production_custom_exceptions.codes
 *
 * Demonstrates:
 * 1. RFC 7807 Problem Details representation for REST API error reporting.
 * 2. Stackless Exception optimization for high-throughput signaling.
 * 3. Thread Interruption flag recovery pattern.
 */
public class StacklessFastControlFlowExceptionDemo {

    // RFC 7807 Problem Details Object Model
    public record ProblemDetail(
            URI type,
            String title,
            int status,
            String detail,
            URI instance,
            Instant timestamp,
            Map<String, Object> extensions
    ) {
        public static ProblemDetail build(int status, String title, String detail, String instancePath) {
            Map<String, Object> ext = new LinkedHashMap<>();
            ext.put("traceId", "TRC-" + Long.toHexString(System.nanoTime()));
            ext.put("environment", "production");

            return new ProblemDetail(
                    URI.create("https://api.raysi.com/problems/" + title.toLowerCase().replace(' ', '-')),
                    title,
                    status,
                    detail,
                    URI.create(instancePath),
                    Instant.now(),
                    ext
            );
        }
    }

    // High-performance Stackless Exception: overrides fillInStackTrace() to skip JNI call
    public static class FlowBreakSignalException extends RuntimeException {
        public FlowBreakSignalException(String message) {
            super(message);
        }

        @Override
        public synchronized Throwable fillInStackTrace() {
            // No-op: prevents walking JVM stack frames
            return this;
        }
    }

    public static void main(String[] args) {
        System.out.println("===============================================================");
        System.out.println("1. RFC 7807 PROBLEM DETAILS FOR REST APIS");
        System.out.println("===============================================================");

        ProblemDetail problem = ProblemDetail.build(
                404,
                "Resource Not Found",
                "The order with ID 'ORD-9821' does not exist in the shard database",
                "/api/v1/orders/ORD-9821"
        );

        System.out.println("HTTP Status Code: " + problem.status());
        System.out.println("Payload JSON:");
        System.out.println("  type:        " + problem.type());
        System.out.println("  title:       " + problem.title());
        System.out.println("  status:      " + problem.status());
        System.out.println("  detail:      " + problem.detail());
        System.out.println("  instance:    " + problem.instance());
        System.out.println("  timestamp:   " + problem.timestamp());
        System.out.println("  extensions:  " + problem.extensions());

        System.out.println("\n===============================================================");
        System.out.println("2. COOPERATIVE CANCELLATION & INTERRUPT FLAG RESTORATION");
        System.out.println("===============================================================");

        Thread worker = new Thread(() -> {
            try {
                System.out.println("  [Worker Thread] Executing task, sleeping 5 seconds...");
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                System.out.println("  [Worker Thread] Caught InterruptedException.");
                System.out.println("  [Worker Thread] Status right after catch: isInterrupted() = "
                        + Thread.currentThread().isInterrupted() + " (JVM cleared it!)");

                // CRITICAL PRODUCTION FIX: Restore interrupted status
                Thread.currentThread().interrupt();
                System.out.println("  [Worker Thread] Status after restoring flag: isInterrupted() = "
                        + Thread.currentThread().isInterrupted());
            }

            if (Thread.currentThread().isInterrupted()) {
                System.out.println("  [Worker Thread] Cooperative shutdown confirmed. Performing clean exit.");
            }
        });

        worker.start();
        try { Thread.sleep(300); } catch (InterruptedException ignored) {}
        System.out.println("  [Main Thread] Requesting cancellation via worker.interrupt()...");
        worker.interrupt();

        try { worker.join(); } catch (InterruptedException ignored) {}

        System.out.println("\n[SUCCESS] Production Error Architecture and Interruption verified.");
    }
}
