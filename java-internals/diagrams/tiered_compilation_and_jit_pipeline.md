# Architectural Diagram: Tiered Compilation & JIT Optimization Pipeline

```mermaid
flowchart TD
    BYTECODE["Java Bytecode (.class)"] --> TIER0["Tier 0: Template Interpreter"]
    
    subgraph PROFILING["Profiling & Trigger Metrics"]
        INVOCATIONS["Method Invocation Counter"]
        BACKEDGES["Loop Backedge Counter"]
    end
    
    TIER0 <--> PROFILING
    
    PROFILING -->|Invocation + Backedge > CompileThreshold| TIER3["Tier 3: C1 Client Compiler (Full Profile)"]
    PROFILING -->|C2 queue full / quick startup| TIER1["Tier 1: C1 Client Compiler (No Profile)"]
    
    TIER3 -->|Collects MethodDataObjects: branch odds, types| TIER4["Tier 4: C2 Server Compiler (Opto)"]

    subgraph C2_OPTIMIZATIONS["C2 Deep Compiler Optimizations"]
        direction TB
        INLINE["1. Method Inlining<br/>(Monomorphic / Bimorphic Devirtualization)"]
        EA["2. Escape Analysis<br/>(GlobalEscape vs ArgEscape vs NoEscape)"]
        SCALAR["3. Scalar Replacement<br/>(Deconstruct object into CPU registers)"]
        LOCK["4. Lock Elision<br/>(Strip synchronized monitors)"]
        LOOP["5. Loop Unrolling & SIMD Vectorization"]
        
        INLINE --> EA
        EA --> SCALAR
        EA --> LOCK
        SCALAR --> LOOP
    end

    TIER4 --> C2_OPTIMIZATIONS
    C2_OPTIMIZATIONS --> NATIVE["Optimized Machine Code (x86-64 / ARM64 Assembly)"]
    
    NATIVE -->|Assumption Invalidated / Class Loaded| DEOPT["Deoptimization Bailout (Uncommon Trap)"]
    DEOPT -->|Revert stack frame to interpreter (OSR)| TIER0
```
