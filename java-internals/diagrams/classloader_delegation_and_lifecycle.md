# Architectural Diagram: ClassLoader Lifecycle & Delegation Subsystem

```mermaid
sequenceDiagram
    autonumber
    participant App as Application Code
    participant Custom as CustomClassLoader
    participant AppLoader as Application / System Loader
    participant PlatLoader as Platform Loader
    participant BootLoader as Bootstrap Loader (C++ libjvm)

    App->>Custom: loadClass("com.example.OrderService")
    
    rect rgb(240, 248, 255)
    Note over Custom,BootLoader: PHASE 1: UPWARD DELEGATION (Search Cache & Delegate)
    Custom->>Custom: findLoadedClass() -> Not Cached
    Custom->>AppLoader: Delegate loadClass()
    AppLoader->>AppLoader: findLoadedClass() -> Not Cached
    AppLoader->>PlatLoader: Delegate loadClass()
    PlatLoader->>PlatLoader: findLoadedClass() -> Not Cached
    PlatLoader->>BootLoader: Delegate loadClass()
    BootLoader->>BootLoader: findLoadedClass() -> Not Cached
    end

    rect rgb(255, 250, 240)
    Note over BootLoader,Custom: PHASE 2: DOWNWARD ATTEMPT TO FIND & LOAD
    BootLoader->>BootLoader: findClass() in java.base -> NOT FOUND
    BootLoader-->>PlatLoader: ClassNotFoundException
    PlatLoader->>PlatLoader: findClass() in JDK modules -> NOT FOUND
    PlatLoader-->>AppLoader: ClassNotFoundException
    AppLoader->>AppLoader: findClass() in CLASSPATH -> FOUND!
    AppLoader->>AppLoader: Read .class bytes into Metaspace
    AppLoader->>AppLoader: Define java.lang.Class<?> mirror instance
    AppLoader-->>Custom: Return loaded Class<?>
    Custom-->>App: Return Class<?> reference
    end
```

---

## The Verification, Preparation & Initialization State Transitions

```mermaid
stateDiagram-v2
    [*] --> Raw_Bytes: Read from disk / network (0xCAFEBABE)
    
    state Linking {
        Raw_Bytes --> Verified: Pass 1-4 (Format, Semantics, StackMapTable, Symbols)
        Verified --> Prepared: Allocate static fields with zero defaults (0, null)
        Prepared --> Resolved: Replace constant pool symbols with direct memory pointers
    }
    
    Resolved --> Initialized: Execute &lt;clinit&gt; static blocks under VM lock
    Initialized --> Instantiated: Execute new + &lt;init&gt; constructor on Heap
    Instantiated --> [*]
```
