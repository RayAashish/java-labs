package oops.c06_abstraction;

/**
 * Demonstrates Abstract Classes vs Modern Java 8/9+ Interfaces:
 * 1. Abstract class sharing state + constructor + template flow.
 * 2. Interfaces with abstract, default (Java 8), static (Java 8), and private (Java 9) methods.
 * 3. Explicit default-method diamond conflict resolution via InterfaceName.super.method().
 */
public class AbstractionAndInterfacesDemo {

    // Abstract Class: holds shared mutable state, constructor, and template workflow
    abstract static class DataExporter {
        private final String sourceName;

        protected DataExporter(String sourceName) {
            this.sourceName = sourceName;
        }

        // Template Method: fixed skeleton delegating formatting steps to subclasses
        public final void export() {
            System.out.println("[Export Start] Source=" + sourceName);
            System.out.println(formatHeader());
            System.out.println(formatBody());
        }

        protected abstract String formatHeader();
        protected abstract String formatBody();
    }

    static class CsvExporter extends DataExporter {
        CsvExporter(String sourceName) { super(sourceName); }
        @Override protected String formatHeader() { return "id,name,role"; }
        @Override protected String formatBody() { return "101,Aashish,JavaArchitect"; }
    }

    // Modern Interface: abstract + default + static + private helpers
    interface Auditable {
        void execute();

        default void ping() {
            logHelper("Auditable default ping()");
        }

        static String version() {
            return "Audit-v2.0";
        }

        private void logHelper(String msg) {
            System.out.println("   [Interface Private Helper] " + msg);
        }
    }

    interface Alertable {
        default void ping() {
            System.out.println("   [Alertable] default ping()");
        }
    }

    // Diamond conflict resolution: both Auditable and Alertable define default void ping()
    static class SecurityGateway implements Auditable, Alertable {
        @Override
        public void execute() {
            System.out.println("SecurityGateway scanning traffic (" + Auditable.version() + ")");
        }

        @Override
        public void ping() {
            Auditable.super.ping(); // Explicitly choose Auditable's default
            Alertable.super.ping(); // Or combine both!
        }
    }

    public static void main(String[] args) {
        System.out.println("=== 1. Abstract Class Template Export ===");
        DataExporter exporter = new CsvExporter("UsersTable");
        exporter.export();

        System.out.println("\n=== 2. Modern Interface & Diamond Resolution ===");
        SecurityGateway gateway = new SecurityGateway();
        gateway.execute();
        gateway.ping();
    }
}
