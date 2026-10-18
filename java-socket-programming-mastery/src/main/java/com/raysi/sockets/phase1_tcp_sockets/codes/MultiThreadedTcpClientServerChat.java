package com.raysi.sockets.phase1_tcp_sockets.codes;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Author: raysi
 * Package: com.raysi.sockets.phase1_tcp_sockets.codes
 * 
 * Phase 1: Multi-Threaded TCP Chat Server & Client.
 * Demonstrates thread-per-client handling via an ExecutorService thread pool,
 * thread-safe client registry, broadcast messaging, and clean resource teardown.
 */
public class MultiThreadedTcpClientServerChat {

    private static final int PORT = 8082;
    // Thread-safe map tracking connected clients: SessionId -> PrintWriter
    private static final Map<String, PrintWriter> activeClients = new ConcurrentHashMap<>();
    private static final ExecutorService workerPool = Executors.newCachedThreadPool();

    public static void main(String[] args) throws IOException {
        System.out.println("=== Multi-Threaded TCP Broadcast Server Running on Port " + PORT + " (Author: raysi) ===");

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            while (true) {
                // Main accept loop: blocks until a new client establishes TCP connection
                Socket socket = serverSocket.accept();
                workerPool.submit(new ClientHandler(socket));
            }
        } finally {
            workerPool.shutdown();
        }
    }

    /**
     * Dedicated task assigned to each connected client socket.
     */
    private static class ClientHandler implements Runnable {
        private final Socket socket;
        private String clientName;
        private PrintWriter writer;

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            String clientAddress = socket.getRemoteSocketAddress().toString();
            try (
                socket;
                BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true)
            ) {
                this.writer = out;
                out.println("ENTER_NAME: Please enter your chat handle:");
                this.clientName = reader.readLine();

                if (clientName == null || clientName.isBlank()) {
                    clientName = "User_" + socket.getPort();
                }

                // Register client into the shared registry
                activeClients.put(clientName, writer);
                System.out.println("[NEW CLIENT] " + clientName + " connected from " + clientAddress);
                broadcast("[SYSTEM]: " + clientName + " joined the chat room. (Total online: " + activeClients.size() + ")");

                String message;
                while ((message = reader.readLine()) != null) {
                    if ("/exit".equalsIgnoreCase(message.trim())) {
                        out.println("[SYSTEM]: Disconnecting...");
                        break;
                    }
                    // Broadcast user message to all connected peers
                    broadcast("[" + clientName + "]: " + message);
                }
            } catch (IOException e) {
                System.err.println("[WARN] Connection dropped for " + clientName + ": " + e.getMessage());
            } finally {
                if (clientName != null) {
                    activeClients.remove(clientName);
                    System.out.println("[DISCONNECT] " + clientName + " left.");
                    broadcast("[SYSTEM]: " + clientName + " left the chat. (Total online: " + activeClients.size() + ")");
                }
            }
        }

        /**
         * Broadcasts a message to all active TCP sockets in the registry.
         */
        private void broadcast(String message) {
            System.out.println("[BROADCAST] " + message);
            for (Map.Entry<String, PrintWriter> entry : activeClients.entrySet()) {
                entry.getValue().println(message);
            }
        }
    }
}
