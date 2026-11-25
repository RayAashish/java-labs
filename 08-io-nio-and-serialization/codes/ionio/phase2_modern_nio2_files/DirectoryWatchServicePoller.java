package ionio.phase2_modern_nio2_files;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Author: raysi
 * Package: com.raysi.ionio.phase2_modern_nio2_files.codes
 *
 * Demonstrates:
 * 1. NIO.2 WatchService: Native OS filesystem event monitoring.
 * 2. Underlying OS primitives: Linux inotify(7), macOS kqueue/FSEvents, Windows ReadDirectoryChangesW.
 * 3. Handling ENTRY_CREATE, ENTRY_MODIFY, and ENTRY_DELETE event kinds.
 * 4. The critical WatchKey lifecycle: take() -> process events -> reset() -> handle cancellation.
 */
public class DirectoryWatchServicePoller {

    public static void main(String[] args) throws IOException, InterruptedException {
        System.out.println("==========================================================================");
        System.out.println(">>> DEMO: Modern NIO.2 WatchService Native Filesystem Poller <<<");
        System.out.println(">>> Author: raysi | Kernel inotify / kqueue Event Dispatch");
        System.out.println("==========================================================================\n");

        Path watchDir = Files.createTempDirectory("watch_service_demo_");
        System.out.println("[Step 1] Initialized directory to watch: " + watchDir);

        CountDownLatch eventsProcessedLatch = new CountDownLatch(3);
        WatchService watchService = FileSystems.getDefault().newWatchService();

        // Register the directory with the WatchService for Create, Modify, Delete events
        WatchKey registeredKey = watchDir.register(
                watchService,
                StandardWatchEventKinds.ENTRY_CREATE,
                StandardWatchEventKinds.ENTRY_MODIFY,
                StandardWatchEventKinds.ENTRY_DELETE
        );

        System.out.println("[Step 2] Registered directory with WatchService (Key: " + registeredKey + ")");

        // Start background watcher daemon thread
        Thread watcherThread = new Thread(() -> {
            try {
                System.out.println("   [WATCHER] Background event loop started. Waiting for OS events...");
                while (!Thread.currentThread().isInterrupted()) {
                    // Blocking wait for filesystem events (equivalent to epoll_wait on inotify fd)
                    WatchKey key = watchService.take();

                    for (WatchEvent<?> event : key.pollEvents()) {
                        WatchEvent.Kind<?> kind = event.kind();

                        if (kind == StandardWatchEventKinds.OVERFLOW) {
                            System.err.println("   [WATCHER] WARNING: Event overflow detected! Some OS events were dropped.");
                            continue;
                        }

                        // The context of the event is the relative path
                        @SuppressWarnings("unchecked")
                        WatchEvent<Path> ev = (WatchEvent<Path>) event;
                        Path filename = ev.context();

                        System.out.printf("   [EVENT DETECTED] Kind: %-14s | Target: %s%n", kind.name(), filename);
                        eventsProcessedLatch.countDown();
                    }

                    // CRITICAL: Must reset key so it can be re-queued for further events!
                    boolean valid = key.reset();
                    if (!valid) {
                        System.out.println("   [WATCHER] Directory key no longer valid (deleted/unmounted). Terminating loop.");
                        break;
                    }
                }
            } catch (InterruptedException e) {
                System.out.println("   [WATCHER] Watcher thread interrupted cleanly.");
            }
        }, "FileSystem-Watcher-Thread");

        watcherThread.setDaemon(true);
        watcherThread.start();

        // Step 3: Trigger filesystem modifications
        try {
            Thread.sleep(500); // Allow watcher thread to enter take()

            System.out.println("\n[Step 3] Simulating filesystem mutations from application thread...");

            // Mutation A: Create file
            Path testFile = watchDir.resolve("config.json");
            System.out.println("   -> Creating: " + testFile.getFileName());
            Files.writeString(testFile, "{\"version\": 1.0}");
            Thread.sleep(200);

            // Mutation B: Modify file
            System.out.println("   -> Modifying: " + testFile.getFileName());
            Files.writeString(testFile, "{\"version\": 2.0, \"status\": \"ACTIVE\"}");
            Thread.sleep(200);

            // Mutation C: Delete file
            System.out.println("   -> Deleting: " + testFile.getFileName());
            Files.delete(testFile);

            // Wait for events to be detected
            boolean allCaught = eventsProcessedLatch.await(6, TimeUnit.SECONDS);
            System.out.printf("%n[Step 4] Latch status: All events captured? %b%n", allCaught);

        } finally {
            // Teardown
            watcherThread.interrupt();
            watchService.close();
            Files.deleteIfExists(watchDir);
            System.out.println("\n[Step 5] WatchService closed and temporary directory cleaned up.");
        }

        System.out.println("==========================================================================");
        System.out.println(">>> ARCHITECTURAL TAKEAWAY <<<");
        System.out.println("Prior to Java 7, monitoring directories required polling with Thread.sleep()");
        System.out.println("and file.listFiles(), consuming 100% CPU on large dirs. WatchService uses");
        System.out.println("native OS kernel notification mechanisms (Linux inotify, macOS kqueue),");
        System.out.println("waking the Java thread ONLY when actual filesystem events occur!");
        System.out.println("==========================================================================");
    }
}
