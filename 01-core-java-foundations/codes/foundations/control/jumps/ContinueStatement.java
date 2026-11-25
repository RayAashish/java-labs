package foundations.control.jumps;

public class ContinueStatement {

    /**
     * suppose we are printing from 1 to n
     * & we wanted to skip these number 3, 6, 7, 8, 10
     */
    private static void continueStatement(int n){
        for (int i = 0; i <= n; i++){
            if (i == 3 || i == 6 || i == 7 || i == 8 || i == 10)
                continue;
            System.out.print(i + ", ");
        }
    }
    public static void main(String[] args) {
        continueStatement(15);
    }
}
