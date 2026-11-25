package oops.c06_abstraction;

/**
 * Concrete Circle implementation of the Shape contract.
 */
public class Circle implements Shape {
    private final double radius;

    public Circle(double radius) {
        if (radius < 0) {
            throw new IllegalArgumentException("Radius cannot be negative");
        }
        this.radius = radius;
    }

    @Override
    public double area() {
        return Math.PI * radius * radius;
    }

    public static void main(String[] args) {
        Shape circle = new Circle(23);
        System.out.printf("Circle area (r=23): %.2f%n", circle.area());
    }
}
