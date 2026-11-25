package lts.java11.runtime;

import java.lang.invoke.MethodHandles;
import java.util.Arrays;
import jdk.jfr.Recording;

/**
 * Java 11 LTS — Nestmates (JEP 181), ProcessHandle (Java 9) & Programmatic JFR (JEP 328).
 */
public class RuntimeApisAndNestmatesDemo {

    private final String secretVaultKey = "KMS-MASTER-2048";

    class NestedReader {
        // Under Java 11 Nestmates (JEP 181), JVM bytecode allows direct NestHost private read
        // without generating compiler synthetic `access$000` bridge methods!
        String readOuterSecretViaLookup() throws Throwable {
            return (String) MethodHandles.lookup()
                    .findGetter(RuntimeApisAndNestmatesDemo.class, "secretVaultKey", String.class)
                    .invokeExact(RuntimeApisAndNestmatesDemo.this);
        }
    }

    public static void main(String[] args) throws Throwable {
        System.out.println("=== Java 11 Nestmates, ProcessHandle & JFR Runtime Demo ===");

        // -------------------------------------------------------------
        // 1. JEP 181 Nest-Based Access Control Reflection Introspection
        // -------------------------------------------------------------
        var outer = new RuntimeApisAndNestmatesDemo();
        var inner = outer.new NestedReader();
        System.out.println("[1] NestHost of NestedReader = " + NestedReader.class.getNestHost().getSimpleName());
        System.out.println("    NestMembers of Outer = " +
                Arrays.stream(RuntimeApisAndNestmatesDemo.class.getNestMembers())
                        .map(Class::getSimpleName).toList());
        System.out.println("    Direct MethodHandles private access result = " + inner.readOuterSecretViaLookup());

        // -------------------------------------------------------------
        // 2. ProcessHandle (Cross-Platform OS Process Introspection)
        // -------------------------------------------------------------
        ProcessHandle self = ProcessHandle.current();
        System.out.printf("%n[2] Current JVM PID=%d | User=%s | CPU=%s%n",
                self.pid(),
                self.info().user().orElse("unknown"),
                self.info().totalCpuDuration().orElse(java.time.Duration.ZERO));

        // -------------------------------------------------------------
        // 3. Programmatic JDK Flight Recorder (JEP 328) In-Process Control
        // -------------------------------------------------------------
        try (Recording recording = new Recording()) {
            recording.enable("jdk.ObjectAllocationInNewTLAB");
            recording.start();
            byte[] allocation = new byte[64 * 1024]; // Trigger minor allocation activity
            recording.stop();
            System.out.println("\n[3] JFR In-Process Recording state=" + recording.getState()
                    + " | sample allocation len=" + allocation.length);
        }
    }
}
