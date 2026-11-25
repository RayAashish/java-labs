package patterns.creational;

/**
 * CREATIONAL PATTERN 2 — FACTORY METHOD PATTERN
 *
 * WHAT IT IS:
 *   Defines an interface/method for creating an object, letting subclasses or factory methods
 *   decide WHICH concrete class to instantiate without exposing construction details to callers.
 *
 * WHY IT MATTERS:
 *   When callers write `new PdfReportExporter()` or `new CsvReportExporter()` everywhere with
 *   complex constructor setup, changing concrete classes or adding formats ripples across the app.
 *
 * IN THIS FILE:
 *   1. BAD CODE:  Callers manually construct concrete exporters with duplicated config logic.
 *   2. GOOD CODE: ExporterFactory centralizes creation behind a clean ReportExporter contract.
 */
public class FactoryMethodPatternDemo {

    interface ReportExporter {
        void export(String title, String body);
    }

    static class PdfExporter implements ReportExporter {
        private final boolean embedFonts;
        PdfExporter(boolean embedFonts) { this.embedFonts = embedFonts; }

        @Override
        public void export(String title, String body) {
            System.out.println("[PDF embedFonts=" + embedFonts + "] " + title + " -> " + body);
        }
    }

    static class CsvExporter implements ReportExporter {
        private final char delimiter;
        CsvExporter(char delimiter) { this.delimiter = delimiter; }

        @Override
        public void export(String title, String body) {
            System.out.println("[CSV delim='" + delimiter + "'] " + title + delimiter + body);
        }
    }

    // =========================================================================
    // ❌ BAD CODE: Callers tightly coupled to concrete classes & constructor args.
    // =========================================================================
    static void badExportReport(String format, String title, String body) {
        if ("PDF".equals(format)) {
            new PdfExporter(true).export(title, body); // Construction details leak to caller!
        } else if ("CSV".equals(format)) {
            new CsvExporter(',').export(title, body);
        }
    }

    // =========================================================================
    // ✅ GOOD CODE: Factory Method encapsulates concrete instantiation & defaults.
    // =========================================================================
    enum ExportFormat { PDF, CSV, MARKDOWN }

    static class ReportExporterFactory {
        public static ReportExporter create(ExportFormat format) {
            return switch (format) {
                case PDF -> new PdfExporter(true);
                case CSV -> new CsvExporter(',');
                case MARKDOWN -> (t, b) -> System.out.println("[MD] # " + t + " | " + b);
            };
        }
    }

    public static void main(String[] args) {
        System.out.println("=== BAD (Scattered Concrete Construction) ===");
        badExportReport("PDF", "Q4 Revenue", "$1.2M");

        System.out.println("\n=== GOOD (Factory Method Decoupling) ===");
        ReportExporter exporter = ReportExporterFactory.create(ExportFormat.MARKDOWN);
        exporter.export("Q4 Revenue", "$1.2M");
    }
}
