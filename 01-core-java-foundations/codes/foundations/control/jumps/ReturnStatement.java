package foundations.control.jumps;

public class ReturnStatement {

    /**
     * Return statement turns out from the function itself & if there is any return type
     * mentioned, then return will provide that return type
     */
    private static void returnStatement(int n){
        if (n > 10)
            return;
        for (int i = 0; i <= n; i++){
            System.out.print(i + ", ");
        }
    }

    /**
     * @param n
     * @return factorial of n
     * example : factorial of n
     */
    private static int returnStatementNonVoid(int n){
        if (n <= 1)
            return 1;
        return n * returnStatementNonVoid(n - 1);
    }
    public static void main(String[] args) {
        returnStatement(10);
        System.out.println();
        System.out.println(returnStatementNonVoid(5));
    }
}
