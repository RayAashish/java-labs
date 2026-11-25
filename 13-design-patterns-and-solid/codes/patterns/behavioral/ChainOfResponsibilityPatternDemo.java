package patterns.behavioral;

/**
 * BEHAVIORAL PATTERN 4 — CHAIN OF RESPONSIBILITY PATTERN
 *
 * WHAT IT IS:
 *   Passes a request sequentially along a chain of handlers. Each handler decides whether
 *   to process/enrich the request, short-circuit/reject it, or pass it to the next handler.
 *
 * WHY IT MATTERS:
 *   Replaces deeply nested `if (authenticated) { if (withinQuota) { if (validBody) ... } }`
 *   spaghetti with composable, reorderable middleware stages!
 *   Real-World Examples: Servlet FilterChain, Spring Security SecurityFilterChain, Netty ChannelPipeline.
 */
public class ChainOfResponsibilityPatternDemo {

    record HttpRequest(String path, String authHeader, int bodyBytes) {}

    abstract static class Middleware {
        private Middleware next;

        public Middleware linkWith(Middleware next) {
            this.next = next;
            return next;
        }

        public boolean handle(HttpRequest req) {
            if (!check(req)) {
                return false; // Short-circuit pipeline immediately!
            }
            return next == null || next.handle(req);
        }

        protected abstract boolean check(HttpRequest req);
    }

    static class AuthMiddleware extends Middleware {
        @Override
        protected boolean check(HttpRequest req) {
            boolean ok = req.authHeader() != null && req.authHeader().startsWith("Bearer ");
            System.out.println("  [1. Auth]      " + (ok ? "PASS" : "401 Unauthorized — chain aborted!"));
            return ok;
        }
    }

    static class PayloadLimitMiddleware extends Middleware {
        private final int maxBytes;
        PayloadLimitMiddleware(int maxBytes) { this.maxBytes = maxBytes; }

        @Override
        protected boolean check(HttpRequest req) {
            boolean ok = req.bodyBytes() <= maxBytes;
            System.out.println("  [2. SizeCheck] " + (ok ? "PASS" : "413 Payload Too Large — chain aborted!"));
            return ok;
        }
    }

    static class ControllerDispatchMiddleware extends Middleware {
        @Override
        protected boolean check(HttpRequest req) {
            System.out.println("  [3. Controller] Served 200 OK for " + req.path());
            return true;
        }
    }

    public static void main(String[] args) {
        Middleware chain = new AuthMiddleware();
        chain.linkWith(new PayloadLimitMiddleware(1024))
             .linkWith(new ControllerDispatchMiddleware());

        System.out.println("--- Request 1 (Valid) ---");
        chain.handle(new HttpRequest("/api/orders", "Bearer jwt-123", 512));

        System.out.println("\n--- Request 2 (Missing Token) ---");
        chain.handle(new HttpRequest("/api/orders", null, 512));

        System.out.println("\n--- Request 3 (Oversized Payload) ---");
        chain.handle(new HttpRequest("/api/orders", "Bearer jwt-123", 4096));
    }
}
