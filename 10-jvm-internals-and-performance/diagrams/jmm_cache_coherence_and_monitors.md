# Architectural Diagram: JMM, Cache Coherence & Monitor Inflation

## 1. Hardware False Sharing & 64-Byte Cache Lines

```
  PHYSICAL 64-BYTE L1 CPU CACHE LINE:
  +--------------------+--------------------+---------------------------------------------+
  | volatile long a    | volatile long b    | Remaining 48 bytes of adjacent memory       |
  | (8 bytes)          | (8 bytes)          |                                             |
  +--------------------+--------------------+---------------------------------------------+
            ^                    ^
            |                    |
        CPU Core 1           CPU Core 2
        (Thread A)           (Thread B)
            |                    |
            v                    v
  Writing 'a' invalidates the WHOLE 64-byte line on Core 2!
  Writing 'b' invalidates the WHOLE 64-byte line on Core 1!
  = Massive hardware bus contention and false sharing slowdown!
```

---

## 2. Monitor Locking & Inflation State Machine

```mermaid
stateDiagram-v2
    [*] --> Unlocked: Object created (Mark Word bits: 01)
    
    Unlocked --> LightweightLocked: Single thread enters synchronized block (CAS displaced mark word to thread stack) (Bits: 00)
    
    LightweightLocked --> Unlocked: Thread exits synchronized block (CAS restore mark word)
    
    LightweightLocked --> HeavyweightInflated: Thread 2 contends while held!<br/>CAS fails -> Spin fails -> Inflate ObjectMonitor (Bits: 10)
    
    state HeavyweightInflated {
        _owner: Current Thread Pointer
        _cxq: Lock-free LIFO waiting stack
        _EntryList: Unblocked threads ready to acquire
        _WaitSet: Threads waiting on wait()
    }
    
    HeavyweightInflated --> Unlocked: Heavyweight owner releases and awakens threads via OS mutex
```

---

## 3. Virtual Thread Lifecycle (Project Loom Continuation)

```mermaid
sequenceDiagram
    autonumber
    participant VT as Virtual Thread
    participant Cont as Continuation
    participant Carrier as Carrier Thread (OS Thread)
    participant Kernel as OS Kernel / I/O Epoll

    VT->>Carrier: Running business task
    VT->>Kernel: Perform blocking Socket Read
    Kernel-->>VT: EAGAIN / Will Block
    VT->>Cont: yield()
    Cont->>Cont: Copy execution stack frames to Heap
    Cont->>Carrier: Unmount
    Note over Carrier: Carrier thread runs other Virtual Threads!
    Kernel-->>Cont: Socket I/O Ready (OS epoll event)
    Cont->>Carrier: Submit continuation to ForkJoinPool
    Carrier->>Cont: Re-mount execution frames to Carrier stack
    Cont->>VT: Resume execution transparently
```
