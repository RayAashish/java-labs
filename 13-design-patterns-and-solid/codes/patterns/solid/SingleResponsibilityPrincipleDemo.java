package patterns.solid;

/**
 * S — SINGLE RESPONSIBILITY PRINCIPLE (SRP)
 *
 * WHAT IT IS:
 *   A class should have ONE, and only one, reason to change (one cohesive actor/responsibility).
 *
 * WHY IT MATTERS:
 *   When business calculations, database persistence, and email delivery live in the same class,
 *   changing the SMTP email template risks breaking tax calculation or SQL persistence, and you
 *   cannot unit-test pricing without touching a database or email server.
 *
 * IN THIS FILE:
 *   1. BAD CODE:  GodInvoiceService mixes calculation, SQL saving, and email formatting.
 *   2. GOOD CODE: Cleanly separated into InvoiceCalculator, InvoiceRepository, InvoiceNotifier,
 *                 orchestrated by a thin InvoiceBillingService.
 */
public class SingleResponsibilityPrincipleDemo {

    record Invoice(String id, String customerEmail, double subtotal, double total) {}

    // =========================================================================
    // ❌ BAD CODE: Violates SRP — 3 unrelated reasons to change in ONE class!
    //   1. Finance changes tax/discount logic -> edits this class.
    //   2. DBA changes database schema/SQL    -> edits this class.
    //   3. Marketing changes email wording    -> edits this class.
    // =========================================================================
    static class BadGodInvoiceManager {
        public void processInvoice(String id, String email, double subtotal) {
            // Responsibility 1: Tax & pricing math
            double tax = subtotal * 0.18;
            double total = subtotal + tax;

            // Responsibility 2: Database persistence
            System.out.println("[BAD-DB] INSERT INTO invoices VALUES ('" + id + "', " + total + ")");

            // Responsibility 3: External Email / SMTP delivery
            System.out.println("[BAD-EMAIL] Sending receipt of $" + total + " to " + email);
        }
    }

    // =========================================================================
    // ✅ GOOD CODE: Each class has ONE focused job & ONE reason to change.
    // =========================================================================

    /** Responsibility 1: Pure business calculation (trivial to unit-test, zero I/O). */
    static class InvoiceCalculator {
        private static final double TAX_RATE = 0.18;

        public Invoice calculate(String id, String customerEmail, double subtotal) {
            if (subtotal < 0) {
                throw new IllegalArgumentException("Subtotal cannot be negative");
            }
            double total = subtotal * (1.0 + TAX_RATE);
            return new Invoice(id, customerEmail, subtotal, total);
        }
    }

    /** Responsibility 2: Persistence boundary only. */
    static class InvoiceRepository {
        public void save(Invoice invoice) {
            System.out.printf("[GOOD-DB] Saved invoice %s ($%.2f)%n", invoice.id(), invoice.total());
        }
    }

    /** Responsibility 3: Notification formatting & delivery only. */
    static class InvoiceNotifier {
        public void sendReceipt(Invoice invoice) {
            System.out.printf("[GOOD-EMAIL] Receipt for %s ($%.2f) sent to %s%n",
                    invoice.id(), invoice.total(), invoice.customerEmail());
        }
    }

    /** Thin orchestrator coordinating the 3 cohesive collaborators. */
    static class InvoiceBillingService {
        private final InvoiceCalculator calculator;
        private final InvoiceRepository repository;
        private final InvoiceNotifier notifier;

        InvoiceBillingService(InvoiceCalculator calculator,
                              InvoiceRepository repository,
                              InvoiceNotifier notifier) {
            this.calculator = calculator;
            this.repository = repository;
            this.notifier = notifier;
        }

        public Invoice issueInvoice(String id, String customerEmail, double subtotal) {
            Invoice invoice = calculator.calculate(id, customerEmail, subtotal);
            repository.save(invoice);
            notifier.sendReceipt(invoice);
            return invoice;
        }
    }

    public static void main(String[] args) {
        System.out.println("=== BAD (Coupled God Class) ===");
        new BadGodInvoiceManager().processInvoice("INV-01", "user@example.com", 100.0);

        System.out.println("\n=== GOOD (SRP Cohesive Components) ===");
        var service = new InvoiceBillingService(
                new InvoiceCalculator(),
                new InvoiceRepository(),
                new InvoiceNotifier()
        );
        service.issueInvoice("INV-02", "user@example.com", 100.0);
    }
}
