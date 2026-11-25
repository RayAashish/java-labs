# Java Socket Programming
> Study Notes

---

## 1. What is Socket Programming?

Socket programming enables two computers to communicate over a network using TCP or UDP. A socket is an endpoint for sending/receiving data between a client and a server.

### Key Concepts

| Term | Description |
|------|-------------|
| Socket | An endpoint for communication (IP + Port) |
| ServerSocket | Listens for incoming client connections (server side) |
| Socket (client) | Connects to server / accepted connection (client side) |
| Port | Logical channel number (0–65535); well-known ports < 1024 |
| IP Address | Network address of the machine (e.g., `127.0.0.1` = localhost) |
| Protocol | TCP (reliable, ordered) or UDP (fast, connectionless) |

---

## 2. TCP vs UDP

| Feature | TCP | UDP |
|---------|-----|-----|
| Connection | Connection-oriented (3-way handshake) | Connectionless |
| Reliability | Guaranteed delivery, ordering | No guarantee |
| Speed | Slower (overhead) | Faster (no overhead) |
| Use Cases | HTTP, FTP, email, file transfer | Streaming, gaming, DNS |
| Java Classes | `Socket` / `ServerSocket` | `DatagramSocket` / `DatagramPacket` |

---

## 3. TCP Socket — Server & Client

### Server Side

The server creates a `ServerSocket`, waits for connections with `accept()`, then reads/writes using streams.

```java
import java.net.*;
import java.io.*;

ServerSocket serverSocket = new ServerSocket(8080);
Socket clientSocket = serverSocket.accept();    // blocks until client connects

// Read from client
BufferedReader in = new BufferedReader(
    new InputStreamReader(clientSocket.getInputStream()));
String msg = in.readLine();

// Write to client
PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
out.println("Hello from server!");

clientSocket.close();
serverSocket.close();
```

### Client Side

The client creates a `Socket` by specifying the server host and port, then communicates using the same stream approach.

```java
Socket socket = new Socket("localhost", 8080);

PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
out.println("Hello from client!");

BufferedReader in = new BufferedReader(
    new InputStreamReader(socket.getInputStream()));
String response = in.readLine();

socket.close();
```

---

## 4. Multi-Threaded Server

A single-threaded server can only handle one client at a time. Use threads to serve multiple clients concurrently.

```java
ServerSocket serverSocket = new ServerSocket(8080);
while (true) {
    Socket clientSocket = serverSocket.accept();
    new Thread(() -> handleClient(clientSocket)).start();
}
```

The `handleClient()` method processes each client independently, allowing concurrent connections without blocking.

---

## 5. UDP Socket

### UDP Sender

```java
DatagramSocket socket = new DatagramSocket();
byte[] data = "Hello UDP".getBytes();
InetAddress address = InetAddress.getByName("localhost");
DatagramPacket packet = new DatagramPacket(data, data.length, address, 9090);
socket.send(packet);
socket.close();
```

### UDP Receiver

```java
DatagramSocket socket = new DatagramSocket(9090);
byte[] buffer = new byte[1024];
DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
socket.receive(packet);    // blocks until a packet arrives
String msg = new String(packet.getData(), 0, packet.getLength());
socket.close();
```

---

## 6. Important Java Classes & Methods

| Method / Class | Description |
|----------------|-------------|
| `ServerSocket(port)` | Creates a server listening on the given port |
| `accept()` | Blocks and waits for a client connection; returns `Socket` |
| `Socket(host, port)` | Creates a client socket connecting to host:port |
| `getInputStream()` | Returns raw input stream from socket |
| `getOutputStream()` | Returns raw output stream from socket |
| `close()` | Closes the socket and releases resources |
| `setSoTimeout(ms)` | Sets read timeout; throws `SocketTimeoutException` |
| `setReuseAddress(true)` | Allows reuse of port immediately after close |
| `DatagramSocket` | UDP socket class (send/receive packets) |
| `DatagramPacket` | Wraps UDP data, address, and port |

---

## 7. Common Patterns & Best Practices

### Always Use try-with-resources

```java
try (ServerSocket ss = new ServerSocket(8080);
     Socket client = ss.accept()) {
    // handle client
} // auto-closed
```

### Exception Handling

| Exception | Cause |
|-----------|-------|
| `SocketException` | Socket error (closed, reset, refused) |
| `SocketTimeoutException` | `setSoTimeout()` exceeded |
| `UnknownHostException` | Host not resolved |
| `IOException` | General I/O failures on streams |

### Best Practices

- Always close sockets in `finally` or use try-with-resources
- Use `BufferedReader` / `PrintWriter` for text; `DataInputStream` for binary
- Set `SO_TIMEOUT` to avoid infinite blocking reads
- Use thread pools (`ExecutorService`) instead of raw threads for production
- Validate port range: **1024–65535** for user applications
- Use `InetAddress.getByName()` for host resolution

---

## 8. Thread Pool Server (Production Pattern)

```java
ServerSocket serverSocket = new ServerSocket(8080);
ExecutorService pool = Executors.newFixedThreadPool(10);

while (true) {
    Socket client = serverSocket.accept();
    pool.submit(() -> handleClient(client));
}
```

---

## 9. Quick Reference Cheat Sheet

| Task | Code |
|------|------|
| TCP Server | `ServerSocket ss = new ServerSocket(port); Socket c = ss.accept();` |
| TCP Client | `Socket s = new Socket(host, port);` |
| Read line | `new BufferedReader(new InputStreamReader(s.getInputStream()))` |
| Write line | `new PrintWriter(s.getOutputStream(), true).println(msg)` |
| UDP Send | `socket.send(new DatagramPacket(data, len, addr, port))` |
| UDP Receive | `socket.receive(packet)` |
| Set Timeout | `socket.setSoTimeout(3000)` |
| Thread server | `while(true) new Thread(() -> handle(ss.accept())).start()` |
| Pool server | `pool.submit(() -> handle(ss.accept()))` |

---

*— End of Notes —*
