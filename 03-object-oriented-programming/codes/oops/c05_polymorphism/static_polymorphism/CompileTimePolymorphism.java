package oops.c05_polymorphism.static_polymorphism;

/**
 * CompileTimePolymorphism
 * also called MethodOverriding or static polymorphism
 * This throws the error at compile time so it's called CompileTimePolymorphism
 * This polymorphism simply means that the a method with same name can be written multiple
 * time which different parameters
 */
public class CompileTimePolymorphism {

    public static void main(String[] args) {
        Calculator cal = new Calculator();
        cal.add("Aashish ", "Ray");
        cal.add(16, 100);
        cal.add(4.3, 9.8);
    }
}

/**
 * Calculator
 * the name of the all function are same
 * but the Parameter is different.
 * This is allowed in java
 */
class Calculator{

    
    public void add(int x, int y){
        System.out.println(x + y);
    }

    public void add(double x, double y){
        System.out.println(x + y);
    }

    public void add(String x, String y){
        System.out.println(x + y);
    }
}
