package com.raysi.sockets.phase4_java11_websocket_client.codes;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Author: raysi
 * Package: com.raysi.sockets.phase4_java11_websocket_client.codes
 * 
 * Phase 4: Native Java 11+ WebSocket Client (JEP 321 standard java.net.http stack).
 * Demonstrates asynchronous WebSocket connection negotiation, reactive frame listening,
 * full-duplex text/binary frame transfer, ping-pong liveness, and graceful close handshake.
 */
public class NativeJavaWebSocketClient {

    private static final String DEFAULT_WEBSOCKET_URI = "wss://echo.websocket.org";

    public static void main(String[] args) throws Exception {
        String targetUri = args.length > 0 ? args[0] : DEFAULT_WEBSOCKET_URI;
        System.out.println("=== Initializing Native Java 11 WebSocket Client (Author: raysi) ===");
        System.out.println("[INFO] Connecting to: " + targetUri);

        CountDownLatch latch = new CountDownLatch(1);

        // 1. Build the modern Java 11 standard HttpClient
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();

        // 2. Define the asynchronous Reactive WebSocket Listener
        WebSocket.Listener listener = new WebSocket.Listener() {
            private final StringBuilder messageBuffer = new StringBuilder();

            @Override
            public void onOpen(WebSocket webSocket) {
                System.out.println("[CONNECTED] WebSocket session opened successfully.");
                // Request 1 incoming message frame (Reactive Flow backpressure)
                webSocket.request(1);
            }

            @Override
            public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
                messageBuffer.append(data);
                if (last) {
                    System.out.println("[RECEIVED TEXT FRAME]: " + messageBuffer);
                    messageBuffer.setLength(0); // Clear buffer for next message
                }
                // Request next frame from underlying channel
                webSocket.request(1);
                return null; // Return null or CompletableFuture if processing is asynchronous
            }

            @Override
            public CompletionStage<?> onBinary(WebSocket webSocket, ByteBuffer data, boolean last) {
                System.out.println("[RECEIVED BINARY FRAME]: " + data.remaining() + " bytes");
                webSocket.request(1);
                return null;
            }

            @Override
            public CompletionStage<?> onPing(WebSocket webSocket, ByteBuffer message) {
                System.out.println("[RECEIVED PING]: Echoing automatic PONG");
                // The implementation automatically returns a matching Pong frame per RFC 6455
                webSocket.request(1);
                return null;
            }

            @Override
            public CompletionStage<?> onPong(WebSocket webSocket, ByteBuffer message) {
                System.out.println("[RECEIVED PONG]: Server heartbeat ACK");
                webSocket.request(1);
                return null;
            }

            @Override
            public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
                System.out.println("[CLOSED] Server closed connection. Code=" + statusCode + ", Reason=" + reason);
                latch.countDown();
                return null;
            }

            @Override
            public void onError(WebSocket webSocket, Throwable error) {
                System.err.println("[ERROR] WebSocket error encountered: " + error.getMessage());
                latch.countDown();
            }
        };

        // 3. Initiate the asynchronous HTTP upgrade handshake
        CompletableFuture<WebSocket> wsFuture = httpClient.newWebSocketBuilder()
                .header("User-Agent", "raysi-java-websocket-client/1.0")
                .connectTimeout(Duration.ofSeconds(10))
                .buildAsync(URI.create(targetUri), listener);

        wsFuture.thenAccept(webSocket -> {
            try {
                // 4. Send asynchronous text frames
                System.out.println("[SEND] Sending hello text frame...");
                webSocket.sendText("Hello from Native Java 11 WebSocket Client! (Author: raysi)", true);

                Thread.sleep(1000);

                // 5. Send Ping frame
                ByteBuffer pingData = ByteBuffer.wrap("PING_CHECK".getBytes(StandardCharsets.UTF_8));
                System.out.println("[SEND] Dispatching Ping heartbeat...");
                webSocket.sendPing(pingData);

                Thread.sleep(1000);

                // 6. Graceful close handshake (Status code 1000 = Normal Closure)
                System.out.println("[CLOSING] Sending CLOSE frame (1000)...");
                webSocket.sendClose(WebSocket.NORMAL_CLOSURE, "Session completed");

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).exceptionally(ex -> {
            System.err.println("[HANDSHAKE FAILED] Could not establish WebSocket connection: " + ex.getMessage());
            latch.countDown();
            return null;
        });

        // Await completion or timeout
        boolean completed = latch.await(15, TimeUnit.SECONDS);
        if (!completed) {
            System.out.println("[INFO] Demo timeout reached. Terminating client.");
        }
    }
}
