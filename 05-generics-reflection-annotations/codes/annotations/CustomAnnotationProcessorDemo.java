package annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;

/**
 * Demonstrates defining a custom @Retention(RUNTIME) annotation
 * and executing an annotation-driven runner using Reflection.
 */
public class CustomAnnotationProcessorDemo {

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    @interface BenchmarkTask {
        String label() default "unnamed";
        int repeat() default 1;
    }

    static class ReportService {
        @BenchmarkTask(label = "Generate Monthly Invoice", repeat = 2)
        public void generateInvoices() {
            System.out.println("   -> Running invoice batch...");
        }

        @BenchmarkTask(label = "Warm Cache")
        public void warmCache() {
            System.out.println("   -> Warming Redis keys...");
        }

        public void helperWithoutAnnotation() {
            System.out.println("   -> Should not be invoked automatically.");
        }
    }

    public static void main(String[] args) throws Exception {
        System.out.println("=== Custom Annotation Runtime Runner ===");
        ReportService service = new ReportService();

        for (Method method : ReportService.class.getDeclaredMethods()) {
            if (method.isAnnotationPresent(BenchmarkTask.class)) {
                BenchmarkTask task = method.getAnnotation(BenchmarkTask.class);
                System.out.println("[Runner] Executing '" + task.label() + "' x" + task.repeat());
                for (int i = 0; i < task.repeat(); i++) {
                    method.invoke(service);
                }
            }
        }
    }
}
