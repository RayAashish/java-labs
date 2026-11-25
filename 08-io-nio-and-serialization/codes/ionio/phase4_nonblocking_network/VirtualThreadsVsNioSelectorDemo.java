package ionio.phase4_nonblocking_network;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Author: raysi
 * Package: com.raysi.ionio.phase4_nonblocking_network.codes
 *
 * Demonstrates the 3 Eras of High-Concurrency Java I/O Architecture:
 * 1. Era 1 (Java 1.0): Platform Thread-Per-Connection (1:1 OS Thread) - Memory bottleneck.
 * 2. Era 2 (Java 1.4): NIO Selectors & Event Loops (1:N Multiplexing) - Complex asynchronous callbacks.
 * 3. Era 3 (Java 21): Virtual Threads + Simple Blocking I/O (M:N Loom Continuations) - Best of both worlds!
 */
public class VirtualThreadsVsNioSelectorDemo {

    private static final int CONCURRENT_CLIENTS = 1000;
    private static final int PORT = 9977;

    public static void main(String[] args) throws Exception {
        System.out.println("==========================================================================");
        System.out.println(">>> DEMO: Classic IO vs NIO Multiplexing vs Java 21 Virtual Threads <<<");
        System.out.println(">>> Author: raysi | Project Loom Virtual Thread Continuation Deep Dive");
        System.out.println("==========================================================================\n");

        CountDownLatch serverReady = new CountDownLatch(1);
        AtomicInteger handledRequests = new AtomicInteger(0);

        // Start Virtual Thread Server (Java 21)
        Thread server = Thread.ofPlatform().daemon().start(() -> {
            try (ServerSocket serverSocket = new ServerSocket(PORT, 1024)) {
                serverReady.countDown();
                // Java 21: One Virtual Thread per incoming connection!
                try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
                    while (!Thread.currentThread().isInterrupted()) {
                        Socket socket = serverSocket.accept();
                        executor.submit(() -> handleClientBlocking(socket, handledRequests));
                    }
                }
            } catch (IOException e) {
                // Server closed
            }
        });

        serverReady.await(3, TimeUnit.SECONDS);
        System.out.println("[Server] Java 21 Virtual Thread Server listening on port " + PORT);
        System.out.println("[Experiment] Launching " + CONCURRENT_CLIENTS + " concurrent Virtual Thread clients...");

        long startTime = System.nanoTime();
        CountDownLatch clientsLatch = new CountDownLatch(CONCURRENT_CLIENTS);

        // Launch 1,000 Virtual Thread clients connecting concurrently
        try (ExecutorService clientExecutor = Executors.newVirtualThreadPerTaskExecutor()) {
            for (int i = 1; i <= CONCURRENT_CLIENTS; i++) {
                final int id = i;
                clientExecutor.submit(() -> {
                    try (Socket clientSocket = new Socket("127.0.0.1", PORT)) {
                        OutputStream out = clientSocket.getOutputStream();
                        InputStream in = clientSocket.getInputStream();

                        String payload = "PING_" + id;
                        out.write(payload.getBytes(StandardCharsets.UTF_8));
                        out.flush();

                        byte[] buffer = new byte[64];
                        int read = in.read(buffer);
                        // Process response
                    } catch (IOException ignored) {
                    } finally {
                        clientsLatch.countDown();
                    }
                });
            }
        }

        boolean allCompleted = clientsLatch.await(10, TimeUnit.SECONDS);
        long elapsedMs = (System.nanoTime() - startTime) / 1_000_000;

        System.out.printf("[Result] Completed %d concurrent client connections in %d ms (Success: %b)%n%n",
                handledRequests.get(), elapsedMs, allCompleted);

        // Architectural Comparative Analysis
        System.out.println("==========================================================================");
        System.out.println(">>> THE EVOLUTIONARY MATRIX: 3 ERAS OF JAVA CONCURRENCY <<<");
        System.out.println("==========================================================================");
        System.out.println("| Era | Architecture | OS Threads | Memory per Conn | Code Style |");
        System.out.println("| :--- | :--- | :--- | :--- | :--- |");
        System.out.println("| 1. Classic IO | Platform Thread/Conn | 1:1 with OS | ~1 MB (Stack) | Simple Synchronous |");
        System.out.println("| 2. NIO / Netty| Multiplexed EventLoop| 1 OS Worker | ~2 KB (State) | Complex Async/Reactive |");
        System.out.println("| 3. Java 21 VT | Virtual Threads (M:N)| Few Carriers| ~1 KB (Heap)  | Simple Synchronous! |");
        System.out.println("--------------------------------------------------------------------------");
        System.out.println("How Java 21 Virtual Threads Work Under the Hood:");
        System.out.println("When a Virtual Thread calls in.read() or out.write(), the JVM runtime");
        System.out.println("unmounts the virtual thread continuation from its carrier OS thread.");
        System.out.println("The carrier thread immediately processes other virtual threads!");
        System.out.println("When the OS socket poller (epoll/kqueue) signals data ready, the continuation");
        System.out.println("is re-mounted onto an available carrier thread. Developers write simple blocking");
        System.out.println("code while achieving Netty-scale non-blocking throughput!");
        System.out.println("==========================================================================");
    }

    private static void handleClientBlocking(Socket socket, AtomicInteger counter) {
        try (socket;
             InputStream in = socket.getInputStream();
             OutputStream out = socket.getOutputStream()) {

            byte[] buffer = new byte[64];
            int bytesRead = in.read(buffer); // Blocks! In Java 21, unmounts Virtual Thread continuation!
            if (bytesRead > 0) {
                out.write("PONG".getBytes(StandardCharsets.UTF_8));
                out.flush();
                counter.incrementAndGet();
            }
        } catch (IOException ignored) {}
    }
}
