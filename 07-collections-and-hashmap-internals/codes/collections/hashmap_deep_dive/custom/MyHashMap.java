package collections.hashmap_deep_dive.custom;

import java.util.*;

/**
 * A detailed, production-grade custom implementation of HashMap from scratch.
 *
 * This class demonstrates the core internal mechanisms of Java's HashMap:
 * 1. Array of bucket nodes with Separate Chaining.
 * 2. The 32-bit perturbation hash function: (h ^ (h >>> 16)).
 * 3. Fast power-of-two index masking: (table.length - 1) & hash.
 * 4. Lazy table allocation on the first put() call.
 * 5. Full support for a null key (placed in bucket 0) and null values.
 * 6. Tail-insertion during collision chaining.
 * 7. Java 8-style high-bit partition optimization during resize() (no rehashing).
 * 8. Comprehensive implementation of standard Map operations.
 *
 * @param <K> the type of keys maintained by this map
 * @param <V> the type of mapped values
 */
public class MyHashMap<K, V> implements MyMap<K, V> {

    // =========================================================================
    // Core Internal Constants (Mirroring java.util.HashMap)
    // =========================================================================

    /**
     * The default initial capacity - MUST be a power of two (16).
     */
    static final int DEFAULT_INITIAL_CAPACITY = 1 << 4; // 16

    /**
     * The maximum capacity, used if a higher value is implicitly specified.
     * Must be a power of two <= 1<<30 to avoid integer overflow.
     */
    static final int MAXIMUM_CAPACITY = 1 << 30; // 1,073,741,824

    /**
     * The default load factor used when none is specified in constructor.
     * 0.75 offers the optimal empirical tradeoff between time and space overhead.
     */
    static final float DEFAULT_LOAD_FACTOR = 0.75f;

    // =========================================================================
    // Internal Fields
    // =========================================================================

    /**
     * The table of buckets, initialized on first use, and resized as necessary.
     * Its length is ALWAYS a power of two.
     */
    private Node<K, V>[] table;

    /**
     * The number of key-value mappings contained in this map.
     */
    private int size;

    /**
     * The next size value at which to resize (capacity * load factor).
     */
    private int threshold;

    /**
     * The load factor for the hash table.
     */
    private final float loadFactor;

    /**
     * Tracks the number of structural modifications (for fail-fast semantics).
     */
    private int modCount;

    // =========================================================================
    // Internal Node Class
    // =========================================================================

    /**
     * Basic singly-linked node used for separate chaining.
     */
    public static class Node<K, V> implements MyMap.Entry<K, V> {
        final int hash;     // Cached perturbation hash of key
        final K key;        // The key
        V value;            // The mapped value
        Node<K, V> next;    // Pointer to next node in collision chain

        public Node(int hash, K key, V value, Node<K, V> next) {
            this.hash = hash;
            this.key = key;
            this.value = value;
            this.next = next;
        }

        @Override
        public K getKey() {
            return key;
        }

        @Override
        public V getValue() {
            return value;
        }

        @Override
        public V setValue(V newValue) {
            V oldValue = this.value;
            this.value = newValue;
            return oldValue;
        }

        @Override
        public final String toString() {
            return key + "=" + value;
        }

        @Override
        public final int hashCode() {
            return Objects.hashCode(key) ^ Objects.hashCode(value);
        }

        @Override
        public final boolean equals(Object o) {
            if (o == this) return true;
            if (o instanceof MyMap.Entry<?, ?> e) {
                return Objects.equals(key, e.getKey()) &&
                       Objects.equals(value, e.getValue());
            }
            return false;
        }
    }

    // =========================================================================
    // Constructors
    // =========================================================================

    /**
     * Constructs an empty MyHashMap with the default initial capacity (16)
     * and the default load factor (0.75).
     */
    public MyHashMap() {
        this(DEFAULT_INITIAL_CAPACITY, DEFAULT_LOAD_FACTOR);
    }

    /**
     * Constructs an empty MyHashMap with the specified initial capacity
     * and the default load factor (0.75).
     *
     * @param initialCapacity the initial capacity
     */
    public MyHashMap(int initialCapacity) {
        this(initialCapacity, DEFAULT_LOAD_FACTOR);
    }

    /**
     * Constructs an empty MyHashMap with the specified initial capacity
     * and load factor.
     *
     * @param initialCapacity the initial capacity
     * @param loadFactor      the load factor
     */
    @SuppressWarnings("unchecked")
    public MyHashMap(int initialCapacity, float loadFactor) {
        if (initialCapacity < 0) {
            throw new IllegalArgumentException("Illegal initial capacity: " + initialCapacity);
        }
        if (initialCapacity > MAXIMUM_CAPACITY) {
            initialCapacity = MAXIMUM_CAPACITY;
        }
        if (loadFactor <= 0 || Float.isNaN(loadFactor)) {
            throw new IllegalArgumentException("Illegal load factor: " + loadFactor);
        }

        this.loadFactor = loadFactor;
        // Compute the threshold rounded to the next power of two.
        // Table array allocation is deferred lazily until the first put() call.
        this.threshold = tableSizeFor(initialCapacity);
    }

    // =========================================================================
    // Hash & Index Helpers
    // =========================================================================

    /**
     * Computes the perturbation hash: key.hashCode() XORed with its upper 16 bits.
     *
     * Why? In tables with small capacity (e.g. 16), index calculation (n-1) & hash
     * only uses the lowest few bits. XORing the high bits prevents persistent
     * collisions for keys that differ only in high-order bits.
     *
     * A null key always returns hash 0.
     */
    static final int hash(Object key) {
        int h;
        return (key == null) ? 0 : (h = key.hashCode()) ^ (h >>> 16);
    }

    /**
     * Returns a power of two size for the given target capacity.
     */
    static final int tableSizeFor(int cap) {
        int n = -1 >>> Integer.numberOfLeadingZeros(cap - 1);
        return (n < 0) ? 1 : (n >= MAXIMUM_CAPACITY) ? MAXIMUM_CAPACITY : n + 1;
    }

    // =========================================================================
    // Core Map Operations (put, get, remove)
    // =========================================================================

    /**
     * Associates the specified value with the specified key in this map.
     *
     * @param key   key with which the specified value is to be associated
     * @param value value to be associated with the specified key
     * @return the previous value associated with key, or null
     */
    @Override
    public V put(K key, V value) {
        return putVal(hash(key), key, value, false);
    }

    /**
     * Internal implementation of put and putIfAbsent.
     */
    private V putVal(int hash, K key, V value, boolean onlyIfAbsent) {
        Node<K, V>[] tab = table;
        int n;

        // Step 1: Lazy initialization - table allocated on first insert
        if (tab == null || (n = tab.length) == 0) {
            n = (tab = resize()).length;
        }

        // Step 2: Compute bucket index using bitwise mask: i = (n - 1) & hash
        int i = (n - 1) & hash;
        Node<K, V> p = tab[i];

        // Step 3: Fast-path - bucket is empty
        if (p == null) {
            tab[i] = new Node<>(hash, key, value, null);
        } else {
            // Step 4: Collision path - bucket is already occupied
            Node<K, V> targetNode = null;
            K k;

            // Check if bucket's head node matches the incoming key
            // Note: (p.hash == hash) check is evaluated first to avoid costly equals() calls!
            if (p.hash == hash && ((k = p.key) == key || (key != null && key.equals(k)))) {
                targetNode = p;
            } else {
                // Traverse singly linked list chain
                Node<K, V> curr = p;
                while (true) {
                    if (curr.hash == hash &&
                        ((k = curr.key) == key || (key != null && key.equals(k)))) {
                        targetNode = curr;
                        break;
                    }
                    if (curr.next == null) {
                        // Reached tail without finding match: append new node at tail (Java 8 style)
                        curr.next = new Node<>(hash, key, value, null);
                        break;
                    }
                    curr = curr.next;
                }
            }

            // If an existing mapping was found, update value and return old value
            if (targetNode != null) {
                V oldValue = targetNode.value;
                if (!onlyIfAbsent || oldValue == null) {
                    targetNode.value = value;
                }
                return oldValue;
            }
        }

        // Step 5: Post-insertion book-keeping
        modCount++;
        if (++size > threshold) {
            resize();
        }
        return null;
    }

    /**
     * Retrieves the value associated with the specified key.
     *
     * @param key the key whose associated value is to be returned
     * @return the mapped value, or null if key is absent
     */
    @Override
    public V get(Object key) {
        Node<K, V> e = getNode(hash(key), key);
        return (e == null) ? null : e.value;
    }

    /**
     * Locates the Node corresponding to the given hash and key.
     */
    private Node<K, V> getNode(int hash, Object key) {
        Node<K, V>[] tab = table;
        Node<K, V> first, e;
        int n;
        K k;

        if (tab != null && (n = tab.length) > 0 &&
            (first = tab[(n - 1) & hash]) != null) {

            // Fast-path: Check head node
            if (first.hash == hash &&
                ((k = first.key) == key || (key != null && key.equals(k)))) {
                return first;
            }

            // Traverse bucket chain
            if ((e = first.next) != null) {
                do {
                    if (e.hash == hash &&
                        ((k = e.key) == key || (key != null && key.equals(k)))) {
                        return e;
                    }
                } while ((e = e.next) != null);
            }
        }
        return null;
    }

    /**
     * Removes the mapping for the specified key from this map if present.
     *
     * @param key key whose mapping is to be removed
     * @return previous value associated with key, or null
     */
    @Override
    public V remove(Object key) {
        Node<K, V> node = removeNode(hash(key), key);
        return (node == null) ? null : node.value;
    }

    /**
     * Internal node removal helper unlinking node from bucket chain.
     */
    private Node<K, V> removeNode(int hash, Object key) {
        Node<K, V>[] tab = table;
        Node<K, V> p;
        int n, index;

        if (tab != null && (n = tab.length) > 0 &&
            (p = tab[index = (n - 1) & hash]) != null) {

            Node<K, V> target = null;
            Node<K, V> prev = null;
            Node<K, V> curr = p;
            K k;

            // Search for target node
            while (curr != null) {
                if (curr.hash == hash &&
                    ((k = curr.key) == key || (key != null && key.equals(k)))) {
                    target = curr;
                    break;
                }
                prev = curr;
                curr = curr.next;
            }

            // If found, unlink node
            if (target != null) {
                if (prev == null) {
                    // Target was head node
                    tab[index] = target.next;
                } else {
                    // Target was middle or tail node
                    prev.next = target.next;
                }
                modCount++;
                size--;
                return target;
            }
        }
        return null;
    }

    /**
     * Resizes the table and rehashes existing nodes.
     *
     * Uses Java 8's High-Bit Test Optimization:
     * When capacity doubles, (newCap - 1) has exactly 1 new bit set compared to (oldCap - 1).
     * Therefore:
     * - If (e.hash & oldCap) == 0: element stays at oldIndex.
     * - If (e.hash & oldCap) != 0: element moves to (oldIndex + oldCap).
     *
     * This avoids costly hash recalculations and maintains original relative node order.
     */
    @SuppressWarnings("unchecked")
    private Node<K, V>[] resize() {
        Node<K, V>[] oldTab = table;
        int oldCap = (oldTab == null) ? 0 : oldTab.length;
        int oldThr = threshold;
        int newCap, newThr = 0;

        if (oldCap > 0) {
            if (oldCap >= MAXIMUM_CAPACITY) {
                threshold = Integer.MAX_VALUE;
                return oldTab;
            } else if ((newCap = oldCap << 1) < MAXIMUM_CAPACITY &&
                       oldCap >= DEFAULT_INITIAL_CAPACITY) {
                // Double capacity and threshold
                newThr = oldThr << 1;
            } else {
                newCap = oldCap << 1;
            }
        } else if (oldThr > 0) {
            // Initial capacity was placed in threshold by constructor
            newCap = oldThr;
        } else {
            // Default initial allocation
            newCap = DEFAULT_INITIAL_CAPACITY;
            newThr = (int) (DEFAULT_INITIAL_CAPACITY * DEFAULT_LOAD_FACTOR);
        }

        if (newThr == 0) {
            float ft = (float) newCap * loadFactor;
            newThr = (newCap < MAXIMUM_CAPACITY && ft < (float) MAXIMUM_CAPACITY)
                    ? (int) ft : Integer.MAX_VALUE;
        }

        threshold = newThr;
        Node<K, V>[] newTab = (Node<K, V>[]) new Node[newCap];
        table = newTab;

        if (oldTab != null) {
            for (int j = 0; j < oldCap; ++j) {
                Node<K, V> e;
                if ((e = oldTab[j]) != null) {
                    oldTab[j] = null; // Help GC

                    if (e.next == null) {
                        // Single node in bucket: direct placement
                        newTab[e.hash & (newCap - 1)] = e;
                    } else {
                        // Chained bucket: split into two sub-chains preserving order
                        Node<K, V> loHead = null, loTail = null;
                        Node<K, V> hiHead = null, hiTail = null;
                        Node<K, V> next;

                        do {
                            next = e.next;
                            // High-bit test: checks if bit at oldCap position is 0 or 1
                            if ((e.hash & oldCap) == 0) {
                                if (loTail == null) {
                                    loHead = e;
                                } else {
                                    loTail.next = e;
                                }
                                loTail = e;
                            } else {
                                if (hiTail == null) {
                                    hiHead = e;
                                } else {
                                    hiTail.next = e;
                                }
                                hiTail = e;
                            }
                        } while ((e = next) != null);

                        // Place low chain at the same index
                        if (loTail != null) {
                            loTail.next = null;
                            newTab[j] = loHead;
                        }
                        // Place high chain at (index + oldCap)
                        if (hiTail != null) {
                            hiTail.next = null;
                            newTab[j + oldCap] = hiHead;
                        }
                    }
                }
            }
        }
        return newTab;
    }

    // =========================================================================
    // Query Operations
    // =========================================================================

    @Override
    public boolean containsKey(Object key) {
        return getNode(hash(key), key) != null;
    }

    @Override
    public boolean containsValue(Object value) {
        Node<K, V>[] tab = table;
        if (tab != null && size > 0) {
            for (Node<K, V> head : tab) {
                for (Node<K, V> e = head; e != null; e = e.next) {
                    if (Objects.equals(e.value, value)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public boolean isEmpty() {
        return size == 0;
    }

    @Override
    public void clear() {
        Node<K, V>[] tab = table;
        if (tab != null && size > 0) {
            size = 0;
            Arrays.fill(tab, null);
            modCount++;
        }
    }

    /**
     * Returns the current array bucket capacity. Useful for test inspection.
     */
    public int capacity() {
        return (table == null) ? 0 : table.length;
    }

    /**
     * Returns current threshold.
     */
    public int threshold() {
        return threshold;
    }

    // =========================================================================
    // Collection Views & Iteration
    // =========================================================================

    @Override
    public Set<K> keySet() {
        Set<K> keys = new LinkedHashSet<>();
        Node<K, V>[] tab = table;
        if (tab != null && size > 0) {
            for (Node<K, V> head : tab) {
                for (Node<K, V> e = head; e != null; e = e.next) {
                    keys.add(e.key);
                }
            }
        }
        return Collections.unmodifiableSet(keys);
    }

    @Override
    public Collection<V> values() {
        List<V> list = new ArrayList<>();
        Node<K, V>[] tab = table;
        if (tab != null && size > 0) {
            for (Node<K, V> head : tab) {
                for (Node<K, V> e = head; e != null; e = e.next) {
                    list.add(e.value);
                }
            }
        }
        return Collections.unmodifiableList(list);
    }

    @Override
    public Set<MyMap.Entry<K, V>> entrySet() {
        Set<MyMap.Entry<K, V>> entries = new LinkedHashSet<>();
        Node<K, V>[] tab = table;
        if (tab != null && size > 0) {
            for (Node<K, V> head : tab) {
                for (Node<K, V> e = head; e != null; e = e.next) {
                    entries.add(e);
                }
            }
        }
        return Collections.unmodifiableSet(entries);
    }

    @Override
    public String toString() {
        if (isEmpty()) {
            return "{}";
        }
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        Node<K, V>[] tab = table;
        if (tab != null) {
            for (Node<K, V> head : tab) {
                for (Node<K, V> e = head; e != null; e = e.next) {
                    if (!first) {
                        sb.append(", ");
                    }
                    sb.append(e.key == this ? "(this Map)" : e.key)
                      .append("=")
                      .append(e.value == this ? "(this Map)" : e.value);
                    first = false;
                }
            }
        }
        return sb.append('}').toString();
    }
}
