package lts.java17.sealedtypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Java 17 LTS — Sealed Interfaces, Permitted Hierarchies & ADTs (JEP 409).
 *
 * Covers:
 * 1. Sealed interface `PaymentMethod` (Sum Type) + Record subtypes (Product Types).
 * 2. All 3 subclass options (`final` / `record`, nested `sealed`, and `non-sealed` open extension).
 * 3. Nested sealed `TransactionResult` omitting `permits` (inferred from same source file).
 * 4. Reflection verification via Class.isSealed() & getPermittedSubclasses().
 */
public class SealedClassesHierarchyDemo {

    // 1. Sealed interface restricting top-level branches
    public sealed interface PaymentMethod permits CreditCard, BankTransfer, PartnerGateway {}

    // Leaf option A: record is implicitly final
    public record CreditCard(String maskedPan, String network) implements PaymentMethod {
        public CreditCard {
            Objects.requireNonNull(maskedPan);
        }
    }

    // Leaf option B: explicitly final class
    public static final class BankTransfer implements PaymentMethod {
        private final String iban;

        public BankTransfer(String iban) {
            this.iban = Objects.requireNonNull(iban);
        }

        public String iban() { return iban; }
    }

    // Extension option C: `non-sealed` deliberately re-opens extension for plugins!
    public static non-sealed class PartnerGateway implements PaymentMethod {
        private final String provider;

        public PartnerGateway(String provider) { this.provider = provider; }

        public String provider() { return provider; }
    }

    // Anyone can extend `PartnerGateway`, even though `PaymentMethod` itself was sealed!
    public static class StripeConnectExtension extends PartnerGateway {
        public StripeConnectExtension() { super("StripeConnect"); }
    }

    // 2. When subtypes live in the same file, `permits` clause is optional
    public sealed interface TransactionResult {
        record Success(UUID txId, BigDecimal amount, Instant at) implements TransactionResult {}
        record Declined(UUID txId, String code) implements TransactionResult {}
    }

    private static TransactionResult process(PaymentMethod method, BigDecimal amount) {
        UUID txId = UUID.randomUUID();
        if (method instanceof CreditCard cc && cc.maskedPan().startsWith("4")) {
            System.out.println("  -> Visa Fast-Lane: " + cc.maskedPan());
            return new TransactionResult.Success(txId, amount, Instant.now());
        } else if (method instanceof CreditCard cc) {
            System.out.println("  -> Card (" + cc.network() + "): " + cc.maskedPan());
            return new TransactionResult.Success(txId, amount, Instant.now());
        } else if (method instanceof BankTransfer wire) {
            System.out.println("  -> SEPA Wire IBAN: " + wire.iban());
            return new TransactionResult.Success(txId, amount, Instant.now());
        } else if (method instanceof PartnerGateway gw) {
            System.out.println("  -> Partner Gateway: " + gw.provider());
            return new TransactionResult.Declined(txId, "PARTNER_MAINTENANCE");
        }
        throw new IllegalStateException("Unreachable under sealed contract");
    }

    public static void main(String[] args) {
        System.out.println("=== Java 17 Sealed Classes & ADTs (JEP 409) Demo ===");

        // Runtime bytecode metadata inspection
        System.out.println("[1] PaymentMethod.isSealed() = " + PaymentMethod.class.isSealed());
        System.out.println("    Permitted subclasses = " +
                Arrays.stream(PaymentMethod.class.getPermittedSubclasses())
                        .map(Class::getSimpleName)
                        .toList());

        List<PaymentMethod> inputs = List.of(
                new CreditCard("4111-XXXX-1111", "VISA"),
                new BankTransfer("DE89370400440532013000"),
                new StripeConnectExtension()
        );

        System.out.println("\n[2] Processing closed polymorphic domain:");
        for (PaymentMethod m : inputs) {
            TransactionResult res = process(m, new BigDecimal("199.95"));
            System.out.println("    Result -> " + res);
        }
    }
}
