package com.raysi.lts.java17.codes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Author: raysi
 * Package: com.raysi.lts.java17.codes
 *
 * Demonstrates:
 * 1. Java 17 Sealed Interfaces & Sealed Hierarchies (Sum Types / ADTs).
 * 2. Exhaustive Domain Modeling without arbitrary inheritance leakage.
 * 3. Pattern Matching for instanceof with flow scoping and smart casting.
 * 4. Combination of Records (Product types) + Sealed Interfaces (Sum types).
 */
public class SealedClassDomainHierarchyDemo {

    // ==========================================
    // 1. Sealed Domain Hierarchy: PaymentMethod
    // ==========================================

    /**
     * Sealed interface restricting permitted payment channel implementations.
     * Guarantees closed-world assumption at compile-time.
     */
    public sealed interface PaymentMethod permits CreditCard, BankTransfer, CryptoWallet {}

    public record CreditCard(String maskedCardNumber, String cardHolder, String expDate)
            implements PaymentMethod {
        public CreditCard {
            Objects.requireNonNull(maskedCardNumber, "Card number required");
        }
    }

    public record BankTransfer(String iban, String swiftBic, String bankName)
            implements PaymentMethod {
        public BankTransfer {
            Objects.requireNonNull(iban, "IBAN required");
        }
    }

    public record CryptoWallet(String walletAddress, String blockchainNetwork)
            implements PaymentMethod {
        public CryptoWallet {
            Objects.requireNonNull(walletAddress, "Wallet address required");
        }
    }

    // ==========================================
    // 2. Sealed Hierarchy: Transaction Result
    // ==========================================

    public sealed interface TransactionResult permits TransactionResult.Success, TransactionResult.Declined, TransactionResult.PendingReview {

        record Success(UUID transactionId, BigDecimal amount, Instant settledAt) implements TransactionResult {}

        record Declined(UUID transactionId, String reasonCode, String failureMessage) implements TransactionResult {}

        record PendingReview(UUID transactionId, String complianceFlag) implements TransactionResult {}
    }

    // ==========================================
    // 3. Domain Service using Pattern Matching
    // ==========================================

    public static class PaymentProcessingService {

        /**
         * Dispatches payment processing using Java 17 Pattern Matching for instanceof.
         * Notice the complete elimination of explicit casts: `(CreditCard) method`.
         */
        public TransactionResult process(PaymentMethod method, BigDecimal amount) {
            UUID txId = UUID.randomUUID();

            if (method instanceof CreditCard card && card.maskedCardNumber().startsWith("4")) {
                System.out.println("[PaymentService] Routing Visa Card ending in " + card.maskedCardNumber());
                return new TransactionResult.Success(txId, amount, Instant.now());
            } else if (method instanceof CreditCard card) {
                System.out.println("[PaymentService] Routing Generic Card: " + card.maskedCardNumber());
                return new TransactionResult.Success(txId, amount, Instant.now());
            } else if (method instanceof BankTransfer transfer) {
                System.out.println("[PaymentService] Initiating SEPA / Wire transfer for IBAN: " + transfer.iban());
                return new TransactionResult.PendingReview(txId, "HIGH_VALUE_WIRE_CLEARANCE");
            } else if (method instanceof CryptoWallet crypto) {
                System.out.println("[PaymentService] Broadcasting to " + crypto.blockchainNetwork() + " -> " + crypto.walletAddress());
                return new TransactionResult.Success(txId, amount, Instant.now());
            }

            // Sealed hierarchy guarantees that all possible subtypes are covered.
            throw new IllegalArgumentException("Unknown payment method type: " + method);
        }

        /**
         * Inspects and logs transaction result using pattern matching with flow scope.
         */
        public void handleResult(TransactionResult result) {
            if (result instanceof TransactionResult.Success success) {
                System.out.printf("[RESULT - SUCCESS] Tx: %s | Amount: %s | Settled: %s%n",
                        success.transactionId(), success.amount(), success.settledAt());
            } else if (result instanceof TransactionResult.Declined declined) {
                System.err.printf("[RESULT - DECLINED] Tx: %s | Code: %s | Reason: %s%n",
                        declined.transactionId(), declined.reasonCode(), declined.failureMessage());
            } else if (result instanceof TransactionResult.PendingReview review) {
                System.out.printf("[RESULT - PENDING] Tx: %s | Compliance Flag: %s%n",
                        review.transactionId(), review.complianceFlag());
            }
        }
    }

    public static void main(String[] args) {
        System.out.println("=== Java 17 Sealed Classes & Pattern Matching Hierarchy Demo ===");

        PaymentProcessingService service = new PaymentProcessingService();

        List<PaymentMethod> testMethods = List.of(
                new CreditCard("4111-XXXX-XXXX-1111", "RAYSI", "12/28"),
                new BankTransfer("DE89370400440532013000", "DEUTDEDDFXX", "Deutsche Bank"),
                new CryptoWallet("0x71C...39724", "Ethereum-Mainnet")
        );

        for (PaymentMethod method : testMethods) {
            System.out.println("\nProcessing: " + method.getClass().getSimpleName());
            TransactionResult result = service.process(method, BigDecimal.valueOf(250.75));
            service.handleResult(result);
        }
    }
}
