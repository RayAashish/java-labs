package patterns.behavioral;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * BEHAVIORAL PATTERN 2 — OBSERVER PATTERN (Pub-Sub)
 *
 * WHAT IT IS:
 *   Defines a 1-to-many dependency so when a Subject changes state, all registered Observers
 *   are automatically notified without the Subject knowing their concrete classes.
 *
 * WHY IT MATTERS:
 *   Without Observer, OrderService/StockTicker directly calls `emailService.send()`,
 *   `analyticsService.record()`, and `mobilePush.notify()` — coupling core state changes to
 *   every downstream consumer. Note: Remember to provide `unsubscribe()` to prevent listener leaks!
 */
public class ObserverPatternDemo {

    interface PriceObserver {
        void onPriceUpdate(String ticker, double newPrice);
    }

    // ✅ GOOD CODE: Thread-safe publisher decoupled from concrete listeners.
    static class StockMarketFeed {
        // CopyOnWriteArrayList prevents ConcurrentModificationException if listener unsubscribes during notify
        private final List<PriceObserver> observers = new CopyOnWriteArrayList<>();

        public void subscribe(PriceObserver observer) {
            observers.add(observer);
        }

        public void unsubscribe(PriceObserver observer) {
            observers.remove(observer);
        }

        public void publishPrice(String ticker, double price) {
            System.out.printf("%n[MarketFeed] %s -> $%.2f%n", ticker, price);
            for (PriceObserver observer : observers) {
                observer.onPriceUpdate(ticker, price);
            }
        }
    }

    public static void main(String[] args) {
        StockMarketFeed feed = new StockMarketFeed();

        PriceObserver tradingBot = (t, p) -> {
            if (p < 180.0) System.out.println("  [TradingBot] BUY signal triggered on " + t + " @ $" + p);
        };
        PriceObserver dashboard = (t, p) -> System.out.println("  [UI Dashboard] Refreshed chart: " + t + "=$" + p);

        feed.subscribe(tradingBot);
        feed.subscribe(dashboard);

        feed.publishPrice("AAPL", 185.50);
        feed.publishPrice("AAPL", 178.20);

        feed.unsubscribe(tradingBot); // Always unsubscribe short-lived listeners to avoid memory leaks!
        feed.publishPrice("AAPL", 175.00);
    }
}
