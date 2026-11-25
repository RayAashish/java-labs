package foundations.control.loops;

public class WhileLoop {

    /**
     * Printing from 0 to 10 using while loop
     */
    private static void whileLoop(){
        int i = 0;
        while (i <= 10){
            System.out.print(i + ", ");
            i++;
        }
    }
    public static void main(String[] args) {
        whileLoop();
    }
}
