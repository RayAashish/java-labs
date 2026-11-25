package patterns.creational;

/**
 * CREATIONAL PATTERN 3 — ABSTRACT FACTORY PATTERN
 *
 * WHAT IT IS:
 *   Provides an interface for creating FAMILIES of related/compatible objects without
 *   specifying their concrete classes.
 *
 * WHY IT MATTERS:
 *   In multi-cloud or multi-UI systems, mixing an AWS S3 Bucket with a GCP Cloud KMS Key
 *   breaks compatibility! Abstract Factory guarantees every created object belongs to the
 *   SAME coherent family.
 *
 * IN THIS FILE:
 *   1. BAD CODE:  Caller manually picks storage + encryptor independently -> risky mismatch!
 *   2. GOOD CODE: CloudProviderFactory guarantees matching ObjectStorage + KeyEncryptor pairs.
 */
public class AbstractFactoryPatternDemo {

    interface ObjectStorage { void upload(String key, String data); }
    interface KeyEncryptor  { String encrypt(String plainText); }

    // --- AWS Family ---
    static class S3Storage implements ObjectStorage {
        @Override public void upload(String key, String data) { System.out.println("[AWS S3] PutObject " + key + " -> " + data); }
    }
    static class AwsKmsEncryptor implements KeyEncryptor {
        @Override public String encrypt(String plainText) { return "AWS_KMS(" + plainText + ")"; }
    }

    // --- GCP Family ---
    static class GcsStorage implements ObjectStorage {
        @Override public void upload(String key, String data) { System.out.println("[GCP GCS] Upload " + key + " -> " + data); }
    }
    static class GcpKmsEncryptor implements KeyEncryptor {
        @Override public String encrypt(String plainText) { return "GCP_KMS(" + plainText + ")"; }
    }

    // =========================================================================
    // ✅ GOOD CODE: Abstract Factory binds compatible products into one family.
    // =========================================================================
    interface CloudInfraFactory {
        ObjectStorage createStorage();
        KeyEncryptor createEncryptor();
    }

    static class AwsInfraFactory implements CloudInfraFactory {
        @Override public ObjectStorage createStorage()  { return new S3Storage(); }
        @Override public KeyEncryptor createEncryptor() { return new AwsKmsEncryptor(); }
    }

    static class GcpInfraFactory implements CloudInfraFactory {
        @Override public ObjectStorage createStorage()  { return new GcsStorage(); }
        @Override public KeyEncryptor createEncryptor() { return new GcpKmsEncryptor(); }
    }

    static class BackupService {
        private final ObjectStorage storage;
        private final KeyEncryptor encryptor;

        BackupService(CloudInfraFactory factory) {
            // Guaranteed coherent family — impossible to mix AWS storage with GCP KMS!
            this.storage = factory.createStorage();
            this.encryptor = factory.createEncryptor();
        }

        void backup(String file, String payload) {
            storage.upload(file, encryptor.encrypt(payload));
        }
    }

    public static void main(String[] args) {
        System.out.println("=== BAD Risk: Mixing S3Storage + GcpKmsEncryptor manually breaks IAM/region contracts! ===");
        System.out.println("=== GOOD: Abstract Factory guarantees cohesive cloud product families ===");
        new BackupService(new AwsInfraFactory()).backup("ledger.csv", "secret-tx");
        new BackupService(new GcpInfraFactory()).backup("ledger.csv", "secret-tx");
    }
}
