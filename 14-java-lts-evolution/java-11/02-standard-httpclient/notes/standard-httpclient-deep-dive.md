# Standard `java.net.http.HttpClient` (JEP 321) Deep Dive

---

## 🟢 Layer 1 — Noob Track: 3 Immutable Building Blocks

Every `java.net.http` call follows one clean 3-step recipe:
1. **`HttpClient`** (The Reusable Browser / Engine): configured with timeouts, HTTP version, redirect policy, executor.
2. **`HttpRequest`** (The Envelope): URI, HTTP method (`GET`/`POST`/`PUT`/`DELETE`), headers, request-level timeout, body.
3. **`HttpResponse<T>`** (The Reply): status code, response headers, negotiated HTTP version, and typed body (`String`, `byte[]`, `Path`, `InputStream`, `Stream<String>`).

---

## 🟡 Layer 2 — Practitioner Track: Sync, Async & Best Practices

### 1. Synchronous vs Asynchronous Dispatch
- **`client.send(req, BodyHandlers.ofString())`**: Blocks current thread cleanly (great with **Java 21+ Virtual Threads**!).
- **`client.sendAsync(req, BodyHandlers.ofString())`**: Immediately returns `CompletableFuture<HttpResponse<String>>`, powered internally by NIO `Selector` + reactive flow backpressure!

### 2. Two Timeouts — Know the Difference!
- **`HttpClient.Builder.connectTimeout(Duration)`:** Fails if TCP handshake doesn't finish in time.
- **`HttpRequest.Builder.timeout(Duration)`:** Fails the whole HTTP exchange with `HttpTimeoutException` if response headers/body take too long. (Without request timeout, hung servers tie up requests indefinitely!)

### 3. Production Anti-Pattern: Per-Request `HttpClient` Allocation
- **BAD:** Calling `HttpClient.newBuilder()...build()` inside a controller/service method! Each client spawns its own `SelectorManager` thread and internal connection pool; until GC runs (or even longer while sockets stay `ESTABLISHED`/`CLOSE_WAIT`), you bleed descriptors and threads!
- **GOOD:** Configure ONE thread-safe singleton `HttpClient` per microservice (optionally with `.executor(Executors.newVirtualThreadPerTaskExecutor())` on Java 21+).

---

## 🔴 Layer 3 — Senior / JVM Track

- **HTTP/2 Upgrade Mechanics:** If `Version.HTTP_2` is selected over plain `http://`, `HttpClient` uses `h2c` HTTP/1.1 Upgrade (`Upgrade: h2c`) and falls back transparently to HTTP/1.1 if the server replies in 1.1; over HTTPS (`https://`), **ALPN** negotiates `h2` cleanly during TLS handshake.
- **Connection Pool Limits:** Tuned via system properties like `jdk.httpclient.connectionPoolSize`, `jdk.httpclient.keepalive.timeout`, and `jdk.httpclient.maxstreams` (HTTP/2).
- **Streaming Large Responses Safely:** Use `HttpResponse.BodyHandlers.ofLines()` or `ofFile(Path)` / `ofInputStream()` so multi-GB downloads don't OOM the JVM heap (unlike `BodyHandlers.ofString()`). **Note:** with `ofInputStream()` or `ofLines()`, always close the stream when finished!
