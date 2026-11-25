package exceptions.phase4_oop_inheritance_and_lambdas;

import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * Author: raysi
 * Package: com.raysi.exceptions.phase4_oop_inheritance_and_lambdas.codes
 *
 * Demonstrates:
 * 1. Method Overriding Exception Rules (Liskov Substitution Principle).
 * 2. Child method throwing narrower checked exception (FileNotFoundException vs IOException).
 * 3. Child method throwing NO checked exception.
 * 4. Freedom of child method to throw Unchecked Exceptions (RuntimeException).
 * 5. Constructor Exception Rules: Inversion where child constructor MUST declare equal or broader.
 */
public class OverridingExceptionRulesCovariantDemo {

    // Base Service Contract
    public static class RemoteStorageEngine {
        public void transfer(String source, String destination) throws IOException {
            System.out.println("  [RemoteStorageEngine] Transferring from " + source + " to " + destination);
            if (source == null || source.isEmpty()) {
                throw new IOException("RemoteStorageEngine: Source cannot be blank");
            }
        }

        public void healthCheck() {
            System.out.println("  [RemoteStorageEngine] Base health check: OK");
        }
    }

    // Subclass A: Narrows the throws clause to FileNotFoundException
    public static class LocalStorageEngine extends RemoteStorageEngine {
        @Override
        public void transfer(String source, String destination) throws FileNotFoundException {
            System.out.println("  [LocalStorageEngine] Fast local transfer: " + source);
            if ("nonexistent.dat".equals(source)) {
                // FileNotFoundException is a narrower subclass of IOException -> VALID LSP
                throw new FileNotFoundException("Local file not found on disk: " + source);
            }
        }
    }

    // Subclass B: Omits throws clause completely and adds unchecked exceptions
    public static class MemoryStorageEngine extends RemoteStorageEngine {
        @Override
        public void transfer(String source, String destination) {
            System.out.println("  [MemoryStorageEngine] In-memory transfer (No I/O can fail) -> No throws clause");
        }

        @Override
        public void healthCheck() throws IllegalStateException, IllegalArgumentException {
            // Unchecked exceptions can be added freely in overriding methods!
            System.out.println("  [MemoryStorageEngine] Health check verifying heap buffers.");
        }
    }

    // Constructor Exception Rule Demonstration:
    public static class BaseConnection {
        public BaseConnection() throws IOException {
            System.out.println("  [BaseConnection] Initializing socket connection...");
            // Checked exception declared
        }
    }

    public static class SecureEncryptedConnection extends BaseConnection {
        // Child constructor CANNOT declare narrower! It must declare IOException or broader (Exception)
        public SecureEncryptedConnection() throws Exception {
            super(); // Evaluates first; cannot be enclosed in try-catch inside constructor!
            System.out.println("  [SecureEncryptedConnection] TLS handshake completed.");
        }
    }

    public static void main(String[] args) {
        System.out.println("===============================================================");
        System.out.println("1. METHOD OVERRIDING: Narrower Checked Exception (LSP)");
        System.out.println("===============================================================");

        RemoteStorageEngine polymorphicStorage = new LocalStorageEngine();
        try {
            polymorphicStorage.transfer("nonexistent.dat", "/tmp/backup.dat");
        } catch (IOException e) {
            System.out.println("Caller caught via base contract: " + e.getClass().getName());
            System.out.println("Message: " + e.getMessage());
        }

        System.out.println("\n===============================================================");
        System.out.println("2. METHOD OVERRIDING: No Exception & Unchecked Freedom");
        System.out.println("===============================================================");

        RemoteStorageEngine memoryStorage = new MemoryStorageEngine();
        try {
            memoryStorage.transfer("cache.bin", "/tmp/cache.bin");
            memoryStorage.healthCheck();
        } catch (IOException e) {
            System.out.println("Will not be reached: " + e.getMessage());
        }

        System.out.println("\n===============================================================");
        System.out.println("3. CONSTRUCTOR EXCEPTION INVERSION RULE");
        System.out.println("===============================================================");

        try {
            SecureEncryptedConnection conn = new SecureEncryptedConnection();
        } catch (Exception e) {
            System.out.println("Constructor exception handled: " + e.getMessage());
        }

        System.out.println("\n[SUCCESS] Method Overriding & Liskov Substitution rules proved.");
    }
}
