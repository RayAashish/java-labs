package patterns.behavioral;

/**
 * BEHAVIORAL PATTERN 3 — TEMPLATE METHOD PATTERN
 *
 * WHAT IT IS:
 *   Defines the invariant skeleton of an algorithm in a `final` base method, delegating
 *   specific customizable steps to subclasses.
 *
 * WHY IT MATTERS:
 *   When CSV, JSON, and XML pipelines all repeat `open -> parse -> validate -> log -> close`,
 *   copy-pasting the orchestration duplicates error handling and metrics everywhere.
 *   Template Method locks the invariant sequence once while letting subclasses implement `parseRaw()`.
 *   JDK Examples: AbstractList, InputStream.read(byte[], int, int), HttpServlet.service().
 */
public class TemplateMethodPatternDemo {

    abstract static class DataImportPipeline {
        // `final` prevents subclasses from breaking the lifecycle sequence!
        public final void runImport(String rawInput) {
            openConnection();
            String parsed = parsePayload(rawInput);
            validate(parsed);
            saveMetrics();
        }

        private void openConnection() { System.out.println("1. [Base] Opened staging buffer"); }

        // Abstract hook implemented by each concrete format
        protected abstract String parsePayload(String rawInput);

        // Optional hook with sensible default
        protected void validate(String parsed) {
            System.out.println("3. [Base] Verified non-empty parsed payload: " + parsed);
        }

        private void saveMetrics() { System.out.println("4. [Base] Recorded import telemetry\n"); }
    }

    static class CsvImportPipeline extends DataImportPipeline {
        @Override
        protected String parsePayload(String rawInput) {
            String tokens = rawInput.replace(",", " | ");
            System.out.println("2. [CSV]  Split columns -> " + tokens);
            return tokens;
        }
    }

    static class JsonImportPipeline extends DataImportPipeline {
        @Override
        protected String parsePayload(String rawInput) {
            System.out.println("2. [JSON] Parsed object tree -> " + rawInput);
            return rawInput;
        }
    }

    public static void main(String[] args) {
        System.out.println("=== CSV Pipeline ===");
        new CsvImportPipeline().runImport("101,Aashish,Java");

        System.out.println("=== JSON Pipeline ===");
        new JsonImportPipeline().runImport("{\"id\":101,\"name\":\"Aashish\"}");
    }
}
