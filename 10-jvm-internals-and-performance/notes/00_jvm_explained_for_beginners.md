# 00. JVM Explained for Beginners — What Happens When You Press Run?

---

## 🟢 Layer 1 — The 3-Step Journey of a Java Program

When you write `Hello.java` and hit **Run**, Java performs three invisible steps:

```text
Hello.java (Human Code)
   │
   ▼  [1] javac Hello.java
Hello.class (Bytecode — universal instructions, 0xCAFEBABE)
   │
   ▼  [2] ClassLoader loads Hello.class into JVM Memory
   │      • Metaspace: stores the class blueprint
   │      • Heap:      stores every 'new Object()' you create
   │      • Stack:     stores local variables & method calls per thread
   ▼  [3] Execution Engine runs it
          • Interpreter starts fast immediately
          • JIT Compiler turns hot loops into native CPU machine code
          • Garbage Collector automatically cleans dead Heap objects
```

### Why Java Is Called "Write Once, Run Anywhere"
C/C++ compiles directly to Windows `.exe` or macOS ARM64 instructions. Java compiles to **Bytecode** (`.class`), and each OS runs its own **JVM** translator — so the exact same `.class` or `.jar` runs on macOS, Linux Docker containers, and Windows!

### Stack vs. Heap in 30 Seconds
- **Stack:** Like a stack of plates for your current method calls. Fast, tiny, automatically wiped when a method returns. Too deep recursion -> **`StackOverflowError`**.
- **Heap:** The giant warehouse where every `new ArrayList<>()` or `new User()` lives. Shared across threads and cleaned by the **Garbage Collector**. Running out of room -> **`OutOfMemoryError: Java heap space`**.

> Ready for the deep dive? Continue to [`01_jdk_jre_jvm_and_runtime_architecture.md`](01_jdk_jre_jvm_and_runtime_architecture.md) and [`diagrams/jvm_architecture_master_diagram.md`](../diagrams/jvm_architecture_master_diagram.md)!
