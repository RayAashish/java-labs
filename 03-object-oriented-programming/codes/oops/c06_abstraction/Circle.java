package oops.c06_abstraction;

public class Circle implements Shape{
    public static final double pi = 3.1415;
    int radius;
    public Circle(int radius){
        this.radius = radius;
    }
    @Override 
    public double area(){
        return 2 * pi * radius * radius;
    }

    public static void main(String[] args) {
        Circle circle = new Circle(23);
        double area = circle.area();
        System.out.println(area);
    }
}

