package com.raysi.sockets.phase2_udp_sockets.codes;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;

/**
 * Author: raysi
 * Package: com.raysi.sockets.phase2_udp_sockets.codes
 * 
 * Phase 2: Connectionless UDP Client.
 * Demonstrates DatagramPacket transmission, SO_TIMEOUT for packet loss detection,
 * and handling unreliable connectionless delivery.
 */
public class UdpClient {

    private static final String SERVER_HOST = "localhost";
    private static final int SERVER_PORT = 9876;
    private static final int TIMEOUT_MS = 2500;

    public static void main(String[] args) {
        System.out.println("=== UDP Client Initializing (Author: raysi) ===");

        try (DatagramSocket socket = new DatagramSocket()) {
            // Set socket timeout so receive() doesn't hang forever if the UDP packet drops
            socket.setSoTimeout(TIMEOUT_MS);

            InetAddress serverAddress = InetAddress.getByName(SERVER_HOST);

            String[] testMessages = {
                "Ping 1: System Health Check",
                "Ping 2: Sensor Telemetry Packet",
                "Ping 3: Audio Stream Chunk #42"
            };

            for (String message : testMessages) {
                byte[] sendBuffer = message.getBytes(StandardCharsets.UTF_8);

                // Prepare outbound DatagramPacket
                DatagramPacket sendPacket = new DatagramPacket(
                        sendBuffer,
                        sendBuffer.length,
                        serverAddress,
                        SERVER_PORT
                );

                System.out.println("\n[SENDING] '" + message + "' to " + serverAddress + ":" + SERVER_PORT);
                socket.send(sendPacket);

                // Await response
                byte[] receiveBuffer = new byte[1024];
                DatagramPacket receivePacket = new DatagramPacket(receiveBuffer, receiveBuffer.length);

                try {
                    socket.receive(receivePacket);
                    String response = new String(
                            receivePacket.getData(),
                            receivePacket.getOffset(),
                            receivePacket.getLength(),
                            StandardCharsets.UTF_8
                    );
                    System.out.println("[RECEIVED ACK] " + response);
                } catch (SocketTimeoutException e) {
                    System.err.println("[TIMEOUT] Packet dropped or server unreachable (Simulated UDP loss)");
                }

                Thread.sleep(1000);
            }

        } catch (IOException | InterruptedException e) {
            System.err.println("[ERROR] UDP Client error: " + e.getMessage());
        }
    }
}
