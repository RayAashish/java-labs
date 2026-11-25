package oops.c04_inheritance;

import java.time.LocalDate;

public class MultiLevelInheritance {
    
    public static void main(String[] args) {
        LocalDate dob = LocalDate.of(2002, 8, 24);
        Child child = new Child(dob);
        child.investments();

        System.out.println(child.getDob());
    }
    
}

class GrandParent{
    public LocalDate dob;

    public LocalDate getDob() {
        return dob;
    }

    GrandParent(LocalDate dob){
        this.dob = dob;
    }
    
    public void investments(){
        System.out.println("Invested in Land");
    }
}

/**
 * Parent
 * {@class GrandParent} is being inherited by Parent Class
 */
class Parent extends GrandParent{
    public Parent(LocalDate dob){
        super(dob);
    }
    @Override 
    public void investments(){
        super.investments();
        System.out.println("Invested in Gold & Stocks");
    }
}

class Child extends Parent{

    public Child(LocalDate dob){
        super(dob);
    }

    @Override 
    public void investments(){
        super.investments();
        System.out.println("Invested in NFTs & Crypto");
    }
}