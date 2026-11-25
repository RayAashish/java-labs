package foundations.control.loops;

public class ForLoop {
    /**
     * @param n
     * printing prime numbers from 1 to n
     */

    private static void forLoop(int n){
        if (n < 0)
            return;
        boolean flag = false;
        for (int i = 1; i <= n; i++){
            for (int j = 2; j < i; j++){
                if (i % j == 0){
                    flag = false;
                    break;
                }else
                    flag = true;
            }
            if (flag)
                System.out.print(i + ", ");
        }
    }
    public static void main(String[] args) {
        forLoop(20);
    }
}
