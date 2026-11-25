package tooling.diagnostics;

import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.lang.management.ThreadMXBean;
import java.util.Arrays;

/**
 * Inspects live JVM process metadata, PID (useful for jcmd/jstack/jfr),
 * active GC beans, and deadlock detection via ManagementFactory.
 */
public class RuntimeDiagnosticsDemo {

    public static void main(String[] args) {
        RuntimeMXBean runtimeMx = ManagementFactory.getRuntimeMXBean();
        ThreadMXBean threadMx = ManagementFactory.getThreadMXBean();

        System.out.println("=== Live JVM Runtime Diagnostics ===");
        System.out.println("Process PID:     " + ProcessHandle.current().pid());
        System.out.println("JVM Name:        " + runtimeMx.getVmName() + " " + runtimeMx.getVmVersion());
        System.out.println("Input VM Args:   " + runtimeMx.getInputArguments());
        System.out.println("Live Threads:    " + threadMx.getThreadCount());
        System.out.println("Deadlocked PIDs: " + Arrays.toString(threadMx.findDeadlockedThreads()));

        ManagementFactory.getGarbageCollectorMXBeans().forEach(gc ->
                System.out.printf("GC Bean: %-25s | collections=%d | time=%dms%n",
                        gc.getName(), gc.getCollectionCount(), gc.getCollectionTime()));
    }
}
