# 01: Native Java 11+ HttpClient & WebSocket Client (JEP 321)
**Author:** `raysi`  
**Package:** `com.raysi.sockets.phase4_java11_websocket_client`  
**Standard:** Java 11 LTS (JEP 321 Standard HTTP Client & WebSocket API)

---

# 1. The Standard Native WebSocket Revolution in Java 11

Prior to Java 11, the JDK had **no built-in WebSocket support**. Java developers building backend microservices that needed to talk to streaming APIs (crypto exchanges, stock tickers, message brokers) were forced to import heavy external dependencies:
* Eclipse Jetty WebSocket Client
* GlassFish Tyrus
* Java-WebSocket library

With **JEP 321 (Java 11 LTS)**, the standard `java.net.http.HttpClient` suite introduced native, production-grade, zero-dependency WebSocket support via:
* `java.net.http.WebSocket`
* `java.net.http.WebSocket.Listener`
* `java.net.http.WebSocket.Builder`

---

# 2. Reactive Backpressure & The Flow API Connection

The Java 11 WebSocket API is designed around non-blocking asynchronous I/O and reactive streams backpressure:

```text
[ Incoming Network Stream ] ---> [ OS TCP Buffer ]
                                         |
                                         | webSocket.request(1)
                                         v
                               [ WebSocket.Listener ]
                               (Processes onText / onBinary)
                                         |
                                         | Completes frame handling
                                         v
                               [ Calls webSocket.request(1) ] ---> (Ready for next frame)
```

### The Role of `webSocket.request(long n)`:
* When `onOpen(webSocket)` fires, incoming message delivery is **paused** until the listener explicitly invokes `webSocket.request(n)`.
* Calling `webSocket.request(1)` signals to the underlying engine: *"I am ready to receive exactly 1 more message fragment."*
* **Architectural Advantage:** Prevents **Memory Exhaustion**. If a server floods the client with 50,000 frames/sec, a slow consumer calling `request(1)` applies TCP backpressure upstream, throttling the sender's TCP sliding window instead of buffering millions of objects on the JVM heap.

---

# 3. Message Fragmentation & The `last` Boolean Flag

WebSocket frames can split large messages across multiple chunks:
```java
@Override
public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
    buffer.append(data);
    if (last) {
        String fullMessage = buffer.toString();
        processCompleteMessage(fullMessage);
        buffer.setLength(0);
    }
    webSocket.request(1);
    return null;
}
```
* **`boolean last`**:
  * `false`: Indicates intermediate frame fragment (Opcode 0x0 Continuation Frame).
  * `true`: Indicates final fragment (FIN bit = 1). The complete message is now fully received.

---

# 4. Control Frames vs Data Frames (RFC 6455 Rules)

| Frame Category | Opcodes | Characteristics & Constraints |
| :--- | :--- | :--- |
| **Data Frames** | `0x1` (Text), `0x2` (Binary) | Can be fragmented (`last = false`). Payload can be arbitrarily large (gigabytes). |
| **Control Frames**| `0x8` (Close), `0x9` (Ping), `0xA` (Pong) | **Cannot be fragmented** (FIN must always be 1). Payload cannot exceed **125 bytes**. Can be injected between fragments of an ongoing data message. |

### Automatic Ping-Pong Handling in Java 11:
Per RFC 6455, when a client receives a `Ping` frame, it **must** return a `Pong` frame with the identical payload. Java 11's implementation handles this automatically in the background—the application listener's `onPing` method is merely a hook for telemetry.

---

# 5. Standard WebSocket Close Status Codes

| Code | RFC 6455 Meaning | Architectural Scenario |
| :--- | :--- | :--- |
| **1000** | Normal Closure | Connection successfully completed its purpose. |
| **1001** | Going Away | Server shutting down or client browser navigating to a new URL. |
| **1002** | Protocol Error | Received invalid framing, unmasked client frame, or illegal opcode. |
| **1003** | Unsupported Data | Received binary data when only text was expected. |
| **1006** | Abnormal Closure | **Never sent on the wire.** Internal code synthesized by the client when the TCP socket crashes abruptly without a Close handshake. |
| **1011** | Internal Server Error | Server encountered an unhandled exception preventing request completion. |
