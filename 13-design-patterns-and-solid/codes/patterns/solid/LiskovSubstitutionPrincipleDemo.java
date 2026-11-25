package patterns.solid;

/**
 * L — LISKOV SUBSTITUTION PRINCIPLE (LSP)
 *
 * WHAT IT IS:
 *   Subtypes must be substitutable for their base types WITHOUT breaking caller invariants,
 *   post-conditions, or throwing surprise UnsupportedOperationException errors.
 *
 * WHY IT MATTERS:
 *   If callers accept a base type `Rectangle` or `Account` and a subclass silently breaks the
 *   expected behavior, polymorphism becomes unreliable and forces ugly `instanceof` checks everywhere.
 *
 * IN THIS FILE:
 *   1. BAD CODE:  Classic Square extends Rectangle — setting width/height mutates both,
 *                 breaking the caller invariant (area == 5 * 10 = 50).
 *   2. GOOD CODE: Separate immutable Shape abstraction (width/height invariants preserved cleanly).
 */
public class LiskovSubstitutionPrincipleDemo {

    // =========================================================================
    // ❌ BAD CODE: Square IS-A Rectangle mathematically, NOT behaviorally in mutable OOP!
    // =========================================================================
    static class BadRectangle {
        protected int width;
        protected int height;

        public void setWidth(int width) { this.width = width; }
        public void setHeight(int height) { this.height = height; }
        public int area() { return width * height; }
    }

    static class BadSquare extends BadRectangle {
        @Override
        public void setWidth(int width) {
            this.width = width;
            this.height = width; // Mutates height unexpectedly!
        }

        @Override
        public void setHeight(int height) {
            this.width = height; // Overwrites previously set width!
            this.height = height;
        }
    }

    private static void verifyRectangleContract(BadRectangle rect) {
        rect.setWidth(5);
        rect.setHeight(10);
        System.out.println("[BAD] Expected area=50, actual=" + rect.area()
                + " (type=" + rect.getClass().getSimpleName() + ")");
    }

    // =========================================================================
    // ✅ GOOD CODE: Model shared immutable capability instead of broken mutating inheritance.
    // =========================================================================
    interface Shape {
        int area();
    }

    record Rectangle(int width, int height) implements Shape {
        @Override
        public int area() {
            return width * height;
        }
    }

    record Square(int side) implements Shape {
        @Override
        public int area() {
            return side * side;
        }
    }

    public static void main(String[] args) {
        System.out.println("=== BAD (LSP Violation: Square silently corrupts Rectangle width) ===");
        verifyRectangleContract(new BadRectangle());
        verifyRectangleContract(new BadSquare()); // Prints 100 instead of 50!

        System.out.println("\n=== GOOD (LSP Respected: Both fulfill Shape.area() without broken invariants) ===");
        Shape rect = new Rectangle(5, 10);
        Shape square = new Square(5);
        System.out.println("[GOOD] Rectangle(5x10) area = " + rect.area());
        System.out.println("[GOOD] Square(5) area       = " + square.area());
    }
}
