package foundations.control.decision;

/**
 * Demonstrates 'if-else' two-way branching.
 */
public class IfElseStatement {
    private static void checkParity(int n) {
        if (n % 2 == 0) {
            System.out.println(n + " is an even number!");
        } else {
            System.out.println(n + " is an odd number!");
        }
    }

    public static void main(String[] args) {
        checkParity(14);
        checkParity(7);
    }
}
