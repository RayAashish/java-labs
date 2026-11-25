# 02: Blocking Streams, Connection Lifecycle & The C10K Bottleneck
**Author:** `raysi`  
**Package:** `com.raysi.sockets.phase1_tcp_sockets`  
**Topic:** Java I/O Streams, Socket Lifecycle States & Concurrency Limits

---

# 1. Blocking I/O (BIO) Stream Mechanics

Java's classic network stack (`java.io`) operates on **Blocking Streams**:
```java
InputStream in = socket.getInputStream();
OutputStream out = socket.getOutputStream();
```

### What Happens When You Call `in.read()`:
1. The user thread issues a `read()` system call into the OS kernel.
2. If the OS socket receive buffer (`rmem`) is empty, the operating system transitions the thread state from `RUNNABLE` to `WAITING` (or `BLOCKED` on I/O).
3. The calling thread is suspended, descheduled by the CPU scheduler, and parked.
4. When network packets arrive, the Network Interface Card (NIC) raises a hardware interrupt, the OS kernel TCP stack copies the bytes into the socket buffer, and the OS wakes the parked thread.
5. The thread resumes, copies the data from kernel space to JVM heap space, and `read()` returns.

```text
User Thread               JVM Heap                       OS Kernel (TCP Stack)
    |                        |                                      |
    | --- socket.read() ---> |                                      |
    | (Blocks)               | ---- sys_read system call ---------> |
    |                        |                                      | Socket buffer empty...
    |                        |                                      | Thread suspended
    |                        |                                      | Packet arrives from NIC!
    |                        |                                      | Copy to kernel buffer
    |                        | <--- Kernel copies to JVM buffer --- | Wake up thread
    | <--- Returns bytes --- |                                      |
```

---

# 2. Socket Lifecycle State Machine

A socket transitions through explicit states during its lifetime:

```text
    +-------------+
    | Unconnected |  new Socket()
    +------+------+
           |
           | socket.connect(endpoint)
           v
    +-------------+
    |  Connected  |  TCP 3-way handshake successful; getInputStream()/getOutputStream() valid
    +------+------+
           |
     +-----+-----+
     |           |
     v           v
+---------+ +----------+
| Input   | |  Output  | socket.shutdownInput() / socket.shutdownOutput()
| Closed  | |  Closed  | (Half-closed connection: sends TCP FIN in one direction)
+----+----+ +----+-----+
     |           |
     +-----+-----+
           |
           | socket.close()
           v
    +-------------+
    |   Closed    |  TCP 4-way teardown completed; all streams closed
    +-------------+
```

### Half-Closed Sockets:
* Calling `socket.shutdownOutput()` transmits a TCP `FIN` packet to the remote peer, notifying them that no more data will be sent, **while keeping the input stream open** to read the peer's final response (used in protocols like HTTP/1.0 and file upload acknowledgments).
* Calling `socket.close()` terminates both directions immediately and releases the underlying OS file descriptor.

---

# 3. The C10K Bottleneck of Classic Thread-Per-Connection BIO

In classic Java TCP programming, serving $N$ concurrent clients requires allocating $N$ threads:

$$\text{Memory Overhead} = N \times \text{Thread Stack Size (-Xss, default 1MB)}$$

* 1,000 concurrent clients $\approx 1{,}000\text{ MB} = 1\text{ GB}$ of RAM.
* 10,000 concurrent clients $\approx 10\text{ GB}$ of RAM just for thread stacks!
* **CPU Thrashing:** As the thread count exceeds the number of physical CPU cores, the OS kernel spends more time performing CPU register saves/restores and cache invalidation (**context switching overhead**) than executing application code.

### The Modern Java 21 Virtual Threads Paradigm
In Java 21+, Virtual Threads (`Executors.newVirtualThreadPerTaskExecutor()`) alleviate the OS thread ceiling. When a virtual thread blocks on `socket.getInputStream().read()`, the JVM unmounts the virtual thread from its underlying OS carrier thread, allowing a single JVM to sustain 1,000,000 concurrent blocking sockets without native memory exhaustion.
