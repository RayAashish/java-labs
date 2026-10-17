package com.raysi.lts.java17.codes;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Author: raysi
 * Package: com.raysi.lts.java17.codes
 *
 * Demonstrates:
 * 1. Java 17 Records as safe, deterministic composite Map keys.
 * 2. Compact constructor validation and canonical constructor invariants.
 * 3. Deep immutability enforcement (defensive copying of mutable collections).
 * 4. Bytecode-synthesized equals() and hashCode() consistency across distinct instances.
 * 5. Functional mutation ("wither" pattern) without violating record immutability.
 */
public class ImmutableRecordAsMapKeyDemo {

    /**
     * Composite Key Record: TenantContextKey
     * Used for multi-tenant caching and lookups where tenant isolation is strictly required.
     */
    public record TenantContextKey(String tenantId, UUID userId, List<String> roles) {

        // Compact Constructor: Enforces invariants and deep immutability
        public TenantContextKey {
            Objects.requireNonNull(tenantId, "tenantId must not be null");
            Objects.requireNonNull(userId, "userId must not be null");

            if (tenantId.isBlank()) {
                throw new IllegalArgumentException("tenantId cannot be blank");
            }

            // CRITICAL: Defensive copy to prevent external mutation from compromising Map hashing
            roles = (roles == null) ? List.of() : List.copyOf(roles);
        }

        // Convenience Constructor
        public TenantContextKey(String tenantId, UUID userId) {
            this(tenantId, userId, List.of());
        }

        // Functional "Wither" Pattern: Returns a new instance with updated tenantId
        public TenantContextKey withTenantId(String newTenantId) {
            return new TenantContextKey(newTenantId, this.userId, this.roles);
        }
    }

    /**
     * Record representing cached session state payload
     */
    public record UserSessionPayload(String username, long loginEpochMs, boolean active) {}

    public static void main(String[] args) {
        System.out.println("=== Java 17 Record As Map Key & Immutability Verification ===");

        UUID sharedUserId = UUID.randomUUID();
        String tenant = "corp-alpha";
        List<String> mutableRolesList = new java.util.ArrayList<>(List.of("ADMIN", "BILLING"));

        // 1. Create initial key and put into HashMap
        TenantContextKey key1 = new TenantContextKey(tenant, sharedUserId, mutableRolesList);
        UserSessionPayload session = new UserSessionPayload("raysi_admin", System.currentTimeMillis(), true);

        Map<TenantContextKey, UserSessionPayload> sessionCache = new HashMap<>();
        sessionCache.put(key1, session);
        System.out.println("[+] Cache entry stored for Key1: " + key1);

        // 2. Demonstrate defensive copying: mutating the original list does NOT corrupt the record or map key
        mutableRolesList.add("SUPER_USER_INTRUDER");
        System.out.println("[+] Mutated external roles list. Record roles: " + key1.roles());

        // 3. Create Key2 with identical values in a separate memory allocation
        TenantContextKey key2 = new TenantContextKey(
                new String("corp-alpha"),
                UUID.fromString(sharedUserId.toString()),
                List.of("ADMIN", "BILLING")
        );

        System.out.println("[+] key1 == key2: " + (key1 == key2) + " (Distinct heap references)");
        System.out.println("[+] key1.equals(key2): " + key1.equals(key2) + " (Value equality)");
        System.out.println("[+] key1.hashCode() == key2.hashCode(): " + (key1.hashCode() == key2.hashCode()));

        // 4. Retrieve value using key2
        UserSessionPayload retrieved = sessionCache.get(key2);
        System.out.println("[+] Retrieved from cache using key2: " + retrieved);
        assert retrieved != null : "Cache lookup failed with identical record value!";

        // 5. Demonstrate Record "Wither" Transformation
        TenantContextKey switchedTenantKey = key1.withTenantId("corp-beta");
        System.out.println("[+] Switched tenant key (wither): " + switchedTenantKey);
        System.out.println("[+] Original key tenant: " + key1.tenantId());
    }
}
