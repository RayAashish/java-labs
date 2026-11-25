package patterns.behavioral;

/**
 * BEHAVIORAL PATTERN 6 — STATE PATTERN
 *
 * WHAT IT IS:
 *   Allows an object to alter its behavior when its internal state changes — modeling each
 *   state as a dedicated class instead of scattering `switch (status)` across every method.
 *
 * WHY IT MATTERS:
 *   In an Order lifecycle (NEW -> PAID -> SHIPPED -> DELIVERED / CANCELLED), scattering
 *   status checks across `pay()`, `ship()`, and `cancel()` creates brittle state-transition bugs.
 *   State objects enforce valid transitions explicitly per state!
 */
public class StatePatternDemo {

    interface OrderState {
        String name();
        OrderState pay(OrderContext ctx);
        OrderState ship(OrderContext ctx);
        OrderState cancel(OrderContext ctx);
    }

    static class OrderContext {
        private OrderState state = new NewOrderState();

        public void pay()    { state = state.pay(this); }
        public void ship()   { state = state.ship(this); }
        public void cancel() { state = state.cancel(this); }
        public String currentState() { return state.name(); }
    }

    static class NewOrderState implements OrderState {
        @Override public String name() { return "NEW"; }
        @Override public OrderState pay(OrderContext ctx) {
            System.out.println("  Payment captured -> transitioning NEW -> PAID");
            return new PaidOrderState();
        }
        @Override public OrderState ship(OrderContext ctx) {
            System.out.println("  [REJECTED] Cannot ship unpaid order!");
            return this;
        }
        @Override public OrderState cancel(OrderContext ctx) {
            System.out.println("  Order cancelled -> CANCELLED");
            return new CancelledOrderState();
        }
    }

    static class PaidOrderState implements OrderState {
        @Override public String name() { return "PAID"; }
        @Override public OrderState pay(OrderContext ctx) {
            System.out.println("  [IGNORED] Already paid!");
            return this;
        }
        @Override public OrderState ship(OrderContext ctx) {
            System.out.println("  Dispatched parcel -> transitioning PAID -> SHIPPED");
            return new ShippedOrderState();
        }
        @Override public OrderState cancel(OrderContext ctx) {
            System.out.println("  Refunding payment -> CANCELLED");
            return new CancelledOrderState();
        }
    }

    static class ShippedOrderState implements OrderState {
        @Override public String name() { return "SHIPPED"; }
        @Override public OrderState pay(OrderContext ctx)    { System.out.println("  [IGNORED] Already paid"); return this; }
        @Override public OrderState ship(OrderContext ctx)   { System.out.println("  [IGNORED] Already shipped"); return this; }
        @Override public OrderState cancel(OrderContext ctx) { System.out.println("  [REJECTED] Already shipped — open a return instead!"); return this; }
    }

    static class CancelledOrderState implements OrderState {
        @Override public String name() { return "CANCELLED"; }
        @Override public OrderState pay(OrderContext ctx)    { System.out.println("  [REJECTED] Cancelled order"); return this; }
        @Override public OrderState ship(OrderContext ctx)   { System.out.println("  [REJECTED] Cancelled order"); return this; }
        @Override public OrderState cancel(OrderContext ctx) { System.out.println("  [IGNORED] Already cancelled"); return this; }
    }

    public static void main(String[] args) {
        OrderContext order = new OrderContext();
        order.ship();   // Rejected cleanly in NEW state
        order.pay();    // NEW -> PAID
        order.ship();   // PAID -> SHIPPED
        order.cancel(); // Rejected cleanly in SHIPPED state
        System.out.println("Final Order State: " + order.currentState());
    }
}
