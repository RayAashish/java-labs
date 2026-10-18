package com.raysi.sockets.phase2_udp_sockets.codes;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;

/**
 * Author: raysi
 * Package: com.raysi.sockets.phase2_udp_sockets.codes
 * 
 * Phase 2: Connectionless UDP Datagram Server.
 * Demonstrates DatagramSocket listening, DatagramPacket buffer handling,
 * extracting sender metadata, and connectionless reply dispatch.
 */
public class UdpServer {

    private static final int PORT = 9876;
    private static final int BUFFER_SIZE = 1024; // 1 KB buffer per datagram

    public static void main(String[] args) {
        System.out.println("=== Starting UDP Datagram Server on port " + PORT + " (Author: raysi) ===");

        try (DatagramSocket socket = new DatagramSocket(PORT)) {
            byte[] receiveBuffer = new byte[BUFFER_SIZE];

            while (true) {
                // Initialize receiving packet with allocated buffer
                DatagramPacket receivePacket = new DatagramPacket(receiveBuffer, receiveBuffer.length);

                // blocks until an incoming UDP datagram arrives from the network
                socket.receive(receivePacket);

                String receivedMessage = new String(
                        receivePacket.getData(),
                        receivePacket.getOffset(),
                        receivePacket.getLength(),
                        StandardCharsets.UTF_8
                ).trim();

                InetAddress clientAddress = receivePacket.getAddress();
                int clientPort = receivePacket.getPort();

                System.out.println("[RECEIVED] " + receivedMessage + " from " + clientAddress + ":" + clientPort);

                // Formulate response
                String responseText = "UDP_ACK: Received '" + receivedMessage + "' at " + System.currentTimeMillis();
                byte[] responseBytes = responseText.getBytes(StandardCharsets.UTF_8);

                // UDP is connectionless: destination address and port must be explicitly specified in each outbound packet
                DatagramPacket sendPacket = new DatagramPacket(
                        responseBytes,
                        responseBytes.length,
                        clientAddress,
                        clientPort
                );

                socket.send(sendPacket);
                System.out.println("[REPLIED] Echoed back to " + clientAddress + ":" + clientPort);
            }
        } catch (IOException e) {
            System.err.println("[ERROR] UDP Server error: " + e.getMessage());
        }
    }
}
