package patterns.structural;

/**
 * STRUCTURAL PATTERN 2 — DECORATOR PATTERN
 *
 * WHAT IT IS:
 *   Attaches new behaviors dynamically to an object by wrapping it inside decorator objects
 *   that implement the EXACT SAME interface.
 *
 * WHY IT MATTERS:
 *   Suppose you want optional Compression, Encryption, and Base64 encoding on a message payload.
 *   Using inheritance creates a combinatorial explosion (`EncryptedCompressedPayload`,
 *   `Base64EncryptedPayload`, etc.)! Decorators let you stack combinations dynamically at runtime.
 *   JDK Examples: new BufferedInputStream(new GZIPInputStream(new FileInputStream(...))).
 */
public class DecoratorPatternDemo {

    interface DataSink {
        void write(String payload);
    }

    static class ConsoleDataSink implements DataSink {
        @Override
        public void write(String payload) {
            System.out.println("[DISK/WIRE WRITE] " + payload);
        }
    }

    // ❌ BAD CODE: Inheritance explosion — every feature combination needs its own subclass!
    static class BadEncryptedCompressedSink extends ConsoleDataSink {
        @Override public void write(String payload) {
            super.write("ENC(ZIP(" + payload + "))"); // Rigid! Can't reorder or omit compression at runtime.
        }
    }

    // ✅ GOOD CODE: Base Decorator + stackable wrappers implementing DataSink!
    abstract static class DataSinkDecorator implements DataSink {
        protected final DataSink delegate;

        protected DataSinkDecorator(DataSink delegate) {
            this.delegate = delegate;
        }
    }

    static class CompressionDecorator extends DataSinkDecorator {
        CompressionDecorator(DataSink delegate) { super(delegate); }

        @Override
        public void write(String payload) {
            delegate.write("GZIP[" + payload + "]");
        }
    }

    static class EncryptionDecorator extends DataSinkDecorator {
        EncryptionDecorator(DataSink delegate) { super(delegate); }

        @Override
        public void write(String payload) {
            delegate.write("AES256{" + payload + "}");
        }
    }

    static class AuditTagDecorator extends DataSinkDecorator {
        AuditTagDecorator(DataSink delegate) { super(delegate); }

        @Override
        public void write(String payload) {
            delegate.write("[AUDIT] " + payload);
        }
    }

    public static void main(String[] args) {
        System.out.println("=== BAD (Rigid Subclass Explosion) ===");
        new BadEncryptedCompressedSink().write("user_token=xyz");

        System.out.println("\n=== GOOD (Composable Runtime Decorator Pipeline) ===");
        DataSink pipeline = new AuditTagDecorator(
                new EncryptionDecorator(
                        new CompressionDecorator(new ConsoleDataSink())));
        pipeline.write("user_token=xyz");
    }
}
