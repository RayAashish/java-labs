package collections.map.linkedhashmap;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BiConsumer;

/**
 * ============================================================================
 * Java Collections Framework Mastery — Production-Grade LRU Cache Implementation
 * ============================================================================
 *
 * Implements a complete Least-Recently-Used (LRU) Cache by subclassing
 * {@link LinkedHashMap}:
 *  1. Uses the accessOrder = true constructor to track usage.
 *  2. Overrides {@link LinkedHashMap#removeEldestEntry(Map.Entry)} to enforce
 *     strict capacity boundaries.
 *  3. Supports an optional eviction listener callback for resource cleanup.
 *  4. Provides thread-safe concurrency models:
 *     - Synchronized Map wrapper
 *     - Fine-grained explicit locking with ReentrantLock
 *  5. Demonstrates multi-threaded cache hits, misses, and evictions.
 *
 * @author raysi
 */
public class LRUCacheImplementation {

    public static void main(String[] args) throws InterruptedException {
        System.out.println("==================================================================");
        System.out.println("   PRODUCTION-GRADE LRU CACHE INTERNALS & EVICTION DEMO          ");
        System.out.println("   Author: raysi                                                  ");
        System.out.println("==================================================================\n");

        demonstrateBasicLRUEviction();
        demonstrateEvictionCallback();
        demonstrateThreadSafeLRUCache();
    }

    /**
     * Demonstrates classic LRU cache behavior:
     * Cache capacity = 3.
     * Insert 1, 2, 3 -> Access 1 -> Insert 4 -> Evicts 2 (since 1 was accessed)!
     */
    private static void demonstrateBasicLRUEviction() {
        System.out.println("--- 1. Bounded LRU Cache Eviction Mechanics (Capacity = 3) ---");
        BoundedLRUCache<Integer, String> cache = new BoundedLRUCache<>(3);

        System.out.println("Inserting [1: One], [2: Two], [3: Three]...");
        cache.put(1, "One");
        cache.put(2, "Two");
        cache.put(3, "Three");
        System.out.println("Cache state: " + cache);

        // Access Key 1 -> Makes Key 1 most-recently-used, Key 2 becomes eldest!
        System.out.println("\n[Access] Reading key 1: " + cache.get(1));
        System.out.println("Cache state after reading 1: " + cache + " (Key 1 moved to MRU tail!)");

        // Insert Key 4 -> Exceeds capacity 3 -> Evicts eldest (Key 2)!
        System.out.println("\n[Action] Inserting [4: Four] (Capacity exceeded)...");
        cache.put(4, "Four");
        System.out.println("Cache state: " + cache);

        System.out.println("Contains Key 2? " + cache.containsKey(2) + " (Evicted as Least Recently Used!)");
        System.out.println("Contains Key 1? " + cache.containsKey(1) + " (Retained because it was accessed!)\n");
    }

    /**
     * Demonstrates cache eviction listener callback to clean up system resources
     * (e.g. Closing database connections, socket handles, or flushing buffers).
     */
    private static void demonstrateEvictionCallback() {
        System.out.println("--- 2. Resource Cleanup with Eviction Listener Hook ---");
        BoundedLRUCache<String, AutoCloseableResource> resourcePool =
                new BoundedLRUCache<>(2, (key, res) -> {
                    System.out.printf("   >>> [EVICTION CALLBACK] Evicting key '%s'. Closing resource '%s'...%n",
                            key, res.getName());
                    res.close();
                });

        System.out.println("Acquiring Resource-A and Resource-B...");
        resourcePool.put("A", new AutoCloseableResource("Resource-A"));
        resourcePool.put("B", new AutoCloseableResource("Resource-B"));

        System.out.println("\n[Action] Adding Resource-C (Forces eviction of Resource-A)...");
        resourcePool.put("C", new AutoCloseableResource("Resource-C"));

        System.out.println("Active Pool Keys: " + resourcePool.keySet());
        System.out.println();
    }

    /**
     * Demonstrates high-throughput thread-safe LRU caching with explicit locking.
     */
    private static void demonstrateThreadSafeLRUCache() throws InterruptedException {
        System.out.println("--- 3. Multi-Threaded Concurrent LRU Cache Verification ---");
        ThreadSafeLRUCache<Integer, String> safeCache = new ThreadSafeLRUCache<>(100);
        int threadCount = 8;
        int operationsPerThread = 500;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        for (int t = 0; t < threadCount; t++) {
            final int threadId = t;
            executor.submit(() -> {
                try {
                    for (int i = 0; i < operationsPerThread; i++) {
                        int key = (threadId * 50) + (i % 80); // Induce deliberate key overlap & contention
                        safeCache.put(key, "Val-" + key);
                        safeCache.get(key);
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();

        System.out.println("All " + (threadCount * operationsPerThread) + " concurrent operations completed.");
        System.out.println("Safe Cache Final Size: " + safeCache.size() + " (Strictly bounded <= 100!)");
        System.out.println("Cache invariant holds: " + (safeCache.size() <= 100));
        System.out.println();
    }

    /**
     * Core Bounded LRU Cache Implementation extending LinkedHashMap.
     */
    public static class BoundedLRUCache<K, V> extends LinkedHashMap<K, V> {
        private final int maxCapacity;
        private final BiConsumer<K, V> evictionListener;

        public BoundedLRUCache(int maxCapacity) {
            this(maxCapacity, null);
        }

        public BoundedLRUCache(int maxCapacity, BiConsumer<K, V> evictionListener) {
            // initialCapacity = maxCapacity + 1 to avoid unnecessary rehashing at limit
            // loadFactor = 1.0f (or 0.75f)
            // accessOrder = true enables LRU access tracking!
            super((int) Math.ceil(maxCapacity / 0.75f) + 1, 0.75f, true);
            if (maxCapacity <= 0) {
                throw new IllegalArgumentException("maxCapacity must be > 0: " + maxCapacity);
            }
            this.maxCapacity = maxCapacity;
            this.evictionListener = evictionListener;
        }

        @Override
        protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
            boolean shouldEvict = size() > maxCapacity;
            if (shouldEvict && evictionListener != null) {
                evictionListener.accept(eldest.getKey(), eldest.getValue());
            }
            return shouldEvict;
        }

        public int getMaxCapacity() {
            return maxCapacity;
        }
    }

    /**
     * Thread-Safe LRU Cache Wrapper using explicit ReentrantLock.
     * Note: ReentrantReadWriteLock is unsuitable for LRU because get() is a structural write!
     */
    public static class ThreadSafeLRUCache<K, V> {
        private final BoundedLRUCache<K, V> cache;
        private final ReentrantLock lock = new ReentrantLock();

        public ThreadSafeLRUCache(int maxCapacity) {
            this.cache = new BoundedLRUCache<>(maxCapacity);
        }

        public V get(K key) {
            lock.lock();
            try {
                return cache.get(key);
            } finally {
                lock.unlock();
            }
        }

        public V put(K key, V value) {
            lock.lock();
            try {
                return cache.put(key, value);
            } finally {
                lock.unlock();
            }
        }

        public V remove(K key) {
            lock.lock();
            try {
                return cache.remove(key);
            } finally {
                lock.unlock();
            }
        }

        public int size() {
            lock.lock();
            try {
                return cache.size();
            } finally {
                lock.unlock();
            }
        }
    }

    /**
     * Mock managed resource for lifecycle demonstration.
     */
    public static class AutoCloseableResource implements AutoCloseable {
        private final String name;
        private boolean closed = false;

        public AutoCloseableResource(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        @Override
        public void close() {
            this.closed = true;
        }

        public boolean isClosed() {
            return closed;
        }
    }
}
