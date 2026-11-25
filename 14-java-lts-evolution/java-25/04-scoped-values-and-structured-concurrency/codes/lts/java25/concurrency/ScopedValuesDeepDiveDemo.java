package lts.java25.concurrency;

import java.util.NoSuchElementException;

/**
 * Java 25 LTS — Finalized ScopedValue (JEP 506 — NO preview flag required!).
 *
 * Covers:
 * 1. Lexically bounded context propagation without manual .remove() cleanup bugs.
 * 2. Safe nested dynamic rebinding (inner scope shadows outer, then restores automatically).
 * 3. Unbound check (`isBound()`, `orElse(...)`, and clean `NoSuchElementException` after exit).
 */
public class ScopedValuesDeepDiveDemo {

    private static final ScopedValue<String> TENANT_ID = ScopedValue.newInstance();
    private static final ScopedValue<String> TRACE_ID = ScopedValue.newInstance();

    public static void main(String[] args) throws Exception {
        System.out.println("=== Java 25 Finalized ScopedValue (JEP 506) Demo ===");

        // Chain multiple bindings fluently in one scope!
        String response = ScopedValue.where(TENANT_ID, "acme-prod")
                .where(TRACE_ID, "trace-1001")
                .call(() -> handleHttpRequest());

        System.out.println("[Main] Request response = " + response);
        System.out.println("[Main] Safely auto-unbound outside scope? isBound=" + TENANT_ID.isBound()
                + ", fallback=" + TENANT_ID.orElse("NONE"));

        try {
            TENANT_ID.get();
        } catch (NoSuchElementException expected) {
            System.out.println("[Main] Calling .get() outside scope rejects stale reads: " + expected.getMessage());
        }
    }

    private static String handleHttpRequest() throws Exception {
        System.out.printf("  [Controller] tenant=%s, trace=%s%n", TENANT_ID.get(), TRACE_ID.get());

        // Temporarily rebind TENANT_ID inside nested scope for elevated internal billing lookup!
        String elevatedAudit = ScopedValue.where(TENANT_ID, "internal-system-admin")
                .call(() -> {
                    System.out.printf("    [Elevated Block] shadowed tenant=%s (still keeps trace=%s!)%n",
                            TENANT_ID.get(), TRACE_ID.get());
                    return "AUDITED_BY_" + TENANT_ID.get();
                });

        // Enclosing outer binding is cleanly restored automatically!
        System.out.printf("  [Controller Resumed] restored tenant=%s%n", TENANT_ID.get());
        return elevatedAudit;
    }
}
