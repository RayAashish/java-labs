# 11. Networking, Sockets, NIO Selectors & WebSockets

> **Consolidated Module:** Combines beginner-friendly blocking TCP/UDP sockets with non-blocking NIO `Selector` event loops, native Java 11+ `HttpClient` WebSockets, and Spring Boot STOMP reference architecture.

---

## ⚡ 2-Minute Rapid Revision Card

- **TCP vs UDP:** **TCP** (`Socket` / `ServerSocket`) is connection-oriented (3-way SYN/SYN-ACK/ACK handshake), ordered, reliable byte streams. **UDP** (`DatagramSocket` / `DatagramPacket`) is connectionless, zero-handshake, fastest for gaming/VoIP/DNS.
- **Always set `SO_TIMEOUT`:** Without `socket.setSoTimeout(ms)`, a dead peer causes `readLine()` to hang a thread forever!
- **Why Classic Blocking I/O Doesn't Scale to 100k Connections:** 1 platform thread = ~1 MB stack + OS context switching -> ~3k–5k connections max. Solved by either **NIO `Selector`** (1 event-loop thread multiplexes thousands of `SocketChannel`s via `epoll`/`kqueue`) OR **Java 21+ Virtual Threads** (blocking code with microscopic heap stacks!).
- **WebSockets (RFC 6455):** Starts as an HTTP/1.1 `GET` with `Upgrade: websocket` -> `101 Switching Protocols` -> persistent full-duplex frame pipe.

---

## 📂 Contents

| Phase | Level | Guide & Labs |
|---|---|---|
| **Beginner TCP** | 🟢 Noob | [`notes/00-beginner-socket-programming-notes.md`](notes/00-beginner-socket-programming-notes.md) + `sockets.beginner.SimpleTcpServer` / `SimpleTcpClient` |
| **Phase 1: TCP** | 🟡 | [`notes/phase1_tcp_sockets/`](notes/phase1_tcp_sockets/) + `TcpEchoServer` / `MultiThreadedTcpClientServerChat` |
| **Phase 2: UDP** | 🟡 | [`notes/phase2_udp_sockets/`](notes/phase2_udp_sockets/) + `UdpServer` / `UdpClient` |
| **Phase 3: NIO Selector** | 🔴 Senior | [`notes/phase3_nio_sockets/`](notes/phase3_nio_sockets/) + `NonBlockingSelectorEchoServer` |
| **Phase 4: WebSocket** | 🔴 Modern | [`notes/phase4_java11_websocket_client/`](notes/phase4_java11_websocket_client/) + `NativeJavaWebSocketClient` |
| **Phase 5: Spring STOMP** | 🔴 Enterprise | [`notes/phase5_spring_websocket/`](notes/phase5_spring_websocket/) + [`spring-websocket-reference/`](spring-websocket-reference/) |
| **Interview** | 🎯 All | [`interview/INTERVIEW_QUESTIONS.md`](interview/INTERVIEW_QUESTIONS.md) |

---

## 🚀 Compile & Run (Pure JDK Labs)

```bash
javac -d out $(find 11-networking-and-sockets/codes -name "*.java")

# Run TCP Echo Server (Terminal 1) and test with: nc localhost 8081
java -cp out sockets.phase1_tcp_sockets.TcpEchoServer

# Run Non-Blocking NIO Selector Echo Server (test with: nc localhost 8083)
java -cp out sockets.phase3_nio_sockets.NonBlockingSelectorEchoServer
```
