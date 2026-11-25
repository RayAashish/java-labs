package exceptions.phase3_autocloseable_and_suppressed;

/**
 * Author: raysi
 * Package: com.raysi.exceptions.phase3_autocloseable_and_suppressed.codes
 *
 * Demonstrates:
 * 1. Creating custom resources implementing java.lang.AutoCloseable.
 * 2. Automatic invocation of close() at scope termination.
 * 3. Strict LIFO (reverse order) closing of multiple resources.
 * 4. Java 9 effectively final resource references in try-with-resources.
 */
public class CustomAutoCloseableResourceDemo {

    static class PipelineResource implements AutoCloseable {
        private final String stageName;

        public PipelineResource(String stageName) {
            this.stageName = stageName;
            System.out.println("  [ACQUIRED] " + stageName);
        }

        public void process() {
            System.out.println("  [PROCESSING] Work executed in " + stageName);
        }

        @Override
        public void close() {
            System.out.println("  [RELEASED] " + stageName + " successfully closed.");
        }
    }

    public static void main(String[] args) {
        System.out.println("===============================================================");
        System.out.println("1. LIFO CLOSING ORDER IN TRY-WITH-RESOURCES");
        System.out.println("===============================================================");

        try (PipelineResource resA = new PipelineResource("Stage-1-DatabaseConnection");
             PipelineResource resB = new PipelineResource("Stage-2-PreparedStatement");
             PipelineResource resC = new PipelineResource("Stage-3-ResultSetCursor")) {

            System.out.println("\n--- Executing Business Pipeline ---");
            resA.process();
            resB.process();
            resC.process();
            System.out.println("--- Business Pipeline Finished. Preparing AutoClose ---\n");

        } // All three close() calls are automatically invoked in REVERSE order: resC -> resB -> resA

        System.out.println("\n===============================================================");
        System.out.println("2. JAVA 9+ EFFECTIVELY FINAL RESOURCE REFERENCE SYNTAX");
        System.out.println("===============================================================");

        // Pre-declared outside try block
        PipelineResource externalResource = new PipelineResource("Stage-External-Resource");

        try (externalResource) { // Valid in Java 9+ (variable must be effectively final)
            externalResource.process();
        }

        System.out.println("\n[SUCCESS] CustomAutoCloseableResourceDemo completed.");
    }
}
