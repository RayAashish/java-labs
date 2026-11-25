package oops.c02_constructors;



public class ConstructorStructure {
    public static void main(String[] args) {
        Planet p1 = new Planet("Earth");
        System.out.println(p1.getName());
    }
}

class Planet{
    private String name;
    private int noOfMoon;

    /**
     * @author Aashish Kumar Ray
     * Here we can see the structure of a constructor:
     * 1. The name should be exactly of Class Name.
     * 2. It shouldn't have any return type.
     * 3. The constructor can be parametrised or non-parameterized
     * 4. If there are no constructor, default constructor is always initilized by default
     * when new keyword is used for object creation.
     * 
     */
    Planet(){}
    Planet(String name){
        this.name = name;
    }
    Planet(int noOfMoon){
        this.noOfMoon = noOfMoon;
    }
    public Planet(String name, int noOfMoon) {
        this.name = name;
        this.noOfMoon = noOfMoon;
    }
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public int getNoOfMoon() {
        return noOfMoon;
    }
    public void setNoOfMoon(int noOfMoon) {
        this.noOfMoon = noOfMoon;
    }
    
}