package concurrency.phase4_executors_and_completablefuture;

import java.util.concurrent.*;

/**
 * Author: raysi
 * Demonstrates:
 * 1. CompletableFuture Async Pipelines: supplyAsync, thenApply, thenCompose, thenCombine.
 * 2. Dedicated ThreadPool isolation (avoiding ForkJoinPool.commonPool starvation).
 * 3. Asynchronous Microservices Fan-Out / Fan-In Aggregation with CompletableFuture.allOf().
 * 4. Resilient Exception Handling: exceptionally(), handle(), and completeOnTimeout().
 */
public class CompletableFutureMicroserviceAggregator {

    // Dedicated custom pool for external I/O microservices
    private static final ExecutorService ioExecutor = Executors.newFixedThreadPool(4, r -> {
        Thread t = new Thread(r, "Microservice-IO-Worker");
        t.setDaemon(true);
        return t;
    });

    public static void main(String[] args) throws ExecutionException, InterruptedException {
        System.out.println("======================================================================");
        System.out.println("  PHASE 4 LAB: COMPLETABLEFUTURE ASYNC MICROSERVICE AGGREGATOR");
        System.out.println("  Author: raysi | Package: com.raysi.concurrency");
        System.out.println("======================================================================\n");

        long start = System.currentTimeMillis();
        String userId = "user-8899";

        System.out.println("[Aggregator] Fan-out: Querying 3 microservices concurrently for: " + userId);

        // 1. Fetch User Profile
        CompletableFuture<UserProfile> profileFuture = CompletableFuture
                .supplyAsync(() -> fetchUserProfile(userId), ioExecutor)
                .thenApply(profile -> {
                    // Intermediate sync transformation: format name
                    profile.setName(profile.getName().toUpperCase());
                    return profile;
                });

        // 2. Fetch Orders, then compose with Recommendation Engine (thenCompose = async flatMap)
        CompletableFuture<String> recommendationFuture = CompletableFuture
                .supplyAsync(() -> fetchRecentOrders(userId), ioExecutor)
                .thenCompose(orders -> CompletableFuture.supplyAsync(() -> generateRecommendation(orders), ioExecutor));

        // 3. Fetch Credit Score with Fallback and Timeout Handling
        CompletableFuture<Integer> creditScoreFuture = CompletableFuture
                .supplyAsync(() -> fetchCreditScore(userId), ioExecutor)
                .completeOnTimeout(650, 150, TimeUnit.MILLISECONDS) // Fallback default if API takes >150ms
                .exceptionally(ex -> {
                    System.err.println("  [Fallback] Credit score service failed: " + ex.getMessage() + ". Using default 600.");
                    return 600;
                });

        // 4. Fan-In: Wait for all futures to complete concurrently using CompletableFuture.allOf()
        CompletableFuture<Void> allServicesFuture = CompletableFuture.allOf(
                profileFuture, recommendationFuture, creditScoreFuture);

        // Non-blocking chain when all complete:
        CompletableFuture<AggregatedDashboard> dashboardFuture = allServicesFuture.thenApply(v -> {
            UserProfile profile = profileFuture.join();
            String recommendation = recommendationFuture.join();
            int creditScore = creditScoreFuture.join();
            return new AggregatedDashboard(profile, recommendation, creditScore);
        });

        // Block only at the final boundary:
        AggregatedDashboard dashboard = dashboardFuture.get();
        long duration = System.currentTimeMillis() - start;

        System.out.println("\n=== AGGREGATED DASHBOARD RESULT ===");
        System.out.println(dashboard);
        System.out.printf("Total End-to-End Elapsed Time: %d ms (All calls ran in parallel!)%n", duration);

        ioExecutor.shutdown();

        System.out.println("\n[Key Takeaway for Interviews]:");
        System.out.println("  - Always provide a custom Executor to supplyAsync(); commonPool() can starve JVM!");
        System.out.println("  - thenApply (T -> U): Synchronous mapping (like Stream.map).");
        System.out.println("  - thenCompose (T -> CompletableFuture<U>): Asynchronous chaining (like Stream.flatMap).");
        System.out.println("  - thenCombine (CF<T>, CF<U> -> CF<V>): Combines two independent futures asynchronously.");
        System.out.println("  - completeOnTimeout() and exceptionally() build resilient, self-healing pipelines.");
    }

    private static UserProfile fetchUserProfile(String userId) {
        sleep(100);
        return new UserProfile(userId, "Jane Doe", "jane@example.com");
    }

    private static String fetchRecentOrders(String userId) {
        sleep(120);
        return "Order#101: Mechanical Keyboard, Order#102: 4K Monitor";
    }

    private static String generateRecommendation(String orders) {
        sleep(80);
        return "Recommended: Ergonomic Mouse Mat & Wrist Rest";
    }

    private static int fetchCreditScore(String userId) {
        sleep(300); // Intentionally slow to trigger completeOnTimeout demo!
        return 780;
    }

    private static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    static class UserProfile {
        private final String userId;
        private String name;
        private final String email;

        public UserProfile(String userId, String name, String email) {
            this.userId = userId;
            this.name = name;
            this.email = email;
        }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        @Override
        public String toString() {
            return String.format("User[id=%s, name=%s, email=%s]", userId, name, email);
        }
    }

    static class AggregatedDashboard {
        private final UserProfile profile;
        private final String recommendation;
        private final int creditScore;

        public AggregatedDashboard(UserProfile profile, String recommendation, int creditScore) {
            this.profile = profile;
            this.recommendation = recommendation;
            this.creditScore = creditScore;
        }

        @Override
        public String toString() {
            return String.format("Dashboard:%n  Profile: %s%n  Recommendation: %s%n  Credit Score: %d",
                    profile, recommendation, creditScore);
        }
    }
}
