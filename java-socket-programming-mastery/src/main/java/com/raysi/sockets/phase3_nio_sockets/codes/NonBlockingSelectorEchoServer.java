package com.raysi.sockets.phase3_nio_sockets.codes;

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

/**
 * Author: raysi
 * Package: com.raysi.sockets.phase3_nio_sockets.codes
 * 
 * Phase 3: High-Performance Non-Blocking Java NIO Selector Echo Server.
 * Demonstrates multiplexed I/O event loops (Reactor pattern) using a single thread,
 * ServerSocketChannel, SocketChannel, Selector, SelectionKey, and ByteBuffers.
 */
public class NonBlockingSelectorEchoServer {

    private static final int PORT = 8083;
    private static final int BUFFER_CAPACITY = 1024;

    public static void main(String[] args) {
        System.out.println("=== Starting Non-Blocking NIO Selector Server on Port " + PORT + " (Author: raysi) ===");

        try (
            // 1. Open the Selector and ServerSocketChannel
            Selector selector = Selector.open();
            ServerSocketChannel serverChannel = ServerSocketChannel.open()
        ) {
            // 2. MUST set to non-blocking mode before registering with Selector
            serverChannel.configureBlocking(false);

            // 3. Bind to local port
            serverChannel.bind(new InetSocketAddress(PORT));

            // 4. Register channel with Selector for incoming connection events (OP_ACCEPT)
            serverChannel.register(selector, SelectionKey.OP_ACCEPT);
            System.out.println("[INFO] NIO Server ready. Waiting for events on a single thread...");

            // 5. The Event Loop (Reactor core)
            while (true) {
                // Blocks until at least one registered channel has an I/O event ready (or wakeup/timeout)
                int readyChannels = selector.select();
                if (readyChannels == 0) continue;

                // 6. Inspect the set of ready SelectionKeys
                Set<SelectionKey> selectedKeys = selector.selectedKeys();
                Iterator<SelectionKey> keyIterator = selectedKeys.iterator();

                while (keyIterator.hasNext()) {
                    SelectionKey key = keyIterator.next();
                    // Crucial: Remove the key immediately to prevent duplicate event processing
                    keyIterator.remove();

                    if (!key.isValid()) continue;

                    if (key.isAcceptable()) {
                        // A new incoming connection is ready to be accepted
                        handleAccept(key, selector);
                    } else if (key.isReadable()) {
                        // Existing client has bytes ready to read in its socket buffer
                        handleRead(key);
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("[ERROR] NIO Server failure: " + e.getMessage());
        }
    }

    /**
     * Accepts new incoming connection, configures non-blocking, and registers for OP_READ.
     */
    private static void handleAccept(SelectionKey key, Selector selector) throws IOException {
        ServerSocketChannel serverChannel = (ServerSocketChannel) key.channel();
        SocketChannel clientChannel = serverChannel.accept();

        if (clientChannel != null) {
            clientChannel.configureBlocking(false);
            // Attach a dedicated ByteBuffer to the selection key for per-client state
            ByteBuffer buffer = ByteBuffer.allocate(BUFFER_CAPACITY);
            clientChannel.register(selector, SelectionKey.OP_READ, buffer);

            System.out.println("[ACCEPT] Accepted non-blocking connection from " + clientChannel.getRemoteAddress());

            // Welcome banner
            String welcome = "NIO_SERVER: Connected to single-threaded Reactor. Send data to echo.\n";
            clientChannel.write(ByteBuffer.wrap(welcome.getBytes(StandardCharsets.UTF_8)));
        }
    }

    /**
     * Reads available bytes into buffer and echoes back to the client.
     */
    private static void handleRead(SelectionKey key) {
        SocketChannel clientChannel = (SocketChannel) key.channel();
        ByteBuffer buffer = (ByteBuffer) key.attachment();

        try {
            buffer.clear(); // Reset position to 0 and limit to capacity
            int bytesRead = clientChannel.read(buffer);

            if (bytesRead == -1) {
                // EOF reached: Client closed connection cleanly
                System.out.println("[DISCONNECT] Client closed connection: " + clientChannel.getRemoteAddress());
                clientChannel.close();
                key.cancel();
                return;
            }

            // Flip buffer: prepare for reading (limit = position, position = 0)
            buffer.flip();
            String received = StandardCharsets.UTF_8.decode(buffer).toString();
            System.out.print("[RECEIVED from " + clientChannel.getRemoteAddress() + "]: " + received);

            // Echo back to client
            String echoMessage = "ECHO: " + received;
            ByteBuffer echoBuffer = ByteBuffer.wrap(echoMessage.getBytes(StandardCharsets.UTF_8));
            clientChannel.write(echoBuffer);

        } catch (IOException e) {
            System.err.println("[WARN] Connection reset by peer: " + clientChannel);
            try {
                clientChannel.close();
            } catch (IOException ignored) {}
            key.cancel();
        }
    }
}
