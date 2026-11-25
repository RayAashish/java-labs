package concurrency.phase1_threads_and_memory_model;

/**
 * Author: raysi
 * Demonstrates:
 * 1. User Threads vs Daemon Threads (JVM shutdown semantics).
 * 2. Cooperative Interruption: Why Thread.stop() was abolished, how interrupt() sets a flag,
 *    and why catching InterruptedException CLEARS the flag (requiring self-interruption).
 */
public class DaemonThreadAndGracefulInterruptDemo {

    public static void main(String[] args) throws InterruptedException {
        System.out.println("======================================================================");
        System.out.println("  PHASE 1 LAB: DAEMON THREADS & COOPERATIVE INTERRUPTION");
        System.out.println("  Author: raysi | Package: com.raysi.concurrency");
        System.out.println("======================================================================\n");

        System.out.println("=== PART 1: Cooperative Interruption in CPU-Intensive Tasks ===");
        Thread computationWorker = new Thread(() -> {
            long count = 0;
            // Best Practice: Continually inspect the interrupted status in tight loops!
            while (!Thread.currentThread().isInterrupted()) {
                count++;
                if (count % 100_000_000 == 0) {
                    System.out.println("[ComputationWorker] Processed " + count + " units...");
                }
            }
            System.out.println("[ComputationWorker] Clean shutdown acknowledged! Final count: " + count);
        }, "Computation-Worker");

        computationWorker.start();
        Thread.sleep(150); // Let it do some work
        System.out.println("[Main] Sending interrupt signal to ComputationWorker...");
        computationWorker.interrupt(); // Sets the interrupt status bit to true
        computationWorker.join();

        System.out.println("\n=== PART 2: The InterruptedException Trap & Restoring Flags ===");
        Thread blockingWorker = new Thread(() -> {
            try {
                System.out.println("[BlockingWorker] Entering Thread.sleep(5000)...");
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                // TRAP: When InterruptedException is caught, JVM CLEARS the interrupt status!
                System.out.println("[BlockingWorker] InterruptedException caught! Flag cleared: "
                        + Thread.currentThread().isInterrupted());

                // SENIOR BEST PRACTICE: Always restore the interrupt flag if you do not rethrow!
                Thread.currentThread().interrupt();
                System.out.println("[BlockingWorker] Restored interrupt flag: "
                        + Thread.currentThread().isInterrupted());
            }

            // Upstream callers can now inspect if interruption occurred:
            if (Thread.currentThread().isInterrupted()) {
                System.out.println("[BlockingWorker] Performing safe cleanup and exiting gracefully.");
            }
        }, "Blocking-Worker");

        blockingWorker.start();
        Thread.sleep(100);
        System.out.println("[Main] Interrupting BlockingWorker...");
        blockingWorker.interrupt();
        blockingWorker.join();

        System.out.println("\n=== PART 3: Daemon Thread Lifecycle Trap ===");
        Thread daemonWorker = new Thread(() -> {
            try {
                System.out.println("[DaemonWorker] Background housekeeper started. Is daemon? "
                        + Thread.currentThread().isDaemon());
                Thread.sleep(10_000);
            } catch (InterruptedException e) {
                System.out.println("[DaemonWorker] Interrupted.");
            } finally {
                // WARNING: Finally blocks in daemon threads may NEVER execute if all user threads terminate!
                System.out.println("[DaemonWorker] Finally block executed (Never rely on this for resource cleanup!).");
            }
        }, "Housekeeper-Daemon");

        daemonWorker.setDaemon(true); // Must be set BEFORE start() or IllegalThreadStateException is thrown!
        daemonWorker.start();
        Thread.sleep(100);

        System.out.println("[Main] Main thread (last User thread) terminating now. JVM will exit immediately,");
        System.out.println("       killing DaemonWorker without waiting for its finally block!");
    }
}
