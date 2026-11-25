package foundations.control.jumps;

public class BreakStatement {

    /**
     * When we need to get out from a control statement, we usually use the
     * break statement at that time
     * For Example : a loop is going from 0 to +inf 
     * but we wanted to print no. less than equal to 10 only
     */
    private static void breakStatement(){
        int x = Integer.MAX_VALUE, count = 0;
        for (int i = 0; i <= x; i++){
            System.out.print(i + ", ");
            count++;
            if (count > 10)
                break;
        }
    }
    public static void main(String[] args) {
        breakStatement();
    }
}
