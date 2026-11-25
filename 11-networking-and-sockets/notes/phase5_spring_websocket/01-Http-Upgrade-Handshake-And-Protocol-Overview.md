# 01: HTTP Upgrade Handshake & Protocol Overview (RFC 6455 & STOMP)
**Author:** `raysi`  
**Package:** `com.raysi.sockets.phase5_spring_websocket`  
**Protocol Standard:** RFC 6455 (The WebSocket Protocol) & STOMP 1.2 Specification  

---

# 1. The HTTP-to-WebSocket Upgrade Handshake Mechanics

## Why Pure HTTP Cannot Support Real-Time Bidirectional Systems
HTTP/1.1 is inherently half-duplex and request-response driven. Every communication requires the client to initiate a connection, exchange request/response headers (often 500–2000 bytes of overhead), and either close the connection or park it via HTTP Keep-Alive. 

Techniques like HTTP polling or long-polling (Comet) suffer from:
1. **Severe Bandwidth & Header Overhead:** Sending 1KB of cookie and header data for every 20-byte payload.
2. **High Latency & Connection Churn:** Constant establishment and teardown of TCP sockets.
3. **Unidirectional Asymmetry:** The server cannot push unprompted data to the client without a pending request.

WebSocket solves this by executing a single standard HTTP handshake to negotiate protocol switching, after which the underlying TCP socket transitions into a persistent, bidirectional, full-duplex framing channel.

---

## The Handshake Protocol Sequence (RFC 6455)

```text
Client (Browser)                                    Server (Spring Boot)
   |                                                         |
   | -------- 1. HTTP GET /ws (Upgrade Request) -----------> |
   |    Headers:                                             |
   |      Host: localhost:8080                               |
   |      Upgrade: websocket                                 |
   |      Connection: Upgrade                                |
   |      Sec-WebSocket-Key: dGhlIHNhbXBsZSBub25jZQ==        |
   |      Sec-WebSocket-Version: 13                          |
   |                                                         |
   | <------- 2. HTTP 101 Switching Protocols -------------- |
   |    Headers:                                             |
   |      Upgrade: websocket                                 |
   |      Connection: Upgrade                                |
   |      Sec-WebSocket-Accept: s3pPLMBiTxaQ9kYGzzhZRbK+xOo= |
   |                                                         |
   | ======================================================= |
   |   HTTP TEARDOWN: TCP SOCKET REMAINS OPEN AS WEBSOCKET   |
   | ======================================================= |
   | <====== Bidirectional Binary/Text Frame Streaming =====> |
```

### Step 1: The Client Upgrade Request
The client initiates an ordinary HTTP/1.1 `GET` request containing mandatory upgrade headers:

```http
GET /ws HTTP/1.1
Host: localhost:8080
Connection: Upgrade
Upgrade: websocket
Sec-WebSocket-Version: 13
Sec-WebSocket-Key: dGhlIHNhbXBsZSBub25jZQ==
Sec-WebSocket-Protocol: v10.stomp, v11.stomp, v12.stomp
Origin: http://localhost:8080
```

- `Connection: Upgrade` & `Upgrade: websocket`: Signals the reverse proxy and server to transition the connection from HTTP to WebSocket.
- `Sec-WebSocket-Key`: A randomly generated 16-byte nonce, base64-encoded by the client. It prevents intermediate caching proxies from erroneously serving cached HTTP responses.
- `Sec-WebSocket-Version`: 13 (the standardized RFC 6455 version).
- `Sec-WebSocket-Protocol`: Requests one or more application sub-protocols (e.g., STOMP).

### Step 2: The Server Cryptographic Verification & HTTP 101 Response
To prove that the server genuinely understands the WebSocket protocol and is not an inadvertent HTTP proxy echoing headers, the server computes a signature:

$$\text{Sec-WebSocket-Accept} = \text{Base64}\Big(\text{SHA-1}\big(\text{Sec-WebSocket-Key} + \text{"258EAFA5-E914-47DA-95CA-C5AB0DC85B11"}\big)\Big)$$

Where `258EAFA5-E914-47DA-95CA-C5AB0DC85B11` is the globally unique RFC 6455 Magic GUID.

The server returns `HTTP 101 Switching Protocols`:

```http
HTTP/1.1 101 Switching Protocols
Upgrade: websocket
Connection: Upgrade
Sec-WebSocket-Accept: s3pPLMBiTxaQ9kYGzzhZRbK+xOo=
Sec-WebSocket-Protocol: v12.stomp
```

From this moment on, HTTP parsing is bypassed. Both endpoints read and write RFC 6455 binary/text frames directly over the established TCP socket.

---

# 2. WebSocket Frame Architecture (RFC 6455)

Unlike TCP's raw byte stream, WebSockets package data into discrete **Frames** with minimal overhead (2 to 10 bytes of framing headers):

```text
 0                   1                   2                   3
 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
+-+-+-+-+-------+-+-------------+-------------------------------+
|F|R|R|R| opcode|M| Payload len |    Extended payload length    |
|I|S|S|S|  (4)  |A|     (7)     |             (16/64)           |
|N|V|V|V|       |S|             |   (if payload len==126/127)   |
| |1|2|3|       |K|             |                               |
+-+-+-+-+-------+-+-------------+ - - - - - - - - - - - - - - - +
|     Extended payload length continued, if payload len == 127  |
+ - - - - - - - - - - - - - - - +-------------------------------+
|                               |Masking-key, if MASK set to 1  |
+-------------------------------+-------------------------------+
| Masking-key (continued)       |          Payload Data         |
+-------------------------------- - - - - - - - - - - - - - - - +
:                     Payload Data continued ...                :
+---------------------------------------------------------------+
```

### Frame Fields Breakdown:
1. **FIN (1 bit):** Indicates if this is the final fragment in a message. Enables streaming large messages across multiple frames without buffering the entire payload in memory.
2. **RSV 1-3 (3 bits):** Reserved for protocol extensions (e.g., `permessage-deflate` compression).
3. **Opcode (4 bits):** Defines the frame payload interpretation:
   - `0x0`: Continuation Frame
   - `0x1`: Text Frame (UTF-8 encoded)
   - `0x2`: Binary Frame
   - `0x8`: Connection Close Frame
   - `0x9`: Ping Frame
   - `0xA`: Pong Frame
4. **MASK (1 bit):** **All frames sent from client to server MUST be masked (MASK=1).** If an unmasked frame is received from a client, the server must terminate the connection immediately. (Prevents cache poisoning attacks on intermediary proxies). Server-to-client frames are unmasked.
5. **Payload Length (7 bits, 7+16 bits, or 7+64 bits):**
   - $0 \le \text{length} \le 125$: The 7-bit field is the actual length.
   - $\text{length} = 126$: The subsequent 2 bytes (16 bits) represent an unsigned 16-bit integer length ($\le 65,535$ bytes).
   - $\text{length} = 127$: The subsequent 8 bytes (64 bits) represent an unsigned 64-bit integer length.
6. **Masking Key (4 bytes):** 32-bit random key used to XOR decode the client payload.

---

# 3. Why Raw WebSockets Fall Short & Why STOMP is Essential

### The Raw WebSocket Dilemma
A raw WebSocket is equivalent to a raw TCP socket in the browser. It delivers raw text or binary frames with zero metadata:
- **No Destination / Routing:** There is no concept of a URL or channel. When the server receives a frame, it has no native way to know whether the message is intended for a chat room, an order execution engine, or a heartbeat.
- **No Subscription Semantics:** There is no built-in way for a client to state "I want updates only for Order #1234".
- **No Header Support:** You cannot attach tokens, content-types, correlation IDs, or timestamps to individual frames without inventing your own custom envelope.
- **Custom Parsing Overhead:** Every team ends up inventing an ad-hoc JSON protocol (e.g., `{"action": "subscribe", "channel": "orders"}`), resulting in brittle parsing logic.

### Enter STOMP: Simple Text Oriented Messaging Protocol
**STOMP** defines a standard frame format inspired by HTTP for enterprise message brokers. By using STOMP over WebSocket:
- Senders publish messages to standardized destination strings (`/topic/chat`, `/app/trade`).
- Receivers subscribe to specific topics or point-to-point queues.
- Spring provides full annotation-driven message routing (`@MessageMapping`, `@SendTo`, `@SubscribeMapping`).

### STOMP Frame Anatomy
A STOMP frame consists of a **Command**, followed by key-value **Headers**, a blank line, an optional **Body**, and a terminating **NULL byte (`^@` or `\0`)**:

```stomp
SEND
destination:/app/chat
content-type:application/json
content-length:44

{"sender":"raysi","content":"System online"} 
```

```stomp
SUBSCRIBE
id:sub-001
destination:/topic/public

 
```

---

# 4. SockJS Fallback Transport Hierarchy

When clients are restricted behind corporate firewalls, HTTP proxies that strip the `Upgrade` header, or restrictive antivirus software, raw WebSockets fail.

**SockJS** provides transparent emulation of the WebSocket API with an automatic transport fallback cascade:

```text
               +------------------------------------+
               |    Attempt Native RFC 6455         |
               |         WebSocket                  |
               +-----------------+------------------+
                                 | (Failed / Blocked)
                                 v
               +------------------------------------+
               |      HTTP Streaming Transport      |
               |   (XHR-Streaming / EventSource)    |
               +-----------------+------------------+
                                 | (Unsupported)
                                 v
               +------------------------------------+
               |       HTTP Long Polling            |
               |          (XHR-Polling)             |
               +------------------------------------+
```

1. **Native WebSocket:** Highest throughput, lowest latency, zero HTTP overhead.
2. **HTTP Streaming (XHR Streaming):** The client opens an HTTP request that the server never completes, streaming chunks as they arrive.
3. **HTTP Polling (XHR Polling):** Sequential long-polling requests where the server holds each request until new data arrives.
