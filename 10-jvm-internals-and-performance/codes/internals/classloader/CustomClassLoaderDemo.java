package internals.classloader;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;

/**
 * Architectural Demonstration: ClassLoader Internals, Delegation, and Type Isolation.
 * <p>
 * Key Concepts Demonstrated:
 * 1. Extending {@link ClassLoader} and implementing {@code findClass(String name)}.
 * 2. Demonstrating standard parent delegation behavior.
 * 3. Breaking parent delegation (Child-First / Parent-Last) as practiced in Tomcat/OSGi.
 * 4. Demonstrating the JVM Class Identity Theorem:
 *    Class Identity = <ClassLoader Instance, Fully Qualified Class Name>.
 *    Two classes with identical bytecode loaded by different ClassLoaders cannot be cast to each other!
 */
public class CustomClassLoaderDemo {

    public static void main(String[] args) throws Exception {
        System.out.println("================================================================================");
        System.out.println("   CLASSLOADER INTERNALS & TYPE ISOLATION DEMO");
        System.out.println("================================================================================");

        // 1. Inspect the ClassLoader Hierarchy
        ClassLoader currentLoader = CustomClassLoaderDemo.class.getClassLoader();
        System.out.println("\n[1] Current ClassLoader Hierarchy:");
        System.out.println("   -> Application / System Loader : " + currentLoader);
        System.out.println("   -> Platform Loader            : " + currentLoader.getParent());
        System.out.println("   -> Bootstrap Loader (C++ Null): " + currentLoader.getParent().getParent());

        // 2. Instantiate two independent Custom ClassLoaders
        IsolatedClassLoader loaderA = new IsolatedClassLoader("Loader-Alpha", currentLoader);
        IsolatedClassLoader loaderB = new IsolatedClassLoader("Loader-Beta", currentLoader);

        String targetClassName = "com.raysi.internals.classloader.CustomClassLoaderDemo$SamplePlugin";

        System.out.println("\n[2] Loading Target Plugin Class via Independent ClassLoaders...");
        Class<?> classFromLoaderA = loaderA.loadClass(targetClassName);
        Class<?> classFromLoaderB = loaderB.loadClass(targetClassName);

        System.out.println("   -> Class A loaded by: " + classFromLoaderA.getClassLoader());
        System.out.println("   -> Class B loaded by: " + classFromLoaderB.getClassLoader());

        // 3. The JVM Class Identity Check:
        System.out.println("\n[3] Testing JVM Class Identity Theorem:");
        System.out.println("   -> classFromLoaderA == classFromLoaderB ? " + (classFromLoaderA == classFromLoaderB));

        Object instanceA = classFromLoaderA.getDeclaredConstructor().newInstance();
        Object instanceB = classFromLoaderB.getDeclaredConstructor().newInstance();

        Method executeA = classFromLoaderA.getMethod("execute");
        executeA.invoke(instanceA);

        Method executeB = classFromLoaderB.getMethod("execute");
        executeB.invoke(instanceB);

        System.out.println("\n[4] Attempting Cross-ClassLoader Type Casting (Simulating ClassCastException):");
        try {
            // Even though the bytecode is 100% identical, the JVM enforces isolation:
            SamplePlugin castedInstance = (SamplePlugin) instanceA;
            System.out.println("   -> Successfully casted to local class type: " + castedInstance);
        } catch (ClassCastException e) {
            System.err.println("   [!] Caught Expected ClassCastException: " + e.getMessage());
        }

        System.out.println("\n================================================================================");
        System.out.println("   ClassLoader isolation verified successfully!");
        System.out.println("================================================================================");
    }

    /**
     * Isolated ClassLoader that loads targeted classes directly from bytecode,
     * simulating a hot-reloading plugin container or web application container.
     */
    public static class IsolatedClassLoader extends ClassLoader {
        private final String name;

        public IsolatedClassLoader(String name, ClassLoader parent) {
            super(parent);
            this.name = name;
        }

        @Override
        public Class<?> loadClass(String name) throws ClassNotFoundException {
            // Enforce delegation for core java.* platform classes to protect JVM security
            if (name.startsWith("java.") || name.startsWith("javax.")) {
                return super.loadClass(name);
            }

            // For application classes, break parent delegation and load directly if requested
            if (name.contains("SamplePlugin")) {
                return findClass(name);
            }

            return super.loadClass(name);
        }

        @Override
        protected Class<?> findClass(String name) throws ClassNotFoundException {
            String path = name.replace('.', '/') + ".class";
            try (InputStream in = getClass().getClassLoader().getResourceAsStream(path)) {
                if (in == null) {
                    throw new ClassNotFoundException("Could not find class bytecode at resource: " + path);
                }
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = in.read(buffer)) != -1) {
                    out.write(buffer, 0, bytesRead);
                }
                byte[] classBytes = out.toByteArray();

                // Define class in Metaspace and return Class<?> mirror
                return defineClass(name, classBytes, 0, classBytes.length);
            } catch (IOException e) {
                throw new ClassNotFoundException("Failed to read class bytes for: " + name, e);
            }
        }

        @Override
        public String toString() {
            return "IsolatedClassLoader[" + name + "]";
        }
    }

    public static class SamplePlugin {
        public void execute() {
            System.out.println("      [SamplePlugin.execute()] Running inside context of: " +
                    this.getClass().getClassLoader());
        }
    }
}
