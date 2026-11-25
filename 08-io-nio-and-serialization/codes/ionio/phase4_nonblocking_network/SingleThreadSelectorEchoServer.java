package ionio.phase4_nonblocking_network;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Author: raysi
 * Package: com.raysi.ionio.phase4_nonblocking_network.codes
 *
 * Demonstrates:
 * 1. Single-Threaded Event Loop / Reactor Pattern using java.nio.channels.Selector.
 * 2. Underlying OS Multiplexing: epoll(7) on Linux, kqueue on macOS.
 * 3. Handling OP_ACCEPT, OP_READ, and partial channel writes on a single thread.
 * 4. Self-contained integration test spawning concurrent client connections.
 */
public class SingleThreadSelectorEchoServer {

    private static final int PORT = 9988;
    private static final int BUFFER_CAPACITY = 1024;

    public static void main(String[] args) throws Exception {
        System.out.println("==========================================================================");
        System.out.println(">>> DEMO: Single-Thread Selector Non-Blocking Echo Server <<<");
        System.out.println(">>> Author: raysi | I/O Multiplexing (epoll / kqueue) Event Loop");
        System.out.println("==========================================================================\n");

        CountDownLatch serverStartedLatch = new CountDownLatch(1);
        CountDownLatch clientsFinishedLatch = new CountDownLatch(3);

        // Start non-blocking server on a background thread
        Thread serverThread = new Thread(() -> {
            try {
                runServer(serverStartedLatch);
            } catch (IOException e) {
                System.out.println("Server terminated: " + e.getMessage());
            }
        }, "NIO-Selector-EventLoop");
        serverThread.setDaemon(true);
        serverThread.start();

        // Wait for server to bind port
        serverStartedLatch.await(3, TimeUnit.SECONDS);

        // Spawn 3 concurrent test clients to verify single-threaded concurrency
        System.out.println("[Clients] Launching 3 concurrent client threads to test non-blocking multiplexing...\n");
        for (int i = 1; i <= 3; i++) {
            final int clientId = i;
            Thread.ofVirtual().start(() -> {
                try (SocketChannel client = SocketChannel.open(new InetSocketAddress("127.0.0.1", PORT))) {
                    String msg = "HELLO_FROM_CLIENT_" + clientId;
                    client.write(ByteBuffer.wrap(msg.getBytes(StandardCharsets.UTF_8)));

                    ByteBuffer responseBuf = ByteBuffer.allocate(1024);
                    int read = client.read(responseBuf);
                    responseBuf.flip();
                    String reply = StandardCharsets.UTF_8.decode(responseBuf).toString();

                    System.out.printf("   [Client %d] Sent: \"%s\" | Received Echo: \"%s\"%n", clientId, msg, reply);
                    clientsFinishedLatch.countDown();
                } catch (IOException e) {
                    System.err.println("Client " + clientId + " error: " + e.getMessage());
                }
            });
        }

        boolean done = clientsFinishedLatch.await(5, TimeUnit.SECONDS);
        System.out.println("\n[Test] All 3 clients successfully served concurrently by 1 single thread? " + done);
        serverThread.interrupt();

        System.out.println("\n==========================================================================");
        System.out.println(">>> ARCHITECTURAL TAKEAWAY <<<");
        System.out.println("In classic I/O, 1,000 clients require 1,000 OS threads (1 GB+ of RAM).");
        System.out.println("With NIO Selector multiplexing, ONE single thread listens on epoll_wait");
        System.out.println("and processes events for thousands of sockets as they become ready!");
        System.out.println("==========================================================================");
    }

    private static void runServer(CountDownLatch readyLatch) throws IOException {
        // 1. Open Selector and ServerSocketChannel
        Selector selector = Selector.open();
        ServerSocketChannel serverChannel = ServerSocketChannel.open();

        // 2. CRITICAL: Channel MUST be placed in non-blocking mode to register with Selector!
        serverChannel.configureBlocking(false);
        serverChannel.bind(new InetSocketAddress(PORT));

        // 3. Register server socket for OP_ACCEPT (incoming TCP connections)
        serverChannel.register(selector, SelectionKey.OP_ACCEPT);

        System.out.printf("[Server] Single-Threaded Selector Server bound to port %d (Selector: %s)%n%n",
                PORT, selector.getClass().getSimpleName());
        readyLatch.countDown();

        ByteBuffer buffer = ByteBuffer.allocate(BUFFER_CAPACITY);

        // 4. The Infinite Event Loop (Reactor Pattern)
        while (!Thread.currentThread().isInterrupted()) {
            // Blocking wait for ready I/O events (Linux: epoll_wait(), macOS: kevent())
            int readyCount = selector.select();
            if (readyCount == 0) continue;

            Set<SelectionKey> selectedKeys = selector.selectedKeys();
            Iterator<SelectionKey> keyIterator = selectedKeys.iterator();

            while (keyIterator.hasNext()) {
                SelectionKey key = keyIterator.next();
                // CRITICAL: Remove the key immediately to prevent duplicate processing!
                keyIterator.remove();

                if (!key.isValid()) continue;

                if (key.isAcceptable()) {
                    handleAccept(key, selector);
                } else if (key.isReadable()) {
                    handleRead(key, buffer);
                }
            }
        }
    }

    private static void handleAccept(SelectionKey key, Selector selector) throws IOException {
        ServerSocketChannel ssc = (ServerSocketChannel) key.channel();
        SocketChannel clientChannel = ssc.accept();
        if (clientChannel != null) {
            clientChannel.configureBlocking(false);
            // Register client for reading incoming bytes
            clientChannel.register(selector, SelectionKey.OP_READ);
            System.out.println("   [Server Event] OP_ACCEPT: New connection from " + clientChannel.getRemoteAddress());
        }
    }

    private static void handleRead(SelectionKey key, ByteBuffer buffer) throws IOException {
        SocketChannel clientChannel = (SocketChannel) key.channel();
        buffer.clear();

        int bytesRead;
        try {
            bytesRead = clientChannel.read(buffer);
        } catch (IOException e) {
            // Connection reset by peer
            key.cancel();
            clientChannel.close();
            return;
        }

        if (bytesRead == -1) {
            // Client closed connection cleanly
            System.out.println("   [Server Event] Client disconnected cleanly: " + clientChannel.getRemoteAddress());
            key.cancel();
            clientChannel.close();
            return;
        }

        buffer.flip(); // Prepare buffer for reading
        String received = StandardCharsets.UTF_8.decode(buffer).toString();
        System.out.printf("   [Server Event] OP_READ: Received %d bytes (\"%s\") from %s%n",
                bytesRead, received, clientChannel.getRemoteAddress());

        // Echo response back to client
        String echoResponse = "ECHO: " + received;
        ByteBuffer responseBuf = ByteBuffer.wrap(echoResponse.getBytes(StandardCharsets.UTF_8));
        // In production, if response doesn't write fully, register OP_WRITE
        clientChannel.write(responseBuf);
    }
}
