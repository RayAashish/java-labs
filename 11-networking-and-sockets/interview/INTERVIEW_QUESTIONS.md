# 11. Networking, Sockets, NIO Selectors & WebSockets — Interview Questions & Answers

---

## 🟢 Beginner Questions

### Q1. What is a Socket (`IP + Port`), and how do TCP and UDP differ?
A socket is a network endpoint (`IP:Port`).
- **TCP (`Socket` / `ServerSocket`):** Connection-oriented (3-way handshake), guaranteed ordered byte stream, congestion control (used in HTTP, DBs, SSH).
- **UDP (`DatagramSocket` / `DatagramPacket`):** Connectionless packets, no delivery/order guarantee, zero handshake latency (used in DNS, VoIP, gaming).

### Q2. What do `ServerSocket.accept()` and `BufferedReader.readLine()` have in common?
Both are **blocking** calls! `accept()` suspends until a TCP client completes the 3-way handshake; `readLine()` suspends until a newline arrives or the socket closes (`null`) or `SO_TIMEOUT` expires.

### Q3. Why should you always configure `socket.setSoTimeout(ms)`?
Without a read timeout, if a client silently drops offline (without sending TCP FIN), `readLine()` blocks the server thread **forever** until thread exhaustion!

---

## 🟡 Intermediate Questions

### Q4. Explain the TCP 3-Way Handshake and `TIME_WAIT` / `SO_REUSEADDR`.
1. Client -> `SYN`, Server -> `SYN-ACK`, Client -> `ACK`.
2. When a server restarts rapidly, the old listening port may sit in **`TIME_WAIT`** (2 MSL) causing `BindException: Address already in use`. Calling `serverSocket.setReuseAddress(true)` before `.bind()` allows binding cleanly immediately.

### Q5. Why does a single-threaded blocking server fail for multiple clients, and what replaces it?
While `handleClient()` loops on Client 1, Client 2 hangs in the OS accept backlog. Replace with:
1. Bounded `ExecutorService` thread pool (classic),
2. **`Executors.newVirtualThreadPerTaskExecutor()`** (Java 21+ — handles 100k+ concurrent blocking sockets effortlessly!), OR
3. Non-blocking NIO **`Selector`** event loops (Netty/Reactor).

### Q6. `HttpURLConnection` (legacy) vs Java 11 `java.net.http.HttpClient`?
`HttpClient` supports **HTTP/2 multiplexing**, **non-blocking async (`sendAsync` returning `CompletableFuture`)**, built-in **WebSocket**, clean Builder API, and HTTP/1.1 connection pooling cleanly out of the box!

---

## 🔴 Senior / Systems & Real-Time WebSockets Questions

### Q7. How does an NIO `Selector` handle 10,000+ connections on a single thread?
`ServerSocketChannel` & `SocketChannel` are set `configureBlocking(false)` and registered with a `Selector` for `OP_ACCEPT` / `OP_READ` / `OP_WRITE`. Under Linux, `Selector` wraps **`epoll(7)`** (`kqueue` on macOS), notifying the single event-loop thread ONLY of sockets with ready kernel buffers in $O(1)$! *(Never forget `iterator.remove()` on `selectedKeys()` or the same ready key spins CPU at 100%!)*

### Q8. Walk through the WebSocket HTTP Upgrade Handshake (RFC 6455).
1. Client sends HTTP/1.1 `GET /ws` with `Connection: Upgrade`, `Upgrade: websocket`, `Sec-WebSocket-Key: <random-base64>`, `Sec-WebSocket-Version: 13`.
2. Server appends RFC magic GUID (`258EAFA5-E914-47DA-95CA-C5AB0DC85B11`), computes SHA-1 + Base64 into `Sec-WebSocket-Accept`, and replies **`HTTP/1.1 101 Switching Protocols`**.
3. The underlying TCP connection remains open for bidirectional full-duplex WebSocket frames!

### Q9. What is STOMP over WebSocket in Spring Boot?
WebSocket gives raw unstructured frames. **STOMP (Simple Text Oriented Messaging Protocol)** adds `CONNECT`, `SUBSCRIBE`, `SEND` frames with destination routing prefixes: `/app/...` hits `@MessageMapping` controllers, `/topic/...` broadcasts pub/sub messages via an in-memory or RabbitMQ/ActiveMQ broker, and `/user/queue/...` targets individual user sessions.

### Q10. Why does NIO `SocketChannel.write(byteBuffer)` sometimes write fewer bytes than `buffer.remaining()`?
In non-blocking mode, if the OS TCP send buffer is full (slow consumer / backpressure), `channel.write(buf)` returns immediately with the number of bytes actually accepted (possibly `0`!). Production servers register `SelectionKey.OP_WRITE` and resume writing remaining bytes only when the socket becomes writable again.
