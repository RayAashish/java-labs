# Java HashMap Code Implementations & Demonstrations

This folder contains a complete, production-grade custom implementation of `HashMap` from scratch, along with interactive demonstrations illustrating every critical internal behavior and interview concept.

---

## 📁 Directory Structure

```
codes/
├── custom/
│   ├── MyMap.java                  # Generic interface mirroring java.util.Map
│   ├── MyHashMap.java              # Complete HashMap implementation from scratch
│   └── MyHashMapTest.java          # Comprehensive test suite with assertions
│
└── demo/
    ├── InternalMechanicsDemo.java  # Perturbation function, bitwise math, high-bit test
    ├── HashCodeEqualsContractDemo.java # Contract violations & mutable key hazard demo
    ├── ConcurrencyIssueDemo.java   # Multi-threaded race conditions vs ConcurrentHashMap
    └── MapImplementationsComparisonDemo.java # HashMap vs LinkedHashMap (LRU) vs TreeMap vs IdentityHashMap
```

---

## 🛠️ How to Compile and Run

Make sure you are in the `hashmap_indetails` directory:

```bash
cd /Users/aashishkumarray/Desktop/java-labs/hashmap_indetails
```

### 1. Compile All Files
```bash
javac codes/custom/*.java codes/demo/*.java
```

### 2. Run the Custom HashMap Test Suite
```bash
java -cp codes custom.MyHashMapTest
```
*Validates basic CRUD, null keys/values, collision chaining, dynamic resizing with high-bit split rehashing, node unlinking, and collection views.*

### 3. Run Internal Mechanics & Bitwise Math Demo
```bash
java -cp codes demo.InternalMechanicsDemo
```
*Displays 32-bit binary representations of raw hashCode, shifted bits, XOR perturbation, index masking, and the Java 8 high-bit resize test.*

### 4. Run Contract Violations & Mutable Key Demo
```bash
java -cp codes demo.HashCodeEqualsContractDemo
```
*Demonstrates what happens when equals() is overridden without hashCode(), when hashCode() is overridden without equals(), and the disastrous silent memory leak when a key is mutated after insertion.*

### 5. Run Concurrency Pitfalls Demo
```bash
java -cp codes demo.ConcurrencyIssueDemo
```
*Executes 10 concurrent threads inserting 1,000 entries each (10,000 total) into HashMap vs ConcurrentHashMap, showcasing race conditions and data loss.*

### 6. Run Map Implementations Comparison Demo
```bash
java -cp codes demo.MapImplementationsComparisonDemo
```
*Demonstrates iteration ordering across HashMap (unordered), LinkedHashMap (insertion order), LinkedHashMap with access-order (LRU Cache with auto-eviction), TreeMap (sorted Red-Black tree), and IdentityHashMap (== reference equality).*
