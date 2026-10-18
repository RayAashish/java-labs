# 01: Sockets, ServerSocket & The TCP Handshake Mechanics
**Author:** `raysi`  
**Package:** `com.raysi.sockets.phase1_tcp_sockets`  
**Topic:** Transport Layer Foundations, Socket Abstractions & Handshake Lifecycle

---

# 1. Anatomy of a Network Socket

A **Socket** is an operating system abstraction representing an endpoint for two-way communication between two processes across an IP network.

At the kernel layer, a socket is identified by a **5-tuple**:
$$\text{Socket Connection} = \big(\text{Source IP}, \text{Source Port}, \text{Destination IP}, \text{Destination Port}, \text{Protocol (TCP/UDP)}\big)$$

### Java Socket Classes:
* **`java.net.ServerSocket`**: The listener. Binds to a local port on the server, registers with the OS kernel, and dequeues established incoming TCP connections.
* **`java.net.Socket`**: The bidirectional communication channel. On the client, it initiates a connection; on the server, it is returned by `serverSocket.accept()` to handle the client.

---

# 2. Under the Hood: The TCP 3-Way Handshake

Before a single byte of application data is transmitted, the client and server execute the **TCP 3-Way Handshake**:

```text
Client (Socket)                                       Server (ServerSocket)
      |                                                        |
      | ------------- 1. SYN (seq = x) ----------------------> |
      |                                                        | (OS places connection in
      |                                                        |  SYN Backlog Queue)
      | <------------ 2. SYN-ACK (seq = y, ack = x + 1) ------ |
      |                                                        |
      | ------------- 3. ACK (seq = x + 1, ack = y + 1) -----> |
      |                                                        | (Connection moves to
      |                                                        |  Accept Queue)
      |                                                        |
      |                                                        | serverSocket.accept() returns
      | <============== ESTABLISHED CONNECTION ===============> | new Socket instance
```

### The Kernel Queues:
1. **SYN Backlog (Incomplete Connection Queue):** When the server receives `SYN`, it stores the half-open connection in this queue while awaiting the final `ACK`. (Vulnerable to SYN Flood attacks without `tcp_syncookies`).
2. **Accept Queue (Completed Connection Queue):** Once the client returns the final `ACK`, the connection transitions to `ESTABLISHED` and is placed into the Accept Queue.
3. **`serverSocket.accept()`:** This method does **not** perform the network handshake. The OS kernel completes the 3-way handshake in the background. `accept()` simply dequeues an already-established connection from the Accept Queue. If the queue is empty, `accept()` blocks the calling thread.

---

# 3. The TCP 4-Way Teardown & The TIME_WAIT State

Graceful connection termination requires exchanging `FIN` (Finish) control flags in both directions:

```text
Initiator (e.g., Client)                              Receiver (e.g., Server)
      |                                                        |
      | ----------------- 1. FIN (seq = u) ------------------> |
      | <---------------- 2. ACK (ack = u + 1) --------------- |
      |                                                        | (Receiver can still send
      |                                                        |  buffered data: CLOSE_WAIT)
      | <---------------- 3. FIN (seq = v) ------------------- |
      | ----------------- 4. ACK (ack = v + 1) --------------> |
      |                                                        | (Receiver socket closes)
      | [ Enters TIME_WAIT state (2 * MSL) ]                   |
```

### Why TIME_WAIT Matters to Backend Architects:
* The active closer enters the `TIME_WAIT` state, remaining there for **$2 \times \text{MSL}$ (Maximum Segment Lifetime)**, typically 60 to 120 seconds.
* **Purpose:** Ensures the final `ACK` arrived reliably and prevents lingering delayed packets from an old connection from corrupting a new connection reusing the same port.
* **Production Hazard:** If a server constantly initiates closes under high traffic, thousands of sockets accumulate in `TIME_WAIT`, exhausting ephemeral ports (`EADDRINUSE`). Solution: Set `SO_REUSEADDR` (`serverSocket.setReuseAddress(true)`).

---

# 4. Critical Socket Options (`SocketOptions`)

| Socket Option | Java Method | Purpose & Architectural Impact |
| :--- | :--- | :--- |
| `SO_TIMEOUT` | `socket.setSoTimeout(ms)` | Max time `read()` or `accept()` will block before throwing `SocketTimeoutException`. Prevents hung threads. |
| `SO_REUSEADDR` | `serverSocket.setReuseAddress(true)` | Allows binding to a port still held in `TIME_WAIT`. Essential for rapid server restarts. |
| `TCP_NODELAY` | `socket.setTcpNoDelay(true)` | Disables Nagle’s algorithm. Sends packets immediately without waiting to coalesce small buffers. Critical for low-latency messaging. |
| `SO_KEEPALIVE` | `socket.setKeepAlive(true)` | Enables OS-level TCP probes on idle connections. (Default interval is usually 2 hours). |
| `SO_RCVBUF` / `SO_SNDBUF`| `socket.setReceiveBufferSize(bytes)` | Tunes the OS kernel socket window sizes for high-bandwidth/high-latency networks. |
