# Java Socket Programming & Spring WebSocket Mastery
**Author:** `raysi`  
**Package Base:** `com.raysi.sockets`  
**Repository:** `java-socket-programming-mastery`

A comprehensive, interview-grade engineering guide and codebase covering low-level network programming through modern real-time architectures: Blocking TCP Sockets, Connectionless UDP Datagrams, Java NIO Selectors & Channels, Native Java 11 WebSockets, and Spring Boot STOMP Message Brokers.

---

## 🌐 The Network Protocol Evolution Hierarchy

| Protocol / Layer | Core Java Class / Tool | Concurrency Model | Characteristics & Production Use Cases |
| :--- | :--- | :--- | :--- |
| **1. TCP (Transmission Control Protocol)** | `java.net.Socket`, `ServerSocket` | Thread-per-connection (or Virtual Threads) | **Connection-oriented, reliable, ordered stream of bytes.** 3-way handshake, TCP congestion control, sliding window. Used for HTTP, relational databases, secure file transfers. |
| **2. UDP (User Datagram Protocol)** | `java.net.DatagramSocket`, `DatagramPacket` | Event-driven / Packet-polling loop | **Connectionless, low-latency, unreliable datagrams.** Zero handshake overhead, no retransmission, no flow control. Used for real-time multiplayer gaming, VoIP, DNS queries, live media streaming. |
| **3. Non-Blocking NIO Sockets** | `java.nio.channels.SocketChannel`, `Selector` | Single/Multi-threaded Event Loop (Reactor) | **Non-blocking multiplexed I/O.** Single selector monitors thousands of sockets for `OP_READ`/`OP_WRITE`/`OP_ACCEPT`. Used in Netty, Tomcat NIO, ultra-high-throughput gateways. |
| **4. Native WebSocket Client** | `java.net.http.WebSocket` (Java 11+) | Asynchronous Reactive Listener | **Bidirectional full-duplex communication over single TCP socket.** Standardized RFC 6455 framing, built into JDK without external libraries. Used for native microservice-to-broker links. |
| **5. Spring WebSocket + STOMP** | `spring-boot-starter-websocket` | ThreadPool Message Channel Pipeline | **Enterprise publish-subscribe messaging layer.** Adds routing prefixes (`/topic`, `/queue`, `/app`), user destinations, SockJS fallback, and message broker integration on top of WebSockets. |

---

## 🗂️ Repository Architecture & Directory Tree

```text
java-socket-programming-mastery/
├── README.md                                            # Master Protocol Cheat Sheet (TCP vs UDP vs WebSocket)
└── src/
    └── main/
        └── java/
            └── com/
                └── raysi/
                    └── sockets/
                        ├── phase1_tcp_sockets/          # Blocking TCP: The Foundation of Network Programming
                        │   ├── codes/
                        │   │   ├── TcpEchoServer.java
                        │   │   └── MultiThreadedTcpClientServerChat.java
                        │   └── notes/
                        │       ├── 01-Sockets-ServerSocket-And-TCP-Handshake.md
                        │       └── 02-Blocking-Streams-And-Connection-Lifecycle.md
                        │
                        ├── phase2_udp_sockets/          # Connectionless UDP: DatagramPackets for Speed/Streaming
                        │   ├── codes/
                        │   │   ├── UdpServer.java
                        │   │   └── UdpClient.java
                        │   └── notes/
                        │       └── 01-DatagramSocket-Vs-Socket-Packet-Loss.md
                        │
                        ├── phase3_nio_sockets/          # Non-Blocking NIO: Selectors & SocketChannels
                        │   ├── codes/
                        │   │   ├── NonBlockingSelectorEchoServer.java
                        │   └── notes/
                        │       └── 01-Nio-SocketChannels-And-Event-Loops.md
                        │
                        ├── phase4_java11_websocket_client/ # Native Java 11+ WebSocket Client
                        │   ├── codes/
                        │   │   └── NativeJavaWebSocketClient.java
                        │   └── notes/
                        │       └── 01-Native-Java11-HttpClient-Websockets.md
                        │
                        └── phase5_spring_websocket/     # Spring Boot Real-Time WebSockets & STOMP Protocol
                            ├── codes/
                            │   ├── WebSocketConfig.java
                            │   ├── ChatMessageController.java
                            │   └── static/
                            │       └── index.html       # SockJS + STOMP client test page
                            └── notes/
                                ├── 01-Http-Upgrade-Handshake-And-Protocol-Overview.md
                                └── 02-Spring-WebSocket-Stomp-Brokers-And-Subscribers.md
```

### 📚 Architecture Documentation Deep-Dives:
- Phase 1: [01-Sockets-ServerSocket-And-TCP-Handshake.md](src/main/java/com/raysi/sockets/phase1_tcp_sockets/notes/01-Sockets-ServerSocket-And-TCP-Handshake.md)
- Phase 1: [02-Blocking-Streams-And-Connection-Lifecycle.md](src/main/java/com/raysi/sockets/phase1_tcp_sockets/notes/02-Blocking-Streams-And-Connection-Lifecycle.md)
- Phase 2: [01-DatagramSocket-Vs-Socket-Packet-Loss.md](src/main/java/com/raysi/sockets/phase2_udp_sockets/notes/01-DatagramSocket-Vs-Socket-Packet-Loss.md)
- Phase 3: [01-Nio-SocketChannels-And-Event-Loops.md](src/main/java/com/raysi/sockets/phase3_nio_sockets/notes/01-Nio-SocketChannels-And-Event-Loops.md)
- Phase 4: [01-Native-Java11-HttpClient-Websockets.md](src/main/java/com/raysi/sockets/phase4_java11_websocket_client/notes/01-Native-Java11-HttpClient-Websockets.md)
- Phase 5: [01-Http-Upgrade-Handshake-And-Protocol-Overview.md](src/main/java/com/raysi/sockets/phase5_spring_websocket/notes/01-Http-Upgrade-Handshake-And-Protocol-Overview.md)
- Phase 5: [02-Spring-WebSocket-Stomp-Brokers-And-Subscribers.md](src/main/java/com/raysi/sockets/phase5_spring_websocket/notes/02-Spring-WebSocket-Stomp-Brokers-And-Subscribers.md)

---

## 💡 How a Spring WebSocket Works Under the Hood

1. **The HTTP Upgrade Handshake:**
   The client sends an ordinary HTTP/1.1 `GET` request to `ws://localhost:8080/ws` with upgrade headers:
   * `Connection: Upgrade`
   * `Upgrade: websocket`
   * `Sec-WebSocket-Key: <nonce>`
   * `Sec-WebSocket-Version: 13`
2. **Protocol Switching (101):**
   The server computes `Sec-WebSocket-Accept` using SHA-1 and the RFC 6455 Magic GUID, responding with `HTTP 101 Switching Protocols`. The TCP socket remains persistently open, eliminating HTTP request/response overhead.
3. **STOMP Sub-Protocol Over Frames:**
   Instead of raw unstructured bytes, Spring communicates using **STOMP (Simple Text Oriented Messaging Protocol)**. STOMP introduces HTTP-like headers and frames allowing clients to:
   * `SUBSCRIBE` to broadcast topics (`/topic/public`) or private queues (`/user/queue/notifications`).
   * `SEND` messages to application endpoints (`/app/chat`).
4. **Broker & Pipeline Dispatch:**
   * Messages starting with `/app` route to `@MessageMapping` controllers for validation and persistence.
   * Messages starting with `/topic` route directly to the in-memory broker (or external RabbitMQ relay) to fan out to connected subscribers.

---

## 🚀 Execution & Verification Guide

### 1. Phase 1: Blocking TCP Sockets
```bash
# Compile and run TCP Echo Server
javac -d out src/main/java/com/raysi/sockets/phase1_tcp_sockets/codes/TcpEchoServer.java
java -cp out com.raysi.sockets.phase1_tcp_sockets.codes.TcpEchoServer

# Test connection via netcat or telnet:
nc localhost 8081

# Compile and run Multi-Threaded TCP Chat Server
javac -d out src/main/java/com/raysi/sockets/phase1_tcp_sockets/codes/MultiThreadedTcpClientServerChat.java
java -cp out com.raysi.sockets.phase1_tcp_sockets.codes.MultiThreadedTcpClientServerChat
```

### 2. Phase 2: Connectionless UDP Sockets
```bash
# Compile UDP Server and Client
javac -d out src/main/java/com/raysi/sockets/phase2_udp_sockets/codes/*.java

# Terminal 1: Start UDP Server
java -cp out com.raysi.sockets.phase2_udp_sockets.codes.UdpServer

# Terminal 2: Run UDP Client
java -cp out com.raysi.sockets.phase2_udp_sockets.codes.UdpClient
```

### 3. Phase 3: Non-Blocking NIO Reactor Server
```bash
# Compile and run NIO Selector Echo Server
javac -d out src/main/java/com/raysi/sockets/phase3_nio_sockets/codes/*.java
java -cp out com.raysi.sockets.phase3_nio_sockets.codes.NonBlockingSelectorEchoServer

# Test with netcat:
nc localhost 8083
```

### 4. Phase 4: Native Java 11 WebSocket Client
```bash
# Compile and run Native WebSocket Client against public echo server
javac -d out src/main/java/com/raysi/sockets/phase4_java11_websocket_client/codes/*.java
java -cp out com.raysi.sockets.phase4_java11_websocket_client.codes.NativeJavaWebSocketClient
```

### 5. Phase 5: Spring Boot WebSocket & STOMP Messaging
Add `spring-boot-starter-websocket` to your Maven `pom.xml`:
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-websocket</artifactId>
</dependency>
```
Open `http://localhost:8080/` in browser to load `index.html`, connect via SockJS + STOMP, subscribe to `/topic/public`, and exchange real-time messages.
