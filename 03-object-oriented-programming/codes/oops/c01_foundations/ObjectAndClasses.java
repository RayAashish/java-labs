package oops.c01_foundations;

/**
 * @author Aashish Kumar Ray
 * ObjectAndClasses
 * In Simple Terms : 
 * Class : A class is a blueprint of a real-world entity
 * Object : An object can be defined in many ways like :
 * 1. It's an instance of a class 
 * 2. It's a immediate result of a class & so on... 
 * 
 * NOTE : A class can exist without an object
 * but an object can't existed without a class
 */
public class ObjectAndClasses {
    /**
     * @param args
     * Here, BankAccount is a class 
     * & rayAccount is an instance(object) of the BankAccount
     */
    public static void main(String[] args) {
        BankAccount rayAccount = new BankAccount();
        rayAccount.setName("Aashish Kumar Ray");
        rayAccount.setBalance(2000.0);
        System.out.println(rayAccount.toString());
    }
}


/**
 * BankAccount
 * Class is a blueprint for an object :
 * for example, in real-world, A bank is there which provides
 * the facility to open a BankAccount to an individual.
 * A BankAccount becomes as a blueprint to create an account for everyone
 * when an account is created from that, it's called an instance(object )
 * of that bank account which means a real world entity is being used.
 */
class BankAccount{
    private double balance = 0; //By Default when bank account is opened, it's balance is 0
    private String name;
    public BankAccount(){}
    public BankAccount(double balance, String name){
        this.balance = balance;
        this.name = name;
    }
    /**
     * Since the balance & name are private varaibles in a class, 
     * It can't be directly accessed or modified by the external classes or packages
     * so, In order to modify it, setter methods are generally used in java 
     * which is also called as (Setters)
     */
    public void setBalance(double balance){
        this.balance = balance;
    }
    public void setName(String name){
        this.name = name;
    }
    /**
     * Since the balance & name are private varaibles in a class, 
     * It can't be directly accessed or modified by the external classes or packages
     * so, In order to access it, getter methods are generally used in java 
     * which is also called as (Getters)
     */
    public double getBalance(){
        return this.balance;
    }

    public String getName(){
        return this.name;
    }

    @Override 
    public String toString(){
        return this.name + ", " + this.balance;
    }

}