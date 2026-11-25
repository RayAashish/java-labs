package exceptions.phase0_beginner;

/**
 * Beginner-friendly walkthrough of try/catch/finally, throw vs throws,
 * custom unchecked exceptions, and try-with-resources cleanup.
 */
public class ExceptionBasicsForBeginnersDemo {

    static class InsufficientFundsException extends RuntimeException {
        public InsufficientFundsException(String message) {
            super(message);
        }
    }

    static class SimpleResource implements AutoCloseable {
        @Override
        public void close() {
            System.out.println("[Cleanup] SimpleResource automatically closed!");
        }
    }

    public static void main(String[] args) {
        System.out.println("=== 1. Basic try / catch / finally ===");
        try {
            int parsed = Integer.parseInt("42A");
            System.out.println("Parsed: " + parsed);
        } catch (NumberFormatException e) {
            System.out.println("Caught invalid number format: " + e.getMessage());
        } finally {
            System.out.println("Finally always runs after try/catch.");
        }

        System.out.println("\n=== 2. Custom Domain Exception ===");
        try {
            withdraw(100.0, 250.0);
        } catch (InsufficientFundsException e) {
            System.out.println("Transaction declined: " + e.getMessage());
        }

        System.out.println("\n=== 3. Modern Try-With-Resources ===");
        try (SimpleResource res = new SimpleResource()) {
            System.out.println("Using " + res.getClass().getSimpleName() + " safely.");
        }
    }

    private static void withdraw(double balance, double amount) {
        if (amount > balance) {
            throw new InsufficientFundsException("Requested $" + amount + " but balance is $" + balance);
        }
    }
}
