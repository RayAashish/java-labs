package lts.java08.interfaces;

/**
 * Java 8 LTS — Interface `default` & `static` Methods + Diamond Resolution.
 *
 * Covers:
 * 1. Evolving an interface safely with `default` without breaking legacy implementors.
 * 2. Resolving a Diamond Conflict using `InterfaceName.super.method()`.
 * 3. Rule 1: Concrete/abstract superclass wins over interface default.
 * 4. `static` interface factory/utility methods (callable only via InterfaceName).
 */
public class DefaultAndStaticMethodsDemo {

    interface Auditable {
        String id();

        // Added later without breaking legacy classes!
        default String describe() {
            return "Auditable[" + id() + "]";
        }

        // Static utility directly on the interface (no separate AuditUtils class needed)
        static String formatTag(String prefix, Auditable item) {
            return prefix + ":" + item.id();
        }
    }

    interface Cacheable {
        String id();

        default String describe() {
            return "Cacheable[" + id() + "]";
        }
    }

    // Legacy implementor untouched when `Auditable.describe()` was added
    record LegacyInvoice(String id) implements Auditable {}

    // Diamond conflict: Both Auditable and Cacheable define default describe()!
    // Must explicitly override and can pick/combine via Interface.super.method().
    record SmartEntity(String id) implements Auditable, Cacheable {
        @Override
        public String describe() {
            return Auditable.super.describe() + " + " + Cacheable.super.describe();
        }
    }

    // Rule 1 demonstration: Class method ALWAYS wins over interface default method!
    static class BaseLabelled {
        public String describe() {
            return "ClassWinsOverInterface";
        }
    }

    static class SubWithClassWin extends BaseLabelled implements Auditable {
        @Override
        public String id() { return "SUB-1"; }
        // Inherits BaseLabelled.describe() automatically — no diamond error!
    }

    public static void main(String[] args) {
        System.out.println("=== Java 8 Interface default & static Methods Demo ===");

        LegacyInvoice inv = new LegacyInvoice("INV-101");
        System.out.println("[1] Legacy class inherits default describe(): " + inv.describe());
        System.out.println("    Interface static helper: " + Auditable.formatTag("TAG", inv));

        SmartEntity smart = new SmartEntity("ENT-77");
        System.out.println("[2] Diamond resolved via Interface.super: " + smart.describe());

        SubWithClassWin sub = new SubWithClassWin();
        System.out.println("[3] Class-wins-over-default rule: " + sub.describe());
    }
}
