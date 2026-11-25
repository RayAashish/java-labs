package oops.c05_polymorphism.dynamic_polymorphism;

/**
 * Demonstrates the overriding visibility invariant:
 * Subclasses may WIDEN access (protected -> public), but may NEVER narrow it.
 */
public class AccessModifierWidening {
    public static void main(String[] args) {
        BaseNotifier notifier = new EmailNotifier();
        notifier.sendAlert();
    }

    static class BaseNotifier {
        protected void sendAlert() {
            System.out.println("Base protected alert");
        }
    }

    static class EmailNotifier extends BaseNotifier {
        // Widening protected -> public is legal; narrowing to private/package-private fails compilation
        @Override
        public void sendAlert() {
            System.out.println("Public email alert (widened visibility)");
        }
    }
}
