# 00. Java I/O for Beginners — Reading & Writing Files in 5 Minutes

---

## 🟢 Layer 1 — Start Simple (`Files.writeString` / `Files.readString`)

If you just want to read or write a text file safely in modern Java (11+), you don't need 4 nested stream classes! Use `java.nio.file.Path` and `Files`:

```java
Path file = Path.of("notes.txt");
Files.writeString(file, "Hello Java I/O!\n", StandardCharsets.UTF_8);
String content = Files.readString(file, StandardCharsets.UTF_8);
```

### When to Upgrade Beyond `Files.readString()`
- **Small config (< 1 MB):** `Files.readString(path)` — 1 line, clean, UTF-8 safe.
- **Huge log file (1 GB+):** `try (var lines = Files.lines(path)) { ... }` — reads line-by-line with tiny RAM!
- **Binary file (images, zip, PDF):** `BufferedInputStream` / `BufferedOutputStream` or `Files.copy(...)`.
- **Network / high-throughput servers:** NIO `Channel` & `ByteBuffer` (covered in Phase 3 & 4).
