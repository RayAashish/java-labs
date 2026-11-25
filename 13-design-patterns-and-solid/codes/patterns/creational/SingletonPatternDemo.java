package patterns.creational;

import java.lang.reflect.Constructor;

/**
 * CREATIONAL PATTERN 1 — SINGLETON PATTERN
 *
 * WHAT IT IS:
 *   Ensures a class has ONLY ONE instance in the JVM and provides a global access point to it.
 *
 * WHY IT MATTERS:
 *   Shared coordinators (connection pool registry, config cache, hardware driver lock) should not
 *   be instantiated repeatedly. However, naive singletons break under multithreading, Reflection,
 *   deserialization, or cloning!
 *
 * IN THIS FILE:
 *   1. BAD CODE:  Naive non-thread-safe lazy singleton (race condition under concurrent threads!).
 *   2. GOOD CODE: (a) Double-Checked Locking with mandatory `volatile` + reflection guard,
 *                 (b) Bill Pugh static inner holder (lazy + lock-free via class loading),
 *                 (c) Enum Singleton (Joshua Bloch's gold standard — immune to reflection & serialization).
 */
public class SingletonPatternDemo {

    // =========================================================================
    // ❌ BAD CODE: Broken under multithreading — two threads both see instance == null!
    // =========================================================================
    static class BadUnsafeSingleton {
        private static BadUnsafeSingleton instance;
        private BadUnsafeSingleton() {}

        public static BadUnsafeSingleton getInstance() {
            if (instance == null) { // Thread A & Thread B can BOTH enter here simultaneously!
                instance = new BadUnsafeSingleton();
            }
            return instance;
        }
    }

    // =========================================================================
    // ✅ GOOD CODE A: Double-Checked Locking (volatile prevents half-constructed reads!)
    // =========================================================================
    static final class ThreadSafeDclSingleton {
        private static volatile ThreadSafeDclSingleton instance;

        private ThreadSafeDclSingleton() {
            if (instance != null) {
                throw new IllegalStateException("Singleton already instantiated — Reflection blocked!");
            }
        }

        public static ThreadSafeDclSingleton getInstance() {
            if (instance == null) { // Fast lock-free check once initialized
                synchronized (ThreadSafeDclSingleton.class) {
                    if (instance == null) {
                        instance = new ThreadSafeDclSingleton();
                    }
                }
            }
            return instance;
        }
    }

    // =========================================================================
    // ✅ GOOD CODE B: Bill Pugh Static Holder (Lazy, zero synchronization overhead)
    // =========================================================================
    static final class HolderSingleton {
        private HolderSingleton() {}

        private static final class Holder {
            // Loaded & initialized lazily & thread-safely by JVM <clinit> on first access
            private static final HolderSingleton INSTANCE = new HolderSingleton();
        }

        public static HolderSingleton getInstance() {
            return Holder.INSTANCE;
        }
    }

    // =========================================================================
    // ✅ GOOD CODE C: Enum Singleton (JVM-enforced safety against Reflection & Serialization)
    // =========================================================================
    enum EnumSingleton {
        INSTANCE;

        public String getClusterMode() {
            return "ACTIVE_PROD";
        }
    }

    public static void main(String[] args) throws Exception {
        System.out.println("=== 1. DCL Singleton Identity & Reflection Defense ===");
        ThreadSafeDclSingleton s1 = ThreadSafeDclSingleton.getInstance();
        ThreadSafeDclSingleton s2 = ThreadSafeDclSingleton.getInstance();
        System.out.println("s1 == s2: " + (s1 == s2));

        Constructor<ThreadSafeDclSingleton> ctor = ThreadSafeDclSingleton.class.getDeclaredConstructor();
        ctor.setAccessible(true);
        try {
            ctor.newInstance();
        } catch (Exception ex) {
            System.out.println("Reflection attack blocked: " + ex.getCause().getMessage());
        }

        System.out.println("\n=== 2. Bill Pugh Holder Singleton ===");
        System.out.println("Holder same instance? " + (HolderSingleton.getInstance() == HolderSingleton.getInstance()));

        System.out.println("\n=== 3. Enum Singleton ===");
        System.out.println("Enum mode=" + EnumSingleton.INSTANCE.getClusterMode()
                + ", same? " + (EnumSingleton.INSTANCE == EnumSingleton.valueOf("INSTANCE")));
    }
}
