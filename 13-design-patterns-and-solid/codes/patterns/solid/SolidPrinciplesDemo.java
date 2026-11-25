package patterns.solid;

/**
 * Demonstrates Open/Closed (OCP) + Dependency Inversion (DIP) +
 * Interface Segregation (ISP) in an Order Notification & Pricing service.
 */
public class SolidPrinciplesDemo {

    // DIP & OCP: Depend on an abstraction so new discount policies never modify CheckoutService
    @FunctionalInterface
    interface DiscountPolicy {
        double apply(double subtotal);
    }

    // ISP: Focused single-purpose capability instead of a bloated "MultiChannelManager"
    @FunctionalInterface
    interface OrderNotifier {
        void notifyCustomer(String orderId, double finalAmount);
    }

    static class CheckoutService {
        private final DiscountPolicy discountPolicy;
        private final OrderNotifier notifier;

        // Constructor Dependency Injection (DIP)
        CheckoutService(DiscountPolicy discountPolicy, OrderNotifier notifier) {
            this.discountPolicy = discountPolicy;
            this.notifier = notifier;
        }

        double checkout(String orderId, double subtotal) {
            double finalAmount = discountPolicy.apply(subtotal);
            notifier.notifyCustomer(orderId, finalAmount);
            return finalAmount;
        }
    }

    public static void main(String[] args) {
        DiscountPolicy vipDiscount = total -> total * 0.85;
        OrderNotifier emailNotifier = (id, amt) ->
                System.out.printf("[EMAIL] Order %s confirmed for $%.2f%n", id, amt);

        CheckoutService service = new CheckoutService(vipDiscount, emailNotifier);
        service.checkout("ORD-501", 200.00);
    }
}
