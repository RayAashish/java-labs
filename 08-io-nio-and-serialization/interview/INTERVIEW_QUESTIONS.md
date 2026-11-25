# 08. Java I/O, NIO, NIO.2 & Serialization — Interview Questions & Answers

---

## 🟢 Beginner Questions

### Q1. Byte Streams vs Character Streams?
- **Byte Streams (`InputStream` / `OutputStream`):** Read/write raw 8-bit bytes (images, audio, PDFs, zip files).
- **Character Streams (`Reader` / `Writer`):** Read/write 16-bit Unicode characters with proper charset encoding/decoding (`.txt`, `.csv`, `.json`).

### Q2. Why should you prefer `java.nio.file.Path` & `Files` over `java.io.File`?
`File.delete()` / `mkdir()` silently return `false` without explaining *why* (permissions vs lock vs missing parent), lacks atomic move/symlink/metadata support, and scales poorly on huge directories (`File.listFiles()` loads all filenames at once). `Files` throws rich `IOException` subclasses (`AccessDeniedException`, `NoSuchFileException`), batches metadata via `BasicFileAttributes`, and supports lazy streaming (`Files.lines`, `Files.newDirectoryStream`).

### Q3. How do you convert an `InputStream` to a `Reader` safely?
Use `new InputStreamReader(inputStream, StandardCharsets.UTF_8)` (an implementation of the GoF **Adapter Pattern**). Always pass `StandardCharsets.UTF_8` explicitly rather than relying on OS default encoding!

### Q4. Why is `BufferedReader` / `BufferedInputStream` dramatically faster than unbuffered streams?
Every unbuffered `FileInputStream.read()` makes an OS kernel **system call (`read(2)`)** crossing User Space <-> Kernel Space (Ring 3 <-> Ring 0). `BufferedInputStream` fetches 8 KB chunks per syscall into memory, cutting 1,000,000 syscalls down to ~125!

---

## 🟡 Intermediate Questions

### Q5. How do you process a 10 GB log file without triggering `OutOfMemoryError`?
Never call `Files.readAllLines(path)` or `Files.readString(path)` (they load the entire file into Heap!). Use **`try (Stream<String> lines = Files.lines(path, StandardCharsets.UTF_8))`** so lines are pulled lazily one at a time and the underlying file descriptor closes deterministically.

### Q6. `Scanner` vs `BufferedReader`?
`Scanner` parses input with heavy Regex engines and tiny 1 KB buffers — ~20x–50x slower and allocates lots of garbage. `BufferedReader` simply reads raw lines using an 8 KB+ char buffer (standard for high-throughput parsing & competitive programming).

### Q7. Explain `Serializable`, `serialVersionUID`, and `transient`.
- `Serializable`: Marker interface allowing `ObjectOutputStream` to serialize object graphs to bytes.
- `serialVersionUID`: Explicit version ID checked on deserialization; omitting it causes JVM auto-computation from class structure, throwing `InvalidClassException` after adding even one method!
- `transient`: Excludes sensitive or non-serializable fields (passwords, `Connection`, `Thread`) from serialization (restored as `null`/`0` unless customized via `readObject`).

### Q8. `Serializable` vs `Externalizable`?
`Serializable` uses JVM reflection automatically (slow, inflexible). `Externalizable` extends `Serializable` and gives you full manual binary control via `writeExternal()` and `readExternal()`, requiring a `public` no-arg constructor.

### Q9. What design patterns appear in `java.io`?
- **Decorator Pattern:** `new BufferedReader(new InputStreamReader(new GZIPInputStream(new FileInputStream("a.gz"))))` dynamically stacks compression, buffering, and charset decoding.
- **Adapter Pattern:** `InputStreamReader` / `OutputStreamWriter` adapt byte streams to character streams.

---

## 🔴 Senior / OS & Kernel Internals Questions

### Q10. Explain the `ByteBuffer` 4-Pointer State Machine (`capacity`, `limit`, `position`, `mark`).
- `capacity`: Total fixed size of buffer.
- `limit`: End index of readable data (after `flip()`) or writable room.
- `position`: Current cursor index.
- `flip()` (Write -> Read): `limit = position; position = 0; mark = -1;`
- `clear()` (Read -> Write): `position = 0; limit = capacity;`
- `compact()`: Copies unread remaining bytes `[position..limit)` to index `0` so you can append new network chunks cleanly!

### Q11. Heap `ByteBuffer` vs Direct (`allocateDirect`) `ByteBuffer`?
- **Heap `ByteBuffer`:** Backed by `byte[]` inside GC Heap. Fast allocation, GC-managed — **but** OS `read`/`write` syscalls cannot pass a movable GC pointer directly to native C/DMA, forcing HotSpot to allocate a temporary off-heap buffer and copy bytes every I/O!
- **Direct `ByteBuffer`:** Allocated **off-heap** via native `malloc`/`Unsafe` outside GC compaction. Expensive to allocate/free (`Cleaner` phantom reference), so **pool & reuse** (Netty `PooledByteBufAllocator`), giving zero-copy OS I/O!

### Q12. How does `FileChannel.transferTo()` achieve OS Zero-Copy (`sendfile(2)`)?
Classic `FileInputStream` -> `Socket.getOutputStream` performs **4 data copies and 4 context switches** (Disk -> Kernel Page Cache -> User Heap Buffer -> Kernel Socket Buffer -> NIC). `FileChannel.transferTo()` delegates to Linux `sendfile(2)` + DMA scatter-gather: bytes flow **Kernel Page Cache -> NIC directly** (zero copies into JVM Heap, 2 context switches!). That is how **Kafka** serves gigabytes/sec with low GC!

### Q13. What is `MappedByteBuffer` (`mmap(2)`), and what is its unmapping trap?
`FileChannel.map(...)` maps a file directly into the process's virtual address space (`mmap(2)`), letting reads/writes happen as raw memory reads/page faults without `read()`/`write()` syscalls. **Trap:** Until Java 22's FFM `MemorySegment`, `MappedByteBuffer` had no deterministic `close()` — files stayed memory-mapped (and undeletable on Windows!) until GC collected the buffer!

### Q14. Why is native Java Deserialization (`ObjectInputStream`) a major security vulnerability?
`ObjectInputStream.readObject()` instantiates arbitrary classes from the byte stream *before* type-casting, allowing attackers to craft **Gadget Chains** achieving Remote Code Execution (RCE)! Never deserialize untrusted bytes; use JSON/Protobuf or configure strict `ObjectInputFilter` (`JEP 290`).

### Q15. Classic Blocking I/O vs NIO `Selector` (`epoll`) vs Java 21+ Virtual Threads?
- **Classic I/O:** 1 OS thread per socket -> breaks at ~3k–5k connections due to 1 MB thread stacks and context switching.
- **NIO `Selector`:** 1 event-loop thread watches 10,000+ non-blocking channels via Linux `epoll(7)` ($O(1)$ ready list). Maximum raw efficiency (Netty), complex callback/state-machine code.
- **Virtual Threads (Project Loom):** Write simple blocking I/O code; when a socket blocks, JVM **unmounts** the tiny virtual thread from its OS carrier thread automatically, giving reactive-grade concurrency with simple imperative stack traces!

---

## 🧩 Tricky Gotchas

1. Forgetting `keyIterator.remove()` in an NIO `Selector` loop -> next `select()` immediately returns the exact same `SelectionKey` in an infinite 100% CPU loop!
2. Calling `files.lines(path)` without `try-with-resources` -> leaks open OS file descriptors (`Too many open files`)!
3. Modifying `static` or `transient` fields during serialization -> `static` fields belong to the Class (not serialized at all!) and `transient` fields restore as `0`/`false`/`null`.
