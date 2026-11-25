package oops.c03_encapsulation;

public class Need {
    public static void main(String[] args) {
        Account account = new Account();
        // System.out.println(account.balance); //Compile time error because no one can modify the balance
        account.depositBalance(200.15);
        System.out.println(account.getBalance());
    }
}


/**
 * Account
 * Why do we even need Encapsulation in the first place 
 * Think of a banking application, where a balance field is there 
 * We will never ever think of it to give a direct access to it.
 * We even don't want the balance to go in negative
 * so we make a setter method of balance which can explicelty
 * check of the balance is negative of not. 
 */
class Account{
    private double balance = 0; //Default balance is 0

    public void depositBalance(double amount){
        if (this.balance < 0)
            throw new RuntimeException("Balance can't be negative");
        this.balance += amount;
    }

    double getBalance(){
        return this.balance;
    }
}