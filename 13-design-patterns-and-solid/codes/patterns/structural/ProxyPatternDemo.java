package patterns.structural;

import java.util.HashMap;
import java.util.Map;

/**
 * STRUCTURAL PATTERN 3 — PROXY PATTERN
 *
 * WHAT IT IS:
 *   Provides a surrogate/placeholder object with the EXACT SAME interface as a real object
 *   to intercept and control access (caching, authorization, lazy loading, metrics).
 *
 * WHY IT MATTERS:
 *   This is literally how Spring's @Cacheable, @Transactional, and @PreAuthorize work!
 *   Cross-cutting concerns (caching, security) stay completely out of core business logic.
 *
 * IN THIS FILE:
 *   1. BAD CODE:  RealUserRepo mixes DB lookup with caching & permission checks.
 *   2. GOOD CODE: SecureCachingRepoProxy wraps UserRepo cleanly, enforcing RBAC + caching.
 */
public class ProxyPatternDemo {

    interface ReportFetcher {
        String fetchReport(String role, String reportId);
    }

    static class DatabaseReportFetcher implements ReportFetcher {
        @Override
        public String fetchReport(String role, String reportId) {
            System.out.println("   [DB QUERY] Running expensive SQL for " + reportId + "...");
            return "CONFIDENTIAL_DATA(" + reportId + ")";
        }
    }

    // ❌ BAD CODE: Mixes security checks, cache management, and SQL inside one class.
    static class BadMonolithFetcher {
        private final Map<String, String> cache = new HashMap<>();
        public String fetch(String role, String id) {
            if (!"ADMIN".equals(role)) throw new SecurityException("Denied");
            return cache.computeIfAbsent(id, k -> "SQL_DATA(" + k + ")");
        }
    }

    // ✅ GOOD CODE: Protection + Caching Proxy intercepts calls transparently!
    static class SecureCachingReportProxy implements ReportFetcher {
        private final ReportFetcher target;
        private final Map<String, String> cache = new HashMap<>();

        SecureCachingReportProxy(ReportFetcher target) {
            this.target = target;
        }

        @Override
        public String fetchReport(String role, String reportId) {
            if (!"ADMIN".equalsIgnoreCase(role)) {
                throw new SecurityException("Access denied for role=" + role);
            }
            if (cache.containsKey(reportId)) {
                System.out.println("   [PROXY CACHE HIT] Returning cached " + reportId);
                return cache.get(reportId);
            }
            String result = target.fetchReport(role, reportId);
            cache.put(reportId, result);
            return result;
        }
    }

    public static void main(String[] args) {
        ReportFetcher fetcher = new SecureCachingReportProxy(new DatabaseReportFetcher());

        System.out.println("=== 1. Unauthorized Caller Blocked by Proxy ===");
        try {
            fetcher.fetchReport("GUEST", "Q4-PNL");
        } catch (SecurityException ex) {
            System.out.println("Blocked cleanly: " + ex.getMessage());
        }

        System.out.println("\n=== 2. Authorized First Call (DB) vs Second Call (Cache Hit) ===");
        System.out.println("Call 1: " + fetcher.fetchReport("ADMIN", "Q4-PNL"));
        System.out.println("Call 2: " + fetcher.fetchReport("ADMIN", "Q4-PNL"));
    }
}
