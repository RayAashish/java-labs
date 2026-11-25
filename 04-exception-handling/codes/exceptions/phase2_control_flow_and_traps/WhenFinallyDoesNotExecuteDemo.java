package exceptions.phase2_control_flow_and_traps;

/**
 * Author: raysi
 * Package: com.raysi.exceptions.phase2_control_flow_and_traps.codes
 *
 * Demonstrates the rare but critical interview scenarios where a 'finally' block
 * does NOT execute:
 * 1. Daemon thread termination when JVM exits.
 * 2. System.exit(0) / Runtime.getRuntime().halt(0) invocation.
 * 3. Infinite loop or deadlock in try/catch.
 */
public class WhenFinallyDoesNotExecuteDemo {

    public static void main(String[] args) throws InterruptedException {
        System.out.println("===============================================================");
        System.out.println("DEMONSTRATION: When Does Finally NOT Execute?");
        System.out.println("===============================================================");

        // SCENARIO 1: Daemon Thread Abrupt Termination
        System.out.println("\n--- SCENARIO 1: Daemon Thread Terminated When Main Thread Exits ---");
        Thread daemonThread = new Thread(() -> {
            try {
                System.out.println("  [Daemon Thread] Running inside try block...");
                Thread.sleep(2000); // Sleep longer than main
                System.out.println("  [Daemon Thread] This will not execute if main thread ends!");
            } catch (InterruptedException e) {
                System.out.println("  [Daemon Thread] Interrupted.");
            } finally {
                System.out.println("  [Daemon Thread] Finally block in daemon thread! (Will be skipped on abrupt VM exit)");
            }
        });
        daemonThread.setDaemon(true);
        daemonThread.start();

        Thread.sleep(200); // Let daemon start
        System.out.println("  [Main Thread] Daemon started. Main thread continuing...");

        // SCENARIO 2: System.exit() behavior explanation
        System.out.println("\n--- SCENARIO 2: System.exit() or Runtime.halt() Behavior ---");
        System.out.println("  Fact: If System.exit(0) is called inside a try block:");
        System.out.println("        1. The JVM halts immediately.");
        System.out.println("        2. Registered ShutdownHooks run.");
        System.out.println("        3. Pending 'finally' blocks on active threads are NEVER executed.");
        System.out.println("  Fact: If Runtime.getRuntime().halt(1) is called, even ShutdownHooks are bypassed!");

        // SCENARIO 3: Controlled demonstration with ShutdownHook proof
        System.out.println("\n--- SCENARIO 3: Normal Completion vs VM Termination Guarantee ---");
        try {
            System.out.println("  [Try] Normal execution block.");
        } finally {
            System.out.println("  [Finally] Executed reliably under normal thread execution lifecycle.");
        }

        System.out.println("\n[SUMMARY] Finally is NOT guaranteed if: System.exit(), JVM crash, SIGKILL, daemon abort, or infinite loop/deadlock.");
    }
}
