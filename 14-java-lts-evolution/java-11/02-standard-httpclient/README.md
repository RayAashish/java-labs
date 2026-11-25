# Java 11 — Standard `HttpClient` (JEP 321)

## ⚡ 2-Minute Revision
- **Why Introduced:** Legacy `HttpURLConnection` (1997) was synchronous-only, lacked HTTP/2, struggled with `PATCH` verbs, and forced everyone to pull in Apache HttpClient / OkHttp.
- **Core Capabilities:** Immutable builders (`HttpClient`, `HttpRequest`), native **HTTP/2 multiplexing + server header compression**, **TLS**, **sync (`send`) & async (`sendAsync` -> `CompletableFuture`)**, built-in **WebSockets**, and reactive **`BodyHandlers` / `BodyPublishers`**.
- **CRITICAL PRODUCTION TRAP:** Never do `HttpClient.newHttpClient()` *per HTTP request*! Each `HttpClient` creates its own selector thread + connection pool (`Executor`), leaking sockets/threads fast. **Create ONE singleton `HttpClient` bean and reuse it!** (Also `.close()` / `try-with-resources` was added in Java 21!)

## 📂 Contents
- [`notes/standard-httpclient-deep-dive.md`](notes/standard-httpclient-deep-dive.md)
- [`codes/lts/java11/httpclient/StandardHttpClientLocalDemo.java`](codes/lts/java11/httpclient/StandardHttpClientLocalDemo.java)
- [`interview/INTERVIEW_QUESTIONS.md`](interview/INTERVIEW_QUESTIONS.md)
