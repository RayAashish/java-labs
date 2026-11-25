# 04. The hashCode() & equals() Contract and Immutability

## 1. The Java `Object` Contract

The contract between `equals(Object obj)` and `hashCode()` is defined in the specification of `java.lang.Object`:

```
                       +-------------------------------+
                       |   If o1.equals(o2) is TRUE    |
                       +-------------------------------+
                                       |
                                       v  (MUST BE TRUE)
                       +-------------------------------+
                       |  o1.hashCode() == o2.hashCode()|
                       +-------------------------------+

                                     vs.

                       +-------------------------------+
                       |  o1.hashCode() == o2.hashCode()|
                       +-------------------------------+
                                       |
                                       v  (NOT NECESSARILY TRUE)
                       +-------------------------------+
                       |   If o1.equals(o2) is TRUE    |
                       |       (Hash Collision)        |
                       +-------------------------------+
```

### The Three Rules of the Contract:
1. **Consistency**: Multiple invocations of `hashCode()` on the same object must consistently return the same integer, provided no information used in `equals` comparisons has been modified.
2. **Equality Implies Equal Hash**: If two objects are equal according to `equals(Object)`, calling `hashCode()` on each must produce the **exact same integer**.
3. **Collision Allowance**: If two objects produce the same hash code, they are **not necessarily equal**. (This is a hash collision).

---

## 2. Breaking the Contract: The 4 Behavioral Scenarios

### Scenario A: Overriding `equals()` but NOT `hashCode()` (Catastrophic)

Suppose you define a `Student` class that compares `id`:

```java
class Student {
    int id;
    Student(int id) { this.id = id; }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Student)) return false;
        return this.id == ((Student) o).id;
    }
    // hashCode() is NOT overridden! Inherits Object.hashCode() (Memory address based)
}
```

Now try using it in a `HashMap`:

```java
Map<Student, String> map = new HashMap<>();
Student s1 = new Student(101);
Student s2 = new Student(101);

System.out.println(s1.equals(s2)); // true!

map.put(s1, "Honor Roll");

System.out.println(map.get(s2));   // null! (FAILED LOOKUP)
System.out.println(map.size());    // 1

map.put(s2, "Dean's List");
System.out.println(map.size());    // 2! (DUPLICATE LOGICAL KEYS IN MAP)
```

#### Why did this happen?
1. Because `hashCode()` was not overridden, `s1` and `s2` use the default identity hash code from `java.lang.Object` (derived from their memory addresses).
2. `s1.hashCode()` $\neq$ `s2.hashCode()`.
3. `s1` is placed in Bucket 3.
4. When calling `map.get(s2)`, HashMap hashes `s2` and searches Bucket 11!
5. Bucket 11 is empty, returning `null`.
6. Furthermore, `map.put(s2, ...)` places `s2` into Bucket 11, resulting in **duplicate logical keys** inside the map!

---

### Scenario B: Overriding `hashCode()` but NOT `equals()` (Catastrophic)

Suppose you override `hashCode()` to return a valid hash, but fail to override `equals()`:

```java
class Student {
    int id;
    Student(int id) { this.id = id; }
    
    @Override
    public int hashCode() {
        return Integer.hashCode(id);
    }
    // equals() is NOT overridden! Inherits Object.equals() (Identity check: this == obj)
}
```

```java
Map<Student, String> map = new HashMap<>();
Student s1 = new Student(101);
Student s2 = new Student(101);

map.put(s1, "Honor Roll");
System.out.println(map.get(s2)); // null!
```

#### Why did this happen?
1. `s1.hashCode()` and `s2.hashCode()` both equal `101`, so they map to the exact same bucket.
2. In the bucket, HashMap checks:
   ```java
   if (p.hash == hash && ((k = p.key) == key || (key != null && key.equals(k))))
   ```
3. `s1 == s2` is `false` (two distinct heap objects).
4. `s2.equals(s1)` calls `Object.equals()`, which evaluates `this == obj` $\to$ `false`!
5. Lookup fails and returns `null`.

---

### Scenario C: Overriding Neither (Defaults to Identity)

- Every distinct instance is treated as a unique key.
- Two instances with identical fields will never match.

---

### Scenario D: Correct Implementation (Both Overridden Consistently)

Both methods must use the **exact same subset of significant fields**:

```java
class Student {
    private final int id;
    private final String name;

    public Student(int id, String name) {
        this.id = id;
        this.name = name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Student student = (Student) o;
        return id == student.id && Objects.equals(name, student.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name);
    }
}
```

---

## 3. The Mutable Key Hazard & Memory Leaks

> **Golden Rule**: Never use a mutable object as a HashMap key. If you must, ensure its mutated fields are not included in `hashCode()` and `equals()`.

### The Fatal Sequence:

```java
class MutableKey {
    String name;
    MutableKey(String name) { this.name = name; }
    
    @Override
    public int hashCode() { return Objects.hash(name); }
    @Override
    public boolean equals(Object o) {
        return (o instanceof MutableKey) && Objects.equals(name, ((MutableKey) o).name);
    }
}

// Execution:
Map<MutableKey, String> map = new HashMap<>();
MutableKey key = new MutableKey("Original");

// Step 1: Put entry. Hash is computed from "Original" (e.g. 1500 -> Bucket 4)
map.put(key, "Important Data");

// Step 2: Mutate the key's state
key.name = "Mutated";

// Step 3: Attempt to get or remove
System.out.println(map.get(key));           // null!
System.out.println(map.containsKey(key));   // false!
System.out.println(map.size());             // 1! (Entry is trapped inside!)
```

### Why Does This Happen Internally?

1. When `put(key, ...)` is called:
   - `key.hashCode()` is computed using `"Original"`.
   - Node is placed in **Bucket 4**. The Node stores `hash = hash("Original")`.
2. When `key.name = "Mutated"` is executed:
   - The key's state changes in-place in memory.
   - The key inside the bucket node now has `name = "Mutated"`, but its bucket position is still **Bucket 4**!
3. When `map.get(key)` is called:
   - HashMap calls `key.hashCode()`, which now returns the hash of `"Mutated"`.
   - HashMap calculates the bucket index for `"Mutated"` $\to$ **Bucket 11**!
   - Bucket 11 is empty $\to$ returns `null`.
4. Even if Bucket 11 coincidentally equaled Bucket 4:
   - HashMap compares `p.hash == hash`.
   - `p.hash` is the cached old hash; `hash` is the new hash.
   - `p.hash != hash` $\to$ lookup still fails!

### Consequences:
- **Silent Data Loss**: The mapping cannot be accessed.
- **Memory Leak**: The entry cannot be deleted using `map.remove(key)`. The object remains referenced in `table`, preventing Garbage Collection from reclaiming either key or value.

---

## 4. Why `String`, `Integer`, and Records Are Ideal Keys

| Key Type | Why It Is Ideal |
| :--- | :--- |
| **`String`** | 1. **Immutable**: Its character array cannot be changed once created.<br/>2. **Cached Hash**: `String` contains a private field `private int hash;`. The hash code is computed only on the first call and cached. Subsequent calls execute in $O(1)$ without re-traversing characters.<br/>3. **Thread-Safe**: Safe to share across threads. |
| **`Integer` / `Long`** | 1. **Immutable**: Primitive wrapper values are final.<br/>2. **Trivial Hash**: Hash computation is virtually instantaneous (`(int)value` or bit shift). |
| **Java Records (Java 16+)**| 1. **Shallow Immutability**: All fields are implicitly `final`.<br/>2. **Automatic Contract**: The compiler auto-generates canonical, consistent implementations of `equals()` and `hashCode()` across all components. |
