package oops.c05_polymorphism.static_polymorphism.overloading_resolution_rules;

/**
 * Demonstrates Tier 2 (Primitive Widening) vs Tier 3 (Autoboxing) in overload resolution.
 * Rule: Primitive widening (int -> long) ALWAYS beats autoboxing (int -> Integer).
 */
public class BoxingUnboxing {
    public static void main(String[] args) {
        OverloadTarget target = new OverloadTarget();
        target.show(12);          // int widens to long (beats autoboxing to Integer)
        target.show((Long) 2L);   // Long unboxes to long (no exact Long overload)
        target.show((byte) 123);  // byte widens to long
    }

    static class OverloadTarget {
        void show(Integer a) {
            System.out.println("Integer (autoboxed)");
        }

        void show(long a) {
            System.out.println("long (widened/unboxed)");
        }
    }
}
