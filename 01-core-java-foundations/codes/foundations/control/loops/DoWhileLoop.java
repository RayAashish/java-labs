package foundations.control.loops;

import java.util.Scanner;

public class DoWhileLoop {

    /**
     * The do while loop is little different from others loop
     * because doesn't matter why, it will execute atleast once before 
     * checking the condition.
     */
    private static void doWhileLoop(){
        Scanner sc = new Scanner(System.in);
        do{
            System.out.print("Enter 1 to break: ");
            int n = sc.nextInt();
            if (n == 1)
                break;
            else
                System.out.println("\nYou're still in the loop");
        }while(true);
    }
    public static void main(String[] args) {
        doWhileLoop();
    }
}
