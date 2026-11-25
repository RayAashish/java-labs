package patterns.solid;

/**
 * I — INTERFACE SEGREGATION PRINCIPLE (ISP)
 *
 * WHAT IT IS:
 *   Clients should NOT be forced to depend on methods they do not use.
 *   Prefer small, focused role interfaces over one monolithic "fat" interface.
 *
 * WHY IT MATTERS:
 *   Fat interfaces force simple implementations to write dummy stubs or throw
 *   UnsupportedOperationException, coupling unrelated features together.
 *
 * IN THIS FILE:
 *   1. BAD CODE:  Monolithic SmartOfficeMachine forces BasicLaserPrinter to stub scan() & fax().
 *   2. GOOD CODE: Segregated Printable, Scannable, Faxable roles — devices implement ONLY what they support.
 */
public class InterfaceSegregationPrincipleDemo {

    // =========================================================================
    // ❌ BAD CODE: Fat interface forces unsupported methods on simple classes!
    // =========================================================================
    interface BadAllInOneMachine {
        void print(String doc);
        void scan(String doc);
        void fax(String doc);
    }

    static class BadSimplePrinter implements BadAllInOneMachine {
        @Override public void print(String doc) { System.out.println("[BAD-PRINT] " + doc); }
        @Override public void scan(String doc) { throw new UnsupportedOperationException("No scanner hardware!"); }
        @Override public void fax(String doc) { throw new UnsupportedOperationException("No fax modem!"); }
    }

    // =========================================================================
    // ✅ GOOD CODE: Focused role interfaces; compose only capabilities that exist.
    // =========================================================================
    interface Printable { void print(String doc); }
    interface Scannable { void scan(String doc); }
    interface Faxable   { void fax(String doc); }

    static class LaserPrinter implements Printable {
        @Override
        public void print(String doc) {
            System.out.println("[GOOD-PRINTER] Printing cleanly: " + doc);
        }
    }

    static class EnterpriseMultifunctionDevice implements Printable, Scannable, Faxable {
        @Override public void print(String doc) { System.out.println("[GOOD-MFP] Print: " + doc); }
        @Override public void scan(String doc)  { System.out.println("[GOOD-MFP] Scan:  " + doc); }
        @Override public void fax(String doc)   { System.out.println("[GOOD-MFP] Fax:   " + doc); }
    }

    public static void main(String[] args) {
        System.out.println("=== BAD (Client risks UnsupportedOperationException) ===");
        BadAllInOneMachine bad = new BadSimplePrinter();
        bad.print("Contract.pdf");
        try {
            bad.fax("Contract.pdf");
        } catch (UnsupportedOperationException ex) {
            System.out.println("[BAD-CRASH] " + ex.getMessage());
        }

        System.out.println("\n=== GOOD (Compile-time safety via role interfaces) ===");
        Printable simple = new LaserPrinter();
        simple.print("Contract.pdf");

        EnterpriseMultifunctionDevice mfp = new EnterpriseMultifunctionDevice();
        mfp.print("Invoice.pdf");
        mfp.scan("ID.png");
    }
}
