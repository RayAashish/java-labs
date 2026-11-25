# Interview Questions — Java 11 Standard `HttpClient`

## 🟢 Beginner

### Q1. What problems in `HttpURLConnection` did Java 11's `HttpClient` solve?
`HttpURLConnection` pre-dated HTTP/1.1, did not support HTTP/2 or WebSockets, offered only blocking synchronous I/O, had an awkward mutable API, and lacked reactive streaming bodies. `java.net.http.HttpClient` supports HTTP/1.1, HTTP/2 multiplexing, `CompletableFuture`-based async (`sendAsync`), WebSockets, and clean immutable builders.

---

## 🟡 Intermediate

### Q2. Why should `HttpClient` be created as a singleton rather than inside a helper method per call?
Each `HttpClient` spins up an internal NIO `SelectorManager` thread, worker pool, and socket connection pool. Instantiating one per HTTP call disables connection/HTTP/2 multiplexing reuse and quickly leaks threads and OS sockets under load!

### Q3. What is the difference between `connectTimeout` on `HttpClient` and `timeout` on `HttpRequest`?
- `HttpClient.Builder.connectTimeout`: limits TCP/TLS **handshake time only**.
- `HttpRequest.Builder.timeout`: limits total time waiting for the **HTTP response**, throwing `HttpTimeoutException` if exceeded. Always set both!

---

## 🔴 Senior

### Q4. How do `HttpResponse.BodyHandlers` avoid `OutOfMemoryError` on multi-gigabyte downloads?
Calling `BodyHandlers.ofString()` or `ofByteArray()` buffers the entire response body into Java heap memory! For large payloads, use **`BodyHandlers.ofFile(path)`** (streams directly to disk via NIO channels), **`BodyHandlers.ofInputStream()`**, or **`BodyHandlers.ofLines()`** (and make sure to close the stream).
