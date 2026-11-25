package oops.c02_constructors;

public class ConstructorChaining {
    public static void main(String[] args) {
        Car c1 = new Car("Super Car");
        Car c2 = new Car(6);
        Car c3 = new Car( -1); //will throw exception (IllegalArgumentException)
        System.out.println(c1);
        System.out.println(c2);
        System.out.println(c3);
    }
}


/**
 * Car
 * Concerete rules of Constructor chaining : 
 * 1. this keyword will come at the top
 * 2. only one this keyword can be used in the entire constructor
 * 3. recursion is prohibited (Constructor can't call itself)
 * Ex 
 *      public Car(){
            this();
        }
 *  this() & super() can't apper in the same constructor
 */
class Car{
    String type;
    int noOfWheel;

    // Recursive constructor is invoked
    // public Car(){
    //     this();
    // }

    public Car(){
        this("Unknown", 0);
    }
    public Car(String type){
        this(type, 0);
    }
    public Car(int noOfWheel){
        this("Unknown", noOfWheel);
        System.out.println("Object created with noOfWheel parameter & Constructor chained");
    }
    /**
     * @param noOfWheel
     * It will throw a compiler error because in 
     * constructor chaining, the concerete rule is that
     * 'this' keyword should come first
     */
    // public Car(int noOfWheel){
    //     System.out.println("Something something..........");
    //     this("Unknown", noOfWheel); 
    // }

    public Car(String type, int noOfWheel){
        if (noOfWheel < 0)
            throw new IllegalArgumentException("No of wheel can't be negative");
        this.type = type;
        this.noOfWheel = noOfWheel;
    }
    @Override
    public String toString() {
        return "Car [type=" + type + ", noOfWheel=" + noOfWheel + "]";
    }
    
}