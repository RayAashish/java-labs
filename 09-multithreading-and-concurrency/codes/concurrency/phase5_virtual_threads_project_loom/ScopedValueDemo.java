package concurrency.phase5_virtual_threads_project_loom;

/**
 * Demonstrates Java 25 ScopedValue (JEP 506, finalized in JDK 25):
 * Safe, immutable, bounded per-thread context sharing that replaces
 * leak-prone ThreadLocal variables for Virtual Threads and request scopes.
 */
public class ScopedValueDemo {

    private static final ScopedValue<String> REQUEST_TRACE_ID = ScopedValue.newInstance();
    private static final ScopedValue<String> CURRENT_USER = ScopedValue.newInstance();

    public static void main(String[] args) {
        System.out.println("=== Java 25 ScopedValue Bounded Context Demo ===");

        ScopedValue.where(REQUEST_TRACE_ID, "trace-98f21")
                .where(CURRENT_USER, "aashish")
                .run(() -> {
                    handleController();
                    // Rebinding creates a nested lexical scope without mutating the outer frame
                    ScopedValue.where(CURRENT_USER, "system-admin")
                            .run(() -> System.out.println("Nested scope user: " + CURRENT_USER.get()));
                    System.out.println("Restored outer scope user: " + CURRENT_USER.get());
                });

        System.out.println("Outside bounded scope, isBound? " + REQUEST_TRACE_ID.isBound());
    }

    private static void handleController() {
        System.out.println("[Controller] trace=" + REQUEST_TRACE_ID.get() + ", user=" + CURRENT_USER.get());
        callService();
    }

    private static void callService() {
        System.out.println("[Service]    trace=" + REQUEST_TRACE_ID.get() + ", user=" + CURRENT_USER.get());
    }
}
