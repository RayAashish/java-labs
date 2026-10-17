package com.raysi.lts.java11.codes;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;

/**
 * Author: raysi
 * Package: com.raysi.lts.java11.codes
 *
 * Demonstrates:
 * 1. Standard Java 11 HttpClient (JEP 321) replacing Apache HttpClient and OkHttp.
 * 2. HTTP/2 protocol support, connection pooling, and timeout configuration.
 * 3. Asynchronous non-blocking I/O execution via sendAsync() & CompletableFuture pipeline.
 * 4. Self-contained local embedded HTTP server for 100% offline test reliability.
 */
public class StandardHttpClientAsyncDemo {

    public static void main(String[] args) throws Exception {
        System.out.println("=== Java 11 Standard HttpClient (Async & Reactive) Demo ===");

        // 1. Start an in-memory loopback HTTP server to eliminate external internet reliance
        int port = 8089;
        HttpServer server = HttpServer.create(new InetSocketAddress("localhost", port), 0);
        server.createContext("/api/v1/health", exchange -> {
            String response = "{\"status\":\"HEALTHY\",\"node\":\"lts-node-11\",\"uptime\":42000}";
            byte[] bytes = response.getBytes();
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        });
        server.setExecutor(Executors.newSingleThreadExecutor());
        server.start();
        System.out.println("[+] Embedded HTTP server active on port: " + port);

        try {
            // 2. Build the Modern HttpClient
            HttpClient client = HttpClient.newBuilder()
                    .version(HttpClient.Version.HTTP_2)
                    .connectTimeout(Duration.ofSeconds(5))
                    .followRedirects(HttpClient.Redirect.NORMAL)
                    .build();

            // 3. Assemble Immutable HttpRequest
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:" + port + "/api/v1/health"))
                    .timeout(Duration.ofSeconds(3))
                    .header("Accept", "application/json")
                    .header("User-Agent", "Java11-Enterprise-LTS-Client/1.0")
                    .GET()
                    .build();

            // 4. Asynchronous Non-Blocking Dispatch with sendAsync()
            System.out.println("[+] Dispatching asynchronous HTTP request via sendAsync()...");
            CompletableFuture<HttpResponse<String>> futureResponse = client.sendAsync(
                    request, 
                    HttpResponse.BodyHandlers.ofString()
            );

            // 5. Compose Asynchronous Pipeline with CompletableFuture
            CompletableFuture<String> processedBodyFuture = futureResponse
                    .thenApply(response -> {
                        System.out.println("  [Async Callback] Status Code: " + response.statusCode());
                        System.out.println("  [Async Callback] HTTP Version: " + response.version());
                        return response.body();
                    })
                    .thenApply(body -> "Processed Payload: " + body.toUpperCase());

            // Await result asynchronously
            String result = processedBodyFuture.join();
            System.out.println(">> Final Async Pipeline Output: " + result);

        } finally {
            server.stop(0);
            System.out.println("[+] Embedded server stopped cleanly.");
        }
    }
}
