package exceptions.phase5_production_custom_exceptions;

import java.sql.SQLException;
import java.time.Instant;
import java.util.UUID;

/**
 * Author: raysi
 * Package: com.raysi.exceptions.phase5_production_custom_exceptions.codes
 *
 * Demonstrates:
 * 1. Enterprise Domain Exception modeling with structured Error Codes.
 * 2. Multi-tier Exception Translation (Infrastructure -> Service -> API).
 * 3. Preserving root-cause stack traces across distributed boundaries.
 * 4. Forensic root-cause traversal utility.
 */
public class DomainExceptionChainingAndErrorCodeDemo {

    public enum ErrorCode {
        ORDER_NOT_FOUND("ORD-4040", "The requested order resource was not found"),
        PAYMENT_GATEWAY_TIMEOUT("PAY-5040", "Downstream payment gateway timed out"),
        PERSISTENCE_TRANSACTION_FAILED("INF-5001", "Database cluster failed to commit transaction");

        private final String code;
        private final String description;

        ErrorCode(String code, String description) {
            this.code = code;
            this.description = description;
        }

        public String getCode() { return code; }
        public String getDescription() { return description; }
    }

    // Base Enterprise Domain Exception
    public static abstract class AbstractDomainException extends RuntimeException {
        private final ErrorCode errorCode;
        private final String correlationId;
        private final Instant timestamp;

        public AbstractDomainException(ErrorCode errorCode, String message, Throwable cause) {
            super(message, cause);
            this.errorCode = errorCode;
            this.correlationId = UUID.randomUUID().toString();
            this.timestamp = Instant.now();
        }

        public ErrorCode getErrorCode() { return errorCode; }
        public String getCorrelationId() { return correlationId; }
        public Instant getTimestamp() { return timestamp; }
    }

    // Concrete Domain Exception
    public static class OrderProcessingException extends AbstractDomainException {
        private final String orderId;

        public OrderProcessingException(String orderId, ErrorCode errorCode, String message, Throwable cause) {
            super(errorCode, message, cause);
            this.orderId = orderId;
        }

        public String getOrderId() { return orderId; }
    }

    // Layer 1: Simulated Database DAO
    public static class OrderDao {
        public void persistOrder(String orderId) throws SQLException {
            // Low-level database failure
            throw new SQLException("Connection refused: PostgreSQL replica at 192.168.1.100:5432 is down.");
        }
    }

    // Layer 2: Service Layer with Exception Translation
    public static class OrderService {
        private final OrderDao dao = new OrderDao();

        public void checkout(String orderId) {
            try {
                dao.persistOrder(orderId);
            } catch (SQLException sqlEx) {
                // TRANSLATION: Wrap low-level SQL exception into semantic business domain exception
                throw new OrderProcessingException(
                        orderId,
                        ErrorCode.PERSISTENCE_TRANSACTION_FAILED,
                        "Unable to persist order '" + orderId + "' due to storage tier outage.",
                        sqlEx // CAUSE PRESERVED!
                );
            }
        }
    }

    public static void main(String[] args) {
        System.out.println("===============================================================");
        System.out.println("ENTERPRISE DOMAIN EXCEPTION TRANSLATION & ROOT CAUSE CHAINING");
        System.out.println("===============================================================");

        OrderService orderService = new OrderService();

        try {
            orderService.checkout("ORD-2026-X89");
        } catch (OrderProcessingException e) {
            System.out.println("\n[API Controller Intercepted Domain Failure]");
            System.out.println("  Order ID:        " + e.getOrderId());
            System.out.println("  Error Code:      " + e.getErrorCode().getCode());
            System.out.println("  Error Title:     " + e.getErrorCode().getDescription());
            System.out.println("  Domain Message:  " + e.getMessage());
            System.out.println("  Correlation ID:  " + e.getCorrelationId());
            System.out.println("  Timestamp:       " + e.getTimestamp());

            System.out.println("\n--- Forensic Causality Chain Traversal ---");
            Throwable current = e;
            int depth = 0;
            while (current != null) {
                System.out.printf("  Depth [%d] -> Class: %s%n", depth, current.getClass().getName());
                System.out.printf("             Message: %s%n", current.getMessage());
                current = current.getCause();
                depth++;
            }

            System.out.println("\nObservation: The root SQLException was fully retained without leaking to external callers!");
        }

        System.out.println("\n[SUCCESS] Domain Exception Chaining Demo completed.");
    }
}
