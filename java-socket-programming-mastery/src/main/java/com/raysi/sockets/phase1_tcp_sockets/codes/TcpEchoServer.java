package com.raysi.sockets.phase1_tcp_sockets.codes;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;

/**
 * Author: raysi
 * Package: com.raysi.sockets.phase1_tcp_sockets.codes
 * 
 * Phase 1: Canonical Blocking Single-Threaded TCP Echo Server.
 * Demonstrates ServerSocket binding, SO_TIMEOUT, accept loop, and socket streams.
 */
public class TcpEchoServer {

    private static final int PORT = 8081;
    private static final int BACKLOG = 50; // Max queue length for incoming connection requests
    private static volatile boolean running = true;

    public static void main(String[] args) {
        System.out.println("=== Starting TCP Echo Server on port " + PORT + " (Author: raysi) ===");

        // Register JVM shutdown hook for graceful termination
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\n[SHUTDOWN] Terminating TCP Echo Server gracefully...");
            running = false;
        }));

        try (ServerSocket serverSocket = new ServerSocket(PORT, BACKLOG, InetAddress.getByName("0.0.0.0"))) {
            // Set SO_TIMEOUT on serverSocket so accept() does not block indefinitely when stopping
            serverSocket.setSoTimeout(2000);
            System.out.println("[INFO] Listening on " + serverSocket.getInetAddress() + ":" + serverSocket.getLocalPort());

            while (running) {
                try {
                    // accept() blocks until a client completes the TCP 3-way handshake (SYN -> SYN-ACK -> ACK)
                    Socket clientSocket = serverSocket.accept();
                    handleClient(clientSocket);
                } catch (SocketTimeoutException ignored) {
                    // Periodic timeout to evaluate the running flag
                }
            }
        } catch (IOException e) {
            System.err.println("[ERROR] Server error: " + e.getMessage());
        }
    }

    /**
     * Handles an accepted client connection sequentially (blocking the server loop).
     */
    private static void handleClient(Socket socket) {
        String clientInfo = socket.getRemoteSocketAddress().toString();
        System.out.println("\n[CONNECT] Accepted connection from: " + clientInfo);

        try (
            socket;
            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter writer = new PrintWriter(socket.getOutputStream(), true) // Auto-flush enabled
        ) {
            // Optional: Set read timeout on the client socket to prevent hung connections
            socket.setSoTimeout(30000);

            writer.println("220 Welcome to raysi TCP Echo Server. Type 'QUIT' to exit.");

            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println("[RECEIVED from " + clientInfo + "]: " + line);

                if ("QUIT".equalsIgnoreCase(line.trim())) {
                    writer.println("221 Goodbye!");
                    System.out.println("[DISCONNECT] Client requested QUIT: " + clientInfo);
                    break;
                }

                // Echo the line back to client
                writer.println("ECHO: " + line);
            }
        } catch (IOException e) {
            System.err.println("[WARN] Connection reset / IO error with " + clientInfo + ": " + e.getMessage());
        }
    }
}
