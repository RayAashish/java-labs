package ionio.phase1_classic_io_streams.serialization;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamException;
import java.io.Serial;
import java.io.Serializable;
import java.util.Base64;

/**
 * Author: raysi
 * Package: com.raysi.ionio.phase1_classic_io_streams.serialization.codes
 *
 * Demonstrates:
 * 1. The transient keyword: Excluding sensitive credentials from default serialization.
 * 2. Custom hooks: private writeObject() and readObject() for encrypted serialization.
 * 3. The Singleton serialization trap and its solution: readResolve().
 * 4. Byte inspection of the Java serialization stream header (0xACED 0x0005).
 */
public class CustomSerializationTransientDemo {

    public static void main(String[] args) throws IOException, ClassNotFoundException {
        System.out.println("==========================================================================");
        System.out.println(">>> DEMO: Custom Serialization, Transient Fields & Singleton Trap <<<");
        System.out.println(">>> Author: raysi | Object Serialization Internals");
        System.out.println("==========================================================================\n");

        // Part 1: Custom Serialization with transient password encryption
        System.out.println("--- Part 1: Custom writeObject / readObject with Transient Encryption ---");
        UserProfile originalProfile = new UserProfile(101L, "raysi", "SuperSecretPassword123!");
        System.out.println("[Step 1] Original Object: " + originalProfile);

        byte[] serializedData = serialize(originalProfile);
        System.out.println("[Step 2] Serialized Payload Size: " + serializedData.length + " bytes");
        inspectStreamHeader(serializedData);

        UserProfile deserializedProfile = (UserProfile) deserialize(serializedData);
        System.out.println("[Step 3] Deserialized Object: " + deserializedProfile);
        System.out.println("   -> Password safely recovered via custom decryption hook: " + deserializedProfile.getPassword());
        System.out.println();

        // Part 2: The Singleton Deserialization Trap & readResolve()
        System.out.println("--- Part 2: Singleton Serialization Invariant Violation & readResolve() ---");
        DatabaseConnectionPool instance1 = DatabaseConnectionPool.getInstance();
        System.out.println("[Step 1] Initial Singleton Instance: " + instance1 + " (Hash: " + System.identityHashCode(instance1) + ")");

        byte[] singletonBytes = serialize(instance1);
        DatabaseConnectionPool instance2 = (DatabaseConnectionPool) deserialize(singletonBytes);
        System.out.println("[Step 2] Deserialized Singleton:     " + instance2 + " (Hash: " + System.identityHashCode(instance2) + ")");

        if (instance1 == instance2) {
            System.out.println("   -> SUCCESS: instance1 == instance2! readResolve() preserved the Singleton invariant.");
        } else {
            System.err.println("   -> FAILURE: New instance spawned! Singleton contract broken.");
        }

        System.out.println("\n==========================================================================");
        System.out.println(">>> TAKEAWAY <<<");
        System.out.println("1. transient prevents fields from being written by defaultWriteObject().");
        System.out.println("2. Custom writeObject/readObject allow manual encryption of transient data.");
        System.out.println("3. Without readResolve(), deserialization creates a brand new object instance");
        System.out.println("   bypassing the private constructor, destroying the Singleton pattern!");
        System.out.println("==========================================================================");
    }

    private static void inspectStreamHeader(byte[] data) {
        if (data.length >= 4) {
            System.out.printf("   ├── Magic Number: 0x%02X%02X (Expected: 0xACED - STREAM_MAGIC)%n", data[0], data[1]);
            System.out.printf("   └── Version:      0x%02X%02X (Expected: 0x0005 - STREAM_VERSION)%n", data[2], data[3]);
        }
    }

    private static byte[] serialize(Object obj) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ObjectOutputStream oos = new ObjectOutputStream(baos)) {
            oos.writeObject(obj);
            oos.flush();
        }
        return baos.toByteArray();
    }

    private static Object deserialize(byte[] data) throws IOException, ClassNotFoundException {
        try (ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(data))) {
            return ois.readObject();
        }
    }

    /**
     * Demonstrates custom serialization of transient sensitive data.
     */
    static class UserProfile implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;

        private final Long userId;
        private final String username;

        // transient prevents this field from being written by defaultWriteObject()
        private transient String password;

        public UserProfile(Long userId, String username, String password) {
            this.userId = userId;
            this.username = username;
            this.password = password;
        }

        public String getPassword() {
            return password;
        }

        // Custom write hook invoked via reflection during serialization
        @Serial
        private void writeObject(ObjectOutputStream out) throws IOException {
            out.defaultWriteObject(); // Serializes userId and username normally
            // Custom logic: XOR obfuscation / Base64 encoding of sensitive transient field
            String encryptedPassword = obfuscate(password);
            out.writeUTF(encryptedPassword);
        }

        // Custom read hook invoked via reflection during deserialization
        @Serial
        private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
            in.defaultReadObject(); // Deserializes userId and username normally
            // Custom logic: Reconstruct transient field from stream
            String encryptedPassword = in.readUTF();
            this.password = deobfuscate(encryptedPassword);
        }

        private String obfuscate(String raw) {
            byte[] bytes = raw.getBytes();
            for (int i = 0; i < bytes.length; i++) {
                bytes[i] = (byte) (bytes[i] ^ 0x5A); // Simple XOR mask
            }
            return Base64.getEncoder().encodeToString(bytes);
        }

        private String deobfuscate(String encoded) {
            byte[] bytes = Base64.getDecoder().decode(encoded);
            for (int i = 0; i < bytes.length; i++) {
                bytes[i] = (byte) (bytes[i] ^ 0x5A);
            }
            return new String(bytes);
        }

        @Override
        public String toString() {
            return "UserProfile{id=" + userId + ", user='" + username + "', pass='" + password + "'}";
        }
    }

    /**
     * Demonstrates readResolve() to preserve Singleton invariants during deserialization.
     */
    static class DatabaseConnectionPool implements Serializable {
        @Serial
        private static final long serialVersionUID = 42L;

        private static final DatabaseConnectionPool INSTANCE = new DatabaseConnectionPool();

        private final String poolName = "ProductionPrimaryPool";

        private DatabaseConnectionPool() {
            // Prevent multiple instances via reflection
            if (INSTANCE != null) {
                throw new IllegalStateException("Singleton already initialized!");
            }
        }

        public static DatabaseConnectionPool getInstance() {
            return INSTANCE;
        }

        // CRITICAL: readResolve() replaces the deserialized object with the true singleton instance
        @Serial
        protected Object readResolve() throws ObjectStreamException {
            return INSTANCE;
        }

        @Override
        public String toString() {
            return "DatabaseConnectionPool[name=" + poolName + "]";
        }
    }
}
