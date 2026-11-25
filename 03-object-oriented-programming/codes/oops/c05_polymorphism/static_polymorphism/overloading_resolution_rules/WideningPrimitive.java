package oops.c05_polymorphism.static_polymorphism.overloading_resolution_rules;

/**
 * Demonstrates Java's primitive widening chain: byte -> short -> int -> long -> float -> double.
 * The compiler selects the smallest widening step that matches an available overload.
 */
public class WideningPrimitive {
    public static void main(String[] args) {
        WideningTarget target = new WideningTarget();
        target.show(1);             // exact int
        target.show(4L);            // exact long
        target.show(12.34);         // exact double
        target.show((byte) 10);     // byte widens to int
        target.show((Long) 12L);    // Long unboxes to long
        target.show((short) 98);    // short widens to int
        target.show(23.3f);         // float widens to double
    }

    static class WideningTarget {
        void show(int x) {
            System.out.println("int");
        }

        void show(double x) {
            System.out.println("double");
        }

        void show(long x) {
            System.out.println("long");
        }
    }
}
