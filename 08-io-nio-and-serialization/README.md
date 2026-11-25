# 08. Java I/O, NIO, NIO.2 & Serialization

> **Learning Method:** Start at **🟢 Noob Track** (reading/writing files with `Path` & `Files`), progress through **🟡 Practitioner Track** (Decorator streams, `BufferedReader`, UTF-8 charsets, serialization), and master **🔴 Senior Track** (OS syscalls, Direct Off-Heap `ByteBuffer`, `sendfile(2)` zero-copy `transferTo()`, `mmap(2)`, and `Selector` event loops).

---

## ⚡ 2-Minute Rapid Revision Card

- **Never use `java.io.File` in modern code:** Returns silent `false` on delete failure. Use **`java.nio.file.Path` + `Files`** (NIO.2).
- **Large Files (10GB+):** Never call `Files.readAllLines()` (loads everything into Heap -> `OOM`!). Always stream lazily with **`try (Stream<String> lines = Files.lines(path))`**.
- **Byte vs Character Streams:** `InputStream`/`OutputStream` read raw bytes (images, PDFs); `Reader`/`Writer` decode UTF-16 chars. Bridge them via `new InputStreamReader(in, StandardCharsets.UTF_8)` (Adapter Pattern).
- **Buffer Rule:** Unbuffered `FileInputStream.read()` makes 1 OS syscall per byte! Wrapping with `BufferedInputStream` (Decorator Pattern) slashes syscalls by 99.98%.
- **NIO Buffer 4-Pointer State Machine:** `capacity >= limit >= position >= mark >= 0`. Call `.flip()` to switch Write -> Read (`limit = position; position = 0`), `.clear()` to reset for Write.
- **Zero-Copy & Memory Mapping:** `FileChannel.transferTo()` uses OS `sendfile(2)` + DMA without copying bytes through JVM Heap; `MappedByteBuffer` maps file pages directly via `mmap(2)`.
- **Serialization Warning:** Native Java `ObjectInputStream` is an RCE security hazard on untrusted bytes! Prefer JSON/Protobuf; use `transient` + `serialVersionUID` when `Serializable` is required.

---

## 📂 Contents

| Phase | Level | Guide & Labs |
|---|---|---|
| **Phase 0 (Start Here)** | 🟢 Noob | [`notes/00-beginner-io-quickstart.md`](notes/00-beginner-io-quickstart.md) + `ionio.phase0_beginner.BeginnerFileReadWriteDemo` |
| **Phase 1** | 🟡/🔴 | Classic I/O Streams, `InputStreamReader` Bridge & Serialization ([`notes/phase1_classic_io_streams/`](notes/phase1_classic_io_streams/)) |
| **Phase 2** | 🟡/🔴 | Modern NIO.2 `Path`, `Files.lines`, `WatchService` ([`notes/phase2_modern_nio2_files/`](notes/phase2_modern_nio2_files/)) |
| **Phase 3** | 🔴 Senior | `ByteBuffer` Direct vs Heap, `transferTo()` Zero-Copy, `MappedByteBuffer` ([`notes/phase3_nio_buffers_channels/`](notes/phase3_nio_buffers_channels/)) |
| **Phase 4** | 🔴 Senior | Non-Blocking `Selector` Event Loops & Virtual Threads ([`notes/phase4_nonblocking_network/`](notes/phase4_nonblocking_network/)) |
| **Interview** | 🎯 All | [`interview/INTERVIEW_QUESTIONS.md`](interview/INTERVIEW_QUESTIONS.md) |

---

## 🚀 Compile & Run

```bash
javac -d out $(find 08-io-nio-and-serialization/codes -name "*.java")
java -cp out ionio.phase0_beginner.BeginnerFileReadWriteDemo
java -cp out ionio.phase1_classic_io_streams.bytestreams.UnbufferedVsBufferedCopyBenchmark
java -cp out ionio.phase2_modern_nio2_files.LazyStreamLargeFileLinesDemo
java -cp out ionio.phase3_nio_buffers_channels.buffers.ByteBufferPositionLimitCapacityDemo
java -cp out ionio.phase3_nio_buffers_channels.channels_and_zerocopy.FileChannelTransferToZeroCopyDemo
```
