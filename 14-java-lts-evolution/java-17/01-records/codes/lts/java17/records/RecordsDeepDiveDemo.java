package lts.java17.records;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.RecordComponent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Java 17 LTS — Records (JEP 395) Deep Dive.
 *
 * Covers:
 * 1. Compact constructor validation, canonical normalization, and defensive copying (Shallow Immutability Trap fix).
 * 2. Safe deterministic composite HashMap keys + Functional "wither" copies.
 * 3. Safe Record deserialization: canonical constructor IS invoked on ObjectInputStream readObject!
 * 4. Reflection introspection via Class.isRecord() & getRecordComponents().
 */
public class RecordsDeepDiveDemo {

    public record TenantContextKey(String tenantId, UUID userId, List<String> roles) implements Serializable {

        // Compact Constructor runs before final fields are written!
        public TenantContextKey {
            Objects.requireNonNull(tenantId, "tenantId required");
            Objects.requireNonNull(userId, "userId required");
            tenantId = tenantId.strip().toLowerCase();
            if (tenantId.isBlank()) {
                throw new IllegalArgumentException("tenantId cannot be blank");
            }
            // Defensive copy guarantees external List mutation CANNOT corrupt HashMap bucket hash!
            roles = (roles == null) ? List.of() : List.copyOf(roles);
        }

        // Custom overloaded non-canonical constructor MUST delegate to canonical via `this(...)`
        public TenantContextKey(String tenantId, UUID userId) {
            this(tenantId, userId, List.of("VIEWER"));
        }

        // Functional "Wither" pattern instead of mutator setters
        public TenantContextKey withTenantId(String nextTenant) {
            return new TenantContextKey(nextTenant, this.userId, this.roles);
        }
    }

    public static void main(String[] args) throws Exception {
        System.out.println("=== Java 17 Records (JEP 395) Deep Dive ===");

        UUID uid = UUID.randomUUID();
        List<String> externalMutableRoles = new ArrayList<>(List.of("ADMIN", "BILLING"));

        // 1. Defensive copying & normalization
        TenantContextKey key1 = new TenantContextKey("  CORP-ALPHA ", uid, externalMutableRoles);
        externalMutableRoles.add("ROGUE_HACKER_ROLE"); // Does NOT affect key1!

        Map<TenantContextKey, String> cache = new HashMap<>();
        cache.put(key1, "ACTIVE_SESSION_XYZ");

        // Structural equality across distinct allocations
        TenantContextKey key2 = new TenantContextKey("corp-alpha", uid, List.of("ADMIN", "BILLING"));
        System.out.printf("[1] key1==key2: %s | key1.equals(key2): %s | cache lookup: %s%n",
                key1 == key2, key1.equals(key2), cache.get(key2));
        System.out.println("    Defensive copy protected roles: " + key1.roles());

        // 2. Functional "wither" copy preserves immutability
        TenantContextKey switched = key1.withTenantId("corp-beta");
        System.out.println("[2] Wither copy: " + switched + " | Original intact: " + key1.tenantId());

        // 3. Serialization always runs canonical constructor (prevents invariant bypass!)
        byte[] serialized;
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             ObjectOutputStream oos = new ObjectOutputStream(baos)) {
            oos.writeObject(key1);
            serialized = baos.toByteArray();
        }
        try (ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(serialized))) {
            TenantContextKey restored = (TenantContextKey) ois.readObject();
            System.out.println("[3] Deserialized cleanly via constructor: equals=" + key1.equals(restored));
        }

        // 4. Reflection metadata API
        System.out.println("[4] isRecord=" + TenantContextKey.class.isRecord() + ", components=" +
                Arrays.stream(TenantContextKey.class.getRecordComponents())
                        .map(RecordComponent::getName)
                        .toList());
    }
}
