package functional.optional;

import java.util.Optional;

/**
 * Demonstrates Optional fluent pipelines, flatMap unnesting,
 * and the classic orElse() vs orElseGet() eager-evaluation trap.
 */
public class OptionalBestPracticesDemo {

    record Profile(Optional<String> githubHandle) {}
    record User(String name, Profile profile) {}

    public static void main(String[] args) {
        System.out.println("=== 1. map() vs flatMap() ===");
        User aashish = new User("Aashish", new Profile(Optional.of("RayAashish")));
        User anonymous = new User("Guest", new Profile(Optional.empty()));

        System.out.println("Aashish handle:   " + extractGithubHandle(aashish));
        System.out.println("Anonymous handle: " + extractGithubHandle(anonymous));

        System.out.println("\n=== 2. orElse() vs orElseGet() Trap ===");
        Optional<String> present = Optional.of("CACHED_TOKEN");

        System.out.println("-- Calling orElse(expensiveFallback()):");
        String r1 = present.orElse(expensiveFallback("orElse")); // STILL EXECUTES expensiveFallback!
        System.out.println("Result 1: " + r1);

        System.out.println("-- Calling orElseGet(() -> expensiveFallback()):");
        String r2 = present.orElseGet(() -> expensiveFallback("orElseGet")); // Skipped lazily!
        System.out.println("Result 2: " + r2);
    }

    private static String extractGithubHandle(User user) {
        return Optional.ofNullable(user)
                .map(User::profile)
                .flatMap(Profile::githubHandle)
                .map(handle -> "@" + handle)
                .orElse("@no-github");
    }

    private static String expensiveFallback(String caller) {
        System.out.println("   [SIDE EFFECT] expensiveFallback() invoked by " + caller + "!");
        return "GENERATED_FALLBACK_TOKEN";
    }
}
