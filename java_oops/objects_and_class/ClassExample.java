package java_oops.objects_and_class;

public class ClassExample {
    public static void main(String[] args) {
        Circle c1 = new Circle(12);
        c1.area();
        c1.fillColor("blue");
    }
}

class Circle{
    int radius;

    public Circle(int radius){ //Constructor
        super();
        this.radius = radius;
    }

    //Methods 
    public void area(){
        System.out.println("Area : " + 2 * 3.1415 * radius);
    }
    public void fillColor(String color){
        System.out.println("Circle filled with " + color);
    }
}