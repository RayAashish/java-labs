# 01: Java NIO Channels, Selectors & The Reactor Event Loop
**Author:** `raysi`  
**Package:** `com.raysi.sockets.phase3_nio_sockets`  
**Topic:** Non-Blocking I/O, Multiplexing, ByteBuffers & Event Loop Architecture

---

# 1. Why NIO (Non-Blocking I/O)? The Paradigm Shift

Traditional `java.io` requires allocating a dedicated operating system thread per socket connection. When thousands of connections sit idle waiting for client input, gigabytes of RAM are wasted on thread stacks, and the CPU wastes cycles on context switching.

**Java NIO (`java.nio.channels`)** introduces multiplexed, non-blocking I/O. A **single thread** can monitor thousands of active sockets simultaneously, waking up only when an OS event (data arrival, connection request) occurs.

```text
       Traditional BIO (Thread-per-Connection)              Modern NIO (Multiplexed Event Loop)
       
Client 1 ---> [ OS Thread 1 (Blocked) ]            Client 1 ----+
Client 2 ---> [ OS Thread 2 (Blocked) ]            Client 2 ----+---> [ Single Selector ] ---> [ 1 Worker Thread ]
Client 3 ---> [ OS Thread 3 (Blocked) ]            Client 3 ----+       (epoll / kqueue)
Client N ---> [ OS Thread N (Blocked) ]            Client N ----+
```

---

# 2. The Three Pillars of Java NIO

### 1. `Channel` (Bidirectional Pipes)
Unlike unidirectional `InputStream` and `OutputStream`, a `Channel` is bidirectional—it can both read and write asynchronously.
* `ServerSocketChannel`: Listens for incoming TCP connections in non-blocking mode.
* `SocketChannel`: TCP socket channel supporting non-blocking data exchange.
* Channels can only register with a Selector when set to `configureBlocking(false)`.

### 2. `Buffer` (In-Memory Data Containers)
Channels read data **into** a buffer and write data **from** a buffer.
A `ByteBuffer` maintains three fundamental pointers:
* **`capacity`**: Maximum number of bytes the buffer can hold.
* **`position`**: Next index to be read from or written to.
* **`limit`**: First index that must NOT be read or written.

```text
Initial allocate(10):  [  0  |  1  |  2  |  3  |  4  |  5  |  6  |  7  |  8  |  9  ]
                       ^                                                           ^
                       position = 0                                       limit = capacity = 10

After put(4 bytes):    [ 'H' | 'E' | 'L' | 'L' |  4  |  5  |  6  |  7  |  8  |  9  ]
                                               ^                                   ^
                                               position = 4               limit = capacity = 10

After flip():          [ 'H' | 'E' | 'L' | 'L' |  4  |  5  |  6  |  7  |  8  |  9  ]
                       ^                       ^
                       position = 0            limit = 4 (ready to read out!)
```

* **`flip()`**: Sets `limit = position` and `position = 0`. Switches buffer from writing to reading mode.
* **`clear()`**: Sets `position = 0` and `limit = capacity`. Resets buffer to overwrite existing data.
* **`compact()`**: Copies unread bytes to the beginning and sets `position` after them, allowing new writes without discarding unread data.

### 3. `Selector` (OS Event Multiplexer)
The `Selector` acts as the event multiplexer. Under the hood, Java NIO delegates to the operating system's fastest kernel notification subsystem:
* **Linux:** `epoll` (`epoll_create`, `epoll_ctl`, `epoll_wait`)
* **macOS / BSD:** `kqueue`
* **Windows:** `IOCP` (I/O Completion Ports)

Channels register their interest with the Selector via bitwise flags:
* `SelectionKey.OP_ACCEPT` (16): Server socket is ready to accept a new connection.
* `SelectionKey.OP_CONNECT` (8): Client socket has completed handshake with server.
* `SelectionKey.OP_READ` (1): Channel has bytes ready to be read.
* `SelectionKey.OP_WRITE` (4): Channel socket buffer has space available to write bytes.

---

# 3. The Reactor Pattern: Single vs Multi-Threaded

Java NIO architectures typically implement the **Reactor Design Pattern**:

### Multi-Reactor (Boss-Worker) Model (Netty Architecture):
```text
[ Incoming Connections ] 
          |
          v
+-------------------+
| Boss Reactor Loop | ---> Accepts SocketChannels, registers with Worker Selectors
+-------------------+
          |
          +-----------------------+-----------------------+
          |                       |                       |
          v                       v                       v
+--------------------+  +--------------------+  +--------------------+
| Worker Reactor #1  |  | Worker Reactor #2  |  | Worker Reactor #3  |
| (epoll event loop) |  | (epoll event loop) |  | (epoll event loop) |
+--------------------+  +--------------------+  +--------------------+
          |                       |                       |
          v                       v                       v
[ Business Logic Pool]  [ Business Logic Pool]  [ Business Logic Pool]
```
1. **Boss Group (Acceptors):** Runs a dedicated event loop accepting new TCP connections and immediately hands the accepted `SocketChannel` off to a Worker.
2. **Worker Group (I/O Multiplexers):** Each worker thread runs its own `Selector` loop handling `OP_READ` and `OP_WRITE` events for a shard of thousands of sockets.
3. **Execution Pipeline:** CPU-intensive business logic is dispatched to a separate thread pool so the Reactor I/O event loop is never blocked.

---

# 4. The Infamous Java NIO Epoll Bug (JDK-6670302)

* **The Problem:** In Linux, a hardware interrupt or closed socket can trigger `epoll_wait()` to return with zero ready events. In older JDKs, the JVM entered an infinite busy-spin loop, causing a single thread to pin CPU core utilization to **100%**.
* **The Netty Solution:** Netty tracks consecutive zero-ready `selector.select()` calls. If the count exceeds a threshold (e.g., 512 times), Netty reconstructs a brand-new `Selector`, migrates all active channel registrations over, and destroys the corrupted old selector.
