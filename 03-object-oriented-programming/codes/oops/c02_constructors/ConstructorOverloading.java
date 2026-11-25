package oops.c02_constructors;



public class ConstructorOverloading {
    public static void main(String[] args) {
        Animal cat = new Animal(4, true, true, "Cat");
        Animal dog = new Animal(true, true, "Dog", 4);
        System.out.println(cat.toString());
        System.out.println(dog.toString());
    }
}



/**
 * Animal
 * The constructors overloading works exactly words like the method overloading
 */
class Animal{
    String type;
    boolean isDomestic;
    boolean isMamal;
    int noOfLegs;

    public Animal(int noOfLegs, boolean isMamal, boolean isDomestic, String type){
        this.type = type;
        this.isDomestic = isDomestic;
        this.isMamal = isMamal;
        this.noOfLegs = noOfLegs;
    }
    public Animal(boolean isMamal, boolean isDomestic, String type, int noOfLegs){
        this.type = type;
        this.isDomestic = isDomestic;
        this.isMamal = isMamal;
        this.noOfLegs = noOfLegs;
    }
    

    public Animal(String type, boolean isDomestic, boolean isMamal, int noOfLegs) {
        this.type = type;
        this.isDomestic = isDomestic;
        this.isMamal = isMamal;
        this.noOfLegs = noOfLegs;
    }

    @Override
    public String toString() {
        return "Animal [type=" + type + ", isDomestic=" + isDomestic + ", isMamal=" + isMamal + ", noOfLegs=" + noOfLegs
                + "]";
    }
}