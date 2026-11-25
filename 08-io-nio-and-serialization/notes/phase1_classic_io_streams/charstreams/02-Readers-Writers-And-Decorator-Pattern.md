# Readers, Writers, and the Decorator Pattern
> **Author:** raysi  
> **Package:** `com.raysi.ionio.phase1_classic_io_streams.charstreams`  
> **Module:** Character Streams, Unicode UTF-8/UTF-16 Mechanics & Design Patterns

---

# 1. Architectural Purpose & Design Pattern

### What Exact Bottleneck in Previous I/O Approaches Does This Solve?
The foundational `InputStream` and `OutputStream` abstractions operate exclusively on **raw 8-bit binary bytes** (`0x00` through `0xFF`). In the early days of computing, when ASCII reigned (where 1 character = 1 byte = 7 bits), treating bytes as characters caused minimal immediate failure. 

However, global computing requires the **Unicode Standard**, which maps over 149,000 characters across all world scripts, symbols, and emojis. In Unicode:
- ASCII characters occupy **1 byte** in UTF-8 (`'A'` = `0x41`).
- Accented Latin/Greek/Cyrillic occupy **2 bytes** (`'ñ'` = `0xC3 0xB1`).
- Asian scripts (Devanagari, Chinese, Japanese, Korean) occupy **3 bytes** (`'ॐ'` = `0xE0 0xA4 0x90`).
- Emojis and historical scripts occupy **4 bytes** (`'🚀'` = `0xF0 0x9F 0x9A 0x80`).

If a developer attempts to read text using raw `InputStream.read()`, multi-byte characters are sliced into disconnected fragments. Converting each raw byte directly via `(char) inputStream.read()` corrupts text irreversibly into **Mojibake** (garbled characters like `Ã±` or `???`).

Java 1.1 introduced `java.io.Reader` and `java.io.Writer` to decouple **binary transport** from **character decoding/encoding**.

### Design Pattern Analysis: Dual GoF Patterns in Action

Character I/O in the JDK is famous for elegantly coupling two distinct Gang of Four design patterns:
1. **The Adapter Pattern (GoF Structural):**  
   `InputStreamReader` acts as the object adapter bridging two completely incompatible interfaces: `InputStream` (binary source) and `Reader` (character target). Internally, it delegates decoding to `sun.nio.cs.StreamDecoder`, which manages byte-to-char transformation according to a specified `Charset`.
   ```
   [ Client Expects Reader ]  ──► [ InputStreamReader (Adapter) ]  ──► [ Adaptee: InputStream (Bytes) ]
                                            │
                                            ▼
                                  [ StreamDecoder / Charset ]
   ```
2. **The Decorator Pattern (GoF Structural):**  
   Once the stream is adapted into a `Reader`, it is decorated with buffering and line-oriented convenience methods via `BufferedReader`. `BufferedReader` wraps any `Reader` instance and maintains an internal `char[] cb` buffer of 8,192 characters (16 KB in heap).

### ASCII Class Hierarchy
```
java.lang.Object
  ├── java.io.Reader (implements java.lang.Readable, java.io.Closeable)
  │     ├── java.io.InputStreamReader (GoF Adapter: InputStream -> Reader)
  │     │     └── java.io.FileReader (Convenience subclass with platform/explicit charset)
  │     ├── java.io.BufferedReader (GoF Decorator: Adds 8192 char buffer & readLine())
  │     │     └── java.io.LineNumberReader
  │     ├── java.io.CharArrayReader
  │     ├── java.io.StringReader
  │     └── java.io.FilterReader
  │           └── java.io.PushbackReader
  │
  └── java.io.Writer (implements java.lang.Appendable, java.io.Closeable, java.io.Flushable)
        ├── java.io.OutputStreamWriter (GoF Adapter: Writer -> OutputStream)
        │     └── java.io.FileWriter
        ├── java.io.BufferedWriter (GoF Decorator: Adds char buffer & newLine())
        ├── java.io.CharArrayWriter
        ├── java.io.StringWriter
        └── java.io.PrintWriter (Formatting & auto-flush)
```

---

# 2. JVM Memory & OS Kernel Interaction (Under the Hood)

### Trace What Happens in Memory During `BufferedReader.readLine()`
When a thread calls `bufferedReader.readLine()`:
1. **Synchronization on `lock`:** Every `Reader` class contains a `protected Object lock;`. `BufferedReader` synchronizes on `this.lock` (by default `this`) to ensure thread-safe pointer manipulation.
2. **Buffer Inspection:** It inspects its internal character buffer: `char[] cb`.
3. **Buffer Refill via Adapter (`StreamDecoder`):** If `nextChar >= nChars` (buffer exhausted), it invokes `fill()`, which delegates to the underlying `InputStreamReader`.
4. **Byte-to-Char Conversion:** The underlying `StreamDecoder` reads raw bytes from the `InputStream` into an internal native/heap byte buffer (`ByteBuffer`).
5. **Charset Decoding Engine:** The decoder decodes variable-length UTF-8 byte sequences into 16-bit UTF-16 code units (`char`).
6. **Line Termination Scan:** `BufferedReader` scans `cb` starting from `nextChar` for `'\n'` (ASCII `0x0A`) or `'\r'` (ASCII `0x0D`).
7. **String Allocation:** Once the delimiter is reached, it constructs a new `java.lang.String` from the character sequence and advances `nextChar` past the newline.

### ASCII Memory Flow Diagram
```
+-----------------------------------------------------------------------------------+
| PHYSICAL HARDWARE / DISK STORAGE                                                  |
|  File on Disk encoded in UTF-8 (1 to 4 bytes per character)                       |
+------------------------------------------┬----------------------------------------+
                                           │
                                           ▼
+-----------------------------------------------------------------------------------+
| LINUX KERNEL SPACE (Ring 0)                                                       |
|  Page Cache: Raw Byte Buffers (e.g. 0xF0 0x9F 0x9A 0x80 for 🚀)                   |
+------------------------------------------┬----------------------------------------+
                                           │  read(fd, byte_buf, len) syscall
                                           ▼
+-----------------------------------------------------------------------------------+
| USER SPACE: JVM HEAP (Ring 3)                                                     |
|                                                                                   |
|  [ FileInputStream ]                                                              |
|        │ (Raw bytes)                                                              |
|        ▼                                                                          |
|  [ InputStreamReader / StreamDecoder ]                                            |
|        │ Consumes 4 bytes: 0xF0 0x9F 0x9A 0x80                                    |
|        │ Decodes via CharsetDecoder (UTF-8 -> UTF-16)                              |
|        │ Produces 2 Java 'char' primitives (Surrogate Pair):                       |
|        │    High Surrogate: '\uD83D'                                              |
|        │    Low Surrogate:  '\uDE80'                                              |
|        ▼                                                                          |
|  [ BufferedReader ]                                                               |
|        │ Stores chars into internal buffer:                                       |
|        ├── char[] cb = [ '\uD83D', '\uDE80', '\n', ... ] (Size: 8,192 chars / 16KB)|
|        ├── int nextChar = 0                                                       |
|        └── int nChars   = 2                                                       |
|        ▼                                                                          |
|  [ Application Thread ]                                                           |
|        └── String line = bufferedReader.readLine(); // "🚀"                       |
+-----------------------------------------------------------------------------------+
```

### Exact Internal JDK Fields (`java.io.BufferedReader`)
Directly from OpenJDK source code:
```java
public class BufferedReader extends Reader {
    private Reader in;

    // Internal character buffer (16-bit UTF-16 code units)
    private char cb[];
    private int nChars;
    private int nextChar;

    private static final int INVALIDATED = -2;
    private static final int UNMARKED = -1;
    private int markedChar = UNMARKED;
    private int readAheadLimit = 0;

    private static int defaultCharBufferSize = 8192; // 8,192 chars = 16,384 bytes in RAM
    private static int defaultExpectedLineLength = 80;
}
```

---

# 3. Step-by-Step State Transitions & Mechanics

### Variable-Length UTF-8 Decoding Mechanics
UTF-8 uses a prefix-coding scheme:
- **1 Byte (ASCII 0..127):** Binary `0xxxxxxx`
- **2 Bytes (U+0080..U+07FF):** Binary `110xxxxx 10xxxxxx`
- **3 Bytes (U+0800..U+FFFF):** Binary `1110xxxx 10xxxxxx 10xxxxxx`
- **4 Bytes (U+10000..U+10FFFF):** Binary `11110xxx 10xxxxxx 10xxxxxx 10xxxxxx`

When `StreamDecoder` encounters a leading byte:
1. It inspects high-order bits: `0b11110000` indicates a 4-byte sequence.
2. It fetches 3 subsequent bytes, validating that each begins with `10xxxxxx`.
3. If valid, it computes the 21-bit Unicode scalar value (Code Point):
   $$\text{Code Point} = ((b_0 \ \& \ \text{0x07}) \ll 18) \ | \ ((b_1 \ \& \ \text{0x3F}) \ll 12) \ | \ ((b_2 \ \& \ \text{0x3F}) \ll 6) \ | \ (b_3 \ \& \ \text{0x3F})$$
4. Since Java's `char` is strictly 16-bit ($2^{16} = 65,536$ maximum values), it cannot store code points $> \text{0xFFFF}$ in a single `char`.
5. It decomposes the code point into a **UTF-16 Surrogate Pair**:
   $$\text{High Surrogate} = \text{0xD800} + ((\text{CodePoint} - \text{0x10000}) \gg 10)$$
   $$\text{Low Surrogate} = \text{0xDC00} + ((\text{CodePoint} - \text{0x10000}) \ \& \ \text{0x3FF})$$
6. It writes two `char` values into `cb[]`. Hence, `String.length()` on `"🚀"` returns `2`, while `codePointCount` returns `1`.

### Delimiter Scanning and Memory Overhead in `readLine()`
`BufferedReader.readLine()` scans for three possible line terminators:
1. `\n` (Unix / Linux / macOS modern)
2. `\r\n` (Windows CRLF)
3. `\r` (Classic Mac OS 9)

**Performance Danger:**  
`readLine()` allocates a new `String` object on the JVM Heap for every single line read. For a 10 GB file containing 50 million lines, naive line iteration instantiates 50,000,000 `String` instances and their underlying `byte[]` arrays, producing severe Young Generation GC churn.

---

# 4. Performance & Trade-Off Matrix

| Feature / Metric | `java.util.Scanner` | Unbuffered `InputStreamReader` | `BufferedReader` (Default 8K) | Custom Byte-Level `FastScanner` |
| :--- | :--- | :--- | :--- | :--- |
| **Parsing 500,000 Ints** | ~950 ms (Extremely slow) | ~450 ms | ~140 ms | **~25 ms (38x faster)** |
| **Internal Memory Buffer** | 1,024 chars | None (reads small blocks) | 8,192 chars (16 KB) | 32,768 bytes (32 KB) |
| **Object Allocation Rate** | Massive (Regex patterns, Strings) | Low | Moderate (`String` per line) | **Zero (Primitive parsing)** |
| **Thread-Safety** | Not thread-safe | Synchronized on `lock` | Synchronized on `lock` | Unsynchronized (Single-thread optimal) |
| **Parsing Engine** | Regular Expression Engine | Manual char extraction | String parsing (`Integer.parseInt`) | Direct ASCII digit bit arithmetic |
| **Best Production Use Case** | Quick console input scripts | Legacy adapter wiring | Standard production text processing | Competitive programming, HFT batch logs |

---

# 5. Production-Grade Java Verification Code

Executable implementations provided in this module:
1. [`InputStreamReaderBridgeDemo.java`](../../../codes/ionio/phase1_classic_io_streams/charstreams/InputStreamReaderBridgeDemo.java): Verifies the Adapter Pattern, UTF-8 variable byte decoding (1 to 4 bytes), surrogate pair formation, and Mojibake corruption.
2. [`BufferedReaderFastCPInputTemplate.java`](../../../codes/ionio/phase1_classic_io_streams/charstreams/BufferedReaderFastCPInputTemplate.java): Implements a high-throughput competitive programming reader and runs an empirical benchmark against `Scanner` and `BufferedReader`.

### Empirical Benchmark Output:
```text
==========================================================================
>>> BENCHMARK: Scanner vs BufferedReader vs Byte-Level FastScanner <<<
>>> Author: raysi | Parsing 500000 integers
==========================================================================

[Step 1] Dataset generated: 3418 KB in RAM.

[Step 2] Benchmarking java.util.Scanner...
   -> Scanner finished: 874 ms | Checksum: 249750000

[Step 3] Benchmarking BufferedReader + StringTokenizer...
   -> BufferedReader+Tokenizer finished: 138 ms | Checksum: 249750000

[Step 4] Benchmarking Byte-Level FastScanner (Zero Object Allocation)...
   -> Byte-Level FastScanner finished: 23 ms | Checksum: 249750000

==========================================================================
>>> PERFORMANCE SUMMARY <<<
==========================================================================
1. Scanner:                    874 ms  (Baseline - Slowest)
2. BufferedReader+Tokenizer:   138 ms  (Speedup: 6.33x)
3. Byte-Level FastScanner:      23 ms  (Speedup: 38.00x over Scanner, 6.00x over BR)
--------------------------------------------------------------------------
Why Scanner is slow: It evaluates regular expressions for every token delimiter,
instantiates String objects per token, and synchronizes internally.
FastScanner reads a 32KB byte[] buffer and parses integer values via bit shifts!
==========================================================================
```

---

# 6. Top 7 High-Signal Interview Questions & Deep Answers

### Q1: Why is `new FileReader("file.txt")` historically considered dangerous prior to Java 11?
**Answer:**  
Prior to Java 11, the `FileReader` constructor did not accept a `Charset` argument. It implicitly hardcoded `Charset.defaultCharset()`, which was dictated by the host operating system's environment variables (`file.encoding`, `LC_ALL`, or Windows code page like `windows-1252`).  
If a developer wrote a UTF-8 file on macOS and opened it on Windows via `new FileReader()`, the JVM silently decoded UTF-8 bytes using Windows-1252, corrupting all non-ASCII data.  
Java 11 finally introduced `new FileReader("file.txt", StandardCharsets.UTF_8)`. In Java 18 (JEP 400), UTF-8 was finally designated as the default charset across all platforms.

### Q2: How does `InputStreamReader` handle incomplete multi-byte sequences at the boundary of a buffer?
**Answer:**  
If a multi-byte character (such as a 4-byte emoji) is split across the boundary of an internal byte chunk (e.g., the first 2 bytes arrive at the end of buffer $N$, and the remaining 2 bytes are in buffer $N+1$), `StreamDecoder` does not fail or throw an exception.  
The underlying `CharsetDecoder` maintains internal state across invocations. It detects that the byte sequence is underflowed (`CoderResult.UNDERFLOW`), buffers the partial prefix bytes internally, reads the next chunk from the `InputStream`, concatenates the trailing bytes, and successfully yields the decoded character.

### Q3: What is the exact Gang of Four design pattern difference between `InputStreamReader` and `BufferedReader`?
**Answer:**  
- **`InputStreamReader` is an Adapter Pattern:** Its intent is **interface conversion**. It bridges two incompatible types (`InputStream` which outputs 8-bit bytes to `Reader` which outputs 16-bit characters).
- **`BufferedReader` is a Decorator Pattern:** Its intent is **behavioral augmentation without changing the interface**. It accepts a `Reader` and returns a `Reader`, decorating it with in-memory buffering (`char[] cb`) and additional utility methods (`readLine()`).

### Q4: Why is `java.util.Scanner` significantly slower than `BufferedReader`?
**Answer:**  
1. **Regular Expressions:** `Scanner` relies on `java.util.regex.Pattern` to delimit tokens. Every call to `hasNext()` or `next()` runs a DFA/NFA regex match against the buffer.
2. **Small Buffer:** `Scanner` uses an internal buffer of only 1,024 characters (vs 8,192 in `BufferedReader`).
3. **Excessive Object Allocations:** `Scanner` creates temporary `Matcher`, `String`, and wrapper objects on nearly every token scan, flooding the Young Gen heap.
4. **Synchronization:** `Scanner` contains synchronized blocks internally.

### Q5: Why can `String.length()` differ from the actual number of visible characters in a text file?
**Answer:**  
Java's `String.length()` returns the number of **16-bit UTF-16 code units** (`char` primitives), **not** the number of Unicode code points or grapheme clusters.  
Any Unicode character outside the Basic Multilingual Plane (BMP, code points $> \text{0xFFFF}$, such as emojis, mathematical alphanumerics, and rare CJK ideographs) is encoded as a **surrogate pair** (two consecutive 16-bit `char`s).  
For `"🚀"`, `length()` is `2`, but `codePointCount(0, 2)` is `1`. For grapheme clusters with zero-width joiners (e.g., family or flag emojis), `length()` can be 8 or more for a single visible glyph.

### Q6: What is the purpose of the `protected Object lock` field in `java.io.Reader`?
**Answer:**  
The `Reader` abstract class was designed in Java 1.1 with built-in thread safety for character streaming.  
Instead of synchronizing on `this` (which exposes the object monitor to external lock contention or accidental deadlocks if external code synchronizes on the stream instance), `Reader` delegates thread synchronization to a dedicated internal lock: `protected Object lock;`.  
Subclasses like `BufferedReader` synchronize on `synchronized (lock)` to protect internal pointers (`nextChar`, `nChars`).

### Q7: What is the performance danger of using `BufferedReader.lines()` without closing the stream?
**Answer:**  
`BufferedReader.lines()` returns a lazy `Stream<String>`. Under the hood, this Stream is backed by an internal Spliterator that holds an open reference to the underlying file descriptor.  
Because Java Streams are not automatically closed upon termination of terminal operations (like `.forEach()` or `.collect()`), failing to wrap `lines()` in a `try-with-resources` block leaves the operating system file descriptor open until the finalizer runs (if at all). In high-throughput servers, this causes **Operating System File Descriptor Exhaustion** (`java.io.IOException: Too many open files`).
