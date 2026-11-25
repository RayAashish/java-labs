package lts.java11.httpclient;

import com.sun.net.httpserver.HttpServer;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;

/**
 * Java 11 LTS — Standard HttpClient (JEP 321): Sync & Async against Local Loopback Server.
 *
 * Uses an embedded HttpServer bound to ephemeral port 0 so this runs 100% offline with zero port conflicts!
 */
public class StandardHttpClientLocalDemo {

    // BEST PRACTICE: Single reusable thread-safe HttpClient instance (never instantiate per request!)
    private static final HttpClient SHARED_CLIENT = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_2)
            .connectTimeout(Duration.ofSeconds(3))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    public static void main(String[] args) throws Exception {
        System.out.println("=== Java 11 Standard HttpClient (JEP 321) Demo ===");

        // Ephemeral port 0 avoids BindException in parallel CI runs
        HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        int port = server.getAddress().getPort();

        server.createContext("/api/v1/health", exchange -> {
            byte[] bytes = "{\"status\":\"HEALTHY\",\"lts\":11}".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        });

        server.createContext("/api/v1/echo", exchange -> {
            byte[] reqBytes = exchange.getRequestBody().readAllBytes();
            String reply = "ECHO:" + new String(reqBytes, StandardCharsets.UTF_8);
            byte[] out = reply.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, out.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(out);
            }
        });

        var serverExecutor = Executors.newSingleThreadExecutor();
        server.setExecutor(serverExecutor);
        server.start();

        try {
            // 1. Synchronous GET
            HttpRequest getReq = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:" + port + "/api/v1/health"))
                    .timeout(Duration.ofSeconds(2))
                    .header("Accept", "application/json")
                    .GET()
                    .build();

            HttpResponse<String> syncResp = SHARED_CLIENT.send(getReq, HttpResponse.BodyHandlers.ofString());
            System.out.printf("[1] Sync GET status=%d, version=%s, body=%s%n",
                    syncResp.statusCode(), syncResp.version(), syncResp.body());

            // 2. Asynchronous POST via sendAsync() + CompletableFuture pipeline
            HttpRequest postReq = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:" + port + "/api/v1/echo"))
                    .timeout(Duration.ofSeconds(2))
                    .header("Content-Type", "text/plain")
                    .POST(HttpRequest.BodyPublishers.ofString("hello-java-11"))
                    .build();

            CompletableFuture<String> asyncBody = SHARED_CLIENT
                    .sendAsync(postReq, HttpResponse.BodyHandlers.ofString())
                    .thenApply(resp -> "status=" + resp.statusCode() + " body=" + resp.body());

            System.out.println("[2] Async POST result: " + asyncBody.join());
        } finally {
            server.stop(0);
            serverExecutor.shutdownNow();
        }
    }
}
