package tooling.modules;

import java.lang.module.ModuleDescriptor;
import java.util.Comparator;
import java.util.ServiceLoader;

/**
 * JPMS (JAVA PLATFORM MODULE SYSTEM) & ServiceLoader INTROSPECTION
 *
 * WHAT IT IS:
 *   Inspects the live JDK ModuleLayer graph (`java.base`, `java.sql`, etc.), module exports,
 *   opens, and ServiceLoader SPI discovery (the mechanism JDBC 4.0+ uses to auto-load drivers!).
 */
public class JpmsAndServiceLoaderDemo {

    interface FormatterPlugin {
        String format(String text);
    }

    public static void main(String[] args) {
        System.out.println("=== 1. Boot Layer JDK Modules (Sample) ===");
        ModuleLayer.boot().modules().stream()
                .map(Module::getName)
                .filter(name -> name.startsWith("java."))
                .sorted()
                .limit(8)
                .forEach(name -> System.out.println("  Module: " + name));

        System.out.println("\n=== 2. Inspecting java.base Exports & Opens ===");
        ModuleDescriptor javaBase = Object.class.getModule().getDescriptor();
        if (javaBase != null) {
            System.out.println("java.base exported packages: " + javaBase.exports().size());
            System.out.println("java.base opened packages:   " + javaBase.opens().size());
        }

        System.out.println("\n=== 3. ServiceLoader JDBC Driver Auto-Discovery Check ===");
        // Demonstrates how DriverManager discovers java.sql.Driver implementations on the module/classpath
        long charsetProviders = ServiceLoader.load(java.nio.charset.spi.CharsetProvider.class)
                .stream()
                .sorted(Comparator.comparing(p -> p.type().getName()))
                .count();
        System.out.println("Registered CharsetProvider SPIs discovered: " + charsetProviders);
    }
}
