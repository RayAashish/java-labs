package lts.java08.async;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Java 8 LTS — CompletableFuture Composable Async Pipelines.
 *
 * Covers:
 * 1. Dependent chaining with `thenCompose` (async flatMap).
 * 2. Parallel fan-out & merge with `thenCombine` and custom bounded I/O Executor.
 * 3. Fault-tolerant partial degradation with `exceptionally` / `handle`.
 * 4. Waiting for heterogeneous async calls with `CompletableFuture.allOf(...)`.
 */
public class CompletableFuturePipelinesDemo {

    record User(String id, String name) {}
    record ProfileDashboard(User user, double walletBalance, int loyaltyPoints) {}

    public static void main(String[] args) {
        System.out.println("=== Java 8 CompletableFuture Async Pipelines Demo ===");

        // Dedicated bounded I/O pool (NEVER share ForkJoinPool.commonPool() for blocking I/O!)
        try (ExecutorService ioPool = Executors.newFixedThreadPool(4)) {

            long start = System.nanoTime();

            // Stage 1: Fetch User, then chain Wallet lookup sequentially via `thenCompose`
            CompletableFuture<User> userFuture = CompletableFuture.supplyAsync(() -> {
                sleepMs(40);
                return new User("U-101", "Alice");
            }, ioPool);

            CompletableFuture<Double> walletFuture = userFuture.thenComposeAsync(user ->
                    CompletableFuture.supplyAsync(() -> {
                        sleepMs(40);
                        return 2450.75;
                    }, ioPool), ioPool);

            // Independent Stage 2 running concurrently with Stage 1 — degrades gracefully on failure!
            CompletableFuture<Integer> loyaltyFuture = CompletableFuture.<Integer>supplyAsync(() -> {
                sleepMs(30);
                if (true) {
                    throw new IllegalStateException("Loyalty service temporarily unavailable");
                }
                return 500;
            }, ioPool).exceptionally(ex -> {
                System.out.println("  [Recover] Fallback for loyalty service -> 0 points (" + ex.getCause().getMessage() + ")");
                return 0;
            });

            // Fan-in merge with `thenCombine`
            CompletableFuture<ProfileDashboard> dashboardFuture = userFuture.thenCombine(walletFuture,
                            (user, balance) -> new ProfileDashboard(user, balance, 0))
                    .thenCombine(loyaltyFuture, (dash, pts) ->
                            new ProfileDashboard(dash.user(), dash.walletBalance(), pts))
                    .orTimeout(2, TimeUnit.SECONDS); // Java 9+ defensive deadline

            ProfileDashboard result = dashboardFuture.join();
            long elapsedMs = (System.nanoTime() - start) / 1_000_000;

            System.out.println("[+] Dashboard built in ~" + elapsedMs + " ms: " + result);

            // Java 8 allOf pattern
            CompletableFuture<String> serviceA = CompletableFuture.supplyAsync(() -> "A_OK", ioPool);
            CompletableFuture<Integer> serviceB = CompletableFuture.supplyAsync(() -> 99, ioPool);
            CompletableFuture.allOf(serviceA, serviceB).join();
            System.out.println("[+] allOf aggregated: " + serviceA.join() + " & " + serviceB.join());
        }
    }

    private static void sleepMs(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
