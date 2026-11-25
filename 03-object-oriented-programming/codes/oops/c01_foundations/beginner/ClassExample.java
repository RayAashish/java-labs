package oops.c01_foundations.beginner;

/**
 * Beginner Class & Object Example: models a geometric Circle with state (radius)
 * and behaviors (area calculation, circumference calculation, and color styling).
 */
public class ClassExample {
    public static void main(String[] args) {
        Circle c1 = new Circle(12);
        c1.area();
        c1.circumference();
        c1.fillColor("blue");
    }

    static class Circle {
        int radius;

        public Circle(int radius) {
            this.radius = radius;
        }

        public void area() {
            System.out.printf("Area : %.2f%n", Math.PI * radius * radius);
        }

        public void circumference() {
            System.out.printf("Circumference : %.2f%n", 2 * Math.PI * radius);
        }

        public void fillColor(String color) {
            System.out.println("Circle filled with " + color);
        }
    }
}
