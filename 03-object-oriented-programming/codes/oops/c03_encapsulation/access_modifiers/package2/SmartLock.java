package oops.c03_encapsulation.access_modifiers.package2;

public class SmartLock {
    private final String lockCode = "abc";
    private boolean isLocked = true;
    private final int maxTries = 3;
    private int tries = 0;
    public void setLocked(boolean isLocked) {
        this.isLocked = isLocked;
    }

    public boolean isLocked(){
        return this.isLocked;
    }

    public void unlock(String passCode){
        if (!this.isLocked)
            System.out.println("The lock is already unlocked");
        if (this.lockCode.equalsIgnoreCase(passCode)){
            System.out.println("The smartlock is unlocked successfully");
        } else {
            if (this.tries == maxTries){
                System.out.println("You're locked for 2 days");
                //I don't want to make the class super big so didn't write the logic for lock cooldwon period
            }
            else {
                this.tries += 1;
            System.out.println("Wrong passcode\nNo of tries remaining : " + (this.maxTries - this.tries));
            }
        }
    }
}
